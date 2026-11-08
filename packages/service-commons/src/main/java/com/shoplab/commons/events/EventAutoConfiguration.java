package com.shoplab.commons.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cắm hạ tầng event (envelope · publisher · consumer base · dedupe) cho service có AMQP (SL-3.2).
 *
 * <p>Active khi: có {@link RabbitTemplate} trên classpath **và** {@code shoplab.events.enabled}
 * khác {@code false}. Service không dùng event (identity, gateway, cart…) không khai amqp ⇒ không
 * có bean nào được tạo, không có kết nối broker nào bị mở.
 *
 * <p><b>Vì sao phải khai {@code after = {Rabbit…, JdbcTemplate…}}</b> (bẫy thật gặp ở SL-3.2):
 * {@code @ConditionalOnBean} chỉ thấy bean **đã đăng ký tại thời điểm nó được đánh giá**. Không
 * khai thứ tự thì auto-config này chạy trước {@code JdbcTemplateAutoConfiguration} ⇒ điều kiện luôn
 * false ⇒ **store dedupe không bao giờ được tạo, không một lỗi nào** — bảng {@code
 * processed_events} đơn giản là không xuất hiện. Phải nói rõ "tôi phụ thuộc 2 auto-config kia".
 *
 * <p><b>Vì sao container factory riêng</b>: consumer cần {@code AcknowledgeMode.MANUAL} — việc
 * {@code ack}/{@code nack} là **quyết định nghiệp vụ** (đã dedupe chưa, retry cạn chưa) nên không
 * thể để Spring tự ack sau khi method trả về.
 */
@AutoConfiguration(
    after = RabbitAutoConfiguration.class,
    afterName = "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration")
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(name = "shoplab.events.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EventProperties.class)
public class EventAutoConfiguration {

  /** Codec dùng chung: cấu hình JSON của event nằm **một chỗ**. */
  @Bean
  @ConditionalOnMissingBean
  public EventEnvelopeCodec eventEnvelopeCodec(EventProperties properties) {
    return new EventEnvelopeCodec(
        new ObjectMapper().findAndRegisterModules(), properties.getMaxVersion());
  }

  @Bean
  @ConditionalOnMissingBean
  public EventPublisher eventPublisher(
      RabbitTemplate rabbitTemplate, EventEnvelopeCodec codec, EventProperties properties) {
    return new EventPublisher(
        rabbitTemplate, codec, properties.getPublishConfirmTimeout().toMillis());
  }

  /**
   * Dedupe store dùng DB của service. Chỉ tạo khi service có {@link JdbcTemplate} (service không có
   * DB — như cart — không rơi vào nhánh này).
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(JdbcTemplate.class)
  @ConditionalOnBean(JdbcTemplate.class)
  public ProcessedEventStore processedEventStore(JdbcTemplate jdbc, EventProperties properties) {
    JdbcProcessedEventStore store = new JdbcProcessedEventStore(jdbc);
    if (properties.isAutoCreateSchema()) {
      store.initSchema();
    }
    return store;
  }

  /**
   * Factory cho {@code @RabbitListener(containerFactory = "eventListenerContainerFactory")}.
   *
   * <p>{@code prefetch = 1} + {@code concurrency = 1} để **giữ thứ tự** trong một queue (ADR-0007
   * chỉ hứa ordering trong 1 queue + 1 consumer). Tăng song song là quyết định cần đo, không mặc
   * định.
   */
  @Bean
  @ConditionalOnMissingBean(name = "eventListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory eventListenerContainerFactory(
      ConnectionFactory connectionFactory, EventProperties properties) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
    factory.setPrefetchCount(1);
    factory.setConcurrentConsumers(1);
    factory.setMaxConcurrentConsumers(1);
    // IT chạy không có broker ⇒ tắt auto-startup bằng property (xem EventProperties).
    factory.setAutoStartup(properties.isConsumerAutoStartup());
    // Consumer tự parse JSON bằng EventEnvelopeCodec ⇒ converter chỉ chuyển byte[] nguyên bản.
    factory.setMessageConverter(new SimpleMessageConverter());
    return factory;
  }
}
