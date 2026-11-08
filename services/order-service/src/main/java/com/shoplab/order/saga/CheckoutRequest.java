package com.shoplab.order.saga;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của {@code POST /v1/checkout}.
 *
 * <p><b>Vì sao KHÔNG có danh sách dòng hàng</b>: dòng hàng lấy từ giỏ của user ở `cart-service`
 * (phương án B đã chốt). Nếu client gửi được danh sách thì (a) hai nguồn sự thật cho "đơn này gồm
 * gì", (b) client tự bỏ bớt dòng để trả ít tiền hơn — đúng loại sửa-một-field mà `SL-2.5` đã chặn.
 *
 * @param voucherCode mã voucher muốn dùng; {@code null}/rỗng = không dùng voucher
 */
public record CheckoutRequest(
    @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]*", message = "chỉ chữ, số, _ và -")
        String voucherCode) {

  public CheckoutRequest {
    voucherCode = voucherCode == null || voucherCode.isBlank() ? null : voucherCode.trim();
  }
}
