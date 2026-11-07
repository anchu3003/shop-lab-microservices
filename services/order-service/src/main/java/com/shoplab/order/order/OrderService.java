package com.shoplab.order.order;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.order.idempotency.IdempotencyRecord;
import com.shoplab.order.idempotency.IdempotencyService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Nghiệp vụ đơn hàng: tạo (idempotent) + chuyển trạng thái + đọc.
 *
 * <p>Ba bất biến:
 *
 * <ol>
 *   <li><b>Tiền do server tính</b> từ snapshot {@code unitPrice × qty} — client không gửi tổng.
 *   <li><b>Idempotent</b>: cùng {@code Idempotency-Key} + cùng payload ⇒ trả lại đúng response cũ,
 *       không tạo đơn thứ hai.
 *   <li><b>State machine một chiều</b>: chỉ {@code PENDING_PAYMENT} mới chuyển được, và mỗi lần
 *       chuyển đều ghi history.
 * </ol>
 */
@Service
public class OrderService {

  private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);
  private static final String CREATE_SCOPE_SUFFIX = ":POST /v1/orders";

  private final OrderRepository orders;
  private final OrderWriter writer;
  private final IdempotencyService idempotency;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public OrderService(
      OrderRepository orders,
      OrderWriter writer,
      IdempotencyService idempotency,
      ObjectMapper objectMapper,
      Clock clock) {
    this.orders = orders;
    this.writer = writer;
    this.idempotency = idempotency;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** {@code replayed=true} ⇒ đây là replay của request trước (controller trả 200 thay vì 201). */
  public record CreateResult(OrderResponse response, boolean replayed) {}

  /**
   * Tạo đơn hàng.
   *
   * @throws IdempotencyService ... các lỗi 400/409/422 theo ADR-0006 D2
   * @throws AppException (400) khi payload sai về nghiệp vụ (discount > subtotal, items rỗng...)
   */
  public CreateResult create(UUID userId, String idempotencyKey, CreateOrderRequest request) {
    // Tính tiền TRƯỚC khi giữ khoá: payload sai thì không nên "đốt" Idempotency-Key của client.
    OrderPricing.Priced priced =
        OrderPricing.price(toPricedItems(request), request.discount(), request.currencyOrDefault());

    String scope = userId + CREATE_SCOPE_SUFFIX;
    String requestHash = IdempotencyService.hash(canonical(request));

    Optional<IdempotencyRecord> completed = idempotency.begin(scope, idempotencyKey, requestHash);
    if (completed.isPresent()) {
      return new CreateResult(replay(completed.get()), true);
    }

    Instant now = Instant.now(clock);
    UUID orderId = UUID.randomUUID();
    OrderRecord order =
        new OrderRecord(
            orderId,
            userId,
            OrderStatus.PENDING_PAYMENT,
            priced.subtotal(),
            priced.discount(),
            priced.total(),
            request.currencyOrDefault(),
            idempotencyKey,
            null,
            now,
            now);
    List<OrderItemRecord> items =
        priced.items().stream()
            .map(
                item ->
                    new OrderItemRecord(
                        UUID.randomUUID(),
                        item.skuId(),
                        item.skuCode(),
                        item.title(),
                        item.unitPrice(),
                        item.currency(),
                        item.qty(),
                        item.lineTotal()))
            .toList();

    OrderResponse response = OrderResponse.from(order, items);
    // Ghi order + history + đánh dấu idempotency trong MỘT transaction của OrderWriter.
    writer.create(order, items, scope, idempotencyKey, toJson(response));
    LOG.info(
        "order created: id={} user={} total={} {}",
        orderId,
        userId,
        order.total(),
        order.currency());
    return new CreateResult(response, false);
  }

  /**
   * Chuyển trạng thái (confirm / cancel / expire) — compare-and-set, ghi history.
   *
   * <p>Idempotent khi trùng đích (gọi 2 lần → 200, không ghi history lần 2). Chuyển <b>sai
   * hướng</b> (vd cancel đơn đã CONFIRMED) → 409: đó là lỗi logic của caller, không phải "thử lại
   * là được".
   */
  @Transactional
  public OrderResponse changeStatus(
      UUID orderId, OrderStatus target, String reason, UUID actorId, boolean admin) {
    OrderRecord order = requireReadable(orderId, actorId, admin);
    Instant now = Instant.now(clock);

    if (order.status() == target) {
      return OrderResponse.from(order, orders.findItems(orderId));
    }
    if (order.status().isFinal()) {
      throw new AppException(
          ErrorCode.CONFLICT,
          "Đơn đã ở trạng thái cuối (" + order.status() + "), không chuyển sang " + target);
    }
    if (orders.markStatus(orderId, order.status(), target, now) == 0) {
      OrderRecord current =
          orders
              .findById(orderId)
              .orElseThrow(() -> new NotFoundException("Order không tồn tại: " + orderId));
      if (current.status() == target) {
        return OrderResponse.from(current, orders.findItems(orderId));
      }
      throw new AppException(
          ErrorCode.CONFLICT,
          "Đơn vừa đổi trạng thái bởi request khác ("
              + current.status()
              + "), không chuyển sang "
              + target);
    }
    orders.insertHistory(orderId, order.status(), target, reason, now);
    LOG.info("order {} status: {} -> {}", orderId, order.status(), target);
    return OrderResponse.from(order.withStatus(target, now), orders.findItems(orderId));
  }

  /** Đọc một đơn — chỉ chủ sở hữu hoặc admin. */
  @Transactional(readOnly = true)
  public OrderResponse get(UUID orderId, UUID actorId, boolean admin) {
    OrderRecord order = requireReadable(orderId, actorId, admin);
    return OrderResponse.from(order, orders.findItems(orderId));
  }

  /**
   * Danh sách đơn — buyer chỉ thấy đơn của mình; admin thấy tất cả (hoặc lọc theo {@code userId}).
   *
   * <p>Phân trang offset (giống catalog) — đủ cho Phase 2; keyset khi số đơn lớn (điều kiện revisit
   * ở `TD-6`).
   */
  @Transactional(readOnly = true)
  public OrderPage list(UUID actorId, boolean admin, String pageRaw, String sizeRaw) {
    int page = parseNonNegative(pageRaw, 0, "page");
    int size = parseNonNegative(sizeRaw, 20, "size");
    if (size < 1 || size > 100) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "size phải trong khoảng 1..100");
    }
    UUID ownerFilter = admin ? null : actorId;
    List<OrderRecord> content = orders.findByUser(ownerFilter, page * size, size);
    List<OrderResponse> responses =
        content.stream()
            .map(order -> OrderResponse.from(order, orders.findItems(order.id())))
            .toList();
    long total = orders.countByUser(ownerFilter);
    int totalPages = (int) Math.ceil((double) total / size);
    return new OrderPage(responses, page, size, total, totalPages);
  }

  private OrderRecord requireReadable(UUID orderId, UUID actorId, boolean admin) {
    OrderRecord order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order không tồn tại: " + orderId));
    if (!admin && !order.userId().equals(actorId)) {
      // 404 chứ không 403: không xác nhận cho người ngoài biết đơn này có tồn tại hay không.
      throw new NotFoundException("Order không tồn tại: " + orderId);
    }
    return order;
  }

  private OrderResponse replay(IdempotencyRecord record) {
    try {
      return objectMapper.readValue(record.responseBody(), OrderResponse.class);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Không đọc lại được response đã lưu cho key " + record.key(), ex);
    }
  }

  private String toJson(OrderResponse response) {
    return objectMapper.writeValueAsString(response);
  }

  /**
   * Canonical payload để hash: serialize record theo đúng thứ tự field khai báo ⇒ cùng body ⇒ cùng
   * hash.
   *
   * <p>Nếu sau này nhận {@code Map} thì phải sort key trước khi hash, không thì {@code
   * {"a":1,"b":2}} và {@code {"b":2,"a":1}} thành 2 hash khác nhau và idempotency mất tác dụng.
   */
  private String canonical(CreateOrderRequest request) {
    return objectMapper.writeValueAsString(request);
  }

  private static List<OrderPricing.PricedItem> toPricedItems(CreateOrderRequest request) {
    String currency = request.currencyOrDefault();
    return request.items().stream()
        .map(
            item ->
                new OrderPricing.PricedItem(
                    item.skuId(),
                    item.skuCode(),
                    item.title(),
                    item.unitPrice(),
                    currency,
                    item.qty(),
                    null))
        .toList();
  }

  private static int parseNonNegative(String raw, int defaultValue, String field) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    try {
      int value = Integer.parseInt(raw);
      if (value < 0) {
        throw new AppException(ErrorCode.VALIDATION_ERROR, field + " phải >= 0");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, field + " phải là số nguyên: " + raw);
    }
  }
}
