package com.shoplab.promotion.lock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một lượt voucher đang/đã bị giữ cho một order.
 *
 * @param subtotal snapshot subtotal lúc lock (để audit vì sao discount ra con số đó)
 * @param discount snapshot số tiền giảm lúc lock — commit dùng đúng con số này, không tính lại (nếu
 *     admin sửa {@code value} giữa chừng, hoá đơn của user vẫn đúng như đã hứa)
 */
public record VoucherLockRecord(
    UUID id,
    UUID voucherId,
    UUID orderId,
    VoucherLockStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    Instant expiresAt,
    Instant createdAt) {

  public VoucherLockRecord withStatus(VoucherLockStatus newStatus) {
    return new VoucherLockRecord(
        id, voucherId, orderId, newStatus, subtotal, discount, expiresAt, createdAt);
  }
}
