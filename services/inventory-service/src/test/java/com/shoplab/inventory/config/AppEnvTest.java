package com.shoplab.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppEnvTest {

  @ParameterizedTest
  @CsvSource({"local,LOCAL", "DEV,DEV", " Staging ,STAGING", "prod,PROD"})
  @DisplayName("parse chấp nhận mọi cách viết hoa/thường + khoảng trắng")
  void shouldParseValidValues(String raw, AppEnv expected) {
    assertThat(AppEnv.parse(raw)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"locall", "production", "dev ", " test"})
  @DisplayName(
      "giá trị lạ chỉ chết khi thật sự lạ — 'dev ' (có space cuối) vẫn OK, 'locall' thì không")
  void shouldRejectUnknownValues(String raw) {
    if (raw.trim().equals("dev")) {
      assertThat(AppEnv.parse(raw)).isEqualTo(AppEnv.DEV);
      return;
    }
    assertThatThrownBy(() -> AppEnv.parse(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("không hợp lệ");
  }

  @Test
  @DisplayName("rỗng/null → lỗi rõ ràng (không âm thầm coi là local)")
  void shouldRejectBlank() {
    assertThatThrownBy(() -> AppEnv.parse(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APP_ENV chưa được set");
    assertThatThrownBy(() -> AppEnv.parse(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APP_ENV chưa được set");
  }

  @Test
  @DisplayName("chỉ LOCAL mới là local")
  void onlyLocalIsLocal() {
    assertThat(AppEnv.LOCAL.isLocal()).isTrue();
    assertThat(AppEnv.DEV.isLocal()).isFalse();
    assertThat(AppEnv.PROD.isLocal()).isFalse();
  }
}
