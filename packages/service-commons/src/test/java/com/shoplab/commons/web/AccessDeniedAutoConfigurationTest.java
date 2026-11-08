package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.access.AccessDeniedException;

/**
 * Kiểm <b>điều kiện</b> của {@link AccessDeniedAutoConfiguration} — phần dễ hỏng nhất khi chuyển
 * handler vào shared lib (SL-2.12).
 *
 * <p>Rủi ro cụ thể: {@code spring-security-core} là dependency <b>optional</b>. Nếu điều kiện
 * {@code @ConditionalOnClass} đặt sai chỗ (trên {@code @Bean} method thay vì cấp class) thì service
 * <b>không</b> có security — như {@code service-template} — sẽ chết lúc khởi động với {@code
 * NoClassDefFoundError}. Test này chứng minh nhánh đó, không cần dựng cả một service.
 */
class AccessDeniedAutoConfigurationTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AccessDeniedAutoConfiguration.class));

  @Test
  @DisplayName("có spring-security trên classpath → advice 403 được đăng ký")
  void shouldRegisterAdviceWhenSecurityPresent() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(AccessDeniedApiExceptionHandler.class);
        });
  }

  @Test
  @DisplayName("KHÔNG có spring-security → auto-config im lặng bỏ qua (không NoClassDefFoundError)")
  void shouldBackOffWhenSecurityMissing() {
    runner
        .withClassLoader(new FilteredClassLoader(AccessDeniedException.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(AccessDeniedApiExceptionHandler.class);
            });
  }
}
