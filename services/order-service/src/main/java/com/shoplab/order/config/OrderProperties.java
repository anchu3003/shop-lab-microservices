package com.shoplab.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số nghiệp vụ của order-service.
 *
 * @param idempotencyRetention giữ row {@code idempotency_keys} đã COMPLETED bao lâu. ADR-0006 D2
 *     chốt 24 h — đủ để client retry muộn (mất mạng, treo máy) vẫn được dedupe.
 * @param inProgressStaleAfter row {@code IN_PROGRESS} cũ hơn mức này bị coi là mồ côi (process chết
 *     giữa chừng) và bị job dọn xoá — nếu không, client sẽ nhận 409 vĩnh viễn cho key đó.
 * @param sweepBatchSize số row mỗi nhịp job dọn
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.order")
public record OrderProperties(
    @DefaultValue("24h") Duration idempotencyRetention,
    @DefaultValue("1h") Duration inProgressStaleAfter,
    @DefaultValue("200") @Min(1) @Max(2000) int sweepBatchSize) {

  public OrderProperties {
    if (idempotencyRetention == null
        || idempotencyRetention.isNegative()
        || idempotencyRetention.isZero()) {
      throw new IllegalArgumentException("shoplab.order.idempotency-retention phải > 0");
    }
    if (inProgressStaleAfter == null
        || inProgressStaleAfter.isNegative()
        || inProgressStaleAfter.isZero()) {
      throw new IllegalArgumentException("shoplab.order.in-progress-stale-after phải > 0");
    }
  }
}
