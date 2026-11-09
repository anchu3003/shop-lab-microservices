package com.shoplab.search.mq;

import com.shoplab.commons.events.AbstractEventConsumer;
import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventEnvelopeCodec;
import com.shoplab.commons.events.EventTopology;
import com.shoplab.commons.events.ProcessedEventStore;
import com.shoplab.search.config.SearchProperties;
import com.shoplab.search.index.SearchIndexService;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer của `q.search` (SL-3.6) — giữ index đồng bộ với catalog.
 *
 * <p>Toàn bộ retry/dedupe/DLQ nằm ở {@link AbstractEventConsumer} (`SL-3.2`); đây là <b>consumer
 * thứ ba</b> dùng lại hạ tầng đó (sau notification, order) — đúng mục tiêu "service mới không phải
 * copy lại hạ tầng event".
 *
 * <p><b>Vì sao ném lỗi khi payload hỏng</b> (khác `order` ack các ca nghiệp vụ): một payload
 * product thiếu `productId`/`price` là <b>message độc</b> — retry 3 lần rồi DLQ, không có "kết cục
 * nghiệp vụ" nào để ack. Index thiếu một product thì reconcile sẽ dựng lại; index ghi một dòng rác
 * thì không ai biết.
 */
@Component
public class ProductEventConsumer extends AbstractEventConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(ProductEventConsumer.class);

  private final SearchIndexService index;

  public ProductEventConsumer(
      EventEnvelopeCodec codec,
      ProcessedEventStore store,
      SearchProperties properties,
      SearchIndexService index) {
    super(codec, store, properties.retryDelays());
    this.index = index;
  }

  @Override
  protected String consumerName() {
    // Ổn định: một nửa của khoá dedupe `(event_id, consumer)`.
    return "search-service";
  }

  @Override
  protected Set<String> handledEventTypes() {
    return Set.of(EventTopology.ROUTING_PRODUCT_UPDATED, EventTopology.ROUTING_PRODUCT_ARCHIVED);
  }

  @Override
  protected void handleEvent(EventEnvelope envelope) {
    switch (envelope.eventType()) {
      case EventTopology.ROUTING_PRODUCT_UPDATED -> index.applyUpdated(envelope.payload());
      case EventTopology.ROUTING_PRODUCT_ARCHIVED ->
          index.applyArchived(envelope.payload(), envelope.occurredAt());
      default -> LOG.warn("bỏ qua event không xử lý: {}", envelope.eventType());
    }
  }

  /** Listener tách khỏi xử lý để `onMessage` (base) test được **không cần broker**. */
  @RabbitListener(
      queues = EventTopology.QUEUE_SEARCH,
      containerFactory = "eventListenerContainerFactory")
  public void onEvent(Message message, com.rabbitmq.client.Channel channel) throws IOException {
    onMessage(message, channel);
  }
}
