package com.shoplab.payment.idempotency;

/**
 * Trạng thái của một khoá idempotency (ADR-0006 D2).
 *
 * <p>{@code IN_PROGRESS} được ghi <b>trước</b> khi làm việc: nếu process chết giữa chừng, client
 * retry sẽ nhận 409 chứ không tạo đơn thứ hai — thà "kẹt 1 key" (được job dọn giải phóng sau 1 h)
 * còn hơn tạo 2 order.
 */
public enum IdempotencyStatus {
  IN_PROGRESS,
  COMPLETED
}
