package com.shoplab.catalog.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Cache-aside (ADR-0009) + <b>single-flight</b> chống cache stampede.
 *
 * <p>Luồng của một lần đọc:
 *
 * <ol>
 *   <li>cache hit ⇒ trả luôn ({@code cache=hit});
 *   <li>miss ⇒ thử lock {@code SET NX PX}; thắng ⇒ đọc DB rồi ghi cache ({@code
 *       cache=miss-loaded});
 *   <li>thua lock ⇒ chờ ngắn ({@code lock-wait}, poll 20 ms) để người thắng điền cache ({@code
 *       cache=hit-after-wait});
 *   <li>chờ hết mà vẫn miss ⇒ <b>tự đọc DB</b> ({@code cache=miss-timeout}). Thà một request chậm
 *       còn hơn giữ 200 request làm con tin — nếu không, một key hot chậm sẽ kéo p99 của cả hệ;
 *   <li>Redis không dùng được ({@link CacheUnavailableException}) ⇒ đọc DB ngay, không chờ ({@code
 *       cache=bypass}).
 * </ol>
 *
 * <p><b>Vì sao lock bằng Redis chứ không {@code synchronized}</b>: JVM lock chỉ đúng khi có đúng 1
 * instance. Compose hôm nay chạy 1 instance, nhưng code phải đúng khi scale ngang — và chính Phase
 * 4 là lúc học điều đó.
 */
@Component
public class CacheAside {

  private static final Logger LOG = LoggerFactory.getLogger(CacheAside.class);

  /** Nhịp poll khi chờ người thắng lock. 20 ms ≈ 1/5 thời gian đọc DB điển hình. */
  static final long POLL_MILLIS = 20L;

  private final CacheStore store;
  private final CacheKeys keys;
  private final CacheProperties properties;
  private final CacheMetrics metrics;
  private final ObjectMapper objectMapper;

  public CacheAside(
      CacheStore store,
      CacheKeys keys,
      CacheProperties properties,
      CacheMetrics metrics,
      ObjectMapper objectMapper) {
    this.store = store;
    this.keys = keys;
    this.properties = properties;
    this.metrics = metrics;
    this.objectMapper = objectMapper;
  }

  /**
   * Trả giá trị trong cache, không có thì gọi {@code loader} (DB) và ghi lại.
   *
   * @param kind tên loại cache để đếm ({@code detail}/{@code list}/{@code sku}/{@code categories})
   * @param key key đầy đủ (xem {@link CacheKeys})
   * @param type kiểu đích để deserialize JSON
   * @param loader nguồn sự thật — <b>chỉ</b> được gọi khi cache miss
   */
  public <T> T getOrLoad(String kind, String key, JavaType type, Supplier<T> loader) {
    return getOrLoad(kind, key, type, properties.ttl(), loader);
  }

  /** Như trên nhưng TTL riêng (vd category đổi ít ⇒ TTL dài hơn detail/list). */
  public <T> T getOrLoad(
      String kind, String key, JavaType type, Duration baseTtl, Supplier<T> loader) {
    if (!properties.enabled()) {
      metrics.bypass(kind);
      log("disabled", kind, key);
      return loader.get();
    }

    Optional<String> cached;
    try {
      cached = store.get(key);
    } catch (CacheUnavailableException ex) {
      metrics.error("get");
      return loadBypassing(kind, key, loader);
    }
    if (cached.isPresent()) {
      T decoded = decode(cached.get(), type);
      if (decoded != null) {
        metrics.hit(kind);
        log("hit", kind, key);
        return decoded;
      }
      // JSON hỏng / không tương thích codec mới: coi như miss rồi ghi đè bằng dữ liệu DB. Không xoá
      // ngay để tránh vòng xoá-ghi liên tục nếu lỗi nằm ở codec chứ không ở dữ liệu.
      log("hit-unreadable", kind, key);
    }

    metrics.miss(kind);
    String lockKey = keys.lock(key);
    String token = UUID.randomUUID().toString();
    boolean won;
    try {
      won = store.lock(lockKey, token, properties.lockTtl());
    } catch (CacheUnavailableException ex) {
      metrics.error("lock");
      return loadBypassing(kind, key, loader);
    }

    if (won) {
      metrics.lockWin(kind);
      try {
        return loadUnderLock(kind, key, type, baseTtl, loader);
      } finally {
        releaseLock(lockKey, token);
      }
    }
    return awaitWinner(kind, key, type, baseTtl, loader);
  }

