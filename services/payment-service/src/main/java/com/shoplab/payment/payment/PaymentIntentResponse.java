package com.shoplab.payment.payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Response của payment intent.
 *
 * @param paymentUrl trang thanh toán để client redirect tới (mock provider ở Phase 2)
 * @param providerTxnId {@code null} cho tới khi thanh toán xong
 * @param expiresAt hết hạn intent — khớp TTL reservation/voucher lock của phiên checkout
 */
public record PaymentIntentResponse(
    UUID id,
    UUID orderId,
    BigDecimal amount,
    String currency,
    String status,
    String paymentUrl,
    String providerTxnId,
    Instant createdAt,
    Instant expiresAt) {

  public static PaymentIntentResponse from(
      PaymentRecord payment, String paymentUrl, Duration intentTtl) {
    return new PaymentIntentResponse(
        payment.id(),
        payment.orderId(),
        payment.amount(),
        payment.currency(),
        payment.status().name(),
        paymentUrl,
        payment.providerTxnId(),
        payment.createdAt(),
        payment.createdAt().plus(intentTtl));
  }
}
