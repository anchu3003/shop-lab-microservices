package com.shoplab.inventory.stock;

import java.util.UUID;

/**
 * Ảnh chụp tồn kho của 1 SKU tại 1 kho.
 *
 * @param onHand hàng thật trong kho
 * @param reserved đang giữ chỗ cho đơn chưa thanh toán
 * @param available {@code onHand - reserved} — con số dùng để quyết định còn bán được hay không
 */
public record StockSnapshot(
    UUID skuId, String warehouseId, int onHand, int reserved, int available) {

  public static StockSnapshot of(UUID skuId, String warehouseId, int onHand, int reserved) {
    return new StockSnapshot(skuId, warehouseId, onHand, reserved, onHand - reserved);
  }
}
