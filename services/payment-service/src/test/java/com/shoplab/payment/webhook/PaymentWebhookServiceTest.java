package com.shoplab.payment.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.payment.outbox.OutboxRepository;
import com.shoplab.payment.outbox.PaymentEvents;
import com.shoplab.payment.payment.PaymentExceptions;
import com.shoplab.payment.payment.PaymentRecord;
import com.shoplab.payment.payment.PaymentRepository;
import com.shoplab.payment.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Test xử lý webhook: dedupe theo event id, so số tiền, compare-and-set trạng thái. */
class PaymentWebhookServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String BODY = "{\"eventId\":\"evt_1\"}";
  private static final String INTENT_ID = "pi_mock_1";

  private final PaymentRepository payments = mock(PaymentRepository.class);
  private final OutboxRepository outbox = mock(OutboxRepository.class);
  private final PaymentWebhookService service =
      new PaymentWebhookService(payments, outbox, Clock.fixed(NOW, ZoneOffset.UTC));

  private static PaymentRecord record(PaymentStatus status) {
    return new PaymentRecord(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        new BigDecimal("180000.00"),
        "VND",
        status,
        "mock",
        INTENT_ID,
        null,
        "key-1",
        NOW,
        NOW,
        null);
  }

  private static PaymentWebhookRequest event(String status, String amount) {
    return new PaymentWebhookRequest(
        "evt_1", INTENT_ID, "txn_mock_1", status, new BigDecimal(amount), "VND");
  }

  @Test
  @DisplayName("provider gửi lại cùng event id → duplicate=true, KHÔNG đổi dữ liệu")
  void duplicateEventShouldBeIgnored() {
    PaymentRecord payment = record(PaymentStatus.SUCCEEDED);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(false);

    PaymentWebhookResponse response = service.apply(BODY, event("succeeded", "180000.00"));

    assertThat(response.duplicate()).isTrue();
    assertThat(response.applied()).isFalse();
    verify(payments, never()).markStatus(any(), any(), any(), any(), any());
    // SL-3.5: webhook lặp KHÔNG được sinh event thứ hai (AC).
    verify(outbox, never()).append(any(), any(), any(), any());
  }

  @Test
  @DisplayName("webhook báo số tiền khác intent → 409")
  void amountMismatchShouldConflict() {
    when(payments.findByProviderIntentId(INTENT_ID))
        .thenReturn(Optional.of(record(PaymentStatus.REQUIRES_PAYMENT)));

    assertThatThrownBy(() -> service.apply(BODY, event("succeeded", "1.00")))
        .isInstanceOf(PaymentExceptions.AmountMismatch.class);
  }

  @Test
  @DisplayName("intent không tồn tại → 404 (provider retry sau khi DB đã có intent)")
  void unknownIntentShouldBe404() {
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.apply(BODY, event("succeeded", "180000.00")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("event mới + REQUIRES_PAYMENT → chuyển SUCCEEDED (applied=true)")
  void newEventShouldApply() {
    PaymentRecord payment = record(PaymentStatus.REQUIRES_PAYMENT);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(payments.markStatus(any(), any(), any(), any(), any())).thenReturn(1);

    PaymentWebhookResponse response = service.apply(BODY, event("succeeded", "180000.00"));

    assertThat(response.applied()).isTrue();
    assertThat(response.status()).isEqualTo("SUCCEEDED");
    verify(payments)
        .markStatus(
            payment.id(),
            PaymentStatus.REQUIRES_PAYMENT,
            PaymentStatus.SUCCEEDED,
            "txn_mock_1",
            NOW);
  }

  @Test
  @DisplayName("SL-3.5: transition thật sang SUCCEEDED ⇒ ghi ĐÚNG 1 outbox `payment.succeeded.v1`")
  void successShouldAppendOutboxEvent() {
    PaymentRecord payment = record(PaymentStatus.REQUIRES_PAYMENT);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(payments.markStatus(any(), any(), any(), any(), any())).thenReturn(1);

    service.apply(BODY, event("succeeded", "180000.00"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<java.util.Map<String, Object>> payload =
        ArgumentCaptor.forClass(java.util.Map.class);
    verify(outbox)
        .append(
            eq(PaymentEvents.AGGREGATE),
            eq(payment.id()),
            eq(PaymentEvents.SUCCEEDED),
            payload.capture());
    // Payload đúng 5 field theo contract (ADR-0007) — không thêm order/user thừa.
    assertThat(payload.getValue())
        .containsEntry("paymentIntentId", payment.id().toString())
        .containsEntry("orderId", payment.orderId().toString())
        .containsEntry("amount", payment.amount())
        .containsEntry("currency", payment.currency())
        .containsEntry("succeededAt", NOW.toString());
  }

  @Test
  @DisplayName("SL-3.5: webhook FAILED ⇒ KHÔNG ghi outbox (chỉ sự kiện succeeded mới có event)")
  void failedShouldNotAppendOutboxEvent() {
    PaymentRecord payment = record(PaymentStatus.REQUIRES_PAYMENT);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(payments.markStatus(any(), any(), any(), any(), any())).thenReturn(1);

    service.apply(BODY, event("failed", "180000.00"));

    verify(outbox, never()).append(any(), any(), any(), any());
  }

  @Test
  @DisplayName("event id khác nhưng kết quả đã đúng → no-op (applied=false)")
  void sameOutcomeShouldBeNoOp() {
    PaymentRecord payment = record(PaymentStatus.SUCCEEDED);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    PaymentWebhookResponse response = service.apply(BODY, event("succeeded", "180000.00"));

    assertThat(response.applied()).isFalse();
    verify(payments, never()).markStatus(any(), any(), any(), any(), any());
    verify(outbox, never()).append(any(), any(), any(), any());
  }

  @Test
  @DisplayName("payment đã terminal mà webhook báo kết quả khác → 409")
  void conflictingFinalStatusShouldConflict() {
    PaymentRecord payment = record(PaymentStatus.SUCCEEDED);
    when(payments.findByProviderIntentId(INTENT_ID)).thenReturn(Optional.of(payment));
    when(payments.insertWebhookEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    assertThatThrownBy(() -> service.apply(BODY, event("failed", "180000.00")))
        .isInstanceOf(AppException.class);
  }

  @Test
  @DisplayName("trạng thái lạ trong payload → 400 (retry không giúp gì)")
  void unsupportedStatusShouldBe400() {
    assertThatThrownBy(() -> service.apply(BODY, event("pending", "180000.00")))
        .isInstanceOf(PaymentExceptions.UnsupportedWebhookStatus.class);
    verify(payments, never()).findByProviderIntentId(any());
  }
}
