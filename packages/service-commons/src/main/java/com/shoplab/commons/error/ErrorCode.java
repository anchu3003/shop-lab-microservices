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

  /**
   * Chưa xác thực: thiếu token, token hết hạn/sai chữ ký, hoặc sai thông tin đăng nhập.
   *
   * <p>Phân biệt với {@link #FORBIDDEN}: 401 = "chưa biết bạn là ai", 403 = "biết bạn là ai nhưng
   * không đủ quyền". Sai quy ước này là nguồn bug khó tìm ở phía client.
   */
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa xác thực"),

  /** Đã xác thực nhưng không đủ quyền với tài nguyên này. */
  FORBIDDEN(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),

  /** Không tìm thấy tài nguyên. */
  NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),

  /**
   * Tài nguyên <b>đã từng</b> tồn tại nhưng không còn hiệu lực và sẽ không quay lại (vd reservation
   * đã hết hạn TTL) ⇒ client <b>không nên</b> retry cùng thứ đó, phải tạo cái mới.
   *
   * <p>Phân biệt với {@link #CONFLICT}: 409 = "trạng thái đang xung đột, thử lại có ích"; 410 = "đã
   * chết".
   */
  GONE(HttpStatus.GONE, "Tài nguyên không còn hiệu lực"),

  /**
   * Dependency không tới được: service đích chết/timeout, hoặc không lấy được dữ liệu cần để phục
   * vụ request (vd JWKS).
   *
   * <p>Phân biệt với {@link #INTERNAL_ERROR}: 503 = "lỗi tạm thời, thử lại được" — client/load
   * balancer nên retry. Dùng 500 cho lỗi lập trình của chính service.
   */
  UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tạm thời không sẵn sàng"),

  /** Xung đột trạng thái (tạo trùng, version lệch...). */
  CONFLICT(HttpStatus.CONFLICT, "Xung đột dữ liệu"),

  /**
   * Dữ liệu đúng cú pháp nhưng **không xử lý được** về mặt ngữ nghĩa — vd dùng lại
   * `Idempotency-Key` cũ cho một payload khác (SL-2.5). Khác 409: 409 là "xung đột tạm thời, thử
   * lại có ích"; 422 là "client phải sửa request".
   */
  UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "Dữ liệu không xử lý được"),

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
    if (status == HttpStatus.UNAUTHORIZED.value()) {
      return UNAUTHORIZED;
    }
    if (status == HttpStatus.FORBIDDEN.value()) {
      return FORBIDDEN;
    }
    if (status == HttpStatus.NOT_FOUND.value()) {
      return NOT_FOUND;
    }
    if (status == HttpStatus.CONFLICT.value()) {
      return CONFLICT;
    }
    if (status == HttpStatus.GONE.value()) {
      return GONE;
    }
    if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
      return UNPROCESSABLE_ENTITY;
    }
    // 502/503/504 đều là "phía sau có vấn đề, thử lại được" → cùng một mã cho client dễ xử lý.
    if (status == HttpStatus.BAD_GATEWAY.value()
        || status == HttpStatus.SERVICE_UNAVAILABLE.value()
        || status == HttpStatus.GATEWAY_TIMEOUT.value()) {
      return UNAVAILABLE;
    }
    if (status >= 400 && status < 500) {
      return REQUEST_ERROR;
    }
    return INTERNAL_ERROR;
  }
}
