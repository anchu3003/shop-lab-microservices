package com.shoplab.promotion.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của promotion-service.
 *
 * <p>Validate ở {@code main()} qua {@code EnvValidator} — xem {@code PromotionServiceApplication}.
 */
public final class RequiredEnv {

  /** Môi trường chạy — log level, endpoint debug, cấu hình hạ tầng đều phụ thuộc. */
  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  /** Mật khẩu Postgres — thiếu thì HikariCP mới báo lỗi ở lần query đầu tiên (muộn, khó hiểu). */
  public static final RequiredConfig POSTGRES_PASSWORD =
      RequiredConfig.of("POSTGRES_PASSWORD", "mật khẩu Postgres (xem .env.example)");

  public static final List<RequiredConfig> ALL = List.of(APP_ENV, POSTGRES_PASSWORD);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
