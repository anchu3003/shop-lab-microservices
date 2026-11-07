package com.shoplab.catalog.sku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.product.ProductRepository;
import com.shoplab.commons.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkuServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-06T09:00:00Z");

  private final SkuRepository skus = mock(SkuRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);

  private final SkuService service =
      new SkuService(skus, products, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("tạo SKU: trim code, giữ priceOverride null nghĩa là dùng giá product")
  void createShouldNormalizeCode() {
    UUID productId = UUID.randomUUID();
    when(products.existsByIdAndDeletedAtIsNull(productId)).thenReturn(true);
    when(skus.existsByCode("AT-DO-M")).thenReturn(false);
    when(skus.save(any(Sku.class))).thenAnswer(inv -> inv.getArgument(0));

    SkuResponse created = service.create(productId, new CreateSkuRequest("  AT-DO-M  ", null, 10));

    assertThat(created.code()).isEqualTo("AT-DO-M");
    assertThat(created.priceOverride()).isNull();
    assertThat(created.stockHint()).isEqualTo(10);
    assertThat(created.status()).isEqualTo(SkuStatus.ACTIVE);
    assertThat(created.createdAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("product đã xoá mềm (existsByIdAndDeletedAtIsNull = false) → 404, KHÔNG ghi SKU")
  void createShouldRejectArchivedProduct() {
    UUID productId = UUID.randomUUID();
    // Soft delete: row vẫn ở DB nhưng deleted_at đã set ⇒ mọi đường API coi như không tồn tại.
    when(products.existsByIdAndDeletedAtIsNull(productId)).thenReturn(false);

    assertThatThrownBy(() -> service.create(productId, new CreateSkuRequest("X-1", null, null)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Product không tồn tại");

    verify(skus, never()).save(any());
  }

  @Test
  @DisplayName("product không tồn tại → 404, KHÔNG ghi SKU")
  void createShouldRejectUnknownProduct() {
    UUID productId = UUID.randomUUID();
    when(products.existsByIdAndDeletedAtIsNull(productId)).thenReturn(false);

    assertThatThrownBy(() -> service.create(productId, new CreateSkuRequest("X", null, null)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Product không tồn tại");

    verify(skus, never()).save(any());
  }

  @Test
  @DisplayName("mã SKU đã dùng → 409 DuplicateSkuException")
  void createShouldRejectDuplicateCode() {
    UUID productId = UUID.randomUUID();
    when(products.existsByIdAndDeletedAtIsNull(productId)).thenReturn(true);
    when(skus.existsByCode("TRUNG")).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.create(
                    productId, new CreateSkuRequest("TRUNG", new BigDecimal("1000.00"), 1)))
        .isInstanceOf(DuplicateSkuException.class)
        .hasMessageContaining("TRUNG");

    verify(skus, never()).save(any());
  }

  @Test
  @DisplayName("findByProduct map đúng sang DTO, giữ thứ tự repository trả về")
  void findByProductShouldMapAll() {
    UUID productId = UUID.randomUUID();
    Sku first = Sku.create(productId, "A-1", null, null, NOW);
    Sku second = Sku.create(productId, "A-2", new BigDecimal("2000.00"), 5, NOW);
    when(skus.findByProductIdOrderByCodeAsc(productId)).thenReturn(List.of(first, second));

    List<SkuResponse> found = service.findByProduct(productId);

    assertThat(found).hasSize(2);
    assertThat(found.get(0).code()).isEqualTo("A-1");
    assertThat(found.get(1).priceOverride()).isEqualByComparingTo("2000.00");
  }
}
