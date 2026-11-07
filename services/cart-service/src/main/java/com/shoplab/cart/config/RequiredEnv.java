package com.shoplab.cart.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của cart-service.
 *
 * <p>Khác mọi service khác: <b>không</b> có {@code POSTGRES_PASSWORD} (cart không có Postgres)
 * nhưng có {@code REDIS_PASSWORD} — Redis của compose bật {@code requirepass}, thiếu password thì
 * Lettuce mới báo lỗi ở lệnh đầu tiên (muộn, khó hiểu).
 */
public final class RequiredEnv {

  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  public static final RequiredConfig REDIS_PASSWORD =
      RequiredConfig.of("REDIS_PASSWORD", "mật khẩu Redis (xem .env.example)");

  public static final List<RequiredConfig> ALL = List.of(APP_ENV, REDIS_PASSWORD);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
