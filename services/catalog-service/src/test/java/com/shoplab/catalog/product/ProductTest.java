package com.shoplab.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test hành vi domain của {@link Product#archive(Instant)} (SL-2.11) — không cần Spring/DB. */
class ProductTest {

  private static final Instant NOW = Instant.parse("2026-11-07T08:00:00Z");

  private static Product newProduct() {
    return Product.create(UUID.randomUUID(), "iPhone", null, new BigDecimal("1.00"), NOW);
  }

  @Test
  @DisplayName("product mới tạo: status ACTIVE, chưa có deletedAt")
  void createdProductIsActive() {
    Product product = newProduct();

    assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
    assertThat(product.deletedAt()).isNull();
  }

  @Test
  @DisplayName("archive: status ARCHIVED + deletedAt + updatedAt = thời điểm xoá")
  void archiveMarksProductDeleted() {
    Product product = newProduct();
    Instant deletedAt = NOW.plusSeconds(3600);

    product.archive(deletedAt);

    assertThat(product.status()).isEqualTo(ProductStatus.ARCHIVED);
    assertThat(product.deletedAt()).isEqualTo(deletedAt);
    assertThat(product.updatedAt()).isEqualTo(deletedAt);
  }

  @Test
  @DisplayName("archive lần 2 không ghi đè deletedAt (idempotent — mốc audit không đổi)")
  void archiveIsIdempotent() {
    Product product = newProduct();
    Instant first = NOW.plusSeconds(60);

    product.archive(first);
    product.archive(first.plusSeconds(3600));

    assertThat(product.status()).isEqualTo(ProductStatus.ARCHIVED);
    assertThat(product.deletedAt()).isEqualTo(first);
    assertThat(product.updatedAt()).isEqualTo(first);
  }
}
