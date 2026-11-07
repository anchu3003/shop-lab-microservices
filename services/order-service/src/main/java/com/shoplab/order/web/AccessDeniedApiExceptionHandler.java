package com.shoplab.order.web;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Map lỗi <b>method security</b> ({@code @PreAuthorize}) thành 403 — không để nó thành 500.
 *
 * <p><b>Vì sao cần class này</b> (phát hiện ở SL-1.5, IT bắt được):
 *
 * <ul>
 *   <li>{@code @PreAuthorize} ném {@link AccessDeniedException} <i>bên trong</i> lời gọi
 *       controller, tức là sau {@code DispatcherServlet} — không phải chỗ {@code
 *       ExceptionTranslationFilter} của Spring Security xử lý.
 *   <li>{@code GlobalExceptionHandler} của commons bắt {@code Exception} cuối cùng ⇒ nếu không có
 *       handler ở đây thì buyer nhận <b>500 INTERNAL_ERROR</b> thay vì <b>403 FORBIDDEN</b>. Client
 *       sẽ tưởng server hỏng, còn log thì đầy stack trace "unhandled exception" cho một hành vi
 *       hoàn toàn bình thường.
 *   <li>identity-service không lộ lỗi này vì {@code /v1/admin/**} được chặn <i>trước</i> ở filter
 *       chain (403 do {@code accessDeniedHandler}). Catalog có phân quyền theo method nên đây là
 *       chốt duy nhất.
 * </ul>
 *
 * <p>Đây là ứng viên để chuyển vào {@code service-commons} khi service thứ tư cần (tránh copy lần
 * thứ ba) — ghi ở {@code Backlog.md}.
 */
@RestControllerAdvice
public class AccessDeniedApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AccessDeniedApiExceptionHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    // warn chứ không error: từ chối quyền là hành vi đúng của hệ thống, không phải sự cố.
    LOG.warn(
        "từ chối quyền {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(ErrorCode.FORBIDDEN.httpStatus())
        .body(ApiError.of(ErrorCode.FORBIDDEN, null, request.getRequestURI()));
  }
}
