package com.shoplab.promotion.voucher;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/** Mã voucher đã tồn tại → HTTP 409 (không để DB ném lỗi unique rồi thành 500). */
public class DuplicateVoucherException extends AppException {

  public DuplicateVoucherException(String code) {
    super(ErrorCode.CONFLICT, "Mã voucher đã tồn tại: " + code);
  }
}
