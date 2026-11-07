package com.shoplab.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Đơn hàng trả ra ngoài — kèm item snapshot (đọc đơn không gọi service khác). */
public record OrderResponse(
    UUID id,
    UUID userId,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    List<Line> items,
    Instant createdAt,
    Instant updatedAt) {

  public record Line(
      UUID skuId,
      String skuCode,
      String title,
      BigDecimal unitPrice,
      int qty,
      BigDecimal lineTotal) {}

  public static OrderResponse from(OrderRecord order, List<OrderItemRecord> items) {
    return new OrderResponse(
        order.id(),
        order.userId(),
        order.status(),
        order.subtotal(),
        order.discount(),
        order.total(),
        order.currency(),
        items.stream()
            .map(
                item ->
                    new Line(
                        item.skuId(),
                        item.skuCode(),
                        item.title(),
                        item.unitPrice(),
                        item.qty(),
                        item.lineTotal()))
            .toList(),
        order.createdAt(),
        order.updatedAt());
  }
}
