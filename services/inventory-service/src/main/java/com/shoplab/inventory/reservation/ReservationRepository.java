package com.shoplab.inventory.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập {@code reservations} + {@code reservation_items}.
 *
 * <p>Method đổi trạng thái trả {@code int} (rowsAffected) và nhận {@code fromStatus}: đó là
 * compare-and-set ở tầng SQL — hai request commit đồng thời thì chỉ một cái "thắng", cái còn lại
 * thấy 0 row và đọc trạng thái hiện tại để trả kết quả idempotent. Không cần lock ứng dụng.
 */
public interface ReservationRepository {

  /** Reservation đang giữ chỗ của order — dùng cho retry idempotent. */
  Optional<ReservationRecord> findPendingByOrderId(UUID orderId);

  Optional<ReservationRecord> findById(UUID id);

  /** Tạo header + items trong cùng transaction của lời gọi. */
  void insert(ReservationRecord reservation, List<ReservationItemRecord> items);

  List<ReservationItemRecord> findItems(UUID reservationId);

  /**
   * {@code UPDATE … WHERE id = :id AND status = :from} — trả về 1 nếu đổi được, 0 nếu trạng thái đã
   * khác (request khác vừa đổi trước).
   */
  int markStatus(UUID id, ReservationStatus from, ReservationStatus to, Instant now);
}
