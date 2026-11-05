package com.shoplab.gateway.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của api-gateway.
 *
 * <p>Validate ở {@code main()} qua {@code EnvValidator} — xem {@code ApiGatewayApplication}.
 */
public final class RequiredEnv {

  /**
   * Môi trường chạy. Bắt buộc vì log level, endpoint debug và cấu hình hạ tầng đều phụ thuộc nó —
   * để mặc định thì rất dễ chạy nhầm cấu hình dev trên môi trường thật.
   */
  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  /**
   * Base URL của identity-service. Bắt buộc vì gateway vừa route tới đó vừa lấy JWKS ở đó: để mặc
   * định {@code localhost} là sai trong container (phải là service name trên network của compose).
   */
  public static final RequiredConfig IDENTITY_SERVICE_URL =
      RequiredConfig.of(
          "IDENTITY_SERVICE_URL", "base URL identity-service, vd http://identity-service:8081");

  /** Toàn bộ biến bắt buộc. Thêm biến mới ở đây, không rải rác trong code. */
  public static final List<RequiredConfig> ALL = List.of(APP_ENV, IDENTITY_SERVICE_URL);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
