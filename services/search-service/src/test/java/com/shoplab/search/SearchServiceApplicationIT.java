package com.shoplab.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.search.index.SearchIndexService;
import com.shoplab.search.index.SearchProduct;
import com.shoplab.search.testsupport.TokenMinter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test của search-service — Postgres thật (Testcontainers), catalog giả (HTTP server
 * trong tiến trình test).
 *
 * <p><b>Vì sao catalog giả chứ không phải WireMock</b>: chỉ cần đúng một endpoint, một vài trạng
 * thái; `com.sun.net.httpserver` có sẵn trong JDK ⇒ không thêm dependency cho một stub 20 dòng.
 *
 * <p><b>Vì sao phải có IT cho phần này</b>: những thứ cần chứng minh đều nằm ở Postgres —
 * `unaccent` khớp tiếng Việt, `tsvector` được ghi, GIN index dùng được, UPSERT không nhân bản row,
 * watermark chống archive oan. Unit test không chạm được cái nào.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      // Không có broker trong IT: không tự start consumer, và đẩy job so lệch ra thật xa.
      "shoplab.events.consumer-auto-startup=false",
      "shoplab.search.drift-check-initial-delay=1h",
      "shoplab.search.drift-check-interval=1h"
    })
@Testcontainers
class SearchServiceApplicationIT {

  private static final String ISSUER = "https://identity.shoplab.local";

  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Product mà catalog "giả" đang có — test reindex đổi nội dung list này. */
  private static final AtomicReference<List<Map<String, Object>>> CATALOG_PRODUCTS =
      new AtomicReference<>(List.of());

