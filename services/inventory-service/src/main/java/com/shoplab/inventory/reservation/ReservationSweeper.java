package com.shoplab.inventory.reservation;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nền dọn reservation hết hạn (SL-2.3).
 *
 * <p>Vì sao phải có: {@code commit} đã có lazy expire, nhưng user <b>bỏ ngang</b> (không commit,
 * không release) thì không có ai kích hoạt đường đó ⇒ {@code reserved} bị giữ vĩnh viễn ⇒ kho còn
 * hàng mà không ai mua được.
 *
 * <p>Thiết kế:
 *
 * <ul>
 *   <li><b>fixedDelay</b> (không phải fixedRate): nhịp sau chỉ bắt đầu sau khi nhịp trước xong ⇒ DB
 *       chậm thì job tự giãn ra, không dồn hàng đợi.
 *   <li>Interval/batch đọc từ env ({@code SHOPLAB_INVENTORY_SWEEP_*}) — chỉnh được mà không build
 *       lại; IT đặt interval ngắn để kiểm job thật thay vì gọi hàm tay.
 *   <li><b>Bắt {@code RuntimeException}</b>: exception lọt ra khỏi {@code @Scheduled} sẽ làm nhịp
 *       đó chết và log đầy stack trace. Bắt lại thì nhịp sau vẫn chạy, và lỗi vẫn được ghi rõ.
 * </ul>
 *
 * <p>Chạy nhiều instance vẫn an toàn: {@code ReservationService.expireOverdue} dùng {@code FOR
 * UPDATE SKIP LOCKED} + conditional {@code markStatus} (xem {@code TD-15}).
 */
@Component
public class ReservationSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(ReservationSweeper.class);

  private final ReservationService service;
  private final Clock clock;

  public ReservationSweeper(ReservationService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Một nhịp quét. {@code initialDelay} để không tranh DB lúc service vừa khởi động (Flyway/Hikari
   * còn đang bận).
   */
  @Scheduled(
      fixedDelayString = "${shoplab.inventory.sweep-interval-ms:60000}",
      initialDelayString = "${shoplab.inventory.sweep-initial-delay-ms:30000}")
  public void sweep() {
    try {
      int expired = service.expireOverdue(Instant.now(clock));
      if (expired > 0) {
        LOG.info("sweeper: đã expire {} reservation quá hạn và trả chỗ", expired);
      }
    } catch (RuntimeException ex) {
      // Không để lỗi 1 nhịp giết job; bỏ nhịp này và thử lại ở nhịp sau.
      LOG.error("sweeper: nhịp quét lỗi — bỏ qua nhịp này", ex);
    }
  }
}
