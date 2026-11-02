package com.shoplab.template;

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
import org.springframework.beans.factory.annotation.Value;
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
    properties = {"shoplab.env=local", "shoplab.debug-endpoints=true"})
class ServiceTemplateApplicationTests {

  private static final String CORRELATION_HEADER = "x-correlation-id";

  /** Giữ socket mở suốt test để readiness check chắc chắn UP. */
  private static final ServerSocket READINESS_STUB = openServer();

  @LocalServerPort private int port;

  /**
   * Tên service đọc từ config — template không hard-code để copy sang service mới là chạy được
   * ngay.
   */
  @Value("${spring.application.name}")
  private String expectedServiceName;

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
  @DisplayName("GET /version → có tên service và git commit")
  void versionShouldExposeServiceIdentity() throws Exception {
    HttpResponse<String> response = get("/version", null);

    assertThat(response.statusCode()).isEqualTo(200);
    // KHÔNG hard-code tên service: template phải copy được sang service mới mà không phải sửa test.
    assertThat(response.body()).contains(expectedServiceName).contains("gitCommit");
  }

  @Test
  @DisplayName("x-correlation-id của client được echo lại nguyên vẹn")
  void shouldEchoClientCorrelationId() throws Exception {
    HttpResponse<String> response = get("/health", "test-correlation-123");

    assertThat(response.headers().firstValue(CORRELATION_HEADER)).contains("test-correlation-123");
  }

  @Test
  @DisplayName("thiếu header → service tự sinh correlation id, không để trống")
  void shouldGenerateCorrelationIdWhenMissing() throws Exception {
    HttpResponse<String> response = get("/health", null);

    assertThat(response.headers().firstValue(CORRELATION_HEADER)).isPresent();
    assertThat(response.headers().firstValue(CORRELATION_HEADER).orElse("")).isNotBlank();
  }

  @Test
  @DisplayName("response lỗi nghiệp vụ → có code + traceId lấy từ header của client")
  void errorResponseShouldCarryTraceId() throws Exception {
    HttpResponse<String> response = get("/debug/error", "trace-abc-999");

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body())
        .contains("\"code\":\"NOT_FOUND\"")
        .contains("\"traceId\":\"trace-abc-999\"");
  }

  @Test
  @DisplayName("path không tồn tại → 404 với body thống nhất, KHÔNG bị biến thành 500")
  void unknownPathShouldReturnUnifiedNotFoundBody() throws Exception {
    HttpResponse<String> response = get("/khong-ton-tai", null);

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
   * Test này tồn tại vì một bug thật: thiếu cờ {@code -parameters} của javac làm
   * {@code @RequestParam long ms} chết lúc runtime với "Name for argument ... not specified". Không
   * có test chạm {@code /debug/slow} thì bug đó chỉ lộ ra khi verify tay bằng curl.
   */
  @Test
  @DisplayName("/debug/slow giữ request mở đúng thời gian yêu cầu")
  void debugSlowShouldHoldRequestForRequestedDuration() throws Exception {
    long startedAt = System.currentTimeMillis();

    HttpResponse<String> response = get("/debug/slow?ms=300", null);

    long elapsedMs = System.currentTimeMillis() - startedAt;
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("300");
    assertThat(elapsedMs).isGreaterThanOrEqualTo(250L);
  }
}
