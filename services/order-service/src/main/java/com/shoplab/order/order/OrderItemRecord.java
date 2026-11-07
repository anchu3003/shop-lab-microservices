package com.shoplab.order.order;

import java.math.BigDecimal;
import java.util.UUID;

/** Một dòng hàng của đơn — <b>snapshot</b> tại thời điểm đặt (ADR-0006 D3). */
public record OrderItemRecord(
    UUID id,
    UUID skuId,
    String skuCode,
    String title,
    BigDecimal unitPrice,
    String currency,
    int qty,
    BigDecimal lineTotal) {}
