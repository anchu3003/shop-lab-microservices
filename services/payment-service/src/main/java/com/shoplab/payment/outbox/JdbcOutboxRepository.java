package com.shoplab.payment.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation của outbox (SL-3.5, chép khuôn `SL-3.3`) — cần `RETURNING` + `SKIP LOCKED`
 * nên JDBC chứ không JPA. `order` là nơi pattern này ra đời; `payment` dùng lại nguyên xi vì bài
 * toán giống hệt: ghi event cùng transaction với thay đổi trạng thái tiền.
 */
@Repository
public class JdbcOutboxRepository implements OutboxRepository {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;

  // Tự tạo mapper thay vì inject: không phụ thuộc bean `ObjectMapper` của web layer (đúng 2 thao
  // tác
  // JSON ở đây). Cùng cách làm với `EventEnvelopeCodec` (commons).
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public JdbcOutboxRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void append(
      String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
    jdbc.update(
        "INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload) "
            + "VALUES (?, ?, ?, ?::jsonb)",
        aggregateType,
        aggregateId,
        eventType,
        writeJson(payload));
  }

  /**
   * Claim + tăng {@code attempts} trong **một câu UPDATE** với subquery `SKIP LOCKED`.
   *
   * <p>Một câu là đủ để nguyên tử: subquery chọn row chưa ai giữ (hoặc claim đã quá hạn), UPDATE
   * đánh dấu trong cùng transaction ngắn ⇒ hai relay chạy song song không thể lấy cùng một row.
   */
  @Override
  @Transactional
  public List<OutboxRecord> claimPending(int batch, Instant now, Instant claimDeadline) {
    return jdbc.query(
        """
        UPDATE outbox SET claimed_at = ?, attempts = attempts + 1
        WHERE id IN (
          SELECT id FROM outbox
          WHERE status = 'PENDING'
            AND (claimed_at IS NULL OR claimed_at < ?)
          ORDER BY created_at
          LIMIT ?
          FOR UPDATE SKIP LOCKED
        )
        RETURNING id, aggregate_type, aggregate_id, event_type, payload, status, attempts,
                  last_error, claimed_at, created_at, sent_at
        """,
        rowMapper(),
        Timestamp.from(now),
        Timestamp.from(claimDeadline),
        batch);
  }

  @Override
  public void markSent(UUID id, Instant now) {
    jdbc.update(
        "UPDATE outbox SET status = 'SENT', sent_at = ?, claimed_at = NULL WHERE id = ?",
        Timestamp.from(now),
        id);
  }

  @Override
  public void markFailed(UUID id, String error) {
    // claimed_at = NULL: trả row ngay cho nhịp quét sau (không phải chờ hết visibility timeout).
    jdbc.update(
        "UPDATE outbox SET last_error = ?, claimed_at = NULL WHERE id = ?", trim(error), id);
  }

  @Override
  public long countByStatus(OutboxStatus status) {
    Long count =
        jdbc.queryForObject(
            "SELECT count(*) FROM outbox WHERE status = ?", Long.class, status.name());
    return count == null ? 0 : count;
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new IllegalStateException("không serialize được payload outbox", ex);
    }
  }

  private RowMapper<OutboxRecord> rowMapper() {
    return (rs, rowNum) ->
        new OutboxRecord(
            rs.getObject("id", UUID.class),
            rs.getString("aggregate_type"),
            rs.getObject("aggregate_id", UUID.class),
            rs.getString("event_type"),
            readJson(rs),
            OutboxStatus.valueOf(rs.getString("status")),
            rs.getInt("attempts"),
            rs.getString("last_error"),
            instant(rs, "claimed_at"),
            instant(rs, "created_at"),
            instant(rs, "sent_at"));
  }

  private Map<String, Object> readJson(ResultSet rs) throws SQLException {
    String raw = rs.getString("payload");
    try {
      return mapper.readValue(raw, MAP_TYPE);
    } catch (Exception ex) {
      throw new IllegalStateException("payload outbox không phải JSON hợp lệ: " + raw, ex);
    }
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts == null ? null : ts.toInstant();
  }

  /** Lỗi dài (stack message) không được làm phình bảng — chỉ giữ 500 ký tự đầu. */
  private String trim(String error) {
    if (error == null) {
      return null;
    }
    return error.length() <= 500 ? error : error.substring(0, 500);
  }
}
