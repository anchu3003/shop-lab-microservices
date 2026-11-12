package com.shoplab.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.catalog.sku.SkuLookupResponse;
import com.shoplab.catalog.testsupport.CacheTestSupport;
import com.shoplab.catalog.testsupport.CacheTestSupport.InMemoryCacheStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code skus?ids=} là đường checkout/gọi giỏ dùng để lấy giá thật ⇒ sai ở đây là sai tiền. Ba thứ
 * được kiểm: cache theo <b>từng SKU</b> (MGET), sentinel cho SKU không mua được, và fail-open.
 */
class SkuLineCacheTest {

  private final InMemoryCacheStore store = new InMemoryCacheStore();
  private final CacheProperties props = CacheTestSupport.ENABLED;
  private final CacheKeys keys = new CacheKeys(props);
  private final CacheMetrics metrics = new CacheMetrics();
  private final SkuLineCache cache =
      new SkuLineCache(store, keys, props, metrics, CacheTestSupport.MAPPER);

  @Test
  @DisplayName("có sẵn trong cache ⇒ KHÔNG gọi DB, trả đúng dòng đã cache")
  void allCachedSkipsDb() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    store.seed(keys.sku(a), json(line(a, "100000.00")));
    store.seed(keys.sku(b), json(line(b, "200000.00")));

    SkuLineCache.Batch batch = cache.load(List.of(a, b), ids -> fail("không được gọi DB"));

    assertThat(batch.lines()).hasSize(2);
    assertThat(batch.lines().get(a).unitPrice()).isEqualByComparingTo("100000.00");
    assertThat(metrics.of("sku", CacheMetrics.HIT)).isEqualTo(2);
    assertThat(store.putCount()).isZero();
  }

  @Test
  @DisplayName("miss một phần: chỉ hỏi DB cho id thiếu, rồi ghi cache id đó")
  void partialMissQueriesOnlyMissing() {
    UUID cached = UUID.randomUUID();
    UUID missing = UUID.randomUUID();
    store.seed(keys.sku(cached), json(line(cached, "50000.00")));

    List<UUID> asked = new ArrayList<>();
    SkuLineCache.Batch batch =
        cache.load(
            List.of(cached, missing),
            ids -> {
              asked.addAll(ids);
              return new SkuLineCache.Batch(Map.of(missing, line(missing, "70000.00")), Set.of());
            });

    assertThat(asked).containsExactly(missing);
    assertThat(batch.lines()).containsOnlyKeys(cached, missing);
    assertThat(store.peek(keys.sku(missing))).isPresent();
  }

  @Test
  @DisplayName("SKU không mua được ⇒ cache sentinel 'null', lần sau không đập DB")
  void notBuyableIsNegativelyCached() {
    UUID ghost = UUID.randomUUID();

    SkuLineCache.Batch first =
        cache.load(List.of(ghost), ids -> new SkuLineCache.Batch(Map.of(), Set.of(ghost)));
    assertThat(first.notBuyable()).containsExactly(ghost);
    assertThat(store.peek(keys.sku(ghost))).contains("null");

    SkuLineCache.Batch second =
        cache.load(List.of(ghost), ids -> fail("đã có sentinel, không được gọi DB"));
    assertThat(second.notBuyable()).containsExactly(ghost);
    assertThat(second.lines()).isEmpty();
  }

  @Test
  @DisplayName("Redis chết ⇒ gọi DB cho toàn bộ id (fail-open), không ném lỗi")
  void redisFailureFallsBackToDb() {
    store.failing = true;
    UUID id = UUID.randomUUID();

    SkuLineCache.Batch batch =
        cache.load(
            List.of(id), ids -> new SkuLineCache.Batch(Map.of(id, line(id, "1.00")), Set.of()));

    assertThat(batch.lines()).hasSize(1);
    assertThat(metrics.of("sku", CacheMetrics.BYPASS)).isEqualTo(1);
  }

  @Test
  @DisplayName("cache tắt ⇒ gọi DB toàn bộ, không ghi cache")
  void disabledBypasses() {
    SkuLineCache off =
        new SkuLineCache(store, keys, CacheTestSupport.DISABLED, metrics, CacheTestSupport.MAPPER);
    UUID id = UUID.randomUUID();

    off.load(List.of(id), ids -> new SkuLineCache.Batch(Map.of(id, line(id, "9.00")), Set.of()));

    assertThat(store.putCount()).isZero();
  }

  @Test
  @DisplayName("danh sách id rỗng ⇒ trả rỗng, không gọi DB/cache")
  void emptyIdsShortCircuit() {
    SkuLineCache.Batch batch = cache.load(List.of(), ids -> fail("không được gọi"));

    assertThat(batch.lines()).isEmpty();
    assertThat(batch.notBuyable()).isEmpty();
  }

  private String json(SkuLookupResponse.SkuLine line) {
    return CacheTestSupport.MAPPER.writeValueAsString(line);
  }

  static SkuLookupResponse.SkuLine line(UUID skuId, String price) {
    return new SkuLookupResponse.SkuLine(
        skuId,
        UUID.randomUUID(),
        "SKU-" + skuId.toString().substring(0, 4),
        "Ao thun",
        new BigDecimal(price));
  }

  private static SkuLineCache.Batch fail(String message) {
    throw new AssertionError(message);
  }
}
