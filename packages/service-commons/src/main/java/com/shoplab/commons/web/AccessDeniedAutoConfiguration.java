package com.shoplab.commons.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;

/**
 * Cắm {@link AccessDeniedApiExceptionHandler} vào mọi service có spring-security (SL-2.12).
 *
 * <p><b>Vì sao là auto-config <i>riêng</i> chứ không thêm {@code @Bean} vào {@code
 * ShoplabCommonsAutoConfiguration}</b>: {@code spring-security-core} là dependency <b>optional</b>
 * của commons, nên có service <i>không</i> có nó trên classpath (vd {@code service-template}). Nếu
 * {@code @ConditionalOnClass} đặt trên một {@code @Bean} method thì Spring vẫn phải resolve <b>kiểu
 * trả về</b> — mà class đó import {@link AccessDeniedException} ⇒ {@code NoClassDefFoundError} lúc
 * khởi động. Đặt điều kiện ở <b>cấp class</b> thì Spring Boot đọc metadata bằng ASM, class không bị
 * load khi thiếu dependency.
 *
 * <p>{@code @ConditionalOnMissingBean} giữ nguyên tinh thần ADR-0004: service vẫn ghi đè được
 * handler khi cần hành vi khác (vd thêm field vào body lỗi).
 */
@AutoConfiguration
@ConditionalOnClass(AccessDeniedException.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AccessDeniedAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AccessDeniedApiExceptionHandler shoplabAccessDeniedApiExceptionHandler() {
    return new AccessDeniedApiExceptionHandler();
  }
}
