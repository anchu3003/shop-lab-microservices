package com.shoplab.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập {@code payments} + {@code payment_webhook_events}.
 *
 * <p>Đổi trạng thái dùng compare-and-set {@code WHERE status = :from} (cùng pattern
 * inventory/promotion/order): hai webhook đồng thời thì chỉ một cái đổi được, cái còn lại thấy 0
 * row ⇒ service đọc lại để trả kết quả đúng thay vì ghi đè.
 */
public interface PaymentRepository {

  void insert(PaymentRecord payment);

  Optional<PaymentRecord> findById(UUID id);

  Optional<PaymentRecord> findByProviderIntentId(String providerIntentId);

  /** Intent còn "sống" của một đơn (REQUIRES_PAYMENT/PROCESSING/SUCCEEDED). */
  Optional<PaymentRecord> findLiveByOrderId(UUID orderId);

  List<PaymentRecord> findByOrderId(UUID orderId);

  /**
   * @return 1 nếu chuyển được, 0 nếu trạng thái hiện tại không còn là {@code from}
   */
  int markStatus(UUID id, PaymentStatus from, PaymentStatus to, String providerTxnId, Instant now);

  /**
   * Ghi nhận sự kiện webhook — <b>nguyên tử</b> qua {@code ON CONFLICT DO NOTHING}.
   *
   * @return {@code true} nếu đây là lần đầu thấy {@code providerEventId}; {@code false} nếu
   *     provider gửi lại (đã xử lý rồi, không được xử lý lần hai)
   */
  boolean insertWebhookEvent(
      String providerEventId,
      UUID paymentId,
      String providerTxnId,
      String status,
      BigDecimal amount,
      String payloadSha256,
      Instant now);
}
