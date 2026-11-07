package com.shoplab.promotion.lock;

/**
 * Vòng đời của một lượt voucher bị giữ (giống {@code ReservationStatus} của inventory — cùng
 * pattern thì cùng tên gọi).
 *
 * <pre>
 *   LOCKED ──commit──▶ COMMITTED     (đơn thanh toán xong: dùng thật)
 *      │
 *      ├──release──▶ RELEASED        (huỷ đơn: trả lượt)
 *      └──hết TTL──▶ EXPIRED         (bỏ ngang: sweeper trả lượt)
 * </pre>
 */
public enum VoucherLockStatus {
  LOCKED,
  COMMITTED,
  RELEASED,
  EXPIRED
}
