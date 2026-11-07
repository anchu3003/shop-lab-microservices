package com.shoplab.promotion.lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập {@code voucher_locks}.
 *
 * <p>Đổi trạng thái dùng compare-and-set ({@code WHERE status = :from}) — hai request commit đồng
 * thời thì chỉ một cái thắng, cái còn lại đọc trạng thái thật để trả kết quả idempotent. Không cần
 * lock ứng dụng (cùng lý do như inventory).
 */
public interface VoucherLockRepository {

  /** Lock đang giữ của order — khoá idempotency cho retry. */
  Optional<VoucherLockRecord> findLockedByOrderId(UUID orderId);

  Optional<VoucherLockRecord> findById(UUID id);

  void insert(VoucherLockRecord lock);

  /**
   * @return 1 nếu đổi được, 0 nếu trạng thái đã khác (request/instance khác xử lý trước)
   */
  int markStatus(UUID id, VoucherLockStatus from, VoucherLockStatus to, Instant now);

  /**
   * Lock quá hạn cho sweeper. {@code FOR UPDATE SKIP LOCKED} + {@code ORDER BY expires_at} — nhiều
   * instance chạy song song vẫn an toàn và lock cũ nhất được dọn trước.
   */
  List<VoucherLockRecord> findExpiredLocked(Instant now, int limit);
}
