package com.shoplab.payment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test validate cấu hình — quan trọng vì đây là <b>fail-closed</b>: cấu hình sai phải chết lúc
 * boot, không phải trả 500 (hoặc tệ hơn: chạy với secret rỗng) khi có request.
 */
class PaymentPropertiesTest {

  private static PaymentProperties valid() {
    return new PaymentProperties(
        Duration.ofMinutes(15),
        "mock",
        "http://localhost:8080/mock-provider/pay",
        "secret",
        Duration.ofMinutes(5),
        true,
        Duration.ofMillis(50),
        Duration.ofHours(24),
        Duration.ofHours(1),
        200);
  }

  @Test
  @DisplayName("cấu hình hợp lệ → dựng được")
  void validConfig() {
    PaymentProperties properties = valid();
    assertThat(properties.intentTtl()).isEqualTo(Duration.ofMinutes(15));
    assertThat(properties.provider()).isEqualTo("mock");
  }

  @Test
  @DisplayName("không có webhook secret → chết (webhook public không thể chạy không chữ ký)")
  void blankSecretRejected() {
    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ofMinutes(15),
                    "mock",
                    "http://localhost:8080/mock-provider/pay",
                    "  ",
                    Duration.ofMinutes(5),
                    false,
                    Duration.ofMillis(50),
                    Duration.ofHours(24),
                    Duration.ofHours(1),
                    200))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("webhook-secret");
  }

  @Test
  @DisplayName("TTL / retention / tolerance phải > 0, mock-latency không âm")
  void invalidDurationsRejected() {
    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ZERO,
                    "mock",
                    "url",
                    "secret",
                    Duration.ofMinutes(5),
                    false,
                    Duration.ofMillis(1),
                    Duration.ofHours(24),
                    Duration.ofHours(1),
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("intent-ttl");

    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ofMinutes(15),
                    "mock",
                    "url",
                    "secret",
                    Duration.ZERO,
                    false,
                    Duration.ofMillis(1),
                    Duration.ofHours(24),
                    Duration.ofHours(1),
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("webhook-tolerance");

    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ofMinutes(15),
                    "mock",
                    "url",
                    "secret",
                    Duration.ofMinutes(5),
                    false,
                    Duration.ofMillis(1),
                    Duration.ZERO,
                    Duration.ofHours(1),
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("idempotency-retention");

    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ofMinutes(15),
                    "mock",
                    "url",
                    "secret",
                    Duration.ofMinutes(5),
                    false,
                    Duration.ofMillis(1),
                    Duration.ofHours(24),
                    Duration.ZERO,
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("in-progress-stale-after");
  }

  @Test
  @DisplayName("mock-latency âm → chết lúc boot")
  void negativeMockLatencyRejected() {
    assertThatThrownBy(
            () ->
                new PaymentProperties(
                    Duration.ofMinutes(15),
                    "mock",
                    "url",
                    "secret",
                    Duration.ofMinutes(5),
                    false,
                    Duration.ofMillis(-1),
                    Duration.ofHours(24),
                    Duration.ofHours(1),
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mock-latency");
  }

  @Test
  @DisplayName("checkoutUrl ghép đúng cả khi base URL có/không có dấu / cuối")
  void checkoutUrlJoinsCorrectly() {
    assertThat(valid().checkoutUrl("pi_1"))
        .isEqualTo("http://localhost:8080/mock-provider/pay/pi_1");

    PaymentProperties withSlash =
        new PaymentProperties(
            Duration.ofMinutes(15),
            "mock",
            "http://localhost:8080/mock-provider/pay/",
            "secret",
            Duration.ofMinutes(5),
            false,
            Duration.ofMillis(1),
            Duration.ofHours(24),
            Duration.ofHours(1),
            10);
    assertThat(withSlash.checkoutUrl("pi_1"))
        .isEqualTo("http://localhost:8080/mock-provider/pay/pi_1");
  }
}
