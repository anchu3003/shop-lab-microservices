package com.shoplab.notification.retention;

import com.shoplab.commons.events.ProcessedEventRetention;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.log.NotificationLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job retention của DB {@code notification} (`TD-30`, `SL-4.0`): dọn {@code notification_logs} (dữ
 * liệu **nghiệp vụ**, 90 ngày) và bảng dedupe {@code processed_events} (dữ liệu **hạ tầng**, 7
 * ngày).
 *
 * <p>Hai ngưỡng khác nhau là cố ý: log email là thứ khách còn tra, marker dedupe chỉ cần sống lâu
 * hơn khả năng redeliver của broker (`EventProperties.Dedup#retention`).
 */
@Component
public class RetentionSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(RetentionSweeper.class);

  private final NotificationLogRepository logs;
  private final ProcessedEventRetention processedEvents;
  private final NotificationProperties properties;
  private final Clock clock;

  public RetentionSweeper(
      NotificationLogRepository logs,
      ProcessedEventRetention processedEvents,
      NotificationProperties properties,
      Clock clock) {
    this.logs = logs;
    this.processedEvents = processedEvents;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.retention.sweep-interval:1h}",
      initialDelayString = "${shoplab.retention.sweep-initial-delay:5m}")
  public void sweep() {
    sweepNotificationLogs();
    sweepProcessedEvents();
  }

  /**
   * @return số row log đã xoá (dùng cho test; job thật bỏ qua giá trị trả về)
   */
  public int sweepNotificationLogs() {
    try {
      Duration retention = properties.logRetention();
      Instant cutoff = Instant.now(clock).minus(retention);
      int deleted = logs.deleteOlderThan(cutoff, properties.cleanupBatchSize());
      if (deleted > 0) {
        LOG.info(
            "notification_logs retention: đã xoá {} row cũ hơn {} (giữ {} · batch {})",
            deleted,
            cutoff,
            retention,
            properties.cleanupBatchSize());
      }
      return deleted;
    } catch (RuntimeException ex) {
      LOG.error("notification_logs retention: nhịp dọn lỗi — bỏ qua nhịp này", ex);
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
