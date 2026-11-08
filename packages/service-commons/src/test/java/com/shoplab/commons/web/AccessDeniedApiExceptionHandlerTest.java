package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.log.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * Hành vi của handler 403 dùng chung (SL-2.12). Test ở đây thay cho 6 bản copy trước đây — sửa một
 * chỗ là mọi service được kiểm cùng lúc.
 */
class AccessDeniedApiExceptionHandlerTest {

  private final AccessDeniedApiExceptionHandler handler = new AccessDeniedApiExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/products");

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("@PreAuthorize từ chối → 403 FORBIDDEN (không phải 500) + body ApiError có traceId")
  void shouldMapAccessDeniedToForbidden() {
    // traceId lấy từ MDC do CorrelationIdFilter đặt — unit test phải tự set để giống request thật.
    MDC.put(MdcKeys.TRACE_ID, "test-trace-403");

    ResponseEntity<ApiError> response =
        handler.handleAccessDenied(new AccessDeniedException("thiếu quyền"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    assertThat(response.getBody().path()).isEqualTo("/v1/products");
    assertThat(response.getBody().traceId()).isEqualTo("test-trace-403");
  }

  @Test
  @DisplayName("body lỗi KHÔNG lộ message nội bộ của Spring Security ra client")
  void shouldNotLeakInternalMessage() {
    ResponseEntity<ApiError> response =
        handler.handleAccessDenied(
            new AccessDeniedException("Access Denied for user 'buyer' with roles [BUYER]"),
            request);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).doesNotContain("BUYER");
  }

  @Test
  @DisplayName(
      "@Order(HIGHEST_PRECEDENCE) còn đó — mất nó là catch-all Exception của GlobalExceptionHandler"
          + " bắt trước ⇒ 403 thành 500 (bẫy thật gặp ở SL-2.12)")
  void shouldStayOrderedBeforeCatchAllAdvice() {
    Order order = AccessDeniedApiExceptionHandler.class.getAnnotation(Order.class);

    assertThat(order).isNotNull();
    assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }
}
