package com.shoplab.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.serviceauth.ServiceTokenClient;
import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderRepository;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.outbox.CheckoutOutbox;
import com.shoplab.order.saga.downstream.DownstreamException;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test đường **tự confirm theo event** `payment.succeeded.v1` (SL-3.5).
 *
 * <p>Đây là chỗ dễ chốt nhầm đơn nhất trong hệ (chuyện tiền), nên test kiểm đúng **ma trận quyết
 * định**: mỗi nhánh "không chốt được" phải ack (không ném lỗi) và **không** chạm downstream.
 */
class CheckoutFinishServiceEventTest {

  private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID INTENT_ID = UUID.randomUUID();
  private static final UUID RESERVATION_ID = UUID.randomUUID();
  private static final UUID LOCK_ID = UUID.randomUUID();
  private static final String EVENT_ID = UUID.randomUUID().toString();
  private static final String INVENTORY_AUDIENCE = "inventory-service";
  private static final String PROMOTION_AUDIENCE = "promotion-service";

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

  @Test
  @DisplayName("đủ điều kiện ⇒ commit stock + voucher bằng SERVICE TOKEN rồi chốt đơn + ghi step")
  void shouldConfirmWithServiceToken() {
    sagaInFlight();
    orderWithStatus(OrderStatus.PENDING_PAYMENT);
    stubTokens();
    when(downstream.commitStock(eq("tok-inventory"), eq(RESERVATION_ID)))
        .thenReturn(new DownstreamResult<>(null, 200));
    when(downstream.commitVoucher(eq("tok-promotion"), eq(LOCK_ID)))
        .thenReturn(new DownstreamResult<>(null, 200));

    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID));

    verify(finalizer).confirm(eq(ORDER_ID), any(), eq(NOW));
    // Bằng chứng cho TD-17: đường event KHÔNG gọi lại API payment (không có token user để gọi).
    verify(downstream, never()).getPaymentIntent(any(), any());

    ArgumentCaptor<SagaStepRecord> steps = ArgumentCaptor.forClass(SagaStepRecord.class);
    verify(sagas, org.mockito.Mockito.atLeastOnce()).addStep(steps.capture());
    List<CheckoutStep> recorded = steps.getAllValues().stream().map(SagaStepRecord::step).toList();
    assertThat(recorded)
        .contains(
            CheckoutStep.STOCK_COMMIT,
            CheckoutStep.VOUCHER_COMMIT,
            CheckoutStep.PAYMENT_EVENT_CONFIRM);
    assertThat(steps.getAllValues())
        .filteredOn(step -> step.step() == CheckoutStep.PAYMENT_VERIFY)
        .singleElement()
        .satisfies(
            step -> assertThat(step.status()).isEqualTo(SagaStepRecord.SagaStepStatus.SKIPPED));
  }

  @Test
  @DisplayName("saga đã CONFIRMED ⇒ no-op (hai đường confirm gặp nhau là bình thường)")
  void shouldIgnoreWhenAlreadyConfirmed() {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga(CheckoutSagaState.CONFIRMED)));

    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID));

    verify(finalizer, never()).confirm(any(), any(), any());
    verify(downstream, never()).commitStock(any(), any());
  }

  @Test
  @DisplayName("đơn đã EXPIRED ⇒ ack + KHÔNG chốt + KHÔNG chạm downstream (cần hoàn tiền tay)")
  void shouldNotConfirmExpiredOrder() {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga(CheckoutSagaState.COMPLETED_START)));
    orderWithStatus(OrderStatus.EXPIRED);

    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID));

    verify(finalizer, never()).confirm(any(), any(), any());
    verify(downstream, never()).commitStock(any(), any());
    verify(downstream, never()).commitVoucher(any(), any());
  }

  @Test
  @DisplayName(
      "paymentIntentId lệch saga ⇒ KHÔNG chốt (chốt nhầm đơn đã trả tiền là lỗi không sửa được)")
  void shouldRejectIntentMismatch() {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga(CheckoutSagaState.COMPLETED_START)));

    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, UUID.randomUUID()));

    verify(finalizer, never()).confirm(any(), any(), any());
    verify(downstream, never()).commitStock(any(), any());
  }

  @Test
  @DisplayName("không có saga / saga chưa COMPLETED_START / payload thiếu field ⇒ ack im lặng")
  void shouldAckWhenNothingToDo() {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.empty());
    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID));

    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga(CheckoutSagaState.STARTED)));
    service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID));

    service.confirmByEvent(EVENT_ID, Map.of("eventId", EVENT_ID));

    verify(finalizer, never()).confirm(any(), any(), any());
    verify(downstream, never()).commitStock(any(), any());
  }

  @Test
  @DisplayName("downstream lỗi khi commit ⇒ NÉM ra để consumer retry (lỗi tạm thời)")
  void shouldPropagateDownstreamFailure() {
    sagaInFlight();
    orderWithStatus(OrderStatus.PENDING_PAYMENT);
    stubTokens();
    when(downstream.commitStock(any(), any()))
        .thenThrow(new DownstreamException("inventory", 503, "inventory sập"));

    assertThatThrownBy(() -> service.confirmByEvent(EVENT_ID, payload(ORDER_ID, INTENT_ID)))
        .isInstanceOf(DownstreamException.class);
    verify(finalizer, never()).confirm(any(), any(), any());
  }

  // ------------------------------------------------------------------ helpers

  private void stubTokens() {
    when(serviceTokens.tokenFor(INVENTORY_AUDIENCE)).thenReturn("tok-inventory");
    when(serviceTokens.tokenFor(PROMOTION_AUDIENCE)).thenReturn("tok-promotion");
  }

  private void sagaInFlight() {
    when(sagas.findById(ORDER_ID)).thenReturn(Optional.of(saga(CheckoutSagaState.COMPLETED_START)));
  }

  private void orderWithStatus(OrderStatus status) {
    when(orders.findById(ORDER_ID))
        .thenReturn(
            Optional.of(
                new OrderRecord(
                    ORDER_ID,
                    USER_ID,
                    status,
                    new BigDecimal("180000.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("180000.00"),
                    "VND",
                    "key-1",
                    INTENT_ID,
                    NOW,
                    NOW)));
  }

  private static CheckoutSagaRecord saga(CheckoutSagaState state) {
    return new CheckoutSagaRecord(
        ORDER_ID,
        USER_ID,
        state,
        true,
        LOCK_ID,
        RESERVATION_ID,
        INTENT_ID,
        "http://pay",
        null,
        NOW,
        NOW);
  }

  private static Map<String, Object> payload(UUID orderId, UUID intentId) {
    return Map.of(
        "orderId",
        orderId.toString(),
        "paymentIntentId",
        intentId.toString(),
        "amount",
        180000,
        "currency",
        "VND");
  }
}
