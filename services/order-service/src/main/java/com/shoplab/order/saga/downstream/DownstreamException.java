package com.shoplab.order.saga.downstream;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Service đích trả lỗi hoặc không tới được — mang theo <b>status gốc</b> để saga quyết định:
 *
 * <ul>
 *   <li>{@code 4xx}: lỗi do dữ liệu/trạng thái (hết hàng, voucher sai, SKU archived) ⇒ trả nguyên
 *       status cho client, kèm message của service đích (nó hiểu ngữ cảnh hơn).
 *   <li>{@code 5xx} / timeout / connection refused: lỗi hạ tầng ⇒ <b>503</b> cho client (thử lại
 *       được), không lộ chi tiết nội bộ.
 * </ul>
 *
 * <p>Kế thừa {@link AppException} để {@code GlobalExceptionHandler} của commons trả đúng {@code
 * ApiError} — không cần handler riêng ở service này.
 */
public class DownstreamException extends AppException {

  private final String service;
  private final Integer httpStatus;

  /** Lỗi nghiệp vụ từ service đích (4xx) hoặc nó trả 5xx (⇒ ta coi là hạ tầng, trả 503). */
  public DownstreamException(String service, int httpStatus, String message) {
    super(errorCodeFor(httpStatus), "[" + service + "] " + message);
    this.service = service;
    this.httpStatus = httpStatus;
  }

  /**
   * 4xx giữ nguyên ngữ nghĩa (client sửa được); 5xx của service đích là lỗi <b>của hệ</b>, không
   * phải của request này ⇒ {@link ErrorCode#UNAVAILABLE} (503, thử lại có ích). Trả 500 cho client
   * trong trường hợp đó là nói dối về nguyên nhân và làm client không retry.
   */
  private static ErrorCode errorCodeFor(int httpStatus) {
    if (httpStatus >= 500) {
      return ErrorCode.UNAVAILABLE;
    }
    return ErrorCode.fromHttpStatus(httpStatus);
  }

  /** Lỗi hạ tầng: timeout, connection refused, response rỗng ⇒ 503. */
  public DownstreamException(String service, String message, Throwable cause) {
    super(ErrorCode.UNAVAILABLE, "[" + service + "] " + message, cause);
    this.service = service;
    this.httpStatus = null;
  }

  public String service() {
    return service;
  }

  /** Status HTTP gốc; {@code null} nghĩa là không có response (timeout/chết). */
  public Integer httpStatus() {
    return httpStatus;
  }

  /** Mã lỗi ngắn để ghi vào {@code checkout_sagas.failure_code} / log. */
  public String failureCode() {
    if (httpStatus == null) {
      return "DOWNSTREAM_UNAVAILABLE";
    }
    return httpStatus >= 500 ? "DOWNSTREAM_ERROR" : "DOWNSTREAM_REJECTED";
  }
}
