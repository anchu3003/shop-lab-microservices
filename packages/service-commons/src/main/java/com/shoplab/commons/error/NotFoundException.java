package com.shoplab.commons.error;

/** Tài nguyên không tồn tại → HTTP 404, body {@code code = NOT_FOUND}. */
public class NotFoundException extends AppException {

  public NotFoundException(String message) {
    super(ErrorCode.NOT_FOUND, message);
  }
}
