package com.shoplab.commons.error;

/**
 * Base cho mọi exception nghiệp vụ của ShopLab — mang theo {@link ErrorCode} để {@code
 * GlobalExceptionHandler} map sang HTTP status + body lỗi thống nhất.
 *
 * <p>Vì sao cần base class: nếu mỗi service tự throw exception riêng thì handler dùng chung không
 * bắt được, và mỗi service lại phải viết một error handler — đúng thứ mà shared lib này muốn tránh.
 */
public class AppException extends RuntimeException {

  private final ErrorCode errorCode;

  public AppException(ErrorCode errorCode) {
    this(errorCode, errorCode.defaultMessage(), null);
  }

  public AppException(ErrorCode errorCode, String message) {
    this(errorCode, message, null);
  }

  public AppException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
