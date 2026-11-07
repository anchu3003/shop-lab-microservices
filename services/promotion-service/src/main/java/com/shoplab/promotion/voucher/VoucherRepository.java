package com.shoplab.promotion.voucher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập bảng {@code vouchers}.
 *
 * <p><b>{@link #lock(String, Instant)} trả {@code int} (rowsAffected)</b> — đó là toàn bộ cơ chế
 * chống bán quá quota: điều kiện {@code used + locked < quota} nằm trong câu UPDATE nên Postgres
 * quyết định trong cùng row lock. Không đọc trước rồi ghi sau (đúng bài học ở `SL-2.2`).
 */
public interface VoucherRepository {

  Optional<VoucherRecord> findByCode(String code);

  Optional<VoucherRecord> findById(UUID id);

  void insert(VoucherRecord voucher);

  /**
   * Giữ 1 lượt: {@code locked += 1} <b>chỉ khi</b> còn lượt và voucher đang trong thời gian hiệu
   * lực.
   *
   * @return 1 nếu giữ được, 0 nếu hết lượt / ngoài cửa sổ thời gian
   */
  int lock(String code, Instant now);

  /** Chốt dùng thật: {@code used += 1, locked -= 1}. */
  int commit(UUID voucherId);

  /** Trả lượt đã giữ: {@code locked -= 1} (không đụng {@code used}). */
  int release(UUID voucherId);
}
