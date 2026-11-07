package com.shoplab.promotion.voucher;

import java.util.Locale;

/**
 * Chuẩn hoá mã voucher — <b>một chỗ duy nhất</b> cho cả đường ghi (tạo voucher) và đường đọc
 * (lock).
 *
 * <p>Vì sao cần: unique index ở DB chỉ có tác dụng nếu service chuẩn hoá <b>trước khi</b> ghi/so.
 * Không chuẩn hoá thì {@code sale10} và {@code SALE10} là hai voucher khác nhau trong DB nhưng
 * client lại coi là một — loại bug dữ liệu không thể sửa về sau.
 */
public final class VoucherCode {

  /** Trim + UPPER: {@code " sale10 "} và {@code "SALE10"} là cùng một voucher. */
  public static String normalize(String rawCode) {
    return rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
  }

  private VoucherCode() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
