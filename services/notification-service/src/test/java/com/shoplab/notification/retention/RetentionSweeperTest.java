package com.shoplab.notification.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.events.ProcessedEventRetention;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.log.NotificationLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Retention của DB {@code notification} (`TD-30`, `SL-4.0`). */
class RetentionSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private final NotificationLogRepository logs = mock(NotificationLogRepository.class);
  private final ProcessedEventRetention processedEvents = mock(ProcessedEventRetention.class);

  private RetentionSweeper sweeper(NotificationProperties properties) {
    return new RetentionSweeper(logs, processedEvents, properties, CLOCK);
  }

  private static NotificationProperties props(Duration retention, int batch) {
    return new NotificationProperties(
        "local", false, List.of(Duration.ofMillis(10)), 50, retention, batch);
  }

  @Test
  @DisplayName(
      "notification_logs: điểm cắt mặc định 90 ngày (dữ liệu nghiệp vụ, không phải 7 ngày)")
  void sweepLogsUsesDefaultRetention() {
    when(logs.deleteOlderThan(any(), eq(1000))).thenReturn(7);

    assertThat(sweeper(props(Duration.ofDays(90), 1000)).sweepNotificationLogs()).isEqualTo(7);
    verify(logs).deleteOlderThan(eq(NOW.minus(Duration.ofDays(90))), eq(1000));
  }

  @Test
  @DisplayName("notification_logs: retention + batch đọc từ cấu hình")
  void sweepLogsUsesConfiguredValues() {
    when(logs.deleteOlderThan(any(), eq(25))).thenReturn(0);

    sweeper(props(Duration.ofDays(30), 25)).sweepNotificationLogs();

    verify(logs).deleteOlderThan(eq(NOW.minus(Duration.ofDays(30))), eq(25));
  }

  @Test
  @DisplayName("notification_logs: DB lỗi ⇒ nuốt exception, vẫn chạy tiếp phần processed_events")
  void sweepSwallowsRuntimeExceptionPerPart() {
    when(logs.deleteOlderThan(any(), any(Integer.class)))
        .thenThrow(new IllegalStateException("db đang bận"));
    when(processedEvents.sweep()).thenReturn(2);

    sweeper(props(Duration.ofDays(90), 1000)).sweep();

    verify(processedEvents).sweep();
  }
}
