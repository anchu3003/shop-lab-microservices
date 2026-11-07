package com.shoplab.payment.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link PaymentRepository}. */
@Repository
public class JdbcPaymentRepository implements PaymentRepository {

  private static final String SELECT =
      """
      select id, order_id, user_id, amount, currency, status, provider, provider_intent_id,
             provider_txn_id, idempotency_key, created_at, updated_at, paid_at
        from payments
      """;

  private final JdbcClient jdbc;

  public JdbcPaymentRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(PaymentRecord payment) {
    jdbc.sql(
            """
            insert into payments (id, order_id, user_id, amount, currency, status, provider,
                                  provider_intent_id, provider_txn_id, idempotency_key,
                                  created_at, updated_at, paid_at)
            values (:id, :orderId, :userId, :amount, :currency, :status, :provider,
                    :providerIntentId, :providerTxnId, :idempotencyKey, :createdAt, :updatedAt, :paidAt)
            """)
        .param("id", payment.id())
        .param("orderId", payment.orderId())
        .param("userId", payment.userId())
        .param("amount", payment.amount())
        .param("currency", payment.currency())
        .param("status", payment.status().name())
        .param("provider", payment.provider())
        .param("providerIntentId", payment.providerIntentId())
        .param("providerTxnId", payment.providerTxnId(), Types.VARCHAR)
        .param("idempotencyKey", payment.idempotencyKey())
        .param("createdAt", Timestamp.from(payment.createdAt()))
        .param("updatedAt", Timestamp.from(payment.updatedAt()))
        .param(
            "paidAt",
            payment.paidAt() == null ? null : Timestamp.from(payment.paidAt()),
            Types.TIMESTAMP)
        .update();
  }

  @Override
  public Optional<PaymentRecord> findById(UUID id) {
    return jdbc.sql(SELECT + " where id = :id").param("id", id).query(this::map).optional();
  }

  @Override
  public Optional<PaymentRecord> findByProviderIntentId(String providerIntentId) {
    return jdbc.sql(SELECT + " where provider_intent_id = :pid")
        .param("pid", providerIntentId)
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<PaymentRecord> findLiveByOrderId(UUID orderId) {
    return jdbc.sql(
            SELECT
                + """
                 where order_id = :orderId
                   and status in ('REQUIRES_PAYMENT', 'PROCESSING', 'SUCCEEDED')
                """)
        .param("orderId", orderId)
        .query(this::map)
        .optional();
  }

  @Override
  public List<PaymentRecord> findByOrderId(UUID orderId) {
    return jdbc.sql(SELECT + " where order_id = :orderId order by created_at desc")
        .param("orderId", orderId)
        .query(this::map)
        .list();
  }

  @Override
  public int markStatus(
      UUID id, PaymentStatus from, PaymentStatus to, String providerTxnId, Instant now) {
    // paid_at chỉ được set khi SUCCEEDED — DB có CHECK ((status='SUCCEEDED') = (paid_at IS NOT
    // NULL))
    // nên truyền sai là bị chặn ngay, không lọt dữ liệu không đối soát được.
    Instant paidAt = to == PaymentStatus.SUCCEEDED ? now : null;
    return jdbc.sql(
            """
            update payments
               set status = :to,
                   provider_txn_id = coalesce(:txn, provider_txn_id),
                   paid_at = :paidAt,
                   updated_at = :now
             where id = :id and status = :from
            """)
        .param("id", id)
        .param("from", from.name())
        .param("to", to.name())
        .param("txn", providerTxnId, Types.VARCHAR)
        .param("paidAt", paidAt == null ? null : Timestamp.from(paidAt), Types.TIMESTAMP)
        .param("now", Timestamp.from(now))
        .update();
  }

  /**
   * {@code ON CONFLICT DO NOTHING} là chỗ then chốt: provider gửi 20 request song song cùng {@code
   * event_id} thì DB đảm bảo chỉ một cái insert được ⇒ chỉ một lần xử lý.
   */
  @Override
  public boolean insertWebhookEvent(
      String providerEventId,
      UUID paymentId,
      String providerTxnId,
      String status,
      BigDecimal amount,
      String payloadSha256,
      Instant now) {
    int inserted =
        jdbc.sql(
                """
                insert into payment_webhook_events (provider_event_id, payment_id, provider_txn_id,
                                                   status, amount, payload_sha256, received_at)
                values (:eventId, :paymentId, :txn, :status, :amount, :hash, :now)
                on conflict (provider_event_id) do nothing
                """)
            .param("eventId", providerEventId)
            .param("paymentId", paymentId)
            .param("txn", providerTxnId, Types.VARCHAR)
            .param("status", status)
            .param("amount", amount)
            .param("hash", payloadSha256)
            .param("now", Timestamp.from(now))
            .update();
    return inserted == 1;
  }

  private PaymentRecord map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    Timestamp paidAt = rs.getTimestamp("paid_at");
    return new PaymentRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("order_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getBigDecimal("amount"),
        rs.getString("currency").trim(),
        PaymentStatus.valueOf(rs.getString("status")),
        rs.getString("provider"),
        rs.getString("provider_intent_id"),
        rs.getString("provider_txn_id"),
        rs.getString("idempotency_key"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        paidAt == null ? null : paidAt.toInstant());
  }
}
