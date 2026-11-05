package com.shoplab.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServicePropertiesTest {

  @Test
  @DisplayName("giữ nguyên giá trị cấu hình và suy ra AppEnv")
  void shouldKeepConfiguredValues() {
    ServiceProperties properties =
        new ServiceProperties(
            "prod",
            true,
            "abc1234",
            new ServiceProperties.Readiness(
                List.of(
                    new ServiceProperties.Readiness.Dependency(
                        "postgres", "db.internal", 5432, Duration.ofMillis(250)))));

    assertThat(properties.appEnv()).isEqualTo(AppEnv.PROD);
    assertThat(properties.debugEndpoints()).isTrue();
    assertThat(properties.gitCommit()).isEqualTo("abc1234");
    assertThat(properties.readiness().dependencies()).hasSize(1);
    assertThat(properties.readiness().dependencies().get(0).host()).isEqualTo("db.internal");
    assertThat(properties.readiness().dependencies().get(0).timeout())
        .isEqualTo(Duration.ofMillis(250));
  }

  @Test
  @DisplayName("APP_ENV sai giá trị → chết ngay lúc bind, không đợi tới request đầu tiên")
  void shouldFailFastOnInvalidEnv() {
    assertThatThrownBy(() -> new ServiceProperties("sai", false, "x", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APP_ENV");
  }

  @Test
  @DisplayName("readiness null → danh sách rỗng, không NPE")
  void shouldDefaultReadinessToEmpty() {
    ServiceProperties properties = new ServiceProperties("local", false, "x", null);

    assertThat(properties.readiness()).isNotNull();
    assertThat(properties.readiness().dependencies()).isEmpty();
  }

  @Test
  @DisplayName("timeout của dependency null → mặc định 500ms")
  void shouldDefaultDependencyTimeout() {
    ServiceProperties.Readiness.Dependency dependency =
        new ServiceProperties.Readiness.Dependency("redis", "localhost", 6379, null);

    assertThat(dependency.timeout()).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  @DisplayName("Readiness null → danh sách rỗng")
  void shouldDefaultReadinessListToEmpty() {
    assertThat(new ServiceProperties.Readiness(null).dependencies()).isEmpty();
  }
}
