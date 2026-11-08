package com.shoplab.order.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Một dòng outbox — "event cần phát" được ghi **cùng transaction** với thay đổi nghiệp vụ (SL-3.3).
 *
 * @param attempts số lần relay đã thử publish (tăng ở bước claim, không phải sau khi fail)
 * @param claimedAt relay đang giữ row; {@code null} = không ai giữ
 * @param sentAt chỉ có khi {@code status = SENT} (ràng buộc CHECK ở DB)
 */
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
