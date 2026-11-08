package com.shoplab.order.saga;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái saga trả cho client (`GET /v1/checkout/{orderId}`) — để client biết "đã giữ hàng chưa,
 * đã có link thanh toán chưa, hỏng ở đâu".
 */
public record SagaStatusResponse(
    UUID orderId,
    CheckoutSagaState state,
    boolean paymentRequired,
    UUID lockId,
    UUID reservationId,
    UUID paymentIntentId,
    String paymentUrl,
    String failureCode,
    Instant createdAt,
    Instant updatedAt) {

  public static SagaStatusResponse from(CheckoutSagaRecord saga) {
    return new SagaStatusResponse(
        saga.orderId(),
        saga.state(),
        saga.paymentRequired(),
        saga.lockId(),
        saga.reservationId(),
        saga.paymentIntentId(),
        saga.paymentUrl(),
        saga.failureCode(),
        saga.createdAt(),
        saga.updatedAt());
  }
}
