package com.shoplab.commons.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dedupe bằng bảng trong DB của consumer (SL-3.2).
 *
 * <p><b>DDL</b> (service nào cũng phải có bảng này; commons tạo hộ nếu {@code
 * shoplab.events.dedup.auto-create-schema=true}):
 *
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS processed_events (
 *   event_id     uuid        NOT NULL,
 *   consumer     varchar(64) NOT NULL,
 *   event_type   varchar(128) NOT NULL,
 *   processed_at timestamptz NOT NULL DEFAULT now(),
 *   PRIMARY KEY (event_id, consumer)
 * );
 * }</pre>
 *
 * <p><b>Khoá chính {@code (event_id, consumer)}</b> chứ không chỉ {@code event_id}: hai consumer
 * khác nhau **phải** cùng xử lý một event (notification gửi mail, search index) — dedupe là theo
 * consumer, không phải theo hệ.
 *
 * <p><b>Vì sao {@code insert … on conflict do nothing} + {@code rowsAffected}</b>: đó là
 * compare-and-set **nguyên tử** ở DB. Kiểu "select rồi insert" sẽ có khe hở giữa 2 bước (2 lần
 * redeliver đồng thời cùng lọt).
 */
public class JdbcProcessedEventStore implements ProcessedEventStore {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcProcessedEventStore.class);

  /** DDL được ship kèm để service có thể tự đưa vào Flyway thay vì auto-create. */
  public static final String SCHEMA_SQL =
      """
      CREATE TABLE IF NOT EXISTS processed_events (
        event_id     uuid        NOT NULL,
        consumer     varchar(64) NOT NULL,
        event_type   varchar(128) NOT NULL,
        processed_at timestamptz NOT NULL DEFAULT now(),
        PRIMARY KEY (event_id, consumer)
      )
      """;

  private final JdbcTemplate jdbc;

  public JdbcProcessedEventStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Tạo bảng nếu chưa có (idempotent) — gọi lúc khởi động khi bật auto-create. */
  public void initSchema() {
    try {
      jdbc.execute(SCHEMA_SQL);
    } catch (RuntimeException ex) {
      // Không chặn khởi động: consumer sẽ tự lỗi và message vào DLQ nếu bảng thật sự thiếu.
      LOG.warn("không tạo được bảng processed_events: {}", ex.getMessage());
    }
  }

  @Override
  public boolean markProcessed(String consumer, EventEnvelope envelope) {
    int rows =
        jdbc.update(
            "INSERT INTO processed_events (event_id, consumer, event_type) VALUES (?, ?, ?) "
                + "ON CONFLICT (event_id, consumer) DO NOTHING",
            java.util.UUID.fromString(envelope.eventId()),
            consumer,
            envelope.eventType());
    return rows > 0;
  }

  @Override
  public void unmarkProcessed(String consumer, EventEnvelope envelope) {
    jdbc.update(
        "DELETE FROM processed_events WHERE event_id = ? AND consumer = ?",
        java.util.UUID.fromString(envelope.eventId()),
        consumer);
  }
}
