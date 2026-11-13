package com.shoplab.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.gateway.testsupport.JwtFixture;
import com.shoplab.gateway.testsupport.UpstreamStub;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * Test end-to-end của gateway: HTTP thật vào gateway → HTTP thật ra service đích (stub).
 *
 * <p><b>Không cần Docker</b>: service đích là {@code HttpServer} của JDK ({@link UpstreamStub}) —
 * nó công bố JWKS và echo lại header, nhờ vậy test chứng minh được đúng thứ quan trọng nhất của
 * gateway: <i>token có được verify không</i> và <i>header danh tính có bị ghi đè không</i>.
 *
 * <p>Tích hợp với identity-service thật (Postgres + JWKS thật) là việc của E2E smoke (SL-1.3) —
 * tách ra để khi test đỏ thì biết ngay lỗi thuộc gateway hay thuộc identity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

  private static final String CORRELATION_HEADER = "x-correlation-id";
  private static final JwtFixture FIXTURE = new JwtFixture();
  private static final UpstreamStub UPSTREAM = startUpstream();
  private static final int CLOSED_PORT = closedPort();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @LocalServerPort private int port;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  private static UpstreamStub startUpstream() {
    try {
      return new UpstreamStub(FIXTURE.jwksJson());
    } catch (IOException e) {
      throw new IllegalStateException("Không dựng được upstream giả", e);
    }
  }

  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("Không lấy được port đóng", e);
    }
  }

  @DynamicPropertySource
  static void gatewayProperties(DynamicPropertyRegistry registry) {
    registry.add("shoplab.env", () -> "local");
    registry.add("shoplab.debug-endpoints", () -> "true");
    // 3 route đầu trỏ vào stub; route cuối trỏ vào port đóng để test 503.
    addRoute(registry, 0, "/v1/auth", UPSTREAM.baseUrl());
    addRoute(registry, 1, "/v1/users", UPSTREAM.baseUrl());
    addRoute(registry, 2, "/.well-known", UPSTREAM.baseUrl());
    addRoute(registry, 3, "/v1/down", "http://127.0.0.1:" + CLOSED_PORT);

    registry.add(
        "shoplab.gateway.jwt.jwks-uri", () -> UPSTREAM.baseUrl() + "/.well-known/jwks.json");
    registry.add("shoplab.gateway.jwt.issuer", () -> JwtFixture.ISSUER);
    registry.add("shoplab.gateway.timeouts.connect", () -> "1s");
    registry.add("shoplab.gateway.timeouts.read", () -> "2s");
    // SL-4.3: test này kiểm routing/auth, KHÔNG kiểm rate limit. Để bật thì mỗi request sẽ thử nói
    // chuyện Redis (không có trong test này) và fail-open kèm timeout 100 ms × N — chậm mà không
    // kiểm thêm được gì. Rate limit có IT riêng (RateLimitIT).
    registry.add("shoplab.gateway.rate-limit.enabled", () -> "false");
  }

  private static void addRoute(
      DynamicPropertyRegistry registry, int index, String prefix, String baseUrl) {
    registry.add("shoplab.gateway.routes[" + index + "].path-prefix", () -> prefix);
    registry.add("shoplab.gateway.routes[" + index + "].base-url", () -> baseUrl);
  }

  private HttpResponse<String> send(
      String method, String path, String token, String correlationId, String body)
      throws Exception {

    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    if (correlationId != null) {
      builder.header(CORRELATION_HEADER, correlationId);
    }
    if (body != null) {
      builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private Map<String, Object> jsonOf(HttpResponse<String> response) throws Exception {
    return MAPPER.readValue(response.body(), Map.class);
  }

  private static String validToken() {
    return FIXTURE.validToken(UUID.randomUUID().toString(), "BUYER");
  }

  @Test
  @DisplayName("route public: không cần token, nhưng header danh tính client gửi phải bị XOÁ")
  void publicRouteShouldPassWithoutTokenAndStripIdentityHeaders() throws Exception {
    String body = "{\"email\":\"a@b.local\",\"password\":\"matkhau123\"}";

    HttpResponse<String> response =
        httpClient.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/v1/auth/login?utm=abc"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                // Client tự khai danh tính → gateway phải xoá trước khi chuyển tiếp.
                .header("x-user-id", "ke-gia-danh")
                .header("x-user-role", "ADMIN")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, Object> echo = jsonOf(response);
    assertThat(echo)
        .containsEntry("method", "POST")
        .containsEntry("path", "/v1/auth/login")
        .containsEntry("query", "utm=abc")
        .containsEntry("body", body);
    assertThat(echo.get("userId")).isNull();
    assertThat(echo.get("role")).isNull();
  }

  @Test
  @DisplayName(
      "route protected: không token → 401 ApiError (code + traceId), không gọi service đích")
  void protectedRouteWithoutTokenShouldReturn401() throws Exception {
    HttpResponse<String> response = send("GET", "/v1/users/me", null, null, null);

    assertThat(response.statusCode()).isEqualTo(401);
    Map<String, Object> error = jsonOf(response);
    assertThat(error).containsEntry("code", "UNAUTHORIZED").containsEntry("path", "/v1/users/me");
    assertThat((String) error.get("traceId")).isNotBlank();
    assertThat((String) error.get("message")).contains("token");
  }

  @Test
  @DisplayName("token hợp lệ → 200 và service đích NHẬN được x-user-id/x-user-role do gateway gắn")
  void protectedRouteWithValidTokenShouldForwardIdentity() throws Exception {
    String userId = UUID.randomUUID().toString();

    HttpResponse<String> response =
        send("GET", "/v1/users/me", FIXTURE.validToken(userId, "BUYER"), null, null);

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, Object> echo = jsonOf(response);
    assertThat(echo).containsEntry("userId", userId).containsEntry("role", "BUYER");
    // Token gốc vẫn được chuyển tiếp — SL-1.5 sẽ verify lại ở tầng service (defense in depth).
    assertThat((String) echo.get("authorization")).startsWith("Bearer ");
  }

  @Test
  @DisplayName("token hết hạn / ký bằng khoá khác / alg=HS256 → 401, không lộ chi tiết nội bộ")
  void invalidTokensShouldReturn401() throws Exception {
    String userId = UUID.randomUUID().toString();

    assertThat(
            send("GET", "/v1/users/me", FIXTURE.expiredToken(userId, "BUYER"), null, null)
                .statusCode())
        .isEqualTo(401);
    assertThat(
            send(
                    "GET",
                    "/v1/users/me",
                    FIXTURE.tokenSignedByAnotherKey(userId, "BUYER"),
                    null,
                    null)
                .statusCode())
        .isEqualTo(401);

    HttpResponse<String> hs256 =
        send("GET", "/v1/users/me", FIXTURE.hs256Token(userId, "ADMIN"), null, null);
    assertThat(hs256.statusCode()).isEqualTo(401);
    assertThat(hs256.body()).contains("UNAUTHORIZED").doesNotContain("Exception");
  }

  @Test
  @DisplayName("x-correlation-id của client được giữ nguyên và truyền xuống service đích")
  void clientCorrelationIdShouldBePreserved() throws Exception {
    HttpResponse<String> response = send("GET", "/v1/users/me", validToken(), "e2e-trace-42", null);

    assertThat(response.headers().firstValue(CORRELATION_HEADER)).contains("e2e-trace-42");
    assertThat(jsonOf(response)).containsEntry("correlationId", "e2e-trace-42");
  }

  @Test
  @DisplayName("client không gửi correlation id → gateway sinh mới, 2 bên dùng CÙNG một giá trị")
  void generatedCorrelationIdShouldBeSharedWithUpstream() throws Exception {
    HttpResponse<String> response = send("GET", "/v1/users/me", validToken(), null, null);

    String correlationId = response.headers().firstValue(CORRELATION_HEADER).orElse("");
    assertThat(correlationId).isNotBlank();
    assertThat(jsonOf(response)).containsEntry("correlationId", correlationId);
  }

  @Test
  @DisplayName("path không có route → 404 ApiError (chỉ sau khi đã xác thực)")
  void unknownRouteShouldReturn404() throws Exception {
    HttpResponse<String> withoutToken = send("GET", "/v2/khong-co", null, null, null);
    HttpResponse<String> withToken = send("GET", "/v2/khong-co", validToken(), null, null);

    // Không lộ thông tin route cho client chưa xác thực.
    assertThat(withoutToken.statusCode()).isEqualTo(401);
    assertThat(withToken.statusCode()).isEqualTo(404);
    assertThat(jsonOf(withToken)).containsEntry("code", "NOT_FOUND");
  }

  @Test
  @DisplayName("service đích chết → 503 ApiError, gateway KHÔNG sập")
  void upstreamDownShouldReturn503() throws Exception {
    HttpResponse<String> response = send("GET", "/v1/down/anything", validToken(), null, null);

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(jsonOf(response)).containsEntry("code", "UNAVAILABLE");
  }

  @Test
  @DisplayName("service đích trả 5xx → gateway giữ NGUYÊN status + body (không bọc lại)")
  void upstreamErrorShouldPassThroughUnchanged() throws Exception {
    HttpResponse<String> response = send("GET", "/v1/users/boom", validToken(), null, null);

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(response.body())
        .isEqualTo("{\"code\":\"INTERNAL_ERROR\",\"message\":\"upstream exploded\"}");
  }

  @Test
  @DisplayName("endpoint hạ tầng của gateway: 401 khi thiếu token, chạy ĐÚNG khi có token")
  void gatewayOwnEndpointsShouldWorkWithToken() throws Exception {
    String token = validToken();

    // Các endpoint này do gateway tự phục vụ (không route đi đâu) — catch-all `/**` không được nuốt
    // chúng.
    HttpResponse<String> version = send("GET", "/version", token, null, null);
    assertThat(version.statusCode()).isEqualTo(200);
    assertThat(version.body()).contains("api-gateway").contains("gitCommit");

    assertThat(send("GET", "/debug/slow?ms=1", token, null, null).statusCode()).isEqualTo(200);

    HttpResponse<String> boom = send("GET", "/debug/boom", token, null, null);
    assertThat(boom.statusCode()).isEqualTo(500);
    assertThat(boom.body()).contains("INTERNAL_ERROR").doesNotContain("Chi tiết nội bộ");

    HttpResponse<String> notFound = send("GET", "/debug/error", token, null, null);
    assertThat(notFound.statusCode()).isEqualTo(404);
    assertThat(notFound.body()).contains("NOT_FOUND");
  }

  @Test
  @DisplayName("JWKS đi qua gateway không cần token (client chỉ cần biết 1 địa chỉ)")
  void jwksShouldBeReachableWithoutToken() throws Exception {
    HttpResponse<String> response = send("GET", "/.well-known/jwks.json", null, null, null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains(JwtFixture.KID);
  }
}
