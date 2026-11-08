package com.shoplab.identity.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.shoplab.identity.user.Role;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Ký và công bố khoá JWT (RS256).
 *
 * <p><b>Vì sao RS256 (bất đối xứng) chứ không HS256 (chia sẻ khoá)</b>: service verify (gateway ở
 * SL-1.2, catalog ở SL-1.5) chỉ cần <b>public key</b> lấy qua JWKS. Với HS256, mọi service verify
 * đều phải giữ khoá ký — nghĩa là một service bị lộ = cả hệ thống bị giả token. Đây là lý do thực
 * tế để trả giá thêm vài dòng code.
 *
 * <p><b>{@code kid}</b> lấy từ JWK thumbprint (RFC 7638): tự tính từ chính public key, nên không
 * phải đặt tên khoá thủ công và không bao giờ lệch giữa header token và JWKS.
 */
@Service
public class JwtService {

  /** Tên claim chứa vai trò — gateway/catalog đọc claim này để phân quyền. */
  public static final String ROLE_CLAIM = "role";

  /**
   * Role của <b>service</b> (SL-3.11) — không phải role của người dùng, và **không** nằm trong enum
   * {@link Role} (bảng `users` chỉ có role người). Nhờ vậy không tồn tại đường nào để một user có
   * `ROLE_SERVICE`; ngược lại, service token không có `sub` là userId nên endpoint nào lỡ coi `sub`
   * là user sẽ **nổ ngay**.
   */
  public static final String SERVICE_ROLE = "SERVICE";

  /** Tiền tố `sub` của service token: {@code svc:<clientId>}. */
  public static final String SERVICE_SUBJECT_PREFIX = "svc:";

  private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
  private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
  private static final String KEY_HINT =
      "Sinh khoá dev: bash tools/gen-dev-keys.sh (file bị .gitignore — KHÔNG commit, AGENTS.md §9).";

  private final JwtProperties properties;
  private final RSAPrivateKey privateKey;
  private final RSAPublicKey publicKey;
  private final String keyId;
  private final NimbusJwtEncoder encoder;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.privateKey = readPrivateKey(properties.privateKeyPath());
    this.publicKey = readPublicKey(properties.publicKeyPath());
    this.keyId = computeKeyId(publicKey);
    this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey())));
  }

  /**
   * Phát access token cho user.
   *
   * @param issuedAt thời điểm phát — truyền vào thay vì gọi {@code Instant.now()} bên trong để test
   *     kiểm soát được thời gian
   */
  public IssuedToken issueAccessToken(UUID userId, Role role, Instant issuedAt) {
    Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim(ROLE_CLAIM, role.name())
            .build();

    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(value, expiresAt, properties.accessTokenTtl().toSeconds());
  }

  /**
   * Phát **service token** (SL-3.11) — token của máy, không của người.
   *
   * <p>Khác access token người ở 4 điểm, tất cả đều có chủ ý:
   *
   * <ul>
   *   <li>{@code sub = svc:<clientId>} — không phải UUID ⇒ dùng nhầm chỗ là nổ, không âm thầm ghi
   *       sai dữ liệu.
   *   <li>{@code role = SERVICE} — service đích phân biệt được "máy" với "người" (dùng cho kiểm
   *       {@code aud}).
   *   <li>{@code aud = [audience]} — token phát cho `inventory` **không** dùng được ở `promotion`.
   *   <li>{@code exp} ngắn (5 phút, cấu hình riêng) — không có cơ chế thu hồi, nên cửa sổ rò rỉ
   *       phải nhỏ.
   * </ul>
   *
   * @param clientId service xin token (đã xác thực secret ở tầng service)
   * @param audience service đích (đã kiểm allowlist)
   * @param issuedAt truyền vào để test kiểm soát thời gian
   */
  public IssuedToken issueServiceToken(
      String clientId, String audience, Duration ttl, Instant issuedAt) {
    Instant expiresAt = issuedAt.plus(ttl);

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(SERVICE_SUBJECT_PREFIX + clientId)
            .audience(List.of(audience))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim(ROLE_CLAIM, SERVICE_ROLE)
            .build();

    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(value, expiresAt, ttl.toSeconds());
  }

  /**
   * JWKS để service khác verify — <b>chỉ public key</b>, không bao giờ lộ private key.
   *
   * <p>Trả {@code Map} đúng cấu trúc JWK Set (RFC 7517) để Spring serialize thành JSON.
   */
  public Map<String, Object> jwks() {
    return new JWKSet(publicJwk()).toJSONObject();
  }

  /** {@code kid} hiện hành — dùng cho test/verify. */
  public String keyId() {
    return keyId;
  }

  /**
   * Public key để service này <b>tự verify</b> token do chính nó phát hành (SL-1.4: identity vừa là
   * issuer vừa là resource server — defense in depth, gateway cũng verify).
   *
   * <p>Chỉ trả public key; private key không bao giờ rời khỏi {@link #signingKey()}.
   */
  public RSAPublicKey publicKey() {
    return publicKey;
  }

  /** Thời gian sống của access token — controller trả về cho client. */
  public Duration accessTokenTtl() {
    return properties.accessTokenTtl();
  }

  private RSAKey signingKey() {
    return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
  }

  private RSAKey publicJwk() {
    return new RSAKey.Builder(publicKey).keyID(keyId).build();
  }

  private static String computeKeyId(RSAPublicKey publicKey) {
    try {
      return new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
    } catch (JOSEException e) {
      throw new JwtKeyException("Không tính được kid từ public key.", e);
    }
  }

  private static RSAPrivateKey readPrivateKey(String path) {
    byte[] der = decodePem(path, PRIVATE_KEY_HEADER, "private key");
    try {
      return (RSAPrivateKey)
          KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    } catch (GeneralSecurityException | RuntimeException e) {
      throw new JwtKeyException(
          "Không đọc được private key PKCS#8 từ '" + path + "'. " + KEY_HINT, e);
    }
  }

  private static RSAPublicKey readPublicKey(String path) {
    byte[] der = decodePem(path, PUBLIC_KEY_HEADER, "public key");
    try {
      return (RSAPublicKey)
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    } catch (GeneralSecurityException | RuntimeException e) {
      throw new JwtKeyException("Không đọc được public key SPKI từ '" + path + "'. " + KEY_HINT, e);
    }
  }

  /** Đọc PEM → DER. Lỗi phải nêu rõ đường dẫn + cách sửa, không để NullPointer khó hiểu. */
  private static byte[] decodePem(String path, String expectedHeader, String what) {
    String pem;
    try {
      pem = Files.readString(Path.of(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new JwtKeyException(
          "Thiếu " + what + ": không đọc được file '" + path + "'. " + KEY_HINT, e);
    }
    if (!pem.contains(expectedHeader)) {
      throw new JwtKeyException(
          "File '" + path + "' không phải PEM dạng '" + expectedHeader + "'. " + KEY_HINT);
    }
    String base64 = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
    try {
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new JwtKeyException("File '" + path + "' không phải base64 hợp lệ.", e);
    }
  }
}
