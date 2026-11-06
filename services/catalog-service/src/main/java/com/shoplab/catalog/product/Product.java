package com.shoplab.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sản phẩm thuộc một {@link com.shoplab.catalog.category.Category}.
 *
 * <p>Thiết kế có chủ ý:
 *
 * <ul>
 *   <li>{@code price} là {@link BigDecimal} + cột {@code NUMERIC(12,2)} — <b>không</b> dùng {@code
 *       double}: số nhị phân không biểu diễn chính xác 0.1 nên cộng/trừ tiền sẽ sai số, thành bug
 *       thật khi Phase 2 tính tổng đơn.
 *   <li>{@code categoryId} lưu UUID trần (không map quan hệ {@code @ManyToOne}): tránh lazy-load
 *       ngoài transaction (đã tắt open-in-view) và tránh kéo cả object khi chỉ cần id.
 *   <li>{@code @Version} = optimistic locking: hai request sửa cùng lúc thì request sau nhận lỗi
 *       thay vì âm thầm ghi đè. Rẻ khi thêm bây giờ, đắt nếu phải sửa dữ liệu về sau.
 * </ul>
 */
@Entity
@Table(name = "product")
public class Product {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA cần constructor rỗng; để {@code protected} để domain không vô tình dùng. */
  protected Product() {}

  private Product(
      UUID id, UUID categoryId, String name, String description, BigDecimal price, Instant now) {
    this.id = id;
    this.categoryId = categoryId;
    this.name = name;
    this.description = description;
    this.price = price;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Product create(
      UUID categoryId, String name, String description, BigDecimal price, Instant now) {
    return new Product(UUID.randomUUID(), categoryId, name, description, price, now);
  }

  /** Sửa nội dung sản phẩm (PUT = thay toàn bộ field sửa được). */
  public void update(String name, String description, BigDecimal price, Instant now) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID categoryId() {
    return categoryId;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public BigDecimal price() {
    return price;
  }

  public long version() {
    return version;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
