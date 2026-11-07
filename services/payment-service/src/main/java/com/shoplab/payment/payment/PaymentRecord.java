package com.shoplab.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng {@code payments}.
 *
 * @param providerIntentId id bên provider — webhook chỉ biết id này nên tra ngược bằng nó
 * @param providerTxnId mã giao dịch thật, {@code null} cho tới khi provider báo thành công/thất
 *     bại. UNIQUE ở DB: chốt chặn cộng tiền 2 lần cho cùng một giao dịch
 * @param paidAt chỉ có khi {@code status = SUCCEEDED} (DB canh bằng CHECK)
 */
public record PaymentRecord(
    UUID id,
    UUID orderId,
    UUID userId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String provider,
    String providerIntentId,
    String providerTxnId,
    String idempotencyKey,
    Instant createdAt,
    Instant updatedAt,
    Instant paidAt) {}
