package com.shoplab.cart.cart;

import jakarta.validation.constraints.NotNull;

/**
 * Body của {@code PUT /v1/carts/items/{skuId}}.
 *
 * <p>Số lượng là <b>tuyệt đối</b> (set), không phải cộng thêm: nhờ vậy request là idempotent —
 * client retry sau timeout không làm số lượng nhảy 2 lần. Cận trên kiểm ở service (đọc từ config),
 * không hard-code trong annotation.
 */
public record SetItemRequest(@NotNull Integer qty) {}
