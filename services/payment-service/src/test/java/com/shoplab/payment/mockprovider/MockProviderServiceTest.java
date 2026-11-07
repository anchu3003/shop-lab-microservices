package com.shoplab.payment.mockprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.payment.PaymentRecord;
import com.shoplab.payment.payment.PaymentRepository;
import com.shoplab.payment.payment.PaymentStatus;
import com.shoplab.payment.webhook.PaymentWebhookRequest;
import com.shoplab.payment.webhook.PaymentWebhookResponse;
import com.shoplab.payment.webhook.PaymentWebhookService;
import com.shoplab.payment.webhook.WebhookSignatureVerifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

/** Test mock provider: chỉ đóng vai provider, còn đường ghi trạng thái vẫn là webhook thật. */
class MockProviderServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String INTENT_ID = "pi_mock_1";

  private final PaymentRepository payments = mock(PaymentRepository.class);
  private final PaymentWebhookService webhookService = mock(PaymentWebhookService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final PaymentProperties properties =
      new PaymentProperties(
          Duration.ofMinutes(15),
          "mock",
          "http://localhost:8080/mock-provider/pay",
          "secret",
          Duration.ofMinutes(5),
          true,
          Duration.ZERO,
          Duration.ofHours(24),
          Duration.ofHours(1),
          200);
  private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(properties, clock);

  private final MockProviderService service =
      new MockProviderService(payments, webhookService, verifier, properties, objectMapper, clock);

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

  @Test
  @DisplayName("payment đã terminal → bấm thanh toán lại không làm gì")
  void finalPaymentShouldBeNoOp() {
    when(payments.findByProviderIntentId(INTENT_ID))
        .thenReturn(Optional.of(record(PaymentStatus.SUCCEEDED)));

    PaymentWebhookResponse response = service.pay(INTENT_ID, false);

    assertThat(response.status()).isEqualTo("SUCCEEDED");
    verify(webhookService, never()).apply(any(), any());
  }

  @Test
  @DisplayName("thanh toán thành công → webhook nhận status=succeeded + có provider_txn_id")
  void successShouldGoThroughWebhook() {
    when(payments.findByProviderIntentId(INTENT_ID))
        .thenReturn(Optional.of(record(PaymentStatus.REQUIRES_PAYMENT)));
    when(webhookService.apply(any(), any()))
        .thenReturn(new PaymentWebhookResponse("evt", UUID.randomUUID(), "SUCCEEDED", false, true));

    service.pay(INTENT_ID, false);

    ArgumentCaptor<PaymentWebhookRequest> captor =
        ArgumentCaptor.forClass(PaymentWebhookRequest.class);
    verify(webhookService).apply(any(), captor.capture());
    assertThat(captor.getValue().status()).isEqualTo("succeeded");
    assertThat(captor.getValue().providerTxnId()).startsWith("txn_mock_");
    assertThat(captor.getValue().amount()).isEqualByComparingTo("180000.00");
  }

  @Test
  @DisplayName("fail=true → provider báo failed (nhánh compensating của SL-2.9)")
  void failFlagShouldProduceFailedEvent() {
    when(payments.findByProviderIntentId(INTENT_ID))
        .thenReturn(Optional.of(record(PaymentStatus.REQUIRES_PAYMENT)));
    when(webhookService.apply(any(), any()))
        .thenReturn(new PaymentWebhookResponse("evt", UUID.randomUUID(), "FAILED", false, true));

    service.pay(INTENT_ID, true);

    ArgumentCaptor<PaymentWebhookRequest> captor =
        ArgumentCaptor.forClass(PaymentWebhookRequest.class);
    verify(webhookService).apply(any(), captor.capture());
    assertThat(captor.getValue().status()).isEqualTo("failed");
  }
}
