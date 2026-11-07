package com.shoplab.order.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job dọn {@code idempotency_keys} (TD-14) — bảng này lớn lên theo mỗi request checkout nên phải có
 * ngưỡng.
 *
 * <p>Giữ {@code COMPLETED} trong 24 h (client retry muộn vẫn dedupe được) và {@code IN_PROGRESS}
 * quá 1 h (coi là mồ côi do process chết) — nếu không dọn, client sẽ nhận 409 <b>vĩnh viễn</b> cho
 * một key mà request trước đã crash.
 *
 * <p>Khuôn giống sweeper của inventory/promotion: {@code fixedDelay}, tham số từ env, bắt {@code
 * RuntimeException} để một nhịp lỗi không giết scheduler.
 */
@Component
public class IdempotencySweeper {

  private static final Logger LOG = LoggerFactory.getLogger(IdempotencySweeper.class);

  private final IdempotencyService service;

  public IdempotencySweeper(IdempotencyService service) {
    this.service = service;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.order.sweep-interval-ms:3600000}",
      initialDelayString = "${shoplab.order.sweep-initial-delay-ms:600000}")
  public void sweep() {
    try {
      service.cleanup();
    } catch (RuntimeException ex) {
      LOG.error("idempotency cleanup: nhịp dọn lỗi — bỏ qua nhịp này", ex);
    }
  }
}
