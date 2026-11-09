package com.shoplab.payment.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình outbox relay của payment ({@code shoplab.outbox.*}, SL-3.5) — cùng ngữ nghĩa với order
 * ({@code SL-3.3}).
 *
 * <p>{@code claim-timeout} phải lớn hơn thời gian publish tối đa (confirm timeout 5 s), nếu không
 * relay khác sẽ lấy lại row đang được gửi.
 */
@ConfigurationProperties(prefix = "shoplab.outbox")
public class OutboxProperties {

  private Duration interval = Duration.ofSeconds(1);

  private Duration initialDelay = Duration.ofSeconds(5);

  private int batchSize = 100;

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
