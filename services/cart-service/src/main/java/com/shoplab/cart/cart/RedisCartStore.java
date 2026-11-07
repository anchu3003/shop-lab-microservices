package com.shoplab.cart.cart;

import com.shoplab.cart.config.CartProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cài đặt {@link CartStore} trên Redis HASH + <b>Lua</b>.
 *
 * <p><b>Vì sao Lua mà không phải đọc/ghi từ Java:</b> một dòng giỏ là JSON ({@code qty} + {@code
 * addedAt}) nên {@code HINCRBY} không dùng được. Nếu đọc ở Java rồi ghi lại thì hai request song
 * song cùng SKU đọc cùng giá trị cũ ⇒ cái ghi sau đè cái trước (<b>lost update</b>) — với giỏ hàng,
 * khách bấm "+" 2 lần nhanh sẽ mất 1 lần. Lua chạy bên trong Redis nên read-modify-write là một
 * bước duy nhất.
 *
 * <p>Bất biến {@code 1..maxQty} và {@code maxLines} được kiểm <b>trong Lua</b>, không kiểm ở Java
 * rồi ghi (kiểm ngoài = TOCTOU: đúng lúc đọc, sai lúc ghi).
 */
@Repository
public class RedisCartStore implements CartStore {

  /** Đặt số lượng tuyệt đối; trả -1 nếu giỏ đã đầy dòng. */
  private static final DefaultRedisScript<Long> SET_ITEM =
      new DefaultRedisScript<>(
          """
          local key = KEYS[1]
          local sku = ARGV[1]
          local qty = tonumber(ARGV[2])
          local maxLines = tonumber(ARGV[3])
          local ttl = tonumber(ARGV[4])
          local now = ARGV[5]
          local existing = redis.call('HGET', key, sku)
          if (not existing) and redis.call('HLEN', key) >= maxLines then
            return -1
          end
          local addedAt = now
          if existing then
            local ok, decoded = pcall(cjson.decode, existing)
            if ok and decoded.addedAt then addedAt = decoded.addedAt end
          end
          redis.call('HSET', key, sku, cjson.encode({qty = qty, addedAt = addedAt}))
          redis.call('EXPIRE', key, ttl)
          return qty
          """,
          Long.class);

  /** Cộng dồn giỏ guest vào giỏ user (cap maxQty), xoá giỏ guest, trả số dòng đã cộng. */
  private static final DefaultRedisScript<Long> MERGE =
      new DefaultRedisScript<>(
          """
          local userKey = KEYS[1]
          local guestKey = KEYS[2]
          local ttl = tonumber(ARGV[1])
          local cap = tonumber(ARGV[2])
          local maxLines = tonumber(ARGV[3])
          local now = ARGV[4]
          local merged = 0
          for _, sku in ipairs(redis.call('HKEYS', guestKey)) do
            local rawGuest = redis.call('HGET', guestKey, sku)
            local okGuest, guest = pcall(cjson.decode, rawGuest)
            if okGuest and guest and guest.qty then
              local rawUser = redis.call('HGET', userKey, sku)
              local allowed = true
              local baseQty = 0
              local addedAt = now
              if rawUser then
                local okUser, user = pcall(cjson.decode, rawUser)
                if okUser and user then
                  if user.qty then baseQty = user.qty end
                  if user.addedAt then addedAt = user.addedAt end
                end
              elseif redis.call('HLEN', userKey) >= maxLines then
                allowed = false
              end
              if allowed then
                local total = baseQty + guest.qty
                if total > cap then total = cap end
                if total < 1 then total = 1 end
                redis.call('HSET', userKey, sku, cjson.encode({qty = total, addedAt = addedAt}))
                merged = merged + 1
              end
            end
          end
          if merged > 0 then redis.call('EXPIRE', userKey, ttl) end
          redis.call('DEL', guestKey)
          return merged
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final CartProperties properties;
  private final ObjectMapper objectMapper;

  public RedisCartStore(
      StringRedisTemplate redis, CartProperties properties, ObjectMapper objectMapper) {
    this.redis = redis;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public Snapshot snapshot(CartOwner owner) {
    String key = cartKey(owner);
    Map<Object, Object> raw = redis.opsForHash().entries(key);
    Map<UUID, CartItem> items = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : raw.entrySet()) {
      UUID skuId = UUID.fromString(entry.getKey().toString());
      JsonNode node = objectMapper.readTree(entry.getValue().toString());
      items.put(
          skuId,
          new CartItem(
              skuId, node.get("qty").asInt(), Instant.parse(node.get("addedAt").asString())));
    }
    Long ttl = redis.getExpire(key);
    return new Snapshot(items, ttl == null ? -1 : ttl);
  }

  @Override
  public boolean setItem(CartOwner owner, UUID skuId, int qty, Instant now) {
    Long result =
        redis.execute(
            SET_ITEM,
            java.util.List.of(cartKey(owner)),
            skuId.toString(),
            Integer.toString(qty),
            Integer.toString(properties.maxLines()),
            Long.toString(properties.ttlSeconds()),
            now.toString());
    return result != null && result >= 0;
  }

  @Override
  public void removeItem(CartOwner owner, UUID skuId) {
    redis.opsForHash().delete(cartKey(owner), skuId.toString());
  }

  @Override
  public void clear(CartOwner owner) {
    redis.delete(cartKey(owner));
  }

  @Override
  public void issueGuestToken(String token, Instant now) {
    redis.opsForValue().set(guestKey(token), "1", properties.ttl());
  }

  @Override
  public boolean isIssuedGuestToken(String token) {
    return Boolean.TRUE.equals(redis.hasKey(guestKey(token)));
  }

  @Override
  public void revokeGuestToken(String token) {
    redis.delete(guestKey(token));
  }

  @Override
  public int mergeGuestIntoUser(CartOwner user, CartOwner guest, Instant now) {
    Long merged =
        redis.execute(
            MERGE,
            java.util.List.of(cartKey(user), cartKey(guest)),
            Long.toString(properties.ttlSeconds()),
            Integer.toString(properties.maxQty()),
            Integer.toString(properties.maxLines()),
            now.toString());
    return merged == null ? 0 : merged.intValue();
  }

  private String cartKey(CartOwner owner) {
    return properties.keyPrefix() + ":" + owner.scope();
  }

  /** Key đánh dấu "guest token này do server phát" — tách khỏi key giỏ. */
  private String guestKey(String token) {
    return properties.keyPrefix() + ":guest:" + token;
  }
}
