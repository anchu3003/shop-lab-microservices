package com.shoplab.notification.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tham số của notification-service (SL-3.4) — đọc từ env (12-factor).
 *
 * @param env môi trường (`local`/`dev`/`prod`) — quyết định `fail-mode` có hiệu lực hay không
 * @param failMode bơm lỗi để test retry/DLQ; **chỉ có tác dụng khi {@code env=local}**
 *     (fail-closed)
 * @param retryDelays backoff của consumer (ngữ nghĩa ADR-0007 D5: 200 ms/1 s/3 s)
 * @param listLimit số dòng tối đa trả về ở `GET /v1/notifications/me`
 * @param logRetention giữ {@code notification_logs} bao lâu trước khi job retention xoá (`TD-30`,
 *     `SL-4.0`). Mặc định **90 ngày**: đây là dữ liệu **nghiệp vụ** (khách còn tra "email tôi nhận
 *     hôm đó"), khác với outbox/processed_events chỉ là dữ liệu hạ tầng.
 * @param cleanupBatchSize số row mỗi nhịp job retention
 */
@ConfigurationProperties(prefix = "shoplab.notification")
public record NotificationProperties(
    @DefaultValue("local") String env,
    @DefaultValue("false") boolean failMode,
    @DefaultValue({"200ms", "1s", "3s"}) List<Duration> retryDelays,
    @DefaultValue("50") int listLimit,
    @DefaultValue("90d") Duration logRetention,
    @DefaultValue("1000") int cleanupBatchSize) {

  public NotificationProperties {
    if (listLimit <= 0 || listLimit > 200) {
      throw new IllegalArgumentException("shoplab.notification.list-limit phải trong (0, 200]");
    }
    if (logRetention == null || logRetention.isZero() || logRetention.isNegative()) {
      throw new IllegalArgumentException("shoplab.notification.log-retention phải > 0");
    }
    if (cleanupBatchSize <= 0) {
      throw new IllegalArgumentException("shoplab.notification.cleanup-batch-size phải > 0");
    }
    retryDelays = retryDelays == null ? List.of() : List.copyOf(retryDelays);
  }
}
