package com.shoplab.gateway.ratelimit;

/**
 * Kết quả một lần trừ token.
 *
 * @param allowed còn token ở <b>mọi</b> xô liên quan (user và/hoặc IP) ⇒ cho qua
 * @param scope xô quyết định — xô chặt nhất khi cho qua, xô cạn khi chặn. Dùng để ghi header nào:
 *     trả số của {@code per-user limit} hay {@code per-ip limit}
 * @param limit kích thước xô của {@code scope} (giá trị cho header {@code X-RateLimit-Limit})
 * @param remaining token còn lại của {@code scope} — đã tính cả refill, làm tròn xuống
 * @param retryAfterMs thời gian tới khi có lại 1 token; {@code 0} khi được qua
 */
public record RateLimitDecision(
    boolean allowed, Bucket scope, int limit, int remaining, long retryAfterMs) {

  /** Xô đã quyết định. */
  public enum Bucket {
    USER,
    IP
  }

  /** Số giây cho header {@code Retry-After} (làm tròn lên, tối thiểu 1). */
  public long retryAfterSeconds() {
    if (retryAfterMs <= 0) {
      return 0;
    }
    return Math.max(1, (retryAfterMs + 999) / 1000);
  }
}
