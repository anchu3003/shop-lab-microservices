package com.shoplab.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi chuẩn của ShopLab — mỗi service trả cùng một bộ {@code code} để client không phải xử lý
 * riêng từng service (AGENTS.md §6).
 *
 * <p>Đây là <b>error code hạ tầng dùng chung</b>, không phải domain error của từng service —
 * service tự định nghĩa thêm mã riêng khi cần (Phase 1+).
 */
public enum ErrorCode {

  /** Input sai/thiếu field. */
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),

  /**
   * Lỗi phía client không map được vào mã cụ thể (405, 415...). HTTP status vẫn được giữ nguyên.
   */
  REQUEST_ERROR(HttpStatus.BAD_REQUEST, "Request không hợp lệ"),

  /** Không tìm thấy tài nguyên. */
  NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),

  /** Xung đột trạng thái (tạo trùng, version lệch...). */
  CONFLICT(HttpStatus.CONFLICT, "Xung đột dữ liệu"),

  /** Lỗi không lường trước — KHÔNG trả chi tiết nội bộ ra ngoài. */
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

  private final HttpStatus httpStatus;
  private final String defaultMessage;

  ErrorCode(HttpStatus httpStatus, String defaultMessage) {
    this.httpStatus = httpStatus;
    this.defaultMessage = defaultMessage;
  }

  /** HTTP status tương ứng để {@code GlobalExceptionHandler} map sang response. */
  public HttpStatus httpStatus() {
    return httpStatus;
  }

  /** Message mặc định khi exception không cung cấp message riêng. */
  public String defaultMessage() {
    return defaultMessage;
  }

  /**
   * Suy ra mã lỗi từ HTTP status của lỗi do Spring MVC sinh ra (404 route sai, 405 method sai...).
   * Không match được thì trả {@link #REQUEST_ERROR} cho 4xx và {@link #INTERNAL_ERROR} cho phần còn
   * lại.
   */
  public static ErrorCode fromHttpStatus(int status) {
    if (status == HttpStatus.BAD_REQUEST.value()) {
      return VALIDATION_ERROR;
    }
    if (status == HttpStatus.NOT_FOUND.value()) {
      return NOT_FOUND;
    }
    if (status == HttpStatus.CONFLICT.value()) {
      return CONFLICT;
    }
    if (status >= 400 && status < 500) {
      return REQUEST_ERROR;
    }
    return INTERNAL_ERROR;
  }
}
