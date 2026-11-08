package com.shoplab.order.saga;

/**
 * Trạng thái của một lần chạy checkout saga (`SL-2.6a`).
 *
 * <p><b>Vì sao là enum chuỗi chứ không phải số</b>: đọc DB bằng mắt được và thêm giá trị mới không
 * làm lệch dữ liệu cũ (cùng lý do đã dùng cho `products.status` ở `SL-2.11`).
 *
 * <p><b>Vì sao tách {@code FAILED_STOCK} và {@code FAILED_PAYMENT}</b> thay vì một {@code FAILED}
 * chung: hai nhánh này bù khác nhau (trước reserve chỉ có voucher; sau reserve phải trả cả hàng) và
 * khi điều tra sự cố thì "hỏng ở đâu" là câu hỏi đầu tiên. Một mã chung sẽ bắt dev đọc log mới
 * biết.
 */
public enum CheckoutSagaState {

  /**
   * Đã ghi nhận, đang chạy các bước (đây là trạng thái bền duy nhất khi process chết giữa chừng).
   */
  STARTED,

  /** Xong bước start: order `PENDING_PAYMENT` + (nếu cần) payment intent đã tạo. */
  COMPLETED_START,

  /** Hết hàng / SKU không mua được — voucher đã lock được trả lại. */
  FAILED_STOCK,

  /** Tạo order lỗi tại chỗ (DB) — stock + voucher đã trả lại, không có order. */
  FAILED_ORDER,

  /** Không tạo được payment intent — stock + voucher trả lại, order `CANCELLED`. */
  FAILED_PAYMENT
}
