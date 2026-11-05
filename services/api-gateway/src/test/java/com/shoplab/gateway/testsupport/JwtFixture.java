package com.shoplab.gateway.testsupport;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

/**
 * Sinh khoá + token cho test của gateway.
 *
 * <p><b>Vì sao không dùng identity-service thật</b>: test phải kiểm chứng <i>gateway</i> (verify
 * chữ ký, claim, ghi đè header) — dựng cả identity + Postgres cho việc đó là chậm và che mất lỗi
 * (một chỗ sai ở identity sẽ làm test gateway đỏ mà không rõ ai sai). Tích hợp thật giữa 2 service
 * là việc của E2E smoke ở SL-1.3.
 *
 * <p>Tên class cố ý <b>không</b> khớp pattern test của surefire ({@code Test*}, {@code *Test}...).
 */
public final class JwtFixture {

  public static final String KID = "test-key-1";
  public static final String ISSUER = "https://identity.shoplab.local";

  private final RSAKey rsaKey;
  private final RSAPrivateKey privateKey;

  public JwtFixture() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
      this.rsaKey =
          new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
              .keyID(KID)
              .build();
    } catch (Exception e) {
      throw new IllegalStateException("Không sinh được khoá test", e);
    }
  }

  /** JWKS công bố đúng khoá đang dùng để ký (chỉ public). */
  public String jwksJson() {
    return new JWKSet(rsaKey).toString();
  }

  /** Public key — để test so sánh với key mà gateway lấy được từ JWKS. */
  public java.security.interfaces.RSAPublicKey publicKey() {
    try {
      return rsaKey.toRSAPublicKey();
    } catch (JOSEException e) {
      throw new IllegalStateException("Không lấy được public key", e);
    }
  }

  /** Token hợp lệ: đúng chữ ký, đúng issuer, còn hạn 15 phút. */
  public String validToken(String userId, String role) {
    Instant now = Instant.now();
    return token(userId, role, ISSUER, now.minusSeconds(5), now.plusSeconds(900));
  }

  public String expiredToken(String userId, String role) {
    Instant now = Instant.now();
    return token(userId, role, ISSUER, now.minusSeconds(3600), now.minusSeconds(60));
  }

  public String wrongIssuerToken(String userId, String role) {
    Instant now = Instant.now();
    return token(userId, role, "https://ke-gia-danh.example", now, now.plusSeconds(900));
  }

  public String tokenWithoutRole(String userId) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject(userId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(900)))
            .build();
    return sign(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(), claims, rsaSigner());
  }

  /** Token ký bằng khoá KHÁC (khoá không có trong JWKS). */
  public String tokenSignedByAnotherKey(String userId, String role) {
    JwtFixture other = new JwtFixture();
    Instant now = Instant.now();
    JWTClaimsSet claims = claims(userId, role, ISSUER, now, now.plusSeconds(900));
    return sign(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(), claims, other.rsaSigner());
  }

  /**
   * Token {@code alg=HS256} ký bằng chính public key làm secret — đòn <b>algorithm confusion</b>:
   * thư viện lỏng sẽ "verify thành công" vì coi public key là khoá đối xứng.
   */
  public String hs256Token(String userId, String role) {
    try {
      byte[] secret = rsaKey.toRSAPublicKey().getEncoded();
      Instant now = Instant.now();
      JWTClaimsSet claims = claims(userId, role, ISSUER, now, now.plusSeconds(900));
      return sign(
          new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KID).build(),
          claims,
          new MACSigner(secret));
    } catch (Exception e) {
      throw new IllegalStateException("Không ký được token HS256", e);
    }
  }

  /** Token không có {@code kid} → gateway không biết dùng khoá nào. */
  public String tokenWithoutKid(String userId, String role) {
    Instant now = Instant.now();
    JWTClaimsSet claims = claims(userId, role, ISSUER, now, now.plusSeconds(900));
    return sign(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims, rsaSigner());
  }

  private String token(
      String userId, String role, String issuer, Instant issuedAt, Instant expiresAt) {
    JWTClaimsSet claims = claims(userId, role, issuer, issuedAt, expiresAt);
    return sign(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(), claims, rsaSigner());
  }

  private static JWTClaimsSet claims(
      String userId, String role, String issuer, Instant issuedAt, Instant expiresAt) {
    return new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(userId)
        .issueTime(Date.from(issuedAt))
        .expirationTime(Date.from(expiresAt))
        .claim("role", role)
        .build();
  }

  private RSASSASigner rsaSigner() {
    // Nimbus 10: constructor nhận RSAPrivateKey không còn ném JOSEException (chỉ bản nhận RSAKey
    // mới ném).
    return new RSASSASigner(privateKey);
  }

  private static String sign(JWSHeader header, JWTClaimsSet claims, JWSSigner signer) {
    try {
      SignedJWT jwt = new SignedJWT(header, claims);
      jwt.sign(signer);
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Không ký được token", e);
    }
  }
}
