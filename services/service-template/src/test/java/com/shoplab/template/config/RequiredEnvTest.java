package com.shoplab.template.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequiredEnvTest {

  @Test
  @DisplayName("APP_ENV là biến bắt buộc duy nhất ở Phase 0, có mô tả để in vào message lỗi")
  void shouldDeclareAppEnvAsRequired() {
    assertThat(RequiredEnv.ALL).containsExactly(RequiredEnv.APP_ENV);
    assertThat(RequiredEnv.APP_ENV.key()).isEqualTo("APP_ENV");
    assertThat(RequiredEnv.APP_ENV.description()).isNotBlank();
  }

  @Test
  @DisplayName("danh sách bắt buộc dùng được với EnvValidator (không rỗng, không null)")
  void shouldBeUsableByEnvValidator() {
    assertThat(RequiredEnv.ALL).isNotEmpty().doesNotContainNull();
  }
}
