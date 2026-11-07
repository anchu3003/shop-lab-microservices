package com.shoplab.payment.mockprovider;

import com.shoplab.payment.config.PaymentProperties;
import com.shoplab.payment.config.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Chốt chặn cuối cho mock provider: <b>fail-closed lúc boot</b>.
 *
 * <p>Mock provider cho phép đánh dấu một đơn là "đã thanh toán" mà không cần đồng nào — bật nhầm ở
 * staging/prod là lỗ hổng cho không hàng. Vì vậy kiểm ngay trong constructor (chết trước khi nhận
 * request đầu tiên), không kiểm lúc gọi.
 *
 * <p>Cùng tinh thần với {@code OpenApiEnvironmentPostProcessor} của commons: cờ "chỉ dùng ở local"
 * phải tự tắt ở môi trường khác, không phụ thuộc người deploy nhớ.
 */
@Component
public class MockProviderGuard {

  private static final Logger LOG = LoggerFactory.getLogger(MockProviderGuard.class);

  public MockProviderGuard(PaymentProperties properties, ServiceProperties service) {
    if (properties.mockEnabled() && !service.appEnv().isLocal()) {
      throw new IllegalStateException(
          "PAYMENT_MOCK_ENABLED=true nhưng APP_ENV="
              + service.appEnv()
              + " — mock provider chỉ được bật ở local/test (nó đánh dấu đơn đã thanh toán mà không"
              + " thu tiền)");
    }
    LOG.info("mock provider enabled={} (APP_ENV={})", properties.mockEnabled(), service.appEnv());
  }
}
