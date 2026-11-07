package com.shoplab.payment.idempotency;

import java.time.Instant;

/**
 * Một dòng {@code idempotency_keys}.
 *
 * @param scope phạm vi khoá: {@code "<userId>:<METHOD path>"} — 2 user dùng cùng một key không đụng
 *     nhau
 * @param requestHash SHA-256 của payload: cùng key mà hash khác ⇒ client dùng lại key cho request
 *     khác (422)
 * @param statusCode + {@code responseBody} = response đã trả lần đầu, dùng để <b>replay</b> khi
 *     retry
 */
public record IdempotencyRecord(
    String scope,
    String key,
    String requestHash,
    IdempotencyStatus status,
    Integer statusCode,
    String responseBody,
    java.util.UUID resourceId,
    Instant createdAt,
    Instant completedAt) {}
