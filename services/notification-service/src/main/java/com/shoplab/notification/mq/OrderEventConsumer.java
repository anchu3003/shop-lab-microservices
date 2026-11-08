package com.shoplab.notification.mq;

import com.shoplab.commons.events.AbstractEventConsumer;
import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventEnvelopeCodec;
import com.shoplab.commons.events.EventTopology;
import com.shoplab.commons.events.ProcessedEventStore;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.notify.NotificationService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer của `q.notification` (SL-3.4) — **vertical slice đầu tiên** của Phase 3.
 *
 * <p>Toàn bộ phần khó (decode → dedupe → retry → DLQ → ack) nằm ở {@link AbstractEventConsumer} của
 * `service-commons`. Class này chỉ trả lời 3 câu: tôi là ai, tôi quan tâm event nào, và xử lý ra
 * sao — đúng mục tiêu của `SL-3.2` (service mới không phải copy lại hạ tầng).
 *
 * <p>`containerFactory = "eventListenerContainerFactory"` là **bắt buộc**: factory đó bật ack thủ
 * công (auto-ack ⇒ message bị coi là xong trước khi handler chạy ⇒ không bao giờ vào DLQ).
 */
@Component
public class OrderEventConsumer extends AbstractEventConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(OrderEventConsumer.class);

  private final NotificationService notifications;

  public OrderEventConsumer(
      EventEnvelopeCodec codec,
      ProcessedEventStore store,
      NotificationProperties properties,
      NotificationService notifications) {
    super(codec, store, properties.retryDelays());
    this.notifications = notifications;
  }

  @Override
  protected String consumerName() {
    // PHẢI ổn định: đây là một nửa của khoá dedupe `(event_id, consumer)`.
    return "notification-service";
  }

  @Override
  protected Set<String> handledEventTypes() {
    return Set.of("order.confirmed.v1", "order.cancelled.v1");
  }

  @Override
  protected void handleEvent(EventEnvelope envelope) {
    switch (envelope.eventType()) {
      case "order.confirmed.v1" -> notifications.onOrderConfirmed(envelope);
      case "order.cancelled.v1" -> notifications.onOrderCancelled(envelope);
      default ->
          // Không nên xảy ra (đã lọc ở handledEventTypes) — log để phát hiện sớm nếu có ai sửa
          // lệch.
          LOG.warn("bỏ qua event không xử lý: {}", envelope.eventType());
    }
  }

  /**
   * Đăng ký listener. Tách khỏi method xử lý để `onMessage` (của base) vẫn test được **không cần
   * broker**.
   */
  @RabbitListener(
      queues = EventTopology.QUEUE_NOTIFICATION,
      containerFactory = "eventListenerContainerFactory")
  public void onEvent(
      org.springframework.amqp.core.Message message, com.rabbitmq.client.Channel channel)
      throws java.io.IOException {
    onMessage(message, channel);
  }
}
