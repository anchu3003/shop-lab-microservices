package com.shoplab.order.saga;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link CheckoutSagaRepository}. */
@Repository
public class JdbcCheckoutSagaRepository implements CheckoutSagaRepository {

  private static final String SELECT_SAGA =
      """
      select order_id, user_id, state, payment_required, lock_id, reservation_id,
             payment_intent_id, payment_url, failure_code, created_at, updated_at
        from checkout_sagas
      """;

  private final JdbcClient jdbc;

  public JdbcCheckoutSagaRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void start(CheckoutSagaRecord saga) {
    jdbc.sql(
            """
            insert into checkout_sagas
              (order_id, user_id, state, payment_required, created_at, updated_at)
            values
              (:orderId, :userId, :state, :paymentRequired, :createdAt, :updatedAt)
            """)
        .param("orderId", saga.orderId())
        .param("userId", saga.userId())
        .param("state", saga.state().name())
        .param("paymentRequired", saga.paymentRequired())
        .param("createdAt", Timestamp.from(saga.createdAt()))
        .param("updatedAt", Timestamp.from(saga.updatedAt()))
        .update();
  }

  @Override
  public void markLock(UUID orderId, UUID lockId, Instant now) {
    jdbc.sql("update checkout_sagas set lock_id = :lockId, updated_at = :now where order_id = :id")
        .param("lockId", lockId)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public void markReservation(UUID orderId, UUID reservationId, Instant now) {
    jdbc.sql(
            """
            update checkout_sagas set reservation_id = :reservationId, updated_at = :now
             where order_id = :id
            """)
        .param("reservationId", reservationId)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public void markCompletedStart(
      UUID orderId, UUID paymentIntentId, String paymentUrl, Instant now) {
    jdbc.sql(
            """
            update checkout_sagas
               set state = :state, payment_intent_id = :paymentIntentId, payment_url = :paymentUrl,
                   updated_at = :now
             where order_id = :id
            """)
        .param("state", CheckoutSagaState.COMPLETED_START.name())
        .param("paymentIntentId", paymentIntentId, Types.OTHER)
        .param("paymentUrl", paymentUrl, Types.VARCHAR)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public void markPaymentRequired(UUID orderId, boolean paymentRequired, Instant now) {
    jdbc.sql(
            """
            update checkout_sagas set payment_required = :paymentRequired, updated_at = :now
             where order_id = :id
            """)
        .param("paymentRequired", paymentRequired)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public void markFailed(UUID orderId, CheckoutSagaState state, String failureCode, Instant now) {
    jdbc.sql(
            """
            update checkout_sagas
               set state = :state, failure_code = :failureCode, updated_at = :now
             where order_id = :id
            """)
        .param("state", state.name())
        .param("failureCode", failureCode, Types.VARCHAR)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public void markState(UUID orderId, CheckoutSagaState state, String failureCode, Instant now) {
    jdbc.sql(
            """
            update checkout_sagas
               set state = :state, failure_code = :failureCode, updated_at = :now
             where order_id = :id
            """)
        .param("state", state.name())
        .param("failureCode", failureCode, Types.VARCHAR)
        .param("now", Timestamp.from(now))
        .param("id", orderId)
        .update();
  }

  @Override
  public List<CheckoutSagaRecord> findExpirable(Instant createdBefore, int limit) {
    return jdbc.sql(
            """
            select s.order_id, s.user_id, s.state, s.payment_required, s.lock_id, s.reservation_id,
                   s.payment_intent_id, s.payment_url, s.failure_code, s.created_at, s.updated_at
              from checkout_sagas s
              join orders o on o.id = s.order_id
             where s.state = 'COMPLETED_START'
               and o.status = 'PENDING_PAYMENT'
               and s.created_at < :before
             order by s.created_at
             limit :limit
            """)
        .param("before", Timestamp.from(createdBefore))
        .param("limit", limit)
        .query(this::mapSaga)
        .list();
  }

  @Override
  public Optional<CheckoutSagaRecord> findById(UUID orderId) {
    return jdbc.sql(SELECT_SAGA + " where order_id = :id")
        .param("id", orderId)
        .query(this::mapSaga)
        .optional();
  }

  @Override
  public void addStep(SagaStepRecord step) {
    jdbc.sql(
            """
            insert into checkout_saga_steps
              (id, order_id, step, status, http_status, duration_ms, detail, created_at)
            values
              (:id, :orderId, :step, :status, :httpStatus, :durationMs, :detail, :createdAt)
            """)
        .param("id", UUID.randomUUID())
        .param("orderId", step.orderId())
        .param("step", step.step().name())
        .param("status", step.status().name())
        .param("httpStatus", step.httpStatus(), Types.INTEGER)
        .param("durationMs", step.durationMs())
        .param("detail", step.detail(), Types.VARCHAR)
        .param("createdAt", Timestamp.from(step.createdAt()))
        .update();
  }

  @Override
  public List<SagaStepRecord> findSteps(UUID orderId) {
    return jdbc.sql(
            """
            select order_id, step, status, http_status, duration_ms, detail, created_at
              from checkout_saga_steps where order_id = :id order by created_at, id
            """)
        .param("id", orderId)
        .query(this::mapStep)
        .list();
  }

  private CheckoutSagaRecord mapSaga(ResultSet rs, int rowNum) throws SQLException {
    return new CheckoutSagaRecord(
        rs.getObject("order_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        CheckoutSagaState.valueOf(rs.getString("state")),
        rs.getBoolean("payment_required"),
        rs.getObject("lock_id", UUID.class),
        rs.getObject("reservation_id", UUID.class),
        rs.getObject("payment_intent_id", UUID.class),
        rs.getString("payment_url"),
        rs.getString("failure_code"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant());
  }

  private SagaStepRecord mapStep(ResultSet rs, int rowNum) throws SQLException {
    int httpStatus = rs.getInt("http_status");
    // `wasNull()` phải đọc NGAY sau lần getInt tương ứng — nếu đọc sau một get khác thì nó trả lời
    // cho cột vừa đọc, không phải cột http_status (bug im lặng: null thành 0).
    boolean httpStatusNull = rs.wasNull();
    return new SagaStepRecord(
        rs.getObject("order_id", UUID.class),
        CheckoutStep.valueOf(rs.getString("step")),
        SagaStepRecord.SagaStepStatus.valueOf(rs.getString("status")),
        httpStatusNull ? null : httpStatus,
        rs.getInt("duration_ms"),
        rs.getString("detail"),
        rs.getTimestamp("created_at").toInstant());
  }
}
