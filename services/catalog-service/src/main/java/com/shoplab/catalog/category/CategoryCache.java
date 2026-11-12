package com.shoplab.catalog.category;

import com.shoplab.catalog.cache.CacheAside;
import com.shoplab.catalog.cache.CacheKeys;
import com.shoplab.catalog.cache.CacheProperties;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Cache của {@code GET /v1/categories} — MỘT key duy nhất, TTL dài (category gần như không đổi).
 *
 * <p>Tách khỏi {@link CategoryService} cùng lý do với {@link
 * com.shoplab.catalog.product.ProductCache}: service không phải biết key/TTL, và giữ số tham số
 * constructor ở mức đọc được.
 */
@Component
public class CategoryCache {

  private final CacheAside cache;
  private final CacheKeys keys;
  private final CacheProperties properties;
  private final JavaType listType;

  public CategoryCache(
      CacheAside cache, CacheKeys keys, CacheProperties properties, ObjectMapper objectMapper) {
    this.cache = cache;
    this.keys = keys;
    this.properties = properties;
    this.listType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryResponse.class);
  }

  /** Toàn bộ category, sắp theo tên. */
  public List<CategoryResponse> all(Supplier<List<CategoryResponse>> loader) {
    return cache.getOrLoad(
        "categories", keys.categories(), listType, properties.categoriesTtl(), loader);
  }
}
