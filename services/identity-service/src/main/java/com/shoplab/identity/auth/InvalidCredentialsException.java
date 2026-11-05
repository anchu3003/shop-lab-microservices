package com.shoplab.identity.auth;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Sai email hoặc mật khẩu khi đăng nhập.
 *
 * <p>Message cố ý <b>giống nhau</b> cho hai trường hợp "email không tồn tại" và "sai mật khẩu": nếu
 * khác nhau thì kẻ tấn công dò được email nào đã đăng ký (user enumeration).
 */
public class InvalidCredentialsException extends AppException {

  public InvalidCredentialsException() {
    super(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
  }
}
