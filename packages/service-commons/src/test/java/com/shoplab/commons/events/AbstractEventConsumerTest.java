package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * {@link AbstractEventConsumer} — 4 nhánh quyết định của at-least-once (SL-3.2): ack khi xong · ack
 * khi trùng · nack khi payload sai · nack khi retry cạn.
 *
 * <p>Không cần broker: {@code Channel} giả + store giả. Retry/DLQ là thứ **không** quan sát được
 * bằng mắt khi hệ đang chạy bình thường nên rất đáng có test.
 */
class AbstractEventConsumerTest {

  private final EventEnvelopeCodec codec =
      new EventEnvelopeCodec(new ObjectMapper().findAndRegisterModules(), 1);

  /** Store giả: dedupe bằng {@code Set}, cùng semantics với bảng thật. */
  private static final class FakeStore implements ProcessedEventStore {

    private final Set<String> processed = new HashSet<>();

    @Override
    public boolean markProcessed(String consumer, EventEnvelope envelope) {
      return processed.add(consumer + "|" + envelope.eventId());
    }

    @Override
    public void unmarkProcessed(String consumer, EventEnvelope envelope) {
      processed.remove(consumer + "|" + envelope.eventId());
    }
  }

  private static final class TestConsumer extends AbstractEventConsumer {

    private final AtomicInteger handled = new AtomicInteger();
    private final boolean alwaysFail;

    TestConsumer(
        EventEnvelopeCodec codec, ProcessedEventStore store, List<Duration> delays, boolean fail) {
      super(codec, store, delays);
      this.alwaysFail = fail;
    }

    @Override
    protected String consumerName() {
      return "test-consumer";
    }

    @Override
    protected Set<String> handledEventTypes() {
      return Set.of("order.confirmed.v1");
    }

    @Override
    protected void handleEvent(EventEnvelope envelope) {
      handled.incrementAndGet();
      if (alwaysFail) {
        throw new IllegalStateException("db đang chết");
      }
    }
  }

  private Message message(EventEnvelope envelope) {
    MessageProperties props = new MessageProperties();
    props.setDeliveryTag(7L);
    return new Message(codec.encode(envelope).getBytes(StandardCharsets.UTF_8), props);
  }

  private EventEnvelope envelope(String eventType) {
    return EventEnvelope.create(eventType, "order-service", Map.of("orderId", "o-1"));
  }

  @Test
  @DisplayName("xử lý xong → handler chạy 1 lần + ack")
  void shouldAckAfterHandling() throws Exception {
    Channel channel = mock(Channel.class);
    TestConsumer consumer = new TestConsumer(codec, new FakeStore(), List.of(), false);

    consumer.onMessage(message(envelope("order.confirmed.v1")), channel);

    assertThat(consumer.handled.get()).isEqualTo(1);
    verify(channel).basicAck(7L, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("cùng eventId 2 lần → handler chạy 1 lần, lần 2 ack luôn (KHÔNG vào DLQ)")
  void shouldDedupeByEventId() throws Exception {
    Channel channel = mock(Channel.class);
    TestConsumer consumer = new TestConsumer(codec, new FakeStore(), List.of(), false);
    EventEnvelope event = envelope("order.confirmed.v1");

    consumer.onMessage(message(event), channel);
    consumer.onMessage(message(event), channel);

    assertThat(consumer.handled.get()).isEqualTo(1);
    verify(channel, times(2)).basicAck(7L, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("handler lỗi → thử 3 lần (1 + 2 retry) rồi nack(requeue=false) = vào DLQ")
  void shouldNackAfterRetriesExhausted() throws Exception {
    Channel channel = mock(Channel.class);
    FakeStore store = new FakeStore();
    TestConsumer consumer =
        new TestConsumer(codec, store, List.of(Duration.ZERO, Duration.ZERO), true);
    EventEnvelope event = envelope("order.confirmed.v1");

    consumer.onMessage(message(event), channel);

    assertThat(consumer.handled.get()).isEqualTo(3);
    verify(channel).basicNack(7L, false, false);
    verify(channel, never()).basicAck(anyLong(), anyBoolean());
    // Marker phải đã được XOÁ: nếu không, replay từ DLQ sẽ bị dedupe chặn (bug im lặng).
    assertThat(store.markProcessed("test-consumer", event)).isTrue();
  }

  @Test
  @DisplayName("payload không hợp lệ → nack(requeue=false) NGAY, không retry")
  void shouldNackBrokenPayloadWithoutRetry() throws Exception {
    Channel channel = mock(Channel.class);
    TestConsumer consumer = new TestConsumer(codec, new FakeStore(), List.of(Duration.ZERO), false);
    MessageProperties props = new MessageProperties();
    props.setDeliveryTag(9L);
    Message broken = new Message("không phải json".getBytes(StandardCharsets.UTF_8), props);

    consumer.onMessage(broken, channel);

    assertThat(consumer.handled.get()).isZero();
    verify(channel).basicNack(9L, false, false);
  }

  @Test
  @DisplayName("event không thuộc consumer → ack + bỏ qua (không chạy handler)")
  void shouldSkipUnhandledEventType() throws Exception {
    Channel channel = mock(Channel.class);
    TestConsumer consumer = new TestConsumer(codec, new FakeStore(), List.of(), false);

    consumer.onMessage(message(envelope("payment.succeeded.v1")), channel);

    assertThat(consumer.handled.get()).isZero();
    verify(channel).basicAck(7L, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }
}
