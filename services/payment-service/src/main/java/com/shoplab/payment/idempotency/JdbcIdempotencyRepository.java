package com.shoplab.payment.idempotency;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link IdempotencyRepository}. */
@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {

  private final JdbcClient jdbc;

  public JdbcIdempotencyRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * {@code ON CONFLICT DO NOTHING} là chỗ then chốt: hai request đồng thời cùng key thì DB đảm bảo
   * chỉ một cái insert được — không cần SELECT trước (SELECT-then-INSERT là khe hở kinh điển).
   */
  @Override
  public int insertIfAbsent(String scope, String key, String requestHash, Instant now) {
    return jdbc.sql(
            """
            insert into idempotency_keys (scope, key, request_hash, status, created_at)
            values (:scope, :key, :hash, 'IN_PROGRESS', :now)
            on conflict (scope, key) do nothing
            """)
        .param("scope", scope)
        .param("key", key)
        .param("hash", requestHash)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public Optional<IdempotencyRecord> find(String scope, String key) {
    return jdbc.sql(
            """
            select scope, key, request_hash, status, status_code, response_body, resource_id,
                   created_at, completed_at
              from idempotency_keys where scope = :scope and key = :key
            """)
        .param("scope", scope)
        .param("key", key)
        .query(
            (rs, rowNum) ->
                new IdempotencyRecord(
                    rs.getString("scope"),
                    rs.getString("key"),
                    rs.getString("request_hash"),
                    IdempotencyStatus.valueOf(rs.getString("status")),
                    rs.getObject("status_code", Integer.class),
                    rs.getString("response_body"),
                    rs.getObject("resource_id", UUID.class),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("completed_at") == null
                        ? null
                        : rs.getTimestamp("completed_at").toInstant()))
        .optional();
  }

  @Override
  public void complete(
      String scope, String key, int statusCode, String responseBody, UUID resourceId, Instant now) {
    jdbc.sql(
            """
            update idempotency_keys
               set status = 'COMPLETED', status_code = :statusCode, response_body = :body,
                   resource_id = :resourceId, completed_at = :now
             where scope = :scope and key = :key
            """)
        .param("scope", scope)
        .param("key", key)
        .param("statusCode", statusCode)
        .param("body", responseBody)
        .param("resourceId", resourceId, java.sql.Types.OTHER)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public int deleteOlderThan(Instant completedBefore, Instant inProgressBefore, int limit) {
    return jdbc.sql(
            """
            delete from idempotency_keys
             where (scope, key) in (
               select scope, key from idempotency_keys
                where (status = 'COMPLETED' and created_at < :completedBefore)
                   or (status = 'IN_PROGRESS' and created_at < :inProgressBefore)
                limit :limit
             )
            """)
        .param("completedBefore", Timestamp.from(completedBefore))
        .param("inProgressBefore", Timestamp.from(inProgressBefore))
        .param("limit", limit)
        .update();
  }
}
