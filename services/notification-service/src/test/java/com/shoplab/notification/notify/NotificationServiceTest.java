package com.shoplab.notification.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.log.NotificationLogRepository;
import com.shoplab.notification.template.TemplateRenderer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit test đường "gửi" notification (SL-3.4) — mock repository, không cần DB. */
class NotificationServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
  private static final String EVENT_ID = "33333333-3333-4333-8333-000000000003";
  private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-000000000001");
  private static final UUID ORDER = UUID.fromString("22222222-2222-4222-8222-000000000002");
  private static final NotificationProperties LOCAL =
      new NotificationProperties("local", false, List.of(Duration.ofMillis(10)), 50);

  private final NotificationLogRepository logs = mock(NotificationLogRepository.class);
  private final TemplateRenderer renderer = new TemplateRenderer();

  @Test
  @DisplayName("order.confirmed → render subject/body từ payload rồi ghi 1 dòng SENT")
  void shouldSendConfirmed() {
    when(logs.findTemplate("order.confirmed"))
        .thenReturn(
            Optional.of(
                new NotificationLogRepository.Template(
                    "Đơn {{orderId}} đã xác nhận",
                    "Đơn {{orderId}} trị giá {{total}} {{currency}}")));

    service(LOCAL).onOrderConfirmed(confirmedEvent());

    NotificationLogRepository.Draft draft = captureDraft();
    assertThat(draft.eventId()).isEqualTo(UUID.fromString(EVENT_ID));
    assertThat(draft.userId()).isEqualTo(USER);
    assertThat(draft.orderId()).isEqualTo(ORDER);
    assertThat(draft.template()).isEqualTo("order.confirmed");
    assertThat(draft.subject()).isEqualTo("Đơn " + ORDER + " đã xác nhận");
    assertThat(draft.body()).isEqualTo("Đơn " + ORDER + " trị giá 70000 VND");
    assertThat(draft.sentAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("recipient là email giả dẫn xuất từ userId (mock provider, không gửi ra ngoài)")
  void shouldUseMockRecipient() {
    when(logs.findTemplate("order.confirmed"))
        .thenReturn(Optional.of(new NotificationLogRepository.Template("s", "b")));

    service(LOCAL).onOrderConfirmed(confirmedEvent());

    assertThat(captureDraft().recipient()).isEqualTo("user-11111111@shoplab.local");
  }

  @Test
  @DisplayName("order.cancelled → dùng template order.cancelled (không lẫn với confirmed)")
  void shouldSendCancelled() {
    when(logs.findTemplate("order.cancelled"))
        .thenReturn(
            Optional.of(
                new NotificationLogRepository.Template("Huỷ {{orderId}}", "vì {{reason}}")));

    service(LOCAL).onOrderCancelled(cancelledEvent());

    NotificationLogRepository.Draft draft = captureDraft();
    assertThat(draft.template()).isEqualTo("order.cancelled");
    assertThat(draft.body()).isEqualTo("vì hết hàng");
  }

  @Test
  @DisplayName("template chưa seed → ném lỗi để consumer retry rồi DLQ (không ghi log giả)")
  void shouldFailWhenTemplateMissing() {
    when(logs.findTemplate("order.confirmed")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(LOCAL).onOrderConfirmed(confirmedEvent()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("thiếu template 'order.confirmed'");
    verify(logs, never()).save(any());
  }

  @Test
  @DisplayName("fail-mode ở env=local → ném lỗi, KHÔNG ghi dòng nào (để test retry/DLQ)")
  void shouldFailWhenFailModeInLocal() {
    NotificationProperties failing =
        new NotificationProperties("local", true, List.of(Duration.ofMillis(10)), 50);

    assertThatThrownBy(() -> service(failing).onOrderConfirmed(confirmedEvent()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SHOPLAB_NOTIFICATION_FAIL_MODE");
    verify(logs, never()).save(any());
  }

  @Test
  @DisplayName("fail-mode=true nhưng env!=local → bỏ qua (fail-closed), vẫn gửi bình thường")
  void shouldIgnoreFailModeOutsideLocal() {
    NotificationProperties prod =
        new NotificationProperties("prod", true, List.of(Duration.ofMillis(10)), 50);
    when(logs.findTemplate("order.confirmed"))
        .thenReturn(Optional.of(new NotificationLogRepository.Template("s", "b")));

    service(prod).onOrderConfirmed(confirmedEvent());

    verify(logs).save(any());
  }

  @Test
  @DisplayName("payload thiếu userId → IllegalArgumentException (payload hỏng, không đoán)")
  void shouldRejectPayloadWithoutUserId() {
    Map<String, Object> payload = new HashMap<>(confirmedEvent().payload());
    payload.remove("userId");
    EventEnvelope broken =
        new EventEnvelope(EVENT_ID, "order.confirmed.v1", 1, NOW, "order-service", "t", payload);

    assertThatThrownBy(() -> service(LOCAL).onOrderConfirmed(broken))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("thiếu field bắt buộc: userId");
  }

  // ---------------------------------------------------------------- helpers

  private NotificationService service(NotificationProperties properties) {
    return new NotificationService(logs, renderer, properties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private NotificationLogRepository.Draft captureDraft() {
    ArgumentCaptor<NotificationLogRepository.Draft> captor =
        ArgumentCaptor.forClass(NotificationLogRepository.Draft.class);
    verify(logs).save(captor.capture());
    return captor.getValue();
  }

  private static EventEnvelope confirmedEvent() {
    return new EventEnvelope(
        EVENT_ID,
        "order.confirmed.v1",
        1,
        NOW,
        "order-service",
        "trace-1",
        Map.of(
            "orderId",
            ORDER.toString(),
            "userId",
            USER.toString(),
            "total",
            70000,
            "currency",
            "VND",
            "itemCount",
            1));
  }

  private static EventEnvelope cancelledEvent() {
    return new EventEnvelope(
        EVENT_ID,
        "order.cancelled.v1",
        1,
        NOW,
        "order-service",
        "trace-2",
        Map.of(
            "orderId", ORDER.toString(),
            "userId", USER.toString(),
            "reason", "hết hàng"));
  }
}
