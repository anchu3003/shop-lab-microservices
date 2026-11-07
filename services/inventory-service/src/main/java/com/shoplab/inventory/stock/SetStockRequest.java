package com.shoplab.inventory.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body của {@code PUT /v1/stock/{skuId}} (admin đặt tồn kho thật).
 *
 * <p>Dùng {@link Integer} (không phải {@code int}) để "thiếu field" trả 400 thay vì âm thầm thành 0
 * — bug im lặng kiểu này rất khó thấy vì "tồn kho 0" trông vẫn hợp lý.
 */
public record SetStockRequest(@NotNull @Min(0) Integer onHand) {}
