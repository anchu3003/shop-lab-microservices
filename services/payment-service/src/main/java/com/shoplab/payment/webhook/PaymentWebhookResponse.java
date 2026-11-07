package com.shoplab.payment.webhook;

import java.util.UUID;

/**
 * Kết quả xử lý webhook.
 *
 * @param duplicate {@code true} = đã thấy {@code eventId} này trước đó (provider gửi lại)
 * @param applied {@code true} = lần này có <b>đổi</b> trạng thái payment
 */
public record PaymentWebhookResponse(
    String eventId, UUID paymentId, String status, boolean duplicate, boolean applied) {}
