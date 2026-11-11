package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Retention của bảng dedupe (`TD-30`, `SL-4.0`). */
class ProcessedEventRetentionTest {

  private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private final ProcessedEventStore store = mock(ProcessedEventStore.class);
  private final EventProperties properties = new EventProperties();

  @Test
  @DisplayName("sweep: cutoff = now − retention (7d mặc định), batch = 1000 mặc định")
  void sweepUsesDefaultRetentionAndBatch() {
    when(store.deleteProcessedBefore(any(), eq(1000))).thenReturn(4);

    int deleted = new ProcessedEventRetention(store, properties, CLOCK).sweep();

    assertThat(deleted).isEqualTo(4);
    verify(store).deleteProcessedBefore(eq(NOW.minus(Duration.ofDays(7))), eq(1000));
  }

  @Test
  @DisplayName("sweep: retention + batch đọc từ cấu hình (override được bằng env)")
  void sweepUsesConfiguredRetentionAndBatch() {
    properties.getDedup().setRetention(Duration.ofDays(30));
    properties.getDedup().setCleanupBatchSize(50);
    when(store.deleteProcessedBefore(any(), eq(50))).thenReturn(0);

    new ProcessedEventRetention(store, properties, CLOCK).sweep();

    verify(store).deleteProcessedBefore(eq(NOW.minus(Duration.ofDays(30))), eq(50));
  }

  @Test
  @DisplayName("sweep: 0 row ⇒ không log (không spam log mỗi nhịp khi bảng đã sạch)")
  void sweepWithoutRowsDoesNotThrow() {
    when(store.deleteProcessedBefore(any(), any(Integer.class))).thenReturn(0);

    assertThat(new ProcessedEventRetention(store, properties, CLOCK).sweep()).isZero();
    verify(store, never()).markProcessed(any(), any());
  }

  @Test
  @DisplayName(
      "Dedup.autoCreateSchema bind được từ shoplab.events.dedup.auto-create-schema (fix bug)")
  void dedupObjectExposesAutoCreateSchema() {
    // Đây là bất biến cấu hình quan trọng: khoá YAML nằm dưới `dedup:` nên property PHẢI là object
    // lồng nhau. Trước SL-4.0 property là field flat ⇒ 3 service khai `false` mà vẫn auto-create.
    assertThat(properties.getDedup().isAutoCreateSchema()).isTrue();
    properties.getDedup().setAutoCreateSchema(false);
    assertThat(properties.getDedup().isAutoCreateSchema()).isFalse();
  }
}
