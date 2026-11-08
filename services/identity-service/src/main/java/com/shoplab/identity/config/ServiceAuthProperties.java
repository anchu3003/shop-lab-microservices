package com.shoplab.identity.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Registry client cho <b>service-to-service auth</b> (SL-3.11) — đọc từ config (12-factor).
 *
 * <pre>
 * shoplab.service-auth:
 *   service-token-ttl: 5m
 *   clients:
 *     order-service:
 *       secret: ${ORDER_SERVICE_CLIENT_SECRET}
 *       allowed-audiences: [inventory-service, promotion-service]
 * </pre>
 *
 * <p><b>Vì sao registry nằm trong config, không phải bảng DB</b>: Phase 3 chỉ có 1–2 client; bảng
 * client + API quản lý client + rotate qua UI là hạ tầng của Phase 6/7 (`AGENTS.md` §3.4 — không
 * over-engineer). Hệ quả chấp nhận: thêm client mới = sửa config + restart (không cần build lại
 * image).
 *
 * <p><b>Vì sao mỗi client có {@code allowedAudiences}</b>: audience do <i>client</i> xin, nên nếu
 * server không giới hạn thì một service bị lộ secret xin được token cho <b>mọi</b> service đích ⇒
 * mất ý nghĩa của {@code aud}. Allowlist biến "quyền đi tới đâu" thành dữ liệu cấu hình, đọc được,
 * review được.
 *
 * @param serviceTokenTtl thời gian sống của service token — ngắn có chủ ý (5 phút): token rò rỉ chỉ
 *     sống rất lâu bằng một cửa sổ cache, không cần cơ chế thu hồi
 * @param clients client id → (secret, audience được phép)
 */
@ConfigurationProperties(prefix = "shoplab.service-auth")
public record ServiceAuthProperties(
    @DefaultValue("5m") Duration serviceTokenTtl, @DefaultValue Map<String, Client> clients) {

  public ServiceAuthProperties {
    clients = clients == null ? Map.of() : Map.copyOf(clients);
    if (serviceTokenTtl == null || serviceTokenTtl.isNegative() || serviceTokenTtl.isZero()) {
      throw new IllegalArgumentException("shoplab.service-auth.service-token-ttl phải > 0");
    }
  }

  /**
   * @return cấu hình của client, rỗng nếu chưa đăng ký
   */
  public java.util.Optional<Client> find(String clientId) {
    return java.util.Optional.ofNullable(clients.get(clientId));
  }

  /**
   * Một client (một service nội bộ).
   *
   * @param secret secret dùng chung giữa service gọi và identity — đọc từ env, <b>không</b> commit
   * @param allowedAudiences service đích mà client này được phép xin token (vd {@code
   *     order-service} → {@code [inventory-service, promotion-service]})
   */
  public record Client(String secret, @DefaultValue List<String> allowedAudiences) {

    public Client {
      if (secret == null || secret.isBlank()) {
        throw new IllegalArgumentException(
            "client service-to-service thiếu secret — đặt qua env (AGENTS.md §9)");
      }
      allowedAudiences = allowedAudiences == null ? List.of() : List.copyOf(allowedAudiences);
    }
  }
}
