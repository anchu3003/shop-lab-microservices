package com.shoplab.inventory.reservation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link ReservationRepository}. */
@Repository
public class JdbcReservationRepository implements ReservationRepository {

  private static final String SELECT =
      """
      select id, order_id, status, expires_at, created_at
        from reservations
      """;

  private final JdbcClient jdbc;

  public JdbcReservationRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ReservationRecord> findPendingByOrderId(UUID orderId) {
    return jdbc.sql(SELECT + " where order_id = :orderId and status = 'PENDING'")
        .param("orderId", orderId)
        .query(this::mapReservation)
        .optional();
  }

  @Override
  public Optional<ReservationRecord> findById(UUID id) {
    return jdbc.sql(SELECT + " where id = :id")
        .param("id", id)
        .query(this::mapReservation)
        .optional();
  }

  @Override
  public void insert(ReservationRecord reservation, List<ReservationItemRecord> items) {
    jdbc.sql(
            """
            insert into reservations (id, order_id, status, expires_at, created_at, updated_at)
            values (:id, :orderId, :status, :expiresAt, :now, :now)
            """)
        .param("id", reservation.id())
        .param("orderId", reservation.orderId())
        .param("status", reservation.status().name())
        .param("expiresAt", Timestamp.from(reservation.expiresAt()))
        .param("now", Timestamp.from(reservation.createdAt()))
        .update();

    for (ReservationItemRecord item : items) {
      jdbc.sql(
              """
              insert into reservation_items (reservation_id, sku_id, qty)
              values (:reservationId, :skuId, :qty)
              """)
          .param("reservationId", reservation.id())
          .param("skuId", item.skuId())
          .param("qty", item.qty())
          .update();
    }
  }

  @Override
  public List<ReservationItemRecord> findItems(UUID reservationId) {
    return jdbc.sql(
            "select sku_id, qty from reservation_items where reservation_id = :id order by sku_id")
        .param("id", reservationId)
        .query(
            (rs, rowNum) ->
                new ReservationItemRecord(rs.getObject("sku_id", UUID.class), rs.getInt("qty")))
        .list();
  }

  /**
   * Reservation {@code PENDING} quá hạn, theo thứ tự quá hạn lâu nhất trước.
   *
   * <p>Dùng {@code FOR UPDATE SKIP LOCKED} + {@code LIMIT}: Postgres lock các row trả về và <b>bỏ
   * qua</b> row đang bị instance khác lock. Nhờ vậy chạy N instance sweeper vẫn đúng, không cần
   * advisory lock.
   */
  @Override
  public List<ReservationRecord> findExpiredPending(Instant now, int limit) {
    return jdbc.sql(
            SELECT
                + " where status = 'PENDING' and expires_at < :now"
                + " order by expires_at"
                + " limit :limit"
                + " for update skip locked")
        .param("now", Timestamp.from(now))
        .param("limit", limit)
        .query(this::mapReservation)
        .list();
  }

  @Override
  public int markStatus(UUID id, ReservationStatus from, ReservationStatus to, Instant now) {
    return jdbc.sql(
            """
            update reservations set status = :to, updated_at = :now
             where id = :id and status = :from
            """)
        .param("id", id)
        .param("from", from.name())
        .param("to", to.name())
        .param("now", Timestamp.from(now))
        .update();
  }

  private ReservationRecord mapReservation(java.sql.ResultSet rs, int rowNum)
      throws java.sql.SQLException {
    return new ReservationRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("order_id", UUID.class),
        ReservationStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("expires_at").toInstant(),
        rs.getTimestamp("created_at").toInstant());
  }
}
