package com.shoplab.payment.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số nghiệp vụ của payment-service.
 *
 * @param intentTtl payment intent sống bao lâu — khớp TTL reservation (15 phút) để cả phiên
 *     checkout giữ chỗ trong cùng một khoảng thời gian
 * @param provider tên provider đang dùng ({@code mock} ở Phase 2), ghi vào cột {@code provider}
 * @param checkoutBaseUrl trang thanh toán trả cho client; mock provider của chính service
 * @param webhookSecret secret HMAC-SHA256 verify webhook — <b>bắt buộc</b>, rỗng thì chết lúc boot
 * @param webhookTolerance cửa sổ chống replay theo {@code X-Provider-Timestamp}
 * @param mockEnabled bật mock provider; bật ngoài {@code local}/{@code test} ⇒ chết lúc boot
 * @param mockLatency độ trễ giả lập của provider
 * @param idempotencyRetention giữ row {@code idempotency_keys} đã COMPLETED bao lâu (ADR-0006 D2:
 *     24 h)
 * @param inProgressStaleAfter row {@code IN_PROGRESS} cũ hơn mức này bị coi là mồ côi (process
 *     chết)
 * @param sweepBatchSize số row mỗi nhịp job dọn
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.payment")
public record PaymentProperties(
    @DefaultValue("15m") Duration intentTtl,
    @DefaultValue("mock") @NotBlank String provider,
    @DefaultValue("http://localhost:8080/mock-provider/pay") @NotBlank String checkoutBaseUrl,
    @DefaultValue("") String webhookSecret,
    @DefaultValue("5m") Duration webhookTolerance,
    @DefaultValue("false") boolean mockEnabled,
    @DefaultValue("50ms") Duration mockLatency,
    @DefaultValue("24h") Duration idempotencyRetention,
    @DefaultValue("1h") Duration inProgressStaleAfter,
    @DefaultValue("200") @Min(1) @Max(2000) int sweepBatchSize) {

  public PaymentProperties {
    if (intentTtl == null || intentTtl.isNegative() || intentTtl.isZero()) {
      throw new IllegalArgumentException("shoplab.payment.intent-ttl phải > 0");
    }
    if (webhookTolerance == null || webhookTolerance.isNegative() || webhookTolerance.isZero()) {
      throw new IllegalArgumentException("shoplab.payment.webhook-tolerance phải > 0");
    }
    if (webhookSecret == null || webhookSecret.isBlank()) {
      // Fail-closed: webhook là endpoint public duy nhất; secret rỗng = ai cũng ký được.
      throw new IllegalArgumentException(
          "shoplab.payment.webhook-secret chưa set (env PAYMENT_WEBHOOK_SECRET) — "
              + "webhook không thể verify chữ ký nếu thiếu secret");
    }
    if (idempotencyRetention == null
        || idempotencyRetention.isNegative()
        || idempotencyRetention.isZero()) {
      throw new IllegalArgumentException("shoplab.payment.idempotency-retention phải > 0");
    }
    if (inProgressStaleAfter == null
        || inProgressStaleAfter.isNegative()
        || inProgressStaleAfter.isZero()) {
      throw new IllegalArgumentException("shoplab.payment.in-progress-stale-after phải > 0");
    }
    if (mockLatency == null || mockLatency.isNegative()) {
      throw new IllegalArgumentException("shoplab.payment.mock-latency không được âm");
    }
  }

  /** Đường dẫn thanh toán cho một intent cụ thể. */
  public String checkoutUrl(String providerIntentId) {
    return checkoutBaseUrl.endsWith("/")
        ? checkoutBaseUrl + providerIntentId
        : checkoutBaseUrl + "/" + providerIntentId;
  }
}
