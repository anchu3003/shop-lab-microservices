package com.shoplab.search.index;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tham số của `GET /v1/search` sau khi validate (SL-3.6) — không có validation nào trong
 * controller: mọi thứ client gửi đều đi qua {@link #parse} trước.
 *
 * <p>Vì sao 400 chứ không âm thầm "clamp" (kẹp về giá trị hợp lệ): client gửi {@code size=1000} mà
 * server lặng lẽ trả 20 là bug im lặng — client tưởng đã lấy đủ dữ liệu. Cùng quyết định với
 * catalog (`SL-1.6`).
 *
 * @param q từ khoá đã trim, lower-case, {@code null} = không lọc theo từ khoá
 */
public record SearchQuery(
    String q, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {

  /**
   * Trần `size`: mỗi query FTS đắt hơn list thường, và trần bảo vệ DB khỏi request `size=100000`.
   */
  public static final int MAX_SIZE = 100;

  public static SearchQuery parse(
      String q,
      String categoryId,
      String minPrice,
      String maxPrice,
      String page,
      String size,
      int maxQueryLength) {
    String term = normalizeTerm(q, maxQueryLength);
    BigDecimal from = parseMoney(minPrice, "minPrice");
    BigDecimal to = parseMoney(maxPrice, "maxPrice");
    if (from != null && to != null && from.compareTo(to) > 0) {
      throw new InvalidQueryException("minPrice không được lớn hơn maxPrice");
    }
    return new SearchQuery(term, parseUuid(categoryId), from, to, parsePage(page), parseSize(size));
  }

  public int offset() {
    return page * size;
  }

  /** Từ khoá rỗng/toàn khoảng trắng = không lọc (client gửi `q=` là chuyện bình thường). */
  private static String normalizeTerm(String q, int maxQueryLength) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String term = q.trim();
    if (term.length() > maxQueryLength) {
      throw new InvalidQueryException(
          "q tối đa " + maxQueryLength + " ký tự, nhận: " + term.length());
    }
    return term;
  }

  private static BigDecimal parseMoney(String raw, String field) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    BigDecimal value;
    try {
      value = new BigDecimal(raw.trim());
    } catch (NumberFormatException ex) {
      throw new InvalidQueryException(field + " phải là số, nhận: '" + raw + "'");
    }
    if (value.signum() < 0) {
      throw new InvalidQueryException(field + " không được âm, nhận: " + raw);
    }
    return value;
  }

  private static UUID parseUuid(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(raw.trim());
    } catch (IllegalArgumentException ex) {
      throw new InvalidQueryException("categoryId phải là UUID, nhận: '" + raw + "'");
    }
  }

  private static int parsePage(String raw) {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    int page;
    try {
      page = Integer.parseInt(raw.trim());
    } catch (NumberFormatException ex) {
      throw new InvalidQueryException("page phải là số nguyên, nhận: '" + raw + "'");
    }
    if (page < 0) {
      throw new InvalidQueryException("page phải >= 0, nhận: " + page);
    }
    return page;
  }

  private static int parseSize(String raw) {
    if (raw == null || raw.isBlank()) {
      return 20;
    }
    int size;
    try {
      size = Integer.parseInt(raw.trim());
    } catch (NumberFormatException ex) {
      throw new InvalidQueryException("size phải là số nguyên, nhận: '" + raw + "'");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new InvalidQueryException("size phải trong khoảng 1.." + MAX_SIZE + ", nhận: " + size);
    }
    return size;
  }

  /** Tham số truy vấn không hợp lệ → HTTP 400 (cùng khuôn `catalog.InvalidQueryException`). */
  public static class InvalidQueryException extends AppException {

    public InvalidQueryException(String message) {
      super(ErrorCode.VALIDATION_ERROR, message);
    }
  }
}
