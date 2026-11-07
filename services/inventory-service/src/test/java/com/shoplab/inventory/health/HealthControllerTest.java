package com.shoplab.inventory.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoplab.commons.web.GlobalExceptionHandler;
import com.shoplab.inventory.config.ServiceProperties;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

  private static MockMvc mockMvc(ServiceProperties properties) {
    return MockMvcBuilders.standaloneSetup(new HealthController(new ReadinessService(properties)))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  private static ServiceProperties propertiesFor(int port) {
    return new ServiceProperties(
        "local",
        false,
        "test",
        new ServiceProperties.Readiness(
            List.of(
                new ServiceProperties.Readiness.Dependency(
                    "stub", "127.0.0.1", port, Duration.ofMillis(300)))));
  }

  private static int closedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Test
  @DisplayName("/health là liveness — 200 kể cả khi dependency chết")
  void healthShouldAlwaysBeUp() throws Exception {
    mockMvc(propertiesFor(closedPort()))
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("/ready là readiness — 200 khi mọi dependency UP")
  void readyShouldBeOkWhenDependencyUp() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      mockMvc(propertiesFor(socket.getLocalPort()))
          .perform(get("/ready"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.dependencies[0].status").value("UP"));
    }
  }

  @Test
  @DisplayName("/ready trả 503 khi dependency chết — LB ngừng đẩy traffic, KHÔNG restart")
  void readyShouldReturn503WhenDependencyDown() throws Exception {
    mockMvc(propertiesFor(closedPort()))
        .perform(get("/ready"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  @DisplayName("không có dependency nào → /ready luôn 200")
  void readyShouldBeUpWhenNoDependencyConfigured() throws Exception {
    mockMvc(
            new ServiceProperties(
                "local", false, "test", new ServiceProperties.Readiness(List.of())))
        .perform(get("/ready"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
