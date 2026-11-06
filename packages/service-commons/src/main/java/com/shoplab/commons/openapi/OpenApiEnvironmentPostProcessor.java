package com.shoplab.commons.openapi;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Tắt {@code /v3/api-docs} ở mọi môi trường **không phải local** (SL-1.8).
 *
 * <p><b>Vì sao cần</b>: spec API là bản đồ chi tiết hệ thống — endpoint, kiểu dữ liệu, mã lỗi. Trên
 * môi trường thật nó là thông tin cho kẻ tấn công, không phải tiện ích. Bật/tắt phải là hành vi
 * <b>mặc định an toàn (fail-closed)</b>, không phụ thuộc ai nhớ set env.
 *
 * <p><b>Cách làm</b>: chạy sau khi config data (yml/env) đã nạp; nếu {@code APP_ENV} khác {@code
 * local} và người dùng <b>không</b> set tường minh {@code SHOPLAB_OPENAPI_ENABLED} thì đặt {@code
 * springdoc.api-docs.enabled=false}. Muốn bật ở môi trường khác (vd staging nội bộ) thì set env đó
 * — quyết định đó phải là hành động có chủ ý.
 *
 * <p>Đăng ký qua {@code META-INF/spring.factories} (cơ chế của {@link EnvironmentPostProcessor}).
 */
public class OpenApiEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  /** Property của springdoc quyết định có expose {@code /v3/api-docs} hay không. */
  static final String ENABLED_PROPERTY = "springdoc.api-docs.enabled";

  /** Env quyết định môi trường chạy (bắt buộc với mọi service — xem {@code RequiredEnv}). */
  static final String APP_ENV_PROPERTY = "APP_ENV";

  /** Override có chủ ý — có set thì tôn trọng, kể cả trên môi trường thật. */
  static final String OVERRIDE_PROPERTY = "SHOPLAB_OPENAPI_ENABLED";

  private static final String LOCAL = "local";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {

    String appEnv = environment.getProperty(APP_ENV_PROPERTY);
    if (appEnv == null || appEnv.isBlank() || LOCAL.equalsIgnoreCase(appEnv.trim())) {
      return;
    }
    if (environment.getProperty(OVERRIDE_PROPERTY) != null) {
      return;
    }
    // addFirst: thắng mọi nguồn khác (kể cả yml đang để default `true`).
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "shoplabOpenApiGuard", Map.of(ENABLED_PROPERTY, Boolean.FALSE.toString())));
  }

  /** Phải chạy SAU khi yml/env đã nạp, nếu không sẽ bị yml ghi đè ngược lại. */
  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }
}
