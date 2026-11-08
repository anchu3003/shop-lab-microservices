package com.shoplab.commons.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Chuyển {@link EventEnvelope} ⇄ JSON. Một chỗ duy nhất quyết định "message trên broker trông thế
 * nào" (SL-3.2).
 *
 * <p><b>Vì sao validate ở đây</b>: consumer nhận message có thể là message **cũ** (nằm trong DLQ từ
 * lần deploy trước) hoặc do người khác publish sai. Validate ở biên (decode) thì lỗi dừng ngay tại
 * chỗ có đủ ngữ cảnh để log, thay vì nổ giữa handler nghiệp vụ.
 */
public class EventEnvelopeCodec {

  private final ObjectMapper mapper;

  /** Version cao nhất mà bản code này hiểu được (mặc định 1 — Phase 3 chỉ có `.v1`). */
  private final int maxSupportedVersion;

  public EventEnvelopeCodec(ObjectMapper mapper, int maxSupportedVersion) {
    this.mapper = mapper;
    this.maxSupportedVersion = maxSupportedVersion;
  }

  /** Encode envelope → JSON (dùng cho publisher). */
  public String encode(EventEnvelope envelope) {
    try {
      return mapper.writeValueAsString(envelope);
    } catch (Exception ex) {
      throw new EventFormatException("không encode được envelope " + envelope.eventId(), ex);
    }
  }

  /**
   * Decode JSON → envelope + validate.
   *
   * @throws EventFormatException khi JSON hỏng, thiếu field bắt buộc, {@code version} không khớp
   *     với hậu tố của {@code eventType}, hoặc version **cao hơn** bản code này hiểu (⇒ DLQ, không
   *     cố parse — ADR-0007 § Consequences)
   */
  public EventEnvelope decode(String json) {
    EventEnvelope envelope;
    try {
      envelope = mapper.readValue(json, EventEnvelope.class);
    } catch (Exception ex) {
      throw new EventFormatException("JSON không parse được thành EventEnvelope", ex);
    }

    validate(envelope);
    return envelope;
  }

  private void validate(EventEnvelope envelope) {
    if (envelope.eventId() == null || envelope.eventId().isBlank()) {
      throw new EventFormatException("thiếu eventId", null);
    }
    if (envelope.producer() == null || envelope.producer().isBlank()) {
      throw new EventFormatException("thiếu producer", null);
    }
    if (envelope.occurredAt() == null) {
      throw new EventFormatException("thiếu occurredAt", null);
    }

    int declared = EventEnvelope.versionOf(envelope.eventType());
    if (declared != envelope.version()) {
      throw new EventFormatException(
          "version trong payload (%d) khác hậu tố eventType (%s)"
              .formatted(envelope.version(), envelope.eventType()),
          null);
    }
    if (envelope.version() > maxSupportedVersion) {
      throw new EventFormatException(
          "version %d chưa được hỗ trợ (max %d) — consumer cũ phải để message vào DLQ"
              .formatted(envelope.version(), maxSupportedVersion),
          null);
    }
  }

  /** Payload rỗng nếu message legacy không có field {@code payload}. */
  static Map<String, Object> emptyPayload() {
    return Map.of();
  }
}
