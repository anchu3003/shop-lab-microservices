package com.shoplab.catalog.sku;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Mã SKU đã tồn tại → HTTP 409.
 *
 * <p>409 chứ không 400: request đúng cú pháp, chỉ xung đột với dữ liệu đã có.
 */
public class DuplicateSkuException extends AppException {

  public DuplicateSkuException(String code) {
    super(ErrorCode.CONFLICT, "SKU đã tồn tại với code '" + code + "'");
  }
}
