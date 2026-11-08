package com.shoplab.order.saga;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.order.config.SagaProperties;
import com.shoplab.order.idempotency.IdempotencyRecord;
import com.shoplab.order.idempotency.IdempotencyService;
import com.shoplab.order.order.OrderItemRecord;
import com.shoplab.order.order.OrderPricing;
import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.saga.downstream.CartView;
import com.shoplab.order.saga.downstream.DownstreamException;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import com.shoplab.order.saga.downstream.PaymentIntent;
import com.shoplab.order.saga.downstream.SkuPrices;
import com.shoplab.order.saga.downstream.StockReservation;
import com.shoplab.order.saga.downstream.VoucherLock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Orchestrator của checkout saga — bước <b>start</b> (`SL-2.6a`, ADR-0006 D4).
 *
 * <p>Chạy tuần tự: đọc giỏ → lấy giá thật (batch catalog) → lock voucher → reserve stock → tạo
 * order → payment intent. Mỗi bước ra ngoài đều **ghi lại** (saga state + step audit) để crash giữa
 * chừng vẫn biết đã giữ gì, và **fail thì bù** đúng những gì đã giữ.
 *
 * <p><b>Vì sao orchestration nằm ở đây chứ không ở client</b>: client không biết (và không nên
 * biết) phải release cái gì khi bước thứ 3 fail. Đặt compensating ở client nghĩa là mọi client phải
 * cài lại cùng một logic — và client cũ thì không.
 *
 * <p><b>Vì sao sync, không broker</b>: Phase 2 chưa có (§3.4). Đây là baseline để Phase 3 đo lại
 * khi chuyển sang event (ADR-0006 Revisit).
 */
@Service
public class CheckoutService {

