package com.shoplab.identity.auth.jwt;

/**
 * Không đọc được khoá JWT (thiếu file, sai định dạng, sai loại khoá).
 *
 * <p>Cố ý <b>fail-fast</b> ngay khi tạo bean: service phát token mà không có khoá thì hoặc là chết
 * lúc khởi động, hoặc là phát token không ai verify được — cái sau tệ hơn nhiều.
 */
public class JwtKeyException extends RuntimeException {

  public JwtKeyException(String message) {
    super(message);
  }

  public JwtKeyException(String message, Throwable cause) {
    super(message, cause);
  }
}
