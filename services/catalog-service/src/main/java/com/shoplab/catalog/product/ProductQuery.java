package com.shoplab.catalog.product;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;

/**
 * Tham số đã validate của {@code GET /v1/products} — tách khỏi controller để test được bằng unit
 * test (không cần HTTP).
 *
 * <p><b>Vì sao sort phải là whitelist</b>:
 *
 * <ul>
 *   <li>Nhận field tự do ⇒ (a) lộ schema nội bộ qua thông báo lỗi, (b) client sort trên cột không
 *       có index ⇒ Postgres phải full scan + sort trong bộ nhớ, chậm đúng lúc tải cao.
 *   <li>Whitelist cũng là chỗ chặn injection kiểu {@code sort=drop table} — value không bao giờ
 *       được ghép vào SQL, chỉ tra trong map.
 * </ul>
 */
public record ProductQuery(
    int page, int size, UUID categoryId, String q, String sortField, Sort.Direction sortDirection) {

  /** Mặc định 20 — đủ cho một màn hình lướt sản phẩm, không quá nặng cho Phase 1. */
  public static final int DEFAULT_SIZE = 20;

  /**
   * Trần cứng 100: đây là dạng "rate limit ở tầng dữ liệu" đầu tiên — một client không thể xin 1
   * triệu row để tự DoS cả service.
   */
  public static final int MAX_SIZE = 100;

  /** Whitelist: key là giá trị client gửi (lowercase), value là property của entity (có index). */
  private static final Map<String, String> SORTABLE =
      Map.of("price", "price", "createdat", "createdAt");

  /** Sort mặc định: mới nhất trước (cột {@code created_at} có index). */
  private static final String DEFAULT_SORT_FIELD = "createdAt";

  private static final int MAX_TERM_LENGTH = 100;

  /** Parse toàn bộ tham số thô từ query string. Mọi giá trị sai ⇒ {@link InvalidQueryException}. */
  public static ProductQuery parse(
      String page, String size, String categoryId, String q, String sort) {

    SortSpec spec = parseSort(sort);
    return new ProductQuery(
        parsePage(page),
        parseSize(size),
        parseCategoryId(categoryId),
        parseTerm(q),
        spec.field(),
        spec.direction());
  }

  private static int parsePage(String raw) {
    if (isBlank(raw)) {
      return 0;
    }
    int page;
    try {
      page = Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new InvalidQueryException("page phải là số nguyên, nhận: '" + raw + "'");
    }
    if (page < 0) {
      throw new InvalidQueryException("page phải >= 0, nhận: " + page);
    }
    return page;
  }

  private static int parseSize(String raw) {
    if (isBlank(raw)) {
      return DEFAULT_SIZE;
    }
    int size;
    try {
      size = Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new InvalidQueryException("size phải là số nguyên, nhận: '" + raw + "'");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new InvalidQueryException("size phải trong khoảng 1.." + MAX_SIZE + ", nhận: " + size);
    }
    return size;
  }

  private static UUID parseCategoryId(String raw) {
    if (isBlank(raw)) {
      return null;
    }
    try {
      return UUID.fromString(raw.trim());
    } catch (IllegalArgumentException e) {
      throw new InvalidQueryException("categoryId phải là UUID, nhận: '" + raw + "'");
    }
  }

  private static String parseTerm(String raw) {
    if (isBlank(raw)) {
      return null;
    }
    String term = raw.trim().toLowerCase(Locale.ROOT);
    if (term.length() > MAX_TERM_LENGTH) {
      throw new InvalidQueryException("q tối đa " + MAX_TERM_LENGTH + " ký tự");
    }
    return term;
  }

  private static SortSpec parseSort(String raw) {
    if (isBlank(raw)) {
      return new SortSpec(DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
    String[] parts = raw.trim().split(",", -1);
    if (parts.length > 2 || parts[0].isBlank()) {
      throw new InvalidQueryException(
          "sort phải có dạng '<field>' hoặc '<field>,<asc|desc>', nhận: '" + raw + "'");
    }
    String field = SORTABLE.get(parts[0].trim().toLowerCase(Locale.ROOT));
    if (field == null) {
      throw new InvalidQueryException(
          "sort chỉ nhận: price, createdAt (không nhận '" + parts[0] + "')");
    }
    Sort.Direction direction = Sort.Direction.ASC;
    if (parts.length == 2 && !parts[1].isBlank()) {
      direction = parseDirection(parts[1]);
    }
    return new SortSpec(field, direction);
  }

  private static Sort.Direction parseDirection(String raw) {
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if ("asc".equals(value)) {
      return Sort.Direction.ASC;
    }
    if ("desc".equals(value)) {
      return Sort.Direction.DESC;
    }
    throw new InvalidQueryException("hướng sort chỉ nhận asc hoặc desc, nhận: '" + raw + "'");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /** Cặp field + hướng sau khi đã validate. */
  private record SortSpec(String field, Sort.Direction direction) {}
}
