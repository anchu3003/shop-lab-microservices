package com.shoplab.payment.payment;

import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.idempotency.IdempotencyService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Phần <b>ghi</b> của việc tạo payment intent — tách khỏi {@link PaymentService} có chủ ý.
 *
 * <p>Cùng lý do đã gặp ở `SL-2.5` (bug thật): nếu cả luồng nằm trong một {@code @Transactional} và
 * bên trong gọi {@code IdempotencyService.begin()} ({@code REQUIRES_NEW}) thì mỗi request cần 2
 * connection cùng lúc ⇒ pool nhỏ + request song song = <b>deadlock pool</b>. Cách đúng: ghi khoá
 * trước khi mở transaction, rồi mở transaction ghi payment.
 */
@Service
public class PaymentWriter {

  private final PaymentRepository payments;
  private final IdempotencyService idempotency;
  private final PaymentProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public PaymentWriter(
      PaymentRepository payments,
      IdempotencyService idempotency,
      PaymentProperties properties,
      ObjectMapper objectMapper,
      Clock clock) {
    this.payments = payments;
    this.idempotency = idempotency;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** {@code existing=true} ⇒ đơn đã có intent (trả 200 thay vì 201). */
  public record Written(PaymentIntentResponse response, boolean existing) {}

  /**
   * Ghi payment intent + đánh dấu idempotency COMPLETED trong <b>một transaction</b>.
   *
   * <p>Nhờ vậy crash giữa chừng không để lại khoá COMPLETED trỏ tới payment không tồn tại (client
   * retry sẽ nhận replay một intent ma).
   */
  @Transactional
  public Written create(
      UUID userId, String idempotencyKey, String scope, CreatePaymentIntentRequest request) {
    String currency = request.currencyOrDefault();

    // Bất biến nghiệp vụ: một đơn chỉ có MỘT intent còn sống. DB cũng canh bằng unique index
    // ux_payments_live_order — kiểm ở đây để trả 200/409 thay vì lỗi ràng buộc 500.
    Optional<PaymentRecord> live = payments.findLiveByOrderId(request.orderId());
    if (live.isPresent()) {
      PaymentRecord existing = live.get();
      if (existing.amount().compareTo(request.amount()) != 0
          || !existing.currency().equals(currency)) {
        throw new PaymentExceptions.IntentExists(
            request.orderId(), existing.amount(), request.amount());
      }
      PaymentIntentResponse response =
          PaymentIntentResponse.from(
              existing,
              properties.checkoutUrl(existing.providerIntentId()),
              properties.intentTtl());
      idempotency.complete(scope, idempotencyKey, 200, toJson(response), existing.id());
      return new Written(response, true);
    }

    Instant now = Instant.now(clock);
    UUID id = UUID.randomUUID();
    String providerIntentId = "pi_" + properties.provider() + "_" + id;
    PaymentRecord payment =
        new PaymentRecord(
            id,
            request.orderId(),
            userId,
            request.amount(),
            currency,
            PaymentStatus.REQUIRES_PAYMENT,
            properties.provider(),
            providerIntentId,
            null,
            idempotencyKey,
            now,
            now,
            null);
    payments.insert(payment);

    PaymentIntentResponse response =
        PaymentIntentResponse.from(
            payment, properties.checkoutUrl(providerIntentId), properties.intentTtl());
    idempotency.complete(scope, idempotencyKey, 201, toJson(response), id);
    return new Written(response, false);
  }

  private String toJson(PaymentIntentResponse response) {
    return objectMapper.writeValueAsString(response);
  }
}
