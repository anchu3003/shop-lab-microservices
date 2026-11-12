package com.shoplab.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.catalog.testsupport.CacheTestSupport;
import com.shoplab.catalog.testsupport.CacheTestSupport.InMemoryCacheStore;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JavaType;

/**
 * Test các <b>quyết định</b> của cache-aside: hit / miss / thắng lock / thua lock / Redis chết /
 * cache tắt. Đây là phần dễ sai nhất của tầng cache, và cũng là phần không thể kiểm bằng cách nhìn
 * log của một lần chạy thành công.
 *
 * <p>Store là bộ nhớ (không Redis) để mỗi test chạy trong vài ms và <b>tất định</b> — Redis thật
 * được kiểm ở {@code CatalogServiceApplicationIT} và ở drill.
 */
class CacheAsideTest {

  private static final String KEY = "catalog:v1:product:abc";
  private static final JavaType TYPE =
      CacheTestSupport.MAPPER.getTypeFactory().constructType(String.class);

  private final InMemoryCacheStore store = new InMemoryCacheStore();
  private final CacheMetrics metrics = new CacheMetrics();
  private final CacheAside cache = CacheTestSupport.aside(store, CacheTestSupport.ENABLED, metrics);

  @Test
  @DisplayName("hit: KHÔNG gọi loader, không ghi lại cache")
  void hitSkipsLoader() {
    store.seed(KEY, "\"cached\"");
    String value = cache.getOrLoad("detail", KEY, TYPE, () -> "db");

    assertThat(value).isEqualTo("cached");
    assertThat(store.putCount()).isZero();
    assertThat(metrics.of("detail", CacheMetrics.HIT)).isEqualTo(1);
    assertThat(metrics.of("detail", CacheMetrics.DB_LOAD)).isZero();
  }

  @Test
  @DisplayName("miss: gọi loader đúng 1 lần, ghi cache, lần 2 là hit")
  void missLoadsOnceAndStores() {
    AtomicInteger loads = new AtomicInteger();
    String first = cache.getOrLoad("detail", KEY, TYPE, () -> "v" + loads.incrementAndGet());
    String second = cache.getOrLoad("detail", KEY, TYPE, () -> "v" + loads.incrementAndGet());

    assertThat(first).isEqualTo("v1");
    assertThat(second).isEqualTo("v1");
    assertThat(loads).hasValue(1);
    assertThat(store.putCount()).isEqualTo(1);
    assertThat(metrics.of("detail", CacheMetrics.MISS)).isEqualTo(1);
    assertThat(metrics.of("detail", CacheMetrics.DB_LOAD)).isEqualTo(1);
  }

  @Test
  @DisplayName("thua lock + hết lock-wait ⇒ tự đọc DB (không treo), KHÔNG ghi cache")
  void losingLockFallsBackToDb() {
    store.holdLock(new CacheKeys(CacheTestSupport.ENABLED).lock(KEY));
    Duration lockWait = CacheTestSupport.ENABLED.lockWait();

    long startedAt = System.nanoTime();
    String value = cache.getOrLoad("detail", KEY, TYPE, () -> "db");
    long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

    assertThat(value).isEqualTo("db");
    // Có chờ (đúng ý đồ single-flight) nhưng có trần — không chờ vô hạn.
    assertThat(elapsedMillis).isGreaterThanOrEqualTo(lockWait.toMillis() - 5);
    assertThat(elapsedMillis).isLessThan(lockWait.toMillis() + 500);
    assertThat(metrics.of("detail", CacheMetrics.LOCK_TIMEOUT)).isEqualTo(1);
    assertThat(store.putCount()).isZero();
  }

  @Test
  @DisplayName("thua lock nhưng người thắng điền cache trong lúc chờ ⇒ đọc từ cache")
  void waitsThenReadsFilledCache() {
    store.holdLock(new CacheKeys(CacheTestSupport.ENABLED).lock(KEY));

    Thread winner =
        new Thread(
            () -> {
              sleep(30);
              store.seed(KEY, "\"from-winner\"");
            });
    winner.start();

    String value = cache.getOrLoad("detail", KEY, TYPE, () -> "db-direct");

    assertThat(value).isEqualTo("from-winner");
    assertThat(metrics.of("detail", CacheMetrics.DB_LOAD)).isZero();
  }

  @Test
  @DisplayName("Redis chết ⇒ fail-open: vẫn trả dữ liệu từ DB, đếm bypass, không ném lỗi")
  void redisFailureIsFailOpen() {
    store.failing = true;

    String value = cache.getOrLoad("detail", KEY, TYPE, () -> "db");

    assertThat(value).isEqualTo("db");
    assertThat(metrics.of("detail", CacheMetrics.BYPASS)).isEqualTo(1);
    assertThat(metrics.summary().errors()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("Redis chết giữa đường (hit trước, chết sau) vẫn không làm hỏng request")
  void redisFailureAfterWarmupStillServes() {
    cache.getOrLoad("detail", KEY, TYPE, () -> "db");
    store.failing = true;

    assertThat(cache.getOrLoad("detail", KEY, TYPE, () -> "db-2")).isEqualTo("db-2");
  }

  @Test
  @DisplayName("cache tắt: không đọc, không ghi, không lock — chỉ gọi loader")
  void disabledBypassesEverything() {
    CacheAside off = CacheTestSupport.aside(store, CacheTestSupport.DISABLED, new CacheMetrics());

    assertThat(off.getOrLoad("detail", KEY, TYPE, () -> "db")).isEqualTo("db");
    assertThat(store.putCount()).isZero();
    assertThat(store.lockAttempts()).isZero();
    assertThat(off.listGeneration()).isZero();
  }

  @Test
  @DisplayName("JSON hỏng trong cache ⇒ coi như miss, ghi đè bằng dữ liệu DB")
  void unreadableValueIsTreatedAsMiss() {
    store.seed(KEY, "khong-phai-json");

    String value = cache.getOrLoad("detail", KEY, TYPE, () -> "db");

    assertThat(value).isEqualTo("db");
    assertThat(store.peek(KEY)).contains("\"db\"");
  }

  @Test
  @DisplayName("loader ném lỗi ⇒ KHÔNG ghi cache (không cache lỗi)")
  void loaderFailureIsNotCached() {
    Throwable thrown =
        org.assertj.core.api.Assertions.catchThrowable(
            () ->
                cache.getOrLoad(
                    "detail",
                    KEY,
                    TYPE,
                    () -> {
                      throw new IllegalStateException("DB lỗi");
                    }));

    assertThat(thrown).isInstanceOf(IllegalStateException.class);
    assertThat(store.putCount()).isZero();
    assertThat(store.peek(KEY)).isEmpty();
  }

  @Test
  @DisplayName("listGeneration: đọc được số; không có hoặc không phải số ⇒ 0")
  void generationParsing() {
    String generationKey = new CacheKeys(CacheTestSupport.ENABLED).listGeneration();

    assertThat(cache.listGeneration()).isZero();
    store.seed(generationKey, "12");
    assertThat(cache.listGeneration()).isEqualTo(12);
    store.seed(generationKey, "rac");
    assertThat(cache.listGeneration()).isZero();
  }

  @Test
  @DisplayName("lock được nhả sau khi ghi (finally) — không giữ key tới hết lock TTL")
  void lockIsReleased() {
    cache.getOrLoad("detail", KEY, TYPE, () -> "db");

    assertThat(store.holdsLock(new CacheKeys(CacheTestSupport.ENABLED).lock(KEY))).isFalse();
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
