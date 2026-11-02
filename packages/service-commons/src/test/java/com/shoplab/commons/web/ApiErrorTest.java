package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.log.MdcKeys;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ApiErrorTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("message null → dùng message mặc định của ErrorCode")
  void shouldFallBackToDefaultMessage() {
    ApiError error = ApiError.of(ErrorCode.NOT_FOUND, null, "/orders/1");

    assertThat(error.message()).isEqualTo(ErrorCode.NOT_FOUND.defaultMessage());
  }

  @Test
  @DisplayName("details null → danh sách rỗng (client không phải xử lý null)")
  void shouldDefaultDetailsToEmptyList() {
    ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR, "lỗi", "/x");

    assertThat(error.details()).isEmpty();
    assertThat(ApiError.of(ErrorCode.INTERNAL_ERROR, "lỗi", "/x", null).details()).isEmpty();
  }

  @Test
  @DisplayName("code luôn là tên ErrorCode, không phải số HTTP")
  void shouldUseErrorCodeName() {
    assertThat(ApiError.of(ErrorCode.CONFLICT, "trùng", "/x").code()).isEqualTo("CONFLICT");
  }

  @Test
  @DisplayName("traceId lấy từ MDC do CorrelationIdFilter set")
  void shouldTakeTraceIdFromMdc() {
    MDC.put(MdcKeys.TRACE_ID, "trace-xyz");

    assertThat(ApiError.of(ErrorCode.NOT_FOUND, "x", "/x").traceId()).isEqualTo("trace-xyz");
  }

  @Test
  @DisplayName("giữ nguyên path và danh sách chi tiết field")
  void shouldKeepPathAndDetails() {
    List<ApiError.FieldViolation> details =
        List.of(new ApiError.FieldViolation("email", "sai định dạng"));

    ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, null, "/users", details);

    assertThat(error.path()).isEqualTo("/users");
    assertThat(error.details())
        .containsExactly(new ApiError.FieldViolation("email", "sai định dạng"));
    assertThat(error.timestamp()).isNotNull();
  }
}
