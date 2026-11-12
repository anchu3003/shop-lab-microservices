package com.shoplab.catalog.sku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.cache.CacheMetrics;
import com.shoplab.catalog.cache.CacheProperties;
import com.shoplab.catalog.cache.SkuLineCache;
import com.shoplab.catalog.product.Product;
import com.shoplab.catalog.product.ProductRepository;
import com.shoplab.catalog.testsupport.CacheTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho {@link SkuLookupService} (SL-2.13) — phần quyết định "SKU nào mua được, giá bao
 * nhiêu".
 *
 * <p>Ba bất biến được test ở đây (không cần Postgres): giá = {@code priceOverride ??
 * product.price}, SKU/product đã archive <b>không</b> được trả, và thứ tự response = thứ tự client
 * gửi.
 *
 * <p>SL-4.2: test chạy với cache <b>TẮT</b> để giữ nguyên ngữ nghĩa cũ; hành vi cache (MGET, hit,
 * sentinel "không mua được") có test riêng ở {@code SkuLineCacheTest}.
 */
class SkuLookupServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-08T09:00:00Z");

  private final SkuRepository skus = mock(SkuRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final CacheProperties cacheProperties = CacheTestSupport.DISABLED;

  private final SkuLookupService service =
      new SkuLookupService(
          skus,
          products,
          new SkuLineCache(
              new CacheTestSupport.InMemoryCacheStore(),
              new com.shoplab.catalog.cache.CacheKeys(cacheProperties),
              cacheProperties,
              new CacheMetrics(),
              CacheTestSupport.MAPPER));

  @Test
  @DisplayName("giá = priceOverride khi SKU có giá riêng; fallback giá product khi override null")
  void unitPriceFallsBackToProduct() {
    Product product = product("Ao thun", "100000.00");
    Sku withOverride = Sku.create(product.id(), "AT-DO-M", new BigDecimal("120000.00"), 5, NOW);
    Sku withoutOverride = Sku.create(product.id(), "AT-DO-L", null, 5, NOW);
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of(withOverride, withoutOverride));
    when(products.findByIdInAndDeletedAtIsNull(any())).thenReturn(List.of(product));

    SkuLookupResponse response =
        service.lookup(new SkuLookupQuery(List.of(withOverride.id(), withoutOverride.id())));

    assertThat(response.missingIds()).isEmpty();
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("120000.00");
    assertThat(response.items().get(0).skuCode()).isEqualTo("AT-DO-M");
    assertThat(response.items().get(0).title()).isEqualTo("Ao thun");
    assertThat(response.items().get(0).productId()).isEqualTo(product.id());
    assertThat(response.items().get(1).unitPrice()).isEqualByComparingTo("100000.00");
  }

  @Test
  @DisplayName("SKU archived (repository đã lọc theo status) → missingIds, không lỗi")
  void archivedSkuIsMissing() {
    Product product = product("Giay", "500000.00");
    Sku live = Sku.create(product.id(), "G-1", null, null, NOW);
    UUID archivedId = UUID.randomUUID();
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of(live));
    when(products.findByIdInAndDeletedAtIsNull(any())).thenReturn(List.of(product));

    SkuLookupResponse response = service.lookup(new SkuLookupQuery(List.of(live.id(), archivedId)));

    assertThat(response.items())
        .extracting(SkuLookupResponse.SkuLine::skuId)
        .containsExactly(live.id());
    assertThat(response.missingIds()).containsExactly(archivedId);
  }

  @Test
  @DisplayName("SKU ACTIVE nhưng product đã xoá mềm → vẫn bị loại (chốt chặn thứ hai)")
  void skuOfArchivedProductIsMissing() {
    Product archived = product("Bi xoa", "90000.00");
    archived.archive(NOW);
    Sku orphan = Sku.create(archived.id(), "BX-1", null, null, NOW);
    // Repository trả về SKU ACTIVE (giả lập dữ liệu lệch do migration tay), nhưng product đã xoá ⇒
    // findByIdInAndDeletedAtIsNull không trả product đó.
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of(orphan));
    when(products.findByIdInAndDeletedAtIsNull(any())).thenReturn(List.of());

    SkuLookupResponse response = service.lookup(new SkuLookupQuery(List.of(orphan.id())));

    assertThat(response.items()).isEmpty();
    assertThat(response.missingIds()).containsExactly(orphan.id());
  }

  @Test
  @DisplayName("thứ tự response = thứ tự client gửi (không phụ thuộc thứ tự DB trả)")
  void orderFollowsQuery() {
    Product product = product("Sach", "70000.00");
    Sku first = Sku.create(product.id(), "S-1", null, null, NOW);
    Sku second = Sku.create(product.id(), "S-2", null, null, NOW);
    // DB trả ngược thứ tự client hỏi.
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of(second, first));
    when(products.findByIdInAndDeletedAtIsNull(any())).thenReturn(List.of(product));

    SkuLookupResponse response =
        service.lookup(new SkuLookupQuery(List.of(first.id(), second.id())));

    assertThat(response.items())
        .extracting(SkuLookupResponse.SkuLine::skuCode)
        .containsExactly("S-1", "S-2");
  }

  @Test
  @DisplayName("không tìm thấy SKU nào → không query product (tránh query rỗng vô nghĩa)")
  void noSkusMeansNoProductQuery() {
    UUID missing = UUID.randomUUID();
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of());

    SkuLookupResponse response = service.lookup(new SkuLookupQuery(List.of(missing)));

    assertThat(response.items()).isEmpty();
    assertThat(response.missingIds()).containsExactly(missing);
    verify(products, never()).findByIdInAndDeletedAtIsNull(any());
  }

  @Test
  @DisplayName("nhiều SKU cùng product → chỉ 1 id product được hỏi (không N+1)")
  void productIdsAreDeduplicated() {
    Product product = product("Ao", "150000.00");
    Sku a = Sku.create(product.id(), "A-1", null, null, NOW);
    Sku b = Sku.create(product.id(), "A-2", null, null, NOW);
    when(skus.findByIdInAndStatus(any(), any())).thenReturn(List.of(a, b));
    when(products.findByIdInAndDeletedAtIsNull(any())).thenReturn(List.of(product));

    service.lookup(new SkuLookupQuery(List.of(a.id(), b.id())));

    verify(products).findByIdInAndDeletedAtIsNull(List.of(product.id()));
  }

  private static Product product(String name, String price) {
    return Product.create(UUID.randomUUID(), name, null, new BigDecimal(price), NOW);
  }
}
