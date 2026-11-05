package com.shoplab.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.identity.auth.jwt.JwtService;
import com.shoplab.identity.testsupport.JwtTestKeys;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của identity-service — chạy trên <b>Postgres thật</b> (Testcontainers).
 *
 * <p>Đây là test duy nhất trong module cần Docker: nó kiểm chứng những thứ unit test không thể —
 * Flyway migrate schema thật, JPA map đúng cột, BCrypt ghi vào DB, và luồng HTTP end-to-end
 * (register → login → token verify bằng public key).
 *
 * <p>Chạy ở phase {@code integration-test} (failsafe, tên file {@code *IT.java}) để {@code docker
 * build} bỏ qua được bằng {@code -DskipITs} — trong image build <b>không có</b> Docker socket.
 *
 * <p><b>Cố ý không dùng {@code @ServiceConnection}</b>: ở Boot 4.1.2 factory connection-details cho
 * Postgres không nằm trong {@code spring-boot-testcontainers}, và ta cũng không muốn phụ thuộc cơ
 * chế tự dò. Khai tường minh {@code @DynamicPropertySource} → đọc là biết thông tin kết nối từ đâu.
 *
 * <p><b>Cố ý không auto-skip khi thiếu Docker</b> ({@code @EnabledIfDockerAvailable}): skip im lặng
 * tạo cảm giác "đã verify" trong khi thực tế chưa chạy gì. Máy không có Docker thì dùng {@code mvn
 * verify -DskipITs} — và biết rõ là đã bỏ qua IT.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"shoplab.env=local", "shoplab.debug-endpoints=true"})
@Testcontainers
class IdentityServiceApplicationIT {

  private static final String CORRELATION_HEADER = "x-correlation-id";

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Khoá JWT sinh trong test — không phụ thuộc {@code infra/keys} (file bị gitignore). */
  private static final JwtTestKeys KEYS = JwtTestKeys.generateIn(createTempDir());

  @LocalServerPort private int port;

  @Value("${spring.application.name}")
  private String expectedServiceName;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  private static Path createTempDir() {
    try {
      return Files.createTempDirectory("identity-it-keys");
    } catch (IOException e) {
      throw new IllegalStateException("Không tạo được thư mục khoá cho test", e);
    }
  }

  @DynamicPropertySource
  static void identityProperties(DynamicPropertyRegistry registry) {
    // Datasource: trỏ vào container (Flyway + JPA dùng chính URL này).
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    registry.add("shoplab.jwt.private-key-path", () -> KEYS.privateKeyPath().toString());
    registry.add("shoplab.jwt.public-key-path", () -> KEYS.publicKeyPath().toString());

    // Readiness check TCP → trỏ vào container để /ready UP thật.
    registry.add("shoplab.readiness.dependencies[0].name", () -> "postgres");
    registry.add("shoplab.readiness.dependencies[0].host", POSTGRES::getHost);
    registry.add("shoplab.readiness.dependencies[0].port", () -> POSTGRES.getMappedPort(5432));
  }

