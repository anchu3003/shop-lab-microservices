package com.shoplab.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.inventory.testsupport.TokenMinter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của inventory-service — chạy trên <b>Postgres thật</b> (Testcontainers).
 *
 * <p>Đây là chỗ duy nhất kiểm được những thứ quyết định đúng/sai của bài toán oversell:
 *
 * <ul>
 *   <li>Conditional {@code UPDATE} có thật sự chặn khi hết hàng (không chỉ "logic đúng").
 *   <li>Rollback: reserve nhiều SKU mà 1 SKU hết hàng thì <b>không</b> SKU nào bị giữ chỗ.
 *   <li>CHECK constraint của DB là chốt chặn cuối.
 *   <li>Ledger đối soát khớp với {@code stock} sau cả chuỗi thao tác.
 * </ul>
 *
 * <p>Test đồng thời (100 request cùng SKU) thuộc {@code SL-2.3} — cố ý không nhồi vào đây.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"shoplab.env=local", "shoplab.debug-endpoints=true"})
@Testcontainers
class InventoryServiceApplicationIT {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  /** Decoder thay thế — {@code @Primary} để thắng bean khai trong {@code SecurityConfig}. */
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
  // Schema / hạ tầng
  // ==========================================================================

  @Test
  @DisplayName("Flyway tạo đủ schema inventory + bất biến tồn kho được DB canh bằng CHECK")
  void flywayMigratedSchema() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);
    assertThat(tables).contains("stock", "reservations", "reservation_items", "stock_ledger");

    List<String> constraints =
        jdbc.queryForList(
            "select conname from pg_constraint where conrelid in"
                + " ('stock'::regclass, 'reservations'::regclass, 'stock_ledger'::regclass)"
                + " order by conname",
            String.class);
    assertThat(constraints)
        .contains(
            "ck_stock_reserved_within_on_hand",
            "ck_stock_on_hand_non_negative",
            "ck_stock_reserved_non_negative",
            "ck_reservations_status",
            "ck_stock_ledger_reason");
  }

  @Test
  @DisplayName("DB chặn reserved > on_hand (chốt chặn cuối, không phụ thuộc code service)")
  void databaseRejectsOversell() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 1);

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    jdbc.update("update stock set reserved = reserved + 5 where sku_id = ?", sku)))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  // ==========================================================================
  // Security
  // ==========================================================================

  @Test
  @DisplayName("Không token → 401; buyer sửa tồn kho → 403; admin → 200")
  void securityOnStockEndpoints() {
    UUID sku = UUID.randomUUID();

    HttpResult anonymous = send(HttpMethod.PUT, "/v1/stock/" + sku, null, Map.of("onHand", 5));
    assertThat(anonymous.status()).isEqualTo(401);

    HttpResult buyer = send(HttpMethod.PUT, "/v1/stock/" + sku, buyerToken(), Map.of("onHand", 5));
    assertThat(buyer.status()).isEqualTo(403);

    HttpResult admin = send(HttpMethod.PUT, "/v1/stock/" + sku, adminToken(), Map.of("onHand", 5));
    assertThat(admin.status()).isEqualTo(200);
    assertThat(admin.body()).contains("\"onHand\":5").contains("\"available\":5");
  }

  // ==========================================================================
  // Reserve / commit / release — bài toán oversell
  // ==========================================================================

  @Test
  @DisplayName("reserve: 201, reserved tăng, available giảm, ledger RESERVE khớp")
  void reserveHoldsStock() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 10);
    UUID orderId = UUID.randomUUID();

    HttpResult reserve = reserve(orderId, Map.of(sku, 3));
    assertThat(reserve.status()).isEqualTo(201);
    assertThat(field(reserve.body(), "reservationId")).isNotBlank();

    StockView stock = stockOf(sku);
    assertThat(stock.onHand()).isEqualTo(10);
    assertThat(stock.reserved()).isEqualTo(3);
    assertThat(stock.available()).isEqualTo(7);

    assertThat(ledgerSum(sku)).isEqualTo(new int[] {10, 3}); // (Σ delta_on_hand, Σ delta_reserved)
  }

  @Test
  @DisplayName("reserve cùng orderId lần 2 → 200 + CÙNG reservationId, kho KHÔNG bị giữ 2 lần")
  void reserveIsIdempotentByOrderId() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 10);
    UUID orderId = UUID.randomUUID();

    HttpResult first = reserve(orderId, Map.of(sku, 4));
    HttpResult second = reserve(orderId, Map.of(sku, 4));

    assertThat(first.status()).isEqualTo(201);
    assertThat(second.status()).isEqualTo(200);
    assertThat(field(second.body(), "reservationId"))
        .isEqualTo(field(first.body(), "reservationId"));
    assertThat(stockOf(sku).reserved()).isEqualTo(4);
    assertThat(countReservations(orderId)).isEqualTo(1);
  }

  @Test
  @DisplayName("reserve quá tồn kho → 409 + details nêu SKU, không giữ chỗ, không tạo reservation")
  void reserveRejectsWhenNotEnoughStock() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 2);
    UUID orderId = UUID.randomUUID();

    HttpResult result = reserve(orderId, Map.of(sku, 5));

    assertThat(result.status()).isEqualTo(409);
    assertThat(result.body()).contains("CONFLICT").contains(sku.toString()).contains("còn 2");
    assertThat(stockOf(sku).reserved()).isZero();
    assertThat(countReservations(orderId)).isZero();
  }

  @Test
  @DisplayName(
      "reserve nhiều SKU: 1 SKU hết hàng ⇒ ALL-OR-NOTHING (SKU còn hàng cũng không bị giữ)")
  void reserveIsAllOrNothing() {
    UUID enough = UUID.randomUUID();
    UUID short1 = UUID.randomUUID();
    setStock(enough, 10);
    setStock(short1, 1);
    UUID orderId = UUID.randomUUID();

    HttpResult result = reserve(orderId, Map.of(enough, 2, short1, 5));

    assertThat(result.status()).isEqualTo(409);
    // Rollback thật: reservation + ledger của SKU đã giữ thành công phải biến mất.
    assertThat(stockOf(enough).reserved()).isZero();
    assertThat(ledgerSum(enough)).isEqualTo(new int[] {10, 0});
    assertThat(countReservations(orderId)).isZero();
  }

  @Test
  @DisplayName("commit: on_hand và reserved cùng giảm; commit lần 2 idempotent (không trừ 2 lần)")
  void commitMovesStockAndIsIdempotent() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 10);
    String reservationId =
        field(reserve(UUID.randomUUID(), Map.of(sku, 3)).body(), "reservationId");

    HttpResult first =
        post("/v1/stock/commit", buyerToken(), Map.of("reservationId", reservationId));
    assertThat(first.status()).isEqualTo(200);
    assertThat(first.body()).contains("\"status\":\"COMMITTED\"").contains("\"qty\":3");

    StockView afterCommit = stockOf(sku);
    assertThat(afterCommit.onHand()).isEqualTo(7);
    assertThat(afterCommit.reserved()).isZero();

    HttpResult second =
        post("/v1/stock/commit", buyerToken(), Map.of("reservationId", reservationId));
    assertThat(second.status()).isEqualTo(200);
    assertThat(stockOf(sku).onHand()).isEqualTo(7); // không trừ lần 2
    assertThat(ledgerSum(sku)).isEqualTo(new int[] {7, 0});
  }

  @Test
  @DisplayName("release: reserved trả lại, on_hand không đổi; lần 2 → 200; sau commit → 409")
  void releaseReturnsStock() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 10);
    String reservationId =
        field(reserve(UUID.randomUUID(), Map.of(sku, 4)).body(), "reservationId");

    HttpResult released =
        post("/v1/stock/release", buyerToken(), Map.of("reservationId", reservationId));
    assertThat(released.status()).isEqualTo(200);
    assertThat(released.body()).contains("\"status\":\"RELEASED\"");
    assertThat(stockOf(sku).reserved()).isZero();
    assertThat(stockOf(sku).onHand()).isEqualTo(10);

    assertThat(
            post("/v1/stock/release", buyerToken(), Map.of("reservationId", reservationId))
                .status())
        .isEqualTo(200);

    HttpResult committed =
        post("/v1/stock/commit", buyerToken(), Map.of("reservationId", reservationId));
    assertThat(committed.status()).isEqualTo(409);
  }

  @Test
  @DisplayName("commit reservation quá TTL → 410 + reserved được trả lại (hàng không bị treo)")
  void commitAfterExpiryReturnsGone() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 10);
    String reservationId =
        field(reserve(UUID.randomUUID(), Map.of(sku, 2)).body(), "reservationId");

    // Đẩy expires_at về quá khứ: mô phỏng user bỏ ngang (sweeper thuộc SL-2.3).
    jdbc.update(
        "update reservations set expires_at = now() - interval '1 minute' where id = ?::uuid",
        reservationId);

    HttpResult result =
        post("/v1/stock/commit", buyerToken(), Map.of("reservationId", reservationId));

    assertThat(result.status()).isEqualTo(410);
    assertThat(result.body()).contains("GONE");
    StockView stock = stockOf(sku);
    assertThat(stock.reserved()).isZero();
    assertThat(stock.onHand()).isEqualTo(10);
    assertThat(statusOf(reservationId)).isEqualTo("EXPIRED");
  }

  @Test
  @DisplayName("release reservation đã commit → 409 (hàng đã rời kho)")
  void releaseAfterCommitIsConflict() {
    UUID sku = UUID.randomUUID();
    setStock(sku, 5);
    String reservationId =
        field(reserve(UUID.randomUUID(), Map.of(sku, 1)).body(), "reservationId");
    post("/v1/stock/commit", buyerToken(), Map.of("reservationId", reservationId));

    HttpResult result =
        post("/v1/stock/release", buyerToken(), Map.of("reservationId", reservationId));

    assertThat(result.status()).isEqualTo(409);
    assertThat(result.body()).contains("CONFLICT");
  }

  @Test
  @DisplayName("reservation không tồn tại → 404; items lặp skuId → 400")
  void validationAndNotFound() {
    HttpResult missing =
        post("/v1/stock/commit", buyerToken(), Map.of("reservationId", UUID.randomUUID()));
    assertThat(missing.status()).isEqualTo(404);

    UUID sku = UUID.randomUUID();
    setStock(sku, 1);
    HttpResult duplicate =
        send(
            HttpMethod.POST,
            "/v1/stock/reserve",
            buyerToken(),
            Map.of(
                "orderId",
                UUID.randomUUID(),
                "items",
                List.of(Map.of("skuId", sku, "qty", 1), Map.of("skuId", sku, "qty", 1))));
    assertThat(duplicate.status()).isEqualTo(400);
    assertThat(duplicate.body()).contains("VALIDATION_ERROR");
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

  /** Đặt tồn kho bằng API admin (đường thật, không insert thẳng DB). */
  private void setStock(UUID sku, int onHand) {
    HttpResult result =
        send(HttpMethod.PUT, "/v1/stock/" + sku, adminToken(), Map.of("onHand", onHand));
    assertThat(result.status()).as("set stock %s=%d", sku, onHand).isEqualTo(200);
  }

  private HttpResult reserve(UUID orderId, Map<UUID, Integer> items) {
    List<Map<String, Object>> payload =
        items.entrySet().stream()
            .map(entry -> Map.<String, Object>of("skuId", entry.getKey(), "qty", entry.getValue()))
            .toList();
    return send(
        HttpMethod.POST,
        "/v1/stock/reserve",
        buyerToken(),
        Map.of("orderId", orderId, "items", payload));
  }

  private StockView stockOf(UUID sku) {
    HttpResult result = send(HttpMethod.GET, "/v1/stock/" + sku, buyerToken(), null);
    assertThat(result.status()).isEqualTo(200);
    return new StockView(
        intField(result.body(), "onHand"),
        intField(result.body(), "reserved"),
        intField(result.body(), "available"));
  }

  /** (Σ delta_on_hand, Σ delta_reserved) — chứng minh ledger khớp với bảng stock. */
  private int[] ledgerSum(UUID sku) {
    return jdbc.queryForObject(
        "select coalesce(sum(delta_on_hand), 0) as h, coalesce(sum(delta_reserved), 0) as r"
            + " from stock_ledger where sku_id = ?",
        (rs, rowNum) -> new int[] {rs.getInt("h"), rs.getInt("r")},
        sku);
  }

  private int countReservations(UUID orderId) {
    return jdbc.queryForObject(
        "select count(*) from reservations where order_id = ?", Integer.class, orderId);
  }

  private String statusOf(String reservationId) {
    return jdbc.queryForObject(
        "select status from reservations where id = ?::uuid", String.class, reservationId);
  }

  private HttpResult post(String path, String token, Object body) {
    return send(HttpMethod.POST, path, token, body);
  }

  private int intField(String json, String name) {
    return ((Number) parsed(json).get(name)).intValue();
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

  private record StockView(int onHand, int reserved, int available) {}
}
