package com.shoplab.commons.events;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Publish event lên {@code shoplab.events} với <b>publisher confirm</b> (SL-3.2, ADR-0007 D4).
 *
 * <p><b>Vì sao phải chờ confirm</b>: {@code convertAndSend} chỉ nói "đã gửi vào socket", **không**
 * nói broker đã nhận và route được. Outbox relay (SL-3.3) cần biết điều đó để quyết định row {@code
 * SENT} hay giữ {@code PENDING} — nếu không, một lần broker restart đúng lúc publish sẽ làm mất
 * event mà DB vẫn ghi "đã gửi".
 *
 * <p>Message luôn {@link MessageDeliveryMode#PERSISTENT}: queue đã durable (SL-3.1) thì message
 * phải persistent, nếu không broker restart là mất (đúng ca đã test ở SL-3.1).
 */
public class EventPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final EventEnvelopeCodec codec;
  private final long confirmTimeoutMillis;

  public EventPublisher(
      RabbitTemplate rabbitTemplate, EventEnvelopeCodec codec, long confirmTimeoutMillis) {
    this.rabbitTemplate = rabbitTemplate;
    this.codec = codec;
    this.confirmTimeoutMillis = confirmTimeoutMillis;
  }

  /**
   * Publish và **chờ broker xác nhận**.
   *
   * @throws EventPublishException khi broker nack, timeout, hoặc không kết nối được — caller
   *     (relay) phải coi là "chưa gửi" và thử lại
   */
  public void publish(EventEnvelope envelope) {
    CorrelationData correlation = new CorrelationData(envelope.eventId());

    rabbitTemplate.convertAndSend(
        EventTopology.EXCHANGE,
        envelope.eventType(),
        codec.encode(envelope),
        message -> {
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          message.getMessageProperties().setContentType("application/json");
          return message;
        },
        correlation);

    CorrelationData.Confirm confirm;
    try {
      confirm = correlation.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
    } catch (Exception ex) {
      // Timeout/interrupt: KHÔNG biết broker đã nhận hay chưa ⇒ coi là thất bại để relay thử lại
      // (at-least-once: trùng còn hơn mất; consumer dedupe theo eventId).
      throw new EventPublishException(
          "không nhận được publisher confirm cho %s sau %d ms"
              .formatted(envelope.eventId(), confirmTimeoutMillis),
          ex);
    }

    if (!confirm.isAck()) {
      throw new EventPublishException(
          "broker nack %s: %s".formatted(envelope.eventId(), confirm.getReason()));
    }

    LOG.debug("published {} ({})", envelope.eventType(), envelope.eventId());
  }
}