  /**
   * Generation hiện tại của list; {@code 0} khi chưa có (lần đầu) hoặc không đọc được.
   *
   * <p>Đọc generation là một round-trip Redis <b>thêm</b> cho mỗi request list. Chấp nhận: đó là
   * giá phải trả để list luôn tươi sau ghi mà không phải SCAN/xoá từng key (xem {@link CacheKeys}).
   */
  public long listGeneration() {
    if (!properties.enabled()) {
      return 0L;
    }
    try {
      return store.get(keys.listGeneration()).map(CacheAside::parseLongOrZero).orElse(0L);
    } catch (CacheUnavailableException ex) {
      metrics.error("get");
      metrics.bypass("list");
      return 0L;
    }
  }

  private static long parseLongOrZero(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return 0L;
    }
  }

  /** Thắng lock: đọc lại cache lần nữa (double-check) rồi mới đọc DB. */
  private <T> T loadUnderLock(
      String kind, String key, JavaType type, Duration baseTtl, Supplier<T> loader) {
    try {
      Optional<String> filled = store.get(key);
      if (filled.isPresent()) {
        T decoded = decode(filled.get(), type);
        if (decoded != null) {
          metrics.hit(kind);
          log("hit-after-lock", kind, key);
          return decoded;
        }
      }
    } catch (CacheUnavailableException ex) {
      metrics.error("get");
      return loadBypassing(kind, key, loader);
    }

    T value = loader.get();
    metrics.dbLoad(kind);
    log("miss-loaded", kind, key);
    putQuietly(kind, key, value, baseTtl);
    return value;
  }

  /** Thua lock: chờ người thắng điền cache; quá hạn thì tự đọc DB (không ghi cache). */
  private <T> T awaitWinner(
      String kind, String key, JavaType type, Duration baseTtl, Supplier<T> loader) {
    long waitMillis = Math.max(0L, properties.lockWait().toMillis());
    long deadline = System.nanoTime() + Duration.ofMillis(waitMillis).toNanos();
    while (System.nanoTime() < deadline) {
      sleepQuietly();
      try {
        Optional<String> filled = store.get(key);
        if (filled.isPresent()) {
          T decoded = decode(filled.get(), type);
          if (decoded != null) {
            metrics.hit(kind);
            log("hit-after-wait", kind, key);
            return decoded;
          }
        }
      } catch (CacheUnavailableException ex) {
        metrics.error("get");
        return loadBypassing(kind, key, loader);
      }
    }
    metrics.lockTimeout(kind);
    log("miss-timeout", kind, key);
    T value = loader.get();
    metrics.dbLoad(kind);
    // Cố ý KHÔNG ghi cache: người thắng lock đang ghi (hoặc Redis hỏng). Hai nguồn ghi cùng key là
    // cách sinh ra giá trị cũ đè giá trị mới khi thứ tự ghi đảo.
    return value;
  }

  private <T> T loadBypassing(String kind, String key, Supplier<T> loader) {
    metrics.bypass(kind);
    log("bypass", kind, key);
    T value = loader.get();
    metrics.dbLoad(kind);
    return value;
  }

  /** Ghi cache sau khi đọc DB — lỗi ở bước này KHÔNG được làm hỏng response. */
  private void putQuietly(String kind, String key, Object value, Duration baseTtl) {
    if (value == null) {
      return;
    }
    try {
      Duration ttl =
          TtlJitter.jitter(
              baseTtl, properties.ttlJitterPercent(), ThreadLocalRandom.current()::nextDouble);
      store.put(key, objectMapper.writeValueAsString(value), ttl);
    } catch (CacheUnavailableException ex) {
      metrics.error("put");
      log("put-failed", kind, key);
    } catch (RuntimeException ex) {
      metrics.error("serialize");
      LOG.warn("cache: không serialize được giá trị cho {} ({}) — bỏ qua", kind, ex.getMessage());
    }
  }

  private void releaseLock(String lockKey, String token) {
    try {
      store.unlock(lockKey, token);
    } catch (CacheUnavailableException ex) {
      // Lock có TTL nên tự nhả — không làm gì thêm, cũng không được để lỗi này nổi lên.
      metrics.error("unlock");
      log("unlock-failed", "lock", lockKey);
    }
  }

  private <T> T decode(String json, JavaType type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (RuntimeException ex) {
      metrics.error("deserialize");
      return null;
    }
  }

  private static void sleepQuietly() {
    try {
      Thread.sleep(POLL_MILLIS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Log mỗi quyết định ở DEBUG, không phải INFO: 3.800 request/s × 1 dòng INFO là tự tạo sự cố. Cần
   * soi từng request thì bật {@code LOG_LEVEL=DEBUG}; số tổng hợp đọc ở {@code /debug/cache}.
   */
  private static void log(String status, String kind, String key) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("cache={} kind={} key={}", status, kind, key);
    }
  }
}
