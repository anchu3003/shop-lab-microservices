package com.shoplab.payment.payment;

import com.shoplab.commons.error.NotFoundException;
import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.idempotency.IdempotencyRecord;
import com.shoplab.payment.idempotency.IdempotencyService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Nghiệp vụ payment intent: tạo (idempotent) + đọc.
 *
 * <p>Hai bất biến:
 *
 * <ol>
 *   <li><b>Idempotent theo hợp đồng ADR-0006 D2</b>: cùng {@code Idempotency-Key} + cùng payload ⇒
 *       trả lại đúng response cũ (200), không tạo intent thứ hai.
 *   <li><b>Một đơn một intent sống</b>: tạo lại cùng số tiền ⇒ trả intent cũ; khác số tiền ⇒ 409.
 * </ol>
 *
 * <p>Class này <b>không</b> mang {@code @Transactional}: nó là orchestrator, cố ý không giữ
 * connection trong lúc {@link IdempotencyService#begin} mở transaction riêng (bài học deadlock pool
 * ở `SL-2.5`).
 */
@Service
public class PaymentService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
  private static final String CREATE_SCOPE_SUFFIX = ":POST /v1/payment-intents";

  private final PaymentRepository payments;
  private final PaymentWriter writer;
  private final IdempotencyService idempotency;
  private final PaymentProperties properties;
  private final ObjectMapper objectMapper;

  public PaymentService(
      PaymentRepository payments,
      PaymentWriter writer,
      IdempotencyService idempotency,
      PaymentProperties properties,
      ObjectMapper objectMapper) {
    this.payments = payments;
    this.writer = writer;
    this.idempotency = idempotency;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** {@code replayed=true} ⇒ controller trả 200 (kèm header) thay vì 201. */
  public record CreateResult(PaymentIntentResponse response, boolean replayed) {}

  /**
   * Tạo payment intent idempotent.
   *
   * @throws com.shoplab.payment.idempotency.IdempotencyExceptions.MissingIdempotencyKey (400)
   * @throws com.shoplab.payment.idempotency.IdempotencyExceptions.KeyReused (422)
   * @throws com.shoplab.payment.idempotency.IdempotencyExceptions.InProgress (409)
   * @throws PaymentExceptions.IntentExists (409) khi đơn đã có intent khác số tiền
   */
  public CreateResult create(
      UUID userId, String idempotencyKey, CreatePaymentIntentRequest request) {
    String scope = userId + CREATE_SCOPE_SUFFIX;
    String requestHash = IdempotencyService.hash(canonical(request));

    Optional<IdempotencyRecord> completed = idempotency.begin(scope, idempotencyKey, requestHash);
    if (completed.isPresent()) {
      return new CreateResult(replay(completed.get()), true);
    }

    PaymentWriter.Written written = writer.create(userId, idempotencyKey, scope, request);
    LOG.info(
        "payment intent {} cho order {} amount={} {} existing={}",
        written.response().id(),
        request.orderId(),
        written.response().amount(),
        written.response().currency(),
        written.existing());
    return new CreateResult(written.response(), written.existing());
  }

  /** Đọc một intent — chỉ chủ sở hữu hoặc admin. */
  @Transactional(readOnly = true)
  public PaymentIntentResponse get(UUID id, UUID actorId, boolean admin) {
    PaymentRecord payment =
        payments
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Payment intent không tồn tại: " + id));
    if (!admin && !payment.userId().equals(actorId)) {
      // 404 chứ không 403: không xác nhận cho người ngoài biết intent này có tồn tại.
      throw new NotFoundException("Payment intent không tồn tại: " + id);
    }
    return toResponse(payment);
  }

  /**
   * Toàn bộ intent của một đơn — buyer chỉ thấy của mình, admin thấy tất cả.
   *
   * <p>Trả danh sách rỗng thay vì 404 khi không có intent (và cũng không lộ đơn của người khác có
   * intent hay không — kết quả luôn đã lọc theo chủ sở hữu).
   */
  @Transactional(readOnly = true)
  public List<PaymentIntentResponse> listByOrder(UUID orderId, UUID actorId, boolean admin) {
    return payments.findByOrderId(orderId).stream()
        .filter(payment -> admin || payment.userId().equals(actorId))
        .map(this::toResponse)
        .toList();
  }

  private PaymentIntentResponse toResponse(PaymentRecord payment) {
    return PaymentIntentResponse.from(
        payment, properties.checkoutUrl(payment.providerIntentId()), properties.intentTtl());
  }

  private PaymentIntentResponse replay(IdempotencyRecord record) {
    try {
      return objectMapper.readValue(record.responseBody(), PaymentIntentResponse.class);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Không đọc lại được response đã lưu cho key " + record.key(), ex);
    }
  }

  /**
   * Canonical payload để hash: serialize record theo đúng thứ tự field khai báo ⇒ cùng body ⇒ cùng
   * hash. Nếu sau này nhận {@code Map} thì phải sort key trước khi hash.
   */
  private String canonical(CreatePaymentIntentRequest request) {
    return objectMapper.writeValueAsString(request);
  }
}
