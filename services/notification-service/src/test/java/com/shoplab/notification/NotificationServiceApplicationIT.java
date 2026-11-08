package com.shoplab.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.notification.testsupport.TokenMinter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của notification-service — Postgres thật (Testcontainers).
 *
 * <p>Kiểm những thứ chỉ DB + HTTP thật mới chứng minh được: Flyway tạo schema & seed template, bất
 * biến {@code event_id UNIQUE} (chốt chặn gửi 2 lần ở tầng dữ liệu), và {@code GET
 * /v1/notifications/me} chỉ trả log của **chính** người gọi.
 *
 * <p><b>Không cần RabbitMQ</b>: consumer được tắt bằng {@code
 * shoplab.events.consumer-auto-startup=false} (cờ của commons — xem {@code EventProperties}); phần
 * ack/nack + dedupe đã có {@code NotificationConsumerIT} lo.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      // Trần 2 dòng để kiểm clamp của ?limit (mặc định 50 ở prod).
      "shoplab.notification.list-limit=2",
      // Không có broker trong IT ⇒ không start listener (nếu không: Authentication failure).
      "shoplab.events.consumer-auto-startup=false"
    })
@Testcontainers
class NotificationServiceApplicationIT {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);
  private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-000000000001");
  private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-000000000002");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  @TestConfiguration
  static class TestJwtConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
      return MINTER.decoder();
    }
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void clear() {
    jdbc.update("delete from notification_logs");
    jdbc.update("delete from processed_events");
  }

  @Test
  @DisplayName("Flyway tạo đủ 3 bảng + seed 2 template (order.confirmed, order.cancelled)")
  void shouldMigrateSchema() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);
    assertThat(tables).contains("notification_logs", "notification_templates", "processed_events");

    List<String> codes =
        jdbc.queryForList("select code from notification_templates order by code", String.class);
    assertThat(codes).containsExactly("order.cancelled", "order.confirmed");
  }

  @Test
  @DisplayName("event_id UNIQUE: ghi cùng event 2 lần → vi phạm ràng buộc (chốt chặn gửi 2 lần)")
  void shouldRejectDuplicateEventId() {
    UUID eventId = UUID.randomUUID();
    insertLog(eventId, USER_A, "Đơn 1", Instant.parse("2026-11-08T10:00:00Z"));

    assertThatThrownBy(
            () -> insertLog(eventId, USER_A, "Đơn 1 lần 2", Instant.parse("2026-11-08T10:00:01Z")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("GET /v1/notifications/me không token → 401")
  void shouldRejectAnonymous() throws Exception {
    HttpResponse<String> response = get("/v1/notifications/me", null);

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  @DisplayName("me chỉ trả log của CHÍNH mình, mới nhất trước (userId lấy từ claim sub)")
  void shouldReturnOnlyOwnRowsNewestFirst() throws Exception {
    insertLog(UUID.randomUUID(), USER_A, "cũ", Instant.parse("2026-11-08T09:00:00Z"));
    insertLog(UUID.randomUUID(), USER_A, "mới", Instant.parse("2026-11-08T11:00:00Z"));
    insertLog(UUID.randomUUID(), USER_B, "của người khác", Instant.parse("2026-11-08T12:00:00Z"));

    HttpResponse<String> response = get("/v1/notifications/me", MINTER.token(USER_A, "BUYER"));

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode rows = objectMapper.readTree(response.body());
    assertThat(rows.size()).isEqualTo(2);
    for (JsonNode row : rows) {
      assertThat(row.get("userId").asString()).isEqualTo(USER_A.toString());
    }
    assertThat(rows.get(0).get("subject").asString()).isEqualTo("mới");
  }

  @Test
  @DisplayName("?limit bị chặn bởi trần cấu hình (limit=99 mà trần 2 → 2 dòng)")
  void shouldClampLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      insertLog(UUID.randomUUID(), USER_A, "n" + i, Instant.parse("2026-11-08T09:0" + i + ":00Z"));
    }

    HttpResponse<String> response =
        get("/v1/notifications/me?limit=99", MINTER.token(USER_A, "BUYER"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(objectMapper.readTree(response.body()).size()).isEqualTo(2);
  }

  // ---------------------------------------------------------------- helpers

  private void insertLog(UUID eventId, UUID userId, String subject, Instant createdAt) {
    jdbc.update(
        """
        INSERT INTO notification_logs (event_id, user_id, order_id, channel, template, recipient,
                                       subject, body, status, created_at, sent_at)
        VALUES (?, ?, ?, 'EMAIL', 'order.confirmed', ?, ?, 'body', 'SENT', ?, ?)
        """,
        eventId,
        userId,
        UUID.randomUUID(),
        "user-" + userId.toString().substring(0, 8) + "@shoplab.local",
        subject,
        java.sql.Timestamp.from(createdAt),
        java.sql.Timestamp.from(createdAt));
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }
}
