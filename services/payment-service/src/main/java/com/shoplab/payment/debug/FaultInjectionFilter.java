package com.shoplab.payment.debug;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Áp công tắc của {@link FaultInjection} lên request thật — chỉ tồn tại khi bật debug endpoints.
 *
 * <p><b>Vì sao dùng filter thay vì sửa {@code PaymentController}</b>: bơm lỗi là chuyện của hạ tầng
 * test, không phải nghiệp vụ. Filter giữ controller sạch (production không có một dòng "if debug"
 * nào) và chỉ chạy khi bean tồn tại.
 *
 * <p>Filter mặc định chạy <b>sau</b> Spring Security ⇒ request đã được xác thực trước khi bị làm
 * chậm — đúng thứ tự của một dependency thật.
 */
@Component
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class FaultInjectionFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(FaultInjectionFilter.class);

  private final FaultInjection faults;
  private final ObjectMapper objectMapper;

  public FaultInjectionFilter(FaultInjection faults, ObjectMapper objectMapper) {
    this.faults = faults;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !faults.active() || !faults.matches(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long sleepMs = faults.sleepMs();
    if (sleepMs > 0) {
      LOG.warn(
          "FAULT INJECTION: giữ {} ms cho {} {}",
          sleepMs,
          request.getMethod(),
          request.getRequestURI());
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    int failStatus = faults.failStatus();
    if (failStatus > 0) {
      LOG.warn("FAULT INJECTION: trả {} cho {}", failStatus, request.getRequestURI());
      writeError(response, request.getRequestURI(), failStatus);
      return;
    }
    chain.doFilter(request, response);
  }

  private void writeError(HttpServletResponse response, String path, int status)
      throws IOException {
    ErrorCode code = ErrorCode.fromHttpStatus(status);
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(ApiError.of(code, "Fault injection: " + status, path)));
  }
}
