package com.shoplab.payment.webhook;

import java.math.BigDecimal;

/**
 * Payload webhook của provider (Phase 2: mock provider sinh ra đúng schema này).
 *
 * <p>{@code eventId} là khoá chống trùng chính: provider retry <b>cùng</b> sự kiện với cùng {@code
 * eventId}; nếu provider đổi id cho mỗi lần gửi thì chốt chặn thứ hai là {@code providerTxnId}
 * (UNIQUE ở DB).
 *
 * @param status {@code succeeded} hoặc {@code failed} (chuỗi thô của provider, map ở service)
 */
public record PaymentWebhookRequest(
    String eventId,
    String providerIntentId,
    String providerTxnId,
    String status,
    BigDecimal amount,
    String currency) {}
