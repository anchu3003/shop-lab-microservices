package com.shoplab.promotion.lock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kết quả giữ chỗ voucher — đủ dữ liệu để order-service dựng order (discount + tổng phải trả) mà
 * không phải gọi lại.
 *
 * @param finalTotal {@code subtotal - discount} — tính sẵn ở đây để hai service không tự làm tròn
 *     khác nhau
 */
public record VoucherLockResponse(
    UUID lockId,
    UUID orderId,
    String voucherCode,
    VoucherLockStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal finalTotal,
    Instant expiresAt) {

  public static VoucherLockResponse from(VoucherLockRecord lock, String voucherCode) {
    return new VoucherLockResponse(
        lock.id(),
        lock.orderId(),
        voucherCode,
        lock.status(),
        lock.subtotal(),
        lock.discount(),
        lock.subtotal().subtract(lock.discount()),
        lock.expiresAt());
  }
}
