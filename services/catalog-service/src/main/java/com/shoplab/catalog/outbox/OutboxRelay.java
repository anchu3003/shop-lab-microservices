package com.shoplab.catalog.outbox;

import com.shoplab.commons.events.EventEnvelope;
import com.shoplab.commons.events.EventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay của outbox (chép khuôn `SL-3.3`, dùng cho catalog từ `SL-3.6`): biến "dòng trong bảng"
 * thành "message trên broker".
 *
 * <p>Khác bản `order`/`payment` đúng một chỗ: {@code producer} trong envelope là {@code
 * catalog-service} — consumer dùng field đó để biết ai phát.
 *
 * <p><b>{@code eventId = outbox.id}</b> — chi tiết quan trọng nhất của class này. Nếu mỗi lần
 * publish lại sinh `eventId` mới thì retry (sau khi broker nack) sẽ tạo **event khác** ⇒ consumer
 * dedupe theo `eventId` không chặn được ⇒ gửi email 2 lần. Dùng id của row làm `eventId` thì **mọi
 * lần thử của cùng một event mang cùng một id** — đúng ngữ nghĩa at-least-once + dedupe (ADR-0007
 * D3).
 *
 * <p><b>Vì sao claim rồi mới publish</b>: claim là transaction ngắn (UPDATE … SKIP LOCKED). Publish
 * nằm **ngoài** transaction để broker chậm không giữ row lock của Postgres; nếu publish fail thì
 * trả claim + `attempts++` để nhịp sau thử lại — event **không** bị mất, chỉ bị trễ.
 *
 * <p>Lỗi ở một row không làm hỏng cả nhịp: mỗi row bọc try/catch riêng (cùng bài học `SL-2.3`).
 */
@Component
public class OutboxRelay {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

  private final OutboxRepository outbox;
  private final EventPublisher publisher;
  private final OutboxProperties properties;
  private final Clock clock;

  public OutboxRelay(
      OutboxRepository outbox, EventPublisher publisher, OutboxProperties properties, Clock clock) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.outbox.interval:1s}",
      initialDelayString = "${shoplab.outbox.initial-delay:5s}")
  public void relay() {
    try {
      drain();
    } catch (RuntimeException ex) {
      // Exception lọt ra khỏi @Scheduled làm nhịp đó chết + log đầy stack trace (bài học SL-2.3).
      LOG.warn("outbox relay: nhịp quét lỗi — bỏ qua nhịp này: {}", ex.getMessage());
    }
  }

  /**
   * Một nhịp quét.
   *
   * @return số message publish thành công trong nhịp này
   */
  public int drain() {
    Instant now = Instant.now(clock);
    List<OutboxRecord> rows =
        outbox.claimPending(
            properties.getBatchSize(), now, now.minus(properties.getClaimTimeout()));
    if (rows.isEmpty()) {
      return 0;
    }

    int sent = 0;
    for (OutboxRecord row : rows) {
      try {
        publisher.publish(toEnvelope(row));
        outbox.markSent(row.id(), Instant.now(clock));
        sent++;
      } catch (RuntimeException ex) {
        outbox.markFailed(row.id(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
        LOG.warn(
            "outbox relay: chưa gửi được {} (attempt {}): {}",
            row.id(),
            row.attempts(),
            ex.getMessage());
      }
    }
    LOG.info("outbox relay: {} / {} message đã gửi", sent, rows.size());
    return sent;
  }

  private EventEnvelope toEnvelope(OutboxRecord row) {
    // eventId CỐ ĐỊNH theo row (không sinh mới mỗi lần thử) — xem javadoc class.
    return new EventEnvelope(
        row.id().toString(),
        row.eventType(),
        EventEnvelope.versionOf(row.eventType()),
        row.createdAt(),
        "catalog-service",
        null,
        row.payload());
  }
}
