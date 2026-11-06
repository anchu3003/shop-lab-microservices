package com.shoplab.catalog.sku;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository của {@link Sku}. */
public interface SkuRepository extends JpaRepository<Sku, UUID> {

  /** SKU của một product, sắp theo mã — thứ tự ổn định để response không "nhảy" giữa 2 lần gọi. */
  List<Sku> findByProductIdOrderByCodeAsc(UUID productId);

  /** Mã SKU phải duy nhất toàn hệ (unique index ở DB); check trước để trả 409 thay vì 500. */
  boolean existsByCode(String code);
}
