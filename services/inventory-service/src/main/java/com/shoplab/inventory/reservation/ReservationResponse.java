package com.shoplab.inventory.reservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reservation trả ra ngoài. */
public record ReservationResponse(
    UUID reservationId,
    UUID orderId,
    ReservationStatus status,
    Instant expiresAt,
    List<Item> items) {

  public record Item(UUID skuId, int qty) {}

  public static ReservationResponse from(
      ReservationRecord reservation, List<ReservationItemRecord> items) {
    return new ReservationResponse(
        reservation.id(),
        reservation.orderId(),
        reservation.status(),
        reservation.expiresAt(),
        items.stream().map(item -> new Item(item.skuId(), item.qty())).toList());
  }
}
