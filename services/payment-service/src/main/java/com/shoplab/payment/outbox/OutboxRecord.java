package com.shoplab.payment.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Một dòng outbox đã được relay claim (SL-3.5) — chép khuôn {@code SL-3.3} của order-service. */
public record OutboxRecord(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    Map<String, Object> payload,
    OutboxStatus status,
    int attempts,
    String lastError,
    Instant claimedAt,
    Instant createdAt,
    Instant sentAt) {}
