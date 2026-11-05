package com.shoplab.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GatewayPropertiesTest {

  private static final List<GatewayProperties.Route> ONE_ROUTE =
      List.of(new GatewayProperties.Route("/v1/auth", "http://identity:8081"));

  @Test
  @DisplayName("giữ nguyên giá trị cấu hình")
  void shouldKeepConfiguredValues() {
    GatewayProperties properties =
        new GatewayProperties(
            ONE_ROUTE,
            List.of("/v1/auth/**", "/health"),
            new GatewayProperties.Jwt(
                "http://identity:8081/.well-known/jwks.json",
                "https://identity.shoplab.local",
                Duration.ofMinutes(10),
                Duration.ofSeconds(20),
                Duration.ofSeconds(5)),
            new GatewayProperties.Timeouts(Duration.ofSeconds(1), Duration.ofSeconds(3)));

    assertThat(properties.routes()).hasSize(1);
    assertThat(properties.publicPaths()).containsExactly("/v1/auth/**", "/health");
    assertThat(properties.jwt().jwksCacheTtl()).isEqualTo(Duration.ofMinutes(10));
    assertThat(properties.jwt().minRefreshInterval()).isEqualTo(Duration.ofSeconds(20));
    assertThat(properties.timeouts().read()).isEqualTo(Duration.ofSeconds(3));
  }

  @Test
  @DisplayName(
      "thiếu cấu hình con → dùng default (cache 5m, chặn dội 10s, skew 30s, connect 2s, read 5s)")
  void shouldApplyDefaults() {
    GatewayProperties properties = new GatewayProperties(ONE_ROUTE, null, null, null);

    assertThat(properties.publicPaths()).isEmpty();
    assertThat(properties.jwt().jwksCacheTtl()).isEqualTo(Duration.ofMinutes(5));
    assertThat(properties.jwt().minRefreshInterval()).isEqualTo(Duration.ofSeconds(10));
    assertThat(properties.jwt().clockSkew()).isEqualTo(Duration.ofSeconds(30));
    assertThat(properties.timeouts().connect()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.timeouts().read()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  @DisplayName("route rỗng → chết ngay lúc bind config (gateway không route được gì là vô dụng)")
  void shouldFailFastWhenNoRoute() {
    assertThatThrownBy(() -> new GatewayProperties(List.of(), null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("routes rỗng");
    assertThatThrownBy(() -> new GatewayProperties(null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
