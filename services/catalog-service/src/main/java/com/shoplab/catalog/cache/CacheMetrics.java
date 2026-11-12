package com.shoplab.catalog.cache;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * Bộ đếm của cache — nguồn số cho AC "hit ratio ≥ 80%" và cho bảng before/after.
 *
 * <p><b>Vì sao không đo bằng log</b>: log phải bật DEBUG (3.800 request/s × DEBUG = tự bắn vào
 * chân), còn log mức INFO thì hoặc quá ồn hoặc quá thưa để tính tỉ lệ. Bộ đếm trong bộ nhớ + đọc
 * một lần ở cuối phép đo là cách rẻ và chính xác nhất.
 *
 * <p>Bộ đếm là <b>theo tiến trình</b> và reset khi restart — đủ dùng cho drill (đọc delta trước/sau
 * một lần bắn tải), không phải metric production (Phase 6 mới có).
 */
@Component
public class CacheMetrics {

  private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<>();

  public static final String HIT = "hit";
  public static final String MISS = "miss";
  public static final String DB_LOAD = "dbLoad";
  public static final String LOCK_WIN = "lockWin";
  public static final String LOCK_TIMEOUT = "lockTimeout";
  public static final String BYPASS = "bypass";
  public static final String ERROR = "error";

  public void hit(String kind) {
    inc(kind, HIT);
  }

  public void miss(String kind) {
    inc(kind, MISS);
  }

  public void dbLoad(String kind) {
    inc(kind, DB_LOAD);
  }

  public void lockWin(String kind) {
    inc(kind, LOCK_WIN);
  }

  public void lockTimeout(String kind) {
    inc(kind, LOCK_TIMEOUT);
  }

  public void bypass(String kind) {
    inc(kind, BYPASS);
  }

  /** Lỗi hạ tầng, đếm theo thao tác Redis ({@code get}/{@code put}/…). */
  public void error(String operation) {
    inc("redis", "error:" + operation.toLowerCase(Locale.ROOT));
  }

  private void inc(String kind, String name) {
    counters.computeIfAbsent(kind + "." + name, key -> new LongAdder()).increment();
  }

  /** Snapshot phẳng {@code {"detail.hit": 123, ...}} — in ra JSON của endpoint debug. */
  public ConcurrentMap<String, Long> snapshot() {
    ConcurrentMap<String, Long> out = new ConcurrentHashMap<>();
    counters.forEach((key, adder) -> out.put(key, adder.sum()));
    return out;
  }

  /** Tổng hợp đọc được bằng mắt: hit ratio + tổng số lần đọc DB. */
  public Summary summary() {
    long hits = 0;
    long misses = 0;
    long dbLoads = 0;
    long lockTimeouts = 0;
    long bypasses = 0;
    long errors = 0;
    for (String kind : new String[] {"detail", "list", "sku", "categories"}) {
      hits += value(kind, HIT);
      misses += value(kind, MISS);
      dbLoads += value(kind, DB_LOAD);
      lockTimeouts += value(kind, LOCK_TIMEOUT);
      bypasses += value(kind, BYPASS);
    }
    for (String key : counters.keySet()) {
      if (key.startsWith("redis.error")) {
        errors += counters.get(key).sum();
      }
    }
    long lookups = hits + misses;
    double hitRatio = lookups == 0 ? 0.0 : (double) hits / lookups;
    return new Summary(hits, misses, hitRatio, dbLoads, lockTimeouts, bypasses, errors);
  }

  private long value(String kind, String name) {
    LongAdder adder = counters.get(kind + "." + name);
    return adder == null ? 0L : adder.sum();
  }

  /** Số tổng hợp cho {@code GET /debug/cache}. */
  public record Summary(
      long hits,
      long misses,
      double hitRatio,
      long dbLoads,
      long lockTimeouts,
      long bypasses,
      long errors) {}

  /** Tiện cho test/log: đọc số của một loại + một tên đếm. */
  public long of(String kind, String name) {
    return value(kind, name);
  }
}
