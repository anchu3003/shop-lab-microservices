package com.shoplab.order.order;

/**
 * Vòng đời của một đơn hàng (ADR-0006 D3).
 *
 * <pre>
 *   PENDING_PAYMENT ──confirm──▶ CONFIRMED   (tiền đã về: trừ kho thật, dùng voucher)
 *        │
 *        ├──cancel──▶ CANCELLED              (user huỷ / thanh toán fail)
 *        └──expire──▶ EXPIRED                (quá 15' không thanh toán)
 * </pre>
 *
 * <p>Ba trạng thái sau là <b>trạng thái cuối</b>: không có đường quay lại (muốn mua lại thì tạo đơn
 * mới). Lý do: đơn đã trừ kho/dùng voucher thì "hồi sinh" sẽ làm sai cả hai service kia.
 */
public enum OrderStatus {
  PENDING_PAYMENT,
  CONFIRMED,
  CANCELLED,
  EXPIRED;

  public boolean isFinal() {
    return this != PENDING_PAYMENT;
  }
}
