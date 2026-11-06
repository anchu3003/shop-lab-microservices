package com.shoplab.catalog.sku;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SKU — biến thể bán thật của một {@link com.shoplab.catalog.product.Product} (màu/size/...).
 *
 * <p>Thiết kế có chủ ý:
 *
 * <ul>
 *   <li>{@code code} là mã duy nhất toàn hệ (kho/vận chuyển dùng) — unique index ở DB.
 *   <li>{@code priceOverride} {@code null} nghĩa là <b>dùng giá của product</b>. Không copy giá vào
 *       SKU: hai nguồn sự thật cho cùng một con số là cách chắc chắn nhất để chúng lệch nhau.
 *   <li>{@code stockHint} chỉ là gợi ý hiển thị. Tồn kho thật + reserve/commit thuộc
 *       inventory-service (Phase 2) — cố ý không dựng nguồn sự thật thứ hai ở đây.
 * </ul>
 */
@Entity
@Table(name = "sku")
public class Sku {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "product_id", nullable = false, updatable = false)
  private UUID productId;

  @Column(name = "code", nullable = false, updatable = false)
  private String code;

  @Column(name = "price_override", precision = 12, scale = 2)
  private BigDecimal priceOverride;

  @Column(name = "stock_hint")
  private Integer stockHint;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** JPA cần constructor rỗng; để {@code protected} để domain không vô tình dùng. */
  protected Sku() {}

  private Sku(
      UUID id,
      UUID productId,
      String code,
      BigDecimal priceOverride,
      Integer stockHint,
      Instant now) {
    this.id = id;
    this.productId = productId;
    this.code = code;
    this.priceOverride = priceOverride;
    this.stockHint = stockHint;
    this.createdAt = now;
  }

  public static Sku create(
      UUID productId, String code, BigDecimal priceOverride, Integer stockHint, Instant now) {
    return new Sku(UUID.randomUUID(), productId, code, priceOverride, stockHint, now);
  }

  public UUID id() {
    return id;
  }

  public UUID productId() {
    return productId;
  }

  public String code() {
    return code;
  }

  public BigDecimal priceOverride() {
    return priceOverride;
  }

  public Integer stockHint() {
    return stockHint;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
