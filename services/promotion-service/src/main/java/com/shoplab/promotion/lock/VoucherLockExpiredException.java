package com.shoplab.promotion.lock;

import com.shoplab.commons.error.AppException;

/**
 * Lượt voucher đã bị giữ quá TTL → HTTP <b>410 Gone</b> (cùng ngữ nghĩa với reservation hết hạn của
 * inventory: "không còn hiệu lực, đừng retry cùng thứ này").
 */
public class VoucherLockExpiredException extends AppException {

  public VoucherLockExpiredException(String message) {
    super(com.shoplab.commons.error.ErrorCode.GONE, message);
  }
}
