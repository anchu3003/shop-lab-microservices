package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;
import com.shoplab.gateway.ratelimit.RateLimitProperties.Rule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Chọn rule: prefix dài nhất thắng, method phải khớp, path lạ rơi về default. */
class RateLimitRulesTest {

  private static final Limit DEFAULT_USER = new Limit(100, 200);
  private static final Limit DEFAULT_IP = new Limit(200, 400);

  private final RateLimitRules rules =
      new RateLimitRules(
          new RateLimitProperties(
              true,
              true,
              "rl",
              DEFAULT_USER,
              DEFAULT_IP,
              List.of("/health"),
              List.of(),
              List.of(
                  new Rule("/v1", null, new Limit(50, 60), null),
                  new Rule("/v1/checkout", "POST", new Limit(10, 20), new Limit(100, 200)),
                  new Rule("/v1/carts", null, null, new Limit(50, 100)))));

  @Test
  @DisplayName("prefix dài nhất thắng: /v1/checkout dùng rule riêng, không phải rule /v1")
  void longestPrefixShouldWin() {
    ResolvedRateLimit resolved = rules.resolve("/v1/checkout", "POST");

    assertThat(resolved.routeKey()).isEqualTo("v1-checkout");
    assertThat(resolved.perUser()).isEqualTo(new Limit(10, 20));
  }

  @Test
  @DisplayName("sai method ⇒ KHÔNG dùng rule đó (GET /v1/checkout không chịu hạn mức của POST)")
  void methodMismatchShouldSkipRule() {
    ResolvedRateLimit resolved = rules.resolve("/v1/checkout/status", "GET");

    assertThat(resolved.routeKey()).isEqualTo("v1");
    assertThat(resolved.perUser()).isEqualTo(new Limit(50, 60));
  }

  @Test
  @DisplayName("rule thiếu hạn mức ⇒ lấy default tương ứng (không phải 'không giới hạn')")
  void missingLimitShouldFallBackToDefault() {
    ResolvedRateLimit resolved = rules.resolve("/v1/carts/abc", "POST");

    assertThat(resolved.routeKey()).isEqualTo("v1-carts");
    // Rule chỉ khai per-ip ⇒ per-user phải rơi về default, KHÔNG phải "không giới hạn".
    assertThat(resolved.perUser()).isEqualTo(DEFAULT_USER);
    assertThat(resolved.perIp()).isEqualTo(new Limit(50, 100));
  }

  @Test
  @DisplayName("path không khớp rule nào ⇒ xô 'default' + hạn mức mặc định")
  void unknownPathShouldUseDefaultBucket() {
    ResolvedRateLimit resolved = rules.resolve("/v1/orders", "GET");

    // /v1 vẫn khớp (prefix ngắn) — đây là hành vi đúng: rule rộng bắt trước default.
    assertThat(resolved.routeKey()).isEqualTo("v1");

    ResolvedRateLimit outside = rules.resolve("/v2/anything", "GET");
    assertThat(outside.routeKey()).isEqualTo(RateLimitRules.DEFAULT_ROUTE_KEY);
    assertThat(outside.perUser()).isEqualTo(DEFAULT_USER);
  }

  @Test
  @DisplayName("routeKey bỏ '/' vì key Redis dùng ':' làm dấu phân cách")
  void routeKeyShouldNotContainSlashes() {
    assertThat(RateLimitRules.routeKey("/v1/checkout")).isEqualTo("v1-checkout");
    assertThat(RateLimitRules.routeKey("/v1/checkout/")).isEqualTo("v1-checkout");
    assertThat(RateLimitRules.routeKey("/")).isEqualTo(RateLimitRules.DEFAULT_ROUTE_KEY);
  }

  @Test
  @DisplayName("boundary segment: /v1/checkoutx KHÔNG khớp rule /v1/checkout")
  void shouldRespectSegmentBoundary() {
    assertThat(rules.resolve("/v1/checkoutx", "POST").routeKey()).isEqualTo("v1");
  }
}
