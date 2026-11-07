package com.shoplab.order.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập {@code orders} + {@code order_items} + {@code order_status_history}.
 *
 * <p>Đổi trạng thái dùng compare-and-set {@code WHERE status = :from} (cùng pattern
 * inventory/promotion): hai request chuyển trạng thái đồng thời thì chỉ một cái thắng, cái còn lại
 * thấy 0 row ⇒ service đọc lại để trả kết quả đúng (idempotent nếu trùng đích, 409 nếu lệch).
 */
public interface OrderRepository {

  Optional<OrderRecord> findById(UUID orderId);

  List<OrderItemRecord> findItems(UUID orderId);

  /** Tạo header + items **trong cùng transaction** của caller. */
  void insert(OrderRecord order, List<OrderItemRecord> items);

  /**
   * @return 1 nếu chuyển được, 0 nếu trạng thái hiện tại không còn là {@code from}
   */
  int markStatus(UUID orderId, OrderStatus from, OrderStatus to, Instant now);

  void insertHistory(UUID orderId, OrderStatus from, OrderStatus to, String reason, Instant now);

  /** Danh sách đơn của một user, mới nhất trước (admin truyền userId = null để lấy tất cả). */
  List<OrderRecord> findByUser(UUID userId, int offset, int limit);

  long countByUser(UUID userId);
}
