package com.shoplab.payment.debug;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit test cho công tắc bơm lỗi của payment-service (`SL-2.9`).
 *
 * <p>Đây là **code test-only** nhưng vẫn phải test: một công tắc bơm lỗi hoạt động sai sẽ làm E2E
 * "xanh giả" (tưởng saga bù đúng trong khi thực ra payment chưa hề lỗi).
 */
class FaultInjectionTest {

  private final FaultInjection faults = new FaultInjection();
  private final FaultInjectionFilter filter = new FaultInjectionFilter(faults, new ObjectMapper());

  @Test
  @DisplayName("sleepMs bị kẹp về [0, MAX_SLEEP_MS] — không giữ thread vô hạn")
  void clampsSleep() {
    faults.apply(-5, 0);
    assertThat(faults.sleepMs()).isZero();
    assertThat(faults.active()).isFalse();

    faults.apply(FaultInjection.MAX_SLEEP_MS + 1_000, 0);
    assertThat(faults.sleepMs()).isEqualTo(FaultInjection.MAX_SLEEP_MS);
  }

  @Test
  @DisplayName("failStatus ngoài 400..599 → lỗi (không bơm 2xx/3xx)")
  void rejectsInvalidFailStatus() {
    assertThatThrownBy(() -> faults.apply(0, 200))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("400..599");
    assertThat(faults.failStatus()).isZero();
  }

  @Test
  @DisplayName("chỉ bơm cho /v1/payment-intents — health/webhook không bị ảnh hưởng")
  void onlyAffectsIntentPath() {
    faults.apply(0, 500);

    assertThat(faults.matches("/v1/payment-intents")).isTrue();
    assertThat(faults.matches("/health")).isFalse();
    assertThat(faults.matches("/v1/webhooks/payment")).isFalse();
  }

  @Test
  @DisplayName("failStatus → filter trả lỗi, KHÔNG gọi chain (request không tới controller)")
  void filterFailsWithoutCallingChain() throws Exception {
    faults.apply(0, 503);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/payment-intents");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString()).contains("Fault injection").contains("traceId");
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  @DisplayName("sleepMs = 0 và failStatus = 0 → filter bỏ qua hoàn toàn")
  void filterPassesWhenInactive() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/payment-intents");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
