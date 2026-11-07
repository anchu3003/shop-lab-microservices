package com.shoplab.promotion.lock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test lớp mỏng {@link VoucherLockSweeper}: gọi service đúng thời điểm và <b>không để lỗi thoát
 * ra</b> (một nhịp lỗi không được giết scheduler). Việc quét thật được kiểm ở IT với interval ngắn.
 */
class VoucherLockSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

  private final VoucherLockService service = mock(VoucherLockService.class);
  private final VoucherLockSweeper sweeper =
      new VoucherLockSweeper(service, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("sweep: gọi expireOverdue đúng 1 lần với thời điểm của Clock")
  void sweepShouldDelegateOnce() {
    when(service.expireOverdue(NOW)).thenReturn(2);

    sweeper.sweep();

    verify(service).expireOverdue(NOW);
  }

  @Test
  @DisplayName("sweep: service lỗi → KHÔNG ném ra ngoài")
  void sweepShouldSwallowException() {
    when(service.expireOverdue(NOW))
        .thenThrow(new IllegalStateException("DB tạm thời không ghi được"));

    sweeper.sweep(); // không được ném

    verify(service).expireOverdue(NOW);
  }
}
