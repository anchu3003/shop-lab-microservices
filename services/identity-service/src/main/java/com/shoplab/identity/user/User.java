package com.shoplab.identity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Người dùng của hệ thống.
 *
 * <p>Thiết kế có chủ ý:
 *
 * <ul>
 *   <li>ID là {@link UUID} do service sinh — không lộ số lượng user, thuận lợi khi sharding sau
 *       này.
 *   <li>Constructor rỗng là {@code protected}: JPA cần, nhưng domain không nên tạo entity rỗng →
 *       chỉ có factory {@link #register} là đường vào hợp lệ.
 *   <li>Không có setter công khai: sửa trạng thái user phải qua method có tên nghiệp vụ.
 * </ul>
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA cần constructor rỗng; để {@code protected} để domain không vô tình dùng. */
  protected User() {}

  private User(
      UUID id, String email, String passwordHash, String displayName, Role role, Instant now) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = role;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Tạo user mới.
   *
   * @param email đã được normalize (chữ thường) ở tầng service — entity không tự đoán chuẩn hoá
   * @param passwordHash hash BCrypt, KHÔNG BAO GIỜ là mật khẩu gốc
   */
  public static User register(
      String email, String passwordHash, String displayName, Role role, Instant now) {
    return new User(UUID.randomUUID(), email, passwordHash, displayName, role, now);
  }

  public UUID id() {
    return id;
  }

  public String email() {
    return email;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public String displayName() {
    return displayName;
  }

  public Role role() {
    return role;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
