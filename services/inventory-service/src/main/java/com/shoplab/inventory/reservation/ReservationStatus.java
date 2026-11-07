package com.shoplab.inventory.reservation;

/**
 * Vòng đời của một reservation (giữ chỗ).
 *
 * <pre>
 *   PENDING ──commit──▶ COMMITTED      (tiền về: hàng rời kho)
 *      │
 *      ├──release──▶ RELEASED          (huỷ đơn: trả chỗ)
 *      └──hết TTL──▶ EXPIRED           (bỏ ngang / thanh toán fail: trả chỗ theo đường hết hạn)
 * </pre>
 *
 * <p>Ba trạng thái cuối là <b>trạng thái cuối</b> — không có đường quay lại PENDING. Muốn giữ chỗ
 * lại thì tạo reservation mới (order khác hoặc orderId khác), vì "hồi sinh" một reservation đã
 * release sẽ làm mất dấu vết trong ledger.
 */
public enum ReservationStatus {
  PENDING,
  COMMITTED,
  RELEASED,
  EXPIRED;

  public boolean isFinal() {
    return this != PENDING;
  }
}