  private HttpResponse<String> get(String path, String correlationId) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .GET();
    if (correlationId != null) {
      builder.header(CORRELATION_HEADER, correlationId);
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, Map<String, String> body) throws Exception {
    return httpClient.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static Map<String, String> registerBody(String email, String password) {
    return Map.of("email", email, "password", password, "displayName", "IT User");
  }

  @Test
  @DisplayName("GET /health → 200 (liveness, không phụ thuộc DB)")
  void healthShouldBeUp() throws Exception {
    HttpResponse<String> response = get("/health", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"");
  }

  @Test
  @DisplayName("GET /ready → 200 khi Postgres thật của container reachable")
  void readyShouldBeUpWhenDatabaseIsReachable() throws Exception {
    HttpResponse<String> response = get("/ready", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"").contains("postgres");
  }

  @Test
  @DisplayName("GET /version → tên service + git commit")
  void versionShouldExposeServiceIdentity() throws Exception {
    HttpResponse<String> response = get("/version", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains(expectedServiceName).contains("gitCommit");
  }

  @Test
  @DisplayName("x-correlation-id của client được echo nguyên vẹn; thiếu thì service tự sinh")
  void shouldHandleCorrelationId() throws Exception {
    assertThat(get("/health", "trace-it-1").headers().firstValue(CORRELATION_HEADER))
        .contains("trace-it-1");

    String generated = get("/health", null).headers().firstValue(CORRELATION_HEADER).orElse("");
    assertThat(generated).isNotBlank();
  }

  @Test
  @DisplayName("Flyway đã migrate trên Postgres thật: có flyway_schema_history + bảng users")
  void flywayShouldMigrateRealDatabase() {
    Integer applied =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
    assertThat(applied).isNotNull().isGreaterThanOrEqualTo(1);

    // Bảng chưa tồn tại thì query này ném exception → test fail (đúng ý đồ).
    Integer userCount = jdbc.queryForObject("select count(*) from users", Integer.class);
    assertThat(userCount).isNotNull().isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName(
      "path không tồn tại: KHÔNG token → 401; CÓ token → 404 body thống nhất (không thành 500)")
  void unknownPathShouldReturnUnifiedNotFoundBody() throws Exception {
    // Không token: security chặn TRƯỚC khi biết route có tồn tại hay không ⇒ 401, không phải 404.
    // Đây là hành vi có chủ ý (không cho người lạ dò danh sách route) — thay đổi từ SL-1.4.
    HttpResponse<String> anonymous = get("/khong-ton-tai", null);
    assertThat(anonymous.statusCode()).isEqualTo(401);
    assertThat(anonymous.body()).contains("\"code\":\"UNAUTHORIZED\"");

    // Có token: qua security rồi mới 404 → body lỗi thống nhất.
    Map<String, Object> session = loginNewUser(uniqueEmail(), "matkhau123");
    HttpResponse<String> response =
        getWithToken("/khong-ton-tai", (String) session.get("accessToken"));

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body()).contains("\"code\":\"NOT_FOUND\"").contains("\"traceId\"");
  }

  @Test
  @DisplayName("lỗi không lường trước → 500 và KHÔNG lộ chi tiết nội bộ")
  void unexpectedErrorShouldNotLeakInternals() throws Exception {
    HttpResponse<String> response = get("/debug/boom", null);

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.body()).contains("\"code\":\"INTERNAL_ERROR\"");
    assertThat(response.body()).doesNotContain("Chi tiết nội bộ");
  }

  /**
   * Test này tồn tại vì một bug thật ở SL-0.4: thiếu cờ {@code -parameters} của javac làm
   * {@code @RequestParam long ms} chết lúc runtime. Không có test chạm {@code /debug/slow} thì bug
   * đó chỉ lộ khi curl tay.
   */
  @Test
  @DisplayName("/debug/slow giữ request mở đúng thời gian yêu cầu")
  void debugSlowShouldHoldRequestForRequestedDuration() throws Exception {
    long startedAt = System.currentTimeMillis();

    HttpResponse<String> response = get("/debug/slow?ms=300", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(System.currentTimeMillis() - startedAt).isGreaterThanOrEqualTo(250L);
  }

  private static String uniqueEmail() {
    return "it-" + UUID.randomUUID() + "@shoplab.local";
  }

  @Test
  @DisplayName("register → 201, role BUYER, response KHÔNG chứa hash mật khẩu")
  void registerShouldCreateBuyerWithoutLeakingHash() throws Exception {
    String email = uniqueEmail();

    HttpResponse<String> response = post("/v1/auth/register", registerBody(email, "matkhau123"));

    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(response.body()).contains(email).contains("\"role\":\"BUYER\"");
    assertThat(response.body()).doesNotContain("password").doesNotContain("$2");
  }

  @Test
  @DisplayName("mật khẩu lưu dạng BCrypt trong DB thật (không plaintext, verify được)")
  void passwordShouldBeStoredAsBcryptHash() throws Exception {
    String email = uniqueEmail();
    post("/v1/auth/register", registerBody(email, "matkhau123"));

    String hash =
        jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);

    assertThat(hash).isNotNull().startsWith("$2").isNotEqualTo("matkhau123");
    assertThat(passwordEncoder.matches("matkhau123", hash)).isTrue();
    assertThat(passwordEncoder.matches("sai-mat-khau", hash)).isFalse();
  }

  @Test
  @DisplayName("email trùng → 409 CONFLICT với ApiError thống nhất")
  void duplicateEmailShouldReturnConflict() throws Exception {
    String email = uniqueEmail();
    assertThat(post("/v1/auth/register", registerBody(email, "matkhau123")).statusCode())
        .isEqualTo(201);

    HttpResponse<String> again = post("/v1/auth/register", registerBody(email, "matkhau456"));

    assertThat(again.statusCode()).isEqualTo(409);
    assertThat(again.body()).contains("\"code\":\"CONFLICT\"").contains("\"traceId\"");
  }

  @Test
  @DisplayName("email được chuẩn hoá: đăng ký hoa/thường, đăng nhập bằng chữ thường vẫn được")
  void emailShouldBeNormalized() throws Exception {
    String email = uniqueEmail();
    post("/v1/auth/register", registerBody(email.toUpperCase(Locale.ROOT), "matkhau123"));

    assertThat(
            jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, email))
        .isEqualTo(1);

    HttpResponse<String> login =
        post("/v1/auth/login", Map.of("email", email, "password", "matkhau123"));
    assertThat(login.statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("password quá ngắn → 400 VALIDATION_ERROR kèm chi tiết field")
  void shortPasswordShouldFailValidation() throws Exception {
    HttpResponse<String> response = post("/v1/auth/register", registerBody(uniqueEmail(), "123"));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body())
        .contains("\"code\":\"VALIDATION_ERROR\"")
        .contains("\"field\":\"password\"");
  }

  @Test
  @DisplayName("login → 200, token verify được bằng public key; sub = id trong DB, kid khớp JWKS")
  void loginShouldReturnVerifiableToken() throws Exception {
    String email = uniqueEmail();
    post("/v1/auth/register", registerBody(email, "matkhau123"));

    HttpResponse<String> response =
        post("/v1/auth/login", Map.of("email", email, "password", "matkhau123"));

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
    assertThat(body).containsEntry("tokenType", "Bearer");
    assertThat((String) body.get("accessToken")).isNotBlank();

    Jwt jwt =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEYS.keyPair().getPublic())
            .build()
            .decode((String) body.get("accessToken"));

    assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("BUYER");
    assertThat(jwt.getHeaders().get("kid")).isEqualTo(jwtService.keyId());

    UUID userId = jdbc.queryForObject("select id from users where email = ?", UUID.class, email);
    assertThat(jwt.getSubject()).isEqualTo(userId.toString());
  }

