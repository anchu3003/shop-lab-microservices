package com.shoplab.promotion.lock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link VoucherLockRepository}. */
@Repository
public class JdbcVoucherLockRepository implements VoucherLockRepository {

  private static final String SELECT =
      """
      select id, voucher_id, order_id, status, subtotal, discount, expires_at, created_at
        from voucher_locks
      """;

  private final JdbcClient jdbc;

  public JdbcVoucherLockRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<VoucherLockRecord> findLockedByOrderId(UUID orderId) {
    return jdbc.sql(SELECT + " where order_id = :orderId and status = 'LOCKED'")
        .param("orderId", orderId)
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<VoucherLockRecord> findById(UUID id) {
    return jdbc.sql(SELECT + " where id = :id").param("id", id).query(this::map).optional();
  }

  @Override
  public void insert(VoucherLockRecord lock) {
    jdbc.sql(
            """
            insert into voucher_locks
              (id, voucher_id, order_id, status, subtotal, discount, expires_at, created_at, updated_at)
            values
              (:id, :voucherId, :orderId, :status, :subtotal, :discount, :expiresAt, :now, :now)
            """)
        .param("id", lock.id())
        .param("voucherId", lock.voucherId())
        .param("orderId", lock.orderId())
        .param("status", lock.status().name())
        .param("subtotal", lock.subtotal())
        .param("discount", lock.discount())
        .param("expiresAt", Timestamp.from(lock.expiresAt()))
        .param("now", Timestamp.from(lock.createdAt()))
        .update();
  }

  @Override
  public int markStatus(UUID id, VoucherLockStatus from, VoucherLockStatus to, Instant now) {
    return jdbc.sql(
            """
            update voucher_locks set status = :to, updated_at = :now
             where id = :id and status = :from
            """)
        .param("id", id)
        .param("from", from.name())
        .param("to", to.name())
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public List<VoucherLockRecord> findExpiredLocked(Instant now, int limit) {
    return jdbc.sql(
            SELECT
                + " where status = 'LOCKED' and expires_at < :now"
                + " order by expires_at"
                + " limit :limit"
                + " for update skip locked")
        .param("now", Timestamp.from(now))
        .param("limit", limit)
        .query(this::map)
        .list();
  }

  private VoucherLockRecord map(ResultSet rs, int rowNum) throws SQLException {
    return new VoucherLockRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("voucher_id", UUID.class),
        rs.getObject("order_id", UUID.class),
        VoucherLockStatus.valueOf(rs.getString("status")),
        rs.getBigDecimal("subtotal"),
        rs.getBigDecimal("discount"),
        rs.getTimestamp("expires_at").toInstant(),
        rs.getTimestamp("created_at").toInstant());
  }
}
