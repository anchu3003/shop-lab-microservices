package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả tra giá batch từ `catalog-service` (`GET /v1/skus?ids=`, `SL-2.13`).
 *
 * @param items SKU mua được, kèm giá bán thật
 * @param missingIds SKU không tồn tại / đã archive ⇒ saga trả `409` và <b>không</b> giữ chỗ gì
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkuPrices(List<SkuPrice> items, List<UUID> missingIds) {

  /**
   * @param unitPrice giá bán = `price_override ?? product.price` (catalog đã quyết, saga không tính
   *     lại)
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SkuPrice(UUID skuId, String skuCode, String title, BigDecimal unitPrice) {}

  public SkuPrices {
    items = items == null ? List.of() : List.copyOf(items);
    missingIds = missingIds == null ? List.of() : List.copyOf(missingIds);
  }
}
