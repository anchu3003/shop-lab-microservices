package com.shoplab.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shoplab.commons.events.EventPublishException;
import com.shoplab.commons.events.EventPublisher;
import com.shoplab.order.idempotency.IdempotencyService;
import com.shoplab.order.outbox.CheckoutOutbox;
import com.shoplab.order.outbox.OutboxRelay;
import com.shoplab.order.testsupport.TokenMinter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của order-service — Postgres thật (Testcontainers).
 *
 * <p>Kiểm những thứ chỉ DB thật chứng minh được: {@code ON CONFLICT} của idempotency chặn 2 đơn cho
 * cùng key, snapshot được lưu đúng, bất biến `total = subtotal - discount` do CHECK canh, và state
 * machine không cho nhảy cóc.
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
      "shoplab.events.consumer-auto-startup=false",
      // SL-4.0: IT tự gọi `relay.drain()` khi cần ⇒ tắt nhịp nền (mặc định 1 s) để test retention
      // kiểm được đúng trạng thái PENDING thay vì bị relay publish chen giữa. Đây cũng là cách khử
      // một nguồn nondeterminism có sẵn của class này.
      "shoplab.outbox.interval=1h"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderServiceApplicationIT {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private IdempotencyService idempotencyService;
  @Autowired private CheckoutOutbox finalizer;
  @Autowired private OutboxRelay relay;
  @Autowired private com.shoplab.order.retention.RetentionSweeper retentionSweeper;

  /** Stub: IT chạy Postgres thật nhưng KHÔNG có RabbitMQ ⇒ thay publisher bằng mock. */
  @MockitoBean private EventPublisher outboxPublisher;

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
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  // ==========================================================================
  // Schema + bất biến tiền
  // ==========================================================================

  @Test
  @DisplayName("Flyway tạo đủ bảng + DB chặn hoá đơn cộng sai tiền (CHECK)")
  void flywayAndMoneyInvariants() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);
    assertThat(tables)
        .contains("orders", "order_items", "order_status_history", "idempotency_keys");

    UUID orderId = seedOrder();
    Throwable badLineTotal =
        catchThrowable(
            () ->
                jdbc.update(
                    """
                    insert into order_items (id, order_id, sku_id, sku_code, title, unit_price,
                                            currency, qty, line_total)
                    values (?, ?, ?, 'S', 'T', 1000.00, 'VND', 2, 9999.00)
                    """,
                    UUID.randomUUID(),
                    orderId,
                    UUID.randomUUID()));
    assertThat(badLineTotal).isInstanceOf(DataIntegrityViolationException.class);

    Throwable badTotal =
        catchThrowable(
            () -> jdbc.update("update orders set total = subtotal + 100 where id = ?", orderId));
    assertThat(badTotal).isInstanceOf(DataIntegrityViolationException.class);
  }

  // ==========================================================================
  // Tạo đơn + idempotency
  // ==========================================================================

  @Test
  @DisplayName("tạo đơn → 201, snapshot đúng, có history, idempotency row COMPLETED")
  void createShouldPersistSnapshotAndHistory() {
    String key = "k-" + UUID.randomUUID();
    HttpResult created = createOrder(buyerToken(), key, orderBody("100000.00", 2, "20000"));

    assertThat(created.status()).isEqualTo(201);
    String orderId = field(created.body(), "id");
    assertThat(created.body())
        .contains("\"status\":\"PENDING_PAYMENT\"")
        .contains("\"subtotal\":200000.00")
        .contains("\"total\":180000.00")
        .contains("\"lineTotal\":200000.00");

    Map<String, Object> item =
        jdbc.queryForMap(
            "select sku_code, title, unit_price, qty, line_total from order_items where order_id = ?::uuid",
            orderId);
    assertThat(item.get("sku_code")).isEqualTo("SKU-IT-1");
    assertThat(item.get("title")).isEqualTo("Áo thun IT");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from order_status_history where order_id = ?::uuid and from_status is null"
                    + " and to_status = 'PENDING_PAYMENT'",
                Integer.class,
                orderId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select status from idempotency_keys where key = ?", String.class, key))
        .isEqualTo("COMPLETED");
  }

  @Test
  @DisplayName(
      "retry CÙNG key + CÙNG payload → 200, cùng orderId, header Idempotent-Replayed, chỉ 1 đơn")
  void retryShouldReplay() {
    String token = buyerToken(); // CÙNG user: scope idempotency là "<userId>:POST /v1/orders"
    String key = "k-" + UUID.randomUUID();
    Map<String, Object> body = orderBody("100000.00", 1, null);

    HttpResult first = createOrder(token, key, body);
    HttpResult second = createOrder(token, key, body);

    assertThat(first.status()).isEqualTo(201);
    assertThat(second.status()).isEqualTo(200);
    assertThat(second.headers().firstValue("Idempotent-Replayed").orElse(null)).isEqualTo("true");
    assertThat(field(second.body(), "id")).isEqualTo(field(first.body(), "id"));
    assertThat(countOrdersByKey(key)).isEqualTo(1); // DB có 1 đơn cho key này, không phải 2
  }

  @Test
  @DisplayName("cùng key nhưng payload khác → 422 (client phải sửa request)")
  void sameKeyDifferentPayloadIs422() {
    String token = buyerToken();
    String key = "k-" + UUID.randomUUID();
    assertThat(createOrder(token, key, orderBody("100000.00", 1, null)).status()).isEqualTo(201);

    HttpResult reuse = createOrder(token, key, orderBody("100000.00", 2, null));

    assertThat(reuse.status()).isEqualTo(422);
    assertThat(reuse.body()).contains("UNPROCESSABLE_ENTITY");
  }

  @Test
  @DisplayName("row đang IN_PROGRESS cùng key → 409 (không xử lý song song); thiếu header → 400")
  void inProgressAndMissingKey() throws Exception {
    UUID userId = UUID.randomUUID();
    String token = MINTER.token(userId, "BUYER");
    String key = "k-" + UUID.randomUUID();
    Map<String, Object> body = orderBody("100000.00", 1, null);

    // Tạo thật 1 lần để row có ĐÚNG request_hash của payload này, rồi lật về IN_PROGRESS
    // (mô phỏng process chết giữa chừng). Hash sai thì sẽ là 422 chứ không phải 409.
    assertThat(createOrder(token, key, body).status()).isEqualTo(201);
    jdbc.update(
        "update idempotency_keys set status = 'IN_PROGRESS', completed_at = null where key = ?",
        key);

    HttpResult conflict = createOrder(token, key, body);
    assertThat(conflict.status()).isEqualTo(409);

    HttpResult missing =
        send(HttpMethod.POST, "/v1/orders", token, Map.of(), orderBody("100000.00", 1, null));
    assertThat(missing.status()).isEqualTo(400);
    assertThat(missing.body()).contains("Idempotency-Key");
  }

  // ==========================================================================
  // State machine + quyền
  // ==========================================================================

  @Test
  @DisplayName(
      "confirm: 200 + history; confirm lần 2 → 200 (không ghi thêm); cancel sau confirm → 409")
  void stateMachineShouldBeOneWay() {
    String orderId = createOrderId(orderBody("100000.00", 1, null));
    String token = lastToken;

    HttpResult confirmed =
        send(HttpMethod.POST, "/v1/orders/" + orderId + "/confirm", token, Map.of(), null);
    assertThat(confirmed.status()).isEqualTo(200);
    assertThat(confirmed.body()).contains("\"status\":\"CONFIRMED\"");

    assertThat(
            send(HttpMethod.POST, "/v1/orders/" + orderId + "/confirm", token, Map.of(), null)
                .status())
        .isEqualTo(200);
    assertThat(historyCount(orderId)).isEqualTo(2); // PENDING + CONFIRMED

    HttpResult cancel =
        send(
            HttpMethod.POST,
            "/v1/orders/" + orderId + "/cancel",
            token,
            Map.of(),
            Map.of("reason", "đổi ý"));
    assertThat(cancel.status()).isEqualTo(409);
  }

  @Test
  @DisplayName("cancel và expire trên đơn PENDING_PAYMENT → 200 + history có reason")
  void cancelAndExpireShouldWork() {
    String cancelled = createOrderId(orderBody("50000.00", 1, null));
    HttpResult cancel =
        send(
            HttpMethod.POST,
            "/v1/orders/" + cancelled + "/cancel",
            lastToken,
            Map.of(),
            Map.of("reason", "hết tiền"));
    assertThat(cancel.status()).isEqualTo(200);
    assertThat(cancel.body()).contains("\"status\":\"CANCELLED\"");
    assertThat(
            jdbc.queryForObject(
                "select reason from order_status_history where order_id = ?::uuid and to_status = 'CANCELLED'",
                String.class,
                cancelled))
        .isEqualTo("hết tiền");

    String expired = createOrderId(orderBody("50000.00", 1, null));
    assertThat(
            send(HttpMethod.POST, "/v1/orders/" + expired + "/expire", lastToken, Map.of(), null)
                .body())
        .contains("\"status\":\"EXPIRED\"");
  }

  @Test
  @DisplayName("đơn của buyer khác → 404; admin xem được; list của buyer chỉ có đơn mình")
  void ownershipAndListing() {
    String buyerA = buyerToken();
    String buyerB = buyerToken();
    String orderId =
        field(
            createOrder(buyerA, "k-" + UUID.randomUUID(), orderBody("30000.00", 1, null)).body(),
            "id");

    assertThat(send(HttpMethod.GET, "/v1/orders/" + orderId, buyerB, Map.of(), null).status())
        .isEqualTo(404);
    assertThat(send(HttpMethod.GET, "/v1/orders/" + orderId, adminToken(), Map.of(), null).status())
        .isEqualTo(200);

    HttpResult mine = send(HttpMethod.GET, "/v1/orders?size=100", buyerA, Map.of(), null);
    assertThat(mine.status()).isEqualTo(200);
    assertThat(mine.body()).contains(orderId);

    HttpResult otherBuyer = send(HttpMethod.GET, "/v1/orders?size=100", buyerB, Map.of(), null);
    assertThat(otherBuyer.body()).doesNotContain(orderId);
  }

  // ==========================================================================
  // Đồng thời + job dọn (TD-14)
  // ==========================================================================

  @Test
  @DisplayName("30 request song song cùng Idempotency-Key ⇒ CHỈ 1 đơn được tạo (không tạo trùng)")
  void concurrentSameKeyCreatesOneOrder() throws Exception {
    UUID userId = UUID.randomUUID();
    String token = MINTER.token(userId, "BUYER");
    String key = "k-" + UUID.randomUUID();
    Map<String, Object> body = orderBody("100000.00", 1, null);
    long before = countOrdersFor(userId);

    int requests = 30;
    ExecutorService pool = Executors.newFixedThreadPool(15);
    CountDownLatch startTogether = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < requests; i++) {
        futures.add(
            pool.submit(
                () -> {
                  startTogether.await();
                  return createOrder(token, key, body).status();
                }));
      }
      startTogether.countDown();
      for (Future<Integer> future : futures) {
        int status = future.get(30, TimeUnit.SECONDS);
        // 201 = tạo mới; 200 = replay; 409 = request khác đang chạy cùng key. Cả 3 đều ĐÚNG.
        assertThat(status).isIn(200, 201, 409);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(countOrdersFor(userId) - before)
        .as("cùng Idempotency-Key chỉ được tạo đúng 1 đơn")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("job dọn idempotency: xoá row cũ, giữ row mới (TD-14)")
  void cleanupShouldDeleteOldRowsOnly() {
    String oldKey = "old-" + UUID.randomUUID();
    String freshKey = "fresh-" + UUID.randomUUID();
    jdbc.update(
        """
        insert into idempotency_keys (scope, key, request_hash, status, created_at)
        values ('user-x:POST /v1/orders', ?, 'h', 'COMPLETED', now() - interval '48 hours'),
               ('user-x:POST /v1/orders', ?, 'h', 'COMPLETED', now())
        """,
        oldKey,
        freshKey);

    int deleted = idempotencyService.cleanup();

    assertThat(deleted).isGreaterThanOrEqualTo(1);
    assertThat(keyExists(oldKey)).isFalse();
    assertThat(keyExists(freshKey)).isTrue();
  }

  // ==========================================================================
  // SL-3.3 — transactional outbox
  // ==========================================================================

  @Test
  @DisplayName(
      "confirm ⇒ order CONFIRMED + ĐÚNG 1 row outbox PENDING (cùng transaction); relay ⇒ SENT")
  void confirmWritesOutboxInSameTransaction() {
    UUID orderId = seedOrder();

    boolean changed = finalizer.confirm(orderId, "thanh toán đã xác nhận", Instant.now());

    assertThat(changed).isTrue();
    assertThat(orderStatus(orderId)).isEqualTo("CONFIRMED");
    // Đúng 1 row: confirm#2 sẽ là idempotent ở tầng service (không tới đây) — nhưng nếu có bug gọi
    // 2
    // lần thì bất biến này bắt được.
    assertThat(countOutbox(orderId, "order.confirmed.v1")).isEqualTo(1);
    assertThat(outboxStatus(orderId)).isEqualTo("PENDING");

    int sent = relay.drain();

    assertThat(sent).isGreaterThanOrEqualTo(1);
    verify(outboxPublisher).publish(argThat(e -> e.eventType().equals("order.confirmed.v1")));
    assertThat(outboxStatus(orderId)).isEqualTo("SENT");
    assertThat(outboxSentAt(orderId)).isNotNull();
  }

  @Test
  @DisplayName(
      "broker lỗi ⇒ order VẪN CONFIRMED, row outbox giữ PENDING + attempts≥1 (không mất event)")
  void orderStaysConfirmedWhenPublishFails() {
    UUID orderId = seedOrder();
    doThrow(new EventPublishException("broker nack"))
        .when(outboxPublisher)
        .publish(any(com.shoplab.commons.events.EventEnvelope.class));

    boolean changed = finalizer.confirm(orderId, "thanh toán đã xác nhận", Instant.now());

    // Điểm cốt lõi của outbox: publish KHÔNG nằm trên đường commit của nghiệp vụ.
    assertThat(changed).isTrue();
    assertThat(orderStatus(orderId)).isEqualTo("CONFIRMED");

    relay.drain();

    assertThat(outboxStatus(orderId)).isEqualTo("PENDING");
    assertThat(outboxAttempts(orderId)).isGreaterThanOrEqualTo(1);
    assertThat(outboxLastError(orderId)).contains("broker nack");
  }

  @Test
  @DisplayName(
      "cancel ⇒ 1 row outbox `order.cancelled.v1`; 2 relay chạy song song ⇒ mỗi row publish 1 lần")
  void cancelWritesOutboxAndRelaysAreExclusive() throws Exception {
    UUID orderId = seedOrder();
    assertThat(finalizer.cancel(orderId, "đổi ý", Instant.now())).isTrue();
    assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
    assertThat(countOutbox(orderId, "order.cancelled.v1")).isEqualTo(1);

    // Claim dùng `FOR UPDATE SKIP LOCKED` + `claimed_at` ⇒ 2 relay không lấy cùng row.
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> a = pool.submit(relay::drain);
      Future<Integer> b = pool.submit(relay::drain);
      assertThat(a.get(30, TimeUnit.SECONDS) + b.get(30, TimeUnit.SECONDS)).isLessThanOrEqualTo(1);
    } finally {
      pool.shutdownNow();
    }

    verify(outboxPublisher, times(1))
        .publish(argThat(e -> e.eventType().equals("order.cancelled.v1")));
    assertThat(outboxStatus(orderId)).isEqualTo("SENT");
  }

  // ==========================================================================
  // Helpers
  // ==========================================================================

  /** Token của lần tạo đơn gần nhất — dùng cho các test cần cùng user với đơn vừa tạo. */
  private String lastToken;

  private String adminToken() {
    return MINTER.token(UUID.randomUUID(), "ADMIN");
  }

  private String buyerToken() {
    lastToken = MINTER.token(UUID.randomUUID(), "BUYER");
    return lastToken;
  }

  private Map<String, Object> orderBody(String unitPrice, int qty, String discount) {
    Map<String, Object> item = new HashMap<>();
    item.put("skuId", UUID.randomUUID());
    item.put("skuCode", "SKU-IT-1");
    item.put("title", "Áo thun IT");
    item.put("unitPrice", new BigDecimal(unitPrice));
    item.put("qty", qty);

    Map<String, Object> body = new HashMap<>();
    body.put("items", List.of(item));
    body.put("currency", "VND");
    if (discount != null) {
      body.put("discount", new BigDecimal(discount));
    }
    return body;
  }

  private HttpResult createOrder(String token, String idempotencyKey, Map<String, Object> body) {
    return send(
        HttpMethod.POST,
        "/v1/orders",
        token,
        idempotencyKey == null ? Map.of() : Map.of("Idempotency-Key", idempotencyKey),
        body);
  }

  private String createOrderId(Map<String, Object> body) {
    HttpResult created = createOrder(buyerToken(), "k-" + UUID.randomUUID(), body);
    assertThat(created.status()).isEqualTo(201);
    return field(created.body(), "id");
  }

  /** Đơn có sẵn cho test bất biến tiền (insert thẳng DB để kiểm CHECK, không qua API). */
  private UUID seedOrder() {
    UUID orderId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    jdbc.update(
        """
        insert into orders (id, user_id, status, subtotal, discount, total, currency,
                            idempotency_key, created_at, updated_at)
        values (?, ?, 'PENDING_PAYMENT', 2000.00, 0, 2000.00, 'VND', 'seed', now(), now())
        """,
        orderId,
        userId);
    return orderId;
  }

  private String orderStatus(UUID orderId) {
    return jdbc.queryForObject("select status from orders where id = ?", String.class, orderId);
  }

  private long countOutbox(UUID orderId, String eventType) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from outbox where aggregate_id = ? and event_type = ?",
            Long.class,
            orderId,
            eventType);
    return count == null ? 0 : count;
  }

  private String outboxStatus(UUID orderId) {
    return jdbc.queryForObject(
        "select status from outbox where aggregate_id = ? order by created_at desc limit 1",
        String.class,
        orderId);
  }

  private int outboxAttempts(UUID orderId) {
    Integer attempts =
        jdbc.queryForObject(
            "select attempts from outbox where aggregate_id = ? order by created_at desc limit 1",
            Integer.class,
            orderId);
    return attempts == null ? 0 : attempts;
  }

  private String outboxLastError(UUID orderId) {
    return jdbc.queryForObject(
        "select last_error from outbox where aggregate_id = ? order by created_at desc limit 1",
        String.class,
        orderId);
  }

  private java.time.Instant outboxSentAt(UUID orderId) {
    java.sql.Timestamp ts =
        jdbc.queryForObject(
            "select sent_at from outbox where aggregate_id = ? order by created_at desc limit 1",
            java.sql.Timestamp.class,
            orderId);
    return ts == null ? null : ts.toInstant();
  }

  private long countOrdersByKey(String idempotencyKey) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from orders where idempotency_key = ?", Long.class, idempotencyKey);
    return count == null ? 0 : count;
  }

  private long countOrdersFor(UUID userId) {
    Long count =
        jdbc.queryForObject("select count(*) from orders where user_id = ?", Long.class, userId);
    return count == null ? 0 : count;
  }

  private int historyCount(String orderId) {
    return jdbc.queryForObject(
        "select count(*) from order_status_history where order_id = ?::uuid",
        Integer.class,
        orderId);
  }

  private boolean keyExists(String key) {
    return jdbc.queryForObject(
            "select count(*) from idempotency_keys where key = ?", Integer.class, key)
        > 0;
  }

  private String field(String json, String name) {
    return String.valueOf(parsed(json).get(name));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parsed(String json) {
    return objectMapper.readValue(json, Map.class);
  }

  private HttpResult send(
      HttpMethod method, String path, String token, Map<String, String> extraHeaders, Object body) {
    String payload = body == null ? "" : objectMapper.writeValueAsString(body);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10));
    if (token != null) {
      builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    extraHeaders.forEach(builder::header);
    if (!payload.isEmpty()) {
      builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
    builder.method(
        method.name(),
        payload.isEmpty()
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

    try {
      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new HttpResult(response.statusCode(), response.body(), response.headers());
    } catch (Exception ex) {
      throw new IllegalStateException("HTTP call failed: " + method + " " + path, ex);
    }
  }

  // ==========================================================================
  // Retention (TD-29 + TD-30, SL-4.0)
  // ==========================================================================

  @Test
  @DisplayName("retention: xoá outbox SENT cũ + processed_events cũ; GIỮ outbox PENDING và row mới")
  void retentionDeletesOnlyOldSentRows() {
    Instant now = Instant.now();
    UUID oldSent = UUID.randomUUID();
    UUID newSent = UUID.randomUUID();
    UUID oldPending = UUID.randomUUID();
    insertOutbox(oldSent, "SENT", now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(10)));
    insertOutbox(newSent, "SENT", now.minus(Duration.ofHours(1)), now.minus(Duration.ofHours(1)));
    insertOutbox(oldPending, "PENDING", now.minus(Duration.ofDays(10)), null);

    UUID oldDedupe = UUID.randomUUID();
    UUID newDedupe = UUID.randomUUID();
    insertProcessedEvent(oldDedupe, now.minus(Duration.ofDays(10)));
    insertProcessedEvent(newDedupe, now.minus(Duration.ofHours(1)));

    retentionSweeper.sweep();

    // outbox: chỉ row SENT cũ bị xoá. PENDING cũ = event CHƯA gửi ⇒ phải còn.
    assertThat(count("select count(*) from outbox where id = ?", oldSent)).isZero();
    assertThat(count("select count(*) from outbox where id = ?", newSent)).isEqualTo(1);
    // PENDING (event CHƯA gửi) không thuộc phạm vi retention — assert cả trạng thái, không chỉ
    // "row còn tồn tại": nếu SQL quên điều kiện `status='SENT'` thì row sẽ biến mất.
    assertThat(count("select count(*) from outbox where id = ? and status = 'PENDING'", oldPending))
        .isEqualTo(1);
    // processed_events: marker cũ bị xoá, marker mới còn.
    assertThat(count("select count(*) from processed_events where event_id = ?", oldDedupe))
        .isZero();
    assertThat(count("select count(*) from processed_events where event_id = ?", newDedupe))
        .isEqualTo(1);
  }

  private void insertOutbox(UUID id, String status, Instant createdAt, Instant sentAt) {
    jdbc.update(
        """
        INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, payload, status,
                            attempts, created_at, sent_at)
        VALUES (?, 'ORDER', ?, 'order.confirmed.v1', '{}'::jsonb, ?, 1, ?, ?)
        """,
        id,
        UUID.randomUUID(),
        status,
        java.sql.Timestamp.from(createdAt),
        sentAt == null ? null : java.sql.Timestamp.from(sentAt));
  }

  private void insertProcessedEvent(UUID eventId, Instant processedAt) {
    jdbc.update(
        "INSERT INTO processed_events (event_id, consumer, event_type, processed_at) "
            + "VALUES (?, 'order-service', 'payment.succeeded.v1', ?)",
        eventId,
        java.sql.Timestamp.from(processedAt));
  }

  private long count(String sql, UUID arg) {
    Long value = jdbc.queryForObject(sql, Long.class, arg);
    return value == null ? 0 : value;
  }

  private record HttpResult(int status, String body, java.net.http.HttpHeaders headers) {}
}
