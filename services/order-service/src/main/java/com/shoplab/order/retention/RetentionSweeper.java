package com.shoplab.order.retention;

import com.shoplab.commons.events.ProcessedEventRetention;
import com.shoplab.order.outbox.OutboxProperties;
import com.shoplab.order.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job retention của DB {@code order} (`TD-29` + `TD-30`, `SL-4.0`): dọn bảng {@code outbox} (row
 * {@code SENT} cũ) và bảng dedupe {@code processed_events}.
 *
 * <p><b>Vì sao hai việc trong một sweeper</b>: cùng DB, cùng nhịp, cùng lý do (dữ liệu phụ tăng
 * theo traffic). Tách thành 2 job chỉ làm tăng số nhịp scheduler mà không thêm giá trị.
 *
 * <p><b>Vì sao {@code fixedDelay} chứ không {@code fixedRate}</b>: nhịp sau chỉ bắt đầu sau khi
 * nhịp trước xong ⇒ job không bao giờ chồng lên chính nó khi bảng lớn (bài học `SL-2.3`).
 *
 * <p>{@code try/catch RuntimeException} ở từng phần: một nhịp lỗi (DB bận, bảng bị lock) không được
 * giết scheduler — mất job dọn một cách im lặng còn tệ hơn lỗi ầm ĩ.
 */
@Component
public class RetentionSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(RetentionSweeper.class);

  private final OutboxRepository outbox;
  private final ProcessedEventRetention processedEvents;
  private final OutboxProperties properties;
  private final Clock clock;

  public RetentionSweeper(
      OutboxRepository outbox,
      ProcessedEventRetention processedEvents,
      OutboxProperties properties,
      Clock clock) {
    this.outbox = outbox;
    this.processedEvents = processedEvents;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.retention.sweep-interval:1h}",
      initialDelayString = "${shoplab.retention.sweep-initial-delay:5m}")
  public void sweep() {
    sweepOutbox();
    sweepProcessedEvents();
  }

  /**
   * @return số row outbox đã xoá (dùng cho test; job thật bỏ qua giá trị trả về)
   */
  public int sweepOutbox() {
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
      return deleted;
    } catch (RuntimeException ex) {
      LOG.error("outbox retention: nhịp dọn lỗi — bỏ qua nhịp này", ex);
      return 0;
    }
  }

  public int sweepProcessedEvents() {
    try {
      return processedEvents.sweep();
    } catch (RuntimeException ex) {
      LOG.error("processed_events retention: nhịp dọn lỗi — bỏ qua nhịp này", ex);
      return 0;
    }
  }
}
