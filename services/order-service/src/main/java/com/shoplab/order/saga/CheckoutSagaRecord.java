package com.shoplab.order.saga;

import java.time.Instant;
import java.util.UUID;

/**
 * State bền của một lần checkout (`checkout_sagas`).
 *
 * @param paymentRequired {@code false} khi đơn 0 đồng (voucher 100%) ⇒ cố ý <b>không</b> tạo
 *     payment intent; `SL-2.6b` đọc cờ này để chốt đơn mà không cần verify payment
 * @param lockId voucher lock đã giữ (null nếu không dùng voucher) — cần để **release** khi bù
 * @param reservationId chỗ hàng đã giữ — cần để **release** khi bù
 * @param failureCode mã lỗi ngắn (`DOWNSTREAM_UNAVAILABLE`, `STOCK_CONFLICT`, …) để tra cứu nhanh
 */
public record CheckoutSagaRecord(
    UUID orderId,
    UUID userId,
    CheckoutSagaState state,
    boolean paymentRequired,
    UUID lockId,
    UUID reservationId,
    UUID paymentIntentId,
    String paymentUrl,
    String failureCode,
    Instant createdAt,
    Instant updatedAt) {

  /** Bản ghi lúc mở saga: chưa giữ gì, state `STARTED`. */
  public static CheckoutSagaRecord started(
      UUID orderId, UUID userId, boolean paymentRequired, Instant now) {
    return new CheckoutSagaRecord(
        orderId,
        userId,
        CheckoutSagaState.STARTED,
        paymentRequired,
        null,
        null,
        null,
        null,
        null,
        now,
        now);
  }
}
