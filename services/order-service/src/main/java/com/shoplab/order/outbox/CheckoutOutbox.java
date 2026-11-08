package com.shoplab.order.outbox;

import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderRepository;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.saga.CheckoutSagaRepository;
import com.shoplab.order.saga.CheckoutSagaState;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chốt/huỷ đơn **và** ghi outbox trong **một transaction** (SL-3.3) — trái tim của pattern.
 *
 * <p><b>Vì sao phải là bean riêng, không phải method trong {@code CheckoutFinishService}</b>:
 * Spring AOP chỉ áp {@code @Transactional} khi lời gọi đi **qua proxy**. Gọi một method
 * {@code @Transactional} từ method khác của **cùng class** (self-invocation) sẽ chạy **không có
 * transaction** — bug im lặng đúng kiểu outbox sinh ra để chống.
 *
 * <p><b>Vì sao transaction ở đây ngắn</b>: nó chỉ chứa UPDATE order + UPDATE saga + INSERT outbox
 * (3 câu SQL local). Các lời gọi HTTP sang inventory/promotion/payment vẫn nằm **ngoài**
 * transaction (xem {@code CheckoutFinishService}) — giữ transaction qua lời gọi mạng là tự bắn vào
 * chân: một dependency chậm sẽ giữ row lock hàng giây.
 *
 * <p><b>Bất biến</b>: nếu {@code confirmPending} trả {@code false} (đơn đã đổi trạng thái ở nơi
 * khác) thì **không** ghi outbox ⇒ không phát event cho một lần chuyển trạng thái không xảy ra.
 */
@Service
public class CheckoutOutbox {

  private static final Logger LOG = LoggerFactory.getLogger(CheckoutOutbox.class);

  private final OrderWriter writer;
  private final OrderRepository orders;
  private final CheckoutSagaRepository sagas;
  private final OutboxRepository outbox;

  public CheckoutOutbox(
      OrderWriter writer,
      OrderRepository orders,
      CheckoutSagaRepository sagas,
      OutboxRepository outbox) {
    this.writer = writer;
    this.orders = orders;
    this.sagas = sagas;
    this.outbox = outbox;
  }

  /**
   * `PENDING_PAYMENT → CONFIRMED` + saga `CONFIRMED` + outbox `order.confirmed.v1`, cùng
   * transaction.
   */
  @Transactional
  public boolean confirm(UUID orderId, String reason, Instant now) {
    boolean changed = writer.confirmPending(orderId, reason, now);
    sagas.markState(orderId, CheckoutSagaState.CONFIRMED, null, now);
    if (changed) {
      append(orderId, OrderEvents.CONFIRMED, now, true);
    }
    return changed;
  }

  /**
   * `PENDING_PAYMENT → CANCELLED` + saga `CANCELLED` + outbox `order.cancelled.v1`, cùng
   * transaction.
   */
  @Transactional
  public boolean cancel(UUID orderId, String reason, Instant now) {
    boolean changed = writer.cancelPending(orderId, reason, now);
    sagas.markState(orderId, CheckoutSagaState.CANCELLED, null, now);
    if (changed) {
      append(orderId, OrderEvents.CANCELLED, now, false);
    }
    return changed;
  }

  private void append(UUID orderId, String eventType, Instant at, boolean confirmed) {
    OrderRecord order =
        orders
            .findById(orderId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "order vừa đổi trạng thái nhưng không đọc được: " + orderId));
    if (confirmed) {
      int items = orders.findItems(orderId).size();
      outbox.append(
          OrderEvents.AGGREGATE,
          orderId,
          eventType,
          OrderEvents.confirmed(
              orderId, order.userId(), order.total(), order.currency(), items, at));
    } else {
      outbox.append(
          OrderEvents.AGGREGATE,
          orderId,
          eventType,
          OrderEvents.cancelled(orderId, order.userId(), "khách huỷ", at));
    }
    LOG.info("outbox: ghi {} cho order={} (cùng transaction)", eventType, orderId);
  }
}
