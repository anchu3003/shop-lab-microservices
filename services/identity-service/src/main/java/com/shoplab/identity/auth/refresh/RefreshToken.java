package com.shoplab.identity.auth.refresh;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Một refresh token đã phát hành (SL-1.4).
 *
 * <p>Chỉ lưu <b>hash</b> của token ({@code token_hash}) — DB bị lộ cũng không dùng lại được token.
 * Token thật là chuỗi random 256-bit, chỉ client giữ.
 *
 * <p>Trạng thái là một <b>state machine</b>, không phải boolean "còn/hết":
 *
 * <ul>
 *   <li>active — {@code revoked_at == null}, {@code replaced_by == null}, chưa hết hạn
 *   <li>rotated — đã được thay bằng token mới ({@code replaced_by != null}); dùng lại token này là
 *       dấu hiệu bị đánh cắp
 *   <li>revoked — logout hoặc bị thu hồi do phát hiện reuse
 * </ul>
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, updatable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by")
  private UUID replacedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** JPA cần constructor rỗng; {@code protected} để domain không tạo entity rỗng. */
  protected RefreshToken() {}

  private RefreshToken(
      UUID id, UUID userId, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  /** Phát token mới cho user — hash đã được tính trước ở tầng service. */
  public static RefreshToken issue(UUID userId, String tokenHash, Instant now, Duration ttl) {
    return new RefreshToken(UUID.randomUUID(), userId, tokenHash, now, now.plus(ttl));
  }

  /** Còn dùng được? (chưa revoke, chưa rotate, chưa hết hạn) */
  public boolean isActive(Instant now) {
    return revokedAt == null && replacedBy == null && expiresAt.isAfter(now);
  }

  /**
   * Đã bị revoke hoặc đã rotate — dùng lại token ở trạng thái này nghĩa là có hai bên cùng giữ
   * token (bị đánh cắp) ⇒ phải revoke cả chuỗi.
   */
  public boolean isRevokedOrRotated() {
    return revokedAt != null || replacedBy != null;
  }

  /** Thu hồi (idempotent: giữ mốc revoke đầu tiên). */
  public void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = now;
    }
  }

  /** Đánh dấu token này đã được thay bằng {@code nextId} (rotation). */
  public void rotateTo(UUID nextId, Instant now) {
    this.revokedAt = now;
    this.replacedBy = nextId;
  }

  public UUID id() {
    return id;
  }

  public UUID userId() {
    return userId;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public Instant revokedAt() {
    return revokedAt;
  }

  public UUID replacedBy() {
    return replacedBy;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
