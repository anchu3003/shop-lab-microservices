package com.shoplab.order.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payload của event do `order-service` phát (SL-3.3) — **chỉ field consumer cần**, không copy cả
 * đơn.
 *
 * <p>Payload lớn là nợ của tương lai: nó bị giữ trong bảng outbox, trong queue, trong DLQ của 2–3
 * service, và mọi thay đổi schema đơn hàng sẽ kéo theo migration message.
 */
public final class OrderEvents {

  /** `order-service` phát khi order → `CONFIRMED`; `notification-service` là consumer. */
  public static final String AGGREGATE = "order";

  public static final String CONFIRMED = "order.confirmed.v1";

  public static final String CANCELLED = "order.cancelled.v1";

  public static Map<String, Object> confirmed(
      UUID orderId,
      UUID userId,
      BigDecimal total,
      String currency,
      int itemCount,
      Instant confirmedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("orderId", orderId.toString());
    payload.put("userId", userId.toString());
    payload.put("total", total);
    payload.put("currency", currency);
    payload.put("itemCount", itemCount);
    payload.put("confirmedAt", confirmedAt.toString());
    return payload;
  }

  public static Map<String, Object> cancelled(
      UUID orderId, UUID userId, String reason, Instant cancelledAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("orderId", orderId.toString());
    payload.put("userId", userId.toString());
    payload.put("reason", reason);
    payload.put("cancelledAt", cancelledAt.toString());
    return payload;
  }

  private OrderEvents() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
