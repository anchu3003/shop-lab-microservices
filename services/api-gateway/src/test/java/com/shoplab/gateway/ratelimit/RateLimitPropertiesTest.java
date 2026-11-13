package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;
import com.shoplab.gateway.ratelimit.RateLimitProperties.Rule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cấu hình rate limit: default phải an toàn, cấu hình sai phải chết lúc bind. */
class RateLimitPropertiesTest {

  @Test
  @DisplayName("bỏ trống hết ⇒ default an toàn (enabled, fail-open, bypass hạ tầng, hạn mức dương)")
  void shouldFillSafeDefaults() {
    RateLimitProperties properties =
        new RateLimitProperties(true, true, null, null, null, null, null, null);

    assertThat(properties.keyPrefix()).isEqualTo("rl");
    assertThat(properties.defaultPerUser().rps()).isPositive();
    assertThat(properties.defaultPerUser().burst()).isGreaterThanOrEqualTo(1);
    assertThat(properties.defaultPerIp().burst())
        .isGreaterThanOrEqualTo(properties.defaultPerUser().burst());
    // Bypass mặc định là bắt buộc: /health bị 429 thì orchestrator tưởng service chết.
    assertThat(properties.bypass()).contains("/health", "/ready");
    // Không tin X-Forwarded-For khi chưa khai proxy — mặc định phải là "không tin ai".
    assertThat(properties.trustedProxies()).isEmpty();
    assertThat(properties.routes()).isEmpty();
  }

  @Test
  @DisplayName("rps <= 0 hoặc burst < 1 ⇒ ném ngay, không để chia 0 lúc chạy")
  void shouldRejectNonsenseLimits() {
    assertThatThrownBy(() -> new Limit(0, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("rps");
    assertThatThrownBy(() -> new Limit(10, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("burst");
  }

  @Test
  @DisplayName(
      "bypass rỗng do người dùng khai ⇒ quay về danh sách mặc định (không vô tình bỏ /health)")
  void emptyBypassShouldFallBackToDefaults() {
    RateLimitProperties properties =
        new RateLimitProperties(true, true, "rl", null, null, List.of(), null, List.<Rule>of());

    assertThat(properties.bypass()).contains("/health");
  }
}
