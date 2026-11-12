package com.shoplab.catalog.web;

import com.shoplab.catalog.cache.CacheMetrics;
import com.shoplab.catalog.cache.CacheProperties;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Số liệu cache cho việc verify {@code SL-4.2} — <b>không phải</b> API cho client.
 *
 * <p>Bật cùng điều kiện với {@link DebugController} ({@code shoplab.debug-endpoints=true}, chỉ set
 * trong compose local) và {@code @Hidden} khỏi OpenAPI ⇒ không lộ ra ngoài, không đổi contract.
 *
 * <p><b>Vì sao cần endpoint này thay vì đọc log</b>: AC của ticket cần <b>hit ratio</b> và <b>số
 * lần đọc DB</b> theo một cửa sổ đo. Log phải bật DEBUG (3.800 request/s ⇒ không dùng được) còn log
 * INFO thì hoặc ồn hoặc thưa; bộ đếm đọc một lần ở cuối phép đo là cách chính xác và rẻ nhất. Đây
 * cũng là khuôn để Phase 6 thay bằng metric thật (Prometheus) mà không đổi chỗ lấy số.
 */
@RestController
@Hidden
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class CacheDebugController {

  private final CacheMetrics metrics;
  private final CacheProperties properties;

  public CacheDebugController(CacheMetrics metrics, CacheProperties properties) {
    this.metrics = metrics;
    this.properties = properties;
  }

  /** Tổng hợp (hit ratio, dbLoads, bypass, error) + bộ đếm chi tiết theo loại. */
  @GetMapping("/debug/cache")
  public Map<String, Object> cache() {
    return Map.of(
        "enabled", properties.enabled(),
        "ttl", properties.ttl().toString(),
        "ttlJitterPercent", properties.ttlJitterPercent(),
        "summary", metrics.summary(),
        "counters", metrics.snapshot());
  }
}
