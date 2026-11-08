package com.shoplab.order.order;

import com.shoplab.order.idempotency.IdempotencyService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    createPending(order, items);
    idempotency.complete(idempotencyScope, idempotencyKey, 201, responseBody, order.id());
  }

  /**
   * Ghi order + history <b>không</b> đụng idempotency — dùng cho saga checkout (`SL-2.6a`).
   *
   * <p>Vì sao cần bản này: response cuối của checkout chỉ biết <b>sau</b> bước payment intent (có
   * `paymentUrl`), nên khoá idempotency được complete muộn hơn, ở tầng saga. Vẫn an toàn: khoá chỉ
   * thành {@code COMPLETED} khi mọi thứ đã tồn tại (bài học `SL-2.5`: không bao giờ để khoá
   * COMPLETED trỏ tới đơn ma).
   */
  @Transactional
  public void createPending(OrderRecord order, List<OrderItemRecord> items) {
    orders.insert(order, items);
    orders.insertHistory(order.id(), null, OrderStatus.PENDING_PAYMENT, null, order.createdAt());
  }

  /** Gắn payment intent vào order (sau khi payment-service tạo xong). */
  @Transactional
  public void attachPaymentIntent(UUID orderId, UUID paymentIntentId, Instant now) {
    orders.attachPaymentIntent(orderId, paymentIntentId, now);
  }

  /**
   * Huỷ đơn còn `PENDING_PAYMENT` — dùng khi saga phải bù sau khi đã tạo order.
   *
   * @return {@code true} nếu thực sự chuyển được (0 row ⇒ đơn đã ở trạng thái khác, vd đã bị huỷ
   *     song song — không ghi history lần 2)
   */
  @Transactional
  public boolean cancelPending(UUID orderId, String reason, Instant now) {
    return transitionPending(orderId, OrderStatus.CANCELLED, reason, now);
  }

  /**
   * Chốt đơn (tiền đã về / đơn 0 đồng): `PENDING_PAYMENT → CONFIRMED` (`SL-2.6b`).
   *
   * @return {@code false} nếu đơn không còn `PENDING_PAYMENT` (đã confirm/huỷ/hết hạn ở nơi khác) —
   *     caller đọc lại trạng thái để trả kết quả đúng, và history không bị ghi 2 lần
   */
  @Transactional
  public boolean confirmPending(UUID orderId, String reason, Instant now) {
    return transitionPending(orderId, OrderStatus.CONFIRMED, reason, now);
  }

  /** Đánh dấu đơn hết hạn (`PENDING_PAYMENT → EXPIRED`) — dùng cho sweeper `SL-2.6b`. */
  @Transactional
  public boolean expirePending(UUID orderId, String reason, Instant now) {
    return transitionPending(orderId, OrderStatus.EXPIRED, reason, now);
  }

  private boolean transitionPending(UUID orderId, OrderStatus to, String reason, Instant now) {
    if (orders.markStatus(orderId, OrderStatus.PENDING_PAYMENT, to, now) != 1) {
      return false;
    }
    orders.insertHistory(orderId, OrderStatus.PENDING_PAYMENT, to, reason, now);
    return true;
  }
}
