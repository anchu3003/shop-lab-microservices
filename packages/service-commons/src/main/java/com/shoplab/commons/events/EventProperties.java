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
  /** Cấu hình bảng dedupe {@code processed_events} ({@code shoplab.events.dedup.*}). */
  private Dedup dedup = new Dedup();

  /**
   * Có tự start {@code @RabbitListener} khi app khởi động không (mặc định bật).
   *
   * <p><b>Vì sao cần cờ này</b> (gặp thật ở SL-3.4): IT của service có consumer chạy **không có
   * broker**. Boot có {@code spring.rabbitmq.listener.simple.auto-startup} nhưng property đó chỉ áp
   * cho factory do Boot tạo — service này dùng {@code eventListenerContainerFactory} tự khai (cần
   * {@code AcknowledgeMode.MANUAL}) nên **không** bị ảnh hưởng ⇒ container cố kết nối, sai
   * credential ⇒ {@code FatalListenerStartupException: Authentication failure} ⇒ **đỏ cả 17 IT** dù
   * lỗi không liên quan logic. Cờ riêng của commons thì áp cho đúng factory của commons.
   *
   * <p>Cũng dùng được khi cần "vận hành chế độ chỉ-publish": tắt consumer bằng env, không cần build
   * lại image.
   */
  private boolean consumerAutoStartup = true;

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

  public boolean isConsumerAutoStartup() {
    return consumerAutoStartup;
  }

  public void setConsumerAutoStartup(boolean consumerAutoStartup) {
    this.consumerAutoStartup = consumerAutoStartup;
  }

  public Dedup getDedup() {
    return dedup;
  }

  public void setDedup(Dedup dedup) {
    this.dedup = dedup == null ? new Dedup() : dedup;
  }

  /**
   * Cấu hình bảng dedupe {@code processed_events} của **DB service này**.
   *
   * <p><b>Vì sao là object lồng nhau, không phải field flat</b> (bẫy thật, `SL-4.0`): 3 service
   * khai {@code shoplab.events.dedup.auto-create-schema: false} trong `application.yml` nhưng
   * property tương ứng lại là {@code shoplab.events.auto-create-schema} (flat) ⇒ khoá YAML **không
   * bind**, DDL auto-create vẫn chạy, và không có lỗi nào để thấy. Cấu trúc lồng nhau làm khoá YAML
   * đúng nghĩa "đây là cấu hình của bảng dedupe".
   */
  public static class Dedup {

    /** Commons tự tạo bảng nếu chưa có (tắt khi service quản DDL bằng Flyway). */
    private boolean autoCreateSchema = true;

    /**
     * Giữ row dedupe bao lâu (mặc định 7 ngày).
     *
     * <p><b>Ràng buộc với outbox</b>: giá trị này phải <b>≥ retention của bảng {@code outbox}</b>.
     * Marker dedupe là thứ chặn việc xử lý lại cùng một event; nếu nó hết hạn **trước** khi row
     * outbox bị xoá thì một lần replay DLQ (hoặc redeliver muộn) sau đó sẽ được xử lý **lần hai** —
     * email gửi 2 lần. Đây là bất biến cấu hình, không phải bất biến code: job không thể kiểm hộ
     * service khác.
     */
    private Duration retention = Duration.ofDays(7);

    /** Số row mỗi nhịp job dọn — nhỏ để transaction DELETE ngắn, không khoá bảng lâu. */
    private int cleanupBatchSize = 1000;

    public boolean isAutoCreateSchema() {
      return autoCreateSchema;
    }

    public void setAutoCreateSchema(boolean autoCreateSchema) {
      this.autoCreateSchema = autoCreateSchema;
    }

    public Duration getRetention() {
      return retention;
    }

    public void setRetention(Duration retention) {
      this.retention = retention;
    }

    public int getCleanupBatchSize() {
      return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize) {
      this.cleanupBatchSize = cleanupBatchSize;
    }
  }
}
