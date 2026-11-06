package com.shoplab.identity.auth.refresh;

import com.shoplab.identity.auth.InvalidRefreshTokenException;
import com.shoplab.identity.auth.jwt.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vòng đời refresh token: phát hành, rotate, thu hồi (SL-1.4).
 *
 * <p><b>Rotation</b>: mỗi lần refresh, token cũ bị đánh dấu rotate và trả token mới. Nhờ đó token
 * bị đánh cắp chỉ dùng được <b>một lần duy nhất</b> trước khi bị phát hiện.
 *
 * <p><b>Reuse detection</b>: nếu một token <i>đã rotate</i> lại được dùng ⇒ có hai bên cùng giữ
 * token (kẻ tấn công + người dùng thật) ⇒ revoke <b>cả chuỗi</b> (đi theo {@code replaced_by}),
 * buộc đăng nhập lại. Đây là lý do phải lưu chuỗi thay vì chỉ một cột "revoked".
 *
 * <p>Token thật là chuỗi random 256-bit; DB chỉ lưu SHA-256. Dùng SHA-256 (không BCrypt) vì token
 * đã có entropy 256-bit — BCrypt chỉ cần cho mật khẩu do con người chọn.
 */
@Service
public class RefreshTokenService {

  private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenService.class);

  /** 32 byte = 256 bit entropy — không thể brute force, không cần salt. */
  private static final int TOKEN_BYTES = 32;

  private static final SecureRandom RANDOM = new SecureRandom();

  private final RefreshTokenRepository tokens;
  private final Clock clock;
  private final Duration ttl;

  public RefreshTokenService(
      RefreshTokenRepository tokens, JwtProperties jwtProperties, Clock clock) {
    this.tokens = tokens;
    this.clock = clock;
    this.ttl = jwtProperties.refreshTokenTtl();
  }

  /** Token vừa phát hành: {@code rawToken} trả cho client, {@code entity} là bản ghi trong DB. */
  public record IssuedRefresh(String rawToken, RefreshToken entity) {}

  /** Kết quả rotate: token thô mới + user sở hữu (để cấp access token mới). */
  public record RotatedRefresh(UUID userId, String rawToken) {}

  /** Phát refresh token mới cho user (dùng ở login và ở mỗi lần rotate). */
  @Transactional
  public IssuedRefresh issue(UUID userId) {
    Instant now = clock.instant();
    String rawToken = generateRawToken();
    RefreshToken entity = RefreshToken.issue(userId, hash(rawToken), now, ttl);
    tokens.save(entity);
    return new IssuedRefresh(rawToken, entity);
  }

  /**
   * Đổi refresh token cũ lấy token mới (rotate).
   *
   * <p><b>{@code noRollbackFor}</b> là bắt buộc, không phải chi tiết vụn: khi phát hiện reuse,
   * method vừa <b>revoke cả chuỗi</b> vừa <b>throw</b> 401. Nếu để rollback mặc định thì
   * transaction huỷ luôn các lệnh revoke vừa ghi ⇒ kẻ tấn công vẫn dùng được token cũ, đúng thứ
   * mình vừa phát hiện. Đã gặp thật ở IT của SL-1.4 (`refresh2` vẫn trả 200 sau khi bị coi là
   * reuse).
   *
   * @throws InvalidRefreshTokenException token không tồn tại / hết hạn / đã dùng
   */
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public RotatedRefresh rotate(String rawToken) {
    Instant now = clock.instant();
    RefreshToken current =
        tokens.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);

    if (current.isRevokedOrRotated()) {
      // REUSE: token đã rotate/revoke mà vẫn được dùng ⇒ coi như bị đánh cắp.
      LOG.warn(
          "phát hiện refresh token dùng lại (tokenId={}, userId={}) — thu hồi cả chuỗi",
          current.id(),
          current.userId());
      revokeChain(current, now);
      throw new InvalidRefreshTokenException();
    }
    if (!current.expiresAt().isAfter(now)) {
      throw new InvalidRefreshTokenException();
    }

    IssuedRefresh next = issue(current.userId());
    current.rotateTo(next.entity().id(), now);
    tokens.save(current);
    return new RotatedRefresh(current.userId(), next.rawToken());
  }

  /**
   * Logout: thu hồi đúng refresh token client đang giữ (idempotent, không báo lỗi nếu không thấy).
   */
  @Transactional
  public void revoke(String rawToken) {
    Optional<RefreshToken> found = tokens.findByTokenHash(hash(rawToken));
    if (found.isEmpty()) {
      // Không phân biệt "không tồn tại" với "đã thu hồi" — tránh oracle cho kẻ dò token.
      return;
    }
    RefreshToken token = found.get();
    if (!token.isRevokedOrRotated()) {
      token.revoke(clock.instant());
      tokens.save(token);
    }
  }

  /**
   * Revoke token bắt đầu + toàn bộ token kế tiếp trong chuỗi rotation.
   *
   * <p>Phòng thủ vòng lặp vô hạn: dữ liệu hỏng (A→B→A) sẽ bị chặn bằng tập {@code seen}.
   */
  private void revokeChain(RefreshToken start, Instant now) {
    Set<UUID> seen = new HashSet<>();
    RefreshToken cursor = start;
    while (cursor != null && seen.add(cursor.id())) {
      cursor.revoke(now);
      tokens.save(cursor);
      UUID nextId = cursor.replacedBy();
      cursor = nextId == null ? null : tokens.findById(nextId).orElse(null);
    }
  }

  private static String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** SHA-256 hex (64 ký tự) — khớp {@code VARCHAR(64)} của cột {@code token_hash}. */
  static String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 là thuật toán bắt buộc của mọi JVM — không có nghĩa là môi trường hỏng.
      throw new IllegalStateException("JVM không hỗ trợ SHA-256", e);
    }
  }
}
