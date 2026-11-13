package com.shoplab.gateway.ratelimit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Đếm chuyện gì đã xảy ra ở cửa — <b>không</b> log từng request.
 *
 * <p>500 VU bị chặn mà log INFO mỗi request là 500 dòng/giây: log thành nguồn gây chậm thứ hai, và
 * người đọc log thật (người) sẽ bỏ qua luôn cả log quan trọng. Vì vậy: đếm vào bộ đếm, log WARN
 * <b>theo cửa sổ</b> 5 s, và cho xem số qua {@code GET /debug/rate-limit}.
 */
@Component
public class RateLimitMetrics {

  /** Cửa sổ chống ngập log — 5 s/lần là đủ để biết "đang bị chặn" mà không thành tiếng ồn. */
  static final long WARN_WINDOW_MS = 5_000L;

  private final AtomicLong allowed = new AtomicLong();
  private final AtomicLong rejected = new AtomicLong();
  private final AtomicLong bypassed = new AtomicLong();
  private final AtomicLong failOpen = new AtomicLong();

  private final AtomicLong lastRejectWarnAt = new AtomicLong();
  private final AtomicLong lastFailOpenWarnAt = new AtomicLong();

  public void noteAllowed() {
    allowed.incrementAndGet();
  }

  public void noteBypassed() {
    bypassed.incrementAndGet();
  }

  /** Tăng bộ đếm 429; trả {@code true} nếu lần này được phép log WARN (chống ngập). */
  public boolean noteRejected(long nowMs) {
    rejected.incrementAndGet();
    return shouldLog(lastRejectWarnAt, nowMs);
  }

  /** Tăng bộ đếm fail-open; trả {@code true} nếu lần này được phép log WARN. */
  public boolean noteFailOpen(long nowMs) {
    failOpen.incrementAndGet();
    return shouldLog(lastFailOpenWarnAt, nowMs);
  }

  /** Ảnh chụp bộ đếm cho {@code /debug/rate-limit} (và cho drill đọc bằng máy). */
  public Map<String, Object> snapshot() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("allowed", allowed.get());
    out.put("rejected", rejected.get());
    out.put("bypassed", bypassed.get());
    out.put("failOpen", failOpen.get());
    return out;
  }

  /**
   * Chỉ một thread được log trong mỗi cửa sổ: {@code CAS} trên mốc thời gian, thread thua thì im.
   * Dùng {@code 0} làm mốc khởi tạo (không phải {@code Long.MIN_VALUE} — hiệu với {@code now} sẽ
   * tràn số và lần log đầu tiên không bao giờ xảy ra).
   */
  private static boolean shouldLog(AtomicLong lastAt, long nowMs) {
    long previous = lastAt.get();
    if (previous != 0 && nowMs - previous < WARN_WINDOW_MS) {
      return false;
    }
    return lastAt.compareAndSet(previous, nowMs);
  }
}
