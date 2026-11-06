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
    return service(instant, Duration.ZERO);
  }

  /** Service với grace window (SL-1.18) — {@link Duration#ZERO} = tắt grace (hành vi strict). */
  private RefreshTokenService service(Instant instant, Duration grace) {
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
            grace,
            "/tmp/khong-doc.pem",
            "/tmp/khong-doc.pub");
    return new RefreshTokenService(repository, properties, Clock.fixed(instant, ZoneOffset.UTC));
  }

  /** Tìm entity trong store theo token thô (RotatedRefresh chỉ trả token thô + userId). */
  private RefreshToken entityOf(String rawToken) {
    String hash = RefreshTokenService.hash(rawToken);
    return store.values().stream()
        .filter(token -> token.tokenHash().equals(hash))
        .findFirst()
        .orElseThrow();
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

  // ---------------------------------------------------------------------------
  // SL-1.18: grace window cho reuse detection
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("grace: dùng lại token vừa rotate trong 30s → cấp token mới, KHÔNG revoke chuỗi")
  void reuseWithinGraceShouldIssueNewTokenWithoutRevoking() {
    RefreshTokenService withGrace = service(NOW, Duration.ofSeconds(30));
    RefreshTokenService.IssuedRefresh first = withGrace.issue(USER);
    RefreshTokenService.RotatedRefresh second = withGrace.rotate(first.rawToken());

    // Tab B gửi lại token CŨ (đã rotate 0 giây trước) — đúng tình huống đua 2 tab.
    RefreshTokenService.RotatedRefresh recovered = withGrace.rotate(first.rawToken());

    assertThat(recovered.rawToken()).isNotBlank().isNotEqualTo(second.rawToken());
    // Không revoke: token cuối chuỗi được nối tiếp, không bị đánh dấu chết.
    RefreshToken latest = entityOf(second.rawToken());
    assertThat(latest.replacedBy()).isNotNull();
    assertThat(latest.revokedAt()).isEqualTo(NOW);

    // Token vừa cấp dùng được, và token cũ vẫn còn trong grace (không bị revoke hàng loạt).
    assertThat(withGrace.rotate(recovered.rawToken()).rawToken()).isNotBlank();
  }

  @Test
  @DisplayName("grace: dùng lại token đã rotate QUÁ 30s → 401 + revoke cả chuỗi (như cũ)")
  void reuseAfterGraceShouldStillRevokeChain() {
    RefreshTokenService withGrace = service(NOW, Duration.ofSeconds(30));
    RefreshTokenService.IssuedRefresh first = withGrace.issue(USER);
    RefreshTokenService.RotatedRefresh second = withGrace.rotate(first.rawToken());

    // 31 giây sau mới dùng lại token đầu ⇒ ngoài grace ⇒ coi là bị đánh cắp.
    RefreshTokenService later = service(NOW.plusSeconds(31), Duration.ofSeconds(30));
    assertThatThrownBy(() -> later.rotate(first.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);

    assertThat(entityOf(second.rawToken()).revokedAt()).isNotNull();
    assertThat(entityOf(second.rawToken()).replacedBy()).isNull();
  }

  @Test
  @DisplayName("grace KHÔNG hồi sinh phiên đã logout: revoke rồi dùng lại token cũ → 401")
  void graceShouldNotResurrectLoggedOutSession() {
    RefreshTokenService withGrace = service(NOW, Duration.ofSeconds(30));
    RefreshTokenService.IssuedRefresh first = withGrace.issue(USER);
    RefreshTokenService.RotatedRefresh second = withGrace.rotate(first.rawToken());
    withGrace.revoke(second.rawToken()); // logout

    assertThatThrownBy(() -> withGrace.rotate(first.rawToken()))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  @DisplayName("grace = 0s → tắt grace, hành vi strict (dùng lại là revoke ngay)")
  void zeroGraceKeepsStrictBehaviour() {
    RefreshTokenService strict = service(NOW, Duration.ZERO);
    RefreshTokenService.IssuedRefresh first = strict.issue(USER);
    strict.rotate(first.rawToken());

    assertThatThrownBy(() -> strict.rotate(first.rawToken()))
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
