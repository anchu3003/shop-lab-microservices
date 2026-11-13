package com.shoplab.gateway.config;

import com.shoplab.gateway.auth.JwtAuthFilter;
import com.shoplab.gateway.auth.JwtVerifier;
import com.shoplab.gateway.ratelimit.ClientIpResolver;
import com.shoplab.gateway.ratelimit.RateLimitFilter;
import com.shoplab.gateway.ratelimit.RateLimitMetrics;
import com.shoplab.gateway.ratelimit.RateLimitProperties;
import com.shoplab.gateway.ratelimit.RateLimitRules;
import com.shoplab.gateway.ratelimit.RateLimiter;
import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

/** Đăng ký filter của gateway vào servlet container với thứ tự chạy tường minh. */
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

  /**
   * Rate limit chạy <b>sau</b> verify token (order {@code +20}) — điều kiện để bucket theo user
   * dùng được {@code x-user-id} <b>đã</b> verify. Chạy trước sẽ buộc filter phải tự verify token
   * (verify 2 lần) hoặc tin {@code sub} chưa verify (lỗ hổng: mỗi request một {@code sub} bịa = vô
   * hạn bucket — xem {@code RateLimitFilter}).
   *
   * <p>Vẫn <b>trước</b> {@code GatewayRoutingController}: 429 phải rẻ hơn việc gọi service đích,
   * nếu không thì "chặn ở biên" chỉ là khẩu hiệu.
   */
  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
      RateLimitProperties properties,
      RateLimitRules rules,
      RateLimiter limiter,
      ClientIpResolver ipResolver,
      RateLimitMetrics metrics,
      Clock clock,
      ObjectMapper objectMapper) {

    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(
            new RateLimitFilter(
                properties, rules, limiter, ipResolver, metrics, clock, objectMapper));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
