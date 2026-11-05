package com.shoplab.gateway.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.shoplab.gateway.config.GatewayProperties;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * Verify access token: chữ ký <b>RS256</b> bằng public key lấy từ JWKS, rồi mới tới các claim.
 *
 * <p>Thứ tự có chủ ý: <b>chữ ký trước, claim sau</b>. Claim đọc từ token chưa verify là dữ liệu do
 * kẻ tấn công cung cấp — tin nó là lỗi kinh điển.
 *
 * <p>Chỉ chấp nhận {@code alg = RS256}: chặn tấn công <b>algorithm confusion</b> (kẻ tấn công ký
 * token bằng HS256 với "khoá" là chính public key — thư viện lỏng sẽ verify thành công).
 */
@Component
public class JwtVerifier {

  /** Claim chứa vai trò — do identity-service phát hành (xem {@code JwtService} của identity). */
  public static final String ROLE_CLAIM = "role";

  private static final String SUBJECT_CLAIM = "sub";

  private final JwksKeyStore keyStore;
  private final GatewayProperties.Jwt properties;
  private final Clock clock;

  public JwtVerifier(JwksKeyStore keyStore, GatewayProperties properties, Clock clock) {
    this.keyStore = keyStore;
    this.properties = properties.jwt();
    this.clock = clock;
  }

  /**
   * Verify token và trả danh tính.
   *
   * @throws InvalidTokenException token không hợp lệ (thiếu/sai chữ ký, hết hạn, sai {@code
   *     iss}...)
   * @throws JwksUnavailableException không lấy được JWKS để verify
   */
  public AuthenticatedUser verify(String token) {
    SignedJWT jwt = parse(token);
    requireRs256(jwt);
    verifySignature(jwt);

    JWTClaimsSet claims = claims(jwt);
    requireNotExpired(claims);
    requireIssuer(claims);

    return new AuthenticatedUser(
        requireClaim(claims, SUBJECT_CLAIM), requireClaim(claims, ROLE_CLAIM));
  }

  private static SignedJWT parse(String token) {
    try {
      return SignedJWT.parse(token);
    } catch (ParseException e) {
      throw new InvalidTokenException("token không đọc được");
    }
  }

  private static void requireRs256(SignedJWT jwt) {
    if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
      throw new InvalidTokenException("thuật toán ký không được phép");
    }
  }

  private void verifySignature(SignedJWT jwt) {
    RSAPublicKey publicKey = keyStore.keyFor(jwt.getHeader().getKeyID());
    try {
      if (!jwt.verify(new RSASSAVerifier(publicKey))) {
        throw new InvalidTokenException("chữ ký không hợp lệ");
      }
    } catch (JOSEException e) {
      throw new InvalidTokenException("chữ ký không hợp lệ");
    }
  }

  private static JWTClaimsSet claims(SignedJWT jwt) {
    try {
      return jwt.getJWTClaimsSet();
    } catch (ParseException e) {
      throw new InvalidTokenException("claims không đọc được");
    }
  }

  private void requireNotExpired(JWTClaimsSet claims) {
    Instant now = clock.instant();
    Date expiresAt = claims.getExpirationTime();
    if (expiresAt == null) {
      throw new InvalidTokenException("token thiếu exp");
    }
    // Cho phép lệch đồng hồ nhỏ: máy phát và máy verify không bao giờ khớp tuyệt đối.
    if (expiresAt.toInstant().plus(properties.clockSkew()).isBefore(now)) {
      throw new InvalidTokenException("token hết hạn");
    }
    Date notBefore = claims.getNotBeforeTime();
    if (notBefore != null && notBefore.toInstant().minus(properties.clockSkew()).isAfter(now)) {
      throw new InvalidTokenException("token chưa có hiệu lực");
    }
  }

  private void requireIssuer(JWTClaimsSet claims) {
    if (!properties.issuer().equals(claims.getIssuer())) {
      throw new InvalidTokenException("iss không hợp lệ");
    }
  }

  private static String requireClaim(JWTClaimsSet claims, String name) {
    Object value = claims.getClaim(name);
    if (value == null || value.toString().isBlank()) {
      throw new InvalidTokenException("token thiếu claim " + name);
    }
    return value.toString();
  }
}
