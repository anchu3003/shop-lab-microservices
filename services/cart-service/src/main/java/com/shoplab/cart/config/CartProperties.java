package com.shoplab.cart.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số nghiệp vụ của cart.
 *
 * @param ttl TTL của giỏ (và của guest token). Làm mới mỗi lần ghi ⇒ giỏ đang dùng không hết hạn
 *     giữa phiên mua; giỏ bỏ quên tự biến mất sau 30 ngày (architecture §8.3)
 * @param maxLines số dòng tối đa trong một giỏ — endpoint public nên phải có cận trên
 * @param maxQty số lượng tối đa cho một dòng (bất biến được kiểm <b>trong Lua</b>, xem {@code
 *     RedisCartStore})
 * @param keyPrefix tiền tố key Redis, tách namespace với cache của service khác
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.cart")
public record CartProperties(
    @DefaultValue("30d") Duration ttl,
    @DefaultValue("50") @Min(1) @Max(1000) int maxLines,
    @DefaultValue("999") @Min(1) @Max(100000) int maxQty,
    @DefaultValue("cart") @NotBlank String keyPrefix) {

  public CartProperties {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("shoplab.cart.ttl phải > 0");
    }
  }

  /** TTL tính bằng giây — Redis `EXPIRE` nhận số nguyên giây. */
  public long ttlSeconds() {
    return ttl.toSeconds();
  }
}
