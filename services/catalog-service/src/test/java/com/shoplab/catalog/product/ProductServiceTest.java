package com.shoplab.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.category.CategoryRepository;
import com.shoplab.catalog.sku.SkuRepository;
import com.shoplab.commons.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  private final ProductRepository products = mock(ProductRepository.class);
  private final CategoryRepository categories = mock(CategoryRepository.class);
  private final SkuRepository skus = mock(SkuRepository.class);

  private final ProductService service =
      new ProductService(products, categories, skus, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("tạo product: trim tên, description rỗng → null, giá giữ nguyên BigDecimal")
  void createShouldNormalizeInput() {
    UUID categoryId = UUID.randomUUID();
    when(categories.existsById(categoryId)).thenReturn(true);
    when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ProductResponse created =
        service.create(
            new CreateProductRequest(
                categoryId, "  iPhone 15  ", "   ", new BigDecimal("25990000.00")));

    assertThat(created.name()).isEqualTo("iPhone 15");
    assertThat(created.description()).isNull();
    assertThat(created.price()).isEqualByComparingTo("25990000.00");
    assertThat(created.createdAt()).isEqualTo(NOW);
    assertThat(created.version()).isZero();
  }

  @Test
  @DisplayName("category không tồn tại → 404, KHÔNG ghi product")
  void createShouldRejectUnknownCategory() {
    UUID categoryId = UUID.randomUUID();
    when(categories.existsById(categoryId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateProductRequest(categoryId, "X", null, new BigDecimal("1.00"))))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Category không tồn tại");

    verify(products, never()).save(any());
  }

  @Test
  @DisplayName("findById không thấy → 404")
  void findByIdShouldThrowWhenMissing() {
    UUID id = UUID.randomUUID();
    when(products.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("update ghi đè field sửa được + đụng updatedAt")
  void updateShouldOverwriteMutableFields() {
    UUID id = UUID.randomUUID();
    Product existing =
        Product.create(
            UUID.randomUUID(), "Cũ", "mô tả cũ", new BigDecimal("1000.00"), NOW.minusSeconds(60));
    when(products.findById(id)).thenReturn(Optional.of(existing));
    // Service flush để version kịp tăng trước khi map DTO (xem ProductService.update).
    when(products.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse updated =
        service.update(id, new UpdateProductRequest("Mới", "  ", new BigDecimal("2000.00")));

    assertThat(updated.name()).isEqualTo("Mới");
    assertThat(updated.description()).isNull();
    assertThat(updated.price()).isEqualByComparingTo("2000.00");
    assertThat(updated.updatedAt()).isEqualTo(NOW);
    assertThat(updated.createdAt()).isEqualTo(NOW.minusSeconds(60));
  }

  @Test
  @DisplayName("delete không thấy → 404, KHÔNG gọi delete")
  void deleteShouldThrowWhenMissing() {
    UUID id = UUID.randomUUID();
    when(products.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    verify(products, never()).delete(any());
  }

  @Test
  @DisplayName("delete có thật → gọi delete đúng entity")
  void deleteShouldRemoveExistingProduct() {
    UUID id = UUID.randomUUID();
    Product existing = Product.create(UUID.randomUUID(), "X", null, new BigDecimal("1.00"), NOW);
    when(products.findById(id)).thenReturn(Optional.of(existing));

    service.delete(id);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(products).delete(captor.capture());
    assertThat(captor.getValue()).isSameAs(existing);
  }
}
