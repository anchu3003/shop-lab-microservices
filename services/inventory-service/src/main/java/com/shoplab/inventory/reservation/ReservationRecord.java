package com.shoplab.inventory.reservation;

import java.time.Instant;
import java.util.UUID;

/** Header của một reservation (1 row/order — xem {@link ReservationItemRecord}). */
public record ReservationRecord(
    UUID id, UUID orderId, ReservationStatus status, Instant expiresAt, Instant createdAt) {}
