package com.shoplab.cart.cart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response của mọi endpoint giỏ.
 *
 * @param owner {@code u:<userId>} hoặc {@code g:<guestToken>} — client biết mình đang đọc giỏ nào
 * @param ttlSeconds thời gian sống còn lại của giỏ (giây); 0 khi giỏ vừa bị xoá
 */
public record CartResponse(String owner, List<LineItem> items, int totalQty, long ttlSeconds) {

  public record LineItem(UUID skuId, int qty, Instant addedAt) {}

  public static CartResponse from(Cart cart) {
    return new CartResponse(
        cart.owner(),
        cart.items().stream()
            .map(item -> new LineItem(item.skuId(), item.qty(), item.addedAt()))
            .toList(),
        cart.totalQty(),
        cart.ttlSeconds());
  }
}
