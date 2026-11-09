package com.shoplab.payment.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cổng ghi/đọc outbox (SL-3.5). */
public interface OutboxRepository {

  /** Ghi 1 event **trong transaction của caller** — đây là toàn bộ ý nghĩa của pattern. */
  void append(
      String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload);

  /** Claim tối đa {@code batch} row PENDING (bỏ qua row đang bị relay khác giữ). */
  List<OutboxRecord> claimPending(int batch, Instant now, Instant claimDeadline);

  void markSent(UUID id, Instant now);

  void markFailed(UUID id, String error);

  long countByStatus(OutboxStatus status);
}
