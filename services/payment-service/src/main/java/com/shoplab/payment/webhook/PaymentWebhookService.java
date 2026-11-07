package com.shoplab.payment.webhook;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.payment.idempotency.IdempotencyService;
import com.shoplab.payment.payment.PaymentExceptions;
import com.shoplab.payment.payment.PaymentRecord;
import com.shoplab.payment.payment.PaymentRepository;
import com.shoplab.payment.payment.PaymentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xử lý webhook provider — idempotent theo khoá <b>tự nhiên của provider</b> (ADR-0006 D2, {@code
 * architecture.md} §6.2).
 *
 * <p>Thứ tự bắt buộc (đổi thứ tự là thủng một lỗ):
 *
 * <ol>
 *   <li>Xác thực chữ ký + timestamp — do controller/verifier làm <b>trước</b> khi parse.
 *   <li>{@code INSERT} vào {@code payment_webhook_events} với {@code ON CONFLICT DO NOTHING}:
 *       insert được ⇒ lần đầu xử lý; không insert được ⇒ provider gửi lại, trả 200 và <b>không đổi
 *       gì</b>.
 *   <li>So số tiền với intent (lệch ⇒ 409).
 *   <li>Compare-and-set trạng thái ({@code WHERE status = :from}) — hai webhook song song thì chỉ
 *       một cái đổi được.
 * </ol>
 *
 * <p>Bước 2 + 4 nằm <b>cùng transaction</b>: event id lạ trỏ tới intent không tồn tại ⇒ rollback ⇒
 * row dedupe cũng biến mất, provider retry lại được (không có trạng thái "đã nhận nhưng chưa xử
 * lý").
 */
@Service
public class PaymentWebhookService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentWebhookService.class);

  private final PaymentRepository payments;
  private final Clock clock;

  public PaymentWebhookService(PaymentRepository payments, Clock clock) {
    this.payments = payments;
    this.clock = clock;
  }

  @Transactional
  public PaymentWebhookResponse apply(String rawBody, PaymentWebhookRequest request) {
    validate(request);
    PaymentStatus target = mapStatus(request.status());
    PaymentRecord payment = requirePayment(request.providerIntentId());
    if (payment.amount().compareTo(request.amount()) != 0) {
      throw new PaymentExceptions.AmountMismatch(payment.amount(), request.amount());
    }

    Instant now = Instant.now(clock);
    if (!recordEvent(rawBody, request, payment, target, now)) {
      LOG.info(
          "webhook {} đã xử lý trước đó cho payment {} — bỏ qua (provider retry)",
          request.eventId(),
          payment.id());
      return new PaymentWebhookResponse(
          request.eventId(), payment.id(), payment.status().name(), true, false);
    }
    return transition(payment, request, target, now);
  }

  private static void validate(PaymentWebhookRequest request) {
    requirePresent(request.eventId(), "eventId");
    requirePresent(request.providerIntentId(), "providerIntentId");
    requirePresent(request.status(), "status");
    if (request.amount() == null) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Webhook thiếu amount");
    }
  }

  private PaymentRecord requirePayment(String providerIntentId) {
    return payments
        .findByProviderIntentId(providerIntentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Không có payment intent với providerIntentId: " + providerIntentId));
  }

  /**
   * @return {@code true} = lần đầu thấy event này (được xử lý tiếp)
   */
  private boolean recordEvent(
      String rawBody,
      PaymentWebhookRequest request,
      PaymentRecord payment,
      PaymentStatus target,
      Instant now) {
    return payments.insertWebhookEvent(
        request.eventId(),
        payment.id(),
        request.providerTxnId(),
        target.name(),
        request.amount(),
        // SHA-256 raw body: đối soát khi tranh chấp, không lưu body (dữ liệu thẻ).
        IdempotencyService.hash(rawBody),
        now);
  }

  private PaymentWebhookResponse transition(
      PaymentRecord payment, PaymentWebhookRequest request, PaymentStatus target, Instant now) {
    if (payment.status() == target) {
      // Đã ở đúng trạng thái (event id khác, cùng kết quả) — không ghi lại, coi như no-op.
      return new PaymentWebhookResponse(
          request.eventId(), payment.id(), payment.status().name(), false, false);
    }
    if (payment.status().isFinal()) {
      throw new AppException(
          ErrorCode.CONFLICT,
          "Payment " + payment.id() + " đã ở trạng thái cuối (" + payment.status() + ")");
    }

    int updated;
    try {
      updated =
          payments.markStatus(payment.id(), payment.status(), target, request.providerTxnId(), now);
    } catch (DataIntegrityViolationException ex) {
      // provider_txn_id UNIQUE: cùng một giao dịch provider đang gắn cho payment khác.
      throw new PaymentExceptions.TransactionAlreadyUsed(request.providerTxnId());
    }
    if (updated == 0) {
      PaymentRecord current = payments.findById(payment.id()).orElseThrow();
      if (current.status() == target) {
        return new PaymentWebhookResponse(
            request.eventId(), payment.id(), current.status().name(), false, false);
      }
      throw new AppException(
          ErrorCode.CONFLICT,
          "Payment "
              + payment.id()
              + " vừa đổi trạng thái bởi webhook khác ("
              + current.status()
              + ")");
    }

    LOG.info(
        "payment {} status: {} -> {} (provider txn {})",
        payment.id(),
        payment.status(),
        target,
        request.providerTxnId());
    return new PaymentWebhookResponse(request.eventId(), payment.id(), target.name(), false, true);
  }

  /** Map chuỗi trạng thái của provider → enum nội bộ; lạ ⇒ 400 (retry không giúp gì). */
  private static PaymentStatus mapStatus(String rawStatus) {
    return switch (rawStatus.trim().toLowerCase(Locale.ROOT)) {
      case "succeeded" -> PaymentStatus.SUCCEEDED;
      case "failed" -> PaymentStatus.FAILED;
      default -> throw new PaymentExceptions.UnsupportedWebhookStatus(rawStatus);
    };
  }

  private static void requirePresent(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Webhook thiếu " + field);
    }
  }
}
