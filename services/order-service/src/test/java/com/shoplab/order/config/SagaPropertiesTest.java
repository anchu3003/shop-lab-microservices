package com.shoplab.order.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho {@link SagaProperties} (`SL-2.6a`).
 *
 * <p>Cấu hình sai phải chết **lúc khởi động**, không phải ở request checkout đầu tiên: một URL gõ
 * sai mà không validate thì triệu chứng là 503 khó hiểu khi user bấm đặt hàng — đúng lúc tệ nhất.
 */
class SagaPropertiesTest {

  @Test
  @DisplayName("cấu hình hợp lệ: trim '/' cuối URL, currency lên chữ HOA")
  void normalizesValues() {
    SagaProperties properties =
        new SagaProperties(
            "http://cart:8083/",
            "http://catalog:8082",
            "http://inventory:8084",
            "http://promotion:8086",
            "http://payment:8088",
            Duration.ofSeconds(2),
            Duration.ofMillis(800),
            "vnd");

    assertThat(properties.cartUrl()).isEqualTo("http://cart:8083");
    assertThat(properties.currency()).isEqualTo("VND");
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(800));
  }

  @Test
  @DisplayName("URL không có scheme → lỗi ngay lúc bind")
  void rejectsUrlWithoutScheme() {
    assertThatThrownBy(
            () ->
                new SagaProperties(
                    "cart:8083",
                    "http://catalog:8082",
                    "http://inventory:8084",
                    "http://promotion:8086",
                    "http://payment:8088",
                    Duration.ofSeconds(2),
                    Duration.ofMillis(800),
                    "VND"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cart-url");
  }

  @Test
  @DisplayName("timeout <= 0 → lỗi (timeout là bắt buộc, không được vô hiệu hoá)")
  void rejectsNonPositiveTimeout() {
    assertThatThrownBy(
            () ->
                new SagaProperties(
                    "http://cart:8083",
                    "http://catalog:8082",
                    "http://inventory:8084",
                    "http://promotion:8086",
                    "http://payment:8088",
                    Duration.ZERO,
                    Duration.ofMillis(800),
                    "VND"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("connect-timeout");
  }

  @Test
  @DisplayName("currency sai định dạng → lỗi")
  void rejectsInvalidCurrency() {
    assertThatThrownBy(
            () ->
                new SagaProperties(
                    "http://cart:8083",
                    "http://catalog:8082",
                    "http://inventory:8084",
                    "http://promotion:8086",
                    "http://payment:8088",
                    Duration.ofSeconds(2),
                    Duration.ofMillis(800),
                    "DONG"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("currency");
  }
}
