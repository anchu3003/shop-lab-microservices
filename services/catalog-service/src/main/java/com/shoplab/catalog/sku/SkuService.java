package com.shoplab.catalog.sku;

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
  private final Clock clock;

  public SkuService(SkuRepository skus, ProductRepository products, Clock clock) {
    this.skus = skus;
    this.products = products;
    this.clock = clock;
  }

  /**
   * Tạo SKU cho product.
   *
   * @throws NotFoundException khi product không tồn tại (404)
   * @throws DuplicateSkuException khi {@code code} đã dùng (409)
   */
  @Transactional
  public SkuResponse create(UUID productId, CreateSkuRequest request) {
    if (!products.existsById(productId)) {
      throw new NotFoundException("Product không tồn tại: " + productId);
    }
    String code = request.code().trim();
    if (skus.existsByCode(code)) {
      throw new DuplicateSkuException(code);
    }
    return SkuResponse.from(
        skus.save(
            Sku.create(
                productId,
                code,
                request.priceOverride(),
                request.stockHint(),
                Instant.now(clock))));
  }

  /** SKU của product, sắp theo mã (dùng cho trang chi tiết). */
  @Transactional(readOnly = true)
  public List<SkuResponse> findByProduct(UUID productId) {
    return skus.findByProductIdOrderByCodeAsc(productId).stream().map(SkuResponse::from).toList();
  }
}
