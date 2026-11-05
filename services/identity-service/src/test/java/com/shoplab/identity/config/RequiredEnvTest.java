package com.shoplab.identity.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequiredEnvTest {

  @Test
  @DisplayName("APP_ENV + POSTGRES_PASSWORD là biến bắt buộc, có mô tả để in vào message lỗi")
  void shouldDeclareRequiredVariables() {
    assertThat(RequiredEnv.ALL).containsExactly(RequiredEnv.APP_ENV, RequiredEnv.POSTGRES_PASSWORD);
    assertThat(RequiredEnv.APP_ENV.key()).isEqualTo("APP_ENV");
    assertThat(RequiredEnv.POSTGRES_PASSWORD.key()).isEqualTo("POSTGRES_PASSWORD");
    assertThat(RequiredEnv.ALL).allSatisfy(item -> assertThat(item.description()).isNotBlank());
  }

  @Test
  @DisplayName("danh sách bắt buộc dùng được với EnvValidator (không rỗng, không null)")
  void shouldBeUsableByEnvValidator() {
    assertThat(RequiredEnv.ALL).isNotEmpty().doesNotContainNull();
  }
}
