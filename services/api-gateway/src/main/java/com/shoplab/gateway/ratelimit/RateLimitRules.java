package com.shoplab.gateway.ratelimit;

import com.shoplab.gateway.routing.RouteMatcher;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chọn rule rate limit cho một request.
 *
 * <p>Luật khớp dùng lại đúng {@link RouteMatcher#matchesPrefix} của routing — prefix dài nhất
 * thắng, khớp theo <b>ranh giới segment</b>. Dùng chung một hàm thay vì viết lại {@code startsWith}
 * là cố ý: lần trước bug "prefix {@code /v1/auth} khớp cả {@code /v1/authz}" đã được sửa một lần,
 * không sửa lại lần hai ở chỗ khác.
 *
 * <p><b>Route key</b> (phần giữa của key Redis): prefix của rule khớp, hoặc {@code default} nếu
 * không rule nào khớp. Nghĩa là mọi path chưa khai rule dùng <b>chung</b> một xô theo user/IP (đúng
 * ngữ nghĩa "hạn mức mặc định"), không phải mỗi path một xô.
 */
@Component
public class RateLimitRules {

  /** Route key cho path không khớp rule nào. */
  static final String DEFAULT_ROUTE_KEY = "default";

  private final RateLimitProperties properties;
  private final List<RateLimitProperties.Rule> rulesByLongestPrefixFirst;

  public RateLimitRules(RateLimitProperties properties) {
    this.properties = properties;
    this.rulesByLongestPrefixFirst =
        properties.routes().stream()
            .sorted(
                Comparator.comparingInt(
                        (RateLimitProperties.Rule rule) -> rule.pathPrefix().length())
                    .reversed())
            .toList();
  }

  /** Rule áp cho request — không bao giờ rỗng: luôn có ít nhất hạn mức mặc định. */
  public ResolvedRateLimit resolve(String path, String method) {
    for (RateLimitProperties.Rule rule : rulesByLongestPrefixFirst) {
      if (!RouteMatcher.matchesPrefix(path, rule.pathPrefix())) {
        continue;
      }
      if (rule.method() != null
          && !rule.method().isBlank()
          && !rule.method().trim().equalsIgnoreCase(method)) {
        continue;
      }
      return new ResolvedRateLimit(
          routeKey(rule.pathPrefix()),
          rule.perUser() == null ? properties.defaultPerUser() : rule.perUser(),
          rule.perIp() == null ? properties.defaultPerIp() : rule.perIp());
    }
    return new ResolvedRateLimit(
        DEFAULT_ROUTE_KEY, properties.defaultPerUser(), properties.defaultPerIp());
  }

  /** {@code /v1/checkout} → {@code v1-checkout}: key không chứa ký tự phân cách của key Redis. */
  static String routeKey(String pathPrefix) {
    String trimmed = pathPrefix.startsWith("/") ? pathPrefix.substring(1) : pathPrefix;
    trimmed = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    return trimmed.isBlank() ? DEFAULT_ROUTE_KEY : trimmed.replace('/', '-');
  }
}
