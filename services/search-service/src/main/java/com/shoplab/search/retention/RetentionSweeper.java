package com.shoplab.search.retention;

import com.shoplab.commons.events.ProcessedEventRetention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job retention của DB {@code search} (`TD-30`, `SL-4.0`): chỉ dọn bảng dedupe {@code
 * processed_events}.
 *
 * <p>Index {@code search_product} **không** dọn theo tuổi: nó là trạng thái hiện tại (1
 * row/product), xoá theo tuổi là xoá sản phẩm khỏi kết quả tìm kiếm. Việc "dọn" index là job
 * reconcile (`IndexDriftChecker`) — khác hẳn ngữ nghĩa retention.
 */
@Component
public class RetentionSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(RetentionSweeper.class);

  private final ProcessedEventRetention processedEvents;

  public RetentionSweeper(ProcessedEventRetention processedEvents) {
    this.processedEvents = processedEvents;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.retention.sweep-interval:1h}",
      initialDelayString = "${shoplab.retention.sweep-initial-delay:5m}")
  public void sweep() {
    try {
      processedEvents.sweep();
    } catch (RuntimeException ex) {
      LOG.error("processed_events retention: nhịp dọn lỗi — bỏ qua nhịp này", ex);
    }
  }
}
