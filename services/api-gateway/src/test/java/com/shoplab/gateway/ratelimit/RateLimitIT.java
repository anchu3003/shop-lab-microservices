package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.gateway.testsupport.JwtFixture;
import com.shoplab.gateway.testsupport.UpstreamStub;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rate limit qua HTTP thật: gateway thật + Redis thật (container) + service đích giả.
 *
 * <p>Ngưỡng được hạ xuống rất nhỏ (burst 2–3) để test <b>không</b> phải bắn hàng trăm request: điều
 * cần chứng minh là <i>cơ chế</i> (bucket đầy ⇒ 429, hai user hai bucket), không phải con số của
 * môi trường thật — con số thật đo bằng {@code tools/rate-limit-drill.sh} ở tầng hệ.
 *
 * <p>Đây là test thay cho "verify bằng mắt": nếu ai đó đảo thứ tự filter (rate limit chạy
 * <b>trước</b> verify token) thì {@code twoUsersShouldNotShareBucket} sẽ đỏ ngay — vì khi đó không
 * còn {@code x-user-id} để đặt key và cả hai user rơi vào bucket IP.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitIT {

  private static final JwtFixture FIXTURE = new JwtFixture();
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static UpstreamStub upstream;

  @LocalServerPort private int port;

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) throws IOException {
    upstream = new UpstreamStub(FIXTURE.jwksJson());

    registry.add("shoplab.env", () -> "local");
    registry.add("shoplab.debug-endpoints", () -> "true");

    registry.add("shoplab.gateway.routes[0].path-prefix", () -> "/v1/users");
    registry.add("shoplab.gateway.routes[0].base-url", upstream::baseUrl);
    registry.add("shoplab.gateway.routes[1].path-prefix", () -> "/v1/products");
    registry.add("shoplab.gateway.routes[1].base-url", upstream::baseUrl);
    registry.add("shoplab.gateway.routes[2].path-prefix", () -> "/.well-known");
    registry.add("shoplab.gateway.routes[2].base-url", upstream::baseUrl);

    registry.add("shoplab.gateway.public-paths[0]", () -> "/v1/products/**");
    registry.add("shoplab.gateway.public-paths[1]", () -> "/.well-known/**");
    // `/health` phải public như production: probe sống/chết mà đòi token thì không ai dùng được.
    registry.add("shoplab.gateway.public-paths[2]", () -> "/health");

    registry.add(
        "shoplab.gateway.jwt.jwks-uri", () -> upstream.baseUrl() + "/.well-known/jwks.json");
    registry.add("shoplab.gateway.jwt.issuer", () -> JwtFixture.ISSUER);
    registry.add("shoplab.gateway.timeouts.connect", () -> "1s");
    registry.add("shoplab.gateway.timeouts.read", () -> "2s");

    // Rate limit: hạn mức 1/1 để test TẤT ĐỊNH. Đặt 2/2 rồi gửi tuần tự là tự tạo flaky: trên
    // runner
    // chậm mỗi request ~300 ms, xô nạp 2 token/s ⇒ request thứ 3 vẫn có token ⇒ "không thấy 429"
    // (đã
    // đỏ thật trên CI). 1/1 + bắn ĐỒNG THỜI ⇒ đúng 1 request lọt, và kiểm luôn tính nguyên tử của
    // EVAL.
    registry.add("shoplab.gateway.rate-limit.default-per-user.rps", () -> "1");
    registry.add("shoplab.gateway.rate-limit.default-per-user.burst", () -> "1");
    registry.add("shoplab.gateway.rate-limit.default-per-ip.rps", () -> "1000");
    registry.add("shoplab.gateway.rate-limit.default-per-ip.burst", () -> "1000");
    registry.add("shoplab.gateway.rate-limit.routes[0].path-prefix", () -> "/v1/products");
    registry.add("shoplab.gateway.rate-limit.routes[0].per-ip.rps", () -> "1");
    registry.add("shoplab.gateway.rate-limit.routes[0].per-ip.burst", () -> "1");

    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  private HttpResponse<String> send(String path, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .GET();
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Bắn {@code count} request <b>đồng thời</b> và trả danh sách status.
   *
   * <p>Đồng thời là điều kiện để test <b>tất định</b>: hạn mức 1 token mà gửi tuần tự thì xô kịp
   * nạp lại giữa hai request (300 ms/request trên runner chậm ⇒ 0,3 token) nên kết quả phụ thuộc
   * tốc độ máy — đã đỏ thật trên CI. Bắn song song thì cả loạt cùng thấy "còn 1 token" ⇒ đúng 1
   * request lọt, và đó chính là tính chất nguyên tử mà {@code EVAL} phải bảo đảm.
   */
  private List<Integer> sendConcurrently(String path, String token, int count) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(count);
    try {
      List<Future<HttpResponse<String>>> futures = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        futures.add(pool.submit(() -> send(path, token)));
      }
      List<Integer> statuses = new ArrayList<>(count);
      for (Future<HttpResponse<String>> future : futures) {
        statuses.add(future.get(20, TimeUnit.SECONDS).statusCode());
      }
      return statuses;
    } finally {
      pool.shutdownNow();
    }
  }

  private static long countOf(List<Integer> statuses, int status) {
    return statuses.stream().filter(code -> code == status).count();
  }

  @Test
  @DisplayName(
      "đọc public vượt burst ⇒ 429 tại biên, có Retry-After + X-RateLimit-* + body ApiError")
  void publicReadShouldBeThrottledAtTheEdge() throws Exception {
    // 6 request song song, hạn mức 1 token ⇒ đúng 1 lọt (EVAL không nguyên tử thì sẽ lọt nhiều
    // hơn).
    List<Integer> statuses = sendConcurrently("/v1/products?page=0", null, 6);
    assertThat(countOf(statuses, 200)).as("200 trong %s", statuses).isEqualTo(1);
    assertThat(countOf(statuses, 429)).as("429 trong %s", statuses).isEqualTo(5);

    HttpResponse<String> rejected = send("/v1/products?page=0", null);

    assertThat(rejected.statusCode()).isEqualTo(429);
    assertThat(rejected.headers().firstValue("Retry-After")).isPresent();
    assertThat(rejected.headers().firstValue("X-RateLimit-Remaining")).contains("0");
    assertThat(rejected.headers().firstValue("X-RateLimit-Limit")).contains("1");

    JsonNode body = MAPPER.readTree(rejected.body());
    assertThat(body.get("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
    // traceId là thứ để user báo lỗi và dev grep log — 429 cũng phải có.
    assertThat(body.get("traceId").asString()).isNotBlank();
    assertThat(body.get("path").asString()).isEqualTo("/v1/products");
  }

  @Test
  @DisplayName("hai user KHÁC nhau không dùng chung bucket: A cạn ⇒ B vẫn 200")
  void twoUsersShouldNotShareBucket() throws Exception {
    String tokenA = FIXTURE.validToken("buyer-a", "BUYER");
    String tokenB = FIXTURE.validToken("buyer-b", "BUYER");

    List<Integer> statuses = sendConcurrently("/v1/users/me", tokenA, 6);
    assertThat(countOf(statuses, 200)).as("A 200 trong %s", statuses).isEqualTo(1);
    assertThat(countOf(statuses, 429)).as("A 429 trong %s", statuses).isEqualTo(5);

    HttpResponse<String> blockedA = send("/v1/users/me", tokenA);
    assertThat(blockedA.statusCode()).isEqualTo(429);
    assertThat(MAPPER.readTree(blockedA.body()).get("code").asString())
        .isEqualTo("TOO_MANY_REQUESTS");

    // Đây là AC "không chặn oan": user khác còn nguyên hạn mức dù cùng IP.
    assertThat(send("/v1/users/me", tokenB).statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("/health không bao giờ 429 (probe chết vì rate limit = tự khai tử)")
  void healthShouldNeverBeThrottled() throws Exception {
    for (int i = 0; i < 10; i++) {
      assertThat(send("/health", null).statusCode()).isEqualTo(200);
    }
  }

  @Test
  @DisplayName("/debug/rate-limit cho biết limiter đang bật + bộ đếm thật (drill dựa vào đây)")
  void debugEndpointShouldExposeCounters() throws Exception {
    String token = FIXTURE.validToken("admin-1", "ADMIN");

    HttpResponse<String> response = send("/debug/rate-limit", token);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode body = MAPPER.readTree(response.body());
    assertThat(body.get("enabled").asBoolean()).isTrue();
    assertThat(body.get("defaultPerUser").get("burst").asInt()).isEqualTo(1);
    assertThat(body.get("counters").get("allowed").asLong()).isGreaterThanOrEqualTo(0);
  }
}
