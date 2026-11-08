package com.shoplab.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.identity.auth.jwt.JwtProperties;
import com.shoplab.identity.auth.jwt.JwtService;
import com.shoplab.identity.config.ServiceAuthProperties;
import com.shoplab.identity.testsupport.JwtTestKeys;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Unit test cấp service token (SL-3.11) — không cần Spring context, không cần DB. */
class ServiceAuthServiceTest {

  private static final String ISSUER = "https://identity.shoplab.local";
  // Decoder kiểm `exp` theo đồng hồ THẬT ⇒ mốc thời gian phải quanh lúc chạy (mốc cố định trong quá
  // khứ làm token "hết hạn ngay khi tạo" — đã sai thật).
  private static final Instant NOW =
      Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
  private static final Duration TTL = Duration.ofMinutes(5);
  private static final String SECRET = "secret-cua-order-service";

  @TempDir Path tempDir;

  private JwtTestKeys keys;
  private ServiceAuthService service;

  @BeforeEach
  void setUp() {
    keys = JwtTestKeys.generateIn(tempDir);
    JwtService jwtService = new JwtService(properties(keys));
    ServiceAuthProperties props =
        new ServiceAuthProperties(
            TTL,
            Map.of(
                "order-service",
                new ServiceAuthProperties.Client(
                    SECRET, List.of("inventory-service", "promotion-service"))));
    service = new ServiceAuthService(props, jwtService, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("đúng client + audience ⇒ token có role=SERVICE, aud, sub=svc:<client>, exp=+5m")
  void shouldIssueServiceToken() {
    ServiceTokenResponse response =
        service.issue(new ServiceTokenRequest("order-service", SECRET, "inventory-service"));

    Jwt jwt = decode(response.accessToken());
    assertThat(jwt.getClaimAsString("role")).isEqualTo("SERVICE");
    assertThat(jwt.getAudience()).containsExactly("inventory-service");
    assertThat(jwt.getSubject()).isEqualTo("svc:order-service");
    assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
    assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
    assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(TTL));
    assertThat(response.expiresIn()).isEqualTo(TTL.toSeconds());
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.serviceId()).isEqualTo("order-service");
  }

  @Test
  @DisplayName("client lạ và sai secret trả CÙNG một lỗi 401 (không dò được client nào tồn tại)")
  void shouldNotLeakWhetherClientExists() {
    assertThatThrownBy(
            () ->
                service.issue(new ServiceTokenRequest("order-service", "sai", "inventory-service")))
        .isInstanceOf(AppException.class)
        .hasMessage("clientId hoặc clientSecret không đúng");

    assertThatThrownBy(
            () ->
                service.issue(
                    new ServiceTokenRequest("khong-ton-tai", SECRET, "inventory-service")))
        .isInstanceOf(AppException.class)
        .hasMessage("clientId hoặc clientSecret không đúng");
  }

  @Test
  @DisplayName("audience ngoài allowlist ⇒ 400 (nếu không, aud chỉ là trang trí)")
  void shouldRejectAudienceOutsideAllowlist() {
    assertThatThrownBy(
            () ->
                service.issue(new ServiceTokenRequest("order-service", SECRET, "payment-service")))
        .isInstanceOf(AppException.class)
        .satisfies(
            ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR))
        .hasMessageContaining("không được phép xin token cho audience 'payment-service'");
  }

  @Test
  @DisplayName("secret sai độ dài khác vẫn bị từ chối (so sánh hằng thời gian không ném lỗi)")
  void shouldRejectSecretOfDifferentLength() {
    assertThatThrownBy(
            () -> service.issue(new ServiceTokenRequest("order-service", "", "inventory-service")))
        .isInstanceOf(AppException.class);
  }

  private Jwt decode(String token) {
    return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.keyPair().getPublic())
        .build()
        .decode(token);
  }

  private static JwtProperties properties(JwtTestKeys keys) {
    return new JwtProperties(
        ISSUER,
        Duration.ofMinutes(15),
        Duration.ofDays(14),
        Duration.ofSeconds(30),
        keys.privateKeyPath().toString(),
        keys.publicKeyPath().toString());
  }
}
