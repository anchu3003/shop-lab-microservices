package com.shoplab.gateway.ratelimit;

import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Token bucket trên Redis, toàn bộ thuật toán nằm trong <b>một</b> Lua script.
 *
 * <p><b>Vì sao phải là Lua</b>: đọc {@code tokens}/{@code ts} ở Java rồi ghi lại là
 * read-modify-write hai bước — hai request song song cùng đọc "còn 1 token" và cả hai đều được qua
 * (đúng loại lỗi rate limit không được phép có). Lua chạy bên trong Redis nên kiểm-và-trừ là một
 * bước duy nhất.
 *
 * <p><b>Vì sao token bucket, không phải fixed window</b>: fixed window ({@code INCR} + {@code
 * PEXPIRE}) cho phép gửi gấp đôi hạn mức ở ranh giới hai cửa sổ — đúng lúc flash sale mở bán thì đó
 * là lỗ hổng, không phải chi tiết nhỏ. Token bucket cho burst <b>có trần</b> ({@code burst}) và tốc
 * độ nạp <b>đều</b> ({@code rps}).
 *
 * <p><b>Vì sao "kiểm hết rồi mới trừ"</b>: với 2 xô (user + IP), nếu trừ user trước rồi mới phát
 * hiện IP đã cạn thì user mất token cho một request không được phục vụ — cạn dần dù không gửi gì.
 * Script kiểm cả hai trước, chỉ trừ khi mọi xô đều còn token.
 *
 * <p>{@code now} do caller truyền (không dùng {@code TIME} của Redis) để test và drill tất định;
 * đánh đổi là nhiều instance lệch đồng hồ thì biên burst lệch vài ms — chấp nhận được.
 */
@Component
public class RedisTokenBucket implements RateLimiter {

  /**
   * KEYS: các xô cần kiểm (1 = chỉ IP, 2 = user + IP). ARGV: {@code nowMs}, rồi cặp {@code
   * rate,burst} cho từng KEYS theo đúng thứ tự.
   *
   * <p>Trả về {@code {allowed, retryAfterMs, remaining_1, ..., remaining_n}}.
   */
  private static final String CONSUME_LUA =
      """
      local now = tonumber(ARGV[1])
      local n = #KEYS
      local before = {}
      local rejected = false
      local retryMs = 0

      for i = 1, n do
        local rate = tonumber(ARGV[2 * i])
        local burst = tonumber(ARGV[2 * i + 1])
        local stored = redis.call('HMGET', KEYS[i], 'tokens', 'ts')
        local tokens = tonumber(stored[1])
        local ts = tonumber(stored[2])
        if tokens == nil then tokens = burst end
        if ts == nil then ts = now end
        local elapsed = now - ts
        if elapsed < 0 then elapsed = 0 end
        local refilled = tokens + elapsed / 1000 * rate
        if refilled > burst then refilled = burst end
        before[i] = refilled
        if refilled < 1 then
          rejected = true
          local wait = math.ceil((1 - refilled) / rate * 1000)
          if wait > retryMs then retryMs = wait end
        end
      end

      if not rejected then
        for i = 1, n do
          local rate = tonumber(ARGV[2 * i])
          local burst = tonumber(ARGV[2 * i + 1])
          local ttl = math.ceil(burst / rate * 1000)
          if ttl < 1000 then ttl = 1000 end
          redis.call('HSET', KEYS[i], 'tokens', before[i] - 1, 'ts', now)
          redis.call('PEXPIRE', KEYS[i], ttl)
          before[i] = before[i] - 1
        end
      end

      local result = { rejected and 0 or 1, retryMs }
      for i = 1, n do
        result[#result + 1] = math.floor(before[i])
      end
      return result
      """;

  /**
   * {@code DefaultRedisScript<List>} buộc phải raw type: Spring Data chỉ nhận {@code Class<T>} và
   * {@code List.class} không mang generic. Ép kiểu từng phần tử ở dưới nên không có rủi ro runtime.
   */
  @SuppressWarnings("rawtypes")
  private static final DefaultRedisScript<List> CONSUME =
      new DefaultRedisScript<>(CONSUME_LUA, List.class);

  private final StringRedisTemplate redis;

  public RedisTokenBucket(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public RateLimitDecision consume(
      String userKey, Limit userLimit, String ipKey, Limit ipLimit, long nowMs) {

    boolean hasUser = userKey != null;
    List<String> keys = hasUser ? List.of(userKey, ipKey) : List.of(ipKey);

    List<Object> args = new ArrayList<>(1 + 2 * keys.size());
    args.add(Long.toString(nowMs));
    if (hasUser) {
      args.add(Integer.toString(userLimit.rps()));
      args.add(Integer.toString(userLimit.burst()));
    }
    args.add(Integer.toString(ipLimit.rps()));
    args.add(Integer.toString(ipLimit.burst()));

    List<?> raw;
    try {
      raw = redis.execute(CONSUME, keys, args.toArray());
    } catch (DataAccessException e) {
      // Gồm cả RedisConnectionFailure (Redis chết) và timeout: với rate limit, cả hai đều là "không
      // kiểm được" ⇒ để caller quyết fail-open, không quyết ở đây.
      throw new RateLimitUnavailableException("gọi Redis để trừ token thất bại", e);
    }

    if (raw == null || raw.size() < 2) {
      throw new RateLimitUnavailableException("Lua trả về rỗng/thiếu phần tử: " + raw, null);
    }

    boolean allowed = ((Number) raw.get(0)).longValue() == 1L;
    long retryAfterMs = ((Number) raw.get(1)).longValue();
    int userRemaining = hasUser ? ((Number) raw.get(2)).intValue() : -1;
    int ipRemaining = ((Number) raw.get(hasUser ? 3 : 2)).intValue();

    if (!allowed) {
      boolean userIsCulprit = hasUser && userRemaining < 1;
      return userIsCulprit
          ? new RateLimitDecision(
              false, RateLimitDecision.Bucket.USER, userLimit.burst(), userRemaining, retryAfterMs)
          : new RateLimitDecision(
              false, RateLimitDecision.Bucket.IP, ipLimit.burst(), ipRemaining, retryAfterMs);
    }

    // Cả hai xô đều cho qua ⇒ báo cáo xô "chặt" hơn (tỉ lệ còn lại thấp hơn) để client thấy sớm là
    // sắp bị chặn, thay vì báo xô còn nhiều rồi bất ngờ 429.
    boolean userIsTighter =
        hasUser && ratio(userRemaining, userLimit.burst()) <= ratio(ipRemaining, ipLimit.burst());
    return userIsTighter
        ? new RateLimitDecision(
            true, RateLimitDecision.Bucket.USER, userLimit.burst(), userRemaining, 0)
        : new RateLimitDecision(true, RateLimitDecision.Bucket.IP, ipLimit.burst(), ipRemaining, 0);
  }

  private static double ratio(int remaining, int burst) {
    return burst <= 0 ? 0 : (double) remaining / burst;
  }
}
