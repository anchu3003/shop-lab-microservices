package com.shoplab.catalog.sku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Dữ liệu SKU trả ra ngoài — DTO, không trả thẳng entity. */
public record SkuResponse(
    UUID id,
    String code,
    BigDecimal priceOverride,
    Integer stockHint,
    SkuStatus status,
    Instant createdAt) {

  public static SkuResponse from(Sku sku) {
    return new SkuResponse(
        sku.id(), sku.code(), sku.priceOverride(), sku.stockHint(), sku.status(), sku.createdAt());
  }
}
