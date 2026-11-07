package com.shoplab.payment.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Yêu cầu tạo payment intent (saga `SL-2.6` là caller).
 *
 * <p><b>Số tiền do caller gửi</b> chứ không phải payment tự tính: payment không đọc DB của order
 * (database per service). Bù lại, webhook <b>bắt buộc</b> khớp số tiền với intent (lệch ⇒ 409) —
 * nghĩa là số tiền không thể bị đổi ở giữa đường.
 *
 * @param orderId đơn cần thanh toán; cùng một đơn chỉ có tối đa một intent còn sống
 * @param amount số tiền phải thu, &gt; 0
 * @param currency mã tiền 3 ký tự; bỏ trống thì mặc định {@code VND}
 */
public record CreatePaymentIntentRequest(
    @NotNull UUID orderId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @Pattern(regexp = "[A-Za-z]{3}", message = "currency phải là 3 ký tự") String currency) {

  /** DB dùng {@code CHAR(3)} ⇒ chuẩn hoá hoa ngay từ đầu, không để 'vnd' và 'VND' thành 2 loại. */
  public String currencyOrDefault() {
    return currency == null || currency.isBlank() ? "VND" : currency.toUpperCase(Locale.ROOT);
  }
}
