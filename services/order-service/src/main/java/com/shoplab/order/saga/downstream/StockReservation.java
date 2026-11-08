package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Chỗ hàng đã giữ ở `inventory-service` (`POST /v1/stock/reserve`) — chỉ cần `reservationId` để
 * commit (`SL-2.6b`) hoặc release (bù).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockReservation(UUID reservationId) {}
