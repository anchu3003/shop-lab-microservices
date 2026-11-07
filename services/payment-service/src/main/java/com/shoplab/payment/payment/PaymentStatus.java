package com.shoplab.payment.payment;

/**
 * Trạng thái của một payment intent.
 *
 * <p>{@code REQUIRES_PAYMENT → PROCESSING → SUCCEEDED | FAILED}; {@code CANCELLED} dành cho nhánh
 * compensating (saga `SL-2.6`, chưa mở endpoint ở ticket này). {@code FAILED} và {@code CANCELLED}
 * là trạng thái cuối của intent — khách muốn thử lại thì tạo intent mới (unique index {@code
 * ux_payments_live_order} chỉ chặn intent còn sống).
 */
public enum PaymentStatus {
  REQUIRES_PAYMENT,
  PROCESSING,
  SUCCEEDED,
  FAILED,
  CANCELLED;

  /** Đã ở trạng thái cuối ⇒ webhook lần sau chỉ là no-op (không đổi dữ liệu). */
  public boolean isFinal() {
    return this == SUCCEEDED || this == FAILED || this == CANCELLED;
  }
}
