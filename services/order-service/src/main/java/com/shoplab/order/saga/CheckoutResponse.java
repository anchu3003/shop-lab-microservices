package com.shoplab.order.saga;

import com.shoplab.order.order.OrderStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả bước **start** của checkout.
 *
 * <p><b>Vì sao trả cả tiền</b>: client cần hiển thị "bạn trả bao nhiêu" ngay sau khi đặt — và con
 * số đó phải là con số server đã ghi vào `orders`, không phải client tự tính.
 *
 * @param paymentRequired {@code false} khi đơn 0 đồng (voucher 100%) ⇒ không có payment intent,
 *     {@code paymentIntentId}/{@code paymentUrl} là {@code null}; `SL-2.6b` sẽ chốt đơn không cần
 *     verify payment
 */
public record CheckoutResponse(
    UUID orderId,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    boolean paymentRequired,
    UUID paymentIntentId,
    String paymentUrl) {}
