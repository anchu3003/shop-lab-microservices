package com.shoplab.search.index;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Một dòng của index (SL-3.6) — bản sao <b>có chọn lọc</b> của product trong catalog.
 *
 * <p>Không phải entity JPA: index không có quan hệ, không cần dirty checking; mọi thao tác là
 * UPSERT / UPDATE có điều kiện trên một bảng phẳng.
 *
 * @param minPrice giá niêm yết của product. Tên giữ theo thiết kế bảng (`min_price`) để khớp
 *     contract API `minPrice`, nhưng <b>không</b> phải min qua các SKU override — giới hạn đã biết
 *     của Phase 3, xem Learning note `SL-3.6`
 */
public record SearchProduct(
    UUID productId,
    String name,
    String description,
    UUID categoryId,
    BigDecimal minPrice,
    String status,
    Instant updatedAt) {

  public static final String ACTIVE = "ACTIVE";
  public static final String ARCHIVED = "ARCHIVED";

  /**
   * Dựng từ payload event `product.updated.v1`.
   *
   * <p>Thiếu field bắt buộc ⇒ ném {@link IllegalArgumentException}: payload hỏng là message độc,
   * retry không làm nó đúng lên — nó phải đi DLQ để người xem, chứ không được âm thầm ghi một dòng
   * index nửa vời (index sai còn tệ hơn index thiếu, vì không ai biết mà reconcile).
   */
  public static SearchProduct fromEvent(Map<String, Object> payload) {
    UUID productId = uuid(payload, "productId");
    UUID categoryId = uuid(payload, "categoryId");
    Object name = payload.get("name");
    Object price = payload.get("price");
    Object updatedAt = payload.get("updatedAt");
    if (name == null || price == null || updatedAt == null) {
      throw new IllegalArgumentException(
          "payload product.updated.v1 thiếu field bắt buộc: name/price/updatedAt");
    }
    String status = payload.get("status") == null ? ACTIVE : payload.get("status").toString();
    Object description = payload.get("description");
    return new SearchProduct(
        productId,
        name.toString(),
        description == null ? null : description.toString(),
        categoryId,
        new BigDecimal(price.toString()),
        status,
        Instant.parse(updatedAt.toString()));
  }

  private static UUID uuid(Map<String, Object> payload, String field) {
    Object value = payload.get(field);
    if (value == null) {
      throw new IllegalArgumentException("payload product event thiếu field bắt buộc: " + field);
    }
    return UUID.fromString(value.toString());
  }
}
