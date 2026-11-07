package com.shoplab.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.shoplab.payment.testsupport.TokenMinter;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của payment-service — Postgres thật (Testcontainers).
 *
 * <p>Kiểm những thứ chỉ DB + HTTP thật chứng minh được: {@code ON CONFLICT} của event webhook chặn
 * xử lý 2 lần (kể cả 20 request song song), UNIQUE {@code provider_txn_id}, CHECK bất biến {@code
 * SUCCEEDED ⇔ paid_at}, và chữ ký HMAC trên raw body qua đúng tầng HTTP.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      "shoplab.payment.webhook-secret=" + PaymentServiceApplicationIT.SECRET,
      "shoplab.payment.mock-enabled=true",
      "shoplab.payment.mock-latency=0ms",
      "spring.datasource.hikari.maximum-pool-size=20"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentServiceApplicationIT {

  static final String SECRET = "it-webhook-secret";

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);
  private static final UUID BUYER = UUID.fromString("44444444-4444-4444-8444-000000000001");
  private static final UUID OTHER = UUID.fromString("44444444-4444-4444-8444-000000000002");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient http =
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
  // Schema + bất biến
  // ==========================================================================

  @Test
  @DisplayName("Flyway tạo đủ bảng + DB chặn payment sai bất biến (CHECK)")
  void flywayAndInvariants() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);
    assertThat(tables).contains("payments", "payment_webhook_events", "idempotency_keys");

    UUID id = UUID.randomUUID();
    Throwable badAmount =
        catchThrowable(
            () ->
                jdbc.update(
                    """
                    insert into payments (id, order_id, user_id, amount, currency, status, provider,
                                          provider_intent_id, idempotency_key, created_at, updated_at)
                    values (?, ?, ?, 0.00, 'VND', 'REQUIRES_PAYMENT', 'mock', ?, 'k', now(), now())
                    """,
                    id,
                    UUID.randomUUID(),
                    BUYER,
                    "pi_zero_" + id));
    assertThat(badAmount).isInstanceOf(DataIntegrityViolationException.class);

    seedPayment("pi_it_check", new BigDecimal("1000.00"), "REQUIRES_PAYMENT");
    Throwable missingPaidAt =
        catchThrowable(
            () ->
                jdbc.update(
                    "update payments set status = 'SUCCEEDED' where provider_intent_id = ?",
                    "pi_it_check"));
    assertThat(missingPaidAt).isInstanceOf(DataIntegrityViolationException.class);
  }

  // ==========================================================================
  // Tạo payment intent + idempotency
  // ==========================================================================

  @Test
  @DisplayName("tạo intent → 201; retry cùng key → 200 cùng id + Idempotent-Replayed")
  void createThenReplay() throws Exception {
    UUID orderId = UUID.randomUUID();
    String key = "k-" + UUID.randomUUID();
    String body = intentBody(orderId, "180000.00");

    HttpResponse<String> first = postIntent(BUYER, key, body);
    assertThat(first.statusCode()).isEqualTo(201);
    JsonNode created = objectMapper.readTree(first.body());
    assertThat(created.get("status").asString()).isEqualTo("REQUIRES_PAYMENT");
    assertThat(created.get("paymentUrl").asString()).contains("/mock-provider/pay/pi_mock_");

    HttpResponse<String> retry = postIntent(BUYER, key, body);
    assertThat(retry.statusCode()).isEqualTo(200);
    assertThat(retry.headers().firstValue("Idempotent-Replayed")).contains("true");
    assertThat(objectMapper.readTree(retry.body()).get("id").asString())
        .isEqualTo(created.get("id").asString());

    assertThat(countPayments(orderId)).isEqualTo(1);
  }

  @Test
  @DisplayName("cùng key payload khác → 422; thiếu header → 400")
  void keyReuseAndMissingHeader() throws Exception {
    UUID orderId = UUID.randomUUID();
    String key = "k-" + UUID.randomUUID();
    assertThat(postIntent(BUYER, key, intentBody(orderId, "180000.00")).statusCode())
        .isEqualTo(201);

    assertThat(postIntent(BUYER, key, intentBody(orderId, "170000.00")).statusCode())
        .isEqualTo(422);

    HttpResponse<String> noHeader =
        send(
            HttpMethod.POST,
            "/v1/payment-intents",
            intentBody(orderId, "180000.00"),
            Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token(BUYER)));
    assertThat(noHeader.statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("đơn đã có intent: cùng tiền → trả intent cũ (200), khác tiền → 409")
  void oneLiveIntentPerOrder() throws Exception {
    UUID orderId = UUID.randomUUID();
    HttpResponse<String> first =
        postIntent(BUYER, "k-" + UUID.randomUUID(), intentBody(orderId, "180000.00"));
    assertThat(first.statusCode()).isEqualTo(201);
    String firstId = objectMapper.readTree(first.body()).get("id").asString();

    HttpResponse<String> same =
        postIntent(BUYER, "k-" + UUID.randomUUID(), intentBody(orderId, "180000.00"));
    assertThat(same.statusCode()).isEqualTo(200);
    assertThat(objectMapper.readTree(same.body()).get("id").asString()).isEqualTo(firstId);

    HttpResponse<String> different =
        postIntent(BUYER, "k-" + UUID.randomUUID(), intentBody(orderId, "99000.00"));
    assertThat(different.statusCode()).isEqualTo(409);
    assertThat(countPayments(orderId)).isEqualTo(1);
  }

  @Test
  @DisplayName("intent của người khác → 404; chủ sở hữu đọc được")
  void otherUsersIntentIsHidden() throws Exception {
    UUID orderId = UUID.randomUUID();
    HttpResponse<String> created =
        postIntent(BUYER, "k-" + UUID.randomUUID(), intentBody(orderId, "150000.00"));
    String id = objectMapper.readTree(created.body()).get("id").asString();

    assertThat(getIntent(id, BUYER).statusCode()).isEqualTo(200);
    assertThat(getIntent(id, OTHER).statusCode()).isEqualTo(404);
  }

  @Test
  @DisplayName("không token → 401 với ApiError (service tự verify JWT, không tin gateway)")
  void missingTokenIs401() throws Exception {
    HttpResponse<String> response =
        send(HttpMethod.GET, "/v1/payment-intents/" + UUID.randomUUID(), null, Map.of());

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(objectMapper.readTree(response.body()).get("code").asString())
        .isEqualTo("UNAUTHORIZED");
  }

  // ==========================================================================
  // Webhook
  // ==========================================================================

  @Test
  @DisplayName("webhook succeeded → payment SUCCEEDED + provider_txn_id + paid_at")
  void webhookSucceeds() throws Exception {
    PayIntent intent = createIntent("250000.00");
    String eventId = "evt_it_" + UUID.randomUUID();
    String txn = "txn_it_" + UUID.randomUUID();

    HttpResponse<String> response =
        postWebhook(webhookBody(eventId, intent.providerIntentId, txn, "succeeded", "250000.00"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(objectMapper.readTree(response.body()).get("applied").asBoolean()).isTrue();
    Map<String, Object> row =
        jdbc.queryForMap(
            "select status, provider_txn_id, paid_at from payments where id = ?::uuid", intent.id);
    assertThat(row.get("status")).isEqualTo("SUCCEEDED");
    assertThat(row.get("provider_txn_id")).isEqualTo(txn);
    assertThat(row.get("paid_at")).isNotNull();
    assertThat(countEvents(eventId)).isEqualTo(1);
  }

  @Test
  @DisplayName("provider gửi lại 5 lần cùng event id → 1 row, duplicate=true, không đổi gì")
  void webhookReplayIsIdempotent() throws Exception {
    PayIntent intent = createIntent("300000.00");
    String eventId = "evt_it_" + UUID.randomUUID();
    String body =
        webhookBody(eventId, intent.providerIntentId, "txn_it_replay", "succeeded", "300000.00");

    for (int i = 0; i < 5; i++) {
      HttpResponse<String> response = postWebhook(body);
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(objectMapper.readTree(response.body()).get("duplicate").asBoolean())
          .isEqualTo(i > 0);
    }

    assertThat(countEvents(eventId)).isEqualTo(1);
    assertThat(statusOf(intent.id)).isEqualTo("SUCCEEDED");
  }

  @Test
  @DisplayName("20 webhook song song cùng event id → đúng 1 row (ON CONFLICT + unique)")
  void concurrentWebhooksApplyOnce() throws Exception {
    PayIntent intent = createIntent("400000.00");
    String eventId = "evt_it_" + UUID.randomUUID();
    String body =
        webhookBody(eventId, intent.providerIntentId, "txn_it_race", "succeeded", "400000.00");

    int threads = 20;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return postWebhook(body).statusCode();
              }));
    }
    start.countDown();
    Map<Integer, Integer> codes = new HashMap<>();
    for (Future<Integer> future : futures) {
      codes.merge(future.get(30, TimeUnit.SECONDS), 1, Integer::sum);
    }
    pool.shutdown();

    assertThat(codes.keySet()).containsExactly(200);
    assertThat(codes.get(200)).isEqualTo(threads);
    assertThat(countEvents(eventId)).isEqualTo(1);
    assertThat(statusOf(intent.id)).isEqualTo("SUCCEEDED");
  }

  @Test
  @DisplayName("chữ ký sai → 401; timestamp cũ → 401; không ghi gì")
  void badSignatureAndStaleTimestamp() throws Exception {
    PayIntent intent = createIntent("120000.00");
    String eventId = "evt_it_" + UUID.randomUUID();
    String body =
        webhookBody(eventId, intent.providerIntentId, "txn_it_bad", "succeeded", "120000.00");

    HttpResponse<String> badSignature =
        send(
            HttpMethod.POST,
            "/v1/webhooks/payment",
            body,
            Map.of(
                "X-Provider-Signature",
                "sha256=deadbeef",
                "X-Provider-Timestamp",
                Long.toString(Instant.now().getEpochSecond())));
    assertThat(badSignature.statusCode()).isEqualTo(401);

    String oldTs = Long.toString(Instant.now().minus(Duration.ofMinutes(30)).getEpochSecond());
    HttpResponse<String> stale =
        send(
            HttpMethod.POST,
            "/v1/webhooks/payment",
            body,
            Map.of("X-Provider-Signature", sign(oldTs, body), "X-Provider-Timestamp", oldTs));
    assertThat(stale.statusCode()).isEqualTo(401);

    assertThat(countEvents(eventId)).isZero();
    assertThat(statusOf(intent.id)).isEqualTo("REQUIRES_PAYMENT");
  }

  @Test
  @DisplayName("webhook báo số tiền khác intent → 409, trạng thái không đổi")
  void amountMismatchRejected() throws Exception {
    PayIntent intent = createIntent("60000.00");
    String body =
        webhookBody(
            "evt_it_" + UUID.randomUUID(),
            intent.providerIntentId,
            "txn_it_wrong",
            "succeeded",
            "1.00");

    assertThat(postWebhook(body).statusCode()).isEqualTo(409);
    assertThat(statusOf(intent.id)).isEqualTo("REQUIRES_PAYMENT");
  }

  // ==========================================================================
  // Mock provider
  // ==========================================================================

  @Test
  @DisplayName("mock-pay → SUCCEEDED; mock-pay?fail=true → FAILED")
  void mockProviderDrivesWebhook() throws Exception {
    PayIntent ok = createIntent("50000.00");
    HttpResponse<String> paid =
        send(HttpMethod.POST, "/mock-provider/pay/" + ok.providerIntentId, "", Map.of());
    assertThat(paid.statusCode()).isEqualTo(200);
    assertThat(statusOf(ok.id)).isEqualTo("SUCCEEDED");

    PayIntent failed = createIntent("51000.00");
    HttpResponse<String> rejected =
        send(
            HttpMethod.POST,
            "/mock-provider/pay/" + failed.providerIntentId + "?fail=true",
            "",
            Map.of());
    assertThat(rejected.statusCode()).isEqualTo(200);
    Map<String, Object> row =
        jdbc.queryForMap("select status, paid_at from payments where id = ?::uuid", failed.id);
    assertThat(row.get("status")).isEqualTo("FAILED");
    assertThat(row.get("paid_at")).isNull();
  }

  // ==========================================================================
  // helpers
  // ==========================================================================

  private record PayIntent(String id, String providerIntentId) {}

  private PayIntent createIntent(String amount) throws Exception {
    UUID orderId = UUID.randomUUID();
    HttpResponse<String> response =
        postIntent(BUYER, "k-" + UUID.randomUUID(), intentBody(orderId, amount));
    assertThat(response.statusCode()).isEqualTo(201);
    JsonNode json = objectMapper.readTree(response.body());
    String paymentUrl = json.get("paymentUrl").asString();
    return new PayIntent(
        json.get("id").asString(), paymentUrl.substring(paymentUrl.lastIndexOf('/') + 1));
  }

  private static String intentBody(UUID orderId, String amount) {
    return "{\"orderId\":\"" + orderId + "\",\"amount\":" + amount + ",\"currency\":\"VND\"}";
  }

  private static String webhookBody(
      String eventId, String providerIntentId, String txn, String status, String amount) {
    return "{\"eventId\":\""
        + eventId
        + "\",\"providerIntentId\":\""
        + providerIntentId
        + "\",\"providerTxnId\":\""
        + txn
        + "\",\"status\":\""
        + status
        + "\",\"amount\":"
        + amount
        + ",\"currency\":\"VND\"}";
  }

  private HttpResponse<String> postIntent(UUID user, String key, String body) throws Exception {
    return send(
        HttpMethod.POST,
        "/v1/payment-intents",
        body,
        Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token(user), "Idempotency-Key", key));
  }

  private HttpResponse<String> getIntent(String id, UUID user) throws Exception {
    return send(
        HttpMethod.GET,
        "/v1/payment-intents/" + id,
        null,
        Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token(user)));
  }

  private HttpResponse<String> postWebhook(String body) throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    return send(
        HttpMethod.POST,
        "/v1/webhooks/payment",
        body,
        Map.of("X-Provider-Signature", sign(timestamp, body), "X-Provider-Timestamp", timestamp));
  }

  private HttpResponse<String> send(
      HttpMethod method, String path, String body, Map<String, String> headers) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10));
    headers.forEach(builder::header);
    if (body == null) {
      builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
    } else {
      builder
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .method(method.name(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    }
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Ký độc lập với service (không dùng code của nó) — nếu không thì test tự chứng minh chính nó.
   */
  private static String sign(String timestamp, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return "sha256="
        + HexFormat.of()
            .formatHex(mac.doFinal((timestamp + "\n" + body).getBytes(StandardCharsets.UTF_8)));
  }

  private String token(UUID userId) {
    return MINTER.token(userId, "BUYER");
  }

  private int countPayments(UUID orderId) {
    return jdbc.queryForObject(
        "select count(*) from payments where order_id = ?::uuid",
        Integer.class,
        orderId.toString());
  }

  private int countEvents(String eventId) {
    return jdbc.queryForObject(
        "select count(*) from payment_webhook_events where provider_event_id = ?",
        Integer.class,
        eventId);
  }

  private String statusOf(String paymentId) {
    return jdbc.queryForObject(
        "select status from payments where id = ?::uuid", String.class, paymentId);
  }

  private void seedPayment(String providerIntentId, BigDecimal amount, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        insert into payments (id, order_id, user_id, amount, currency, status, provider,
                              provider_intent_id, idempotency_key, created_at, updated_at)
        values (?, ?, ?, ?, 'VND', ?, 'mock', ?, 'seed', now(), now())
        """,
        id,
        UUID.randomUUID(),
        BUYER,
        amount,
        status,
        providerIntentId);
  }
}
