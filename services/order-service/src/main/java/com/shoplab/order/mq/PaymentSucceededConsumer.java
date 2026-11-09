package com.shoplab.order.mq;

import com.shoplab.commons.events.AbstractEventConsumer;
import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventEnvelopeCodec;
import com.shoplab.commons.events.EventProperties;
import com.shoplab.commons.events.EventTopology;
import com.shoplab.commons.events.ProcessedEventStore;
import com.shoplab.order.saga.CheckoutFinishService;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer của `q.order` (SL-3.5) — service **đầu tiên** consume event do service khác phát.
 *
 * <p>Nhiệm vụ duy nhất: nhận `payment.succeeded.v1` và giao cho {@link CheckoutFinishService} tự
 * chốt đơn. Toàn bộ retry/dedupe/DLQ nằm ở {@link AbstractEventConsumer} (`SL-3.2`) — giống
 * `notification-service` ở `SL-3.4`, đây là lần thứ hai chứng minh hạ tầng commons dùng lại được.
 *
 * <p><b>Vì sao không ném lỗi cho các ca "không chốt được"</b> (đơn đã `EXPIRED`, intent lệch): đó
 * là kết cục nghiệp vụ, retry không làm nó khác đi — message chỉ quay lại 3 lần rồi vào DLQ, tạo
 * cảm giác "có lỗi hệ thống" trong khi vấn đề là con người (hoàn tiền). Những ca đó ack + log
 * ERROR/WARN.
 */
@Component
public class PaymentSucceededConsumer extends AbstractEventConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentSucceededConsumer.class);

  private final CheckoutFinishService finish;

  public PaymentSucceededConsumer(
      EventEnvelopeCodec codec,
      ProcessedEventStore store,
      EventProperties properties,
      CheckoutFinishService finish) {
    super(codec, store, properties.getRetryDelays());
    this.finish = finish;
  }

  @Override
  protected String consumerName() {
    // Ổn định: một nửa của khoá dedupe `(event_id, consumer)`.
    return "order-service";
  }

  @Override
  protected Set<String> handledEventTypes() {
    return Set.of(EventTopology.ROUTING_PAYMENT_SUCCEEDED);
  }

  @Override
  protected void handleEvent(EventEnvelope envelope) {
    if (!EventTopology.ROUTING_PAYMENT_SUCCEEDED.equals(envelope.eventType())) {
      LOG.warn("bỏ qua event không xử lý: {}", envelope.eventType());
      return;
    }
    finish.confirmByEvent(envelope.eventId(), envelope.payload());
  }

  /**
   * Listener tách khỏi xử lý để `onMessage` (base) test được **không cần broker** — cùng khuôn
   * `SL-3.4`.
   */
  @RabbitListener(
      queues = EventTopology.QUEUE_ORDER,
      containerFactory = "eventListenerContainerFactory")
  public void onEvent(Message message, com.rabbitmq.client.Channel channel) throws IOException {
    onMessage(message, channel);
  }
}
