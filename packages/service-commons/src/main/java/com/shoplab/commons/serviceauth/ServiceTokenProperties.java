package com.shoplab.commons.serviceauth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình **phía gọi** của service-to-service auth (SL-3.11) — đọc từ env (12-factor).
 *
 * <pre>
 * shoplab.service-auth:
 *   client-id: order-service
 *   client-secret: ${ORDER_SERVICE_CLIENT_SECRET}
 *   token-url: ${IDENTITY_URL:http://identity-service:8081}/v1/auth/service-token
 *   ttl-skew: 30s
 * </pre>
 *
 * <p>Chỉ service nào **gọi** service khác mới cần block này. Service chỉ nhận request (vd
 * `inventory`) không khai ⇒ không có client bean, không đi xin token (xem {@code
 * ServiceTokenClientAutoConfiguration}).
 *
 * @param clientId tên service trong registry của identity
 * @param clientSecret secret tương ứng — chỉ đọc từ env
 * @param tokenUrl endpoint cấp token
 * @param ttlSkew xin token mới sớm hơn hạn bao lâu (token hết hạn giữa request là lỗi khó lần)
 */
@ConfigurationProperties(prefix = "shoplab.service-auth")
public record ServiceTokenProperties(
    String clientId,
    String clientSecret,
    @DefaultValue("http://localhost:8081/v1/auth/service-token") String tokenUrl,
    @DefaultValue("30s") Duration ttlSkew) {

  /** Đã cấu hình để đi xin token chưa. */
  public boolean configured() {
    return clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }
}
