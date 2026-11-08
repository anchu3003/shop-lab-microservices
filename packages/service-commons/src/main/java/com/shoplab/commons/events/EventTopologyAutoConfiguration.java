package com.shoplab.commons.events;

import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Khai topology RabbitMQ bằng code (SL-3.1) — nguồn: ADR-0007 § Decision (D2).
 *
 * <p><b>Vì sao khai bằng code, không dùng {@code definitions.json}</b>: file JSON phải import tay
 * mỗi lần dựng máy mới ⇒ dễ lệch với ADR và lệch giữa các máy. Code thì mọi service khai **giống
 * hệt nhau** (cùng một class ở commons) và việc khai là idempotent.
 *
 * <p><b>Ai chạy khai báo này?</b> Mọi service có {@code spring-boot-starter-amqp} trên classpath và
 * bật {@code shoplab.events.topology.enabled} (mặc định bật). Nhiều service cùng khai **không**
 * xung đột vì tham số giống nhau.
 *
 * <p>⚠️ <b>Đổi tham số queue = phải xoá queue cũ</b>: RabbitMQ trả {@code PRECONDITION_FAILED} khi
 * khai một queue đã tồn tại với {@code durable}/argument khác. Trên máy dev: xoá queue qua {@code
 * tools/rabbit-topology.sh purge}/UI hoặc {@code docker compose down -v} (volume broker).
 *
 * <p>Không có phần publish/consume ở đây — đó là {@code SL-3.2}.
 */
@AutoConfiguration
@ConditionalOnClass({Declarables.class, TopicExchange.class})
@ConditionalOnProperty(
    name = "shoplab.events.topology.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class EventTopologyAutoConfiguration {

  /**
   * Argument DLX của queue — đặt ở queue (không phải exchange) để message bị {@code nack} **hẳn**
   * đi sang {@code shoplab.dlx} với routing key {@code <queue>} ⇒ rơi đúng {@code <queue>.dlq}.
   */
  static final String ARG_DLX = "x-dead-letter-exchange";

  /** Routing key khi dead-letter: chính tên queue, để DLQ bind 1-1 và không lẫn giữa các queue. */
  static final String ARG_DLK = "x-dead-letter-routing-key";

  @Bean
  @ConditionalOnMissingBean
  public Declarables shoplabEventTopology() {
    TopicExchange events = new TopicExchange(EventTopology.EXCHANGE, true, false);
    DirectExchange deadLetter = new DirectExchange(EventTopology.DEAD_LETTER_EXCHANGE, true, false);

    List<Declarable> declarables = new ArrayList<>(List.of(events, deadLetter));

    for (String queueName : EventTopology.QUEUES) {
      // Queue bền (durable) + DLX: message không mất khi broker restart, message lỗi không chặn
      // queue.
      Queue queue =
          QueueBuilder.durable(queueName)
              .deadLetterExchange(EventTopology.DEAD_LETTER_EXCHANGE)
              .deadLetterRoutingKey(queueName)
              .build();
      Queue dlq = QueueBuilder.durable(EventTopology.dlqOf(queueName)).build();
      // DLQ bind 1-1 theo tên queue (§ D5: replay tay, cần biết message thuộc queue nào).
      declarables.add(queue);
      declarables.add(dlq);
      declarables.add(BindingBuilder.bind(dlq).to(deadLetter).with(queueName));

      for (String routingKey : EventTopology.routingKeysFor(queueName)) {
        declarables.add(BindingBuilder.bind(queue).to(events).with(routingKey));
      }
    }

    return new Declarables(declarables);
  }

  /**
   * Khai topology ngay khi khởi động — xem {@link EventTopologyDeclarer} (bẫy: {@code RabbitAdmin}
   * khai lazy, service chưa có consumer/publisher thì không ai mở connection ⇒ topology không tồn
   * tại dù code đã "khai").
   */
  @Bean
  @ConditionalOnMissingBean
  public EventTopologyDeclarer shoplabEventTopologyDeclarer(AmqpAdmin amqpAdmin) {
    return new EventTopologyDeclarer(amqpAdmin);
  }
}
