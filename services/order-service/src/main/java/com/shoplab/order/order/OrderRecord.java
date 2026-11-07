package com.shoplab.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Header của một đơn hàng.
 *
 * @param subtotal tổng tiền hàng trước giảm giá (tính từ snapshot, lưu luôn — không tính lại khi
 *     đọc)
 * @param discount số tiền voucher đã lock (0 nếu không dùng); do saga truyền vào khi có voucher
 * @param total {@code subtotal - discount}
 * @param idempotencyKey khoá client gửi — lưu để audit/tra cứu (dedupe thật ở bảng {@code
 *     idempotency_keys})
 */
public record OrderRecord(
    UUID id,
    UUID userId,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    String idempotencyKey,
    UUID paymentIntentId,
    Instant createdAt,
    Instant updatedAt) {

  public OrderRecord withStatus(OrderStatus newStatus, Instant now) {
    return new OrderRecord(
        id,
        userId,
        newStatus,
        subtotal,
        discount,
        total,
        currency,
        idempotencyKey,
        paymentIntentId,
        createdAt,
        now);
  }
}
