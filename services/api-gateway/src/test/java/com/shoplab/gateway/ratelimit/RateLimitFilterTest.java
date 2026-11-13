package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.log.MdcKeys;
import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;
import com.shoplab.gateway.ratelimit.RateLimitProperties.Rule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Quyết định của filter: cho qua / 429 / fail-open — cùng header và body đi kèm.
 *
 * <p>Kiểm ở đây chứ <b>không</b> qua Redis: thứ cần chứng minh là <i>quyết định</i>. Thuật toán
 * token bucket thật kiểm ở {@code RedisTokenBucketIT} (Redis thật — Lua không mock được), cú pháp
 * HTTP thật kiểm ở {@code RateLimitIT}.
 */
class RateLimitFilterTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"), ZoneOffset.UTC);
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private static final Limit USER_LIMIT = new Limit(10, 20);
  private static final Limit IP_LIMIT = new Limit(100, 200);

  @AfterEach
  void clearMdc() {
    MDC.remove(MdcKeys.TRACE_ID);
  }

  private static RateLimitProperties properties(boolean enabled, boolean failOpen) {
    return new RateLimitProperties(
        enabled,
        failOpen,
        "rl",
        USER_LIMIT,
        IP_LIMIT,
        List.of("/health"),
        List.of(),
        List.of(new Rule("/v1/checkout", "POST", USER_LIMIT, IP_LIMIT)));
  }

  private static RateLimitFilter filter(RateLimitProperties properties, RateLimiter limiter) {
    return new RateLimitFilter(
        properties,
        new RateLimitRules(properties),
        limiter,
        new ClientIpResolver(properties),
        new RateLimitMetrics(),
        CLOCK,
        MAPPER);
  }

  private static MockHttpServletRequest request(String method, String path) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRequestURI(path);
    request.setRemoteAddr("10.0.0.5");
    return request;
  }

  @Test
  @DisplayName("còn token ⇒ đi tiếp + đủ 3 header X-RateLimit-* (limit/remaining/reset)")
  void allowedShouldPassWithHeaders() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter(
            properties(true, true),
            (userKey, userLimit, ipKey, ipLimit, now) ->
                new RateLimitDecision(true, RateLimitDecision.Bucket.IP, 200, 199, 0))
        .doFilter(request("GET", "/v1/products"), response, chain);

    assertThat(chain.getRequest()).as("chain phải được gọi").isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("200");
    assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("199");
    // Thiếu 1 token ở 200 rps ⇒ đầy lại trong 5 ms, nhưng header tính bằng GIÂY và làm tròn LÊN
    // (client chờ 1 s là chắc chắn có token; làm tròn xuống có thể khiến client retry sớm 4 ms
    // nữa).
    assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1794528001");
  }

  @Test
  @DisplayName("hết token ⇒ 429 + Retry-After + body ApiError (code + traceId), chain KHÔNG chạy")
  void rejectedShouldReturn429AndNeverTouchUpstream() throws Exception {
    MDC.put(MdcKeys.TRACE_ID, "trace-429");
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter(
            properties(true, true),
            (userKey, userLimit, ipKey, ipLimit, now) ->
                new RateLimitDecision(false, RateLimitDecision.Bucket.USER, 20, 0, 1_600))
        .doFilter(request("POST", "/v1/checkout"), response, chain);

    // Đây là cả mục đích của ticket: 429 trả ở biên nên service đích không nhận request nào.
    assertThat(chain.getRequest()).as("chain KHÔNG được gọi").isNull();
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("2");
    assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

    JsonNode body = MAPPER.readTree(response.getContentAsString());
    assertThat(body.get("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
    assertThat(body.get("traceId").asString()).isEqualTo("trace-429");
    assertThat(body.get("path").asString()).isEqualTo("/v1/checkout");
  }

  @Test
  @DisplayName("path hạ tầng (/health) không bị đếm — 429 ở đây là tự khai tử readiness")
  void bypassPathShouldNotCallLimiter() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter(
            properties(true, true),
            (userKey, userLimit, ipKey, ipLimit, now) -> {
              throw new AssertionError("không được gọi limiter cho path bypass");
            })
        .doFilter(request("GET", "/health"), response, chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("enabled=false ⇒ không chạm Redis lần nào (công tắc cho load test + ca sự cố)")
  void disabledShouldSkipLimiter() throws Exception {
    MockFilterChain chain = new MockFilterChain();

    filter(
            properties(false, true),
            (userKey, userLimit, ipKey, ipLimit, now) -> {
              throw new AssertionError("không được gọi limiter khi đã tắt");
            })
        .doFilter(request("GET", "/v1/products"), new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  @DisplayName("Redis lỗi + fail-open=true ⇒ cho qua + đếm failOpen (KHÔNG chặn cả hệ)")
  void redisFailureShouldFailOpen() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    RateLimitMetrics metrics = new RateLimitMetrics();
    RateLimitProperties properties = properties(true, true);

    new RateLimitFilter(
            properties,
            new RateLimitRules(properties),
            (userKey, userLimit, ipKey, ipLimit, now) -> {
              throw new RateLimitUnavailableException("redis chết", null);
            },
            new ClientIpResolver(properties),
            metrics,
            CLOCK,
            MAPPER)
        .doFilter(request("GET", "/v1/products"), new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).as("fail-open phải cho request đi").isNotNull();
    assertThat(metrics.snapshot()).containsEntry("failOpen", 1L).containsEntry("allowed", 0L);
  }

  @Test
  @DisplayName("Redis lỗi + fail-open=false ⇒ 503 UNAVAILABLE (chọn 'thà chặn còn hơn nhận tải')")
  void redisFailureShouldReturn503WhenFailOpenDisabled() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter(
            properties(true, false),
            (userKey, userLimit, ipKey, ipLimit, now) -> {
              throw new RateLimitUnavailableException("redis chết", null);
            })
        .doFilter(request("GET", "/v1/products"), response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString()).contains("UNAVAILABLE");
  }

  @Test
  @DisplayName("key theo user CHỈ khi có danh tính đã verify; khách không có x-user-id ⇒ chỉ xô IP")
  void userBucketShouldOnlyUseVerifiedIdentity() throws Exception {
    AtomicReference<String> seenUserKey = new AtomicReference<>();
    AtomicReference<String> seenIpKey = new AtomicReference<>();
    RateLimiter recording =
        (userKey, userLimit, ipKey, ipLimit, now) -> {
          seenUserKey.set(userKey);
          seenIpKey.set(ipKey);
          return new RateLimitDecision(true, RateLimitDecision.Bucket.IP, 200, 199, 0);
        };
    RateLimitFilter filter = filter(properties(true, true), recording);

    filter.doFilter(
        request("GET", "/v1/products"), new MockHttpServletResponse(), new MockFilterChain());
    assertThat(seenUserKey.get()).as("khách không có bucket user").isNull();
    assertThat(seenIpKey.get()).isEqualTo("rl:default:ip:10.0.0.5");

    MockHttpServletRequest authenticated = request("POST", "/v1/checkout");
    authenticated.addHeader("x-user-id", "buyer-1");
    filter.doFilter(authenticated, new MockHttpServletResponse(), new MockFilterChain());
    assertThat(seenUserKey.get()).isEqualTo("rl:v1-checkout:u:buyer-1");
    assertThat(seenIpKey.get()).isEqualTo("rl:v1-checkout:ip:10.0.0.5");
  }

  @Test
  @DisplayName("lỗi bất ngờ từ limiter (không phải Redis) cũng fail-open, không thành 500")
  void unexpectedLimiterErrorShouldAlsoFailOpen() throws Exception {
    MockFilterChain chain = new MockFilterChain();

    filter(
            properties(true, true),
            (userKey, userLimit, ipKey, ipLimit, now) -> {
              throw new IllegalStateException("bug parse kết quả Lua");
            })
        .doFilter(request("GET", "/v1/products"), new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  @DisplayName("429 lặp lại chỉ log WARN 1 lần/5 s (không làm ngập log ở 500 VU)")
  void rejectionWarningsShouldBeThrottled() {
    RateLimitMetrics metrics = new RateLimitMetrics();
    long now = 1_000_000L;

    assertThat(metrics.noteRejected(now)).as("lần đầu log").isTrue();
    assertThat(metrics.noteRejected(now + 1_000)).as("trong cửa sổ 5 s").isFalse();
    assertThat(metrics.noteRejected(now + 5_000)).as("hết cửa sổ ⇒ log lại").isTrue();
    assertThat(metrics.snapshot()).containsEntry("rejected", 3L);
  }
}
