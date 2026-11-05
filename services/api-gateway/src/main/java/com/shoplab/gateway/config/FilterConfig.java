package com.shoplab.gateway.config;

import com.shoplab.gateway.auth.JwtAuthFilter;
import com.shoplab.gateway.auth.JwtVerifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

/** Đăng ký {@link JwtAuthFilter} vào servlet container với thứ tự chạy tường minh. */
@Configuration
public class FilterConfig {

  /**
   * Thứ tự: <b>sau</b> {@code CorrelationIdFilter} của commons (order {@code HIGHEST_PRECEDENCE}) —
   * vì filter này log lý do từ chối và ghi {@code ApiError} cần {@code traceId} đã có trong MDC.
   * Chạy trước nó thì lỗi 401 sẽ không truy vết được.
   */
  @Bean
  public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(
      JwtVerifier verifier, GatewayProperties properties, ObjectMapper objectMapper) {

    FilterRegistrationBean<JwtAuthFilter> registration =
        new FilterRegistrationBean<>(new JwtAuthFilter(verifier, properties, objectMapper));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
