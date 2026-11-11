package com.shoplab.commons.events;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job retention cho bảng dedupe {@code processed_events} (`TD-30`, `SL-4.0`).
 *
 * <p><b>Vì sao ở commons nhưng KHÔNG tự {@code @Scheduled}</b>: câu SQL và ngữ nghĩa "xoá theo
 * tuổi" là giống nhau ở mọi consumer ⇒ để một chỗ. Nhưng <b>nhịp chạy</b> là quyết định vận hành
 * của từng service (DB to/nhỏ khác nhau, và tắt job ở một service không được kéo theo service khác)
 * ⇒ mỗi service tự gọi {@link #sweep()} từ sweeper của nó. Hệ quả tốt: không có scheduler ẩn trong
 * library — người đọc code service thấy rõ job nào chạy ở đâu.
 *
 * <p><b>Bất biến</b>: {@code retention} phải <b>≥ retention của bảng {@code outbox}</b> — xem
 * javadoc {@link EventProperties.Dedup#getRetention()}.
 */
public class ProcessedEventRetention {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessedEventRetention.class);

  private final ProcessedEventStore store;
  private final EventProperties properties;
  private final Clock clock;

  public ProcessedEventRetention(
      ProcessedEventStore store, EventProperties properties, Clock clock) {
    this.store = store;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Một nhịp dọn: xoá tối đa {@code cleanupBatchSize} marker cũ hơn {@code retention}.
   *
   * <p>Chỉ xoá **một batch** mỗi nhịp (không lặp tới khi sạch): bảng lớn thì dọn dần qua nhiều nhịp
   * còn hơn một transaction dài khoá bảng trong lúc traffic đang chạy.
   *
   * @return số row đã xoá
   */
  public int sweep() {
    Duration retention = properties.getDedup().getRetention();
    int batch = properties.getDedup().getCleanupBatchSize();
    Instant cutoff = Instant.now(clock).minus(retention);
    int deleted = store.deleteProcessedBefore(cutoff, batch);
    if (deleted > 0) {
      LOG.info(
          "processed_events retention: đã xoá {} row cũ hơn {} (giữ {} · batch {})",
          deleted,
          cutoff,
          retention,
          batch);
    }
    return deleted;
  }
}
