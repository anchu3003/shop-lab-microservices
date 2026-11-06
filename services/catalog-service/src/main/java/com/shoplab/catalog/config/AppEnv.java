package com.shoplab.catalog.config;

import java.util.Locale;

/**
 * Môi trường chạy của service, lấy từ env {@code APP_ENV}.
 *
 * <p>Cố ý là enum thay vì {@code String}: gõ sai {@code APP_ENV=locall} phải chết lúc boot, chứ
 * không âm thầm coi như "không phải local" rồi bật/tắt nhầm hành vi.
 */
public enum AppEnv {
  LOCAL,
  DEV,
  STAGING,
  PROD;

  /**
   * Parse giá trị thô từ env.
   *
   * @throws IllegalArgumentException khi rỗng hoặc không thuộc 4 giá trị hợp lệ — message nêu rõ
   *     giá trị nhận được và danh sách hợp lệ
   */
  public static AppEnv parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException(
          "APP_ENV chưa được set — không biết service đang chạy ở môi trường nào");
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    for (AppEnv candidate : values()) {
      if (candidate.name().equals(normalized)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException(
        "APP_ENV='" + raw + "' không hợp lệ — chỉ nhận: local, dev, staging, prod");
  }

  /** Có phải máy dev không — dùng để quyết định bật endpoint debug, log verbose... */
  public boolean isLocal() {
    return this == LOCAL;
  }
}
