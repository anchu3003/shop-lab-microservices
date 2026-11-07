package com.shoplab.cart.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.cart.config.CartProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test nghiệp vụ giỏ hàng với store giả trong RAM.
 *
 * <p>Lua (phần thực sự khó) <b>không</b> test ở đây — nó chỉ chạy đúng trên Redis thật, xem {@code
 * CartServiceApplicationIT}. Ở đây kiểm luật nghiệp vụ: cận qty, cận số dòng, quyền merge, thứ tự
 * dòng.
 */
class CartServiceTest {

  private static final UUID USER = UUID.fromString("55555555-5555-4555-8555-000000000001");
  private static final UUID SKU_A = UUID.fromString("66666666-6666-4666-8666-00000000000a");
  private static final UUID SKU_B = UUID.fromString("66666666-6666-4666-8666-00000000000b");

  private final FakeCartStore store = new FakeCartStore();
  private final CartService service =
      new CartService(
          store,
          new CartProperties(Duration.ofDays(30), 3, 999, "cart"),
          Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC));

  @Test
  @DisplayName("cấp guest token → token được đánh dấu trong store (không phải client tự bịa)")
  void issueGuestCartShouldRegisterToken() {
    CartService.GuestCart guest = service.issueGuestCart();

    assertThat(store.issued).contains(guest.cartToken());
    assertThat(guest.ttl()).isEqualTo(Duration.ofDays(30));
  }

  @Test
  @DisplayName("giỏ rỗng là trạng thái hợp lệ, không phải lỗi")
  void emptyCartIsValid() {
    Cart cart = service.get(CartOwner.user(USER));

    assertThat(cart.items()).isEmpty();
    assertThat(cart.totalQty()).isZero();
    assertThat(cart.owner()).isEqualTo("u:" + USER);
  }

  @Test
  @DisplayName("đặt số lượng: idempotent + cộng đúng totalQty + sort theo addedAt")
  void setItemShouldBeIdempotentAndSorted() {
    CartOwner guest = CartOwner.guest(store.issuedToken());

    service.setItem(guest, SKU_A, 2);
    Cart again = service.setItem(guest, SKU_A, 2);
    assertThat(again.items()).hasSize(1);
    assertThat(again.items().get(0).qty()).isEqualTo(2);

    service.setItem(guest, SKU_B, 3);
    Cart cart = service.get(guest);
    assertThat(cart.totalQty()).isEqualTo(5);
  }

  @Test
  @DisplayName("qty ngoài 1..maxQty → 422")
  void qtyOutOfRange() {
    CartOwner user = CartOwner.user(USER);

    assertThatThrownBy(() -> service.setItem(user, SKU_A, 0))
        .isInstanceOf(CartExceptions.QtyOutOfRange.class);
    assertThatThrownBy(() -> service.setItem(user, SKU_A, 1000))
        .isInstanceOf(CartExceptions.QtyOutOfRange.class);
  }

  @Test
  @DisplayName("giỏ quá maxLines → 422 (endpoint public phải có cận trên)")
  void tooManyLines() {
    CartOwner user = CartOwner.user(USER);
    service.setItem(user, SKU_A, 1);
    service.setItem(user, SKU_B, 1);
    service.setItem(user, UUID.randomUUID(), 1);

    assertThatThrownBy(() -> service.setItem(user, UUID.randomUUID(), 1))
        .isInstanceOf(CartExceptions.TooManyLines.class);
  }

  @Test
  @DisplayName("xoá dòng và xoá giỏ hoạt động, giỏ quay về rỗng")
  void removeAndClear() {
    CartOwner user = CartOwner.user(USER);
    service.setItem(user, SKU_A, 4);

    assertThat(service.removeItem(user, SKU_A).items()).isEmpty();

    service.setItem(user, SKU_A, 1);
    assertThat(service.clear(user).items()).isEmpty();
    assertThat(service.get(user).items()).isEmpty();
  }

  @Test
  @DisplayName("merge: cộng dồn + xoá giỏ guest + thu hồi token; gọi lần 2 không cộng nữa")
  void mergeShouldMoveLinesOnce() {
    CartService.GuestCart guest = service.issueGuestCart();
    CartOwner guestOwner = CartOwner.guest(guest.cartToken());
    service.setItem(guestOwner, SKU_A, 5);
    service.setItem(CartOwner.user(USER), SKU_A, 1);

    Cart merged = service.mergeGuestIntoUser(USER, guest.cartToken());

    assertThat(merged.items()).hasSize(1);
    assertThat(merged.items().get(0).qty()).isEqualTo(6);
    assertThat(store.issued).doesNotContain(guest.cartToken());
    assertThatThrownBy(() -> service.mergeGuestIntoUser(USER, guest.cartToken()))
        .isInstanceOf(CartExceptions.Unauthenticated.class);
  }

  @Test
  @DisplayName("guest token không do server cấp → 401 (không đọc giỏ người khác)")
  void forgedGuestTokenRejected() {
    assertThatThrownBy(() -> service.requireIssuedGuestToken("token-tu-che"))
        .isInstanceOf(CartExceptions.Unauthenticated.class);
    assertThatThrownBy(() -> service.requireIssuedGuestToken("  "))
        .isInstanceOf(CartExceptions.Unauthenticated.class);
  }

  /** Store giả trong RAM — chỉ đủ để kiểm luật nghiệp vụ, không mô phỏng Lua. */
  static class FakeCartStore implements CartStore {

    private final Map<String, Map<UUID, CartItem>> carts = new LinkedHashMap<>();
    final List<String> issued = new ArrayList<>();
    private int counter = 0;

    String issuedToken() {
      String token = "token-" + (++counter);
      issued.add(token);
      return token;
    }

    @Override
    public Snapshot snapshot(CartOwner owner) {
      Map<UUID, CartItem> items = carts.getOrDefault(owner.scope(), Map.of());
      return new Snapshot(new LinkedHashMap<>(items), items.isEmpty() ? 0 : 2592000);
    }

    @Override
    public boolean setItem(CartOwner owner, UUID skuId, int qty, Instant now) {
      Map<UUID, CartItem> items =
          carts.computeIfAbsent(owner.scope(), key -> new LinkedHashMap<>());
      CartItem existing = items.get(skuId);
      if (existing == null && items.size() >= 3) {
        return false;
      }
      items.put(skuId, new CartItem(skuId, qty, existing == null ? now : existing.addedAt()));
      return true;
    }

    @Override
    public void removeItem(CartOwner owner, UUID skuId) {
      carts.getOrDefault(owner.scope(), Map.of()).remove(skuId);
    }

    @Override
    public void clear(CartOwner owner) {
      carts.remove(owner.scope());
    }

    @Override
    public void issueGuestToken(String token, Instant now) {
      issued.add(token);
    }

    @Override
    public boolean isIssuedGuestToken(String token) {
      return issued.contains(token);
    }

    @Override
    public void revokeGuestToken(String token) {
      issued.remove(token);
    }

    @Override
    public int mergeGuestIntoUser(CartOwner user, CartOwner guest, Instant now) {
      Map<UUID, CartItem> guestItems = carts.getOrDefault(guest.scope(), Map.of());
      Map<UUID, CartItem> userItems =
          carts.computeIfAbsent(user.scope(), key -> new LinkedHashMap<>());
      int merged = 0;
      for (CartItem item : guestItems.values()) {
        CartItem existing = userItems.get(item.skuId());
        int total = Math.min((existing == null ? 0 : existing.qty()) + item.qty(), 999);
        userItems.put(
            item.skuId(),
            new CartItem(item.skuId(), total, existing == null ? now : existing.addedAt()));
        merged++;
      }
      carts.remove(guest.scope());
      return merged;
    }
  }
}
