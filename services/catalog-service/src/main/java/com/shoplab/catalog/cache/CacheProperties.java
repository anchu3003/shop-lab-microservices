package com.shoplab.catalog.cache;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số của cache-aside đường đọc (ADR-0009, {@code SL-4.2}).
 *
 * <p><b>Vì sao cấu hình được toàn bộ</b>: before/after của ticket này phải đo trên <b>cùng một
 * image</b> — bật/tắt bằng {@code SHOPLAB_CATALOG_CACHE_ENABLED} rồi chạy lại cùng bộ load test. So
 * 2 commit khác nhau thì không tách được "cache có tác dụng" khỏi "hệ đổi vì lý do khác".
 *
 * @param enabled tắt cache hoàn toàn (mọi request đi thẳng DB, không gọi Redis)
 * @param ttl TTL gốc của {@code detail}/{@code list}/{@code sku}; bị jitter trước khi ghi
 * @param ttlJitterPercent biên jitter (±%). 0 = tắt jitter (dùng khi cần TTL chính xác để test)
 * @param categoriesTtl TTL của danh sách category — đổi rất ít nên dài hơn
 * @param lockTtl TTL của lock single-flight; phải &gt; thời gian đọc DB bình thường, đủ ngắn để
 *     process chết giữa đường không khoá key vĩnh viễn
 * @param lockWait thời gian request thua lock chờ cache được điền trước khi tự đọc DB
 * @param keyPrefix prefix mọi key — đổi giá trị này là "xoá" toàn bộ cache cũ (namespace mới)
 * @param warnInterval nhịp tối đa của log WARN khi Redis lỗi (tránh ngập log lúc sự cố)
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.catalog.cache")
public record CacheProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("60s") Duration ttl,
    @DefaultValue("10") @Min(0) @Max(90) int ttlJitterPercent,
    @DefaultValue("5m") Duration categoriesTtl,
    @DefaultValue("2s") Duration lockTtl,
    @DefaultValue("100ms") Duration lockWait,
    @DefaultValue("catalog:v1") @NotBlank String keyPrefix,
    @DefaultValue("5s") Duration warnInterval) {

  public CacheProperties {
    requirePositive(ttl, "shoplab.catalog.cache.ttl");
    requirePositive(categoriesTtl, "shoplab.catalog.cache.categories-ttl");
    requirePositive(lockTtl, "shoplab.catalog.cache.lock-ttl");
    if (lockWait == null || lockWait.isNegative()) {
      throw new IllegalArgumentException("shoplab.catalog.cache.lock-wait không được âm");
    }
    if (keyPrefix != null && (keyPrefix.contains(" ") || keyPrefix.contains("/"))) {
      throw new IllegalArgumentException(
          "shoplab.catalog.cache.key-prefix không được chứa khoảng trắng hoặc '/': " + keyPrefix);
    }
  }

  private static void requirePositive(Duration value, String name) {
    if (value == null || value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException(name + " phải > 0");
    }
  }
}
