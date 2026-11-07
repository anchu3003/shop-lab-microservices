package com.shoplab.promotion.voucher;

/**
 * Cách một voucher giảm giá (Phase 2 chỉ có 2 loại — chốt với user ở `SL-2.4`).
 *
 * <ul>
 *   <li>{@link #PERCENT}: giảm {@code value}% của subtotal, có thể bị chặn trần bởi {@code
 *       maxDiscount}.
 *   <li>{@link #FIXED}: giảm đúng {@code value} (số tiền), không phụ thuộc subtotal.
 * </ul>
 *
 * <p>Không có {@code FREESHIP} ở Phase 2: phí ship chưa tồn tại trong luồng checkout ⇒ thêm loại đó
 * lúc này là dạy một khái niệm chưa có đất dùng.
 */
public enum VoucherType {
  PERCENT,
  FIXED
}
