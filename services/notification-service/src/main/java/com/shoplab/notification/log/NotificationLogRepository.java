package com.shoplab.notification.log;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Truy cập `notification_logs` + `notification_templates` (SL-3.4).
 *
 * <p><b>`event_id` có UNIQUE</b> ở DB: nếu code dedupe sai (hoặc 2 consumer cùng chạy) thì INSERT
 * thứ hai **nổ** thay vì gửi email lần 2 — bất biến ở tầng dữ liệu, không chỉ ở tầng ứng dụng.
 */
@Repository
public class NotificationLogRepository {

  private final JdbcTemplate jdbc;

  public NotificationLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * @return mẫu template theo `code`, rỗng nếu chưa seed
   */
  public Optional<Template> findTemplate(String code) {
    List<Template> rows =
        jdbc.query(
            "SELECT subject, body FROM notification_templates WHERE code = ?",
            (rs, i) -> new Template(rs.getString("subject"), rs.getString("body")),
            code);
    return rows.stream().findFirst();
  }

  /** Dữ liệu của một lần "gửi" — gộp thành record để không truyền 8 tham số rời (Checkstyle). */
  public record Draft(
      UUID eventId,
      UUID userId,
      UUID orderId,
      String template,
      String recipient,
      String subject,
      String body,
      Instant sentAt) {}

  @Transactional
  public void save(Draft draft) {
    jdbc.update(
        """
        INSERT INTO notification_logs (event_id, user_id, order_id, channel, template, recipient,
                                       subject, body, status, sent_at)
        VALUES (?, ?, ?, 'EMAIL', ?, ?, ?, ?, 'SENT', ?)
        """,
        draft.eventId(),
        draft.userId(),
        draft.orderId(),
        draft.template(),
        draft.recipient(),
        draft.subject(),
        draft.body(),
        java.sql.Timestamp.from(draft.sentAt()));
  }

  /** Log của một user, mới nhất trước (dùng cho `GET /v1/notifications/me`). */
  public List<NotificationLog> findByUser(UUID userId, int limit) {
    return jdbc.query(
        """
        SELECT id, event_id, user_id, order_id, channel, template, recipient, subject, body, status,
               created_at, sent_at
        FROM notification_logs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?
        """,
        rowMapper(),
        userId,
        limit);
  }

  /** Template đã render — chỉ 2 field cần cho việc gửi. */
  public record Template(String subject, String body) {}

  /** Response của API: **không** trả `body` đầy đủ? Không — trả cả body để verify được bằng mắt. */
  public record NotificationLog(
      UUID id,
      UUID eventId,
      UUID userId,
      UUID orderId,
      String channel,
      String template,
      String recipient,
      String subject,
      String body,
      String status,
      Instant createdAt,
      Instant sentAt) {}

  /**
   * Xoá log cũ hơn {@code cutoff} (job retention — `TD-30`, `SL-4.0`).
   *
   * <p>Điều kiện tuổi dùng {@code sent_at} (không `created_at`): thứ khách tra là "email tôi
   * **nhận** lúc nào". Row chưa gửi được (`sent_at IS NULL`) **không** bị xoá bởi job này — chúng
   * là dấu vết của một lần gửi **hỏng**, cần người xem chứ không phải dữ liệu cũ.
   *
   * <p>Batch + subquery: xem {@code JdbcProcessedEventStore.deleteProcessedBefore} (Postgres không
   * nhận {@code LIMIT} trong DELETE).
   *
   * @param cutoff xoá row có {@code sent_at < cutoff}
   * @param batch số row tối đa mỗi lần gọi
   * @return số row đã xoá
   */
  @Transactional
  public int deleteOlderThan(Instant cutoff, int batch) {
    return jdbc.update(
        """
        DELETE FROM notification_logs
        WHERE id IN (
          SELECT id FROM notification_logs
          WHERE sent_at IS NOT NULL AND sent_at < ?
          ORDER BY sent_at
          LIMIT ?
        )
        """,
        Timestamp.from(cutoff),
        batch);
  }

  private RowMapper<NotificationLog> rowMapper() {
    return (rs, i) ->
        new NotificationLog(
            rs.getObject("id", UUID.class),
            rs.getObject("event_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("order_id", UUID.class),
            rs.getString("channel"),
            rs.getString("template"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant());
  }
}
