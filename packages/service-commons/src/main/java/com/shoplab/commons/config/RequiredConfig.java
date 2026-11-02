package com.shoplab.commons.config;

/**
 * Mô tả một biến môi trường bắt buộc.
 *
 * @param key tên biến (UPPER_SNAKE, vd {@code APP_ENV})
 * @param description giải thích ngắn — được in vào message lỗi để người đọc biết phải set gì
 */
public record RequiredConfig(String key, String description) {

  public static RequiredConfig of(String key, String description) {
    return new RequiredConfig(key, description);
  }
}
