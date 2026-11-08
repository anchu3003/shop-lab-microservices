package com.shoplab.order.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventPublishException;
import com.shoplab.commons.events.EventPublisher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * {@link OutboxRelay} (SL-3.3) — những hành vi **không** quan sát được khi hệ chạy bình thường:
 * đánh dấu SENT, giữ PENDING khi lỗi, và {@code eventId} cố định qua các lần thử.
 */
class OutboxRelayTest {

  private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final OutboxRepository outbox = mock(OutboxRepository.class);
  private final EventPublisher publisher = mock(EventPublisher.class);
  private final OutboxProperties properties = new OutboxProperties();

  private OutboxRecord row(UUID id) {
    return new OutboxRecord(
        id,
        "order",
        UUID.randomUUID(),
        "order.confirmed.v1",
        Map.of("orderId", "o-1"),
        OutboxStatus.PENDING,
        1,
        null,
        NOW,
        NOW,
        null);
  }

  @Test
  @DisplayName("publish được ⇒ markSent + eventId = outbox.id (không sinh id mới)")
  void shouldMarkSentAndKeepStableEventId() {
    UUID id = UUID.randomUUID();
    when(outbox.claimPending(eq(10), any(), any())).thenReturn(List.of(row(id)));
    properties.setBatchSize(10);

    int sent = new OutboxRelay(outbox, publisher, properties, clock).drain();

    assertThat(sent).isEqualTo(1);
    verify(outbox).markSent(eq(id), any());
    verify(outbox, never()).markFailed(any(), anyString());

    ArgumentCaptor<EventEnvelope> envelope = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(publisher).publish(envelope.capture());
    // Chốt chặn dedupe ở consumer: retry phải mang CÙNG eventId ⇒ chính là id của row outbox.
    assertThat(envelope.getValue().eventId()).isEqualTo(id.toString());
    assertThat(envelope.getValue().eventType()).isEqualTo("order.confirmed.v1");
    assertThat(envelope.getValue().payload()).containsEntry("orderId", "o-1");
  }

  @Test
  @DisplayName("broker nack ⇒ markFailed (giữ PENDING, có last_error), KHÔNG markSent")
  void shouldKeepPendingWhenPublishFails() {
    UUID id = UUID.randomUUID();
    when(outbox.claimPending(eq(10), any(), any())).thenReturn(List.of(row(id)));
    doThrow(new EventPublishException("broker nack"))
        .when(publisher)
        .publish(any(EventEnvelope.class));
    properties.setBatchSize(10);

    int sent = new OutboxRelay(outbox, publisher, properties, clock).drain();

    assertThat(sent).isZero();
    verify(outbox, never()).markSent(any(), any());
    verify(outbox).markFailed(eq(id), eq("EventPublishException: broker nack"));
  }

  @Test
  @DisplayName("claim rỗng ⇒ không publish gì (nhịp quét im lặng, không log ồn)")
  void shouldDoNothingWhenNoRows() {
    when(outbox.claimPending(any(Integer.class), any(), any())).thenReturn(List.of());

    int sent = new OutboxRelay(outbox, publisher, properties, clock).drain();

    assertThat(sent).isZero();
    verify(publisher, never()).publish(any(EventEnvelope.class));
  }

  @Test
  @DisplayName("một row lỗi KHÔNG chặn các row khác trong cùng nhịp")
  void shouldContinueAfterOneRowFails() {
    UUID bad = UUID.randomUUID();
    UUID good = UUID.randomUUID();
    when(outbox.claimPending(any(Integer.class), any(), any()))
        .thenReturn(List.of(row(bad), row(good)));
    doThrow(new EventPublishException("nack"))
        .doNothing()
        .when(publisher)
        .publish(any(EventEnvelope.class));

    int sent = new OutboxRelay(outbox, publisher, properties, clock).drain();

    assertThat(sent).isEqualTo(1);
    verify(outbox).markFailed(eq(bad), anyString());
    verify(outbox).markSent(eq(good), any());
  }

  @Test
  @DisplayName("claim dùng claim-timeout từ cấu hình (visibility timeout)")
  void shouldUseConfiguredClaimTimeout() {
    properties.setClaimTimeout(Duration.ofSeconds(45));
    when(outbox.claimPending(any(Integer.class), any(), any())).thenReturn(List.of());

    new OutboxRelay(outbox, publisher, properties, clock).drain();

    ArgumentCaptor<Instant> deadline = ArgumentCaptor.forClass(Instant.class);
    verify(outbox).claimPending(any(Integer.class), eq(NOW), deadline.capture());
    assertThat(deadline.getValue()).isEqualTo(NOW.minusSeconds(45));
  }
}
