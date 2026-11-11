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

  /**
   * Xoá row {@code SENT} cũ hơn {@code cutoff} — job retention (`TD-29`, `SL-4.0`).
   *
   * <p><b>Chỉ {@code SENT}</b>: row {@code PENDING} là event **chưa gửi**, xoá là mất event thật.
   * Điều kiện tuổi dùng {@code sent_at} (không phải {@code created_at}) — mốc "còn cần giữ để điều
   * tra" là lúc event **đã gửi**.
   *
   * @param cutoff xoá row có {@code sent_at < cutoff}
   * @param batch số row tối đa mỗi lần gọi
   * @return số row đã xoá (0 ⇒ bảng đã sạch tới ngưỡng)
   */
  int deleteSentOlderThan(Instant cutoff, int batch);
}
