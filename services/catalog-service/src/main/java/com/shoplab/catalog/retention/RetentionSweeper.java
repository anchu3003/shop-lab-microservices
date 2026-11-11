package com.shoplab.catalog.retention;

import com.shoplab.catalog.outbox.OutboxProperties;
import com.shoplab.catalog.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job retention của bảng {@code outbox} ở DB {@code catalog} (`TD-29`, `SL-4.0`).
 *
 * <p>Catalog là **producer thuần** (không consume) ⇒ chỉ có outbox cần dọn; không có {@code
 * processed_events} để chạm tới.
 *
 * <p>Bản copy thứ 3 của cùng logic (order · payment · catalog) — chấp nhận có ý thức như quyết định
 * ở `TD-32`: mỗi service sở hữu DB và migration của nó, gộp vào commons khi có producer thứ 4.
 */
@Component
public class RetentionSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(RetentionSweeper.class);

  private final OutboxRepository outbox;
  private final OutboxProperties properties;
  private final Clock clock;

  public RetentionSweeper(OutboxRepository outbox, OutboxProperties properties, Clock clock) {
    this.outbox = outbox;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.retention.sweep-interval:1h}",
      initialDelayString = "${shoplab.retention.sweep-initial-delay:5m}")
  public void sweep() {
    try {
      Duration retention = properties.getRetention();
      Instant cutoff = Instant.now(clock).minus(retention);
      int deleted = outbox.deleteSentOlderThan(cutoff, properties.getCleanupBatchSize());
      if (deleted > 0) {
        LOG.info(
            "outbox retention: đã xoá {} row SENT cũ hơn {} (giữ {} · batch {})",
            deleted,
            cutoff,
            retention,
            properties.getCleanupBatchSize());
      }
    } catch (RuntimeException ex) {
      LOG.error("outbox retention: nhịp dọn lỗi — bỏ qua nhịp này", ex);
    }
  }
}
