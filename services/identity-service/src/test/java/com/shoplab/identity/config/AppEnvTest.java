package com.shoplab.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppEnvTest {

  @Test
  @DisplayName("nhận 4 giá trị hợp lệ, không phân biệt hoa thường và khoảng trắng")
  void shouldParseValidValues() {
    assertThat(AppEnv.parse("local")).isEqualTo(AppEnv.LOCAL);
    assertThat(AppEnv.parse(" DEV ")).isEqualTo(AppEnv.DEV);
    assertThat(AppEnv.parse("Staging")).isEqualTo(AppEnv.STAGING);
    assertThat(AppEnv.parse("PROD")).isEqualTo(AppEnv.PROD);
  }

  @Test
  @DisplayName("APP_ENV null → lỗi nêu rõ tên biến")
  void shouldRejectNull() {
    assertThatThrownBy(() -> AppEnv.parse(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APP_ENV");
  }

  @Test
  @DisplayName("APP_ENV rỗng → lỗi")
  void shouldRejectBlank() {
    assertThatThrownBy(() -> AppEnv.parse("   ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("giá trị lạ → lỗi nêu cả giá trị nhận được lẫn danh sách hợp lệ")
  void shouldRejectUnknownValue() {
    assertThatThrownBy(() -> AppEnv.parse("locall"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("locall")
        .hasMessageContaining("local, dev, staging, prod");
  }

  @Test
  @DisplayName("chỉ LOCAL mới là local")
  void shouldReportLocalOnly() {
    assertThat(AppEnv.LOCAL.isLocal()).isTrue();
    assertThat(AppEnv.PROD.isLocal()).isFalse();
  }
}
