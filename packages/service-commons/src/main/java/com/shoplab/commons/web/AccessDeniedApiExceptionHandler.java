package com.shoplab.commons.web;

import com.shoplab.commons.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
 *   <li>{@link GlobalExceptionHandler} bắt {@code Exception} cuối cùng ⇒ nếu không có handler này
 *       thì buyer nhận <b>500 INTERNAL_ERROR</b> thay vì <b>403 FORBIDDEN</b>. Client sẽ tưởng
 *       server hỏng, còn log thì đầy stack trace "unhandled exception" cho một hành vi hoàn toàn
 *       bình thường.
 *   <li>Service nào chặn <i>trước</i> filter chain (như {@code /v1/admin/**}) thì 403 do {@code
 *       accessDeniedHandler} của Security trả, không đi qua đây. Nhánh này chỉ dành cho phân quyền
 *       theo <b>method</b>.
 * </ul>
 *
 * <p><b>Vì sao ở {@code service-commons}</b> (SL-2.12, trả {@code TD-3}): đây là hành vi của
 * <i>toàn hệ</i> — mã lỗi 403 nằm trong contract {@code ApiError}, không phải chuyện riêng của một
 * service. Trước SL-2.12 class này bị copy <b>6 lần</b> (catalog, inventory, cart, order, payment,
 * promotion) với nội dung giống nhau từng byte. Bản duy nhất nằm ở đây thì service mới chỉ cần khai
 * dependency là có 403 đúng chuẩn.
 *
 * <p><b>Chỉ active khi có spring-security trên classpath</b> — xem {@link
 * AccessDeniedAutoConfiguration}: service không dùng security (vd {@code service-template}) không
 * bị {@code NoClassDefFoundError}.
 *
 * <p><b>Vì sao có {@code @Order(HIGHEST_PRECEDENCE)}</b> (bẫy thật gặp ở SL-2.12): {@code
 * ExceptionHandlerExceptionResolver} lấy advice <b>đầu tiên</b> khớp trong cache, và một advice
 * khớp cả <i>tổ tiên</i> của exception. {@link GlobalExceptionHandler} có
 * {@code @ExceptionHandler(Exception.class)} — khớp mọi thứ, kể cả {@code
 * AuthorizationDeniedException} (con của {@link AccessDeniedException}). Khi handler còn là
 * {@code @Component} trong từng service, thứ tự đăng ký (component scan trước auto-config) làm nó
 * thắng; chuyển sang auto-config thì thứ tự <b>đảo lại</b> ⇒ 403 biến thành <b>500</b>.
 * {@code @Order} nói tường minh "advice cụ thể xét trước catch-all", không phụ thuộc thứ tự đăng ký
 * bean.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
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
