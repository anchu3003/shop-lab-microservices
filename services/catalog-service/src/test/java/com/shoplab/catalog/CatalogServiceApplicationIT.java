package com.shoplab.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.catalog.testsupport.TokenMinter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
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
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      // SL-1.15: seed dev nằm ở `db/seed` (không nạp mặc định) — IT cần seed để test phân trang.
      "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
      // SL-3.6: IT KHÔNG có broker. Relay mà chạy thì mỗi giây một lần cố publish rồi thất bại —
      // log ồn ào và test chậm, trong khi điều cần kiểm là "có dòng outbox" (publish là việc của
      // relay, đã có test riêng ở order/payment). Đẩy nhịp ra thật xa.
      "shoplab.outbox.initial-delay=1h",
      "shoplab.outbox.interval=1h"
    })
@Testcontainers
class CatalogServiceApplicationIT {

  /** Phải khớp {@code shoplab.jwt.issuer} trong application.yml. */
  private static final String ISSUER = "https://identity.shoplab.local";

  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  /**
   * Redis thật cho test cache (SL-4.2). IT dùng Redis thật chứ không fake vì thứ cần kiểm ở đây là
   * hành vi <b>của Redis</b> dưới cùng lúc nhiều request ({@code SET NX}, MGET), không phải logic
   * Java — logic đã có unit test với store trong bộ nhớ.
   */
  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private org.springframework.data.redis.core.StringRedisTemplate redis;

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
    // SL-4.2: Redis cho cache-aside. Jitter = 0 để TTL tất định (jitter có test riêng).
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("shoplab.catalog.cache.ttl-jitter-percent", () -> 0);
  }

  // ==========================================================================
  // Schema / hạ tầng
  // ==========================================================================

  @Test
  @DisplayName("Flyway đã tạo schema catalog + ghi lịch sử migration (V1..V4, seed repeatable)")
  void flywayMigratedCatalogSchema() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'"
                + " order by table_name",
            String.class);

    assertThat(tables).contains("category", "product", "sku", "outbox", "flyway_schema_history");

    List<String> scripts =
        jdbc.queryForList("select script from flyway_schema_history", String.class);
    assertThat(scripts)
        .contains(
            "V1__create_catalog.sql",
            "V2__create_sku.sql",
            "V3__add_soft_delete.sql",
            // SL-3.6: bảng outbox (producer event product).
            "V4__create_outbox.sql",
            "R__seed_dev_data.sql");

    // SL-2.11: cột soft delete phải tồn tại, `status` có DEFAULT 'ACTIVE' (20 row seed không phải
    // UPDATE).
    // KHÔNG assert "cả bảng đều ACTIVE": IT khác có thể đã archive product trước test này (thứ tự
    // test không được đảm bảo) ⇒ assertion kiểu aggregate toàn bảng là flaky.
    List<Map<String, Object>> columns =
        jdbc.queryForList(
            "select column_name, is_nullable, column_default from information_schema.columns"
                + " where table_name = 'product' and column_name in ('status', 'deleted_at')");
    assertThat(columns).hasSize(2);
    Map<String, Object> status =
        columns.stream()
            .filter(c -> "status".equals(c.get("column_name")))
            .findFirst()
            .orElseThrow();
    assertThat(String.valueOf(status.get("column_default"))).contains("ACTIVE");
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
    // Tên unique: slug bỏ dấu từ SL-1.15 nên "Áo thun" sẽ trùng category seed (slug "ao-thun").
    String name = "Áo thun " + UUID.randomUUID().toString().substring(0, 8);
    HttpResult created = post("/v1/categories", sellerToken(), Map.of("name", name));

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
  // SL-2.11: soft delete product (ADR-0006 D5)
  // ==========================================================================

  @Test
  @DisplayName(
      "DELETE product = xoá MỀM: row còn, status ARCHIVED, deleted_at set, SKU archive theo")
  void deleteArchivesProductInsteadOfRemoving() {
    UUID categoryId = createCategory(sellerToken(), "Do choi");
    String token = sellerToken();
    UUID productId = createProduct(token, categoryId, "Lego City", "1200000.00");
    UUID skuId = createSku(token, productId, "LEGO-CITY-1");

    // Trước khi xoá: API trả status ACTIVE (field mới của SL-2.11).
    assertThat(get("/v1/products/" + productId).body()).contains("\"status\":\"ACTIVE\"");

    HttpResult deleted = delete("/v1/products/" + productId, token);
    assertThat(deleted.status()).isEqualTo(204);

    // (1) API coi như không tồn tại
    assertThat(get("/v1/products/" + productId).status()).isEqualTo(404);

    // (2) List không còn — kể cả khi filter đúng category của nó
    HttpResult list = get("/v1/products?categoryId=" + categoryId + "&size=100");
    assertThat(list.status()).isEqualTo(200);
    assertThat(ids(list.body())).doesNotContain(productId.toString());

    // (3) DB: row VẪN CÒN, đánh dấu ARCHIVED + deleted_at
    Map<String, Object> row =
        jdbc.queryForMap("select status, deleted_at from product where id = ?", productId);
    assertThat(row.get("status")).isEqualTo("ARCHIVED");
    assertThat(row.get("deleted_at")).isNotNull();

    // (4) SKU còn row (không bị ON DELETE CASCADE xoá) nhưng đã ARCHIVED ⇒ Phase 2 còn tra được
    // sku_id
    Map<String, Object> sku = jdbc.queryForMap("select status from sku where id = ?", skuId);
    assertThat(sku.get("status")).isEqualTo("ARCHIVED");

    // (5) SL-3.6: `product.archived.v1` nằm trong outbox NGAY (cùng transaction với UPDATE product)
    // — đây là bằng chứng "catalog không gọi thẳng search": nó chỉ ghi ý định phát event.
    Map<String, Object> event =
        jdbc.queryForMap(
            "select event_type, payload::text as payload, status from outbox"
                + " where aggregate_id = ? and event_type = 'product.archived.v1'",
            productId);
    assertThat(event.get("status")).isEqualTo("PENDING");
    assertThat(String.valueOf(event.get("payload"))).contains(productId.toString());
  }

  @Test
  @DisplayName(
      "SL-3.6: tạo + sửa product ⇒ 2 dòng outbox `product.updated.v1` với payload đủ field")
  void productWritesUpdatedEventsToOutbox() {
    UUID categoryId = createCategory(sellerToken(), "Do gia dung");
    String token = sellerToken();
    UUID productId = createProduct(token, categoryId, "Noi chien khong dau", "990000.00");

    assertThat(
            put(
                    "/v1/products/" + productId,
                    token,
                    Map.of("name", "Noi chien khong dau 6L", "price", "1290000.00"))
                .status())
        .isEqualTo(200);

    // Hai lần đổi (create + update) ⇒ 2 event: sửa nhiều lần là hợp lệ, khác với
    // `payment.succeeded`
    // (một intent chỉ được phát một lần).
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select payload::text as payload from outbox"
                + " where aggregate_id = ? and event_type = 'product.updated.v1'"
                + " order by created_at",
            productId);
    assertThat(rows).hasSize(2);

    // Payload của lần sửa phải là trạng thái MỚI (nếu ghi trước khi flush thì đây là chỗ lộ ra).
    // jsonb đọc qua JDBC là PGobject ⇒ ép ::text ngay trong câu query cho ra String.
    String lastPayload = String.valueOf(rows.get(1).get("payload"));
    assertThat(lastPayload)
        .contains("Noi chien khong dau 6L")
        .contains("1290000.00")
        .contains(categoryId.toString())
        .contains("\"status\": \"ACTIVE\"");
  }

  @Test
  @DisplayName("product đã xoá mềm: PUT / POST SKU / DELETE lần 2 đều 404, không sinh dữ liệu mới")
  void archivedProductIsInvisibleToWrites() {
    UUID categoryId = createCategory(sellerToken(), "Noi that");
    String token = sellerToken();
    UUID productId = createProduct(token, categoryId, "Noi chien", "900000.00");
    assertThat(delete("/v1/products/" + productId, token).status()).isEqualTo(204);

    HttpResult put =
        put("/v1/products/" + productId, token, Map.of("name", "X", "price", "1000.00"));
    assertThat(put.status()).isEqualTo(404);

    HttpResult addSku =
        post(
            "/v1/products/" + productId + "/skus",
            token,
            Map.of("code", "NOI-" + UUID.randomUUID()));
    assertThat(addSku.status()).isEqualTo(404);

    HttpResult secondDelete = delete("/v1/products/" + productId, token);
    assertThat(secondDelete.status()).isEqualTo(404);
    assertThat(secondDelete.body()).contains("NOT_FOUND");

    // Không có SKU mới nào được tạo cho product đã xoá.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sku where product_id = ?", Integer.class, productId))
        .isZero();
  }

  @Test
  @DisplayName(
      "DB là chốt chặn cuối: status ACTIVE mà deleted_at NOT NULL → vi phạm CHECK constraint")
  void databaseRejectsInconsistentSoftDeleteState() {
    UUID categoryId = createCategory(sellerToken(), "Van phong pham");
    UUID productId = createProduct(sellerToken(), categoryId, "But bi", "5000.00");

    assertThatThrownBy(
            () -> jdbc.update("update product set deleted_at = now() where id = ?", productId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ==========================================================================
  // SL-2.13: GET /v1/skus?ids= — batch giá cho saga checkout + giỏ hàng
  // ==========================================================================

  @Test
  @DisplayName(
      "GET /v1/skus (public, không token) → 200; giá = priceOverride nếu có, ngược lại giá product")
  void skuLookupReturnsPricesWithoutToken() {
    String token = sellerToken();
    UUID categoryId = createCategory(token, "Batch gia");
    UUID productId = createProduct(token, categoryId, "Ao batch " + UUID.randomUUID(), "180000.00");
    String overrideCode = code("BATCH-A");
    UUID withOverride = createSkuWithPrice(token, productId, overrideCode, "150000.00");
    UUID plain = createSku(token, productId, code("BATCH-B"));

    HttpResult result = get("/v1/skus?ids=" + withOverride + "," + plain);

    assertThat(result.status()).isEqualTo(200);
    List<Map<String, Object>> items = listField(result.body(), "items");
    assertThat(items).hasSize(2);
    // Thứ tự = thứ tự client gửi (không phụ thuộc thứ tự DB).
    assertThat(String.valueOf(items.get(0).get("skuId"))).isEqualTo(withOverride.toString());
    assertThat(String.valueOf(items.get(0).get("skuCode"))).isEqualTo(overrideCode);
    assertThat(String.valueOf(items.get(0).get("productId"))).isEqualTo(productId.toString());
    assertThat(String.valueOf(items.get(0).get("title"))).startsWith("Ao batch");
    // So sánh theo Number: JSON parse ra Double nên "150000.00" không bằng chuỗi "150000.0".
    assertThat(((Number) items.get(0).get("unitPrice")).doubleValue()).isEqualTo(150000.0);
    assertThat(((Number) items.get(1).get("unitPrice")).doubleValue()).isEqualTo(180000.0);
    assertThat(listField(result.body(), "missingIds")).isEmpty();
  }

  @Test
  @DisplayName("SKU của product đã xoá mềm → nằm trong missingIds, KHÔNG nằm trong items")
  void skuLookupSkipsArchivedSku() {
    String token = sellerToken();
    UUID categoryId = createCategory(token, "Batch archive");
    UUID productId = createProduct(token, categoryId, "Sap xoa " + UUID.randomUUID(), "90000.00");
    UUID skuId = createSku(token, productId, code("GONE-1"));

    assertThat(delete("/v1/products/" + productId, token).status()).isEqualTo(204);

    HttpResult result = get("/v1/skus?ids=" + skuId);

    assertThat(result.status()).isEqualTo(200);
    assertThat(listField(result.body(), "items")).isEmpty();
    assertThat(String.valueOf(listField(result.body(), "missingIds").get(0)))
        .isEqualTo(skuId.toString());
  }

  @Test
  @DisplayName("id không tồn tại + id trùng lặp ⇒ missingIds đúng, items không nhân bản")
  void skuLookupHandlesUnknownAndDuplicateIds() {
    String token = sellerToken();
    UUID categoryId = createCategory(token, "Batch dedupe");
    UUID productId = createProduct(token, categoryId, "Trung " + UUID.randomUUID(), "10000.00");
    UUID skuId = createSku(token, productId, code("DUP-1"));
    UUID unknown = UUID.randomUUID();

    HttpResult result = get("/v1/skus?ids=" + skuId + "," + unknown + "," + skuId);

    assertThat(result.status()).isEqualTo(200);
    assertThat(listField(result.body(), "items")).hasSize(1);
    assertThat(listField(result.body(), "missingIds")).hasSize(1);
    assertThat(String.valueOf(listField(result.body(), "missingIds").get(0)))
        .isEqualTo(unknown.toString());
  }

  @Test
  @DisplayName("ids rỗng / sai UUID / quá 100 id → 400 VALIDATION_ERROR")
  void skuLookupRejectsInvalidIds() {
    HttpResult missing = get("/v1/skus");
    assertThat(missing.status()).isEqualTo(400);
    assertThat(missing.body()).contains("VALIDATION_ERROR").contains("ids");

    assertThat(get("/v1/skus?ids=khong-phai-uuid").status()).isEqualTo(400);

    String tooMany =
        java.util.stream.IntStream.range(0, 101)
            .mapToObj(i -> UUID.randomUUID().toString())
            .collect(java.util.stream.Collectors.joining(","));
    HttpResult overLimit = get("/v1/skus?ids=" + tooMany);
    assertThat(overLimit.status()).isEqualTo(400);
    assertThat(overLimit.body()).contains("tối đa 100");
  }

  // ==========================================================================
  // Helpers
  // ==========================================================================
  // ==========================================================================
  // SL-4.2 — cache-aside trên Redis thật
  // ==========================================================================

  @Test
  @DisplayName("SL-4.2: đọc lần 2 từ cache (hit), update thì detail + list trả dữ liệu MỚI ngay")
  void cacheServesReadsAndInvalidatesOnWrite() {
    String token = sellerToken();
    UUID categoryId = createCategory(token, "Cache IT");
    UUID productId = createProduct(token, categoryId, "Ao cache IT", "100000.00");

    HttpResult first = get("/v1/products/" + productId);
    assertThat(first.status()).isEqualTo(200);
    Map<String, Long> before = cacheCounters();

    HttpResult second = get("/v1/products/" + productId);
    assertThat(second.status()).isEqualTo(200);
    // Cùng dữ liệu, nhưng lần 2 KHÔNG được chạm DB.
    assertThat(second.body()).isEqualTo(first.body());
    Map<String, Long> after = cacheCounters();
    assertThat(delta(after, before, "detail.hit")).as("lần đọc 2 phải là cache hit").isPositive();
    assertThat(delta(after, before, "detail.dbLoad")).as("hit thì không đọc DB").isZero();

    // Update ⇒ vô hiệu hoá NGAY, không chờ TTL 60 s.
    HttpResult updated =
        put(
            "/v1/products/" + productId,
            token,
            Map.of("name", "Ao cache MOI", "price", "100000.00"));
    assertThat(updated.status()).isEqualTo(200);

    assertThat(get("/v1/products/" + productId).body()).contains("Ao cache MOI");
    // List cũng phải tươi: nếu chỉ xoá key detail mà quên bump generation thì chỗ này đỏ.
    HttpResult list = get("/v1/products?page=0&size=50&q=ao%20cache%20moi");
    assertThat(list.status()).isEqualTo(200);
    assertThat(list.body()).contains("Ao cache MOI");
  }

  @Test
  @DisplayName("SL-4.2: 20 request đồng thời trên cùng key vừa xoá ⇒ DB chỉ bị đọc ≤ 2 lần")
  void singleFlightCollapsesConcurrentMisses() throws Exception {
    String token = sellerToken();
    UUID categoryId = createCategory(token, "Stampede IT");
    UUID productId = createProduct(token, categoryId, "Ao stampede", "1000.00");

    // Dựng cache rồi xoá đúng key ⇒ mọi request đồng thời cùng miss key đó.
    assertThat(get("/v1/products/" + productId).status()).isEqualTo(200);
    redis.delete("catalog:v1:product:" + productId);

    Map<String, Long> before = cacheCounters();
    int threads = 20;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Callable<Integer>> tasks = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        tasks.add(() -> get("/v1/products/" + productId).status());
      }
      for (Future<Integer> result : pool.invokeAll(tasks)) {
        assertThat(result.get(15, TimeUnit.SECONDS)).isEqualTo(200);
      }
    } finally {
      pool.shutdownNow();
    }

    Map<String, Long> after = cacheCounters();
    // Đây là toàn bộ ý nghĩa của single-flight: 20 request, 1 lần đọc DB (cho phép 2 vì có khe
    // double-check/lock hết hạn — trên 2 là single-flight hỏng).
    assertThat(delta(after, before, "detail.dbLoad"))
        .as("single-flight phải gộp về 1 lần đọc DB")
        .isLessThanOrEqualTo(2L);
    assertThat(delta(after, before, "detail.hit")).isGreaterThanOrEqualTo(threads - 2L);
  }

  /** Bộ đếm cache dạng phẳng từ {@code /debug/cache} (endpoint chỉ tồn tại khi debug bật). */
  @SuppressWarnings("unchecked")
  private Map<String, Long> cacheCounters() {
    HttpResult result = get("/debug/cache");
    assertThat(result.status()).as("/debug/cache phải bật trong IT").isEqualTo(200);
    Map<String, Object> body = objectMapper.readValue(result.body(), Map.class);
    Map<String, Object> counters = (Map<String, Object>) body.get("counters");
    Map<String, Long> flat = new java.util.HashMap<>();
    counters.forEach((key, value) -> flat.put(key, ((Number) value).longValue()));
    return flat;
  }

  private static long delta(Map<String, Long> after, Map<String, Long> before, String key) {
    return after.getOrDefault(key, 0L) - before.getOrDefault(key, 0L);
  }

  private String sellerToken() {
    return MINTER.token(UUID.randomUUID(), "SELLER");
  }

  private String buyerToken() {
    return MINTER.token(UUID.randomUUID(), "BUYER");
  }

  private UUID createCategory(String token, String name) {
    // SL-1.15: slug giờ bỏ dấu ("Điện thoại" → "dien-thoai") nên dễ trùng category seed
    // ("Dien thoai"). Thêm hậu tố ngẫu nhiên để slug luôn unique — đây cũng là cách test phải
    // viết: KHÔNG phụ thuộc việc slug của mình có trùng dữ liệu sẵn có hay không.
    String uniqueName = name + " " + UUID.randomUUID().toString().substring(0, 8);
    HttpResult created = post("/v1/categories", token, Map.of("name", uniqueName));
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

  private UUID createSku(String token, UUID productId, String code) {
    HttpResult created = post("/v1/products/" + productId + "/skus", token, Map.of("code", code));
    assertThat(created.status()).as("tạo SKU '%s' phải thành công", code).isEqualTo(201);
    return UUID.fromString(field(created.body(), "id"));
  }

  /** Tạo SKU có giá riêng ({@code priceOverride}) — dùng cho test batch giá (SL-2.13). */
  private UUID createSkuWithPrice(String token, UUID productId, String code, String price) {
    HttpResult created =
        post(
            "/v1/products/" + productId + "/skus",
            token,
            Map.of("code", code, "priceOverride", price));
    assertThat(created.status()).as("tạo SKU '%s' phải thành công", code).isEqualTo(201);
    return UUID.fromString(field(created.body(), "id"));
  }

  /**
   * Mã SKU unique theo từng lần chạy: {@code ux_sku_code} là UNIQUE toàn hệ, mã cố định sẽ đụng
   * nhau khi test chạy lại trên cùng container / khi nhiều test dùng cùng tiền tố.
   */
  private static String code(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
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

  /** Một field dạng mảng (vd {@code items}/{@code missingIds}) — dùng cho test batch giá. */
  @SuppressWarnings("unchecked")
  private <T> List<T> listField(String json, String name) {
    Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
    List<T> values = (List<T>) parsed.get(name);
    return values == null ? List.of() : values;
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
