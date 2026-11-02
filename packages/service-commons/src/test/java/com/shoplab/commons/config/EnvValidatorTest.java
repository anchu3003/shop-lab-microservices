package com.shoplab.commons.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvValidatorTest {

  private static final RequiredConfig APP_ENV = RequiredConfig.of("APP_ENV", "môi trường chạy");
  private static final RequiredConfig TOKEN = RequiredConfig.of("TOKEN", "token nội bộ");

  @Test
  @DisplayName("đủ biến bắt buộc → không ném gì")
  void shouldPassWhenAllRequiredPresent() {
    Map<String, String> env = Map.of("APP_ENV", "local", "TOKEN", "abc");

    assertThatCode(() -> EnvValidator.validate(env, List.of(APP_ENV, TOKEN)))
        .doesNotThrowAnyException();
    assertThat(EnvValidator.findMissing(env, List.of(APP_ENV, TOKEN))).isEmpty();
  }

  @Test
  @DisplayName("thiếu biến → gom TẤT CẢ vào một message, không báo từng cái một")
  void shouldReportAllMissingKeysAtOnce() {
    Map<String, String> env = Map.of("OTHER", "x");

    assertThatThrownBy(() -> EnvValidator.validate(env, List.of(APP_ENV, TOKEN)))
        .isInstanceOf(MissingConfigException.class)
        .hasMessageContaining("APP_ENV")
        .hasMessageContaining("TOKEN")
        .hasMessageContaining(".env.example");
  }

  @Test
  @DisplayName("biến rỗng/toàn khoảng trắng cũng coi như thiếu")
  void shouldTreatBlankValueAsMissing() {
    Map<String, String> env = Map.of("APP_ENV", "   ");

    assertThatThrownBy(() -> EnvValidator.validate(env, List.of(APP_ENV)))
        .isInstanceOf(MissingConfigException.class);
  }

  @Test
  @DisplayName("danh sách biến thiếu trả về đúng key để tầng trên xử lý tiếp")
  void shouldExposeMissingKeys() {
    MissingConfigException exception = new MissingConfigException(List.of(APP_ENV, TOKEN));

    assertThat(exception.missingKeys()).containsExactly("APP_ENV", "TOKEN");
  }

  @Test
  @DisplayName("không khai báo biến bắt buộc nào → luôn pass")
  void shouldPassWhenNothingRequired() {
    assertThat(EnvValidator.findMissing(Map.of(), List.of())).isEmpty();
  }
}
