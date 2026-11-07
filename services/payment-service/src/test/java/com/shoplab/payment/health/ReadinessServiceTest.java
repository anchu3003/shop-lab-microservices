package com.shoplab.payment.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.health.HealthCheckResult;
import com.shoplab.payment.config.ServiceProperties;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReadinessServiceTest {

  private static ServiceProperties propertiesWith(String name, int port) {
    return new ServiceProperties(
        "local",
        false,
        "test",
        new ServiceProperties.Readiness(
            List.of(
                new ServiceProperties.Readiness.Dependency(
                    name, "127.0.0.1", port, Duration.ofMillis(300)))));
  }

  @Test
  @DisplayName("mỗi dependency trong config tạo đúng một check")
  void shouldBuildOneCheckPerDependency() {
    ServiceProperties properties =
        new ServiceProperties(
            "local",
            false,
            "test",
            new ServiceProperties.Readiness(
                List.of(
                    new ServiceProperties.Readiness.Dependency(
                        "pg", "localhost", 5432, Duration.ofMillis(300)),
                    new ServiceProperties.Readiness.Dependency(
                        "redis", "localhost", 6379, Duration.ofMillis(300)))));

    assertThat(new ReadinessService(properties).dependencyCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("dependency kết nối được → UP")
  void shouldReportUpForReachableDependency() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      ReadinessService service =
          new ReadinessService(propertiesWith("stub", socket.getLocalPort()));

      List<HealthCheckResult> results = service.checkAll();

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().healthy()).isTrue();
      assertThat(results.getFirst().name()).isEqualTo("stub");
    }
  }

  @Test
  @DisplayName("dependency chết → DOWN, readiness tổng phải là false")
  void shouldReportDownForUnreachableDependency() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    ReadinessService service = new ReadinessService(propertiesWith("stub", closedPort));

    List<HealthCheckResult> results = service.checkAll();

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().healthy()).isFalse();
  }

  @Test
  @DisplayName("không khai báo dependency → không check gì, luôn sẵn sàng")
  void shouldHandleEmptyDependencyList() {
    ReadinessService service =
        new ReadinessService(
            new ServiceProperties(
                "local", false, "test", new ServiceProperties.Readiness(List.of())));

    assertThat(service.dependencyCount()).isZero();
    assertThat(service.checkAll()).isEmpty();
  }
}
