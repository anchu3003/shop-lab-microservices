package com.shoplab.payment.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Truy cập {@code idempotency_keys}. */
public interface IdempotencyRepository {

  /**
   * Ghi row {@code IN_PROGRESS} nếu chưa có — <b>nguyên tử</b> ở DB qua {@code ON CONFLICT DO
   * NOTHING}.
   *
   * @return 1 nếu đây là lần đầu (giữ được khoá), 0 nếu khoá đã tồn tại
   */
  int insertIfAbsent(String scope, String key, String requestHash, Instant now);

  Optional<IdempotencyRecord> find(String scope, String key);

  /** Ghi lại response để retry sau này được replay đúng y nguyên. */
  void complete(
      String scope, String key, int statusCode, String responseBody, UUID resourceId, Instant now);

  /**
   * Dọn row cũ (TD-14): {@code COMPLETED} quá hạn retention + {@code IN_PROGRESS} bị bỏ quên quá
   * lâu.
   *
   * @return số row đã xoá
   */
  int deleteOlderThan(Instant completedBefore, Instant inProgressBefore, int limit);
}
