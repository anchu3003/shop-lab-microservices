package com.shoplab.commons.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TcpHealthCheckTest {

  @Test
  @DisplayName("port đang mở → UP")
  void shouldReportUpWhenPortIsOpen() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      TcpHealthCheck check =
          new TcpHealthCheck(
              "postgres", "127.0.0.1", serverSocket.getLocalPort(), Duration.ofMillis(500));

      HealthCheckResult result = check.check();

      assertThat(result.healthy()).isTrue();
      assertThat(result.status()).isEqualTo(HealthStatus.UP);
      assertThat(result.name()).isEqualTo("postgres");
      assertThat(result.detail()).isEqualTo("ok");
      assertThat(result.latency()).isNotNull();
      assertThat(check.host()).isEqualTo("127.0.0.1");
      assertThat(check.port()).isEqualTo(serverSocket.getLocalPort());
    }
  }

  @Test
  @DisplayName("port đã đóng → DOWN kèm lý do (KHÔNG ném exception)")
  void shouldReportDownWhenPortIsClosed() throws IOException {
    int closedPort;
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      closedPort = serverSocket.getLocalPort();
    }

    HealthCheckResult result =
        new TcpHealthCheck("redis", "127.0.0.1", closedPort, Duration.ofMillis(300)).check();

    assertThat(result.healthy()).isFalse();
    assertThat(result.status()).isEqualTo(HealthStatus.DOWN);
    assertThat(result.detail()).isNotBlank();
    assertThat(result.name()).isEqualTo("redis");
  }

  @Test
  @DisplayName("host không phân giải được → DOWN, không làm sập request")
  void shouldReportDownWhenHostIsUnresolvable() {
    HealthCheckResult result =
        new TcpHealthCheck("ghost", "khong-ton-tai.invalid", 1234, Duration.ofMillis(300)).check();

    assertThat(result.healthy()).isFalse();
    assertThat(result.detail()).isNotBlank();
  }

  @Test
  @DisplayName("timeout null → dùng mặc định 1s, không NPE")
  void shouldFallBackToDefaultTimeout() {
    TcpHealthCheck check = new TcpHealthCheck("x", "127.0.0.1", 1, null);

    assertThat(check.check().status()).isEqualTo(HealthStatus.DOWN);
  }

  @Test
  @DisplayName("factory DependencyHealthCheck.tcp() tạo được check dùng được")
  void shouldCreateThroughFactory() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      DependencyHealthCheck check =
          DependencyHealthCheck.tcp(
              "pg", "127.0.0.1", serverSocket.getLocalPort(), Duration.ofMillis(500));

      assertThat(check.name()).isEqualTo("pg");
      assertThat(check.check().healthy()).isTrue();
    }
  }

  @Test
  @DisplayName("HealthCheckResult: factory up/down set đúng trạng thái")
  void shouldBuildResultsThroughFactories() {
    assertThat(HealthCheckResult.up("a", Duration.ofMillis(1)).healthy()).isTrue();
    assertThat(HealthCheckResult.down("b", "lý do", Duration.ofMillis(1)).healthy()).isFalse();
    assertThat(HealthCheckResult.down("b", "lý do", Duration.ofMillis(1)).detail())
        .isEqualTo("lý do");
  }
}
