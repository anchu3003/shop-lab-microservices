package com.shoplab.gateway.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.shoplab.gateway.config.GatewayProperties;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Giữ public key của identity-service, lấy từ JWKS.
 *
 * <p><b>Vì sao gateway không giữ khoá ký</b>: gateway chỉ cần verify. Nếu nó giữ private key thì
 * một lỗ hổng ở gateway = giả được token cho cả hệ thống. Đây chính là lý do chọn RS256 thay vì
 * HS256 (SL-1.1).
 *
 * <p><b>Cache + refetch khi gặp {@code kid} lạ</b>: JWKS được cache theo TTL để không gọi identity
 * mỗi request (1 request token = 1 lần HTTP nữa thì gateway thành nút thắt ngay). Nhưng khi
 * identity rotate khoá, token mới sẽ mang {@code kid} chưa có trong cache → lúc đó <b>refetch ngay
 * 1 lần</b>, nhờ vậy rotate khoá không cần restart gateway (và cũng không cần chờ hết TTL).
 */
@Component
public class JwksKeyStore {

  private static final Logger LOG = LoggerFactory.getLogger(JwksKeyStore.class);

  private final RestClient restClient;
  private final GatewayProperties.Jwt properties;
  private final Clock clock;
  private final Object refreshLock = new Object();

  private volatile Map<String, RSAPublicKey> keys = Map.of();
  private volatile Instant loadedAt = Instant.EPOCH;

  public JwksKeyStore(RestClient gatewayRestClient, GatewayProperties properties, Clock clock) {
    this.restClient = gatewayRestClient;
    this.properties = properties.jwt();
    this.clock = clock;
  }

  /**
   * Public key theo {@code kid} của token.
   *
   * @throws InvalidTokenException khi token không có {@code kid} hoặc {@code kid} không tồn tại
   *     trong JWKS (kể cả sau khi đã refetch)
   * @throws JwksUnavailableException khi không lấy được JWKS từ identity
   */
  public RSAPublicKey keyFor(String keyId) {
    if (keyId == null || keyId.isBlank()) {
      throw new InvalidTokenException("token thiếu kid");
    }
    RSAPublicKey cached = currentKeys().get(keyId);
    if (cached != null) {
      return cached;
    }
    // kid lạ = dấu hiệu khoá vừa rotate → nạp lại JWKS (bỏ qua TTL) rồi tra lại.
    RSAPublicKey refreshed = refreshAfterUnknownKid().get(keyId);
    if (refreshed == null) {
      throw new InvalidTokenException("kid không có trong JWKS");
    }
    return refreshed;
  }

  /** Số khoá đang giữ — dùng cho log lúc boot và cho test. */
  public int cachedKeyCount() {
    return currentKeys().size();
  }

  private Map<String, RSAPublicKey> currentKeys() {
    if (clock.instant().isBefore(loadedAt.plus(properties.jwksCacheTtl()))) {
      return keys;
    }
    return refresh();
  }

  private Map<String, RSAPublicKey> refresh() {
    // Một lock cho cả refresh: nhiều request cùng lúc gặp cache hết hạn thì chỉ 1 request đi lấy
    // JWKS, tránh "cache stampede" đúng lúc identity đang bận (bài học sẽ gặp lại ở Phase 4).
    synchronized (refreshLock) {
      // Có thể thread khác vừa refresh xong trong lúc ta chờ lock.
      if (clock.instant().isBefore(loadedAt.plus(properties.jwksCacheTtl()))) {
        return keys;
      }
      return load();
    }
  }

  /**
   * Nạp lại JWKS <b>bỏ qua TTL</b> — dùng khi gặp {@code kid} lạ (khoá vừa rotate).
   *
   * <p>Chặn dội bằng {@code minRefreshInterval} tính từ <b>lần nạp gần nhất</b> ({@code loadedAt}):
   * nếu không, kẻ tấn công chỉ cần gửi token với {@code kid} rác liên tục là biến mỗi request thành
   * một lần gọi identity — tự làm nghẽn chính mình. Dùng chung mốc {@code loadedAt} với đường TTL
   * để tránh nạp hai lần liền nhau khi cache vừa mới nạp xong mà gặp kid lạ.
   */
  private Map<String, RSAPublicKey> refreshAfterUnknownKid() {
    synchronized (refreshLock) {
      if (clock.instant().isBefore(loadedAt.plus(properties.minRefreshInterval()))) {
        return keys;
      }
      return load();
    }
  }

  private Map<String, RSAPublicKey> load() {
    Map<String, RSAPublicKey> parsed = fetch();
    this.keys = Map.copyOf(parsed);
    this.loadedAt = clock.instant();
    LOG.info("jwks: nạp {} khoá từ {}", parsed.size(), properties.jwksUri());
    return this.keys;
  }

  private Map<String, RSAPublicKey> fetch() {
    try {
      String json = restClient.get().uri(properties.jwksUri()).retrieve().body(String.class);
      if (json == null || json.isBlank()) {
        throw new JwksUnavailableException("JWKS trả về rỗng: " + properties.jwksUri());
      }
      Map<String, RSAPublicKey> parsed = new LinkedHashMap<>();
      for (JWK jwk : JWKSet.parse(json).getKeys()) {
        if (jwk instanceof RSAKey rsaKey && rsaKey.getKeyID() != null) {
          parsed.put(rsaKey.getKeyID(), rsaKey.toRSAPublicKey());
        }
      }
      if (parsed.isEmpty()) {
        throw new JwksUnavailableException("JWKS không có khoá RSA nào: " + properties.jwksUri());
      }
      return parsed;
    } catch (RestClientException | ParseException | JOSEException e) {
      throw new JwksUnavailableException("Không lấy được JWKS từ " + properties.jwksUri(), e);
    }
  }
}
