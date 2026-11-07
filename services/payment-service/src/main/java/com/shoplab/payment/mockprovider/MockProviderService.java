package com.shoplab.payment.mockprovider;

import com.shoplab.commons.error.NotFoundException;
import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.payment.PaymentRecord;
import com.shoplab.payment.payment.PaymentRepository;
import com.shoplab.payment.webhook.PaymentWebhookRequest;
import com.shoplab.payment.webhook.PaymentWebhookResponse;
import com.shoplab.payment.webhook.PaymentWebhookService;
import com.shoplab.payment.webhook.WebhookSignatureVerifier;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Provider giả cho Phase 2 (chưa nối cổng thanh toán thật).
 *
 * <p><b>Vì sao đi qua đúng đường webhook</b> thay vì gọi thẳng repository: mock phải chứng minh
 * được <i>toàn bộ</i> đường thật — ký HMAC, verify chữ ký, dedupe theo event id, compare-and-set
 * trạng thái. Nếu mock tự set {@code status = SUCCEEDED} thì test xanh mà production gãy ở đúng chỗ
 * khó thấy nhất.
 *
 * <p>Độ trễ {@code mock-latency} mô phỏng provider thật (ADR-0006 D4: p99 ~50 ms) để timeout/ngân
 * sách latency của saga (`SL-2.6`) có cái để đo.
 */
@Service
@ConditionalOnProperty(prefix = "shoplab.payment", name = "mock-enabled", havingValue = "true")
public class MockProviderService {

  private static final Logger LOG = LoggerFactory.getLogger(MockProviderService.class);

  private final PaymentRepository payments;
  private final PaymentWebhookService webhookService;
  private final WebhookSignatureVerifier verifier;
  private final PaymentProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public MockProviderService(
      PaymentRepository payments,
      PaymentWebhookService webhookService,
      WebhookSignatureVerifier verifier,
      PaymentProperties properties,
      ObjectMapper objectMapper,
      Clock clock) {
    this.payments = payments;
    this.webhookService = webhookService;
    this.verifier = verifier;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /**
   * Mô phỏng khách bấm "thanh toán" trên trang của provider.
   *
   * @param fail {@code true} ⇒ provider báo {@code failed} (để test nhánh compensating `SL-2.9`)
   */
  public PaymentWebhookResponse pay(String providerIntentId, boolean fail) {
    PaymentRecord payment =
        payments
            .findByProviderIntentId(providerIntentId)
            .orElseThrow(
                () -> new NotFoundException("Payment intent không tồn tại: " + providerIntentId));
    if (payment.status().isFinal()) {
      // Provider thật cũng vậy: bấm lại không tạo giao dịch thứ hai.
      LOG.info(
          "mock-pay: payment {} đã terminal ({}) — không làm gì", payment.id(), payment.status());
      return new PaymentWebhookResponse(null, payment.id(), payment.status().name(), false, false);
    }

    sleepMockLatency();

    String suffix = UUID.randomUUID().toString();
    PaymentWebhookRequest event =
        new PaymentWebhookRequest(
            "evt_" + properties.provider() + "_" + suffix,
            providerIntentId,
            "txn_" + properties.provider() + "_" + suffix,
            fail ? "failed" : "succeeded",
            payment.amount(),
            payment.currency());
    String rawBody = objectMapper.writeValueAsString(event);
    String timestamp = Long.toString(clock.instant().getEpochSecond());
    String signature = verifier.sign(timestamp, rawBody);
    // Verify lại chính payload mình vừa ký: chứng minh đường xác thực thật sự chạy, không bị bỏ
    // qua.
    verifier.verify(signature, timestamp, rawBody);
    return webhookService.apply(rawBody, event);
  }

  private void sleepMockLatency() {
    try {
      Thread.sleep(properties.mockLatency().toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOG.warn("mock-pay bị interrupt khi giả lập độ trễ provider");
    }
  }
}
