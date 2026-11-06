package com.shoplab.identity.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.identity.testsupport.JwtTestKeys;
import com.shoplab.identity.user.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Unit test cho {@link JwtService} — không cần Spring context, không cần DB.
 *
 * <p>Điểm mấu chốt: token phải verify được bằng <b>public key</b> (không dùng khoá ký), và {@code
 * kid} trong header phải khớp {@code kid} công bố ở JWKS — lệch chỗ này thì service verify sẽ không
 * tìm thấy khoá và trả 401 hàng loạt mà rất khó đoán nguyên nhân.
 */
class JwtServiceTest {

  private static final String ISSUER = "https://identity.shoplab.local";

  @TempDir Path tempDir;

  private JwtTestKeys keys;
  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    keys = JwtTestKeys.generateIn(tempDir);
    jwtService = new JwtService(properties(keys.privateKeyPath(), keys.publicKeyPath()));
  }

  private static JwtProperties properties(Path privateKey, Path publicKey) {
    return new JwtProperties(
        ISSUER,
        Duration.ofMinutes(15),
        Duration.ofDays(14),
        Duration.ofSeconds(30),
        privateKey.toString(),
        publicKey.toString());
  }

  private Jwt decode(String token) {
    return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.keyPair().getPublic())
        .build()
        .decode(token);
  }

  @Test
  @DisplayName("token ký RS256 verify được bằng PUBLIC key (verifier không cần khoá ký)")
  void shouldIssueTokenVerifiableWithPublicKeyOnly() {
    // JWT timestamp chỉ có độ chính xác GIÂY → truncate để assert đẳng thức được.
    Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    UUID userId = UUID.randomUUID();

    IssuedToken token = jwtService.issueAccessToken(userId, Role.BUYER, issuedAt);
    Jwt jwt = decode(token.value());

    assertThat(jwt.getSubject()).isEqualTo(userId.toString());
    assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
    assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("BUYER");
    assertThat(jwt.getIssuedAt()).isEqualTo(issuedAt);
    assertThat(jwt.getExpiresAt()).isEqualTo(issuedAt.plus(Duration.ofMinutes(15)));
  }

  @Test
  @DisplayName("expiresIn trả về đúng số giây theo TTL cấu hình")
  void shouldReportExpiresInSeconds() {
    IssuedToken token = jwtService.issueAccessToken(UUID.randomUUID(), Role.SELLER, Instant.now());

    assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    assertThat(jwtService.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  @DisplayName("kid trong header token khớp kid công bố ở JWKS")
  void keyIdShouldMatchJwks() {
    IssuedToken token = jwtService.issueAccessToken(UUID.randomUUID(), Role.BUYER, Instant.now());

    String headerKid = (String) decode(token.value()).getHeaders().get("kid");
    assertThat(headerKid).isEqualTo(jwtService.keyId());
    assertThat(jwksKey()).containsEntry("kid", jwtService.keyId());
  }

  @Test
  @DisplayName("JWKS chỉ công bố public (n, e) — KHÔNG lộ private exponent 'd'")
  void jwksShouldExposeOnlyPublicParameters() {
    Map<String, Object> key = jwksKey();

    assertThat(key).containsEntry("kty", "RSA").containsKeys("n", "e", "kid");
    assertThat(key).doesNotContainKey("d");
  }

  @Test
  @DisplayName("thiếu file private key → JwtKeyException nêu rõ đường dẫn + cách sửa")
  void shouldFailFastWhenPrivateKeyMissing() {
    JwtProperties missing = properties(tempDir.resolve("khong-ton-tai.pem"), keys.publicKeyPath());

    assertThatThrownBy(() -> new JwtService(missing))
        .isInstanceOf(JwtKeyException.class)
        .hasMessageContaining("khong-ton-tai.pem")
        .hasMessageContaining("gen-dev-keys.sh");
  }

  @Test
  @DisplayName("file khoá sai định dạng → JwtKeyException (không chạy với khoá nửa vời)")
  void shouldFailFastWhenKeyIsNotPem() {
    JwtProperties wrongFormat = properties(keys.publicKeyPath(), keys.publicKeyPath());

    assertThatThrownBy(() -> new JwtService(wrongFormat))
        .isInstanceOf(JwtKeyException.class)
        .hasMessageContaining("BEGIN PRIVATE KEY");
  }

  @Test
  @DisplayName("kid ổn định giữa hai lần khởi tạo cùng khoá (thumbprint RFC 7638)")
  void keyIdShouldBeStableForSameKey() {
    JwtService another = new JwtService(properties(keys.privateKeyPath(), keys.publicKeyPath()));

    assertThat(another.keyId()).isEqualTo(jwtService.keyId());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> jwksKey() {
    List<Map<String, Object>> keysJson = (List<Map<String, Object>>) jwtService.jwks().get("keys");
    assertThat(keysJson).hasSize(1);
    return keysJson.get(0);
  }

  @Test
  @DisplayName("khoá test ghi ra file đúng định dạng PEM")
  void testKeysShouldBeWrittenAsPem() throws Exception {
    assertThat(Files.readString(keys.privateKeyPath())).startsWith("-----BEGIN PRIVATE KEY-----");
    assertThat(Files.readString(keys.publicKeyPath())).startsWith("-----BEGIN PUBLIC KEY-----");
  }
}
