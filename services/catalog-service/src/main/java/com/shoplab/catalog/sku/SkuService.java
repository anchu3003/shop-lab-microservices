package com.shoplab.catalog.sku;

import com.shoplab.catalog.cache.CatalogCache;
import com.shoplab.catalog.product.ProductRepository;
import com.shoplab.commons.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ SKU: tạo biến thể cho một product đã tồn tại. */
@Service
public class SkuService {

  private final SkuRepository skus;
  private final ProductRepository products;
  private final CatalogCache cache;
  private final Clock clock;

  public SkuService(
      SkuRepository skus, ProductRepository products, CatalogCache cache, Clock clock) {
    this.skus = skus;
    this.products = products;
    this.cache = cache;
    this.clock = clock;
  }

  /**
   * Tạo SKU cho product.
   *
   * @throws NotFoundException khi product không tồn tại <b>hoặc</b> đã bị xoá mềm (SL-2.11: không
   *     cho thêm biến thể vào sản phẩm đã xoá)
   * @throws DuplicateSkuException khi {@code code} đã dùng (409)
   */
  @Transactional
  public SkuResponse create(UUID productId, CreateSkuRequest request) {
    if (!products.existsByIdAndDeletedAtIsNull(productId)) {
      throw new NotFoundException("Product không tồn tại: " + productId);
    }
    String code = request.code().trim();
    if (skus.existsByCode(code)) {
      throw new DuplicateSkuException(code);
    }
    SkuResponse created =
        SkuResponse.from(
            skus.save(
                Sku.create(
                    productId,
                    code,
                    request.priceOverride(),
                    request.stockHint(),
                    Instant.now(clock))));
    // SL-4.2: `GET /v1/products/{id}` trả kèm danh sách SKU ⇒ thêm SKU là detail cũ sai. List thì
    // không chứa SKU nên không cần bump generation.
    cache.productDetailChanged(productId);
    return created;
  }

  /** SKU của product, sắp theo mã (dùng cho trang chi tiết). */
  @Transactional(readOnly = true)
  public List<SkuResponse> findByProduct(UUID productId) {
    return skus.findByProductIdOrderByCodeAsc(productId).stream().map(SkuResponse::from).toList();
  }
}
