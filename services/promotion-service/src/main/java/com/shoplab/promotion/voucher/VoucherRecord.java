package com.shoplab.promotion.voucher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cấu hình một voucher (map 1-1 với bảng {@code vouchers}).
 *
 * @param value PERCENT: phần trăm (0 < value <= 100); FIXED: số tiền giảm
 * @param maxDiscount trần giảm giá cho PERCENT; {@code null} = không chặn
 * @param minOrder subtotal tối thiểu để dùng được voucher
 * @param used số lượt đã dùng thật (commit)
 * @param locked số lượt đang bị giữ (lock chưa commit)
 */
public record VoucherRecord(
    UUID id,
    String code,
    VoucherType type,
    BigDecimal value,
    BigDecimal maxDiscount,
    BigDecimal minOrder,
    int quota,
    int used,
    int locked,
    Instant startAt,
    Instant endAt,
    Instant createdAt) {

  /** Số lượt còn có thể giữ — con số client/admin quan tâm ("còn 3 lượt"). */
  public int available() {
    return quota - used - locked;
  }
}
