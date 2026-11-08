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
import com.shoplab.order.config.SagaProperties;
import com.shoplab.order.config.SagaPropertiesTest;
import com.shoplab.order.idempotency.IdempotencyRecord;
import com.shoplab.order.idempotency.IdempotencyService;
import com.shoplab.order.idempotency.IdempotencyStatus;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.saga.downstream.CartView;
import com.shoplab.order.saga.downstream.DownstreamException;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import com.shoplab.order.saga.downstream.SkuPrices;
import com.shoplab.order.saga.downstream.StockReservation;
import com.shoplab.order.saga.downstream.VoucherLock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit test cho phần **quyết định bù** của {@link CheckoutService} (`SL-2.6a`).
 *
 * <p>Kiểm bằng mock: fail ở bước nào thì đúng những tài nguyên đã giữ phải được trả, và **không**
 * trả cái chưa giữ. Đường HTTP thật + DB thật nằm ở {@code CheckoutSagaIT}.
 */
class CheckoutServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID SKU_ID = UUID.randomUUID();
  private static final String SCOPE = USER_ID + ":POST /v1/checkout";

  private final DownstreamGateway downstream = mock(DownstreamGateway.class);
  private final CheckoutSagaRepository sagas = mock(CheckoutSagaRepository.class);
  private final OrderWriter writer = mock(OrderWriter.class);
  private final IdempotencyService idempotency = mock(IdempotencyService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final SagaProperties properties = SagaPropertiesTest.defaults();

  private final CheckoutService service =
      new CheckoutService(
          downstream,
          sagas,
          writer,
          idempotency,
          properties,
          objectMapper,
          Clock.fixed(NOW, ZoneOffset.UTC));

  // __MORE_TESTS__

  @Test
  @DisplayName("giỏ rỗng → 422, KHÔNG giữ voucher/hàng")
  void emptyCartIsUnprocessable() {
    stubNewRun();
    when(downstream.readCart(any()))
        .thenReturn(new DownstreamResult<>(new CartView(List.of()), 200));

    assertThatThrownBy(() -> service.checkout(USER_ID, "token", "key-1", new CheckoutRequest(null)))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);

    verify(downstream, never()).lockVoucher(any(), any(), any(), any());
    verify(downstream, never()).reserveStock(any(), any(), any());
  }

  @Test
  @DisplayName("SKU không có giá → 409 và chưa gọi lock voucher")
  void missingSkuStopsBeforeHolding() {
    stubNewRun();
    when(downstream.readCart(any())).thenReturn(cart(2));
    when(downstream.priceSkus(any(), any()))
        .thenReturn(new DownstreamResult<>(new SkuPrices(List.of(), List.of(SKU_ID)), 200));

    assertThatThrownBy(() -> service.checkout(USER_ID, "token", "key-2", new CheckoutRequest(null)))
        .isInstanceOf(AppException.class)
        .hasMessageContaining(SKU_ID.toString())
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.CONFLICT);

    verify(downstream, never()).lockVoucher(any(), any(), any(), any());
  }

  @Test
  @DisplayName("reserve 409 (hết hàng) → RELEASE voucher, không tạo order")
  void reserveConflictReleasesVoucher() {
    UUID lockId = UUID.randomUUID();
    stubNewRun();
    sagaFound(lockId, null);
    when(downstream.readCart(any())).thenReturn(cart(1));
    when(downstream.priceSkus(any(), any())).thenReturn(priced());
    when(downstream.lockVoucher(any(), any(), any(), any()))
        .thenReturn(
            new DownstreamResult<>(new VoucherLock(lockId, new BigDecimal("10000.00")), 201));
    when(downstream.reserveStock(any(), any(), any()))
        .thenThrow(new DownstreamException("inventory", 409, "hết hàng"));

    assertThatThrownBy(() -> service.checkout(USER_ID, "token", "key-3", new CheckoutRequest("V1")))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.CONFLICT);

    verify(downstream).releaseVoucher(eq("token"), eq(lockId));
    verify(downstream, never()).releaseStock(any(), any());
    verify(writer, never()).createPending(any(), any());
  }

  // __MORE_TESTS2__

  @Test
  @DisplayName("payment fail → release stock + voucher + cancel order, trả 503, trả lại khoá")
  void paymentFailureCompensatesEverything() {
    UUID lockId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    stubNewRun();
    sagaFound(lockId, reservationId);
    when(downstream.readCart(any())).thenReturn(cart(1));
    when(downstream.priceSkus(any(), any())).thenReturn(priced());
    when(downstream.lockVoucher(any(), any(), any(), any()))
        .thenReturn(
            new DownstreamResult<>(new VoucherLock(lockId, new BigDecimal("10000.00")), 201));
    when(downstream.reserveStock(any(), any(), any()))
        .thenReturn(new DownstreamResult<>(new StockReservation(reservationId), 201));
    when(downstream.createPaymentIntent(any(), any(), any(), any()))
        .thenThrow(
            new DownstreamException("payment", "timeout", new QueryTimeoutException("slow")));

    assertThatThrownBy(() -> service.checkout(USER_ID, "token", "key-4", new CheckoutRequest("V1")))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.UNAVAILABLE);

    verify(downstream).releaseStock(eq("token"), eq(reservationId));
    verify(downstream).releaseVoucher(eq("token"), eq(lockId));
    verify(writer).cancelPending(any(), anyString(), any());
    // Khoá idempotency được TRẢ khi fail ⇒ client retry cùng key được ngay.
    verify(idempotency).abort(eq(SCOPE), eq("key-4"));
  }

  @Test
  @DisplayName("đơn 0 đồng → KHÔNG gọi payment intent, paymentRequired=false")
  void zeroTotalSkipsPayment() {
    stubNewRun();
    when(downstream.readCart(any())).thenReturn(cart(1));
    when(downstream.priceSkus(any(), any())).thenReturn(priced());
    when(downstream.lockVoucher(any(), any(), any(), any()))
        .thenReturn(
            new DownstreamResult<>(
                new VoucherLock(UUID.randomUUID(), new BigDecimal("100000.00")), 201));
    when(downstream.reserveStock(any(), any(), any()))
        .thenReturn(new DownstreamResult<>(new StockReservation(UUID.randomUUID()), 201));

    CheckoutService.CheckoutResult result =
        service.checkout(USER_ID, "token", "key-5", new CheckoutRequest("FREE"));

    assertThat(result.response().paymentRequired()).isFalse();
    assertThat(result.response().paymentIntentId()).isNull();
    assertThat(result.response().total()).isEqualByComparingTo("0.00");
    verify(downstream, never()).createPaymentIntent(any(), any(), any(), any());
  }

  @Test
  @DisplayName("replay: khoá đã COMPLETED → trả response cũ, không gọi service nào")
  void replayUsesStoredResponse() {
    CheckoutResponse stored =
        new CheckoutResponse(
            UUID.randomUUID(),
            OrderStatus.PENDING_PAYMENT,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("100.00"),
            "VND",
            true,
            UUID.randomUUID(),
            "http://pay");
    when(idempotency.begin(any(), any(), any()))
        .thenReturn(
            Optional.of(
                new IdempotencyRecord(
                    SCOPE,
                    "key-6",
                    "hash",
                    IdempotencyStatus.COMPLETED,
                    201,
                    objectMapper.writeValueAsString(stored),
                    stored.orderId(),
                    NOW,
                    NOW)));

    CheckoutService.CheckoutResult result =
        service.checkout(USER_ID, "token", "key-6", new CheckoutRequest(null));

    assertThat(result.replayed()).isTrue();
    assertThat(result.response().orderId()).isEqualTo(stored.orderId());
    verify(downstream, never()).readCart(any());
  }

  private void stubNewRun() {
    when(idempotency.begin(any(), any(), any())).thenReturn(Optional.empty());
  }

  private void sagaFound(UUID lockId, UUID reservationId) {
    when(sagas.findById(any()))
        .thenReturn(
            Optional.of(
                new CheckoutSagaRecord(
                    UUID.randomUUID(),
                    USER_ID,
                    CheckoutSagaState.STARTED,
                    true,
                    lockId,
                    reservationId,
                    null,
                    null,
                    null,
                    NOW,
                    NOW)));
  }

  private static DownstreamResult<CartView> cart(int qty) {
    return new DownstreamResult<>(new CartView(List.of(new CartView.CartLine(SKU_ID, qty))), 200);
  }

  private static DownstreamResult<SkuPrices> priced() {
    return new DownstreamResult<>(
        new SkuPrices(
            List.of(
                new SkuPrices.SkuPrice(SKU_ID, "STUB-1", "Stub item", new BigDecimal("100000.00"))),
            List.of()),
        200);
  }
}
