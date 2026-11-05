package com.shoplab.identity.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoplab.identity.auth.InvalidRefreshTokenException;
import com.shoplab.identity.auth.jwt.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho vòng đời refresh token — repository giả lập bằng {@link Map}, không cần DB.
 *
 * <p>Kiểm chứng phần <b>logic khó</b>: rotation, reuse detection (revoke cả chuỗi), hết hạn,
 * logout. Luồng HTTP thật nằm ở {@code IdentityServiceApplicationIT}.
 */
class RefreshTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
  private static final UUID USER = UUID.randomUUID();

  private final Map<UUID, RefreshToken> store = new HashMap<>();
  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    service = serviceAt(NOW);
  }

  /** Service với clock cố định — repository mock luôn đọc/ghi cùng một {@link #store}. */
  private RefreshTokenService serviceAt(Instant instant) {
    RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    when(repository.save(any(RefreshToken.class)))
        .thenAnswer(
            invocation -> {
              RefreshToken token = invocation.getArgument(0);
              store.put(token.id(), token);
              return token;
            });
    when(repository.findById(any()))
        .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.<UUID>getArgument(0))));
    when(repository.findByTokenHash(anyString()))
        .thenAnswer(
            invocation -> {
              String hash = invocation.getArgument(0);
              return store.values().stream().filter(t -> t.tokenHash().equals(hash)).findFirst();
            });

    JwtProperties properties =
        new JwtProperties(
            "https://identity.shoplab.local",
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            "/tmp/khong-doc.pem",
            "/tmp/khong-doc.pub");
    return new RefreshTokenService(repository, properties, Clock.fixed(instant, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("issue: token thô khác hash, hash là SHA-256 hex 64 ký tự, hết hạn sau TTL 14 ngày")
  void issueShouldStoreHashOnly() {
    RefreshTokenService.IssuedRefresh issued = service.issue(USER);
    RefreshToken entity = issued.entity();

    assertThat(issued.rawToken()).isNotBlank().isNotEqualTo(entity.tokenHash());
    assertThat(entity.tokenHash()).hasSize(64).matches("[0-9a-f]{64}");
    assertThat(entity.tokenHash()).isEqualTo(RefreshTokenService.hash(issued.rawToken()));
    assertThat(entity.userId()).isEqualTo(USER);
    assertThat(entity.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
    assertThat(entity.isActive(NOW)).isTrue();
  }

  @Test
  @DisplayName("rotate: token cũ bị revoke + trỏ tới token mới; token mới dùng tiếp được")
  void rotateShouldRevokeOldAndIssueNew() {
    RefreshTokenService.IssuedRefresh first = service.issue(USER);

    RefreshTokenService.RotatedRefresh rotated = service.rotate(first.rawToken());

    RefreshToken old = store.get(first.entity().id());
    assertThat(old.revokedAt()).isEqualTo(NOW);
    assertThat(old.replacedBy()).isNotNull();
    assertThat(rotated.userId()).isEqualTo(USER);
    assertThat(rotated.rawToken()).isNotEqualTo(first.rawToken());

    // Token mới còn sống → rotate tiếp được (chuỗi hoạt động đúng).
    assertThat(service.rotate(rotated.rawToken()).rawToken()).isNotBlank();
  }

  @Test
  @DisplayName(
      "REUSE: dùng lại token đã rotate → 401 VÀ revoke cả chuỗi (token mới nhất cũng chết)")
  void reusingRotatedTokenShouldRevokeWholeChain() {
    RefreshTokenService.IssuedRefresh first = service.issue(USER);
    RefreshTokenService.RotatedRefresh second = service.rotate(first.rawToken());
    RefreshTokenService.RotatedRefresh third = service.rotate(second.rawToken());

    // Kẻ tấn công dùng lại token ĐẦU TIÊN (bản sao đã bị rotate) ⇒ bị phát hiện.
    assertThatThrownBy(() -> service.rotate(first.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);

    // Cả chuỗi bị thu hồi: token mới nhất cũng không dùng được nữa.
    assertThat(store.values()).allMatch(RefreshToken::isRevokedOrRotated);
    assertThatThrownBy(() -> service.rotate(third.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  @DisplayName("token không tồn tại → 401 (không lộ 'chưa từng có' hay 'đã dùng')")
  void unknownTokenShouldBeRejected() {
    assertThatThrownBy(() -> service.rotate("token-khong-ton-tai"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  @DisplayName("token hết hạn → 401 (chỉ từ chối, không revoke chuỗi)")
  void expiredTokenShouldBeRejected() {
    RefreshTokenService.IssuedRefresh issued = service.issue(USER);
    // Mốc thời gian: sau TTL 14 ngày + 1 giây.
    RefreshTokenService later = serviceAt(NOW.plus(Duration.ofDays(14)).plusSeconds(1));

    assertThatThrownBy(() -> later.rotate(issued.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  @DisplayName("logout (revoke): token không dùng lại được; gọi lại vẫn không lỗi (idempotent)")
  void revokeShouldInvalidateTokenIdempotently() {
    RefreshTokenService.IssuedRefresh issued = service.issue(USER);

    service.revoke(issued.rawToken());
    service.revoke(issued.rawToken());

    assertThat(store.get(issued.entity().id()).revokedAt()).isNotNull();
    assertThatThrownBy(() -> service.rotate(issued.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  @DisplayName("hash: tất định, khác nhau với token khác nhau, đúng 64 ký tự hex")
  void hashShouldBeStableAndCollisionFree() {
    assertThat(RefreshTokenService.hash("abc")).isEqualTo(RefreshTokenService.hash("abc"));
    assertThat(RefreshTokenService.hash("abc")).isNotEqualTo(RefreshTokenService.hash("abd"));
    assertThat(RefreshTokenService.hash("abc")).hasSize(64).matches("[0-9a-f]{64}");
  }
}
