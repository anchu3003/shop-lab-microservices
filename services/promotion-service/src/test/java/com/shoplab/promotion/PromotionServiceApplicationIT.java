package com.shoplab.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.shoplab.promotion.testsupport.TokenMinter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
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
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của promotion-service — Postgres thật (Testcontainers).
 *
 * <p>Kiểm những thứ chỉ DB thật mới chứng minh được: conditional UPDATE chặn bán quá quota, bất
 * biến {@code used + locked <= quota} do CHECK canh, sweeper chạy thật, và 100 request song song
 * cùng voucher.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      // Sweeper chạy nhịp ngắn để kiểm đường scheduled thật.
      "shoplab.promotion.sweep-interval-ms=300",
      "shoplab.promotion.sweep-initial-delay-ms=200",
      // 100 request song song cần đủ connection.
      "spring.datasource.hikari.maximum-pool-size=20"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PromotionServiceApplicationIT {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;

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
  // Schema + bất biến quota
  // ==========================================================================

  @Test
  @DisplayName("Flyway tạo đủ bảng + CHECK bất biến quota (used + locked <= quota)")
  void flywayMigratedSchema() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);
    assertThat(tables).contains("vouchers", "voucher_locks", "flyway_schema_history");

    List<String> scripts =
        jdbc.queryForList("select script from flyway_schema_history", String.class);
    assertThat(scripts).contains("V1__create_promotion.sql");
  }

  @Test
  @DisplayName("DB chặn vượt quota: set locked vượt quota → vi phạm CHECK")
  void databaseRejectsOverQuota() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        insert into vouchers (id, code, type, value, max_discount, min_order, quota, used, locked,
                            start_at, end_at, created_at)
        values (?, ?, 'FIXED', 10000, null, 0, 1, 0, 0, now() - interval '1 hour',
                now() + interval '1 hour', now())
        """,
        id,
        "QUOTA-" + id.toString().substring(0, 8));

    Throwable thrown =
        catchThrowable(
            () -> jdbc.update("update vouchers set locked = locked + 2 where id = ?", id));
    assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
  }

  // ==========================================================================
  // Security + CRUD
  // ==========================================================================

  @Test
  @DisplayName("Không token → 401; buyer tạo voucher → 403; admin → 201")
  void securityOnVoucherEndpoints() {
    Map<String, Object> body =
        voucherBody("SEC-" + UUID.randomUUID().toString().substring(0, 8), "10", null, 5, "0");

    assertThat(send(HttpMethod.POST, "/v1/vouchers", null, body).status()).isEqualTo(401);
    assertThat(send(HttpMethod.POST, "/v1/vouchers", buyerToken(), body).status()).isEqualTo(403);
    assertThat(send(HttpMethod.POST, "/v1/vouchers", adminToken(), body).status()).isEqualTo(201);
  }

  @Test
  @DisplayName("Tạo voucher: mã chuẩn hoá UPPER, trùng mã → 409, PERCENT > 100 → 400")
  void createVoucherRules() {
    String code = "sale-" + UUID.randomUUID().toString().substring(0, 8);
    HttpResult created =
        send(
            HttpMethod.POST,
            "/v1/vouchers",
            adminToken(),
            voucherBody(code, "10", "50000", 5, "0"));
    assertThat(created.status()).isEqualTo(201);
    assertThat(created.body())
        .contains("\"code\":\"" + code.toUpperCase() + "\"")
        .contains("\"available\":5");

    HttpResult duplicate =
        send(HttpMethod.POST, "/v1/vouchers", adminToken(), voucherBody(code, "10", null, 5, "0"));
    assertThat(duplicate.status()).isEqualTo(409);

    HttpResult invalidPercent =
        send(
            HttpMethod.POST,
            "/v1/vouchers",
            adminToken(),
            voucherBody(
                "BAD-" + UUID.randomUUID().toString().substring(0, 8), "150", null, 5, "0"));
    assertThat(invalidPercent.status()).isEqualTo(400);
    assertThat(invalidPercent.body()).contains("VALIDATION_ERROR");
  }

  // ==========================================================================
  // lock / commit / release
  // ==========================================================================

  @Test
  @DisplayName("lock: 201 + discount theo subtotal, voucher locked=1 available giảm")
  void lockHoldsQuota() {
    String code = createVoucher("10", "50000", 5, "0");
    UUID orderId = UUID.randomUUID();

    HttpResult locked = lock(code, orderId, "200000.00");

    assertThat(locked.status()).isEqualTo(201);
    assertThat(locked.body())
        .contains("\"discount\":20000.00")
        .contains("\"finalTotal\":180000.00")
        .contains("\"status\":\"LOCKED\"");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 1, 5}); // used, locked, quota
  }

  @Test
  @DisplayName("lock cùng orderId lần 2 → 200 + CÙNG lockId, locked KHÔNG tăng")
  void lockIsIdempotentByOrderId() {
    String code = createVoucher("10", null, 5, "0");
    UUID orderId = UUID.randomUUID();

    HttpResult first = lock(code, orderId, "100000.00");
    HttpResult second = lock(code, orderId, "100000.00");

    assertThat(first.status()).isEqualTo(201);
    assertThat(second.status()).isEqualTo(200);
    assertThat(field(second.body(), "lockId")).isEqualTo(field(first.body(), "lockId"));
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 1, 5});
    assertThat(countLocks(orderId)).isEqualTo(1);
  }

  @Test
  @DisplayName("lock: đơn dưới minOrder → 409, không chiếm lượt; hết lượt → 409")
  void lockRejectsWhenNotUsable() {
    String code = createVoucher("10", null, 1, "150000");
    assertThat(lock(code, UUID.randomUUID(), "100000.00").status()).isEqualTo(409);

    String small = createVoucher("10", null, 1, "0");
    assertThat(lock(small, UUID.randomUUID(), "100000.00").status()).isEqualTo(201);
    HttpResult exhausted = lock(small, UUID.randomUUID(), "100000.00");
    assertThat(exhausted.status()).isEqualTo(409);
    assertThat(exhausted.body()).contains("CONFLICT");
    assertThat(voucherCounters(small)).isEqualTo(new int[] {0, 1, 1});
  }

  @Test
  @DisplayName("commit: used+1 locked-1; commit lần 2 idempotent; release sau commit → 409")
  void commitConsumesQuota() {
    String code = createVoucher("10", null, 5, "0");
    String lockId = field(lock(code, UUID.randomUUID(), "100000.00").body(), "lockId");

    HttpResult committed = post("/v1/vouchers/commit", Map.of("lockId", lockId));
    assertThat(committed.status()).isEqualTo(200);
    assertThat(committed.body()).contains("\"status\":\"COMMITTED\"");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {1, 0, 5});

    assertThat(post("/v1/vouchers/commit", Map.of("lockId", lockId)).status()).isEqualTo(200);
    assertThat(voucherCounters(code)).isEqualTo(new int[] {1, 0, 5}); // không dùng 2 lượt

    assertThat(post("/v1/vouchers/release", Map.of("lockId", lockId)).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("release: locked-1, used không đổi; lần 2 idempotent")
  void releaseReturnsQuota() {
    String code = createVoucher("20", null, 5, "0");
    String lockId = field(lock(code, UUID.randomUUID(), "100000.00").body(), "lockId");

    HttpResult released = post("/v1/vouchers/release", Map.of("lockId", lockId));
    assertThat(released.status()).isEqualTo(200);
    assertThat(released.body()).contains("\"status\":\"RELEASED\"");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 0, 5});

    assertThat(post("/v1/vouchers/release", Map.of("lockId", lockId)).status()).isEqualTo(200);
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 0, 5});
  }

  @Test
  @DisplayName("commit lock quá TTL → 410 + trả lượt ngay (status EXPIRED)")
  void commitAfterExpiryReturnsGone() {
    String code = createVoucher("10", null, 5, "0");
    String lockId = field(lock(code, UUID.randomUUID(), "100000.00").body(), "lockId");
    jdbc.update(
        "update voucher_locks set expires_at = now() - interval '1 minute' where id = ?::uuid",
        lockId);

    HttpResult result = post("/v1/vouchers/commit", Map.of("lockId", lockId));

    assertThat(result.status()).isEqualTo(410);
    assertThat(result.body()).contains("GONE");
    assertThat(lockStatus(lockId)).isEqualTo("EXPIRED");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 0, 5});
  }

  // ==========================================================================
  // SL-2.4: sweeper + 100 request đồng thời
  // ==========================================================================

  @Test
  @DisplayName("sweeper (job thật) trả lượt cho lock quá TTL")
  void sweeperReleasesExpiredLocks() throws Exception {
    String code = createVoucher("10", null, 5, "0");
    String lockId = field(lock(code, UUID.randomUUID(), "100000.00").body(), "lockId");
    jdbc.update(
        "update voucher_locks set expires_at = now() - interval '1 minute' where id = ?::uuid",
        lockId);

    awaitUntil(() -> voucherCounters(code)[1] == 0, Duration.ofSeconds(15));

    assertThat(lockStatus(lockId)).isEqualTo("EXPIRED");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 0, 5});
  }

  @Test
  @DisplayName("sweeper KHÔNG đụng lock còn hạn")
  void sweeperLeavesActiveLocksAlone() throws Exception {
    String code = createVoucher("10", null, 5, "0");
    String lockId = field(lock(code, UUID.randomUUID(), "100000.00").body(), "lockId");

    Thread.sleep(1_500); // >= 3 nhịp sweeper

    assertThat(lockStatus(lockId)).isEqualTo("LOCKED");
    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 1, 5});
  }

  @Test
  @DisplayName(
      "100 request song song cùng voucher (quota 10) ⇒ đúng 10×201 / 90×409, không vượt quota")
  void concurrentLockNeverExceedsQuota() throws Exception {
    String code = createVoucher("10", null, 10, "0");

    int requests = 100;
    ExecutorService pool = Executors.newFixedThreadPool(20);
    CountDownLatch startTogether = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < requests; i++) {
        UUID orderId = UUID.randomUUID();
        futures.add(
            pool.submit(
                () -> {
                  startTogether.await();
                  return lock(code, orderId, "100000.00").status();
                }));
      }
      startTogether.countDown();

      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> future : futures) {
        statuses.add(future.get(30, TimeUnit.SECONDS));
      }

      long created = statuses.stream().filter(status -> status == 201).count();
      long rejected = statuses.stream().filter(status -> status == 409).count();
      assertThat(created).as("số lock được cấp phải bằng quota").isEqualTo(10);
      assertThat(rejected).as("phần còn lại bị từ chối 409").isEqualTo(90);
    } finally {
      pool.shutdownNow();
    }

    assertThat(voucherCounters(code)).isEqualTo(new int[] {0, 10, 10});
    assertThat(
            jdbc.queryForObject(
                "select count(*) from voucher_locks where status = 'LOCKED' and voucher_id ="
                    + " (select id from vouchers where code = ?)",
                Integer.class,
                code))
        .isEqualTo(10);
  }

  // ==========================================================================
  // Helpers
  // ==========================================================================

  private String adminToken() {
    return MINTER.token(UUID.randomUUID(), "ADMIN");
  }

  private String buyerToken() {
    return MINTER.token(UUID.randomUUID(), "BUYER");
  }

  /** Body tạo voucher; {@code maxDiscount} null = không chặn trần. */
  private Map<String, Object> voucherBody(
      String code, String value, String maxDiscount, int quota, String minOrder) {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("code", code);
    body.put("type", "PERCENT");
    body.put("value", new BigDecimal(value));
    body.put("quota", quota);
    body.put("minOrder", new BigDecimal(minOrder));
    body.put("startAt", Instant.now().minusSeconds(60).toString());
    body.put("endAt", Instant.now().plusSeconds(3600).toString());
    if (maxDiscount != null) {
      body.put("maxDiscount", new BigDecimal(maxDiscount));
    }
    return body;
  }

  /** Tạo voucher qua API admin, trả mã đã UPPER hoá. */
  private String createVoucher(String value, String maxDiscount, int quota, String minOrder) {
    String code = "V" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    HttpResult created =
        send(
            HttpMethod.POST,
            "/v1/vouchers",
            adminToken(),
            voucherBody(code, value, maxDiscount, quota, minOrder));
    assertThat(created.status()).as("tạo voucher %s", code).isEqualTo(201);
    return code;
  }

  private HttpResult lock(String code, UUID orderId, String subtotal) {
    return send(
        HttpMethod.POST,
        "/v1/vouchers/lock",
        buyerToken(),
        Map.of("code", code, "orderId", orderId, "subtotal", new BigDecimal(subtotal)));
  }

  private HttpResult post(String path, Map<String, Object> body) {
    return send(HttpMethod.POST, path, buyerToken(), body);
  }

  /** (used, locked, quota) — đọc trực tiếp DB để assert kế toán, không tin response. */
  private int[] voucherCounters(String code) {
    return jdbc.queryForObject(
        "select used, locked, quota from vouchers where code = ?",
        (rs, rowNum) -> new int[] {rs.getInt("used"), rs.getInt("locked"), rs.getInt("quota")},
        code);
  }

  private int countLocks(UUID orderId) {
    return jdbc.queryForObject(
        "select count(*) from voucher_locks where order_id = ?", Integer.class, orderId);
  }

  private String lockStatus(String lockId) {
    return jdbc.queryForObject(
        "select status from voucher_locks where id = ?::uuid", String.class, lockId);
  }

  private void awaitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(200);
    }
    assertThat(condition.getAsBoolean()).as("điều kiện chưa đạt sau %s", timeout).isTrue();
  }

  private String field(String json, String name) {
    return String.valueOf(parsed(json).get(name));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parsed(String json) {
    return objectMapper.readValue(json, Map.class);
  }

  private HttpResult send(HttpMethod method, String path, String token, Object body) {
    String payload = body == null ? "" : objectMapper.writeValueAsString(body);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10));
    if (token != null) {
      builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
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
      return new HttpResult(response.statusCode(), response.body());
    } catch (Exception ex) {
      throw new IllegalStateException("HTTP call failed: " + method + " " + path, ex);
    }
  }

  private record HttpResult(int status, String body) {}
}
