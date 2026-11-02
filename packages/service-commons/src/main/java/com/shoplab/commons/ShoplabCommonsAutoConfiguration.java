package com.shoplab.commons;

import com.shoplab.commons.web.CorrelationIdFilter;
import com.shoplab.commons.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Tự động cấu hình hạ tầng dùng chung cho mọi service ShopLab.
 *
 * <p>Vì sao dùng auto-configuration thay vì {@code @ComponentScan} trên package của thư viện:
 *
 * <ul>
 *   <li>service chỉ cần khai dependency là có đủ bean — không phải nhớ scan package nào;
 *   <li>mọi bean đều {@code @ConditionalOnMissingBean} → service ghi đè được khi cần;
 *   <li>tường minh: danh sách bean nhìn thấy ở một chỗ, không phải đi tìm trong 20 package.
 * </ul>
 *
 * <p>Đăng ký qua {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ShoplabCommonsAutoConfiguration {

  /**
   * Đăng ký filter qua {@link FilterRegistrationBean} (không expose filter như bean riêng) để tránh
   * câu hỏi "bean này có bị Spring Boot tự đăng ký lần thứ hai không".
   */
  @Bean
  @ConditionalOnMissingBean(name = "shoplabCorrelationIdFilterRegistration")
  public FilterRegistrationBean<CorrelationIdFilter> shoplabCorrelationIdFilterRegistration() {
    FilterRegistrationBean<CorrelationIdFilter> registration =
        new FilterRegistrationBean<>(new CorrelationIdFilter());
    // Chạy sớm nhất: mọi filter/log phía sau đều cần có traceId trong MDC.
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  @ConditionalOnMissingBean
  public GlobalExceptionHandler shoplabGlobalExceptionHandler() {
    return new GlobalExceptionHandler();
  }
}
