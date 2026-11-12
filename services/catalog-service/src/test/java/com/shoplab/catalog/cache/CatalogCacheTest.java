package com.shoplab.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.shoplab.catalog.testsupport.CacheTestSupport;
import com.shoplab.catalog.testsupport.CacheTestSupport.InMemoryCacheStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invalidation là chỗ <b>duy nhất</b> cache có thể làm hệ trả dữ liệu sai. Test ở đây kiểm "ghi thì
 * xoá đúng key nào", không kiểm định dạng JSON của giá trị.
 *
 * <p>Đặc biệt quan trọng: {@code productChanged} phải xoá <b>cả</b> key SKU của product — giá SKU
 * là {@code priceOverride ?? product.price}, nên đổi giá product mà không xoá dòng SKU là bán giá
 * cũ (sai tiền, không phải sai số đo).
 */
class CatalogCacheTest {

  private final InMemoryCacheStore store = new InMemoryCacheStore();
  private final CacheProperties props = CacheTestSupport.ENABLED;
  private final CacheKeys keys = new CacheKeys(props);
  private final CacheMetrics metrics = new CacheMetrics();
  private final CatalogCache invalidator = new CatalogCache(store, keys, metrics);

  @Test
  @DisplayName("productChanged: xoá detail + MỌI key SKU của product, và bump generation của list")
  void productChangedClearsDetailSkusAndList() {
    UUID productId = UUID.randomUUID();
    UUID skuA = UUID.randomUUID();
    UUID skuB = UUID.randomUUID();
    UUID otherSku = UUID.randomUUID();
    store.seed(keys.detail(productId), "{}");
    store.seed(keys.sku(skuA), "{}");
    store.seed(keys.sku(skuB), "{}");
    store.seed(keys.sku(otherSku), "{}");
    store.seed(keys.list(0, "fingerprint"), "{}");

    invalidator.productChanged(productId, List.of(skuA, skuB));

    assertThat(store.peek(keys.detail(productId))).isEmpty();
    assertThat(store.peek(keys.sku(skuA))).isEmpty();
    assertThat(store.peek(keys.sku(skuB))).isEmpty();
    // SKU của product khác KHÔNG được xoá — xoá quá tay là tự làm hit ratio sụp mỗi lần ghi.
    assertThat(store.peek(keys.sku(otherSku))).isPresent();
    // List không xoá từng key: bump generation (0 → 1) làm key cũ thành vô nghĩa.
    assertThat(store.peek(keys.listGeneration())).contains("1");
    // Key list cũ vẫn còn trong Redis nhưng không ai đọc nữa (chết theo TTL).
    assertThat(store.peek(keys.list(0, "fingerprint"))).isPresent();
  }

  @Test
  @DisplayName("productDetailChanged: chỉ xoá detail, KHÔNG bump generation (list không chứa SKU)")
  void detailOnlyChangeDoesNotTouchGeneration() {
    UUID productId = UUID.randomUUID();
    store.seed(keys.detail(productId), "{}");

    invalidator.productDetailChanged(productId);

    assertThat(store.peek(keys.detail(productId))).isEmpty();
    assertThat(store.peek(keys.listGeneration())).isEmpty();
  }

  @Test
  @DisplayName("categoriesChanged: xoá đúng 1 key categories")
  void categoriesChangedClearsSingleKey() {
    store.seed(keys.categories(), "[]");

    invalidator.categoriesChanged();

    assertThat(store.peek(keys.categories())).isEmpty();
  }

  @Test
  @DisplayName("Redis chết lúc invalidate ⇒ KHÔNG ném lỗi (request ghi vẫn phải thành công)")
  void invalidateIsQuietWhenRedisIsDown() {
    store.failing = true;

    assertThatCode(() -> invalidator.productChanged(UUID.randomUUID(), List.of(UUID.randomUUID())))
        .doesNotThrowAnyException();
    assertThatCode(() -> invalidator.bumpListGeneration()).doesNotThrowAnyException();
    assertThatCode(() -> invalidator.categoriesChanged()).doesNotThrowAnyException();
    assertThat(metrics.of("invalidate", CacheMetrics.BYPASS)).isGreaterThan(0);
  }
}
