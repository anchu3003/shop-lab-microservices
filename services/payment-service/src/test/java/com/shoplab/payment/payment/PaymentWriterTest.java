package com.shoplab.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.idempotency.IdempotencyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Test phần ghi: bất biến "một đơn một intent sống" + ghi idempotency trong cùng transaction.
 *
 * <p>Nhánh 409 là nhánh dễ bỏ sót nhất: nếu chỉ kiểm "đã có intent thì trả lại", hai intent khác số
 * tiền cho cùng một đơn sẽ cùng tồn tại ⇒ hai đường thu tiền.
 */
class PaymentWriterTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final UUID USER = UUID.fromString("22222222-2222-4222-8222-000000000001");
  private static final UUID ORDER = UUID.fromString("33333333-3333-4333-8333-000000000001");
  private static final String SCOPE = USER + ":POST /v1/payment-intents";

  private final PaymentRepository payments = mock(PaymentRepository.class);
  private final IdempotencyService idempotency = mock(IdempotencyService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PaymentProperties properties =
      new PaymentProperties(
          Duration.ofMinutes(15),
          "mock",
          "http://localhost:8080/mock-provider/pay",
          "secret",
          Duration.ofMinutes(5),
          true,
          Duration.ofMillis(1),
          Duration.ofHours(24),
          Duration.ofHours(1),
          200);

  private final PaymentWriter writer =
      new PaymentWriter(
          payments, idempotency, properties, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

  private static PaymentRecord live(BigDecimal amount) {
    return new PaymentRecord(
        UUID.randomUUID(),
        ORDER,
        USER,
        amount,
        "VND",
        PaymentStatus.REQUIRES_PAYMENT,
        "mock",
        "pi_mock_existing",
        null,
        "key-old",
        NOW,
        NOW,
        null);
  }

  @Test
  @DisplayName("đơn chưa có intent → tạo mới + đánh dấu idempotency 201")
  void newIntentShouldBeInserted() {
    when(payments.findLiveByOrderId(ORDER)).thenReturn(Optional.empty());

    PaymentWriter.Written written =
        writer.create(
            USER,
            "key-1",
            SCOPE,
            new CreatePaymentIntentRequest(ORDER, new BigDecimal("180000"), "VND"));

    assertThat(written.existing()).isFalse();
    assertThat(written.response().status()).isEqualTo("REQUIRES_PAYMENT");
    assertThat(written.response().paymentUrl()).contains("/mock-provider/pay/");
    verify(payments).insert(any(PaymentRecord.class));
    verify(idempotency).complete(eq(SCOPE), eq("key-1"), eq(201), any(), any());
  }

  @Test
  @DisplayName("đơn đã có intent CÙNG số tiền → trả intent cũ (200), không tạo row thứ hai")
  void sameAmountShouldReturnExisting() {
    when(payments.findLiveByOrderId(ORDER))
        .thenReturn(Optional.of(live(new BigDecimal("180000.00"))));

    PaymentWriter.Written written =
        writer.create(
            USER,
            "key-2",
            SCOPE,
            new CreatePaymentIntentRequest(ORDER, new BigDecimal("180000"), "VND"));

    assertThat(written.existing()).isTrue();
    verify(payments, never()).insert(any(PaymentRecord.class));
    verify(idempotency).complete(eq(SCOPE), eq("key-2"), eq(200), any(), any());
  }

  @Test
  @DisplayName("đơn đã có intent KHÁC số tiền → 409 (hai đường thu tiền là lỗi)")
  void differentAmountShouldConflict() {
    when(payments.findLiveByOrderId(ORDER))
        .thenReturn(Optional.of(live(new BigDecimal("180000.00"))));

    assertThatThrownBy(
            () ->
                writer.create(
                    USER,
                    "key-3",
                    SCOPE,
                    new CreatePaymentIntentRequest(ORDER, new BigDecimal("99000"), "VND")))
        .isInstanceOf(PaymentExceptions.IntentExists.class);
    verify(payments, never()).insert(any(PaymentRecord.class));
  }
}
