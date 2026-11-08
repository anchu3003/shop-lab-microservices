package com.shoplab.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventEnvelopeCodec;
import com.shoplab.commons.events.ProcessedEventStore;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.log.NotificationLogRepository;
import com.shoplab.notification.mq.OrderEventConsumer;
import com.shoplab.notification.notify.NotificationService;
import com.shoplab.notification.template.TemplateRenderer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Luồng consumer trên **Postgres thật** (SL-3.4): event → render → `notification_logs`, dedupe theo
 * `eventId`, và fail-mode ⇒ `nack(requeue=false)` (DLQ).
 *
 * <p><b>Không cần RabbitMQ</b>: gọi thẳng `onMessage(message, channel)` với `Channel` mock. Cái cần
 * chứng minh là **quyết định** ack/nack + tác dụng phụ trong DB; việc broker chuyển message do
 * `tools/e2e-async.sh` lo trên stack thật (`SL-3.7`).
 */
@Testcontainers
@SpringBootTest(
    properties = {
      "shoplab.env=local",
      "shoplab.notification.fail-mode=false",
      // Không có broker trong IT ⇒ không start listener của commons (nếu không: Authentication
      // failure làm đỏ cả context). Đây là cờ của commons, KHÔNG phải
      // `spring.rabbitmq.listener.simple.auto-startup` (Boot không áp cho factory tự khai).
      "shoplab.events.consumer-auto-startup=false"
    })
class NotificationConsumerIT {

  private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ProcessedEventStore store;
  @Autowired private NotificationProperties properties;

  private final EventEnvelopeCodec codec =
      new EventEnvelopeCodec(new ObjectMapper().findAndRegisterModules(), 1);
  private final Channel channel = mock(Channel.class);

  private OrderEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = consumerWith(properties);
    clear();
  }

  @AfterEach
  void tearDown() {
    clear();
  }

  private void clear() {
    jdbc.update("delete from notification_logs");
    jdbc.update("delete from processed_events");
  }

  private OrderEventConsumer consumerWith(NotificationProperties props) {
    NotificationService service =
        new NotificationService(
            new NotificationLogRepository(jdbc),
            new TemplateRenderer(),
            props,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new OrderEventConsumer(codec, store, props, service);
  }

  private Message message(EventEnvelope envelope) {
    MessageProperties props = new MessageProperties();
    props.setDeliveryTag(1L);
    return new Message(codec.encode(envelope).getBytes(StandardCharsets.UTF_8), props);
  }

  private EventEnvelope confirmed(String reuseEventId) {
    Map<String, Object> payload =
        Map.of(
            "orderId",
            UUID.randomUUID().toString(),
            "userId",
            UUID.randomUUID().toString(),
            "total",
            200000,
            "currency",
            "VND",
            "itemCount",
            2,
            "confirmedAt",
            NOW.toString());
    EventEnvelope fresh = EventEnvelope.create("order.confirmed.v1", "order-service", payload);
    return reuseEventId == null
        ? fresh
        : new EventEnvelope(
            reuseEventId,
            fresh.eventType(),
            fresh.version(),
            fresh.occurredAt(),
            fresh.producer(),
            fresh.traceId(),
            payload);
  }

  private long count() {
    Long c = jdbc.queryForObject("select count(*) from notification_logs", Long.class);
    return c == null ? 0 : c;
  }

  @Test
  @DisplayName("order.confirmed → 1 dòng notification_logs SENT, subject đã render, ack")
  void shouldSendEmailAndAck() throws Exception {
    EventEnvelope event = confirmed(null);

    consumer.onMessage(message(event), channel);

    assertThat(count()).isEqualTo(1);
    Map<String, Object> row =
        jdbc.queryForMap("select event_id, template, subject, status from notification_logs");
    assertThat(row.get("event_id")).hasToString(event.eventId());
    assertThat(row.get("template")).isEqualTo("order.confirmed");
    assertThat((String) row.get("subject")).contains("đã được xác nhận");
    assertThat(row.get("status")).isEqualTo("SENT");
    verify(channel).basicAck(1L, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("cùng eventId 2 lần → vẫn 1 dòng (dedupe), lần 2 ack luôn, không DLQ")
  void shouldDedupeByEventId() throws Exception {
    EventEnvelope first = confirmed(null);
    EventEnvelope sameId = confirmed(first.eventId());

    consumer.onMessage(message(first), channel);
    consumer.onMessage(message(sameId), channel);

    assertThat(count()).isEqualTo(1);
    verify(channel, times(2)).basicAck(1L, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("fail-mode (env=local) → thử 3 lần rồi nack(requeue=false) = DLQ, KHÔNG ghi log")
  void shouldRetryThenNackWhenHandlerFails() throws Exception {
    OrderEventConsumer failing =
        consumerWith(
            new NotificationProperties("local", true, List.of(Duration.ZERO, Duration.ZERO), 50));

    failing.onMessage(message(confirmed(null)), channel);

    assertThat(count()).isZero();
    verify(channel).basicNack(1L, false, false);
    verify(channel, never()).basicAck(anyLong(), anyBoolean());
    // Marker dedupe phải được XOÁ ⇒ replay từ DLQ còn chạy lại được.
    Long processed = jdbc.queryForObject("select count(*) from processed_events", Long.class);
    assertThat(processed).isZero();
  }

  @Test
  @DisplayName("fail-mode nhưng env!=local → bỏ qua bơm lỗi (fail-closed), vẫn gửi bình thường")
  void shouldIgnoreFailModeOutsideLocal() throws Exception {
    OrderEventConsumer prodLike =
        consumerWith(new NotificationProperties("prod", true, List.of(Duration.ZERO), 50));

    prodLike.onMessage(message(confirmed(null)), channel);

    assertThat(count()).isEqualTo(1);
    verify(channel).basicAck(1L, false);
  }
}
