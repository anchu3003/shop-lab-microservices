package com.shoplab.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.NotFoundException;
import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.idempotency.IdempotencyRecord;
import com.shoplab.payment.idempotency.IdempotencyService;
import com.shoplab.payment.idempotency.IdempotencyStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Test nghiệp vụ payment intent: tạo idempotent (4 nhánh) + quyền đọc. */
class PaymentServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final UUID USER = UUID.fromString("22222222-2222-4222-8222-000000000001");
  private static final UUID OTHER = UUID.fromString("22222222-2222-4222-8222-000000000002");
  private static final UUID ORDER = UUID.fromString("33333333-3333-4333-8333-000000000001");
  private static final String KEY = "key-1";

  private final PaymentRepository payments = mock(PaymentRepository.class);
  private final PaymentWriter writer = mock(PaymentWriter.class);
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

  private final PaymentService service =
      new PaymentService(payments, writer, idempotency, properties, objectMapper);

  private static CreatePaymentIntentRequest request(BigDecimal amount) {
    return new CreatePaymentIntentRequest(ORDER, amount, "VND");
  }

  private static PaymentRecord record(PaymentStatus status, UUID userId) {
    return new PaymentRecord(
        UUID.randomUUID(),
        ORDER,
        userId,
        new BigDecimal("180000.00"),
        "VND",
        status,
        "mock",
        "pi_mock_1",
        status == PaymentStatus.SUCCEEDED ? "txn_mock_1" : null,
        KEY,
        NOW,
        NOW,
        status == PaymentStatus.SUCCEEDED ? NOW : null);
  }

  @Test
  @DisplayName("lần đầu: giữ khoá được → gọi writer, replayed=false")
  void firstRequestShouldWrite() {
    when(idempotency.begin(eq(USER + ":POST /v1/payment-intents"), eq(KEY), any()))
        .thenReturn(Optional.empty());
    PaymentIntentResponse response =
        PaymentIntentResponse.from(
            record(PaymentStatus.REQUIRES_PAYMENT, USER), "url", Duration.ofMinutes(15));
    when(writer.create(eq(USER), eq(KEY), any(), any()))
        .thenReturn(new PaymentWriter.Written(response, false));

    PaymentService.CreateResult result =
        service.create(USER, KEY, request(new BigDecimal("180000")));

    assertThat(result.replayed()).isFalse();
    assertThat(result.response().status()).isEqualTo("REQUIRES_PAYMENT");
    verify(writer).create(eq(USER), eq(KEY), any(), any());
  }

  @Test
  @DisplayName("retry cùng key: replay đúng response cũ, KHÔNG tạo intent mới")
  void retryShouldReplayStoredResponse() {
    PaymentIntentResponse stored =
        PaymentIntentResponse.from(
            record(PaymentStatus.REQUIRES_PAYMENT, USER), "url", Duration.ofMinutes(15));
    IdempotencyRecord completed =
        new IdempotencyRecord(
            USER + ":POST /v1/payment-intents",
            KEY,
            "hash",
            IdempotencyStatus.COMPLETED,
            201,
            objectMapper.writeValueAsString(stored),
            stored.id(),
            NOW,
            NOW);
    when(idempotency.begin(any(), eq(KEY), any())).thenReturn(Optional.of(completed));

    PaymentService.CreateResult result =
        service.create(USER, KEY, request(new BigDecimal("180000")));

    assertThat(result.replayed()).isTrue();
    assertThat(result.response().id()).isEqualTo(stored.id());
    verify(writer, never()).create(any(), any(), any(), any());
  }

  @Test
  @DisplayName("đọc intent của người khác → 404 (không lộ sự tồn tại)")
  void otherUsersIntentShouldBe404() {
    when(payments.findById(any()))
        .thenReturn(Optional.of(record(PaymentStatus.REQUIRES_PAYMENT, USER)));

    assertThatThrownBy(() -> service.get(UUID.randomUUID(), OTHER, false))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("admin đọc được intent của người khác; paymentUrl lấy từ cấu hình")
  void adminShouldReadAndUrlComeFromConfig() {
    PaymentRecord payment = record(PaymentStatus.REQUIRES_PAYMENT, USER);
    when(payments.findById(any())).thenReturn(Optional.of(payment));

    PaymentIntentResponse response = service.get(payment.id(), OTHER, true);

    assertThat(response.paymentUrl())
        .isEqualTo("http://localhost:8080/mock-provider/pay/" + payment.providerIntentId());
    assertThat(response.expiresAt()).isEqualTo(payment.createdAt().plus(Duration.ofMinutes(15)));
  }

  @Test
  @DisplayName("danh sách theo order: buyer chỉ thấy intent của mình, admin thấy hết")
  void listByOrderShouldFilterByOwner() {
    PaymentRecord mine = record(PaymentStatus.REQUIRES_PAYMENT, USER);
    PaymentRecord theirs = record(PaymentStatus.FAILED, OTHER);
    when(payments.findByOrderId(ORDER)).thenReturn(List.of(mine, theirs));

    assertThat(service.listByOrder(ORDER, USER, false)).hasSize(1);
    assertThat(service.listByOrder(ORDER, USER, true)).hasSize(2);
  }
}
