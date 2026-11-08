package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shoplab.order.config.SagaProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Cổng ra của saga: gọi 5 service khác bằng **sync REST** (ADR-0006 D4 — Phase 2 chưa có broker).
 *
 * <p><b>Ba quyết định ở lớp này</b>:
 *
 * <ol>
 *   <li><b>Timeout là bắt buộc, không phải tùy chọn</b>: {@code 2s} connect / {@code 800ms} read từ
 *       config. Sync mà không có timeout nghĩa là một service chậm giữ connection của order-service
 *       tới khi client bỏ đi — tệ hơn "1 dependency chết kéo checkout chết": nó còn làm cạn pool
 *       của chính mình.
 *   <li><b>Forward JWT của user</b> cho service đích (chúng tự verify — defense in depth). Service
 *       token để Phase 3.
 *   <li><b>Lỗi được phân loại, không nuốt</b>: 4xx từ service đích là lỗi dữ liệu (trả nguyên
 *       status cho client), timeout/5xx là lỗi hạ tầng (503). Xem {@link DownstreamException}.
 * </ol>
 *
 * <p><b>Compensating không được ném exception</b>: {@link #releaseStock} / {@link #releaseVoucher}
 * chỉ log khi fail. Đang bù mà ném tiếp thì client nhận lỗi khác lỗi thật, và phần bù còn lại không
 * bao giờ chạy.
 */
@Component
public class DownstreamGateway {

  private static final Logger LOG = LoggerFactory.getLogger(DownstreamGateway.class);

  private final RestClient client;
  private final SagaProperties properties;
  private final ObjectMapper objectMapper;

  public DownstreamGateway(SagaProperties properties, ObjectMapper objectMapper) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
    factory.setReadTimeout(properties.readTimeout());
    // `RestClient.builder()` thay vì inject `RestClient.Builder`: Boot 4 tách module http-client
    // nên
    // bean đó không có sẵn chỉ với `starter-webmvc` (đã gặp thật: context fail "required a bean of
    // type RestClient$Builder"). Tự dựng builder ở đây rẻ hơn là kéo thêm 1 starter chỉ để lấy
    // bean.
    this.client = RestClient.builder().requestFactory(factory).build();
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** Giỏ của user (`GET /v1/carts`). */
  public DownstreamResult<CartView> readCart(String bearer) {
    return call(
        "cart",
        "đọc giỏ",
        () ->
            toResult(
                client
                    .get()
                    .uri(properties.cartUrl() + "/v1/carts")
                    .headers(headers -> headers.setBearerAuth(bearer))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError, (request, response) -> fail("cart", response))
                    .toEntity(CartView.class)));
  }

  /** Batch giá/tên SKU (`GET /v1/skus?ids=`, `SL-2.13`). */
  public DownstreamResult<SkuPrices> priceSkus(String bearer, List<UUID> skuIds) {
    String ids = skuIds.stream().distinct().map(UUID::toString).collect(Collectors.joining(","));
    return call(
        "catalog",
        "tra giá SKU",
        () ->
            toResult(
                client
                    .get()
                    .uri(properties.catalogUrl() + "/v1/skus?ids={ids}", ids)
                    .headers(headers -> headers.setBearerAuth(bearer))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError, (request, response) -> fail("catalog", response))
                    .toEntity(SkuPrices.class)));
  }

  /** Giữ 1 lượt voucher — {@code orderId} là khoá idempotency của promotion. */
  public DownstreamResult<VoucherLock> lockVoucher(
      String bearer, String code, UUID orderId, BigDecimal subtotal) {
    return call(
        "promotion",
        "lock voucher",
        () ->
            toResult(
                client
                    .post()
                    .uri(properties.promotionUrl() + "/v1/vouchers/lock")
                    .headers(headers -> headers.setBearerAuth(bearer))
                    .body(new VoucherLockBody(code, orderId, subtotal))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError, (request, response) -> fail("promotion", response))
                    .toEntity(VoucherLock.class)));
  }

  /** Giữ chỗ tồn kho — {@code orderId} là khoá idempotency của inventory. */
  public DownstreamResult<StockReservation> reserveStock(
      String bearer, UUID orderId, Map<UUID, Integer> quantities) {
    List<StockReserveBody.Item> items =
        quantities.entrySet().stream()
            .map(entry -> new StockReserveBody.Item(entry.getKey(), entry.getValue()))
            .toList();
    return call(
        "inventory",
        "reserve stock",
        () ->
            toResult(
                client
                    .post()
                    .uri(properties.inventoryUrl() + "/v1/stock/reserve")
                    .headers(headers -> headers.setBearerAuth(bearer))
                    .body(new StockReserveBody(orderId, items))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError, (request, response) -> fail("inventory", response))
                    .toEntity(StockReservation.class)));
  }

  /**
   * Tạo payment intent. Khoá idempotency = {@code orderId} (mỗi order tối đa 1 intent còn sống).
   */
  public DownstreamResult<PaymentIntent> createPaymentIntent(
      String bearer, UUID orderId, BigDecimal amount, String currency) {
    return call(
        "payment",
        "tạo payment intent",
        () ->
            toResult(
                client
                    .post()
                    .uri(properties.paymentUrl() + "/v1/payment-intents")
                    .headers(
                        headers -> {
                          headers.setBearerAuth(bearer);
                          // orderId là khoá tự nhiên: retry cùng đơn ⇒ payment trả lại intent cũ
                          // thay
                          // vì tạo intent thứ hai (payment-service đã ràng buộc 1 intent sống/đơn).
                          headers.set("Idempotency-Key", orderId.toString());
                        })
                    .body(new PaymentIntentBody(orderId, amount, currency))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError, (request, response) -> fail("payment", response))
                    .toEntity(PaymentIntent.class)));
  }

  /**
   * Trả chỗ hàng — <b>bù</b>, nên fail chỉ log. Endpoint {@code release} của inventory idempotent ⇒
   * gọi 2 lần không phá dữ liệu.
   */
  public void releaseStock(String bearer, UUID reservationId) {
    compensate(
        "inventory",
        "release stock",
        () ->
            client
                .post()
                .uri(properties.inventoryUrl() + "/v1/stock/release")
                .headers(headers -> headers.setBearerAuth(bearer))
                .body(new ReservationRefBody(reservationId))
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError, (request, response) -> fail("inventory", response))
                .toBodilessEntity());
  }

  /** Trả lượt voucher — bù, fail chỉ log (endpoint promotion cũng idempotent). */
  public void releaseVoucher(String bearer, UUID lockId) {
    compensate(
        "promotion",
        "release voucher",
        () ->
            client
                .post()
                .uri(properties.promotionUrl() + "/v1/vouchers/release")
                .headers(headers -> headers.setBearerAuth(bearer))
                .body(new VoucherRefBody(lockId))
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError, (request, response) -> fail("promotion", response))
                .toBodilessEntity());
  }

  private <T> T call(String service, String action, Supplier<T> invocation) {
    long startNanos = System.nanoTime();
    try {
      T result = invocation.get();
      LOG.debug("downstream {} {} ok ({} ms)", service, action, elapsedMs(startNanos));
      return result;
    } catch (DownstreamException ex) {
      throw ex;
    } catch (ResourceAccessException ex) {
      // Timeout / connection refused: không tới được service đích, không phải lỗi dữ liệu.
      throw new DownstreamException(service, action + " thất bại: " + ex.getMessage(), ex);
    } catch (RestClientException ex) {
      throw new DownstreamException(service, action + " thất bại: " + ex.getMessage(), ex);
    }
  }

  private void compensate(String service, String action, Runnable invocation) {
    try {
      invocation.run();
      LOG.info("compensating {} {} ok", service, action);
    } catch (RuntimeException ex) {
      // Không ném tiếp: phần bù còn lại vẫn phải chạy, và lỗi THẬT của saga mới là thứ client cần.
      // Hàng/voucher còn treo sẽ được sweeper `SL-2.6b` dọn theo TTL.
      LOG.warn(
          "compensating {} {} FAIL (TTL sweeper SL-2.6b sẽ dọn): {}",
          service,
          action,
          ex.getMessage());
    }
  }

  /**
   * Handler của RestClient: response lỗi → {@link DownstreamException} kèm message service đích.
   */
  private void fail(String service, ClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();
    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    throw new DownstreamException(service, status, extractMessage(body, status));
  }

  /**
   * Lấy {@code message} trong {@code ApiError} của service đích; body lạ thì trả mô tả ngắn.
   *
   * <p>Vì sao không ném khi parse lỗi: đang xử lý một lỗi mà parse body hỏng rồi ném tiếp thì
   * client nhận 500 "unexpected" thay vì status thật — mất đúng thông tin cần nhất.
   */
  private String extractMessage(String body, int status) {
    if (body.isBlank()) {
      return "HTTP " + status + " (body rỗng)";
    }
    try {
      ErrorPayload payload = objectMapper.readValue(body, ErrorPayload.class);
      if (payload != null && payload.message() != null) {
        return payload.message();
      }
    } catch (RuntimeException ignored) {
      // Rơi xuống dùng body thô bên dưới.
    }
    return "HTTP " + status + ": " + (body.length() > 200 ? body.substring(0, 200) + "…" : body);
  }

  private static long elapsedMs(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
  }

  private static <T> DownstreamResult<T> toResult(ResponseEntity<T> entity) {
    return new DownstreamResult<>(entity.getBody(), entity.getStatusCode().value());
  }

  /** Chỉ phần cần đọc của {@code ApiError} — không phụ thuộc toàn bộ schema của commons. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record ErrorPayload(String code, String message) {}

  /** Body {@code POST /v1/vouchers/lock}. */
  record VoucherLockBody(String code, UUID orderId, BigDecimal subtotal) {}

  /**
   * Body {@code POST /v1/stock/reserve} — tên field phải khớp <b>chính xác</b> {@code
   * ReserveRequest} của inventory: {@code items}, không phải {@code lines}.
   *
   * <p>Bài học thật (`SL-2.6a`): stub trong IT không validate body nên sai tên field vẫn "pass";
   * chỉ chạy compose thật mới lộ (`400 VALIDATION_ERROR` từ inventory). Vì vậy stub giờ kiểm tối
   * thiểu {@code items} tồn tại.
   */
  record StockReserveBody(UUID orderId, List<Item> items) {
    record Item(UUID skuId, int qty) {}
  }

  /** Body {@code POST /v1/stock/{commit,release}} — tên field là {@code reservationId}. */
  record ReservationRefBody(UUID reservationId) {}

  /** Body {@code POST /v1/vouchers/{commit,release}} — tên field là {@code lockId}. */
  record VoucherRefBody(UUID lockId) {}

  /** Body {@code POST /v1/payment-intents}. */
  record PaymentIntentBody(UUID orderId, BigDecimal amount, String currency) {}
}
