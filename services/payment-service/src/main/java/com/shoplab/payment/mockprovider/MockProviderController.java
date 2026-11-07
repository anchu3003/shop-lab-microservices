package com.shoplab.payment.mockprovider;

import com.shoplab.payment.webhook.PaymentWebhookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trang thanh toán giả của provider (Phase 2) — <b>public</b> và chỉ tồn tại khi {@code
 * shoplab.payment.mock-enabled=true} (mặc định local; {@link MockProviderGuard} chặn khởi động ở
 * môi trường khác).
 *
 * <p>Không cần token: người dùng thật trên trang provider cũng không mang token của hệ thống. Đây
 * là lý do endpoint này phải fail-closed theo môi trường thay vì dựa vào auth.
 */
@Tag(name = "Mock provider (local only)")
@RestController
@RequestMapping("/mock-provider")
@ConditionalOnProperty(prefix = "shoplab.payment", name = "mock-enabled", havingValue = "true")
public class MockProviderController {

  private final MockProviderService service;

  public MockProviderController(MockProviderService service) {
    this.service = service;
  }

  @Operation(
      summary = "Mô phỏng khách thanh toán (mock provider, local only)",
      description = "Sinh provider_txn_id rồi đi qua đúng đường webhook (ký HMAC + dedupe).")
  @PostMapping("/pay/{providerIntentId}")
  public PaymentWebhookResponse pay(
      @PathVariable String providerIntentId, @RequestParam(defaultValue = "false") boolean fail) {
    return service.pay(providerIntentId, fail);
  }
}