  private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class);

  /** Scope idempotency: `<userId>:POST /v1/checkout` — tách khỏi scope tạo order thường. */
  private static final String SCOPE_SUFFIX = ":POST /v1/checkout";

  private final DownstreamGateway downstream;
  private final CheckoutSagaRepository sagas;
  private final OrderWriter writer;
  private final IdempotencyService idempotency;
  private final SagaProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public CheckoutService(
      DownstreamGateway downstream,
      CheckoutSagaRepository sagas,
      OrderWriter writer,
      IdempotencyService idempotency,
      SagaProperties properties,
      ObjectMapper objectMapper,
      Clock clock) {
    this.downstream = downstream;
    this.sagas = sagas;
    this.writer = writer;
    this.idempotency = idempotency;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** {@code replayed=true} ⇒ controller trả 200 thay vì 201 (retry của cùng request). */
  public record CheckoutResult(CheckoutResponse response, boolean replayed) {}

  // __PART_B__

  /**
   * Điểm vào của saga.
   *
   * @throws com.shoplab.order.idempotency.IdempotencyExceptions lỗi 400/409/422 theo ADR-0006 D2
   *     (thiếu key, key đang chạy, dùng lại key với payload khác)
   * @throws AppException 422 giỏ rỗng · 409 SKU không mua được / hết hàng · 503 không tới được
   *     service đích
   */
  public CheckoutResult checkout(
      UUID userId, String bearer, String idempotencyKey, CheckoutRequest request) {
    String scope = userId + SCOPE_SUFFIX;
    String requestHash = IdempotencyService.hash(canonical(request));

    Optional<IdempotencyRecord> completed = idempotency.begin(scope, idempotencyKey, requestHash);
    if (completed.isPresent()) {
      return new CheckoutResult(replay(completed.get()), true);
    }
    try {
      return new CheckoutResult(run(userId, bearer, idempotencyKey, request, scope), false);
    } catch (RuntimeException ex) {
      // Fail ⇒ trả khoá để client sửa giỏ rồi bấm lại được NGAY (không phải chờ job dọn 1 h).
      abortQuietly(scope, idempotencyKey);
      throw ex;
    }
  }

  private CheckoutResponse run(
      UUID userId, String bearer, String idempotencyKey, CheckoutRequest request, String scope) {
    UUID orderId = UUID.randomUUID();
    // Ghi saga TRƯỚC mọi call ra ngoài: crash ở bất kỳ đâu vẫn còn dấu vết "đã bắt đầu".
    sagas.start(CheckoutSagaRecord.started(orderId, userId, true, Instant.now(clock)));

    CheckoutStep current = CheckoutStep.CART;
    long stepStart = System.nanoTime();
    try {
      CartView cart = stepCart(bearer, orderId);
      Map<UUID, Integer> quantities = quantitiesOf(cart);

      current = CheckoutStep.PRICE;
      stepStart = System.nanoTime();
      SkuPrices prices = stepPrice(bearer, orderId, cart);

      current = CheckoutStep.VOUCHER_LOCK;
      stepStart = System.nanoTime();
      List<OrderPricing.PricedItem> rawItems = toPricedItems(cart, prices);
      BigDecimal subtotal = OrderPricing.price(rawItems, BigDecimal.ZERO, currency()).subtotal();
      VoucherLock lock = stepVoucher(bearer, orderId, request.voucherCode(), subtotal);
      OrderPricing.Priced priced = OrderPricing.price(rawItems, discountOf(lock), currency());

      // Đơn 0 đồng (voucher 100%) không tạo payment intent — quyết định đã chốt ở ticket.
      boolean paymentRequired = priced.total().signum() > 0;
      sagas.markPaymentRequired(orderId, paymentRequired, Instant.now(clock));

      current = CheckoutStep.STOCK_RESERVE;
      stepStart = System.nanoTime();
      stepReserve(bearer, orderId, quantities);

      current = CheckoutStep.ORDER_CREATE;
      stepStart = System.nanoTime();
      stepCreateOrder(userId, orderId, idempotencyKey, priced);

      current = CheckoutStep.PAYMENT_INTENT;
      stepStart = System.nanoTime();
      PaymentIntent intent = stepPayment(bearer, orderId, priced, paymentRequired);

      return settle(orderId, idempotencyKey, scope, priced, paymentRequired, intent);
    } catch (RuntimeException ex) {
      fail(orderId, bearer, current, stepStart, ex);
      throw ex;
    }
  }

  // __PART_C__

  private CartView stepCart(String bearer, UUID orderId) {
    long start = System.nanoTime();
    DownstreamResult<CartView> result = downstream.readCart(bearer);
    CartView cart = result.body() == null ? new CartView(List.of()) : result.body();
    if (cart.items().isEmpty()) {
      // 422 chứ không 409: request đúng cú pháp nhưng không xử lý được (không có gì để mua) —
      // client phải thêm hàng; thử lại y nguyên thì vẫn fail.
      throw new AppException(
          ErrorCode.UNPROCESSABLE_ENTITY, "Giỏ hàng rỗng — không có gì để checkout");
    }
    addStep(
        orderId,
        CheckoutStep.CART,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        cart.items().size() + " dòng");
    return cart;
  }

  private SkuPrices stepPrice(String bearer, UUID orderId, CartView cart) {
    long start = System.nanoTime();
    List<UUID> skuIds = cart.items().stream().map(CartView.CartLine::skuId).toList();
    DownstreamResult<SkuPrices> result = downstream.priceSkus(bearer, skuIds);
    SkuPrices prices = result.body() == null ? new SkuPrices(List.of(), skuIds) : result.body();

    List<UUID> unavailable = new ArrayList<>(prices.missingIds());
    Map<UUID, SkuPrices.SkuPrice> bySku =
        prices.items().stream()
            .collect(Collectors.toMap(SkuPrices.SkuPrice::skuId, price -> price, (a, b) -> a));
    for (CartView.CartLine line : cart.items()) {
      if (!bySku.containsKey(line.skuId()) && !unavailable.contains(line.skuId())) {
        unavailable.add(line.skuId());
      }
    }
    if (!unavailable.isEmpty()) {
      // Không có giá ⇒ không tính được tiền ⇒ dừng TRƯỚC khi giữ bất cứ thứ gì (không phải bù).
      throw new AppException(
          ErrorCode.CONFLICT,
          "SKU không mua được (không tồn tại hoặc đã archive): " + idsToString(unavailable));
    }
    addStep(
        orderId,
        CheckoutStep.PRICE,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        bySku.size() + " SKU có giá");
    return prices;
  }

  private VoucherLock stepVoucher(
      String bearer, UUID orderId, String voucherCode, BigDecimal subtotal) {
    long start = System.nanoTime();
    if (voucherCode == null) {
      addStep(
          orderId,
          CheckoutStep.VOUCHER_LOCK,
          SagaStepRecord.SagaStepStatus.SKIPPED,
          null,
          start,
          "không dùng voucher");
      return null;
    }
    DownstreamResult<VoucherLock> result =
        downstream.lockVoucher(bearer, voucherCode, orderId, subtotal);
    VoucherLock lock = result.body();
    if (lock == null || lock.lockId() == null) {
      throw new DownstreamException("promotion", 502, "lock voucher không trả lockId");
    }
    // Ghi NGAY id của lock: đây là thứ duy nhất cho phép bù nếu bước sau fail.
    sagas.markLock(orderId, lock.lockId(), Instant.now(clock));
    addStep(
        orderId,
        CheckoutStep.VOUCHER_LOCK,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "discount=" + lock.discount());
    return lock;
  }

  // __PART_D__

  private void stepReserve(String bearer, UUID orderId, Map<UUID, Integer> quantities) {
    long start = System.nanoTime();
    DownstreamResult<StockReservation> result =
        downstream.reserveStock(bearer, orderId, quantities);
    StockReservation reservation = result.body();
    if (reservation == null || reservation.reservationId() == null) {
      throw new DownstreamException("inventory", 502, "reserve không trả reservationId");
    }
    sagas.markReservation(orderId, reservation.reservationId(), Instant.now(clock));
    addStep(
        orderId,
        CheckoutStep.STOCK_RESERVE,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "reservation=" + reservation.reservationId());
  }

  private void stepCreateOrder(
      UUID userId, UUID orderId, String idempotencyKey, OrderPricing.Priced priced) {
    long start = System.nanoTime();
    Instant now = Instant.now(clock);
    OrderRecord order =
        new OrderRecord(
            orderId,
            userId,
            OrderStatus.PENDING_PAYMENT,
            priced.subtotal(),
            priced.discount(),
            priced.total(),
            currency(),
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
    writer.createPending(order, items);
    addStep(
        orderId,
        CheckoutStep.ORDER_CREATE,
        SagaStepRecord.SagaStepStatus.OK,
        null,
        start,
        "total=" + priced.total());
  }

  private PaymentIntent stepPayment(
      String bearer, UUID orderId, OrderPricing.Priced priced, boolean paymentRequired) {
    long start = System.nanoTime();
    if (!paymentRequired) {
      addStep(
          orderId,
          CheckoutStep.PAYMENT_INTENT,
          SagaStepRecord.SagaStepStatus.SKIPPED,
          null,
          start,
          "total=0 (voucher 100%) nên không cần thanh toán");
      return PaymentIntent.none();
    }
    DownstreamResult<PaymentIntent> result =
        downstream.createPaymentIntent(bearer, orderId, priced.total(), currency());
    PaymentIntent intent = result.body();
    if (intent == null || intent.id() == null) {
      throw new DownstreamException("payment", 502, "payment intent không trả id");
    }
    addStep(
        orderId,
        CheckoutStep.PAYMENT_INTENT,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "intent=" + intent.id());
    return intent;
  }

  private CheckoutResponse settle(
      UUID orderId,
      String idempotencyKey,
      String scope,
      OrderPricing.Priced priced,
      boolean paymentRequired,
      PaymentIntent intent) {
    Instant now = Instant.now(clock);
    if (!intent.absent()) {
      writer.attachPaymentIntent(orderId, intent.id(), now);
    }
    sagas.markCompletedStart(orderId, intent.id(), intent.paymentUrl(), now);

    CheckoutResponse response =
        new CheckoutResponse(
            orderId,
            OrderStatus.PENDING_PAYMENT,
            priced.subtotal(),
            priced.discount(),
            priced.total(),
            currency(),
            paymentRequired,
            intent.id(),
            intent.paymentUrl());
    // Complete khoá SAU CÙNG: chỉ replay được khi order + intent + saga đã tồn tại thật.
    idempotency.complete(scope, idempotencyKey, 201, toJson(response), orderId);
    LOG.info(
        "checkout started: order={} total={} {} paymentRequired={}",
        orderId,
        priced.total(),
        currency(),
        paymentRequired);
    return response;
  }

  // __PART_E__

  /**
   * Xử lý khi saga fail: ghi state bền → ghi step FAILED → **bù** những gì đã giữ.
   *
   * <p>Không ném gì từ đây (exception gốc được ném tiếp ở {@code run}): đang xử lý lỗi mà lỗi thêm
   * lần nữa thì client nhận 500 thay vì status thật của bước hỏng.
   */
  private void fail(
      UUID orderId, String bearer, CheckoutStep step, long stepStart, RuntimeException ex) {
    String failureCode = failureCodeOf(ex);
    Instant now = Instant.now(clock);
    LOG.warn(
        "checkout saga failed: order={} step={} code={} msg={}",
        orderId,
        step,
        failureCode,
        ex.getMessage());
    try {
      Optional<CheckoutSagaRecord> found = sagas.findById(orderId);
      if (found.isEmpty()) {
        return;
      }
      CheckoutSagaRecord saga = found.get();
      sagas.addStep(
          new SagaStepRecord(
              orderId,
              step,
              SagaStepRecord.SagaStepStatus.FAILED,
              httpStatusOf(ex),
              (int) elapsedMs(stepStart),
              truncate(failureCode + ": " + ex.getMessage()),
              now));
      // State bền TRƯỚC khi bù: nếu process chết giữa lúc bù, row đã nói "đã hỏng ở bước nào".
      sagas.markFailed(orderId, failedStateOf(step), failureCode, now);
      compensate(saga, bearer, step, failureCode);
    } catch (RuntimeException cleanupFailure) {
      LOG.error(
          "dọn saga sau fail cũng lỗi: order={} — cần sweeper SL-2.6b dọn tiếp",
          orderId,
          cleanupFailure);
    }
  }

  private void compensate(
      CheckoutSagaRecord saga, String bearer, CheckoutStep step, String failureCode) {
    // Thứ tự: hàng trước, voucher sau — stock là tài nguyên khan hiếm, voucher sai thì sửa được.
    if (saga.reservationId() != null) {
      downstream.releaseStock(bearer, saga.reservationId());
    }
    if (saga.lockId() != null) {
      downstream.releaseVoucher(bearer, saga.lockId());
    }
    if (step == CheckoutStep.ORDER_CREATE || step == CheckoutStep.PAYMENT_INTENT) {
      boolean cancelled =
          writer.cancelPending(
              saga.orderId(), "checkout thất bại: " + failureCode, Instant.now(clock));
      LOG.info("compensating cancel order {}: {}", saga.orderId(), cancelled ? "OK" : "state khác");
    }
  }

  private static CheckoutSagaState failedStateOf(CheckoutStep step) {
    return switch (step) {
      case CART, PRICE, VOUCHER_LOCK, STOCK_RESERVE -> CheckoutSagaState.FAILED_STOCK;
      case ORDER_CREATE -> CheckoutSagaState.FAILED_ORDER;
      case PAYMENT_INTENT -> CheckoutSagaState.FAILED_PAYMENT;
      // Bước của bước finish (`SL-2.6b`) không bao giờ rơi vào đây: nó không bù bằng cách đổi state
      // của lần start, mà để saga giữ `COMPLETED_START` cho retry confirm.
      default -> throw new IllegalStateException("fail() chỉ dùng cho bước start, nhận: " + step);
    };
  }

  private static String failureCodeOf(RuntimeException ex) {
    if (ex instanceof DownstreamException downstream) {
      return downstream.failureCode() + "_" + downstream.service().toUpperCase(Locale.ROOT);
    }
    if (ex instanceof AppException appException) {
      return appException.errorCode().name();
    }
    return "INTERNAL_ERROR";
  }

  private static Integer httpStatusOf(RuntimeException ex) {
    return ex instanceof DownstreamException downstream ? downstream.httpStatus() : null;
  }

  private static String truncate(String detail) {
    if (detail == null) {
      return null;
    }
    return detail.length() <= 200 ? detail : detail.substring(0, 197) + "…";
  }

  // __PART_F__

  private void addStep(
      UUID orderId,
      CheckoutStep step,
      SagaStepRecord.SagaStepStatus status,
      Integer httpStatus,
      long startNanos,
      String detail) {
    sagas.addStep(
        new SagaStepRecord(
            orderId,
            step,
            status,
            httpStatus,
            (int) elapsedMs(startNanos),
            truncate(detail),
            Instant.now(clock)));
  }

  private List<OrderPricing.PricedItem> toPricedItems(CartView cart, SkuPrices prices) {
    Map<UUID, SkuPrices.SkuPrice> bySku =
        prices.items().stream()
            .collect(Collectors.toMap(SkuPrices.SkuPrice::skuId, price -> price, (a, b) -> a));
    List<OrderPricing.PricedItem> items = new ArrayList<>(cart.items().size());
    for (CartView.CartLine line : cart.items()) {
      SkuPrices.SkuPrice price = bySku.get(line.skuId());
      items.add(
          new OrderPricing.PricedItem(
              price.skuId(),
              price.skuCode(),
              price.title(),
              price.unitPrice(),
              currency(),
              line.qty(),
              // lineTotal do OrderPricing tính — truyền null để không có 2 nguồn sự thật cho tiền.
              null));
    }
    return items;
  }

  private static Map<UUID, Integer> quantitiesOf(CartView cart) {
    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (CartView.CartLine line : cart.items()) {
      quantities.merge(line.skuId(), line.qty(), Integer::sum);
    }
    return quantities;
  }

  private static BigDecimal discountOf(VoucherLock lock) {
    return lock == null ? BigDecimal.ZERO : lock.discount();
  }

  private static String idsToString(List<UUID> ids) {
    return ids.stream().map(UUID::toString).collect(Collectors.joining(", "));
  }

  private String currency() {
    return properties.currency();
  }

  /** Đọc saga cho `GET /v1/checkout/{orderId}` — người ngoài nhận 404 (không lộ sự tồn tại). */
  public SagaStatusResponse status(UUID orderId, UUID actorId, boolean admin) {
    CheckoutSagaRecord saga =
        sagas
            .findById(orderId)
            .orElseThrow(() -> new NotFoundException("Checkout saga không tồn tại: " + orderId));
    if (!admin && !saga.userId().equals(actorId)) {
      throw new NotFoundException("Checkout saga không tồn tại: " + orderId);
    }
    return SagaStatusResponse.from(saga);
  }

  private void abortQuietly(String scope, String idempotencyKey) {
    try {
      idempotency.abort(scope, idempotencyKey);
    } catch (RuntimeException ex) {
      // Không để lỗi dọn khoá che lỗi thật của checkout; khoá mồ côi sẽ do job dọn xử lý.
      LOG.warn("không xoá được idempotency key {}: {}", idempotencyKey, ex.getMessage());
    }
  }

  private CheckoutResponse replay(IdempotencyRecord record) {
    try {
      return objectMapper.readValue(record.responseBody(), CheckoutResponse.class);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Không đọc lại được response checkout đã lưu cho key " + record.key(), ex);
    }
  }

  private String canonical(CheckoutRequest request) {
    return objectMapper.writeValueAsString(request);
  }

  private String toJson(CheckoutResponse response) {
    return objectMapper.writeValueAsString(response);
  }

  private static long elapsedMs(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
  }
}
