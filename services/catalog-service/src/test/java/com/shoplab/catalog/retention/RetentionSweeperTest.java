package com.shoplab.catalog.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.outbox.OutboxProperties;
import com.shoplab.catalog.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Retention của bảng {@code outbox} ở DB {@code catalog} (`TD-29`, `SL-4.0`). */
class RetentionSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private final OutboxRepository outbox = mock(OutboxRepository.class);
  private final OutboxProperties properties = new OutboxProperties();

  private RetentionSweeper sweeper() {
    return new RetentionSweeper(outbox, properties, CLOCK);
  }

  @Test
  @DisplayName("xoá row SENT cũ hơn 7 ngày với batch mặc định 1000")
  void sweepUsesDefaultRetention() {
    when(outbox.deleteSentOlderThan(any(), eq(1000))).thenReturn(4);

    sweeper().sweep();

    verify(outbox).deleteSentOlderThan(eq(NOW.minus(Duration.ofDays(7))), eq(1000));
  }

  @Test
  @DisplayName("retention + batch đọc từ cấu hình")
  void sweepUsesConfiguredValues() {
    properties.setRetention(Duration.ofHours(6));
    properties.setCleanupBatchSize(10);

    sweeper().sweep();

    verify(outbox).deleteSentOlderThan(eq(NOW.minus(Duration.ofHours(6))), eq(10));
  }

  @Test
  @DisplayName("DB lỗi ⇒ nuốt exception (scheduler không chết)")
  void sweepSwallowsRuntimeException() {
    when(outbox.deleteSentOlderThan(any(), any(Integer.class)))
        .thenThrow(new IllegalStateException("db đang bận"));

    sweeper().sweep(); // không ném ra ngoài
    assertThat(properties.getRetention()).isPositive();
  }
}
