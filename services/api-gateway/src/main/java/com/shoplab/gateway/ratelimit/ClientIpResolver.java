package com.shoplab.gateway.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Lấy IP dùng làm khoá bucket — <b>không tin header client tự đặt</b>.
 *
 * <p>Quy tắc: chỉ đọc {@code X-Forwarded-For} khi peer TCP nằm trong {@code trusted-proxies}
 * (proxy/LB của mình đã ghi header đó). Không có cấu hình ⇒ luôn dùng IP của peer TCP. Nếu tin
 * {@code X-Forwarded-For} vô điều kiện thì kẻ tấn công chỉ cần đổi header mỗi request là có vô hạn
 * bucket ⇒ rate limit bằng 0.
 *
 * <p>Khi peer <b>được</b> tin: đi từ phải sang trái và bỏ qua các hop tin cậy — phần tử <b>phải
 * nhất không tin cậy</b> là IP thật mà proxy tin cậy gần nhất nhìn thấy. Lấy phần tử <b>trái</b>
 * nhất sẽ là IP do client tự khai (client chèn được vào đầu danh sách).
 *
 * <p>Chỉ so khớp IP <b>chính xác</b> (không CIDR): dải proxy của repo này là vài container, thêm
 * parser CIDR chỉ để sai. Khi có LB thật thì khai IP của nó, hoặc mở ticket để hỗ trợ CIDR.
 */
@Component
public class ClientIpResolver {

  private static final String FORWARDED_FOR = "X-Forwarded-For";
  private static final String UNKNOWN = "unknown";

  private final Set<String> trustedProxies;

  public ClientIpResolver(RateLimitProperties properties) {
    this.trustedProxies = Set.copyOf(properties.trustedProxies());
  }

  public String resolve(HttpServletRequest request) {
    String peer = request.getRemoteAddr();
    if (peer == null || peer.isBlank()) {
      return UNKNOWN;
    }
    if (!trustedProxies.contains(peer)) {
      return peer;
    }
    String forwarded = request.getHeader(FORWARDED_FOR);
    if (forwarded == null || forwarded.isBlank()) {
      return peer;
    }
    List<String> hops = List.of(forwarded.split(","));
    for (int i = hops.size() - 1; i >= 0; i--) {
      String hop = hops.get(i).trim();
      if (hop.isEmpty()) {
        continue;
      }
      if (!trustedProxies.contains(hop)) {
        return hop;
      }
    }
    return peer;
  }
}
