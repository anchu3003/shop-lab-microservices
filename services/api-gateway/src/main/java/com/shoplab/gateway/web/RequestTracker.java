package com.shoplab.gateway.web;

import com.shoplab.commons.web.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ghi nhớ request đang chạy để lúc shutdown log ra được <b>đang chờ request nào</b>.
 *
 * <p>Không có cái này thì log shutdown chỉ nói "đang chờ request" mà không nói là request gì — đúng
 * lúc cần debug nhất (deploy treo 30s) lại không có thông tin.
 *
 * <p>Chạy sau {@code CorrelationIdFilter} (filter này là bean thường → order mặc định thấp nhất)
 * nên {@link CorrelationId#current()} đã có giá trị.
 */
@Component
public class RequestTracker extends OncePerRequestFilter {

  /** key = mô tả request, value = thời điểm bắt đầu (nanos). */
  private final Map<String, Long> inFlight = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String key = describe(request);
    inFlight.put(key, System.nanoTime());
    try {
      chain.doFilter(request, response);
    } finally {
      // Bắt buộc: thread Tomcat được tái sử dụng, không xoá là tracker phình dần và báo sai.
      inFlight.remove(key);
    }
  }

  /** Số request đang xử lý. */
  public int inFlightCount() {
    return inFlight.size();
  }

  /** Mô tả các request đang xử lý, kèm thời gian đã chạy — dùng cho log shutdown. */
  public List<String> inFlightRequests() {
    List<String> descriptions = new ArrayList<>();
    inFlight.forEach(
        (key, startedAtNanos) ->
            descriptions.add(
                key + " (" + (System.nanoTime() - startedAtNanos) / 1_000_000L + " ms)"));
    return List.copyOf(descriptions);
  }

  private static String describe(HttpServletRequest request) {
    String correlationId = CorrelationId.current();
    String prefix = correlationId == null ? "-" : correlationId;
    return prefix + " " + request.getMethod() + " " + request.getRequestURI();
  }
}
