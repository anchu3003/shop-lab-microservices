package com.shoplab.order.order;

import com.shoplab.order.idempotency.IdempotencyService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phần <b>ghi</b> của việc tạo đơn — tách khỏi {@link OrderService} có chủ ý.
 *
 * <p><b>Vì sao phải tách</b> (bug thật gặp ở bước verify `SL-2.5`): nếu cả luồng nằm trong một
 * {@code @Transactional} và bên trong gọi {@code IdempotencyService.begin()} với {@code
 * REQUIRES_NEW}, thì mỗi request cần <b>2 connection cùng lúc</b> (1 cho transaction ngoài, 1 cho
 * transaction mới). Với pool 5 và 20 request song song, cả 5 thread giữ 1 connection và chờ
 * connection thứ hai ⇒ <b>deadlock pool</b> (`CannotCreateTransactionException`) — request trả 500
 * dù logic đúng.
 *
 * <p>Cách đúng (và đúng với ADR-0006 D2 "ghi khoá trong transaction riêng"): ghi khoá <b>trước</b>,
 * khi chưa có transaction nào; sau đó mở transaction ghi order. Mỗi request chỉ giữ 1 connection
 * tại một thời điểm.
 */
@Service
public class OrderWriter {

  private final OrderRepository orders;
  private final IdempotencyService idempotency;

  public OrderWriter(OrderRepository orders, IdempotencyService idempotency) {
    this.orders = orders;
    this.idempotency = idempotency;
  }

  /**
   * Ghi order + items + history và đánh dấu idempotency COMPLETED — <b>một transaction</b>.
   *
   * <p>Nhờ vậy hoặc cả hai cùng có, hoặc cùng không: crash giữa chừng không bao giờ để lại khoá
   * COMPLETED trỏ tới đơn không tồn tại.
   */
  @Transactional
  public void create(
      OrderRecord order,
      List<OrderItemRecord> items,
      String idempotencyScope,
      String idempotencyKey,
      String responseBody) {
    orders.insert(order, items);
    orders.insertHistory(order.id(), null, OrderStatus.PENDING_PAYMENT, null, order.createdAt());
    idempotency.complete(idempotencyScope, idempotencyKey, 201, responseBody, order.id());
  }
}