  @Test
  @DisplayName("login sai mật khẩu / email không tồn tại → cùng 401 UNAUTHORIZED")
  void loginWithBadCredentialsShouldReturnUnauthorized() throws Exception {
    String email = uniqueEmail();
    post("/v1/auth/register", registerBody(email, "matkhau123"));

    HttpResponse<String> wrongPassword =
        post("/v1/auth/login", Map.of("email", email, "password", "sai-mat-khau"));
    HttpResponse<String> unknownEmail =
        post("/v1/auth/login", Map.of("email", uniqueEmail(), "password", "matkhau123"));

    assertThat(wrongPassword.statusCode()).isEqualTo(401);
    assertThat(unknownEmail.statusCode()).isEqualTo(401);

    // Không được lộ email nào đã tồn tại: `code` + `message` phải GIỐNG NHAU.
    // (Không so cả body: `traceId` và `timestamp` khác nhau theo từng request là đúng thiết kế.)
    Map<String, Object> wrongBody = objectMapper.readValue(wrongPassword.body(), Map.class);
    Map<String, Object> unknownBody = objectMapper.readValue(unknownEmail.body(), Map.class);
    assertThat(wrongBody).containsEntry("code", "UNAUTHORIZED");
    assertThat(wrongBody.get("message")).isEqualTo(unknownBody.get("message"));
  }

  @Test
  @DisplayName("JWKS → 200, kid khớp token, chỉ có tham số public (không có 'd')")
  void jwksShouldExposePublicKeyOnly() throws Exception {
    HttpResponse<String> response = get("/.well-known/jwks.json", null);

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, Object> jwks = objectMapper.readValue(response.body(), Map.class);
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> keys =
        (java.util.List<Map<String, Object>>) jwks.get("keys");

    assertThat(keys).hasSize(1);
    Map<String, Object> key = keys.get(0);
    assertThat(key).containsEntry("kty", "RSA").containsEntry("kid", jwtService.keyId());
    assertThat(key).containsKeys("n", "e").doesNotContainKey("d");
  }

  // ---------------------------------------------------------------------------
  // SL-1.4 — refresh token (rotation + reuse detection), RBAC, 401/403 dạng ApiError
  // ---------------------------------------------------------------------------

