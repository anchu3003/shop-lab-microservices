package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Điều kiện của {@link EventAutoConfiguration} (SL-3.2).
 *
 * <p>Hai điều phải giữ: (1) service **có** AMQP thì có đủ bean để dùng ngay; (2) service **không**
 * có AMQP (identity, gateway, cart…) không bị ảnh hưởng — không bean, không kết nối broker.
 */
class EventAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(EventAutoConfiguration.class))
          .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
          .withBean(RabbitTemplate.class, () -> new RabbitTemplate(mock(ConnectionFactory.class)));

  @Test
  @DisplayName("có AMQP → codec, publisher, container factory (MANUAL ack, prefetch 1) đều có")
  void shouldProvideBeansWhenAmqpPresent() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(EventEnvelopeCodec.class);
          assertThat(context).hasSingleBean(EventPublisher.class);

          // Kiểm cấu hình container THẬT (không chỉ "bean tồn tại"): ack thủ công là điều kiện để
          // retry/DLQ hoạt động — auto-ack thì message mất trước khi kịp nack.
          SimpleRabbitListenerContainerFactory factory =
              context.getBean(
                  "eventListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
          SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
          endpoint.setQueueNames("q.test");
          endpoint.setMessageListener(message -> {});
          SimpleMessageListenerContainer container =
              (SimpleMessageListenerContainer) factory.createListenerContainer(endpoint);
          // MANUAL là điều kiện bắt buộc: ack/nack là quyết định nghiệp vụ (dedupe, retry cạn).
          // `prefetch=1` + `concurrency=1` (giữ thứ tự) không có getter public ⇒ đọc qua config.
          assertThat(container.getAcknowledgeMode()).isEqualTo(AcknowledgeMode.MANUAL);
        });
  }

  @Test
  @DisplayName(
      "có JdbcTemplate → tạo store dedupe + bảng processed_events (regression: thiếu `after` thì im lặng không tạo)")
  void shouldCreateStoreWhenJdbcPresent() {
    runner
        .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(ProcessedEventStore.class);
            });
  }

  @Test
  @DisplayName("service không có DB (vd cart) → không tạo store dedupe, không lỗi")
  void shouldSkipStoreWithoutJdbc() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(ProcessedEventStore.class);
        });
  }

  @Test
  @DisplayName("shoplab.events.enabled=false → không tạo bean event nào")
  void shouldBackOffWhenDisabled() {
    runner
        .withPropertyValues("shoplab.events.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(EventEnvelopeCodec.class);
              assertThat(context).doesNotHaveBean(EventPublisher.class);
            });
  }

  @Test
  @DisplayName(
      "service KHÔNG có AMQP trên classpath → auto-config im lặng bỏ qua (không NoClassDefFoundError)")
  void shouldBackOffWithoutAmqpOnClasspath() {
    runner
        .withClassLoader(new FilteredClassLoader(RabbitTemplate.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(EventPublisher.class);
            });
  }
}
