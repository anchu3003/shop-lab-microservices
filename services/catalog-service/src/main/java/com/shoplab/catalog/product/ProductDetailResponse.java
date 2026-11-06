package com.shoplab.catalog.product;

import com.shoplab.catalog.sku.Sku;
import com.shoplab.catalog.sku.SkuResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết một product kèm danh sách SKU — dùng cho {@code GET /v1/products/{id}}.
 *
 * <p>Tách khỏi {@link ProductResponse} (dùng cho list) có chủ ý: nếu list cũng trả SKU thì mỗi
 * trang 20 product sẽ thành 21 query (N+1) — đúng loại lỗi chỉ lộ ra khi tải cao. Trang chi tiết
 * chỉ có 1 product nên 1 query phụ là chấp nhận được.
 */
public record ProductDetailResponse(
    UUID id,
    UUID categoryId,
    String name,
    String description,
    BigDecimal price,
    long version,
    Instant createdAt,
    Instant updatedAt,
    List<SkuResponse> skus) {

  public static ProductDetailResponse from(Product product, List<Sku> skus) {
    return new ProductDetailResponse(
        product.id(),
        product.categoryId(),
        product.name(),
        product.description(),
        product.price(),
        product.version(),
        product.createdAt(),
        product.updatedAt(),
        skus.stream().map(SkuResponse::from).toList());
  }
}