  private HttpResponse<String> getWithToken(String path, String token) throws Exception {
    return httpClient.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  /** Đăng ký + đăng nhập user mới; trả body login ({@code accessToken} + {@code refreshToken}). */
  private Map<String, Object> loginNewUser(String email, String password) throws Exception {
    post("/v1/auth/register", registerBody(email, password));
    HttpResponse<String> login =
        post("/v1/auth/login", Map.of("email", email, "password", password));
    assertThat(login.statusCode()).isEqualTo(200);
    return objectMapper.readValue(login.body(), Map.class);
  }

  @Test
  @DisplayName("SL-1.4: /v1/users/me lấy danh tính từ TOKEN (không cần header của gateway)")
  void meShouldReturnIdentityFromToken() throws Exception {
    Map<String, Object> session = loginNewUser(uniqueEmail(), "matkhau123");

    HttpResponse<String> response =
        getWithToken("/v1/users/me", (String) session.get("accessToken"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"role\":\"BUYER\"");
  }

  @Test
  @DisplayName("SL-1.4: thiếu/sai token → 401 dạng ApiError (JSON, có traceId — không phải HTML)")
  void missingOrBadTokenShouldReturnApiError() throws Exception {
    HttpResponse<String> missing = get("/v1/users/me", null);
    assertThat(missing.statusCode()).isEqualTo(401);
    assertThat(missing.body()).contains("\"code\":\"UNAUTHORIZED\"").contains("\"traceId\"");

    HttpResponse<String> garbage = getWithToken("/v1/users/me", "khong-phai-jwt");
    assertThat(garbage.statusCode()).isEqualTo(401);
    assertThat(garbage.body()).contains("\"code\":\"UNAUTHORIZED\"");
  }

  @Test
  @DisplayName(
      "SL-1.4: refresh → cặp token MỚI dùng được; dùng lại token cũ → 401 và cả chuỗi bị thu hồi")
  void refreshShouldRotateAndDetectReuse() throws Exception {
    Map<String, Object> session = loginNewUser(uniqueEmail(), "matkhau123");
    String refresh1 = (String) session.get("refreshToken");

    HttpResponse<String> refreshed = post("/v1/auth/refresh", Map.of("refreshToken", refresh1));
    assertThat(refreshed.statusCode()).isEqualTo(200);
    Map<String, Object> rotated = objectMapper.readValue(refreshed.body(), Map.class);
    String refresh2 = (String) rotated.get("refreshToken");
    assertThat(refresh2).isNotEqualTo(refresh1);

    // Access token mới phải dùng được ngay.
    assertThat(getWithToken("/v1/users/me", (String) rotated.get("accessToken")).statusCode())
        .isEqualTo(200);

    // Dùng LẠI token đã rotate ⇒ 401 (phát hiện bị đánh cắp) ...
    HttpResponse<String> reuse = post("/v1/auth/refresh", Map.of("refreshToken", refresh1));
    assertThat(reuse.statusCode()).isEqualTo(401);
    assertThat(reuse.body()).contains("\"code\":\"UNAUTHORIZED\"");

    // ... và cả chuỗi bị thu hồi: token mới nhất cũng chết.
    assertThat(post("/v1/auth/refresh", Map.of("refreshToken", refresh2)).statusCode())
        .isEqualTo(401);
  }

  @Test
  @DisplayName("SL-1.4: DB chỉ lưu HASH của refresh token — không thấy token thật")
  void refreshTokenShouldBeStoredAsHash() throws Exception {
    String email = uniqueEmail();
    Map<String, Object> session = loginNewUser(email, "matkhau123");
    String rawRefresh = (String) session.get("refreshToken");

    var hashes =
        jdbc.queryForList(
            "select rt.token_hash from refresh_tokens rt join users u on u.id = rt.user_id"
                + " where u.email = ?",
            String.class,
            email);

    assertThat(hashes).isNotEmpty().doesNotContain(rawRefresh);
    assertThat(hashes).allMatch(hash -> hash.length() == 64);
  }

  @Test
  @DisplayName("SL-1.4: logout → 204, refresh token đó không dùng được nữa")
  void logoutShouldInvalidateRefreshToken() throws Exception {
    Map<String, Object> session = loginNewUser(uniqueEmail(), "matkhau123");
    String refresh = (String) session.get("refreshToken");

    HttpResponse<String> logout = post("/v1/auth/logout", Map.of("refreshToken", refresh));
    assertThat(logout.statusCode()).isEqualTo(204);

    assertThat(post("/v1/auth/refresh", Map.of("refreshToken", refresh)).statusCode())
        .isEqualTo(401);
  }

  @Test
  @DisplayName("SL-1.4: /v1/admin/users — admin 200, buyer 403 (403 chứ KHÔNG phải 401)")
  void adminEndpointShouldDistinguishRole() throws Exception {
    // admin seed sẵn khi shoplab.env=local (LocalDevUserSeeder) — mật khẩu dev giống README.
    HttpResponse<String> adminLogin =
        post("/v1/auth/login", Map.of("email", "admin@shoplab.local", "password", "matkhau123"));
    assertThat(adminLogin.statusCode()).isEqualTo(200);
    String adminToken =
        (String) objectMapper.readValue(adminLogin.body(), Map.class).get("accessToken");

    assertThat(getWithToken("/v1/admin/users", adminToken).statusCode()).isEqualTo(200);

    Map<String, Object> buyer = loginNewUser(uniqueEmail(), "matkhau123");
    HttpResponse<String> forbidden =
        getWithToken("/v1/admin/users", (String) buyer.get("accessToken"));

    assertThat(forbidden.statusCode()).isEqualTo(403);
    assertThat(forbidden.body()).contains("\"code\":\"FORBIDDEN\"");
  }
}
