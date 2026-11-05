package com.shoplab.gateway.routing;

import com.shoplab.gateway.config.GatewayProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Khớp path của request với route trong config.
 *
 * <p>Luật: <b>prefix dài nhất thắng</b> — để sau này thêm route cụ thể hơn (vd {@code
 * /v1/auth/admin}) mà không phải quan tâm thứ tự khai trong file config. Xếp sẵn theo độ dài giảm
 * dần một lần lúc tạo.
 *
 * <p>Khớp theo <b>ranh giới đường dẫn</b>, không phải {@code startsWith} thô: prefix {@code
 * /v1/auth} <b>không</b> được khớp với {@code /v1/authz} — bug kinh điển của gateway tự viết.
 */
@Component
public class RouteMatcher {

  private final List<GatewayProperties.Route> routesByLongestPrefixFirst;

  public RouteMatcher(GatewayProperties properties) {
    this.routesByLongestPrefixFirst =
        properties.routes().stream()
            .sorted(
                Comparator.comparingInt(
                        (GatewayProperties.Route route) -> route.pathPrefix().length())
                    .reversed())
            .toList();
  }

  /** Route khớp request, rỗng nghĩa là gateway không biết gửi đi đâu → 404. */
  public Optional<GatewayProperties.Route> match(String path) {
    return routesByLongestPrefixFirst.stream()
        .filter(route -> matchesPrefix(path, route.pathPrefix()))
        .findFirst();
  }

  /** So khớp có ý thức về ranh giới segment: {@code /v1/auth} hoặc {@code /v1/auth/...}. */
  public static boolean matchesPrefix(String path, String prefix) {
    if (path == null || prefix == null || prefix.isBlank()) {
      return false;
    }
    String normalizedPrefix =
        prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    return path.equals(normalizedPrefix) || path.startsWith(normalizedPrefix + "/");
  }
}
