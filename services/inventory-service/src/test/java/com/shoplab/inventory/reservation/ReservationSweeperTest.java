package com.shoplab.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test {@link ReservationSweeper}: chỉ kiểm <b>hành vi của lớp mỏng</b> (gọi service đúng thời
 * điểm, không để lỗi thoát ra). Việc quét thật được kiểm ở IT, nơi scheduler chạy với interval
 * ngắn.
 */
class ReservationSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

  private final ReservationService service = mock(ReservationService.class);
  private final ReservationSweeper sweeper =
      new ReservationSweeper(service, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("sweep: gọi expireOverdue đúng 1 lần với thời điểm của Clock")
  void sweepShouldDelegateOnce() {
    when(service.expireOverdue(NOW)).thenReturn(3);

    sweeper.sweep();

    verify(service).expireOverdue(NOW);
  }

  @Test
  @DisplayName("sweep: service lỗi → KHÔNG ném ra ngoài (nhịp sau vẫn chạy)")
  void sweepShouldSwallowException() {
    when(service.expireOverdue(NOW))
        .thenThrow(new IllegalStateException("DB tạm thời không ghi được"));

    sweeper.sweep(); // không được ném

    verify(service).expireOverdue(NOW);
    assertThat(true).isTrue(); // tới được đây nghĩa là exception đã bị bắt
  }
}
