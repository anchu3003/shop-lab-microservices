package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoplab.commons.log.MdcKeys;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Codec = hợp đồng "message trên broker trông thế nào" (SL-3.2). */
class EventEnvelopeCodecTest {

  private final EventEnvelopeCodec codec =
      new EventEnvelopeCodec(new ObjectMapper().findAndRegisterModules(), 1);

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("encode → decode giữ nguyên 7 field")
  void shouldRoundTrip() {
    EventEnvelope original =
        new EventEnvelope(
            "evt-1",
            "order.confirmed.v1",
            1,
            java.time.Instant.parse("2026-11-08T09:15:04Z"),
            "order-service",
            "trace-1",
            Map.of("orderId", "o-1", "total", 200000));

    EventEnvelope decoded = codec.decode(codec.encode(original));

    assertThat(decoded).isEqualTo(original);
  }

  @Test
  @DisplayName("create(): sinh eventId, parse version từ eventType, traceId lấy từ MDC")
  void shouldCreateWithTraceIdFromMdc() {
    MDC.put(MdcKeys.TRACE_ID, "trace-from-request");

    EventEnvelope envelope =
        EventEnvelope.create("payment.succeeded.v1", "payment-service", Map.of("orderId", "o-1"));

    assertThat(envelope.eventId()).isNotBlank();
    assertThat(envelope.version()).isEqualTo(1);
    assertThat(envelope.traceId()).isEqualTo("trace-from-request");
    assertThat(envelope.occurredAt()).isNotNull();
  }

  @Test
  @DisplayName("version cao hơn code hiểu → EventFormatException (consumer cũ phải đẩy DLQ)")
  void shouldRejectUnsupportedVersion() {
    String json =
        """
        {"eventId":"evt-2","eventType":"order.confirmed.v2","version":2,
         "occurredAt":"2026-11-08T09:15:04Z","producer":"order-service","traceId":"t","payload":{}}
        """;

    assertThatThrownBy(() -> codec.decode(json))
        .isInstanceOf(EventFormatException.class)
        .hasMessageContaining("chưa được hỗ trợ");
  }

  @Test
  @DisplayName("version trong payload khác hậu tố eventType → từ chối (không đoán)")
  void shouldRejectVersionMismatch() {
    String json =
        """
        {"eventId":"evt-3","eventType":"order.confirmed.v1","version":2,
         "occurredAt":"2026-11-08T09:15:04Z","producer":"order-service","traceId":"t","payload":{}}
        """;

    assertThatThrownBy(() -> codec.decode(json))
        .isInstanceOf(EventFormatException.class)
        .hasMessageContaining("khác hậu tố");
  }

  @Test
  @DisplayName("JSON hỏng / thiếu eventId / eventType không có hậu tố version → từ chối")
  void shouldRejectBrokenMessages() {
    assertThatThrownBy(() -> codec.decode("không phải json"))
        .isInstanceOf(EventFormatException.class);
    assertThatThrownBy(
            () ->
                codec.decode(
                    """
                    {"eventType":"order.confirmed.v1","version":1,
                     "occurredAt":"2026-11-08T09:15:04Z","producer":"p","payload":{}}
                    """))
        .isInstanceOf(EventFormatException.class)
        .hasMessageContaining("eventId");
    assertThatThrownBy(() -> EventEnvelope.versionOf("order.confirmed"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("hậu tố version");
  }
}
