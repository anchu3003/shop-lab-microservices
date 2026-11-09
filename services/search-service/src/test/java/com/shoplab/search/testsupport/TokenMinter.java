package com.shoplab.search.testsupport;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Sinh & verify token cho integration test — cùng một cặp khoá RSA sinh trong RAM.
 *
 * <p>Vì sao không dùng token thật của identity-service: IT của promotion chỉ cần chứng minh
 * <b>promotion tự chặn/cho qua đúng</b>, không phải chứng minh identity ký đúng (việc đó identity
 * IT + e2e lo). Sinh khoá trong test ⇒ không phụ thuộc {@code infra/keys} (file bị gitignore) và
 * không cần dựng thêm HTTP server giả làm JWKS.
 *
 * <p>Đường JWKS thật (tải public key qua mạng) được kiểm chứng ở bước verify compose — gọi qua
 * gateway + gọi thẳng promotion bằng token thật.
 */
public final class TokenMinter {

  /** Claim chứa role — khớp {@code JwtService.ROLE_CLAIM} của identity-service. */
  private static final String ROLE_CLAIM = "role";

  private final NimbusJwtEncoder encoder;
  private final RSAPublicKey publicKey;
  private final String issuer;

  private TokenMinter(RSAKey rsaKey, RSAPublicKey publicKey, String issuer) {
    JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
    this.encoder = new NimbusJwtEncoder(source);
    this.publicKey = publicKey;
    this.issuer = issuer;
  }

  /** Sinh cặp khoá mới + minter tương ứng. */
  public static TokenMinter generate(String issuer) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
      RSAKey rsaKey =
          new RSAKey.Builder(publicKey)
              .privateKey((RSAPrivateKey) keyPair.getPrivate())
              .keyID("promotion-it")
              .build();
      return new TokenMinter(rsaKey, publicKey, issuer);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM không hỗ trợ RSA", e);
    }
  }

  /** Access token hợp lệ 15 phút cho user với role cho trước. */
  public String token(UUID userId, String role) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .claim(ROLE_CLAIM, role)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .build();
    return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /** Decoder tương ứng — thay {@code JwtDecoder} thật (vốn tải JWKS qua mạng) trong IT. */
  public JwtDecoder decoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    // Kiểm cả `iss` như production: token do nơi khác phát hành phải bị từ chối.
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }
}
