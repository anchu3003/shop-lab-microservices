package com.shoplab.catalog.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Truy cập bảng {@code outbox} (SL-3.3). */
public interface OutboxRepository {

  /** Ghi outbox **trong transaction của caller** — đây là toàn bộ ý nghĩa của pattern. */
  void append(
      String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload);

  /**
   * Giữ (claim) tối đa {@code batch} row `PENDING` để publish.
   *
   * <p>Row trả về đã được đánh `claimed_at` + `attempts++` trong **một transaction ngắn** ⇒ relay
   * khác chạy song song sẽ **không** thấy chúng (nhờ `FOR UPDATE SKIP LOCKED` + điều kiện
   * `claimed_at`). Không giữ lock DB trong lúc publish (broker chậm không được khoá bảng).
   */
  List<OutboxRecord> claimPending(int batch, Instant now, Instant claimDeadline);

  /** Publish xong ⇒ đánh dấu để không gửi lại. */
  void markSent(UUID id, Instant now);

  /**
   * Publish lỗi ⇒ ghi lý do, **trả claim** để nhịp sau thử lại (event chưa gửi thì còn nghĩa vụ).
   */
  void markFailed(UUID id, String error);

  /** Đếm row theo trạng thái — dùng cho test/verify nhanh và log nhịp quét. */
  long countByStatus(OutboxStatus status);

  /**
   * Xoá row {@code SENT} cũ hơn {@code cutoff} — job retention (`TD-29`, `SL-4.0`).
   *
   * <p><b>Chỉ {@code SENT}</b>: row {@code PENDING} là event **chưa gửi**, xoá là mất event thật.
   * Điều kiện tuổi dùng {@code sent_at} (không phải {@code created_at}): thứ quyết định "còn cần
   * giữ để điều tra" là lúc nó **đã gửi**.
   *
   * <p>Xoá theo batch để transaction ngắn (không khoá bảng, không tạo WAL khổng lồ).
   *
   * @param cutoff xoá row có {@code sent_at < cutoff}
   * @param batch số row tối đa mỗi lần gọi
   * @return số row đã xoá (0 ⇒ bảng đã sạch tới ngưỡng)
   */
  int deleteSentOlderThan(Instant cutoff, int batch);
}
