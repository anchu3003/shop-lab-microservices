package com.shoplab.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Dữ liệu product trả ra ngoài — DTO, không trả thẳng entity. */
public record ProductResponse(
    UUID id,
    UUID categoryId,
    String name,
    String description,
    BigDecimal price,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.id(),
        product.categoryId(),
        product.name(),
        product.description(),
        product.price(),
        product.version(),
        product.createdAt(),
        product.updatedAt());
  }
}
