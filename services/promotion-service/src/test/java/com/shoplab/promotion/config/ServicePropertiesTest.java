package com.shoplab.promotion.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServicePropertiesTest {

  @Test
  @DisplayName("APP_ENV sai → chết ngay lúc bind, không đợi request đầu tiên")
  void shouldFailFastOnInvalidEnv() {
    assertThatThrownBy(() -> new ServiceProperties("locall", false, "abc", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APP_ENV");
  }

  @Test
  @DisplayName("readiness null → tự thành danh sách rỗng (không NPE khi check)")
  void shouldDefaultReadinessToEmpty() {
    ServiceProperties properties = new ServiceProperties("local", true, "abc123", null);

    assertThat(properties.readiness().dependencies()).isEmpty();
    assertThat(properties.debugEndpoints()).isTrue();
    assertThat(properties.appEnv()).isEqualTo(AppEnv.LOCAL);
    assertThat(properties.gitCommit()).isEqualTo("abc123");
  }

  @Test
  @DisplayName("dependencies null → rỗng; timeout null → 500ms")
  void shouldDefaultNestedValues() {
    assertThat(new ServiceProperties.Readiness(null).dependencies()).isEmpty();

    ServiceProperties.Readiness.Dependency dependency =
        new ServiceProperties.Readiness.Dependency("pg", "localhost", 5432, null);
    assertThat(dependency.timeout()).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  @DisplayName("giữ nguyên dependency khai báo trong config")
  void shouldKeepDeclaredDependencies() {
    ServiceProperties.Readiness.Dependency dependency =
        new ServiceProperties.Readiness.Dependency("pg", "db", 5432, Duration.ofMillis(200));

    ServiceProperties properties =
        new ServiceProperties(
            "local", false, "abc", new ServiceProperties.Readiness(List.of(dependency)));

    assertThat(properties.readiness().dependencies()).containsExactly(dependency);
  }
}
