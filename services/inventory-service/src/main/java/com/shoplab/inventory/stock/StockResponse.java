package com.shoplab.inventory.stock;

import java.util.UUID;

/** Dữ liệu tồn kho trả ra ngoài — DTO, không trả thẳng row của DB. */
public record StockResponse(
    UUID skuId, String warehouseId, int onHand, int reserved, int available) {

  public static StockResponse from(StockSnapshot snapshot) {
    return new StockResponse(
        snapshot.skuId(),
        snapshot.warehouseId(),
        snapshot.onHand(),
        snapshot.reserved(),
        snapshot.available());
  }
}
