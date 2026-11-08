package com.shoplab.notification.notify;

import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.notification.config.NotificationProperties;
import com.shoplab.notification.log.NotificationLogRepository;
import com.shoplab.notification.template.TemplateRenderer;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * "Gửi" thông báo cho một event (SL-3.4) — mock provider: render template rồi ghi
 * `notification_logs`.
 *
 * <p><b>Thứ tự cố ý</b>: render → ghi log → log "đã gửi". Không có SMTP nên "gửi" = ghi log; điều
 * này vẫn đủ để verify async end-to-end (email không tới được thì dòng log cũng không có).
 *
 * <p><b>Lỗi được ném thẳng ra</b> để consumer base retry rồi đẩy DLQ — service này **không** tự
 * nuốt lỗi: nuốt lỗi là biến "gửi thất bại" thành "đã gửi" trong mắt hệ thống.
 */
@Service
public class NotificationService {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

  /** Tiền tố email giả — mock provider, không gửi ra ngoài. */
  private static final String RECIPIENT_DOMAIN = "@shoplab.local";

  private final NotificationLogRepository logs;
  private final TemplateRenderer renderer;
  private final NotificationProperties properties;
  private final Clock clock;

  public NotificationService(
      NotificationLogRepository logs,
      TemplateRenderer renderer,
      NotificationProperties properties,
      Clock clock) {
    this.logs = logs;
    this.renderer = renderer;
    this.properties = properties;
    this.clock = clock;
  }

  /** Xử lý `order.confirmed.v1`. */
  public void onOrderConfirmed(EventEnvelope event) {
    Map<String, Object> payload = event.payload();
    Map<String, Object> vars = new LinkedHashMap<>(payload);
    send(event, "order.confirmed", uuid(payload, "userId"), uuid(payload, "orderId"), vars);
  }

  /** Xử lý `order.cancelled.v1`. */
  public void onOrderCancelled(EventEnvelope event) {
    Map<String, Object> payload = event.payload();
    Map<String, Object> vars = new LinkedHashMap<>(payload);
    send(event, "order.cancelled", uuid(payload, "userId"), uuid(payload, "orderId"), vars);
  }

  private void send(
      EventEnvelope event,
      String templateCode,
      UUID userId,
      UUID orderId,
      Map<String, Object> vars) {
    failIfConfigured(templateCode);

    NotificationLogRepository.Template template =
        logs.findTemplate(templateCode)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "thiếu template '"
                            + templateCode
                            + "' — seed ở V1 hoặc bổ sung migration"));

    String subject = renderer.render(template.subject(), vars);
    String body = renderer.render(template.body(), vars);
    String recipient = shortId(userId) + RECIPIENT_DOMAIN;
    Instant now = Instant.now(clock);

    logs.save(
        new NotificationLogRepository.Draft(
            UUID.fromString(event.eventId()),
            userId,
            orderId,
            templateCode,
            recipient,
            subject,
            body,
            now));

    LOG.info(
        "đã gửi {} tới {} (event={} order={}) — mock provider",
        templateCode,
        recipient,
        event.eventId(),
        orderId);
  }

  /**
   * Bơm lỗi để test retry/DLQ — **chỉ hoạt động khi `shoplab.env=local`**.
   *
   * <p>Guard ở đây (không phải chỉ `@ConditionalOnProperty`) vì đọc env rồi mới quyết định: cấu
   * hình `fail-mode=true` lọt sang môi trường khác cũng **không** làm hỏng gì (chỉ log cảnh báo) —
   * fail-closed.
   */
  private void failIfConfigured(String templateCode) {
    if (!properties.failMode()) {
      return;
    }
    if (!"local".equalsIgnoreCase(properties.env())) {
      LOG.warn("SHOPLAB_NOTIFICATION_FAIL_MODE bị bỏ qua vì shoplab.env != local");
      return;
    }
    throw new IllegalStateException(
        "bơm lỗi theo SHOPLAB_NOTIFICATION_FAIL_MODE (" + templateCode + ")");
  }

  private UUID uuid(Map<String, Object> payload, String field) {
    Object value = payload.get(field);
    if (value == null) {
      throw new IllegalArgumentException("payload thiếu field bắt buộc: " + field);
    }
    return UUID.fromString(String.valueOf(value));
  }

  private String shortId(UUID userId) {
    return "user-" + userId.toString().substring(0, 8);
  }
}
