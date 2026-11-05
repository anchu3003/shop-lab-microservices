package com.shoplab.identity.auth;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Email đã được đăng ký.
 *
 * <p>Dùng {@link ErrorCode#CONFLICT} (409) — mã hạ tầng dùng chung, không thêm mã domain vào shared
 * lib (shared lib chỉ chứa hạ tầng — AGENTS.md §6). Message nêu rõ email để client sửa được.
 */
public class EmailAlreadyExistsException extends AppException {

  public EmailAlreadyExistsException(String email) {
    super(ErrorCode.CONFLICT, "Email '" + email + "' đã được đăng ký");
  }
}
