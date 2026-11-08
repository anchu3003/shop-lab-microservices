package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

/**
 * Topology khai bằng code (SL-3.1) — test không cần broker: nội dung khai báo là dữ liệu thuần.
 *
 * <p>Đây là test **hợp đồng**: tên/routing key sai một ký tự thì RabbitMQ không báo lỗi, message
 * rơi vào hư không và chỉ lộ ra khi thiếu email/index (rất khó truy).
 */
class EventTopologyAutoConfigurationTest {

  private final List<Declarable> declarables =
      List.copyOf(new EventTopologyAutoConfiguration().shoplabEventTopology().getDeclarables());

  private <T extends Declarable> List<T> of(Class<T> type) {
    return declarables.stream().filter(type::isInstance).map(type::cast).toList();
  }

  @Test
  @DisplayName("2 exchange: shoplab.events (topic) + shoplab.dlx (direct), đều durable")
  void shouldDeclareExchanges() {
    List<TopicExchange> events = of(TopicExchange.class);
    List<DirectExchange> deadLetter = of(DirectExchange.class);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getName()).isEqualTo(EventTopology.EXCHANGE);
    assertThat(events.get(0).isDurable()).isTrue();

    assertThat(deadLetter).hasSize(1);
    assertThat(deadLetter.get(0).getName()).isEqualTo(EventTopology.DEAD_LETTER_EXCHANGE);
    assertThat(deadLetter.get(0).isDurable()).isTrue();
  }

  @Test
  @DisplayName("3 queue consumer + 3 DLQ, tất cả durable, KHÔNG auto-delete")
  void shouldDeclareQueuesAndDlq() {
    List<Queue> queues = of(Queue.class);

    assertThat(queues).hasSize(6);
    assertThat(queues)
        .extracting(Queue::getName)
        .containsExactlyInAnyOrder(
            "q.notification",
            "q.notification.dlq",
            "q.order",
            "q.order.dlq",
            "q.search",
            "q.search.dlq");
    assertThat(queues).allSatisfy(q -> assertThat(q.isDurable()).isTrue());
    // auto-delete = true thì queue biến mất khi consumer cuối rời đi ⇒ mất message (ADR-0007 D2
    // loại).
    assertThat(queues).allSatisfy(q -> assertThat(q.isAutoDelete()).isFalse());
  }

  @Test
  @DisplayName("queue consumer có DLX trỏ shoplab.dlx + routing key = tên queue (để rơi đúng DLQ)")
  void shouldWireDeadLetterOnConsumerQueues() {
    of(Queue.class).stream()
        .filter(q -> !q.getName().endsWith(EventTopology.DLQ_SUFFIX))
        .forEach(
            q -> {
              assertThat(q.getArguments())
                  .containsEntry("x-dead-letter-exchange", EventTopology.DEAD_LETTER_EXCHANGE);
              assertThat(q.getArguments()).containsEntry("x-dead-letter-routing-key", q.getName());
            });

    // DLQ thì KHÔNG có DLX (không có chuyện DLQ dead-letter tiếp — tránh vòng lặp vô hạn).
    of(Queue.class).stream()
        .filter(q -> q.getName().endsWith(EventTopology.DLQ_SUFFIX))
        .forEach(q -> assertThat(q.getArguments()).doesNotContainKey("x-dead-letter-exchange"));
  }

  @Test
  @DisplayName("binding: 5 routing key của ADR-0007 vào đúng exchange + DLQ bind theo tên queue")
  void shouldDeclareBindings() {
    List<Binding> eventBindings =
        of(Binding.class).stream()
            .filter(b -> b.getExchange().equals(EventTopology.EXCHANGE))
            .toList();

    assertThat(eventBindings)
        .extracting(Binding::getRoutingKey)
        .containsExactlyInAnyOrder(
            EventTopology.ROUTING_ORDER_CONFIRMED,
            EventTopology.ROUTING_ORDER_CANCELLED,
            EventTopology.ROUTING_PAYMENT_SUCCEEDED,
            EventTopology.ROUTING_PRODUCT_UPDATED,
            EventTopology.ROUTING_PRODUCT_ARCHIVED);

    assertThat(
            eventBindings.stream()
                .filter(b -> b.getRoutingKey().equals(EventTopology.ROUTING_PAYMENT_SUCCEEDED))
                .map(Binding::getDestination)
                .toList())
        .containsExactly(EventTopology.QUEUE_ORDER);

    List<Binding> dlqBindings =
        of(Binding.class).stream()
            .filter(b -> b.getExchange().equals(EventTopology.DEAD_LETTER_EXCHANGE))
            .toList();
    assertThat(dlqBindings)
        .extracting(Binding::getDestination)
        .containsExactlyInAnyOrder("q.notification.dlq", "q.order.dlq", "q.search.dlq");
    assertThat(dlqBindings)
        .allSatisfy(
            b -> assertThat(b.getRoutingKey()).isEqualTo(b.getDestination().replace(".dlq", "")));
  }

  @Test
  @DisplayName("routingKeysFor: queue lạ phải nổ ngay (fail fast, không im lặng bỏ qua)")
  void shouldRejectUnknownQueue() {
    assertThatThrownBy(() -> EventTopology.routingKeysFor("q.khong-ton-tai"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("q.khong-ton-tai");
  }
}
