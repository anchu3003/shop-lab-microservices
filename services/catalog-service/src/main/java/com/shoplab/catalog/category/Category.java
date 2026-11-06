package com.shoplab.catalog.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Nhóm sản phẩm (vd "Điện thoại", "Áo thun").
 *
 * <p>Thiết kế có chủ ý:
 *
 * <ul>
 *   <li>ID là {@link UUID} do service sinh — không lộ số lượng, thuận lợi khi sharding sau này.
 *   <li>Constructor rỗng là {@code protected}: JPA cần, nhưng domain không nên tạo entity rỗng.
 *   <li>{@code slug} là định danh đọc được, do tầng service sinh từ {@code name} — entity không tự
 *       đoán chuẩn hoá.
 * </ul>
 */
@Entity
@Table(name = "category")
public class Category {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA cần constructor rỗng; để {@code protected} để domain không vô tình dùng. */
  protected Category() {}

  private Category(UUID id, String name, String slug, Instant now) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Category create(String name, String slug, Instant now) {
    return new Category(UUID.randomUUID(), name, slug, now);
  }

  /** Đổi tên — không đổi slug (slug đã là định danh công khai, đổi sẽ làm chết link cũ). */
  public void rename(String name, Instant now) {
    this.name = name;
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String slug() {
    return slug;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
