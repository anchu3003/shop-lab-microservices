package com.shoplab.search.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.events.ProcessedEventRetention;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Retention của DB {@code search} (`TD-30`, `SL-4.0`) — chỉ dọn {@code processed_events}. */
class RetentionSweeperTest {

  private final ProcessedEventRetention processedEvents = mock(ProcessedEventRetention.class);

  @Test
  @DisplayName("sweep: gọi retention của bảng dedupe (không chạm index search_product)")
  void sweepDelegatesToProcessedEventsRetention() {
    when(processedEvents.sweep()).thenReturn(3);

    new RetentionSweeper(processedEvents).sweep();

    verify(processedEvents).sweep();
  }

  @Test
  @DisplayName("lỗi ở retention ⇒ nuốt exception (scheduler không chết)")
  void sweepSwallowsRuntimeException() {
    when(processedEvents.sweep()).thenThrow(new IllegalStateException("db đang bận"));

    new RetentionSweeper(processedEvents).sweep();

    assertThat(true).isTrue(); // tới được đây nghĩa là không ném ra ngoài
  }
}
