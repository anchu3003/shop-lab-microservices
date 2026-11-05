package com.shoplab.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit test cho {@link UserHeaderRequest} — đây là lớp chặn <b>giả mạo danh tính</b>, sai ở đây là
 * lỗ bảo mật chứ không phải bug hiển thị.
 */
class UserHeaderRequestTest {

  private static MockHttpServletRequest clientRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/users/me");
    request.addHeader(UserHeaders.USER_ID, "ke-gia-danh");
    request.addHeader(UserHeaders.USER_ROLE, "ADMIN");
    request.addHeader("accept", "application/json");
    return request;
  }

  @Test
  @DisplayName("request public (không danh tính) → header x-user-* do client gửi bị XOÁ sạch")
  void shouldStripClientSuppliedUserHeaders() {
    UserHeaderRequest wrapped = new UserHeaderRequest(clientRequest(), null);

    assertThat(wrapped.getHeader(UserHeaders.USER_ID)).isNull();
    assertThat(wrapped.getHeader(UserHeaders.USER_ROLE)).isNull();
    assertThat(Collections.list(wrapped.getHeaders(UserHeaders.USER_ID))).isEmpty();
    assertThat(Collections.list(wrapped.getHeaderNames())).doesNotContain(UserHeaders.USER_ID);
  }

  @Test
  @DisplayName("sau khi verify → header mang giá trị của gateway, không phải của client")
  void shouldOverrideWithGatewayValues() {
    UserHeaderRequest wrapped =
        new UserHeaderRequest(clientRequest(), new AuthenticatedUser("user-123", "BUYER"));

    assertThat(wrapped.getHeader(UserHeaders.USER_ID)).isEqualTo("user-123");
    assertThat(wrapped.getHeader(UserHeaders.USER_ROLE)).isEqualTo("BUYER");
    assertThat(Collections.list(wrapped.getHeaders(UserHeaders.USER_ID)))
        .containsExactly("user-123");
    assertThat(Collections.list(wrapped.getHeaderNames()))
        .contains(UserHeaders.USER_ID, UserHeaders.USER_ROLE);
  }

  @Test
  @DisplayName("header khác (accept, authorization...) giữ nguyên")
  void shouldKeepOtherHeaders() {
    MockHttpServletRequest request = clientRequest();
    request.addHeader("authorization", "Bearer abc");
    UserHeaderRequest wrapped = new UserHeaderRequest(request, null);

    assertThat(wrapped.getHeader("accept")).isEqualTo("application/json");
    assertThat(wrapped.getHeader("authorization")).isEqualTo("Bearer abc");
  }
}
