package com.shoplab.template.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của service.
 *
 * <p>Validate ở {@code main()} qua {@code EnvValidator} — xem {@code ServiceTemplateApplication}.
 * Service mới copy file này và thêm biến của mình vào {@link #ALL}.
 */
public final class RequiredEnv {

  /**
   * Môi trường chạy. Bắt buộc vì log level, endpoint debug và cấu hình hạ tầng đều phụ thuộc nó —
   * để mặc định thì rất dễ chạy nhầm cấu hình dev trên môi trường thật.
   */
  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  /** Toàn bộ biến bắt buộc. Thêm biến mới ở đây, không rải rác trong code. */
  public static final List<RequiredConfig> ALL = List.of(APP_ENV);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
