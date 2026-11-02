package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo");

  @Test
  @DisplayName("AppException → HTTP status + code lấy từ ErrorCode")
  void shouldMapAppExceptionToItsOwnStatus() {
    ResponseEntity<ApiError> response =
        handler.handleAppException(new NotFoundException("không có"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    assertThat(response.getBody().message()).isEqualTo("không có");
    assertThat(response.getBody().path()).isEqualTo("/demo");
  }

  @Test
  @DisplayName("AppException không có message riêng → dùng message mặc định")
  void shouldFallBackToDefaultMessage() {
    ResponseEntity<ApiError> response =
        handler.handleAppException(new AppException(ErrorCode.CONFLICT), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorCode.CONFLICT.defaultMessage());
  }

  @Test
  @DisplayName("lỗi validation → 400 kèm chi tiết từng field")
  void shouldReturnFieldViolationsForValidationErrors() throws NoSuchMethodException {
    Method method =
        GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleHandler", Sample.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Sample(), "sample");
    bindingResult.rejectValue("name", "NotBlank", "không được để trống");

    ResponseEntity<ApiError> response =
        handler.handleValidation(
            new MethodArgumentNotValidException(parameter, bindingResult), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().details()).hasSize(1);
    assertThat(response.getBody().details().get(0).field()).isEqualTo("name");
    assertThat(response.getBody().details().get(0).message()).isEqualTo("không được để trống");
  }

  @Test
  @DisplayName("lỗi 4xx của Spring MVC (405...) → GIỮ NGUYÊN status, không biến thành 500")
  void shouldPreserveStatusForSpringMvcClientErrors() {
    ResponseEntity<ApiError> response =
        handler.handleUnexpected(
            new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("REQUEST_ERROR");
  }

  @Test
  @DisplayName("lỗi không lường trước → 500 và KHÔNG lộ chi tiết nội bộ")
  void shouldHideInternalDetailsForUnexpectedErrors() {
    ResponseEntity<ApiError> response =
        handler.handleUnexpected(new IllegalStateException("chi tiết nội bộ tuyệt mật"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    assertThat(response.getBody().message()).doesNotContain("chi tiết nội bộ tuyệt mật");
  }

  /** Bean có property {@code name} để BeanPropertyBindingResult tìm thấy field. */
  static class Sample {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static void sampleHandler(Sample sample) {
    // Chỉ tồn tại để lấy MethodParameter cho MethodArgumentNotValidException trong test.
  }
}
