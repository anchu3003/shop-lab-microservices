package com.shoplab.catalog.category;

import java.time.Instant;
import java.util.UUID;

/** Dữ liệu category trả ra ngoài — DTO, không trả thẳng entity (tránh lộ cấu trúc DB). */
public record CategoryResponse(
    UUID id, String name, String slug, Instant createdAt, Instant updatedAt) {

  public static CategoryResponse from(Category category) {
    return new CategoryResponse(
        category.id(),
        category.name(),
        category.slug(),
        category.createdAt(),
        category.updatedAt());
  }
}
