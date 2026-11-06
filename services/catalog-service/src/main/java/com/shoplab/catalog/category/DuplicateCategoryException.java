package com.shoplab.catalog.category;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Category đã tồn tại (trùng slug) → HTTP 409.
 *
 * <p>409 chứ không 400: request đúng cú pháp, chỉ xung đột với trạng thái hiện có của hệ thống —
 * client xử lý hai loại này khác nhau.
 */
public class DuplicateCategoryException extends AppException {

  public DuplicateCategoryException(String slug) {
    super(ErrorCode.CONFLICT, "Category đã tồn tại với slug '" + slug + "'");
  }
}
