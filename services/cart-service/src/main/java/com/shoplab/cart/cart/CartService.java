package com.shoplab.cart.cart;

import com.shoplab.cart.config.CartProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Nghiệp vụ giỏ hàng.
 *
 * <p>Ba bất biến:
 *
 * <ol>
 *   <li><b>Mỗi giỏ thuộc đúng một owner</b> ({@code u:<userId>} hoặc {@code g:<guestToken>}) —
 *       không có đường nào để đọc giỏ của người khác nếu không có token/JWT của họ.
 *   <li><b>{@code qty} ∈ 1..maxQty</b> và số dòng ≤ {@code maxLines}; kiểm <b>trong Lua</b> để
 *       không có khe TOCTOU giữa kiểm và ghi.
 *   <li><b>Merge là thao tác một chiều guest → user</b>, cộng dồn và cap ở {@code maxQty}; xoá giỏ
 *       guest sau khi xong ⇒ gọi lại lần 2 không cộng lần nữa (idempotent theo dữ liệu).
 * </ol>
 */
@Service
public class CartService {

  private static final Logger LOG = LoggerFactory.getLogger(CartService.class);

  private final CartStore store;
  private final CartProperties properties;
  private final Clock clock;

  public CartService(CartStore store, CartProperties properties, Clock clock) {
    this.store = store;
    this.properties = properties;
    this.clock = clock;
  }

  /** Cấp guest token mới — đây là "chìa khoá" để khách chưa đăng nhập có giỏ. */
  public GuestCart issueGuestCart() {
    String token = UUID.randomUUID().toString();
    store.issueGuestToken(token, Instant.now(clock));
    LOG.info("guest cart issued: {}", token);
    return new GuestCart(token, properties.ttl());
  }

  /**
   * @param ttl thời gian sống của token (cùng TTL với giỏ)
   */
  public record GuestCart(String cartToken, java.time.Duration ttl) {}

  public Cart get(CartOwner owner) {
    CartStore.Snapshot snapshot = store.snapshot(owner);
    if (snapshot.items().isEmpty()) {
      return Cart.empty(owner.scope(), Math.max(snapshot.ttlSeconds(), 0));
    }
    List<CartItem> items =
        snapshot.items().values().stream()
            .sorted(java.util.Comparator.comparing(CartItem::addedAt))
            .toList();
    int totalQty = items.stream().mapToInt(CartItem::qty).sum();
    return new Cart(owner.scope(), items, totalQty, Math.max(snapshot.ttlSeconds(), 0));
  }

  /** Đặt số lượng tuyệt đối cho một dòng — idempotent, gọi lại cùng qty cho cùng kết quả. */
  public Cart setItem(CartOwner owner, UUID skuId, int qty) {
    if (qty < 1 || qty > properties.maxQty()) {
      throw new CartExceptions.QtyOutOfRange(qty, properties.maxQty());
    }
    if (!store.setItem(owner, skuId, qty, Instant.now(clock))) {
      throw new CartExceptions.TooManyLines(properties.maxLines());
    }
    return get(owner);
  }

  public Cart removeItem(CartOwner owner, UUID skuId) {
    store.removeItem(owner, skuId);
    return get(owner);
  }

  public Cart clear(CartOwner owner) {
    store.clear(owner);
    return Cart.empty(owner.scope(), 0);
  }

  /**
   * Merge giỏ guest vào giỏ user (gọi sau khi login).
   *
   * @throws CartExceptions.Unauthenticated khi guest token không do server cấp (đã bị thu hồi /
   *     bịa)
   */
  public Cart mergeGuestIntoUser(UUID userId, String guestToken) {
    requireIssuedGuestToken(guestToken);
    CartOwner user = CartOwner.user(userId);
    CartOwner guest = CartOwner.guest(guestToken.trim());
    int merged = store.mergeGuestIntoUser(user, guest, Instant.now(clock));
    store.revokeGuestToken(guestToken.trim());
    LOG.info("cart merged: user={} guest={} lines={}", userId, guestToken, merged);
    return get(user);
  }

  /** Kiểm guest token có thật sự do server cấp — nếu không thì 401 (không đọc giỏ người khác). */
  public void requireIssuedGuestToken(String guestToken) {
    if (guestToken == null || guestToken.isBlank()) {
      throw new CartExceptions.Unauthenticated("thiếu header X-Cart-Token");
    }
    String token = guestToken.trim();
    if (!store.isIssuedGuestToken(token)) {
      throw new CartExceptions.Unauthenticated("X-Cart-Token không hợp lệ hoặc đã hết hạn");
    }
  }
}
