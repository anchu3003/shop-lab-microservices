package com.shoplab.promotion.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số nghiệp vụ của promotion-service — đọc từ env để đổi hành vi không phải build lại
 * (12-factor).
 *
 * @param lockTtl thời gian giữ chỗ 1 lượt voucher. Khớp TTL reservation của inventory (15 phút,
 *     ADR-0006 D1) vì checkout giữ cả hai trong cùng một phiên thanh toán — lệch nhau thì có lúc
 *     hàng bị giữ mà voucher đã hết hạn (hoặc ngược lại), compensating phải xử lý thêm một trường
 *     hợp không cần thiết.
 * @param sweepBatchSize số voucher lock tối đa sweeper xử lý mỗi nhịp
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.promotion")
public record PromotionProperties(
    @DefaultValue("15m") Duration lockTtl,
    @DefaultValue("100") @Min(1) @Max(1000) int sweepBatchSize) {

  public PromotionProperties {
    if (lockTtl == null || lockTtl.isNegative() || lockTtl.isZero()) {
      throw new IllegalArgumentException("shoplab.promotion.lock-ttl phải > 0");
    }
  }
}
