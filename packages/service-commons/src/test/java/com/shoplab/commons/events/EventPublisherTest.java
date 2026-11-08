package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoplab.commons.log.MdcKeys;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * {@link EventPublisher} — điều quan trọng nhất cần chốt là **publisher confirm**: gửi xong phải
 * biết broker đã nhận, nếu không outbox relay (SL-3.3) không phân biệt được "đã gửi" và "đã ghi vào
 * socket".
 */
class EventPublisherTest {

  private final EventEnvelopeCodec codec =
      new EventEnvelopeCodec(new ObjectMapper().findAndRegisterModules(), 1);

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @SuppressWarnings("unchecked")
  private RabbitTemplate templateAckingBroker(
      AtomicReference<String> body,
      AtomicReference<org.springframework.amqp.core.Message> sentMessage) {
    RabbitTemplate template = mock(RabbitTemplate.class);
    doAnswer(
            invocation -> {
              // Tham số: exchange, routingKey, object(JSON), messagePostProcessor, correlationData
              Object[] args = invocation.getArguments();
              body.set((String) args[2]);
              CorrelationData correlation = (CorrelationData) args[4];
              MessagePostProcessor postProcessor = (MessagePostProcessor) args[3];
              org.springframework.amqp.core.Message message =
                  new org.springframework.amqp.core.Message(
                      ((String) args[2]).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                      new org.springframework.amqp.core.MessageProperties());
              sentMessage.set(postProcessor.postProcessMessage(message));
              correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(template)
        .convertAndSend(
            anyString(),
            anyString(),
            any(),
            any(MessagePostProcessor.class),
            any(CorrelationData.class));
    return template;
  }

  @Test
  @DisplayName(
      "publish → đủ 7 field trong JSON, routingKey = eventType, message persistent, traceId từ MDC")
  void shouldPublishFullEnvelopeAndWaitForConfirm() {
    AtomicReference<String> body = new AtomicReference<>();
    AtomicReference<org.springframework.amqp.core.Message> sent = new AtomicReference<>();
    RabbitTemplate template = templateAckingBroker(body, sent);
    MDC.put(MdcKeys.TRACE_ID, "trace-abc");

    new EventPublisher(template, codec, 1000)
        .publish(
            EventEnvelope.create("order.confirmed.v1", "order-service", Map.of("orderId", "o-1")));

    EventEnvelope envelope = codec.decode(body.get());
    assertThat(envelope.eventType()).isEqualTo("order.confirmed.v1");
    assertThat(envelope.version()).isEqualTo(1);
    assertThat(envelope.producer()).isEqualTo("order-service");
    assertThat(envelope.traceId()).isEqualTo("trace-abc");
    assertThat(envelope.eventId()).isNotBlank();
    assertThat(envelope.occurredAt()).isNotNull();
    assertThat(envelope.payload()).containsEntry("orderId", "o-1");

    assertThat(sent.get().getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    // correlationData được truyền ⇒ confirm THẬT SỰ được yêu cầu (không phải "fire and forget").
    ArgumentCaptor<CorrelationData> cd = ArgumentCaptor.forClass(CorrelationData.class);
    verify(template)
        .convertAndSend(
            eq(EventTopology.EXCHANGE),
            eq("order.confirmed.v1"),
            anyString(),
            any(MessagePostProcessor.class),
            cd.capture());
    assertThat(cd.getValue().getId()).isEqualTo(envelope.eventId());
  }

  @Test
  @DisplayName("broker nack → EventPublishException (relay phải coi là CHƯA gửi)")
  void shouldThrowWhenBrokerNacks() {
    RabbitTemplate template = mock(RabbitTemplate.class);
    doAnswer(
            invocation -> {
              CorrelationData correlation = (CorrelationData) invocation.getArguments()[4];
              correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
              return null;
            })
        .when(template)
        .convertAndSend(
            anyString(),
            anyString(),
            any(),
            any(MessagePostProcessor.class),
            any(CorrelationData.class));

    EventPublisher publisher = new EventPublisher(template, codec, 1000);

    assertThatThrownBy(
            () ->
                publisher.publish(
                    EventEnvelope.create("order.confirmed.v1", "order-service", Map.of())))
        .isInstanceOf(EventPublishException.class)
        .hasMessageContaining("nack");
  }
}
