package com.shoplab.catalog.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * {@link CacheStore} trên Redis (Lettuce, {@link StringRedisTemplate}).
 *
 * <p><b>Fail-open ở đúng một chỗ</b>: mọi lỗi Redis được đổi thành {@link
 * CacheUnavailableException} ngay tại đây, kèm log WARN có rate-limit. Tầng trên chỉ cần một {@code
 * catch} là xong — không service nào phải tự nghĩ "Redis chết thì sao".
 *
 * <p>Vì sao rate-limit log: Redis chết mà log mỗi request thì 3.800 request/s làm ngập log và chính
 * việc ghi log trở thành tải. Một dòng mỗi {@code warn-interval} là đủ để biết đang sự cố, cộng với
 * bộ đếm ở {@link CacheMetrics} (không mất mát).
 */
@Component
public class RedisCacheStore implements CacheStore {

  private static final Logger LOG = LoggerFactory.getLogger(RedisCacheStore.class);

  /**
   * Compare-and-delete: chỉ xoá lock nếu token còn là của mình. Chạy trong 1 EVAL ⇒ atomic, không
   * có khe giữa GET và DEL.
   */
  private static final DefaultRedisScript<Long> UNLOCK =
      new DefaultRedisScript<>(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
          else
            return 0
          end
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final CacheProperties properties;

  /** Mốc thời gian (ms) của lần WARN gần nhất — dùng cho rate-limit log. */
  private final AtomicLong lastWarnAt = new AtomicLong(0);

  public RedisCacheStore(StringRedisTemplate redis, CacheProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Optional<String> get(String key) {
    return Optional.ofNullable(guard("get", () -> redis.opsForValue().get(key)));
  }

  @Override
  public List<Optional<String>> getAll(List<String> keys) {
    if (keys.isEmpty()) {
      return List.of();
    }
    List<String> raw = guard("getAll", () -> redis.opsForValue().multiGet(keys));
    if (raw == null) {
      return keys.stream().map(k -> Optional.<String>empty()).toList();
    }
    List<Optional<String>> result = new ArrayList<>(raw.size());
    for (String value : raw) {
      result.add(Optional.ofNullable(value));
    }
    return result;
  }

  @Override
  public void put(String key, String value, Duration ttl) {
    guard(
        "put",
        () -> {
          redis.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
          return null;
        });
  }

  @Override
  public boolean lock(String key, String token, Duration ttl) {
    Boolean acquired =
        guard(
            "lock",
            () ->
                redis.opsForValue().setIfAbsent(key, token, ttl.toMillis(), TimeUnit.MILLISECONDS));
    return Boolean.TRUE.equals(acquired);
  }

  @Override
  public void unlock(String key, String token) {
    guard("unlock", () -> redis.execute(UNLOCK, List.of(key), token));
  }

  @Override
  public boolean delete(String key) {
    return Boolean.TRUE.equals(guard("delete", () -> redis.delete(key)));
  }

  @Override
  public void deleteAll(Collection<String> keys) {
    if (keys.isEmpty()) {
      return;
    }
    guard(
        "deleteAll",
        () -> {
          redis.delete(keys);
          return null;
        });
  }

  @Override
  public long increment(String key) {
    Long value = guard("increment", () -> redis.opsForValue().increment(key));
    return value == null ? 0L : value;
  }

  /**
   * Chạy 1 lệnh Redis, đổi mọi lỗi thành {@link CacheUnavailableException}.
   *
   * <p>Lỗi thường gặp: Redis chết ({@code RedisConnectionFailureException}), hết {@code
   * connect-timeout}, sai mật khẩu. Cả 3 đều dẫn tới cùng một hành vi đúng: đọc DB.
   */
  private <T> T guard(String operation, RedisCall<T> call) {
    try {
      return call.run();
    } catch (RuntimeException ex) {
      // KHÔNG đếm metric ở đây: việc đếm nằm ở tầng CacheAside để mọi impl CacheStore đều được
      // đếm (fake store trong test, impl khác sau này). Ở đây chỉ log có rate-limit.
      warnThrottled(operation, ex);
      throw new CacheUnavailableException(
          "Redis lỗi ở '" + operation + "': " + ex.getMessage(), ex);
    }
  }

  private void warnThrottled(String operation, RuntimeException ex) {
    long now = System.currentTimeMillis();
    long interval = Math.max(1, properties.warnInterval().toMillis());
    long last = lastWarnAt.get();
    // CAS để nhiều thread không cùng in 1 dòng.
    if (now - last >= interval && lastWarnAt.compareAndSet(last, now)) {
      LOG.warn(
          "cache: Redis không dùng được ({}: {}) — đang đọc thẳng DB (fail-open)",
          operation,
          ex.getClass().getSimpleName());
    }
  }

  /** Cho phép lambda ném unchecked — giữ {@link #guard} gọn. */
  @FunctionalInterface
  interface RedisCall<T> {
    T run();
  }
}
