package com.shoplab.search.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tham số của search-service (SL-3.6) — đọc từ env (12-factor).
 *
 * @param retryDelays backoff của consumer (ngữ nghĩa ADR-0007 D5: 200 ms/1 s/3 s)
 * @param catalogUrl base URL của catalog-service — dùng cho <b>reconcile</b> và cho phép so số row.
 *     Đây là phụ thuộc **một chiều, chậm rãi** (chỉ khi bấm reindex hoặc theo lịch), không nằm trên
 *     đường request của khách
 * @param catalogPageSize số product mỗi lần đọc khi reindex — 100 là trần của `GET /v1/products`
 * @param driftCheckInterval chu kỳ so số row giữa index và catalog
 * @param driftCheckInitialDelay chờ trước lần so đầu tiên (cho app khởi động xong)
 * @param driftThresholdPercent lệch bao nhiêu phần trăm thì log WARN (mặc định 1%)
 * @param maxQueryLength chặn `q` dài (mỗi query FTS vẫn tốn CPU dù có index)
 */
@ConfigurationProperties(prefix = "shoplab.search")
public record SearchProperties(
    @DefaultValue({"200ms", "1s", "3s"}) List<Duration> retryDelays,
    @DefaultValue("http://localhost:8082") String catalogUrl,
    @DefaultValue("100") int catalogPageSize,
    @DefaultValue("10m") Duration driftCheckInterval,
    @DefaultValue("60s") Duration driftCheckInitialDelay,
    @DefaultValue("1") BigDecimal driftThresholdPercent,
    @DefaultValue("100") int maxQueryLength) {

  public SearchProperties {
    retryDelays = retryDelays == null ? List.of() : List.copyOf(retryDelays);
    if (catalogPageSize < 1 || catalogPageSize > 100) {
      throw new IllegalArgumentException("shoplab.search.catalog-page-size phải trong 1..100");
    }
    if (maxQueryLength < 1) {
      throw new IllegalArgumentException("shoplab.search.max-query-length phải > 0");
    }
    if (driftThresholdPercent == null || driftThresholdPercent.signum() < 0) {
      throw new IllegalArgumentException("shoplab.search.drift-threshold-percent phải >= 0");
    }
  }
}
