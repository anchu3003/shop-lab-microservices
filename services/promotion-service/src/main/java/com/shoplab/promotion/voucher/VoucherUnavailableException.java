package com.shoplab.promotion.voucher;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Không giữ được lượt voucher → HTTP <b>409</b>.
 *
 * <p>Vì sao 409 cho cả 4 lý do (chưa tới giờ / đã hết hạn / hết lượt / chưa đủ đơn tối thiểu): với
 * client, cả 4 đều là "trạng thái của voucher không cho dùng" chứ không phải lỗi cú pháp; khác nhau
 * nằm ở {@link Reason} để client hiển thị đúng thông báo (và để test khẳng định đúng nguyên nhân,
 * không chỉ khẳng định "có lỗi").
 */
public class VoucherUnavailableException extends AppException {

  /** Lý do cụ thể — trả kèm trong message để log/UI không phải đoán. */
  public enum Reason {
    NOT_STARTED("Voucher chưa tới thời gian sử dụng"),
    ENDED("Voucher đã hết thời gian sử dụng"),
    EXHAUSTED("Voucher đã hết lượt"),
    BELOW_MIN_ORDER("Đơn chưa đủ giá trị tối thiểu để dùng voucher");

    private final String message;

    Reason(String message) {
      this.message = message;
    }

    public String message() {
      return message;
    }
  }

  private final Reason reason;

  public VoucherUnavailableException(Reason reason, String detail) {
    super(ErrorCode.CONFLICT, reason.message() + (detail == null ? "" : ": " + detail));
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
