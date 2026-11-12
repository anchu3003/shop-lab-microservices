package com.shoplab.catalog.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sinh key cache. Một chỗ duy nhất — key rải rác trong code là cách chắc chắn nhất để invalidation
 * xoá sai chỗ (bug chỉ lộ ra khi dữ liệu cũ, rất khó tái hiện).
 *
 * <p>Schema (prefix mặc định {@code catalog:v1}):
 *
 * <pre>
 *   catalog:v1:product:&lt;productId&gt;              → ProductDetailResponse
 *   catalog:v1:sku:&lt;skuId&gt;                      → 1 dòng SkuLine (hoặc "null" = không mua được)
 *   catalog:v1:list:&lt;generation&gt;:&lt;fingerprint&gt;   → PageResponse&lt;ProductResponse&gt;
 *   catalog:v1:list:gen                          → generation hiện tại (INCR khi ghi)
 *   catalog:v1:categories                        → List&lt;CategoryResponse&gt;
 *   catalog:v1:lock:&lt;sha256(key)&gt;                → lock single-flight
 * </pre>
 *
 * <p><b>Vì sao list có {@code generation}</b>: list nhận 5 tham số tự do ⇒ số tổ hợp key là vô hạn,
 * không thể liệt kê để xoá (SCAN ở 3.800 RPS là tự sát). Bump generation là 1 lệnh {@code INCR} làm
 * <b>toàn bộ</b> list cũ thành vô nghĩa; chúng chết dần theo TTL. Đây là "version key" mà ticket
 * gọi tên.
 */
@Component
public final class CacheKeys {

  private final String prefix;

  public CacheKeys(CacheProperties properties) {
    this.prefix = properties.keyPrefix();
  }

  public String detail(UUID productId) {
    return prefix + ":product:" + productId;
  }

  public String sku(UUID skuId) {
    return prefix + ":sku:" + skuId;
  }

  public String list(long generation, String fingerprint) {
    return prefix + ":list:" + generation + ":" + fingerprint;
  }

  public String listGeneration() {
    return prefix + ":list:gen";
  }

  public String categories() {
    return prefix + ":categories";
  }

  /**
   * Key lock = hash của key đích, không phải key đích + hậu tố.
   *
   * <p>Lý do: key list có thể dài (hash 24 ký tự + prefix) và Redis cluster băm theo <b>toàn bộ</b>
   * key — dùng hậu tố thì lock nằm khác slot với dữ liệu, không sao, nhưng key dài vô ích trong
   * log/metric. Hash cố định 24 ký tự cho mọi loại.
   */
  public String lock(String targetKey) {
    return prefix + ":lock:" + sha256Hex(targetKey).substring(0, 24);
  }

  /**
   * Fingerprint của một truy vấn list — cùng tham số ⇒ cùng key, khác tham số ⇒ khác key.
   *
   * <p><b>Null phải khác chuỗi rỗng/'-'</b>: {@code q=} (không lọc) và {@code q=-} (lọc theo tên có
   * dấu gạch) cho ra <b>kết quả khác nhau</b>. Nếu mã hoá cả hai thành {@code "-"} thì hai truy vấn
   * khác nhau dùng chung một key ⇒ trả sai dữ liệu. Vì vậy dùng marker {@code \u0000} không thể
   * xuất hiện trong query string.
   */
  public static String listFingerprint(
      int page, int size, UUID categoryId, String q, String sortField, String sortDirection) {
    String raw =
        page
            + "|"
            + size
            + "|"
            + marker(categoryId == null ? null : categoryId.toString())
            + "|"
            + marker(q)
            + "|"
            + sortField
            + "|"
            + sortDirection;
    return sha256Hex(raw).substring(0, 24);
  }

  /** {@code null} → marker riêng, để không bao giờ trùng một giá trị thật. */
  private static String marker(String value) {
    return value == null ? "\u0000null" : "=" + value;
  }

  static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      // SHA-256 là bắt buộc trong mọi JRE — không có nghĩa máy hỏng.
      throw new IllegalStateException("JRE không có SHA-256", ex);
    }
  }
}
