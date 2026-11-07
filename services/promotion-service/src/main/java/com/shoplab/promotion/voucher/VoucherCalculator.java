package com.shoplab.promotion.voucher;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tính số tiền giảm giá của voucher — <b>hàm thuần</b>, không DB, không Spring ⇒ test được mọi
 * biên.
 *
 * <p>Quy ước (chốt ở `SL-2.4`):
 *
 * <ul>
 *   <li>Làm tròn {@link RoundingMode#HALF_UP} tới 2 chữ số — tiền tệ, không dùng float/double
 *       (AGENTS.md §6).
 *   <li>Giảm giá <b>không bao giờ vượt</b> subtotal: voucher 100k cho đơn 30k ⇒ giảm 30k, tổng về 0
 *       chứ không âm.
 *   <li>PERCENT bị chặn trần bởi {@code maxDiscount} nếu có (cách phổ biến để "giảm 50% tối đa
 *       100k").
 * </ul>
 */
public final class VoucherCalculator {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int MONEY_SCALE = 2;

  /**
   * Tính giảm giá cho một subtotal đã thoả điều kiện (caller đã kiểm cửa sổ thời gian + {@code
   * minOrder}).
   *
   * @throws IllegalArgumentException khi subtotal âm (lỗi lập trình, không phải lỗi người dùng)
   */
  public static BigDecimal discountFor(VoucherRecord voucher, BigDecimal subtotal) {
    if (subtotal == null || subtotal.signum() < 0) {
      throw new IllegalArgumentException("subtotal phải >= 0, nhận: " + subtotal);
    }
    BigDecimal raw =
        switch (voucher.type()) {
          case PERCENT ->
              subtotal.multiply(voucher.value()).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
          case FIXED -> voucher.value();
        };
    if (voucher.maxDiscount() != null) {
      raw = raw.min(voucher.maxDiscount());
    }
    // Không giảm quá subtotal: tổng đơn không được âm.
    return raw.min(subtotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private VoucherCalculator() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
