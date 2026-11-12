package com.shoplab.catalog.testsupport;

import com.shoplab.catalog.cache.CacheAside;
import com.shoplab.catalog.cache.CacheKeys;
import com.shoplab.catalog.cache.CacheMetrics;
import com.shoplab.catalog.cache.CacheProperties;
import com.shoplab.catalog.cache.CacheStore;
import com.shoplab.catalog.cache.CacheUnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hạ tầng test cho tầng cache: {@link CacheStore} trong bộ nhớ + bộ tham số dựng sẵn.
 *
 * <p><b>Vì sao không dùng Redis thật trong unit test</b>: logic cần kiểm là <i>quyết định</i> (hit
 * / miss / thua lock / Redis chết / vô hiệu hoá), không phải cú pháp Redis. Redis thật được kiểm ở
 * IT (1 test single-flight) và ở drill bằng `docker stop shoplab-redis` — mỗi tầng kiểm đúng thứ nó
 * kiểm được.
 *
 * <p>{@link #failing} cho phép bật "Redis chết" giữa chừng để test fail-open mà không cần chờ
 * timeout thật.
 */
public final class CacheTestSupport {

  /** Jitter = 0 để test tất định (jitter có test riêng trong {@code TtlJitterTest}). */
  public static final CacheProperties ENABLED =
      new CacheProperties(
          true,
          Duration.ofSeconds(60),
          0,
          Duration.ofMinutes(5),
          Duration.ofSeconds(2),
          Duration.ofMillis(100),
          "catalog:v1",
          Duration.ofSeconds(5));

  /** Cache tắt: mọi lời gọi đi thẳng loader — dùng cho test nghiệp vụ thuần. */
  public static final CacheProperties DISABLED =
      new CacheProperties(
          false,
          Duration.ofSeconds(60),
          0,
          Duration.ofMinutes(5),
          Duration.ofSeconds(2),
          Duration.ofMillis(100),
          "catalog:v1",
          Duration.ofSeconds(5));

  public static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private CacheTestSupport() {
    throw new AssertionError("utility class — không khởi tạo");
  }

  /** {@link CacheAside} dựng sẵn trên store cho trước. */
  public static CacheAside aside(
      CacheStore store, CacheProperties properties, CacheMetrics metrics) {
    return new CacheAside(store, new CacheKeys(properties), properties, metrics, MAPPER);
  }

  /** {@link CacheStore} trong bộ nhớ — đủ để tái hiện mọi nhánh của cache-aside. */
  public static final class InMemoryCacheStore implements CacheStore {

    private final Map<String, String> data = new ConcurrentHashMap<>();
    private final Set<String> locks = ConcurrentHashMap.newKeySet();
    private final AtomicInteger putCount = new AtomicInteger();
    private final AtomicInteger lockAttempts = new AtomicInteger();

    /** Bật để mô phỏng Redis chết: mọi thao tác ném {@link CacheUnavailableException}. */
    public volatile boolean failing;

    @Override
    public Optional<String> get(String key) {
      guard();
      return Optional.ofNullable(data.get(key));
    }

    @Override
    public List<Optional<String>> getAll(List<String> keys) {
      guard();
      List<Optional<String>> result = new ArrayList<>(keys.size());
      for (String key : keys) {
        result.add(Optional.ofNullable(data.get(key)));
      }
      return result;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
      guard();
      putCount.incrementAndGet();
      data.put(key, value);
    }

    @Override
    public boolean lock(String key, String token, Duration ttl) {
      guard();
      lockAttempts.incrementAndGet();
      // Không mô phỏng hết hạn lock: test nào cần "lock bị người khác giữ" thì tự giữ trước.
      return locks.add(key);
    }

    @Override
    public void unlock(String key, String token) {
      guard();
      locks.remove(key);
    }

    @Override
    public boolean delete(String key) {
      guard();
      return data.remove(key) != null;
    }

    @Override
    public void deleteAll(Collection<String> keys) {
      guard();
      keys.forEach(data::remove);
    }

    @Override
    public long increment(String key) {
      guard();
      return Long.parseLong(
          data.merge(key, "1", (old, one) -> String.valueOf(Long.parseLong(old) + 1)));
    }

    /** Ghi thẳng vào store (không qua putCount) — để dựng trạng thái "đã có trong cache". */
    public void seed(String key, String value) {
      data.put(key, value);
    }

    /** Đọc thẳng, không kiểm `failing` — để assert nội dung sau khi test. */
    public Optional<String> peek(String key) {
      return Optional.ofNullable(data.get(key));
    }

    public Map<String, String> snapshot() {
      return new LinkedHashMap<>(data);
    }

    public int putCount() {
      return putCount.get();
    }

    public int lockAttempts() {
      return lockAttempts.get();
    }

    public boolean holdsLock(String key) {
      return locks.contains(key);
    }

    public void holdLock(String key) {
      locks.add(key);
    }

    public void clear() {
      data.clear();
      locks.clear();
    }

    private void guard() {
      if (failing) {
        throw new CacheUnavailableException("Redis giả lập đang chết (test)", null);
      }
    }
  }
}
