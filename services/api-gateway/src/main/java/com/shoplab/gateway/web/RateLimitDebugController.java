package com.shoplab.gateway.web;

import com.shoplab.gateway.ratelimit.RateLimitMetrics;
import com.shoplab.gateway.ratelimit.RateLimitProperties;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trạng thái rate limit — để <b>drill và vận hành</b> đọc được bằng máy, không phải đoán từ log.
 *
 * <p>Cần vì 2 lý do rất cụ thể:
 *
 * <ul>
 *   <li>Khi bị 429 hàng loạt do ai đó đổi cấu hình, câu hỏi đầu tiên luôn là "ngưỡng đang là bao
 *       nhiêu" — trả lời bằng cách {@code docker inspect} env của container là cách làm của người
 *       đoán mò.
 *   <li>Drill cần biết limiter có <b>đang bật</b> hay không trước khi kết luận "không thấy 429"
 *       (một kết luận sai rất dễ xảy ra nếu limiter đang tắt).
 * </ul>
 *
 * <p>Bật/tắt bằng {@code shoplab.debug-endpoints} như các endpoint debug khác; {@code @Hidden} để
 * không lọt vào OpenAPI spec.
 */
@RestController
@Hidden
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class RateLimitDebugController {

  private final RateLimitProperties properties;
  private final RateLimitMetrics metrics;

  public RateLimitDebugController(RateLimitProperties properties, RateLimitMetrics metrics) {
    this.properties = properties;
    this.metrics = metrics;
  }

  /** Cấu hình đang có hiệu lực + bộ đếm từ lúc khởi động. */
  @GetMapping("/debug/rate-limit")
  public Map<String, Object> state() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("enabled", properties.enabled());
    out.put("failOpen", properties.failOpen());
    out.put("keyPrefix", properties.keyPrefix());
    out.put("defaultPerUser", limit(properties.defaultPerUser()));
    out.put("defaultPerIp", limit(properties.defaultPerIp()));
    out.put("trustedProxies", properties.trustedProxies());
    out.put("bypass", properties.bypass());
    out.put("routes", properties.routes().stream().map(this::rule).toList());
    out.put("counters", metrics.snapshot());
    return out;
  }

  private Map<String, Object> rule(RateLimitProperties.Rule rule) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("pathPrefix", rule.pathPrefix());
    out.put("method", rule.method() == null ? "*" : rule.method());
    out.put("perUser", rule.perUser() == null ? null : limit(rule.perUser()));
    out.put("perIp", rule.perIp() == null ? null : limit(rule.perIp()));
    return out;
  }

  private Map<String, Object> limit(RateLimitProperties.Limit limit) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("rps", limit.rps());
    out.put("burst", limit.burst());
    return out;
  }
}
