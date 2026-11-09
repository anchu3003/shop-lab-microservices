package com.shoplab.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.serviceauth.ServiceTokenClient;
import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderRepository;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.outbox.CheckoutOutbox;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import com.shoplab.order.saga.downstream.PaymentStatusResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho bước **finish** của saga (`SL-2.6b`): confirm · cancel · expire.
 *
 * <p>Ba câu hỏi được kiểm, đúng ba chỗ dễ sai: (1) có **verify payment** trước khi chốt không, (2)
 * có **bỏ qua** verify với đơn 0 đồng không, (3) cancel/expire có chạm đúng tài nguyên và chặn đúng
 * trạng thái không.
 */
class CheckoutFinishServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final UUID RESERVATION_ID = UUID.randomUUID();
  private static final UUID LOCK_ID = UUID.randomUUID();
  private static final UUID INTENT_ID = UUID.randomUUID();

  private final DownstreamGateway downstream = mock(DownstreamGateway.class);
  private final CheckoutSagaRepository sagas = mock(CheckoutSagaRepository.class);
  private final OrderRepository orders = mock(OrderRepository.class);
  private final OrderWriter writer = mock(OrderWriter.class);
  private final CheckoutOutbox finalizer = mock(CheckoutOutbox.class);
  private final ServiceTokenClient serviceTokens = mock(ServiceTokenClient.class);

  private final CheckoutFinishService service =
      new CheckoutFinishService(
          downstream,
          sagas,
          orders,
          writer,
          finalizer,
          serviceTokens,
          Clock.fixed(NOW, ZoneOffset.UTC));

  // __FINISH_TESTS2__

  @Test
  @DisplayName("payment SUCCEEDED → commit stock + voucher, order CONFIRMED, saga CONFIRMED")
  void confirmCommitsWhenPaymentSucceeded() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));
    orderPending();
    when(downstream.getPaymentIntent(any(), eq(INTENT_ID)))
        .thenReturn(new DownstreamResult<>(new PaymentStatusResponse("SUCCEEDED"), 200));
    when(downstream.commitStock(any(), eq(RESERVATION_ID)))
        .thenReturn(new DownstreamResult<>(null, 200));
    when(downstream.commitVoucher(any(), eq(LOCK_ID)))
        .thenReturn(new DownstreamResult<>(null, 200));

    SagaStatusResponse response = service.confirm(ORDER_ID, USER_ID, false, "token");

    assertThat(response.orderId()).isEqualTo(ORDER_ID);
    verify(downstream).commitStock("token", RESERVATION_ID);
    verify(downstream).commitVoucher("token", LOCK_ID);
    verify(finalizer).confirm(eq(ORDER_ID), anyString(), any());
  }

  @Test
  @DisplayName("confirm lần 2 (saga đã CONFIRMED) → trả luôn, KHÔNG gọi service nào")
  void confirmIsIdempotent() {
    sagaFound(state(CheckoutSagaState.CONFIRMED, true));

    SagaStatusResponse response = service.confirm(ORDER_ID, USER_ID, false, "token");

    assertThat(response.state()).isEqualTo(CheckoutSagaState.CONFIRMED);
    verify(downstream, never()).getPaymentIntent(any(), any());
    verify(downstream, never()).commitStock(any(), any());
    verify(finalizer, never()).confirm(any(), anyString(), any());
  }

  @Test
  @DisplayName("payment CHƯA SUCCEEDED → 409, KHÔNG commit gì (không tin lời client)")
  void confirmRejectsWhenPaymentPending() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));
    orderPending();
    when(downstream.getPaymentIntent(any(), eq(INTENT_ID)))
        .thenReturn(new DownstreamResult<>(new PaymentStatusResponse("REQUIRES_PAYMENT"), 200));

    assertThatThrownBy(() -> service.confirm(ORDER_ID, USER_ID, false, "token"))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("chưa SUCCEEDED")
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.CONFLICT);

    verify(downstream, never()).commitStock(any(), any());
    verify(finalizer, never()).confirm(any(), anyString(), any());
  }

  @Test
  @DisplayName("đơn 0 đồng → KHÔNG gọi payment-service, vẫn commit + CONFIRMED")
  void confirmSkipsPaymentForZeroTotal() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, false));
    orderPending();
    when(downstream.commitStock(any(), any())).thenReturn(new DownstreamResult<>(null, 200));
    when(downstream.commitVoucher(any(), any())).thenReturn(new DownstreamResult<>(null, 200));

    service.confirm(ORDER_ID, USER_ID, false, "token");

    verify(downstream, never()).getPaymentIntent(any(), any());
    verify(finalizer).confirm(eq(ORDER_ID), anyString(), any());
  }

  // __FINISH_TESTS3__

  @Test
  @DisplayName("cancel → release stock + voucher rồi CANCELLED")
  void cancelReleasesAndCancels() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));

    service.cancel(ORDER_ID, USER_ID, false, "token", "đổi ý");

    verify(downstream).releaseStock("token", RESERVATION_ID);
    verify(downstream).releaseVoucher("token", LOCK_ID);
    verify(finalizer).cancel(ORDER_ID, "đổi ý", NOW);
  }

  @Test
  @DisplayName("cancel đơn đã CONFIRMED → 409 (huỷ cần hoàn tiền — Phase 6)")
  void cancelAfterConfirmIsConflict() {
    sagaFound(state(CheckoutSagaState.CONFIRMED, true));

    assertThatThrownBy(() -> service.cancel(ORDER_ID, USER_ID, false, "token", null))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("CONFIRMED")
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.CONFLICT);

    verify(downstream, never()).releaseStock(any(), any());
  }

  @Test
  @DisplayName("expire (sweeper) → RELEASE bằng service token rồi order/saga EXPIRED (TD-25)")
  void expireReleasesWithServiceToken() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));
    orderPending();
    when(serviceTokens.tokenFor("inventory-service")).thenReturn("tok-inventory");
    when(serviceTokens.tokenFor("promotion-service")).thenReturn("tok-promotion");
    when(writer.expirePending(eq(ORDER_ID), anyString(), any())).thenReturn(true);

    boolean expired = service.expire(ORDER_ID);

    assertThat(expired).isTrue();
    verify(sagas)
        .markState(
            eq(ORDER_ID), eq(CheckoutSagaState.EXPIRED), eq("PAYMENT_WINDOW_EXCEEDED"), any());
    // TD-25: sweeper CÓ service token ⇒ trả hàng/voucher ngay, không chờ TTL của service khác.
    verify(downstream).releaseStock(eq("tok-inventory"), eq(RESERVATION_ID));
    verify(downstream).releaseVoucher(eq("tok-promotion"), eq(LOCK_ID));
  }

  @Test
  @DisplayName("expire: release lỗi ⇒ vẫn expire (đơn phải rời trạng thái chờ) — TD-25")
  void expireStillWorksWhenReleaseFails() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));
    orderPending();
    when(serviceTokens.tokenFor(any())).thenThrow(new IllegalStateException("identity chết"));
    when(writer.expirePending(eq(ORDER_ID), anyString(), any())).thenReturn(true);

    assertThat(service.expire(ORDER_ID)).isTrue();
    verify(sagas)
        .markState(
            eq(ORDER_ID), eq(CheckoutSagaState.EXPIRED), eq("PAYMENT_WINDOW_EXCEEDED"), any());
  }

  @Test
  @DisplayName("expire: đơn không còn PENDING_PAYMENT ⇒ không release (tránh trả hàng đơn đã bán)")
  void expireSkipsWhenOrderNoLongerPending() {
    sagaFound(state(CheckoutSagaState.COMPLETED_START, true));
    when(orders.findById(ORDER_ID)).thenReturn(Optional.of(confirmedOrder()));

    assertThat(service.expire(ORDER_ID)).isFalse();
    verify(downstream, never()).releaseStock(any(), any());
    verify(writer, never()).expirePending(any(), anyString(), any());
  }

  @Test
  @DisplayName("expire saga đã kết thúc → không làm gì (idempotent)")
  void expireSkipsTerminalSaga() {
    sagaFound(state(CheckoutSagaState.CONFIRMED, true));

    assertThat(service.expire(ORDER_ID)).isFalse();
    verify(writer, never()).expirePending(any(), anyString(), any());
  }

  private void sagaFound(CheckoutSagaRecord saga) {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga));
  }

  private void orderPending() {
    when(orders.findById(ORDER_ID))
        .thenReturn(
            Optional.of(
                new OrderRecord(
                    ORDER_ID,
                    USER_ID,
                    OrderStatus.PENDING_PAYMENT,
                    new BigDecimal("100.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("100.00"),
                    "VND",
                    "key",
                    INTENT_ID,
                    NOW,
                    NOW)));
  }

  /** Đơn đã CONFIRMED — dùng để kiểm sweeper không release hàng của đơn đã bán. */
  private static OrderRecord confirmedOrder() {
    return new OrderRecord(
        ORDER_ID,
        USER_ID,
        OrderStatus.CONFIRMED,
        new BigDecimal("100.00"),
        BigDecimal.ZERO,
        new BigDecimal("100.00"),
        "VND",
        "key",
        INTENT_ID,
        NOW,
        NOW);
  }

  private static CheckoutSagaRecord state(CheckoutSagaState state, boolean paymentRequired) {
    return new CheckoutSagaRecord(
        ORDER_ID,
        USER_ID,
        state,
        paymentRequired,
        LOCK_ID,
        RESERVATION_ID,
        paymentRequired ? INTENT_ID : null,
        paymentRequired ? "http://pay" : null,
        null,
        NOW,
        NOW);
  }
}
