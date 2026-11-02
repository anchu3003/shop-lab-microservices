package com.shoplab.commons.config;

import java.util.List;

/**
 * Ném khi thiếu biến môi trường bắt buộc.
 *
 * <p>Cố ý gom <b>tất cả</b> biến thiếu vào một message: sửa 5 biến mà phải chạy lại 5 lần vì mỗi
 * lần chỉ báo một biến là trải nghiệm tệ. Fail fast nhưng fail một lần cho xong.
 */
public class MissingConfigException extends RuntimeException {

  private final List<String> missingKeys;

  public MissingConfigException(List<RequiredConfig> missing) {
    super(buildMessage(missing));
    this.missingKeys = missing.stream().map(RequiredConfig::key).toList();
  }

  /** Danh sách key bị thiếu — để test và để tầng trên xử lý tiếp nếu cần. */
  public List<String> missingKeys() {
    return missingKeys;
  }

  private static String buildMessage(List<RequiredConfig> missing) {
    StringBuilder message =
        new StringBuilder(
            "Thiếu biến môi trường bắt buộc — service dừng ngay để không chạy nửa vời:");
    for (RequiredConfig item : missing) {
      message
          .append(System.lineSeparator())
          .append("  - ")
          .append(item.key())
          .append(": ")
          .append(item.description());
    }
    message.append(System.lineSeparator()).append("Xem .env.example để lấy giá trị mẫu.");
    return message.toString();
  }
}
