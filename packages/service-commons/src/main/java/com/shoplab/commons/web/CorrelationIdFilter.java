package com.shoplab.commons.web;

import com.shoplab.commons.log.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gắn correlation id vào mọi request:
 *
 * <ol>
 *   <li>đọc header {@code x-correlation-id} do client/gateway gửi (nếu có và hợp lệ),
 *   <li>thiếu/không hợp lệ thì sinh mới,
 *   <li>đẩy vào MDC để mọi log trong request đều có {@code traceId},
 *   <li>echo lại vào response header để client tự đối chiếu khi báo lỗi.
 * </ol>
 *
 * <p>Filter này phải chạy <b>sớm nhất</b> (order cao nhất) — nếu không, log của các filter đứng
 * trước nó sẽ không có {@code traceId} và mất khả năng truy vết.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String correlationId = CorrelationId.resolve(request.getHeader(CorrelationId.HEADER));
    MDC.put(MdcKeys.TRACE_ID, correlationId);
    response.setHeader(CorrelationId.HEADER, correlationId);

    long startedAt = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
      LOG.info(
          "http {} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      // Bắt buộc: thread của Tomcat được tái sử dụng, không xoá MDC là rò rỉ traceId sang request
      // sau.
      MDC.remove(MdcKeys.TRACE_ID);
    }
  }
}
