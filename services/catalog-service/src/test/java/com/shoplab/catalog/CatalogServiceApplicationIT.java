package com.shoplab.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.catalog.testsupport.TokenMinter;
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
 * Integration test của catalog-service — chạy trên <b>Postgres thật</b> (Testcontainers).
 *
 * <p>Kiểm những thứ unit test không thể: Flyway tạo đúng schema, JPA map đúng cột (validate),
 * security chain chặn/cho qua đúng, và phân quyền 401/403/404/400 trên HTTP thật.
 *
 * <p><b>Vì sao vẫn có 401 khi gọi thẳng service</b>: đây chính là AC "gọi thẳng :8082 không token →
 * 401". IT chạy ở {@code RANDOM_PORT} nên mỗi request đều là "gọi thẳng" — không có gateway nào
 * đứng trước để tự phong role.
 *
 * <p><b>Thay {@code JwtDecoder} bằng khoá sinh trong test</b>: decoder thật tải JWKS qua mạng từ
 * identity-service. IT không dựng identity ⇒ inject decoder dùng public key của {@link
 * TokenMinter}. Đường JWKS thật được verify ở bước compose (không phải ở IT).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"shoplab.env=local", "shoplab.debug-endpoints=true"})
@Testcontainers
class CatalogServiceApplicationIT {

  /** Phải khớp {@code shoplab.jwt.issuer} trong application.yml. */
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
  static void catalogProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  // ==========================================================================
  // Schema / hạ tầng
  // ==========================================================================

