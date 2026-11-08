package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Payment intent vừa tạo ở `payment-service` (`POST /v1/payment-intents`).
 *
 * @param paymentUrl trang thanh toán trả cho client (mock provider ở Phase 2) — {@code null} khi
 *     đơn 0 đồng (saga cố ý không tạo intent)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentIntent(UUID id, String paymentUrl, String status) {

  /**
   * Kết quả "không cần thanh toán" (đơn 0 đồng) — dùng thay cho {@code null} để tránh NPE rải rác.
   */
  public static PaymentIntent none() {
    return new PaymentIntent(null, null, null);
  }

  public boolean absent() {
    return id == null;
  }
}
