package com.shoplab.catalog.product;

import com.shoplab.catalog.cache.CacheAside;
import com.shoplab.catalog.cache.CacheKeys;
import com.shoplab.catalog.web.PageResponse;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Cache của 2 đường ĐỌC product — gói phần "làm sao cache" lại một chỗ (ADR-0009).
 *
 * <p><b>Vì sao không nhét thẳng {@code CacheAside} + {@code CacheKeys} + {@code JavaType} vào
 * {@link ProductService}</b>: service sẽ có 9 tham số (checkstyle chặn ở 7, và đúng là quá nhiều).
 * Quan trọng hơn: service nghiệp vụ không nên biết key cache trông thế nào — nó chỉ nên nói "cho
 * tôi detail của id này, nếu chưa có thì đọc ở đây".
 */
@Component
public class ProductCache {

  private final CacheAside cache;
  private final CacheKeys keys;
  private final JavaType detailType;
  private final JavaType listType;

  public ProductCache(CacheAside cache, CacheKeys keys, ObjectMapper objectMapper) {
    this.cache = cache;
    this.keys = keys;
    this.detailType = objectMapper.getTypeFactory().constructType(ProductDetailResponse.class);
    this.listType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(PageResponse.class, ProductResponse.class);
  }

  /** Detail theo id — key {@code catalog:v1:product:<id>}. */
  public ProductDetailResponse detail(UUID productId, Supplier<ProductDetailResponse> loader) {
    return cache.getOrLoad("detail", keys.detail(productId), detailType, loader);
  }

  /**
   * Một trang list. Key gồm <b>generation hiện tại</b>: một lần bump (khi ghi) làm mọi trang cũ vô
   * nghĩa mà không phải xoá từng key (tổ hợp filter/sort là vô hạn).
   */
  public PageResponse<ProductResponse> list(
      ProductQuery query, Supplier<PageResponse<ProductResponse>> loader) {
    String fingerprint =
        CacheKeys.listFingerprint(
            query.page(),
            query.size(),
            query.categoryId(),
            query.q(),
            query.sortField(),
            query.sortDirection().name());
    String key = keys.list(cache.listGeneration(), fingerprint);
    return cache.getOrLoad("list", key, listType, loader);
  }
}
