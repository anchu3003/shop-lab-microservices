package com.shoplab.commons.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;

/**
 * Test cơ chế "fail-closed" của spec OpenAPI (SL-1.8).
 *
 * <p>Đây là loại logic bảo mật mà test rẻ hơn nhiều so với verify tay: chỉ cần chứng minh 3 nhánh —
 * local giữ nguyên, môi trường khác bị ép tắt, có override thì tôn trọng.
 */
class OpenApiEnvironmentPostProcessorTest {

  private static final String ENABLED = OpenApiEnvironmentPostProcessor.ENABLED_PROPERTY;

  private final OpenApiEnvironmentPostProcessor processor = new OpenApiEnvironmentPostProcessor();

  private static MockEnvironment environmentWith(String appEnv, String override) {
    MockEnvironment environment = new MockEnvironment();
    // yml của service đang để default `true` — mô phỏng đúng giá trị sau khi config data nạp.
    TestPropertyValues.of(ENABLED + "=true").applyTo(environment);
    if (appEnv != null) {
      TestPropertyValues.of("APP_ENV=" + appEnv).applyTo(environment);
    }
    if (override != null) {
      TestPropertyValues.of("SHOPLAB_OPENAPI_ENABLED=" + override).applyTo(environment);
    }
    return environment;
  }

  @Test
  @DisplayName("APP_ENV=local → KHÔNG đụng vào springdoc (dev vẫn xem được spec)")
  void shouldKeepEnabledOnLocal() {
    MockEnvironment environment = environmentWith("LOCAL", null);

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty(ENABLED)).isEqualTo("true");
  }

  @Test
  @DisplayName("APP_ENV=prod (không override) → ép springdoc.api-docs.enabled=false")
  void shouldDisableOutsideLocal() {
    MockEnvironment environment = environmentWith("prod", null);

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty(ENABLED)).isEqualTo("false");
  }

  @Test
  @DisplayName("APP_ENV=staging + override true → tôn trọng quyết định có chủ ý của người set env")
  void shouldRespectExplicitOverride() {
    MockEnvironment environment = environmentWith("staging", "true");

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty(ENABLED)).isEqualTo("true");
  }

  @Test
  @DisplayName("thiếu APP_ENV → không đụng gì (để service tự fail-fast ở EnvValidator)")
  void shouldDoNothingWithoutAppEnv() {
    MockEnvironment environment = environmentWith(null, null);

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty(ENABLED)).isEqualTo("true");
  }

  @Test
  @DisplayName("chạy SAU ConfigDataEnvironmentPostProcessor (nếu không sẽ bị yml ghi đè ngược)")
  void shouldRunAfterConfigData() {
    assertThat(processor.getOrder()).isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);
  }
}
