package com.shoplab.order.order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link OrderRepository}. */
@Repository
public class JdbcOrderRepository implements OrderRepository {

  private static final String SELECT_ORDER =
      """
      select id, user_id, status, subtotal, discount, total, currency, idempotency_key,
             payment_intent_id, created_at, updated_at
        from orders
      """;

  private final JdbcClient jdbc;

  public JdbcOrderRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<OrderRecord> findById(UUID orderId) {
    return jdbc.sql(SELECT_ORDER + " where id = :id")
        .param("id", orderId)
        .query(this::mapOrder)
        .optional();
  }

  @Override
  public List<OrderItemRecord> findItems(UUID orderId) {
    return jdbc.sql(
            """
            select id, sku_id, sku_code, title, unit_price, currency, qty, line_total
              from order_items where order_id = :id order by id
            """)
        .param("id", orderId)
        .query(
            (rs, rowNum) ->
                new OrderItemRecord(
                    rs.getObject("id", UUID.class),
                    rs.getObject("sku_id", UUID.class),
                    rs.getString("sku_code"),
                    rs.getString("title"),
                    rs.getBigDecimal("unit_price"),
                    rs.getString("currency"),
                    rs.getInt("qty"),
                    rs.getBigDecimal("line_total")))
        .list();
  }

  @Override
  public void insert(OrderRecord order, List<OrderItemRecord> items) {
    jdbc.sql(
            """
            insert into orders
              (id, user_id, status, subtotal, discount, total, currency, idempotency_key,
               payment_intent_id, created_at, updated_at)
            values
              (:id, :userId, :status, :subtotal, :discount, :total, :currency, :idempotencyKey,
               :paymentIntentId, :createdAt, :updatedAt)
            """)
        .param("id", order.id())
        .param("userId", order.userId())
        .param("status", order.status().name())
        .param("subtotal", order.subtotal())
        .param("discount", order.discount())
        .param("total", order.total())
        .param("currency", order.currency())
        .param("idempotencyKey", order.idempotencyKey())
        .param("paymentIntentId", order.paymentIntentId(), java.sql.Types.OTHER)
        .param("createdAt", Timestamp.from(order.createdAt()))
        .param("updatedAt", Timestamp.from(order.updatedAt()))
        .update();

    for (OrderItemRecord item : items) {
      jdbc.sql(
              """
              insert into order_items
                (id, order_id, sku_id, sku_code, title, unit_price, currency, qty, line_total)
              values
                (:id, :orderId, :skuId, :skuCode, :title, :unitPrice, :currency, :qty, :lineTotal)
              """)
          .param("id", item.id())
          .param("orderId", order.id())
          .param("skuId", item.skuId())
          .param("skuCode", item.skuCode())
          .param("title", item.title())
          .param("unitPrice", item.unitPrice())
          .param("currency", item.currency())
          .param("qty", item.qty())
          .param("lineTotal", item.lineTotal())
          .update();
    }
  }

  @Override
  public int markStatus(UUID orderId, OrderStatus from, OrderStatus to, Instant now) {
    return jdbc.sql(
            "update orders set status = :to, updated_at = :now where id = :id and status = :from")
        .param("id", orderId)
        .param("from", from.name())
        .param("to", to.name())
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public void insertHistory(
      UUID orderId, OrderStatus from, OrderStatus to, String reason, Instant now) {
    jdbc.sql(
            """
            insert into order_status_history (id, order_id, from_status, to_status, reason, created_at)
            values (:id, :orderId, :from, :to, :reason, :now)
            """)
        .param("id", UUID.randomUUID())
        .param("orderId", orderId)
        .param("from", from == null ? null : from.name(), java.sql.Types.VARCHAR)
        .param("to", to.name())
        .param("reason", reason, java.sql.Types.VARCHAR)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public List<OrderRecord> findByUser(UUID userId, int offset, int limit) {
    String sql =
        SELECT_ORDER
            + (userId == null ? "" : " where user_id = :userId")
            + " order by created_at desc, id limit :limit offset :offset";
    JdbcClient.StatementSpec spec = jdbc.sql(sql).param("limit", limit).param("offset", offset);
    if (userId != null) {
      spec = spec.param("userId", userId);
    }
    return spec.query(this::mapOrder).list();
  }

  @Override
  public long countByUser(UUID userId) {
    String sql = "select count(*) from orders" + (userId == null ? "" : " where user_id = :userId");
    JdbcClient.StatementSpec spec = jdbc.sql(sql);
    if (userId != null) {
      spec = spec.param("userId", userId);
    }
    Long count = spec.query(Long.class).single();
    return count == null ? 0 : count;
  }

  private OrderRecord mapOrder(ResultSet rs, int rowNum) throws SQLException {
    return new OrderRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("user_id", UUID.class),
        OrderStatus.valueOf(rs.getString("status")),
        rs.getBigDecimal("subtotal"),
        rs.getBigDecimal("discount"),
        rs.getBigDecimal("total"),
        rs.getString("currency"),
        rs.getString("idempotency_key"),
        rs.getObject("payment_intent_id", UUID.class),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant());
  }
}