  @Test
  @DisplayName("Flyway đã tạo schema catalog + ghi lịch sử migration (V1, V2, seed repeatable)")
  void flywayMigratedCatalogSchema() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);

    assertThat(tables).contains("category", "product", "sku", "flyway_schema_history");

    List<String> scripts =
        jdbc.queryForList("select script from flyway_schema_history", String.class);
    assertThat(scripts)
        .contains("V1__create_catalog.sql", "V2__create_sku.sql", "R__seed_dev_data.sql");
  }

  @Test
  @DisplayName("Probe công khai: /health và /version không cần token")
  void probesArePublic() {
    assertThat(get("/health").status()).isEqualTo(200);

    HttpResult version = get("/version");
    assertThat(version.status()).isEqualTo(200);
    assertThat(version.body()).contains("catalog-service");
  }

  // ==========================================================================
  // Đọc công khai (guest)
  // ==========================================================================

  @Test
  @DisplayName("Guest đọc product vừa tạo mà KHÔNG cần token")
  void guestCanReadProductWithoutToken() {
    UUID categoryId = createCategory(sellerToken(), "Điện thoại");
    UUID productId = createProduct(sellerToken(), categoryId, "iPhone 15", "25990000.00");

    HttpResult result = get("/v1/products/" + productId);

    assertThat(result.status()).isEqualTo(200);
    assertThat(result.body()).contains("\"name\":\"iPhone 15\"").contains("25990000.00");
  }

  // ==========================================================================
  // Ghi cần quyền: seller 201 · buyer 403 · không token 401
  // ==========================================================================

  @Test
  @DisplayName("seller tạo category → 201, guest list thấy ngay")
  void sellerCreatesCategory() {
    HttpResult created = post("/v1/categories", sellerToken(), Map.of("name", "Áo thun"));

    assertThat(created.status()).isEqualTo(201);
    assertThat(created.headers().get("location")).startsWith("/v1/categories/");

    HttpResult list = get("/v1/categories");
    assertThat(list.status()).isEqualTo(200);
    assertThat(list.body()).contains("Áo thun");
  }

  @Test
  @DisplayName("buyer tạo category → 403 (đã xác thực nhưng thiếu quyền) kèm ApiError")
  void buyerCannotCreateCategory() {
    HttpResult result = post("/v1/categories", buyerToken(), Map.of("name", "Sách"));

    assertThat(result.status()).isEqualTo(403);
    assertThat(result.body()).contains("FORBIDDEN").contains("traceId");
  }

  @Test
  @DisplayName("Không token mà POST product → 401 (không phụ thuộc gateway)")
  void postProductWithoutTokenIsUnauthorized() {
    HttpResult result = post("/v1/products", null, Map.of());

    assertThat(result.status()).isEqualTo(401);
    assertThat(result.body()).contains("UNAUTHORIZED").contains("traceId");
  }

  @Test
  @DisplayName("Client tự gửi x-user-role: admin KHÔNG leo được quyền (chỉ token được tin)")
  void forgedUserRoleHeaderIsIgnored() {
    UUID categoryId = createCategory(sellerToken(), "Nội thất");

    HttpResult result =
        send(
            HttpMethod.POST,
            "/v1/products",
            null,
            Map.of("categoryId", categoryId.toString(), "name", "Ghế", "price", "100000.00"),
            Map.of("x-user-role", "admin", "x-user-id", UUID.randomUUID().toString()));

    assertThat(result.status()).isEqualTo(401);
  }

  @Test
  @DisplayName("seller tạo product → 201; buyer tạo product → 403")
  void sellerCreatesProductBuyerForbidden() {
    UUID categoryId = createCategory(sellerToken(), "Phụ kiện");

    HttpResult created =
        post(
            "/v1/products",
            sellerToken(),
            Map.of(
                "categoryId", categoryId.toString(),
                "name", "Sạc 65W",
                "description", "Sạc nhanh",
                "price", "490000.00"));
    assertThat(created.status()).isEqualTo(201);
    assertThat(created.body()).contains("\"version\":0");

    HttpResult forbidden =
        post(
            "/v1/products",
            buyerToken(),
            Map.of("categoryId", categoryId.toString(), "name", "Cáp", "price", "90000.00"));
    assertThat(forbidden.status()).isEqualTo(403);
  }

  @Test
  @DisplayName("PUT product có thật → version trong response phải TĂNG (optimistic lock thật)")
  void updateBumpsVersionInResponse() {
    UUID categoryId = createCategory(sellerToken(), "Máy ảnh");
    UUID productId = createProduct(sellerToken(), categoryId, "Sony A7", "45000000.00");

    HttpResult updated =
        put(
            "/v1/products/" + productId,
            sellerToken(),
            Map.of("name", "Sony A7 IV", "price", "52000000.00"));

    assertThat(updated.status()).isEqualTo(200);
    // Bug đã gặp: entity managed nhưng version chỉ tăng lúc flush ⇒ response trả version CŨ (0).
    assertThat(updated.body()).contains("\"version\":1");

    assertThat(get("/v1/products/" + productId).body()).contains("\"name\":\"Sony A7 IV\"");
  }

  @Test
  @DisplayName("PUT/DELETE product không tồn tại → 404 ApiError")
  void missingProductReturns404() {
    UUID missing = UUID.randomUUID();
    String token = sellerToken();

    HttpResult put = put("/v1/products/" + missing, token, Map.of("name", "X", "price", "1000.00"));
    assertThat(put.status()).isEqualTo(404);
    assertThat(put.body()).contains("NOT_FOUND");

    HttpResult delete = delete("/v1/products/" + missing, token);
    assertThat(delete.status()).isEqualTo(404);
    assertThat(delete.body()).contains("NOT_FOUND");
  }

  @Test
  @DisplayName("Body sai → 400 kèm details nêu đúng field (name, price)")
  void invalidBodyReturns400WithFieldDetails() {
    UUID categoryId = createCategory(sellerToken(), "Thực phẩm");

    HttpResult result =
        post(
            "/v1/products",
            sellerToken(),
            Map.of("categoryId", categoryId.toString(), "name", " ", "price", "-1.00"));

    assertThat(result.status()).isEqualTo(400);
    assertThat(result.body())
        .contains("VALIDATION_ERROR")
        .contains("\"name\"")
        .contains("\"price\"");
  }

  @Test
  @DisplayName("Tạo category trùng slug → 409 CONFLICT (không phải 500 của DB)")
  void duplicateCategoryReturns409() {
    String token = sellerToken();
    assertThat(post("/v1/categories", token, Map.of("name", "Sách")).status()).isEqualTo(201);

    HttpResult duplicate = post("/v1/categories", token, Map.of("name", "Sách"));

    assertThat(duplicate.status()).isEqualTo(409);
    assertThat(duplicate.body()).contains("CONFLICT");
  }

  // ==========================================================================
  // SL-1.6: phân trang · filter · sort whitelist · SKU · seed
  // ==========================================================================

  @Test
  @DisplayName("Phân trang: page 0 và page 1 không trùng id, totalElements đúng theo filter")
  void paginationSplitsResults() {
    UUID categoryId = createCategory(sellerToken(), "Trang tri " + UUID.randomUUID());
    String token = sellerToken();
    for (int i = 1; i <= 7; i++) {
      createProduct(token, categoryId, "Den ngu " + i, (1000 * i) + ".00");
    }

    HttpResult page0 = get("/v1/products?categoryId=" + categoryId + "&page=0&size=5");
    assertThat(page0.status()).isEqualTo(200);
    assertThat(page0.body())
        .contains("\"totalElements\":7")
        .contains("\"totalPages\":2")
        .contains("\"page\":0")
        .contains("\"size\":5");

    HttpResult page1 = get("/v1/products?categoryId=" + categoryId + "&page=1&size=5");

    List<String> ids0 = ids(page0.body());
    List<String> ids1 = ids(page1.body());
    assertThat(ids0).hasSize(5);
    assertThat(ids1).hasSize(2);
    assertThat(ids0).doesNotContainAnyElementsOf(ids1);
  }

  @Test
  @DisplayName("size ngoài 1..100 · page âm → 400 (KHÔNG âm thầm clamp)")
  void invalidPagingIsRejected() {
    assertThat(get("/v1/products?size=0").status()).isEqualTo(400);
    assertThat(get("/v1/products?size=101").status()).isEqualTo(400);
    assertThat(get("/v1/products?page=-1").status()).isEqualTo(400);
    assertThat(get("/v1/products?size=100").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("sort=price,desc → giảm dần; sort lạ → 400 (không lọt vào SQL)")
  void sortIsWhitelistedAndWorks() {
    UUID categoryId = createCategory(sellerToken(), "The thao " + UUID.randomUUID());
    String token = sellerToken();
    createProduct(token, categoryId, "Bong da", "300000.00");
    createProduct(token, categoryId, "Vot cau long", "900000.00");
    createProduct(token, categoryId, "Giay bong ro", "1500000.00");

    HttpResult sorted = get("/v1/products?categoryId=" + categoryId + "&sort=price,desc&size=10");

    assertThat(sorted.status()).isEqualTo(200);
    List<Double> prices = prices(sorted.body());
    assertThat(prices).containsExactly(1500000.0, 900000.0, 300000.0);

    assertThat(get("/v1/products?sort=drop%20table").status()).isEqualTo(400);
    assertThat(get("/v1/products?sort=name,asc").status()).isEqualTo(400);
  }

  @Test
  @DisplayName("q tìm theo tên KHÔNG phân biệt hoa/thường")
  void searchIsCaseInsensitive() {
    UUID categoryId = createCategory(sellerToken(), "Tim kiem " + UUID.randomUUID());
    createProduct(sellerToken(), categoryId, "IPhone IT 555", "1000000.00");

    HttpResult found = get("/v1/products?q=iphone%20it%20555&size=10");

    assertThat(found.status()).isEqualTo(200);
    assertThat(found.body()).contains("IPhone IT 555");
    assertThat(found.body()).contains("\"totalElements\":1");
  }

  @Test
  @DisplayName("Chi tiết product trả kèm SKU; tạo SKU trùng mã → 409")
  void detailIncludesSkus() {
    UUID categoryId = createCategory(sellerToken(), "Phu kien " + UUID.randomUUID());
    String token = sellerToken();
    UUID productId = createProduct(token, categoryId, "Op lung", "150000.00");

    HttpResult created =
        post(
            "/v1/products/" + productId + "/skus",
            token,
            Map.of("code", "OP-" + productId.toString().substring(0, 8), "stockHint", 5));
    assertThat(created.status()).isEqualTo(201);

    HttpResult detail = get("/v1/products/" + productId);
    assertThat(detail.status()).isEqualTo(200);
    assertThat(detail.body()).contains("\"skus\"").contains("OP-");

    String code = field(created.body(), "code");
    HttpResult duplicate = post("/v1/products/" + productId + "/skus", token, Map.of("code", code));
    assertThat(duplicate.status()).isEqualTo(409);
  }

  @Test
  @DisplayName("Seed dev (R__seed_dev_data.sql) đã nạp: list public có >= 20 product")
  void seedDataIsPresent() {
    HttpResult list = get("/v1/products?size=1");

    assertThat(list.status()).isEqualTo(200);
    String body = list.body();
    int total = Integer.parseInt(body.replaceAll(".*\"totalElements\":(\\d+).*", "$1"));
    assertThat(total).isGreaterThanOrEqualTo(20);
  }

  // ==========================================================================
  // Helpers
  // ==========================================================================

  private String sellerToken() {
    return MINTER.token(UUID.randomUUID(), "SELLER");
  }

  private String buyerToken() {
    return MINTER.token(UUID.randomUUID(), "BUYER");
  }

  private UUID createCategory(String token, String name) {
    HttpResult created = post("/v1/categories", token, Map.of("name", name));
    assertThat(created.status()).as("tạo category '%s' phải thành công", name).isEqualTo(201);
    return UUID.fromString(field(created.body(), "id"));
  }

  private UUID createProduct(String token, UUID categoryId, String name, String price) {
    HttpResult created =
        post(
            "/v1/products",
            token,
            Map.of("categoryId", categoryId.toString(), "name", name, "price", price));
    assertThat(created.status()).as("tạo product '%s' phải thành công", name).isEqualTo(201);
    return UUID.fromString(field(created.body(), "id"));
  }

  @SuppressWarnings("unchecked")
  private String field(String json, String name) {
    Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
    return String.valueOf(parsed.get(name));
  }

  /** Danh sách id trong {@code content} của response page. */
  @SuppressWarnings("unchecked")
  private List<String> ids(String json) {
    Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
    List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
    return content.stream().map(item -> String.valueOf(item.get("id"))).toList();
  }

  /** Danh sách giá (theo thứ tự trả về) để assert sort. */
  @SuppressWarnings("unchecked")
  private List<Double> prices(String json) {
    Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
    List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
    return content.stream().map(item -> ((Number) item.get("price")).doubleValue()).toList();
  }

  private HttpResult get(String path) {
    return send(HttpMethod.GET, path, null, null, Map.of());
  }

  private HttpResult post(String path, String token, Object body) {
    return send(HttpMethod.POST, path, token, body, Map.of());
  }

  private HttpResult put(String path, String token, Object body) {
    return send(HttpMethod.PUT, path, token, body, Map.of());
  }

  private HttpResult delete(String path, String token) {
    return send(HttpMethod.DELETE, path, token, null, Map.of());
  }

  private HttpResult send(
      HttpMethod method, String path, String token, Object body, Map<String, String> extraHeaders) {
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
      return new HttpResult(response.statusCode(), response.body(), firstHeaders(response));
    } catch (Exception e) {
      throw new IllegalStateException("Gọi " + method + " " + path + " thất bại", e);
    }
  }

  private static Map<String, String> firstHeaders(HttpResponse<String> response) {
    return response.headers().map().entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT),
                entry -> entry.getValue().getFirst()));
  }

  /** Response thô — chỉ cần status, body, header để assert. */
  private record HttpResult(int status, String body, Map<String, String> headers) {}
}
