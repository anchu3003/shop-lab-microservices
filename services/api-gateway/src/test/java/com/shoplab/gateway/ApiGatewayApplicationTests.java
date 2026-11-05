package com.shoplab.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Test end-to-end của service template: dựng thật Tomcat trên port ngẫu nhiên, gọi HTTP thật.
 *
 * <p>Vì sao không dùng Postgres/Redis thật: test phải chạy được trong CI (SL-0.5) mà CI không có hạ
 * tầng → readiness trỏ vào một port mở sẵn do test tự tạo. Nhờ vậy test vẫn kiểm chứng đúng đường
 * đi (filter → controller → readiness check → response) mà không phụ thuộc môi trường.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      // Gateway fail-fast nếu không có route → khai 1 route trỏ vào port đóng; test này chỉ kiểm
      // tra
      // hạ tầng (health/ready/version/debug), không gọi route nào.
      "shoplab.gateway.routes[0].path-prefix=/v1/auth",
      "shoplab.gateway.routes[0].base-url=http://127.0.0.1:1"
    })
class ApiGatewayApplicationTests {

  private static final String CORRELATION_HEADER = "x-correlation-id";

  /** Giữ socket mở suốt test để readiness check chắc chắn UP. */
  private static final ServerSocket READINESS_STUB = openServer();

  @LocalServerPort private int port;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  @DynamicPropertySource
  static void readinessStubProperties(DynamicPropertyRegistry registry) {
    registry.add("shoplab.readiness.dependencies[0].name", () -> "stub");
    registry.add("shoplab.readiness.dependencies[0].host", () -> "127.0.0.1");
    registry.add("shoplab.readiness.dependencies[0].port", () -> READINESS_STUB.getLocalPort());
    registry.add("shoplab.readiness.dependencies[0].timeout", () -> "500ms");
  }

  private static ServerSocket openServer() {
    try {
      return new ServerSocket(0);
    } catch (IOException e) {
      throw new IllegalStateException("Không mở được port cho readiness stub", e);
    }
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

  @Test
  @DisplayName("GET /health → 200 (liveness)")
  void healthShouldBeUp() throws Exception {
    HttpResponse<String> response = get("/health", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"");
  }

  @Test
  @DisplayName("GET /ready → 200 khi dependency OK, kèm chi tiết từng dependency")
  void readyShouldBeUpWhenDependenciesAreReachable() throws Exception {
    HttpResponse<String> response = get("/ready", null);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"").contains("stub");
  }

  @Test
  @DisplayName("endpoint hạ tầng của gateway KHÔNG nằm trong public-paths → 401 (policy SL-1.2)")
  void infrastructureEndpointsShouldRequireToken() throws Exception {
    // /health và /ready là public (probe), còn /version + /debug/* thì KHÔNG: policy của ticket là
    // "public = /v1/auth/**, /.well-known/**, /health, /ready; còn lại bắt buộc token".
    // Trường hợp CÓ token hợp lệ được kiểm tra ở GatewayRoutingTest (nơi có JWKS thật để verify).
    for (String path :
        new String[] {
          "/version", "/debug/slow?ms=1", "/debug/error", "/debug/boom", "/khong-ton-tai"
        }) {
      HttpResponse<String> response = get(path, null);

      assertThat(response.statusCode()).as("path %s", path).isEqualTo(401);
      assertThat(response.body()).as("path %s", path).contains("\"code\":\"UNAUTHORIZED\"");
    }
  }

  @Test
  @DisplayName("x-correlation-id của client được echo lại nguyên vẹn")
  void shouldEchoClientCorrelationId() throws Exception {
    HttpResponse<String> response = get("/health", "test-correlation-123");

    assertThat(response.headers().firstValue(CORRELATION_HEADER)).contains("test-correlation-123");
  }
}
