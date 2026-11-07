package com.shoplab.inventory.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của inventory-service.
 *
 * <p>Validate ở {@code main()} qua {@code EnvValidator} — xem {@code InventoryServiceApplication}.
 */
public final class RequiredEnv {

  /**
   * Môi trường chạy. Bắt buộc vì log level, endpoint debug và cấu hình hạ tầng đều phụ thuộc nó.
   */
  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  /**
   * Mật khẩu Postgres. Bắt buộc vì service có DB thật: thiếu nó thì HikariCP mới báo lỗi ở lần
   * query đầu tiên, muộn và khó hiểu.
   */
  public static final RequiredConfig POSTGRES_PASSWORD =
      RequiredConfig.of("POSTGRES_PASSWORD", "mật khẩu Postgres (xem .env.example)");

  /** Toàn bộ biến bắt buộc. Thêm biến mới ở đây, không rải rác trong code. */
  public static final List<RequiredConfig> ALL = List.of(APP_ENV, POSTGRES_PASSWORD);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