  private static final HttpServer CATALOG_STUB = startCatalogStub();

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private SearchIndexService index;

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
  static void searchProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "shoplab.search.catalog-url",
        () -> "http://localhost:" + CATALOG_STUB.getAddress().getPort());
  }

  // ==========================================================================
  // Schema
  // ==========================================================================

  @Test
  @DisplayName(
      "Flyway tạo schema: search_product + GIN index + processed_events + extension unaccent")
  void schemaCreated() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);
    assertThat(tables).contains("search_product", "processed_events", "flyway_schema_history");

    List<String> extensions = jdbc.queryForList("select extname from pg_extension", String.class);
    assertThat(extensions).contains("unaccent");

    String indexDef =
        jdbc.queryForObject(
            "select indexdef from pg_indexes where indexname = 'ix_search_product_vector'",
            String.class);
    assertThat(indexDef).containsIgnoringCase("gin").contains("search_vector");
  }

  // ==========================================================================
  // Đường event → index → search
  // ==========================================================================

  @Test
  @DisplayName("AC1: product.updated ⇒ search thấy ngay, giá đúng; search_vector được ghi")
  void updatedEventMakesProductSearchable() {
    UUID categoryId = UUID.randomUUID();
    UUID productId = createIndexedProduct("Điện thoại Samsung", categoryId, "12990000.00");

    String body = get("/v1/search?q=dien%20thoai").body();

    assertThat(body).contains(productId.toString()).contains("12990000");
    // Cột `search_vector` phải có dữ liệu: nếu quên tính vector thì API vẫn "không lỗi" nhưng
    // KHÔNG BAO GIỜ trả kết quả — loại bug chỉ lộ ra khi kiểm trực tiếp DB.
    String vector =
        jdbc.queryForObject(
            "select search_vector::text from search_product where product_id = ?",
            String.class,
            productId);
    assertThat(vector).contains("dien").contains("thoai");
  }

  @Test
  @DisplayName("AC3: `q=dien%` không làm lỗi (tsquery, không phải LIKE với wildcard)")
  void wildcardCharactersAreHarmless() {
    createIndexedProduct("Điện thoại Xiaomi", UUID.randomUUID(), "5990000.00");

    assertThat(get("/v1/search?q=dien%25").status()).isEqualTo(200);
    assertThat(get("/v1/search?q=%25%25%25").status()).isEqualTo(200);
    assertThat(get("/v1/search?q=dien%20thoai%25").body()).doesNotContain("\"status\":500");
  }

  @Test
  @DisplayName("AC2: product.archived ⇒ search không còn; row chuyển ARCHIVED (không xoá cứng)")
  void archivedEventHidesProduct() {
    UUID productId = createIndexedProduct("Nồi chiên không dầu", UUID.randomUUID(), "1990000.00");
    assertThat(get("/v1/search?q=noi%20chien").body()).contains(productId.toString());

    index.applyArchived(
        Map.of("productId", productId.toString(), "archivedAt", Instant.now().toString()),
        Instant.now());

    assertThat(get("/v1/search?q=noi%20chien").body()).doesNotContain(productId.toString());
    assertThat(
            jdbc.queryForObject(
                "select status from search_product where product_id = ?", String.class, productId))
        .isEqualTo(SearchProduct.ARCHIVED);
  }

  @Test
  @DisplayName("event đến LỆCH THỨ TỰ: event cũ không ghi đè dữ liệu mới (chốt updated_at)")
  void olderEventDoesNotOverwriteNewer() {
    UUID categoryId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    Instant newer = Instant.parse("2026-11-09T10:00:05Z");
    Instant older = Instant.parse("2026-11-09T10:00:00Z");

    index.applyUpdated(payload(productId, "Điện thoại mới", categoryId, "9990000.00", newer));
    index.applyUpdated(payload(productId, "Điện thoại cũ", categoryId, "1110000.00", older));

    assertThat(get("/v1/search?q=dien%20thoai").body())
        .contains("Điện thoại mới")
        .doesNotContain("Điện thoại cũ");
    assertThat(
            jdbc.queryForObject(
                "select min_price from search_product where product_id = ?",
                java.math.BigDecimal.class,
                productId))
        .isEqualByComparingTo("9990000.00");
  }

  @Test
  @DisplayName("filter: category + khoảng giá + phân trang; chỉ trả ACTIVE")
  void filtersAndPaging() {
    UUID categoryId = UUID.randomUUID();
    createIndexedProduct("Máy lọc nước A", categoryId, "3000000.00");
    createIndexedProduct("Máy lọc nước B", categoryId, "7000000.00");
    createIndexedProduct("Máy lọc nước C", UUID.randomUUID(), "9000000.00");

    String byCategory = get("/v1/search?q=may%20loc%20nuoc&categoryId=" + categoryId).body();
    assertThat(byCategory)
        .contains("Máy lọc nước A", "Máy lọc nước B")
        .doesNotContain("Máy lọc nước C");

    String byPrice = get("/v1/search?q=may%20loc%20nuoc&minPrice=5000000&maxPrice=8000000").body();
    assertThat(byPrice)
        .contains("Máy lọc nước B")
        .doesNotContain("Máy lọc nước A", "Máy lọc nước C");

    String paged = get("/v1/search?q=may%20loc%20nuoc&size=1&page=1").body();
    assertThat(paged).contains("\"size\":1").contains("\"page\":1");
  }

  @Test
  @DisplayName("tham số sai ⇒ 400 ApiError; search là public nên không token vẫn 200")
  void queryValidationAndPublicAccess() {
    assertThat(get("/v1/search?size=0").status()).isEqualTo(400);
    assertThat(get("/v1/search?size=1000").status()).isEqualTo(400);
    assertThat(get("/v1/search?page=-1").status()).isEqualTo(400);
    assertThat(get("/v1/search?categoryId=khong-phai-uuid").status()).isEqualTo(400);
    assertThat(get("/v1/search").status()).isEqualTo(200);
  }

  // ==========================================================================
  // Reconcile (AC4)
  // ==========================================================================

  @Test
  @DisplayName("AC4: xoá tay row index ⇒ reindex dựng lại đủ; gọi 2 lần KHÔNG nhân bản row")
  void reindexRebuildsMissingRowsIdempotently() {
    UUID categoryId = UUID.randomUUID();
    List<Map<String, Object>> catalog = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      catalog.add(catalogProduct("Sản phẩm reindex " + i, categoryId, "100000" + i + ".00"));
    }
    CATALOG_PRODUCTS.set(catalog);

    // "Ai đó xoá tay row index" — đúng ca AC4.
    jdbc.update("delete from search_product");
    assertThat(get("/v1/search?q=san%20pham%20reindex").body())
        .doesNotContain("Sản phẩm reindex 1");

    HttpResult first = post("/v1/admin/search/reindex", adminToken());
    assertThat(first.status()).isEqualTo(200);
    assertThat(first.body()).contains("\"scanned\":5").contains("\"upserted\":5");
    assertThat(get("/v1/search?q=san%20pham%20reindex&size=10").body())
        .contains("Sản phẩm reindex 1", "Sản phẩm reindex 5");

    // Lần 2: cùng kết quả, KHÔNG thêm row (UPSERT theo product_id).
    HttpResult second = post("/v1/admin/search/reindex", adminToken());
    assertThat(second.status()).isEqualTo(200);
    assertThat(second.body()).contains("\"archived\":0");
    Long rows =
        jdbc.queryForObject(
            "select count(*) from search_product where category_id = ?", Long.class, categoryId);
    assertThat(rows).isEqualTo(5L);
  }

  @Test
  @DisplayName("reconcile archive row index mồ côi (catalog không còn product đó)")
  void reindexArchivesOrphans() {
    UUID lonelyId =
        createIndexedProduct("Hàng đã bị xoá khỏi catalog", UUID.randomUUID(), "1000.00");
    CATALOG_PRODUCTS.set(List.of(catalogProduct("Hàng còn sống", UUID.randomUUID(), "2000.00")));

    HttpResult result = post("/v1/admin/search/reindex", adminToken());

    assertThat(result.status()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "select status from search_product where product_id = ?", String.class, lonelyId))
        .isEqualTo(SearchProduct.ARCHIVED);
    assertThat(get("/v1/search?q=hang%20da%20bi%20xoa").body()).doesNotContain(lonelyId.toString());
  }

  @Test
  @DisplayName("reindex chỉ ADMIN: không token ⇒ 401, buyer ⇒ 403")
  void reindexRequiresAdmin() {
    assertThat(post("/v1/admin/search/reindex", null).status()).isEqualTo(401);

    String buyer = MINTER.token(UUID.randomUUID(), "BUYER");
    assertThat(post("/v1/admin/search/reindex", buyer).status()).isEqualTo(403);
  }

  // ==========================================================================
  // Catalog giả (chỉ đúng endpoint reindex dùng: GET /v1/products)
  // ==========================================================================

  private static HttpServer startCatalogStub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/products", exchange -> respond(exchange, productsJson(exchange.getRequestURI())));
      server.start();
      return server;
    } catch (IOException ex) {
      throw new IllegalStateException("không dựng được catalog stub", ex);
    }
  }

  private static void respond(HttpExchange exchange, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  /** Trả đúng shape `PageResponse<ProductResponse>` của catalog, có phân trang thật. */
  private static String productsJson(URI uri) {
    int page = queryInt(uri, "page", 0);
    int size = queryInt(uri, "size", 100);
    List<Map<String, Object>> all = CATALOG_PRODUCTS.get();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    int totalPages = size <= 0 ? 0 : (all.size() + size - 1) / size;
    String content =
        all.subList(from, to).stream()
            .map(SearchServiceApplicationIT::toJson)
            .collect(Collectors.joining(","));
    return "{\"content\":["
        + content
        + "],\"page\":"
        + page
        + ",\"size\":"
        + size
        + ",\"totalElements\":"
        + all.size()
        + ",\"totalPages\":"
        + totalPages
        + "}";
  }

  private static int queryInt(URI uri, String name, int fallback) {
    String query = uri.getQuery();
    if (query == null) {
      return fallback;
    }
    for (String pair : query.split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2 && parts[0].equals(name)) {
        return Integer.parseInt(parts[1]);
      }
    }
    return fallback;
  }

  private static Map<String, Object> catalogProduct(String name, UUID categoryId, String price) {
    Instant now = Instant.parse("2026-11-09T09:00:00Z");
    Map<String, Object> product = new LinkedHashMap<>();
    product.put("id", UUID.randomUUID().toString());
    product.put("categoryId", categoryId.toString());
    product.put("name", name);
    product.put("description", "mô tả " + name);
    product.put("price", price);
    product.put("status", "ACTIVE");
    product.put("version", 0);
    product.put("createdAt", now.toString());
    product.put("updatedAt", now.toString());
    return product;
  }

  private static String toJson(Map<String, Object> values) {
    return values.entrySet().stream()
        .map(entry -> "\"" + entry.getKey() + "\":" + jsonValue(entry.getValue()))
        .collect(Collectors.joining(",", "{", "}"));
  }

  private static String jsonValue(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Number) {
      return value.toString();
    }
    return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  // ==========================================================================
  // Helpers
  // ==========================================================================

  private UUID createIndexedProduct(String name, UUID categoryId, String price) {
    UUID productId = UUID.randomUUID();
    index.applyUpdated(payload(productId, name, categoryId, price, Instant.now()));
    return productId;
  }

  private static Map<String, Object> payload(
      UUID productId, String name, UUID categoryId, String price, Instant updatedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("productId", productId.toString());
    payload.put("name", name);
    payload.put("description", "mô tả " + name);
    payload.put("categoryId", categoryId.toString());
    payload.put("price", price);
    payload.put("status", "ACTIVE");
    payload.put("updatedAt", updatedAt.toString());
    return payload;
  }

  private String adminToken() {
    // Claim `role` viết HOA: SecurityConfig map 1-1 thành authority `ROLE_<role>` ⇒
    // `hasRole('ADMIN')`.
    return MINTER.token(UUID.randomUUID(), "ADMIN");
  }

  private HttpResult get(String path) {
    return send(HttpMethod.GET, path, null);
  }

  private HttpResult post(String path, String token) {
    return send(HttpMethod.POST, path, token);
  }

  private HttpResult send(HttpMethod method, String path, String token) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(20))
            .method(method.name(), HttpRequest.BodyPublishers.noBody());
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    try {
      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new HttpResult(response.statusCode(), response.body());
    } catch (IOException | InterruptedException ex) {
      throw new IllegalStateException("gọi " + path + " thất bại", ex);
    }
  }

  private record HttpResult(int status, String body) {}
}
