package com.shoplab.identity.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình JWT của identity-service.
 *
 * @param issuer giá trị {@code iss} trong token — service verify có thể kiểm tra
 * @param accessTokenTtl thời gian sống của access token (mặc định 15 phút)
 * @param refreshTokenTtl thời gian sống của refresh token (mặc định 14 ngày) — dài vì nó là thứ duy
 *     nhất giữ phiên đăng nhập, nhưng rotate mỗi lần dùng nên cửa sổ bị đánh cắp hẹp
 * @param privateKeyPath đường dẫn private key PKCS#8 PEM — CHỈ service này có
 * @param publicKeyPath đường dẫn public key SPKI PEM — công bố qua JWKS
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.jwt")
public record JwtProperties(
    @DefaultValue("https://identity.shoplab.local") String issuer,
    @DefaultValue("15m") Duration accessTokenTtl,
    @DefaultValue("14d") Duration refreshTokenTtl,
    @DefaultValue("30s") Duration refreshReuseGrace,
    @NotBlank String privateKeyPath,
    @NotBlank String publicKeyPath) {

  public JwtProperties {
    if (accessTokenTtl == null) {
      accessTokenTtl = Duration.ofMinutes(15);
    }
    if (refreshTokenTtl == null) {
      refreshTokenTtl = Duration.ofDays(14);
    }
    if (refreshReuseGrace == null) {
      refreshReuseGrace = Duration.ofSeconds(30);
    }
  }
}
