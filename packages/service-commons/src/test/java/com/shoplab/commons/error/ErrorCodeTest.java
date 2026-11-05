package com.shoplab.commons.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {

  @Test
  @DisplayName("map đúng các status quen thuộc")
  void shouldMapKnownStatuses() {
    assertThat(ErrorCode.fromHttpStatus(400)).isEqualTo(ErrorCode.VALIDATION_ERROR);
    assertThat(ErrorCode.fromHttpStatus(401)).isEqualTo(ErrorCode.UNAUTHORIZED);
    assertThat(ErrorCode.fromHttpStatus(403)).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(ErrorCode.fromHttpStatus(404)).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(ErrorCode.fromHttpStatus(409)).isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  @DisplayName("4xx khác (405, 415...) → REQUEST_ERROR nhưng HTTP status vẫn được giữ")
  void shouldFallBackForOtherClientErrors() {
    assertThat(ErrorCode.fromHttpStatus(405)).isEqualTo(ErrorCode.REQUEST_ERROR);
    assertThat(ErrorCode.fromHttpStatus(415)).isEqualTo(ErrorCode.REQUEST_ERROR);
  }

  @Test
  @DisplayName("5xx và status lạ → INTERNAL_ERROR")
  void shouldFallBackToInternalError() {
    assertThat(ErrorCode.fromHttpStatus(500)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    assertThat(ErrorCode.fromHttpStatus(503)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    assertThat(ErrorCode.fromHttpStatus(200)).isEqualTo(ErrorCode.INTERNAL_ERROR);
  }

  @Test
  @DisplayName("mỗi mã có http status và message mặc định")
  void shouldExposeStatusAndMessage() {
    assertThat(ErrorCode.NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ErrorCode.VALIDATION_ERROR.defaultMessage()).isNotBlank();
  }
}
