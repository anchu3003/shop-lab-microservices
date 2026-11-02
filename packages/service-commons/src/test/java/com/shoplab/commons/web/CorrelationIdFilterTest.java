package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.log.MdcKeys;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("giữ id của client, đẩy vào MDC và echo lại response header")
  void shouldReuseClientCorrelationId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
    request.addHeader(CorrelationId.HEADER, "client-abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> duringRequest = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> duringRequest.set(CorrelationId.current()));

    assertThat(duringRequest.get()).isEqualTo("client-abc-123");
    assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("client-abc-123");
  }

  @Test
  @DisplayName("thiếu header → sinh id mới và trả về cho client")
  void shouldGenerateCorrelationIdWhenHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> duringRequest = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> duringRequest.set(CorrelationId.current()));

    assertThat(duringRequest.get()).isNotBlank();
    assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(duringRequest.get());
  }

  @Test
  @DisplayName("xoá MDC sau request — thread Tomcat tái sử dụng, không xoá là rò traceId")
  void shouldClearMdcAfterRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request, response, (req, res) -> assertThat(CorrelationId.current()).isNotNull());

    assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
  }

  @Test
  @DisplayName("xoá MDC cả khi xử lý request ném exception")
  void shouldClearMdcEvenWhenChainThrows() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request,
                    response,
                    (req, res) -> {
                      throw new IllegalStateException("lỗi trong chain");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
  }
}
