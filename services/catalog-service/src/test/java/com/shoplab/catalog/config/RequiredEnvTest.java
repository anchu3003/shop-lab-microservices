package com.shoplab.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequiredEnvTest {

  @Test
  @DisplayName("đủ APP_ENV + POSTGRES_PASSWORD → không lỗi")
  void shouldPassWhenAllRequiredEnvPresent() {
    assertThatCode(
            () ->
                EnvValidator.validate(
                    Map.of("APP_ENV", "local", "POSTGRES_PASSWORD", "secret"), RequiredEnv.ALL))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("thiếu env bắt buộc → MissingConfigException nêu rõ tên biến")
  void shouldFailWhenEnvMissing() {
    assertThatThrownBy(() -> EnvValidator.validate(Map.of(), RequiredEnv.ALL))
        .isInstanceOf(MissingConfigException.class)
        .hasMessageContaining("APP_ENV")
        .hasMessageContaining("POSTGRES_PASSWORD");
  }

  @Test
  @DisplayName("danh sách bắt buộc đúng 2 biến — thêm biến mới phải sửa test này")
  void shouldDeclareExactlyTwoRequiredVars() {
    assertThat(RequiredEnv.ALL).hasSize(2);
  }
}
