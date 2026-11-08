package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

/**
 * Giỏ hàng đọc từ `cart-service` (`GET /v1/carts`) — chỉ lấy phần saga cần.
 *
 * <p>{@code ignoreUnknown = true} vì cart trả thêm `owner`, `ttlSeconds`, `addedAt`… Bám vào toàn
 * bộ response của service khác là tự nguyện làm hỏng mình khi họ thêm field (họ có quyền, ta chỉ
 * cần phần ta dùng).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartView(List<CartLine> items) {

  /** Một dòng giỏ: chỉ có {@code skuId} + {@code qty} — **không** có giá (`TD-20`). */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CartLine(UUID skuId, int qty) {}

  public CartView {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
