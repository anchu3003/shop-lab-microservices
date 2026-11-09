package com.shoplab.order.saga;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.order.testsupport.TokenMinter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của checkout saga (`SL-2.6a`) — Postgres thật + **stub HTTP thật** cho 5 service
 * đích.
 *
 * <p><b>Vì sao stub bằng {@link HttpServer} của JDK chứ không mock {@code DownstreamGateway}</b>:
 * mock lớp gateway là bỏ qua đúng phần dễ sai nhất — serialize body, đọc JSON của service khác, map
 * status lỗi. Stub HTTP giữ nguyên đường thật (RestClient + Jackson), chỉ thay "đầu dây bên kia";
 * nhờ vậy test cũng chạy song song an toàn (mock có expectation tuần tự thì không).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      "spring.datasource.hikari.maximum-pool-size=20",
      // SL-3.5: order giờ có consumer `q.order`. IT chạy KHÔNG có broker ⇒ tắt listener của commons
      // (bài học SL-3.4: `spring.rabbitmq.listener.simple.auto-startup` của Boot không áp cho
      // factory
      // tự khai ⇒ thiếu cờ này là `FatalListenerStartupException` làm đỏ cả context).
      "shoplab.events.consumer-auto-startup=false"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CheckoutSagaIT {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);
  private static final Downstream DOWNSTREAM = Downstream.start();

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CheckoutSagaSweeper sweeper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  @TestConfiguration
  static class TestJwtConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
      return MINTER.decoder();
    }
  }

  @DynamicPropertySource
  static void sagaUrls(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("shoplab.saga.cart-url", DOWNSTREAM::url);
    registry.add("shoplab.saga.catalog-url", DOWNSTREAM::url);
    registry.add("shoplab.saga.inventory-url", DOWNSTREAM::url);
    registry.add("shoplab.saga.promotion-url", DOWNSTREAM::url);
    registry.add("shoplab.saga.payment-url", DOWNSTREAM::url);
    registry.add("shoplab.saga.connect-timeout", () -> "1s");
    registry.add("shoplab.saga.read-timeout", () -> "1s");
    // Sweeper: cửa sổ thanh toán ngắn để test expire được ngay; delay/interval dài để scheduler
    // KHÔNG tự chạy giữa test (test gọi `expireDue()` trực tiếp ⇒ tất định).
    registry.add("shoplab.saga.payment-window", () -> "1s");
    registry.add("shoplab.saga.sweep-interval", () -> "1h");
    registry.add("shoplab.saga.sweep-initial-delay", () -> "1h");
  }

  @AfterAll
  static void stopStub() {
    DOWNSTREAM.close();
  }

  @BeforeEach
  void resetStubAndTables() {
    DOWNSTREAM.reset();
    jdbc.update("delete from checkout_saga_steps");
    jdbc.update("delete from checkout_sagas");
    jdbc.update("delete from order_status_history");
    jdbc.update("delete from order_items");
    jdbc.update("delete from orders");
    jdbc.update("delete from idempotency_keys");
  }

  // __TESTS__

  @Test
  @DisplayName(
      "happy path: 201 + order PENDING_PAYMENT + payment intent; saga COMPLETED_START, đủ 6 bước")
  void happyPath() {
    UUID skuId = UUID.randomUUID();
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", skuId.toString(), "qty", 2));
    DOWNSTREAM.unitPrice = new BigDecimal("100000.00");
    DOWNSTREAM.discount = new BigDecimal("20000.00");

    HttpResult result = checkout("VOUCHER10", "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(201);
    CheckoutResponse body = objectMapper.readValue(result.body(), CheckoutResponse.class);
    assertThat(body.status().name()).isEqualTo("PENDING_PAYMENT");
    assertThat(body.subtotal()).isEqualByComparingTo("200000.00");
    assertThat(body.discount()).isEqualByComparingTo("20000.00");
    assertThat(body.total()).isEqualByComparingTo("180000.00");
    assertThat(body.paymentRequired()).isTrue();
    assertThat(body.paymentIntentId()).isNotNull();
    assertThat(body.paymentUrl()).isNotBlank();

    Map<String, Object> order =
        jdbc.queryForMap(
            "select status, total, payment_intent_id from orders where id = ?::uuid",
            body.orderId().toString());
    assertThat(order.get("status")).isEqualTo("PENDING_PAYMENT");
    assertThat((BigDecimal) order.get("total")).isEqualByComparingTo("180000.00");
    assertThat(order.get("payment_intent_id")).isNotNull();

    Map<String, Object> saga =
        jdbc.queryForMap(
            "select state, payment_required, lock_id, reservation_id from checkout_sagas"
                + " where order_id = ?::uuid",
            body.orderId().toString());
    assertThat(saga.get("state")).isEqualTo("COMPLETED_START");
    assertThat(saga.get("payment_required")).isEqualTo(true);
    assertThat(saga.get("lock_id")).isNotNull();
    assertThat(saga.get("reservation_id")).isNotNull();

    List<String> steps =
        jdbc.queryForList(
            "select step || ':' || status from checkout_saga_steps"
                + " where order_id = ?::uuid order by created_at, id",
            String.class,
            body.orderId().toString());
    assertThat(steps)
        .containsExactly(
            "CART:OK",
            "PRICE:OK",
            "VOUCHER_LOCK:OK",
            "STOCK_RESERVE:OK",
            "ORDER_CREATE:OK",
            "PAYMENT_INTENT:OK");
  }

  @Test
  @DisplayName(
      "đơn 0 đồng (voucher 100%): KHÔNG gọi payment, paymentRequired=false, bước payment SKIPPED")
  void zeroTotalSkipsPayment() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.unitPrice = new BigDecimal("50000.00");
    DOWNSTREAM.discount = new BigDecimal("50000.00");

    HttpResult result = checkout("FREE100", "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(201);
    CheckoutResponse body = objectMapper.readValue(result.body(), CheckoutResponse.class);
    assertThat(body.total()).isEqualByComparingTo("0.00");
    assertThat(body.paymentRequired()).isFalse();
    assertThat(body.paymentIntentId()).isNull();
    assertThat(DOWNSTREAM.count("payment-intents")).isZero();

    Map<String, Object> saga =
        jdbc.queryForMap(
            "select state, payment_required, payment_intent_id from checkout_sagas"
                + " where order_id = ?::uuid",
            body.orderId().toString());
    assertThat(saga.get("state")).isEqualTo("COMPLETED_START");
    assertThat(saga.get("payment_required")).isEqualTo(false);
    assertThat(saga.get("payment_intent_id")).isNull();

    String paymentStep =
        jdbc.queryForObject(
            "select status from checkout_saga_steps where order_id = ?::uuid"
                + " and step = 'PAYMENT_INTENT'",
            String.class,
            body.orderId().toString());
    assertThat(paymentStep).isEqualTo("SKIPPED");
  }

  @Test
  @DisplayName("giỏ rỗng → 422, không tạo order, không giữ voucher/hàng")
  void emptyCartIsUnprocessable() {
    DOWNSTREAM.cartItems = List.of();

    HttpResult result = checkout(null, "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(422);
    assertThat(result.body()).contains("UNPROCESSABLE_ENTITY");
    assertThat(countOrders()).isZero();
    assertThat(DOWNSTREAM.count("vouchers/lock")).isZero();
    assertThat(DOWNSTREAM.count("stock/reserve")).isZero();
  }

  @Test
  @DisplayName("SKU không mua được → 409, CHƯA giữ gì nên không có compensating call nào")
  void missingSkuHoldsNothing() {
    UUID skuId = UUID.randomUUID();
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", skuId.toString(), "qty", 1));
    DOWNSTREAM.skuMissing = true;

    HttpResult result = checkout("VOUCHER10", "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(409);
    assertThat(result.body()).contains("CONFLICT").contains(skuId.toString());
    assertThat(countOrders()).isZero();
    assertThat(DOWNSTREAM.count("vouchers/lock")).isZero();
    assertThat(DOWNSTREAM.count("stock/reserve")).isZero();
  }

  // __TESTS2__

  @Test
  @DisplayName("hết hàng ở bước reserve → 409 + voucher đã lock được RELEASE (state FAILED_STOCK)")
  void reserveConflictReleasesVoucher() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    DOWNSTREAM.reserveStatus = 409;

    HttpResult result = checkout("VOUCHER10", "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(409);
    assertThat(DOWNSTREAM.count("stock/reserve")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/release")).isEqualTo(1);
    assertThat(countOrders()).isZero();

    String state =
        jdbc.queryForObject(
            "select state from checkout_sagas order by created_at desc limit 1", String.class);
    assertThat(state).isEqualTo("FAILED_STOCK");
  }

  @Test
  @DisplayName("payment lỗi 500 → 503 + release stock & voucher + order CANCELLED (FAILED_PAYMENT)")
  void paymentFailureCompensates() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    DOWNSTREAM.paymentStatus = 500;

    HttpResult result = checkout("VOUCHER10", "key-" + UUID.randomUUID());

    assertThat(result.status()).isEqualTo(503);
    assertThat(DOWNSTREAM.count("stock/reserve")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("stock/release")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/release")).isEqualTo(1);

    Map<String, Object> saga =
        jdbc.queryForMap(
            "select order_id, state, failure_code from checkout_sagas"
                + " order by created_at desc limit 1");
    assertThat(saga.get("state")).isEqualTo("FAILED_PAYMENT");
    assertThat((String) saga.get("failure_code")).contains("DOWNSTREAM");

    String orderStatus =
        jdbc.queryForObject(
            "select status from orders where id = ?::uuid", String.class, saga.get("order_id"));
    assertThat(orderStatus).isEqualTo("CANCELLED");
    // Bù xong thì khoá idempotency bị trả ⇒ client retry lại được ngay với cùng key.
    assertThat(jdbc.queryForObject("select count(*) from idempotency_keys", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("retry cùng Idempotency-Key → 200 + cùng orderId, KHÔNG gọi lại reserve/lock")
  void replayReturnsSameOrder() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    String key = "key-" + UUID.randomUUID();

    HttpResult first = checkout("VOUCHER10", key);
    HttpResult second = checkout("VOUCHER10", key);

    assertThat(first.status()).isEqualTo(201);
    assertThat(second.status()).isEqualTo(200);
    assertThat(second.headers().get("idempotent-replayed")).isEqualTo("true");
    assertThat(objectMapper.readValue(second.body(), CheckoutResponse.class).orderId())
        .isEqualTo(objectMapper.readValue(first.body(), CheckoutResponse.class).orderId());
    assertThat(countOrders()).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/lock")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("stock/reserve")).isEqualTo(1);
  }

  @Test
  @DisplayName("10 request song song cùng key ⇒ 1 order, 1 reservation, 1 voucher lock")
  void parallelSameKeyCreatesOneOrder() throws Exception {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    String key = "key-" + UUID.randomUUID();

    int parallel = 10;
    ExecutorService pool = Executors.newFixedThreadPool(parallel);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<HttpResult>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < parallel; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return checkout("VOUCHER10", key);
                }));
      }
      start.countDown();
      Map<Integer, Integer> statuses = new java.util.TreeMap<>();
      for (Future<HttpResult> future : futures) {
        int status = future.get(30, TimeUnit.SECONDS).status();
        statuses.merge(status, 1, Integer::sum);
      }
      assertThat(statuses).containsKey(201);
    } finally {
      pool.shutdownNow();
    }

    assertThat(countOrders()).isEqualTo(1);
    assertThat(DOWNSTREAM.count("stock/reserve")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/lock")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("payment-intents")).isEqualTo(1);
  }

  // __HELPERS__

  /** Một buyer cho mỗi test (mỗi test = 1 instance) ⇒ scope idempotency nhất quán trong test. */
  private final String buyer = MINTER.token(UUID.randomUUID(), "BUYER");

  // ==========================================================================
  // SL-2.6b: confirm · cancel · sweeper
  // ==========================================================================

  @Test
  @DisplayName(
      "confirm khi payment SUCCEEDED → 200 CONFIRMED + commit stock/voucher + order CONFIRMED")
  void confirmAfterPayment() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");
    DOWNSTREAM.paymentIntentStatus = "SUCCEEDED";

    HttpResult result = postJson("/v1/checkout/" + orderId + "/confirm", Map.of());

    assertThat(result.status()).isEqualTo(200);
    assertThat(result.body()).contains("\"state\":\"CONFIRMED\"");
    assertThat(DOWNSTREAM.count("stock/commit")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/commit")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select status from orders where id = ?::uuid", String.class, orderId.toString()))
        .isEqualTo("CONFIRMED");
    List<String> steps =
        jdbc.queryForList(
            "select step || ':' || status from checkout_saga_steps"
                + " where order_id = ?::uuid order by created_at, id",
            String.class,
            orderId.toString());
    assertThat(steps).contains("PAYMENT_VERIFY:OK", "STOCK_COMMIT:OK", "VOUCHER_COMMIT:OK");
  }

  @Test
  @DisplayName("confirm khi payment CHƯA SUCCEEDED → 409, không commit gì")
  void confirmBeforePaymentIsConflict() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");

    HttpResult result = postJson("/v1/checkout/" + orderId + "/confirm", Map.of());

    assertThat(result.status()).isEqualTo(409);
    assertThat(result.body()).contains("CONFLICT");
    assertThat(DOWNSTREAM.count("stock/commit")).isZero();
    assertThat(
            jdbc.queryForObject(
                "select status from orders where id = ?::uuid", String.class, orderId.toString()))
        .isEqualTo("PENDING_PAYMENT");
  }

  @Test
  @DisplayName("confirm 2 lần → 200 cả hai, history chỉ 2 dòng (create + confirm)")
  void confirmIsIdempotent() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");
    DOWNSTREAM.paymentIntentStatus = "SUCCEEDED";

    assertThat(postJson("/v1/checkout/" + orderId + "/confirm", Map.of()).status()).isEqualTo(200);
    assertThat(postJson("/v1/checkout/" + orderId + "/confirm", Map.of()).status()).isEqualTo(200);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from order_status_history where order_id = ?::uuid",
                Integer.class,
                orderId.toString()))
        .isEqualTo(2);
  }

  // __FINISH_IT2__

  @Test
  @DisplayName("đơn 0 đồng: confirm KHÔNG gọi payment-service, vẫn commit + CONFIRMED")
  void confirmZeroTotalSkipsPayment() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.unitPrice = new BigDecimal("50000.00");
    DOWNSTREAM.discount = new BigDecimal("50000.00");
    UUID orderId = checkoutOrder("FREE100");
    int paymentReadsBefore = DOWNSTREAM.count("/v1/payment-intents/");

    HttpResult result = postJson("/v1/checkout/" + orderId + "/confirm", Map.of());

    assertThat(result.status()).isEqualTo(200);
    assertThat(result.body()).contains("\"state\":\"CONFIRMED\"");
    assertThat(DOWNSTREAM.count("/v1/payment-intents/")).isEqualTo(paymentReadsBefore);
    String verifyStep =
        jdbc.queryForObject(
            "select status from checkout_saga_steps where order_id = ?::uuid"
                + " and step = 'PAYMENT_VERIFY'",
            String.class,
            orderId.toString());
    assertThat(verifyStep).isEqualTo("SKIPPED");
  }

  @Test
  @DisplayName(
      "cancel → release stock + voucher, order CANCELLED; cancel 2 lần vẫn 200; confirm sau đó 409")
  void cancelReleasesAndBlocksConfirm() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");

    HttpResult cancelled =
        postJson("/v1/checkout/" + orderId + "/cancel", Map.of("reason", "đổi ý"));

    assertThat(cancelled.status()).isEqualTo(200);
    assertThat(cancelled.body()).contains("\"state\":\"CANCELLED\"");
    assertThat(DOWNSTREAM.count("stock/release")).isEqualTo(1);
    assertThat(DOWNSTREAM.count("vouchers/release")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select status from orders where id = ?::uuid", String.class, orderId.toString()))
        .isEqualTo("CANCELLED");

    assertThat(postJson("/v1/checkout/" + orderId + "/cancel", Map.of()).status()).isEqualTo(200);
    DOWNSTREAM.paymentIntentStatus = "SUCCEEDED";
    assertThat(postJson("/v1/checkout/" + orderId + "/confirm", Map.of()).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("cancel sau confirm → 409 (tiền đã về, huỷ cần hoàn tiền)")
  void cancelAfterConfirmIsConflict() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");
    DOWNSTREAM.paymentIntentStatus = "SUCCEEDED";
    assertThat(postJson("/v1/checkout/" + orderId + "/confirm", Map.of()).status()).isEqualTo(200);

    assertThat(postJson("/v1/checkout/" + orderId + "/cancel", Map.of()).status()).isEqualTo(409);
  }

  @Test
  @DisplayName(
      "sweeper: saga quá payment-window ⇒ order EXPIRED + saga EXPIRED; vòng 2 không làm gì")
  void sweeperExpiresOverdueOrder() {
    DOWNSTREAM.cartItems = List.of(Map.of("skuId", UUID.randomUUID().toString(), "qty", 1));
    DOWNSTREAM.discount = new BigDecimal("10000.00");
    UUID orderId = checkoutOrder("VOUCHER10");
    // Đẩy saga về quá khứ để vượt payment-window (1 s trong IT) mà không phải chờ.
    jdbc.update(
        "update checkout_sagas set created_at = now() - interval '2 minutes'"
            + " where order_id = ?::uuid",
        orderId.toString());

    int first = sweeper.expireDue();
    int second = sweeper.expireDue();

    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    assertThat(
            jdbc.queryForObject(
                "select status from orders where id = ?::uuid", String.class, orderId.toString()))
        .isEqualTo("EXPIRED");
    assertThat(
            jdbc.queryForObject(
                "select state from checkout_sagas where order_id = ?::uuid",
                String.class,
                orderId.toString()))
        .isEqualTo("EXPIRED");
    // Sweeper không gọi release (không có token user) — hàng/voucher về theo TTL sweeper.
    assertThat(DOWNSTREAM.count("stock/release")).isZero();
    assertThat(DOWNSTREAM.count("vouchers/release")).isZero();
  }

  private HttpResult checkout(String voucherCode, String idempotencyKey) {
    Map<String, Object> body = voucherCode == null ? Map.of() : Map.of("voucherCode", voucherCode);
    return send(HttpMethod.POST, "/v1/checkout", body, Map.of("Idempotency-Key", idempotencyKey));
  }

  private int countOrders() {
    Integer count = jdbc.queryForObject("select count(*) from orders", Integer.class);
    return count == null ? 0 : count;
  }

  /** `SL-2.6b`: confirm/cancel dùng token của buyer (đã set trong `send`). */
  private HttpResult postJson(String path, Object body) {
    return send(HttpMethod.POST, path, body, Map.of());
  }

  /** Checkout xong rồi trả về `orderId` — dùng chung cho các test bước finish. */
  private UUID checkoutOrder(String voucherCode) {
    HttpResult result = checkout(voucherCode, "key-" + UUID.randomUUID());
    assertThat(result.status()).isEqualTo(201);
    return objectMapper.readValue(result.body(), CheckoutResponse.class).orderId();
  }

  private HttpResult send(
      HttpMethod method, String path, Object body, Map<String, String> extraHeaders) {
    String payload = body == null ? "" : objectMapper.writeValueAsString(body);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(30))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer);
    if (!payload.isEmpty()) {
      builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
    extraHeaders.forEach(builder::header);
    builder.method(
        method.name(),
        payload.isEmpty()
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
    try {
      HttpResponse<String> response =
          httpClient.send(
              builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      Map<String, String> headers =
          response.headers().map().entrySet().stream()
              .collect(
                  java.util.stream.Collectors.toMap(
                      entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT),
                      entry -> entry.getValue().getFirst()));
      return new HttpResult(response.statusCode(), response.body(), headers);
    } catch (Exception ex) {
      throw new IllegalStateException("Gọi " + method + " " + path + " thất bại", ex);
    }
  }

  /** Response thô — chỉ cần status/body/header để assert. */
  private record HttpResult(int status, String body, Map<String, String> headers) {}

  // __STUB__

  /** Stub 5 service đích: đủ endpoint saga gọi, cộng công tắc bơm lỗi từng bước. */
  static final class Downstream implements AutoCloseable {

    private final HttpServer server;
    private final List<String> calls = Collections.synchronizedList(new ArrayList<>());

    volatile List<Map<String, Object>> cartItems = List.of();
    volatile boolean skuMissing;
    volatile BigDecimal unitPrice = new BigDecimal("100000.00");
    volatile BigDecimal discount = BigDecimal.ZERO;
    volatile int reserveStatus = 201;
    volatile int paymentStatus = 201;

    /** Trạng thái mà `GET /v1/payment-intents/{id}` trả về (`SL-2.6b`). */
    volatile String paymentIntentStatus = "REQUIRES_PAYMENT";

    private Downstream(HttpServer server) {
      this.server = server;
    }

    static Downstream start() {
      try {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        Downstream stub = new Downstream(server);
        server.createContext("/", stub::handle);
        // Pool nhiều thread để test "10 request song song" đo được đúng thứ cần đo (idempotency của
        // saga), không phải đo độ kiên nhẫn của stub.
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
        return stub;
      } catch (IOException ex) {
        throw new IllegalStateException("không dựng được stub downstream", ex);
      }
    }

    String url() {
      return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
      server.stop(0);
    }

    void reset() {
      calls.clear();
      cartItems = List.of();
      skuMissing = false;
      unitPrice = new BigDecimal("100000.00");
      discount = BigDecimal.ZERO;
      reserveStatus = 201;
      paymentStatus = 201;
      paymentIntentStatus = "REQUIRES_PAYMENT";
    }

    /** Số request đã tới stub (path kết thúc bằng {@code pathSuffix}). */
    int count(String pathSuffix) {
      synchronized (calls) {
        return (int) calls.stream().filter(call -> call.endsWith(pathSuffix)).count();
      }
    }

    private void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      calls.add(exchange.getRequestMethod() + " " + path);
      try {
        route(exchange, path);
      } finally {
        exchange.close();
      }
    }

    private void route(HttpExchange exchange, String path) throws IOException {
      // `SL-2.6b`: saga đọc lại payment intent để tự xác minh "đã trả tiền chưa".
      if (path.startsWith("/v1/payment-intents/")) {
        String id = path.substring("/v1/payment-intents/".length());
        write(exchange, 200, "{\"id\":\"" + id + "\",\"status\":\"" + paymentIntentStatus + "\"}");
        return;
      }
      switch (path) {
        case "/v1/carts" -> write(exchange, 200, cartJson());
        case "/v1/skus" -> write(exchange, 200, skuJson(exchange));
        case "/v1/vouchers/lock" ->
            write(
                exchange,
                201,
                "{\"lockId\":\""
                    + UUID.randomUUID()
                    + "\",\"discount\":"
                    + discount.toPlainString()
                    + "}");
        case "/v1/vouchers/release", "/v1/stock/release" ->
            write(exchange, 200, "{\"status\":\"RELEASED\"}");
        case "/v1/vouchers/commit", "/v1/stock/commit" ->
            write(exchange, 200, "{\"status\":\"COMMITTED\"}");
        case "/v1/stock/reserve" -> reserve(exchange);
        case "/v1/payment-intents" -> payment(exchange);
        default ->
            write(exchange, 404, "{\"code\":\"NOT_FOUND\",\"message\":\"stub: " + path + "\"}");
      }
    }

    private void reserve(HttpExchange exchange) throws IOException {
      // Stub kiểm tối thiểu body: saga phải gửi đúng field `items` (inventory trả 400 nếu sai) —
      // bài học SL-2.6a: stub dễ dãi quá thì test xanh mà chạy thật đỏ.
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      if (!body.contains("\"items\"")) {
        write(
            exchange,
            400,
            "{\"code\":\"VALIDATION_ERROR\",\"message\":\"stub: thiếu items trong reserve body\"}");
        return;
      }
      if (reserveStatus != 201) {
        write(exchange, reserveStatus, "{\"code\":\"CONFLICT\",\"message\":\"stub: hết hàng\"}");
        return;
      }
      write(exchange, 201, "{\"reservationId\":\"" + UUID.randomUUID() + "\"}");
    }

    private void payment(HttpExchange exchange) throws IOException {
      if (paymentStatus >= 400) {
        write(
            exchange,
            paymentStatus,
            "{\"code\":\"INTERNAL_ERROR\",\"message\":\"stub: payment chết\"}");
        return;
      }
      write(
          exchange,
          201,
          "{\"id\":\""
              + UUID.randomUUID()
              + "\",\"paymentUrl\":\"http://pay.stub/checkout\",\"status\":\"REQUIRES_PAYMENT\"}");
    }

    private String cartJson() {
      String items =
          cartItems.stream()
              .map(
                  item ->
                      "{\"skuId\":\"" + item.get("skuId") + "\",\"qty\":" + item.get("qty") + "}")
              .collect(Collectors.joining(","));
      return "{\"owner\":\"u:stub\",\"items\":[" + items + "],\"totalQty\":1,\"ttlSeconds\":100}";
    }

    private String skuJson(HttpExchange exchange) {
      String query = exchange.getRequestURI().getQuery();
      String raw = query == null ? "" : query.replaceFirst("^ids=", "");
      List<String> ids = raw.isBlank() ? List.of() : List.of(raw.split(","));
      String quoted = ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
      if (skuMissing) {
        return "{\"items\":[],\"missingIds\":[" + quoted + "]}";
      }
      String items =
          ids.stream()
              .map(
                  id ->
                      "{\"skuId\":\""
                          + id
                          + "\",\"productId\":\""
                          + UUID.randomUUID()
                          + "\",\"skuCode\":\"STUB-1\",\"title\":\"Stub item\",\"unitPrice\":"
                          + unitPrice.toPlainString()
                          + "}")
              .collect(Collectors.joining(","));
      return "{\"items\":[" + items + "],\"missingIds\":[]}";
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
      byte[] payload = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, payload.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(payload);
      }
    }
  }
}
