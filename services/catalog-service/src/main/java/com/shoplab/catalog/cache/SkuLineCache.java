package com.shoplab.catalog.cache;

import com.shoplab.catalog.sku.SkuLookupResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Cache cho {@code GET /v1/skus?ids=} — cache <b>theo từng SKU</b>, không theo tổ hợp request
 * (ADR-0009 D3).
 *
 * <p><b>Vì sao không cache theo tổ hợp</b>: endpoint nhận tới 100 id ⇒ số tổ hợp là vô hạn, còn giỏ
 * hàng thật mỗi lần một khác ⇒ cache theo tổ hợp gần như <b>luôn miss</b> (hit ratio ~0) dù cùng
 * 100 SKU được hỏi đi hỏi lại. Cache theo từng SKU thì 1 {@code MGET} phục vụ mọi tổ hợp.
 *
 * <p>Sentinel {@code "null"} cho SKU <b>không mua được</b> (archived / product đã xoá mềm): client
 * hỏi id sai hoặc id cũ không đập DB mỗi lần. Đây là negative caching có chủ ý, TTL bằng TTL chung
 * (≤ 66 s) để SKU được mở bán lại không bị "ẩn" quá lâu.
 */
@Component
public class SkuLineCache {

  private static final Logger LOG = LoggerFactory.getLogger(SkuLineCache.class);

  private static final String NOT_BUYABLE = "null";

  private final CacheStore store;
  private final CacheKeys keys;
  private final CacheProperties properties;
  private final CacheMetrics metrics;
  private final ObjectMapper objectMapper;
  private final JavaType lineType;

  public SkuLineCache(
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
    this.lineType = objectMapper.getTypeFactory().constructType(SkuLookupResponse.SkuLine.class);
  }

  /**
   * Kết quả một lần tra: dòng bán được theo id + tập id <b>không</b> mua được (không tồn tại /
   * archived / product đã xoá mềm).
   */
  public record Batch(Map<UUID, SkuLookupResponse.SkuLine> lines, Set<UUID> notBuyable) {}

  /**
   * Lấy dòng SKU từ cache, phần thiếu thì gọi {@code loader} (DB) và điền lại.
   *
   * @param ids id cần tra (đã validate ở {@code SkuLookupQuery}, không trùng)
   * @param loader nguồn sự thật cho <b>tập id còn thiếu</b>
   */
  public Batch load(List<UUID> ids, Function<List<UUID>, Batch> loader) {
    if (ids.isEmpty()) {
      return new Batch(Map.of(), Set.of());
    }
    if (!properties.enabled()) {
      metrics.bypass("sku");
      return loader.apply(ids);
    }

    Map<UUID, SkuLookupResponse.SkuLine> lines = new HashMap<>();
    Set<UUID> notBuyable = new HashSet<>();
    List<UUID> missing = new ArrayList<>();

    List<Optional<String>> cached;
    try {
      cached = store.getAll(ids.stream().map(keys::sku).toList());
    } catch (CacheUnavailableException ex) {
      metrics.bypass("sku");
      return loader.apply(ids);
    }
    for (int i = 0; i < ids.size(); i++) {
      Optional<String> value = cached.get(i);
      if (value.isEmpty()) {
        missing.add(ids.get(i));
      } else if (NOT_BUYABLE.equals(value.get())) {
        notBuyable.add(ids.get(i));
        metrics.hit("sku");
      } else {
        SkuLookupResponse.SkuLine decoded = decodeLine(value.get());
        if (decoded == null) {
          missing.add(ids.get(i));
        } else {
          lines.put(ids.get(i), decoded);
          metrics.hit("sku");
        }
      }
    }
    if (missing.isEmpty()) {
      return new Batch(lines, notBuyable);
    }
    metrics.miss("sku");
    return fillMissing(lines, notBuyable, missing, loader);
  }

