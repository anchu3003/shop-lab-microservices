package com.shoplab.commons.events;

import com.rabbitmq.client.Channel;
import com.shoplab.commons.log.MdcKeys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * Khung consumer event dùng chung (SL-3.2): parse → dedupe → handler → ack, lỗi ⇒ retry ⇒ DLQ.
 *
 * <p>Service chỉ cần: kế thừa class này, khai {@link #consumerName()}, {@link
 * #handledEventTypes()}, {@link #handleEvent(EventEnvelope)} và đăng ký listener với {@code
 * containerFactory = "eventListenerContainerFactory"} (ack thủ công).
 *
 * <p><b>Thứ tự xử lý trong một message</b> (quan trọng, đừng đảo):
 *
 * <ol>
 *   <li>Decode + validate. Hỏng/version lạ ⇒ {@code nack(requeue=false)} → DLQ ngay, **không**
 *       retry (retry payload sai chỉ tốn 4,2 s và che mất tín hiệu).
 *   <li>Event không thuộc {@link #handledEventTypes()} ⇒ ack + bỏ qua (một queue có thể bind nhiều
 *       routing key, consumer có thể chỉ quan tâm một phần).
 *   <li>Dedupe theo {@code (eventId, consumer)} **trước** khi xử lý (chặn redeliver đồng thời), và
 *       **xoá dấu** nếu xử lý thất bại — marker chỉ giữ khi đã thành công, nếu không DLQ replay sẽ
 *       bị chính nó chặn.
 *   <li>Handler: retry theo {@code retryDelays} (mặc định 200 ms/1 s/3 s). Cạn ⇒ xoá dấu + {@code
 *       nack(requeue=false)} → DLQ.
 * </ol>
 *
 * <p>Mọi nhánh log đều có {@code eventId} + {@code traceId} của **producer** (đặt lại vào MDC trong
 * lúc xử lý) ⇒ grep một id là thấy cả chuỗi từ service phát sang service nhận.
 */
public abstract class AbstractEventConsumer implements ChannelAwareMessageListener {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEventConsumer.class);

  private final EventEnvelopeCodec codec;
  private final ProcessedEventStore store;
  private final List<Duration> retryDelays;

  protected AbstractEventConsumer(
      EventEnvelopeCodec codec, ProcessedEventStore store, List<Duration> retryDelays) {
    this.codec = codec;
    this.store = store;
    this.retryDelays = List.copyOf(retryDelays);
  }

  /**
   * Tên consumer dùng trong {@code processed_events} + log; phải **ổn định** qua các lần deploy.
   */
  protected abstract String consumerName();

  /** Các {@code eventType} mà consumer này thực sự xử lý (vd {@code order.confirmed.v1}). */
  protected abstract Set<String> handledEventTypes();

  /** Xử lý nghiệp vụ. Ném exception ⇒ được coi là lỗi tạm thời (sẽ retry rồi vào DLQ). */

  /** Xử lý nghiệp vụ. Ném exception ⇒ coi là lỗi tạm thời (retry rồi vào DLQ). */
  protected abstract void handleEvent(EventEnvelope envelope);

  @Override
  public void onMessage(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    String raw = new String(message.getBody(), StandardCharsets.UTF_8);

    EventEnvelope envelope;
    try {
      envelope = codec.decode(raw);
    } catch (EventFormatException ex) {
      // Không retry: payload sai thì retry chỉ tốn thời gian. Đẩy DLQ để người xem lại.
      LOG.error("[{}] message không hợp lệ → DLQ: {}", consumerName(), ex.getMessage());
      channel.basicNack(deliveryTag, false, false);
      return;
    }

    String previousTraceId = MDC.get(MdcKeys.TRACE_ID);
    if (envelope.traceId() != null && !envelope.traceId().isBlank()) {
      MDC.put(MdcKeys.TRACE_ID, envelope.traceId());
    }
    try {
      if (!handledEventTypes().contains(envelope.eventType())) {
        LOG.warn(
            "[{}] bỏ qua event không thuộc consumer: {} ({})",
            consumerName(),
            envelope.eventType(),
            envelope.eventId());
        channel.basicAck(deliveryTag, false);
        return;
      }

      if (!store.markProcessed(consumerName(), envelope)) {
        LOG.info(
            "[{}] event {} đã xử lý trước đó — ack, bỏ qua", consumerName(), envelope.eventId());
        channel.basicAck(deliveryTag, false);
        return;
      }

      try {
        handleWithRetry(envelope);
      } catch (RuntimeException ex) {
        // Xoá dấu trước khi DLQ: nếu không, replay từ DLQ sẽ bị dedupe chặn và không bao giờ chạy
        // lại.
        store.unmarkProcessed(consumerName(), envelope);
        LOG.error(
            "[{}] xử lý event {} thất bại sau {} lần thử → DLQ",
            consumerName(),
            envelope.eventId(),
            retryDelays.size() + 1,
            ex);
        channel.basicNack(deliveryTag, false, false);
        return;
      }

      channel.basicAck(deliveryTag, false);
      LOG.info(
          "[{}] xử lý xong event {} ({})",
          consumerName(),
          envelope.eventType(),
          envelope.eventId());

    } finally {
      if (previousTraceId == null) {
        MDC.remove(MdcKeys.TRACE_ID);
      } else {
        MDC.put(MdcKeys.TRACE_ID, previousTraceId);
      }
    }
  }

  /** Thử lần đầu + retry theo {@code retryDelays}; ném lại exception cuối cùng nếu tất cả fail. */
  private void handleWithRetry(EventEnvelope envelope) {
    RuntimeException last = null;
    for (int attempt = 0; attempt <= retryDelays.size(); attempt++) {
      try {
        handleEvent(envelope);
        return;
      } catch (RuntimeException ex) {
        last = ex;
        if (attempt < retryDelays.size()) {
          Duration delay = retryDelays.get(attempt);
          LOG.warn(
              "[{}] lỗi xử lý {} (lần {}): {} — thử lại sau {} ms",
              consumerName(),
              envelope.eventId(),
              attempt + 1,
              ex.getMessage(),
              delay.toMillis());
          sleep(delay);
        }
      }
    }
    throw last == null ? new IllegalStateException("handler không chạy") : last;
  }

  /** Tách riêng để test bỏ qua thời gian chờ (truyền {@code retryDelays} rỗng). */
  void sleep(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("bị interrupt khi retry", ex);
    }
  }
}
