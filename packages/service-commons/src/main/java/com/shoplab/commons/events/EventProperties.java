package com.shoplab.commons.events;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình hạ tầng event ({@code shoplab.events.*}) — đọc từ env theo 12-factor.
 *
 * <pre>
 * SHOPLAB_EVENTS_ENABLED=true
 * SHOPLAB_EVENTS_MAX_VERSION=1
 * SHOPLAB_EVENTS_PUBLISH_CONFIRM_TIMEOUT=5s
 * SHOPLAB_EVENTS_RETRY_DELAYS=200ms,1s,3s
 * SHOPLAB_EVENTS_AUTO_CREATE_SCHEMA=true
 * </pre>
 */
@ConfigurationProperties(prefix = "shoplab.events")
public class EventProperties {

  /** Bật/tắt toàn bộ hạ tầng event của service (mặc định bật khi có AMQP trên classpath). */
  private boolean enabled = true;

  /** Version envelope cao nhất mà service này hiểu — nhận version cao hơn ⇒ DLQ. */
  private int maxVersion = 1;

  /** Chờ publisher confirm bao lâu trước khi coi là "chưa gửi" (relay sẽ thử lại). */
  private Duration publishConfirmTimeout = Duration.ofSeconds(5);

  /** Backoff giữa các lần thử trong consumer (ADR-0007 D5: 3 lần). */
  private List<Duration> retryDelays =
      List.of(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofSeconds(3));

  /**
   * Commons tự tạo bảng {@code processed_events} nếu chưa có (tắt khi service quản DDL bằng
   * Flyway).
   */
  private boolean autoCreateSchema = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxVersion() {
    return maxVersion;
  }

  public void setMaxVersion(int maxVersion) {
    this.maxVersion = maxVersion;
  }

  public Duration getPublishConfirmTimeout() {
    return publishConfirmTimeout;
  }

  public void setPublishConfirmTimeout(Duration publishConfirmTimeout) {
    this.publishConfirmTimeout = publishConfirmTimeout;
  }

  public List<Duration> getRetryDelays() {
    return retryDelays;
  }

  public void setRetryDelays(List<Duration> retryDelays) {
    this.retryDelays = retryDelays;
  }

  public boolean isAutoCreateSchema() {
    return autoCreateSchema;
  }

  public void setAutoCreateSchema(boolean autoCreateSchema) {
    this.autoCreateSchema = autoCreateSchema;
  }
}
