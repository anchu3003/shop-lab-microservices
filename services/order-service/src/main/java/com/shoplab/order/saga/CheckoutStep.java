package com.shoplab.order.saga;

/**
 * Các bước của saga start — dùng cho log + bảng audit {@code checkout_saga_steps}.
 *
 * <p>Thứ tự khai báo **chính là thứ tự chạy** (theo architecture.md §6.1): đọc giỏ → lấy giá → lock
 * voucher → reserve stock → tạo order → tạo payment intent. Test compensating dựa vào thứ tự này để
 * biết "fail ở bước này thì đã giữ được gì".
 */
public enum CheckoutStep {
  CART,
  PRICE,
  VOUCHER_LOCK,
  STOCK_RESERVE,
  ORDER_CREATE,
  PAYMENT_INTENT
}
