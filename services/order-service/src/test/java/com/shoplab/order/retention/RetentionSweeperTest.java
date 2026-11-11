package com.shoplab.order.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.events.ProcessedEventRetention;
import com.shoplab.order.outbox.OutboxProperties;
import com.shoplab.order.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Retention của DB {@code order} (`TD-29` + `TD-30`, `SL-4.0`). */
class RetentionSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private final OutboxRepository outbox = mock(OutboxRepository.class);
  private final ProcessedEventRetention processedEvents = mock(ProcessedEventRetention.class);
  private final OutboxProperties properties = new OutboxProperties();

  private RetentionSweeper sweeper() {
    return new RetentionSweeper(outbox, processedEvents, properties, CLOCK);
  }

  @Test
  @DisplayName("outbox: xoá row SENT cũ hơn 7 ngày, batch 1000 (mặc định)")
  void sweepOutboxUsesDefaultRetention() {
    when(outbox.deleteSentOlderThan(any(), eq(1000))).thenReturn(12);

    assertThat(sweeper().sweepOutbox()).isEqualTo(12);
    verify(outbox).deleteSentOlderThan(eq(NOW.minus(Duration.ofDays(7))), eq(1000));
  }

  @Test
  @DisplayName("outbox: retention + batch đọc từ cấu hình (override bằng env)")
  void sweepOutboxUsesConfiguredValues() {
    properties.setRetention(Duration.ofDays(1));
    properties.setCleanupBatchSize(5);
    when(outbox.deleteSentOlderThan(any(), eq(5))).thenReturn(1);

    sweeper().sweepOutbox();

    verify(outbox).deleteSentOlderThan(eq(NOW.minus(Duration.ofDays(1))), eq(5));
  }

  @Test
  @DisplayName("outbox: DB lỗi ⇒ nuốt exception (scheduler không chết), trả 0")
  void sweepOutboxSwallowsRuntimeException() {
    when(outbox.deleteSentOlderThan(any(), any(Integer.class)))
        .thenThrow(new IllegalStateException("db đang bận"));

    assertThat(sweeper().sweepOutbox()).isZero();
  }

  @Test
  @DisplayName("sweep(): chạy cả 2 phần — outbox + processed_events")
  void sweepRunsBothParts() {
    when(outbox.deleteSentOlderThan(any(), any(Integer.class))).thenReturn(2);
    when(processedEvents.sweep()).thenReturn(3);

    sweeper().sweep();

    verify(outbox).deleteSentOlderThan(any(), any(Integer.class));
    verify(processedEvents).sweep();
  }

  @Test
  @DisplayName(
      "processed_events: dedupe store lỗi ⇒ nuốt exception, không kéo sập nhịp outbox sau đó")
  void sweepProcessedEventsSwallowsRuntimeException() {
    when(processedEvents.sweep()).thenThrow(new IllegalStateException("db đang bận"));

    assertThat(sweeper().sweepProcessedEvents()).isZero();
  }
}
