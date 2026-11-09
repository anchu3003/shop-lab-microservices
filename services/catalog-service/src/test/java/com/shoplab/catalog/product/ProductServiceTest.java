package com.shoplab.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.category.CategoryRepository;
import com.shoplab.catalog.outbox.CatalogOutbox;
import com.shoplab.catalog.sku.SkuRepository;
import com.shoplab.catalog.sku.SkuStatus;
import com.shoplab.commons.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  private final ProductRepository products = mock(ProductRepository.class);
  private final CategoryRepository categories = mock(CategoryRepository.class);
  private final SkuRepository skus = mock(SkuRepository.class);
  private final CatalogOutbox outbox = mock(CatalogOutbox.class);

  private final ProductService service =
      new ProductService(products, categories, skus, outbox, Clock.fixed(NOW, ZoneOffset.UTC));

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
    // SL-3.6: event phải được ghi cùng lần commit với INSERT product. Unit test chỉ kiểm được
    // "đã gọi đúng 1 lần" — bất biến transaction thật được kiểm ở IT (rollback thì outbox cũng
    // mất).
    verify(outbox).productUpdated(any(Product.class));
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
  @DisplayName("findDetail không thấy → 404")
  void findDetailShouldThrowWhenMissing() {
    UUID id = UUID.randomUUID();
    when(products.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findDetail(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("update ghi đè field sửa được + đụng updatedAt")
  void updateShouldOverwriteMutableFields() {
    UUID id = UUID.randomUUID();
    Product existing =
        Product.create(
            UUID.randomUUID(), "Cũ", "mô tả cũ", new BigDecimal("1000.00"), NOW.minusSeconds(60));
    when(products.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
    // Service flush để version kịp tăng trước khi map DTO (xem ProductService.update).
    when(products.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse updated =
        service.update(id, new UpdateProductRequest("Mới", "  ", new BigDecimal("2000.00")));

    assertThat(updated.name()).isEqualTo("Mới");
    assertThat(updated.description()).isNull();
    assertThat(updated.price()).isEqualByComparingTo("2000.00");
    assertThat(updated.updatedAt()).isEqualTo(NOW);
    assertThat(updated.createdAt()).isEqualTo(NOW.minusSeconds(60));
    assertThat(updated.status()).isEqualTo(ProductStatus.ACTIVE);
    // Event mang dữ liệu SAU flush (`version`/`updatedAt` mới) — gọi trước `saveAndFlush` là
    // consumer nhận giá trị cũ.
    verify(outbox).productUpdated(any(Product.class));
  }

  @Test
  @DisplayName("update product đã xoá mềm → 404 (findByIdAndDeletedAtIsNull không trả row)")
  void updateShouldRejectArchivedProduct() {
    UUID id = UUID.randomUUID();
    when(products.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(id, new UpdateProductRequest("Mới", null, new BigDecimal("1.00"))))
        .isInstanceOf(NotFoundException.class);

    verify(products, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("delete không thấy → 404, KHÔNG archive và KHÔNG đụng SKU")
  void deleteShouldThrowWhenMissing() {
    UUID id = UUID.randomUUID();
    when(products.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    verify(products, never()).delete(any());
    verify(skus, never()).updateStatusByProductId(any(), any());
    // Product không tồn tại ⇒ không có thay đổi nào ⇒ KHÔNG được phát event (event cho một việc
    // không xảy ra là thứ khiến index và catalog lệch nhau theo chiều không thể đoán).
    verify(outbox, never()).productArchived(any(), any());
    verify(outbox, never()).productUpdated(any());
  }

  @Test
  @DisplayName("delete = XOÁ MỀM: product ARCHIVED + deletedAt, SKU archive theo — KHÔNG xoá row")
  void deleteShouldArchiveProductAndSkus() {
    UUID id = UUID.randomUUID();
    Product existing = Product.create(UUID.randomUUID(), "X", null, new BigDecimal("1.00"), NOW);
    when(products.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
    when(skus.updateStatusByProductId(id, SkuStatus.ARCHIVED)).thenReturn(2);

    service.delete(id);

    assertThat(existing.status()).isEqualTo(ProductStatus.ARCHIVED);
    assertThat(existing.deletedAt()).isEqualTo(NOW);
    assertThat(existing.updatedAt()).isEqualTo(NOW);
    // Hard delete là thứ đã bị bỏ (TD-12): xoá row sẽ mất luôn SKU do ON DELETE CASCADE.
    verify(products, never()).delete(any());
    verify(skus).updateStatusByProductId(id, SkuStatus.ARCHIVED);
    // Mốc thời gian trong event phải BẰNG `deleted_at` — một sự kiện, một mốc (không đọc lại đồng
    // hồ ở chỗ thứ hai rồi lệch vài ms so với dữ liệu đã ghi).
    verify(outbox).productArchived(id, NOW);
  }
}
