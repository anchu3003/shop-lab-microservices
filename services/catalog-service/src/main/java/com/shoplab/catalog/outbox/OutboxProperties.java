package com.shoplab.catalog.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình outbox relay (`shoplab.outbox.*`).
 *
 * <pre>
 * SHOPLAB_OUTBOX_INTERVAL=1s
 * SHOPLAB_OUTBOX_INITIAL_DELAY=5s
 * SHOPLAB_OUTBOX_BATCH_SIZE=100
 * SHOPLAB_OUTBOX_CLAIM_TIMEOUT=30s
 * </pre>
 *
 * <p>{@code interval} 1 s là đánh đổi có chủ ý (ADR-0007 D4): trễ tối đa ~1 s cho email/index — đổi
 * lại không phải thêm hạ tầng CDC. Tăng lên khi bảng outbox lớn (quét tốn I/O) hoặc giảm khi cần
 * realtime.
 */
@ConfigurationProperties(prefix = "shoplab.outbox")
public class OutboxProperties {

  /** Nhịp relay. */
  private Duration interval = Duration.ofSeconds(1);

  /** Chờ lần quét đầu để app khởi động xong + DB migration xong. */
  private Duration initialDelay = Duration.ofSeconds(5);

  /** Số row mỗi nhịp — nhỏ để transaction claim ngắn. */
  private int batchSize = 100;

  /**
   * Visibility timeout của claim: row bị claim quá lâu (relay chết giữa đường) thì coi như bỏ và
   * cho relay khác lấy lại. Phải lớn hơn thời gian publish tối đa (confirm timeout 5 s).
   */
  private Duration claimTimeout = Duration.ofSeconds(30);

  public Duration getInterval() {
    return interval;
  }

  public void setInterval(Duration interval) {
    this.interval = interval;
  }

  public Duration getInitialDelay() {
    return initialDelay;
  }

  public void setInitialDelay(Duration initialDelay) {
    this.initialDelay = initialDelay;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getClaimTimeout() {
    return claimTimeout;
  }

  public void setClaimTimeout(Duration claimTimeout) {
    this.claimTimeout = claimTimeout;
  }
}
