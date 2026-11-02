package com.shoplab.commons.web;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Biến mọi exception thành {@link ApiError} thống nhất. Không có class này thì mỗi service tự viết
 * một kiểu response lỗi, client phải xử lý riêng từng service.
 *
 * <p>Nguyên tắc: lỗi do client gây ra thì giữ HTTP status gốc và trả message rõ ràng; lỗi không
 * lường trước thì log full stack nhưng <b>không</b> trả chi tiết nội bộ ra ngoài (AGENTS.md §9).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Exception nghiệp vụ có chủ đích → map thẳng theo {@link ErrorCode}. */
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiError> handleAppException(AppException ex, HttpServletRequest request) {
    LOG.warn("app exception [{}]: {}", ex.errorCode(), ex.getMessage());
    return ResponseEntity.status(ex.errorCode().httpStatus())
        .body(ApiError.of(ex.errorCode(), ex.getMessage(), request.getRequestURI()));
  }

  /** Lỗi validation của Bean Validation → trả chi tiết từng field để client sửa được. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<ApiError.FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new ApiError.FieldViolation(
                        fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();
    LOG.warn("validation failed: {} field(s)", violations.size());

    return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
        .body(ApiError.of(ErrorCode.VALIDATION_ERROR, null, request.getRequestURI(), violations));
  }

  /**
   * Lưới cuối. Phải xử lý riêng nhánh {@link ErrorResponse} (404 route sai, 405 method sai...) vì
   * nếu không thì lỗi 404 của Spring sẽ bị biến thành 500 — sai hoàn toàn về ngữ nghĩa.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    if (ex instanceof ErrorResponse errorResponse
        && errorResponse.getStatusCode().is4xxClientError()) {
      int status = errorResponse.getStatusCode().value();
      LOG.warn("client error {}: {}", status, ex.getMessage());
      return ResponseEntity.status(status)
          .body(
              ApiError.of(
                  ErrorCode.fromHttpStatus(status), ex.getMessage(), request.getRequestURI()));
    }

    LOG.error("unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
        .body(ApiError.of(ErrorCode.INTERNAL_ERROR, null, request.getRequestURI()));
  }
}
