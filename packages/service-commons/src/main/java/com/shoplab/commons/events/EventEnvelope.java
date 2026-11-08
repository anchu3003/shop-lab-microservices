package com.shoplab.commons.events;

import com.shoplab.commons.web.CorrelationId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope chuẩn của mọi event ShopLab — 7 field, chốt ở <a
 * href="../../../../../../../docs/adr/0007-event-contract.md">ADR-0007</a> (D1).
 *
 * <p><b>Không</b> đặt field nghiệp vụ ở tầng envelope: {@code payload} là chỗ duy nhất chứa dữ liệu
 * của event. Nhờ vậy consumer đọc được "cái khung" (ai phát, khi nào, loại gì, dedupe bằng gì) mà
 * không phải biết nghiệp vụ của service khác.
 *
 * @param eventId định danh **duy nhất toàn hệ** của lần phát này — consumer dedupe theo field này
 * @param eventType trùng khớp routing key, có hậu tố version: {@code order.confirmed.v1}
 * @param version lấy từ hậu tố {@code .v<N>}; consumer nhận version lạ phải đẩy DLQ (không cố
 *     parse)
 * @param occurredAt thời điểm **sự kiện xảy ra** (không phải lúc publish) — ISO-8601 UTC
 * @param producer tên service phát
 * @param traceId nối log xuyên service; lấy từ MDC nếu caller không truyền
 * @param payload dữ liệu nghiệp vụ (object JSON)
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String producer,
    String traceId,
    Map<String, Object> payload) {

  /** Hậu tố version trong {@code eventType}. */
  private static final String VERSION_MARKER = ".v";

  /**
   * Tạo envelope mới: sinh {@code eventId}, lấy {@code occurredAt = now}, {@code traceId} từ MDC
   * (nếu đang trong request) và parse {@code version} từ {@code eventType}.
   */
  public static EventEnvelope create(
      String eventType, String producer, Map<String, Object> payload) {
    return new EventEnvelope(
        UUID.randomUUID().toString(),
        eventType,
        versionOf(eventType),
        Instant.now(),
        producer,
        CorrelationId.current(),
        payload == null ? Map.of() : new LinkedHashMap<>(payload));
  }

  /**
   * Parse version từ {@code <aggregate>.<event>.v<N>} — sai format là lỗi lập trình, phải nổ ngay
   * (routing key sai thì RabbitMQ không báo gì, message rơi vào hư không).
   */
  public static int versionOf(String eventType) {
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType rỗng");
    }
    int idx = eventType.lastIndexOf(VERSION_MARKER);
    if (idx < 0) {
      throw new IllegalArgumentException(
          "eventType phải có hậu tố version, vd 'order.confirmed.v1': " + eventType);
    }
    try {
      return Integer.parseInt(eventType.substring(idx + VERSION_MARKER.length()));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("version không phải số trong eventType: " + eventType, ex);
    }
  }
}