  /**
   * Điền phần thiếu, có lock theo <b>tập</b> id thiếu (không phải từng id): một lần bắn tải với
   * cùng bộ id (giỏ giống nhau) chỉ đập DB một lần thay vì N lần.
   */
  private Batch fillMissing(
      Map<UUID, SkuLookupResponse.SkuLine> lines,
      Set<UUID> notBuyable,
      List<UUID> missing,
      Function<List<UUID>, Batch> loader) {
    String lockKey = keys.lock("sku-batch:" + batchFingerprint(missing));
    String token = UUID.randomUUID().toString();
    boolean won;
    try {
      won = store.lock(lockKey, token, properties.lockTtl());
    } catch (CacheUnavailableException ex) {
      metrics.bypass("sku");
      return merge(lines, notBuyable, loader.apply(missing));
    }

    if (!won) {
      Batch fromWait = awaitBatch(lines, notBuyable, missing);
      if (fromWait != null) {
        return fromWait;
      }
    } else {
      metrics.lockWin("sku");
    }

    try {
      Batch loaded = loader.apply(missing);
      metrics.dbLoad("sku");
      putLines(missing, loaded);
      return merge(lines, notBuyable, loaded);
    } finally {
      if (won) {
        try {
          store.unlock(lockKey, token);
        } catch (CacheUnavailableException ex) {
          LOG.debug("cache: không nhả được lock sku-batch ({})", ex.getMessage());
        }
      }
    }
  }

  /** Chờ người thắng điền cache; quá hạn trả {@code null} để caller tự đọc DB. */
  private Batch awaitBatch(
      Map<UUID, SkuLookupResponse.SkuLine> lines, Set<UUID> notBuyable, List<UUID> missing) {
    long waitMillis = Math.max(0L, properties.lockWait().toMillis());
    long deadline = System.nanoTime() + Duration.ofMillis(waitMillis).toNanos();
    List<String> cacheKeys = missing.stream().map(keys::sku).toList();
    while (System.nanoTime() < deadline) {
      try {
        Thread.sleep(CacheAside.POLL_MILLIS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return null;
      }
      List<Optional<String>> recheck;
      try {
        recheck = store.getAll(cacheKeys);
      } catch (CacheUnavailableException ex) {
        return null;
      }
      Map<UUID, SkuLookupResponse.SkuLine> found = new HashMap<>(lines);
      Set<UUID> notBuyableNow = new HashSet<>(notBuyable);
      boolean complete = true;
      for (int i = 0; i < missing.size(); i++) {
        Optional<String> value = recheck.get(i);
        if (value.isEmpty()) {
          complete = false;
        } else if (NOT_BUYABLE.equals(value.get())) {
          notBuyableNow.add(missing.get(i));
        } else {
          SkuLookupResponse.SkuLine decoded = decodeLine(value.get());
          if (decoded == null) {
            complete = false;
          } else {
            found.put(missing.get(i), decoded);
          }
        }
      }
      if (complete) {
        metrics.hit("sku");
        return new Batch(found, notBuyableNow);
      }
    }
    metrics.lockTimeout("sku");
    return null;
  }

  private void putLines(List<UUID> requested, Batch loaded) {
    Duration base = properties.ttl();
    int jitter = properties.ttlJitterPercent();
    for (UUID id : requested) {
      SkuLookupResponse.SkuLine line = loaded.lines().get(id);
      try {
        String json = line == null ? NOT_BUYABLE : objectMapper.writeValueAsString(line);
        Duration ttl = TtlJitter.jitter(base, jitter, ThreadLocalRandom.current()::nextDouble);
        store.put(keys.sku(id), json, ttl);
      } catch (CacheUnavailableException ex) {
        // Store đã đếm + WARN; response vẫn đúng vì dữ liệu từ DB. Thoát vòng lặp thay vì thử
        // từng key còn lại trên cùng một Redis đang chết.
        return;
      } catch (RuntimeException ex) {
        metrics.error("serialize");
        LOG.warn("cache: không serialize được dòng SKU {} ({})", id, ex.getMessage());
      }
    }
  }

  private SkuLookupResponse.SkuLine decodeLine(String json) {
    try {
      return objectMapper.readValue(json, lineType);
    } catch (RuntimeException ex) {
      metrics.error("deserialize");
      return null;
    }
  }

  private static Batch merge(
      Map<UUID, SkuLookupResponse.SkuLine> lines, Set<UUID> notBuyable, Batch extra) {
    Map<UUID, SkuLookupResponse.SkuLine> merged = new HashMap<>(lines);
    merged.putAll(extra.lines());
    Set<UUID> mergedNotBuyable = new LinkedHashSet<>(notBuyable);
    mergedNotBuyable.addAll(extra.notBuyable());
    return new Batch(merged, mergedNotBuyable);
  }

  /** Fingerprint của tập id thiếu — dùng làm lock key (`sku-batch:<hash>`). */
  private static String batchFingerprint(List<UUID> ids) {
    String joined = ids.stream().sorted().map(UUID::toString).reduce("", (a, b) -> a + b);
    return CacheKeys.sha256Hex(joined).substring(0, 16);
  }
}
