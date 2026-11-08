package com.shoplab.commons.serviceauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit test luật kiểm token ở service đích (SL-3.11).
 *
 * <p>Cái phải chứng minh: {@code aud} chặn được service token phát cho service khác, **và** không
 * làm hỏng token người dùng (không có {@code aud}) — nếu sai vế thứ hai thì cả hệ 401 hàng loạt sau
 * khi deploy.
 */
class ServiceTokenRulesTest {

  private static final String ISSUER = "https://identity.shoplab.local";
  private static final String ME = "inventory-service";
  // `JwtValidators.createDefaultWithIssuer` kiểm `exp` theo đồng hồ THẬT ⇒ token trong test phải
  // sống
  // quanh thời điểm chạy, không dùng mốc cố định (đã sai thật lần đầu: token "hết hạn" ngay khi
  // tạo).
  private static final Instant NOW = Instant.now();

  private final OAuth2TokenValidator<Jwt> validator = ServiceTokenRules.validator(ISSUER, ME);

  @Test
  @DisplayName("service token đúng audience ⇒ pass")
  void shouldAcceptServiceTokenWithMatchingAudience() {
    assertThat(validate(jwt("SERVICE", List.of(ME))).hasErrors()).isFalse();
  }

  @Test
  @DisplayName("service token phát cho service khác ⇒ fail (đây là toàn bộ giá trị của `aud`)")
  void shouldRejectServiceTokenForAnotherService() {
    OAuth2TokenValidatorResult result = validate(jwt("SERVICE", List.of("promotion-service")));

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.getErrorCode()).isEqualTo("invalid_token");
              assertThat(error.getDescription()).contains(ME).contains("promotion-service");
            });
  }

  @Test
  @DisplayName("token người dùng (không có aud) ⇒ pass — không hồi quy hợp đồng token cũ")
  void shouldAcceptUserTokenWithoutAudience() {
    assertThat(validate(jwt("BUYER", List.of())).hasErrors()).isFalse();
  }

  @Test
  @DisplayName("sai issuer ⇒ fail kể cả khi role/aud đúng (token nơi khác phát hành)")
  void shouldRejectWrongIssuer() {
    Jwt token =
        Jwt.withTokenValue("t")
            .header("alg", "RS256")
            .issuer("https://ke-gia-mao.example")
            .subject("svc:order-service")
            .audience(List.of(ME))
            .claim(ServiceTokenRules.ROLE_CLAIM, "SERVICE")
            .issuedAt(NOW)
            .expiresAt(NOW.plusSeconds(300))
            .build();

    assertThat(validate(token).hasErrors()).isTrue();
  }

  @Test
  @DisplayName("token hết hạn ⇒ fail (kiểm exp vẫn do validator issuer lo)")
  void shouldRejectExpiredToken() {
    Jwt expired =
        Jwt.withTokenValue("t")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("svc:order-service")
            .audience(List.of(ME))
            .claim(ServiceTokenRules.ROLE_CLAIM, "SERVICE")
            .issuedAt(NOW.minusSeconds(600))
            .expiresAt(NOW.minusSeconds(300))
            .build();

    assertThat(validate(expired).hasErrors()).isTrue();
  }

  private OAuth2TokenValidatorResult validate(Jwt token) {
    return validator.validate(token);
  }

  private static Jwt jwt(String role, List<String> audience) {
    Jwt.Builder builder =
        Jwt.withTokenValue("t")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("SERVICE".equals(role) ? "svc:order-service" : "user-1")
            .claim(ServiceTokenRules.ROLE_CLAIM, role)
            .issuedAt(NOW)
            .expiresAt(NOW.plusSeconds(300));
    if (!audience.isEmpty()) {
      builder.audience(audience);
    }
    return builder.claims(claims -> claims.putAll(Map.of())).build();
  }
}
