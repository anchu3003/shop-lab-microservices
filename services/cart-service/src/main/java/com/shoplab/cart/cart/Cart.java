package com.shoplab.cart.cart;

import java.util.List;

/**
 * Toàn bộ giỏ của một owner.
 *
 * <p>Giỏ rỗng là trạng thái <b>hợp lệ</b> (200 với {@code items=[]}), không phải 404: khách vừa xoá
 * hết hàng hoặc vừa merge xong vẫn phải đọc được giỏ.
 *
 * @param ttlSeconds thời gian sống còn lại của key Redis — client biết giỏ sắp bị dọn
 */
public record Cart(String owner, List<CartItem> items, int totalQty, long ttlSeconds) {

  public static Cart empty(String owner, long ttlSeconds) {
    return new Cart(owner, List.of(), 0, ttlSeconds);
  }
}
