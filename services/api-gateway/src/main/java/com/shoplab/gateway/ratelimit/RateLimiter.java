package com.shoplab.gateway.ratelimit;

import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;

/**
 * Trừ token khỏi token bucket.
 *
 * <p>Tách interface để filter test được <b>quyết định</b> (429 hay không, header nào) mà không cần
 * Redis; thuật toán token bucket thật được kiểm ở IT với Redis thật (Lua chỉ chạy đúng trên Redis —
 * mock Lua là test giả).
 */
public interface RateLimiter {

  /**
   * Trừ 1 token ở mọi xô liên quan — <b>nguyên tử</b>: nếu một xô cạn thì <b>không</b> xô nào bị
   * trừ (nếu trừ trước rồi mới biết xô kia cạn thì user bị trừ oan token cho request không được
   * phục vụ).
   *
   * @param userKey key xô theo user; {@code null} = request không có danh tính ⇒ chỉ dùng xô IP
   * @param ipKey key xô theo IP (luôn có)
   * @param nowMs mốc thời gian (ms) do caller truyền vào — để test/drill tất định và để cả cụm
   *     gateway dùng cùng một cách tính thời gian
   * @throws RateLimitUnavailableException không nói chuyện được với Redis
   */
  RateLimitDecision consume(
      String userKey, Limit userLimit, String ipKey, Limit ipLimit, long nowMs);
}
