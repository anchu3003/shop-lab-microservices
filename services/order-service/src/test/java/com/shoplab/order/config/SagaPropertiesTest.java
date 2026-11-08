package com.shoplab.order.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho {@link SagaProperties} (`SL-2.6a` + `SL-2.6b`).
 *
 * <p>Cấu hình sai phải chết **lúc khởi động**, không phải ở request checkout đầu tiên: URL gõ sai
 * mà không validate thì triệu chứng là 503 khó hiểu đúng lúc user bấm đặt hàng — lúc tệ nhất.
 *
 * <p>Lưu ý: record này **chỉ có một constructor** (constructor binding của Spring Boot không chấp
 * nhận nhiều constructor) ⇒ test nào cũng phải truyền đủ tham số; {@link #defaults()} giữ phần đó ở
 * một chỗ.
 */
public class SagaPropertiesTest {

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
            "vnd",
            Duration.ofMinutes(15),
            Duration.ofSeconds(60),
            Duration.ofSeconds(30),
            50);

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
                    "VND",
                    Duration.ofMinutes(15),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(30),
                    50))
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
                    "VND",
                    Duration.ofMinutes(15),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(30),
                    50))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("connect-timeout");
  }

  @Test
  @DisplayName("payment-window <= 0 → lỗi (cửa sổ thanh toán không được vô hiệu hoá)")
  void rejectsNonPositivePaymentWindow() {
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
                    "VND",
                    Duration.ZERO,
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(30),
                    50))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("payment-window");
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
                    "DONG",
                    Duration.ofMinutes(15),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(30),
                    50))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("currency");
  }

  @Test
  @DisplayName("cấu hình mặc định của lab: payment-window 15′ = TTL reservation/voucher")
  void defaultsMatchDownstreamTtl() {
    SagaProperties properties = defaults();

    assertThat(properties.paymentWindow()).isEqualTo(Duration.ofMinutes(15));
    assertThat(properties.sweepInterval()).isEqualTo(Duration.ofSeconds(60));
    assertThat(properties.sweepInitialDelay()).isEqualTo(Duration.ofSeconds(30));
    assertThat(properties.sweepBatchSize()).isEqualTo(50);
  }

  /** Cấu hình dùng trong test — một chỗ duy nhất để sửa khi record thêm tham số. */
  public static SagaProperties defaults() {
    return new SagaProperties(
        "http://cart:8083",
        "http://catalog:8082",
        "http://inventory:8084",
        "http://promotion:8086",
        "http://payment:8088",
        Duration.ofSeconds(2),
        Duration.ofMillis(800),
        "VND",
        Duration.ofMinutes(15),
        Duration.ofSeconds(60),
        Duration.ofSeconds(30),
        50);
  }
}
