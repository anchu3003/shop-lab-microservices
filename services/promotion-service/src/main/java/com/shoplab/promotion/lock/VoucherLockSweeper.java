package com.shoplab.promotion.lock;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nền trả lượt voucher bị giữ quá TTL (SL-2.4) — tương ứng cột "User không thanh toán trong
 * 15'" ở {@code architecture.md} §6.1 (job của inventory trả stock, job này trả voucher).
 *
 * <p>Thiết kế theo đúng khuôn đã kiểm ở `SL-2.3`: {@code fixedDelay} (nhịp sau chờ nhịp trước
 * xong), interval/batch đọc từ env, và bắt {@code RuntimeException} để một nhịp lỗi không giết
 * scheduler. Chạy nhiều instance vẫn an toàn nhờ {@code FOR UPDATE SKIP LOCKED} + conditional
 * {@code markStatus}.
 */
@Component
public class VoucherLockSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(VoucherLockSweeper.class);

  private final VoucherLockService service;
  private final Clock clock;

  public VoucherLockSweeper(VoucherLockService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.promotion.sweep-interval-ms:60000}",
      initialDelayString = "${shoplab.promotion.sweep-initial-delay-ms:30000}")
  public void sweep() {
    try {
      int expired = service.expireOverdue(Instant.now(clock));
      if (expired > 0) {
        LOG.info("sweeper: đã expire {} voucher lock quá hạn và trả lượt", expired);
      }
    } catch (RuntimeException ex) {
      LOG.error("sweeper: nhịp quét lỗi — bỏ qua nhịp này", ex);
    }
  }
}
