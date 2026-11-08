package com.shoplab.payment.debug;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Công tắc bơm lỗi cho E2E (`SL-2.9`) — {@code /debug/**} đã nằm trong public-path của
 * payment-service và controller này chỉ tồn tại khi {@code shoplab.debug-endpoints=true}.
 *
 * <p><b>Vì sao public là chấp nhận được ở đây</b>: nó không tồn tại ngoài local/E2E (property không
 * được set ⇒ 404 route), và bản thân nó chỉ làm chậm/lỗi đường {@code /v1/payment-intents} — không
 * đọc hay sửa dữ liệu. Mọi endpoint nghiệp vụ vẫn verify JWT như cũ.
 */
@Hidden
@RestController
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class DebugFaultsController {

  private final FaultInjection faults;

  public DebugFaultsController(FaultInjection faults) {
    this.faults = faults;
  }

  /**
   * Bật bơm lỗi: {@code sleepMs} giữ request mở, {@code failStatus} trả lỗi ngay.
   *
   * <p>Ví dụ: {@code POST /debug/faults?sleepMs=2000} (chậm hơn `SAGA_READ_TIMEOUT` 800 ms ⇒ saga
   * phải timeout) hoặc {@code POST /debug/faults?failStatus=500}.
   */
  @PostMapping("/debug/faults")
  public Map<String, Object> apply(
      @RequestParam(defaultValue = "0") long sleepMs,
      @RequestParam(defaultValue = "0") int failStatus) {
    faults.apply(sleepMs, failStatus);
    return state();
  }

  /** Tắt bơm lỗi — E2E luôn gọi ở đầu và cuối kịch bản để không rò sang test khác. */
  @PostMapping("/debug/faults/reset")
  public Map<String, Object> reset() {
    faults.reset();
    return state();
  }

  private Map<String, Object> state() {
    return Map.of(
        "sleepMs", faults.sleepMs(),
        "failStatus", faults.failStatus(),
        "pathPrefix", FaultInjection.DEFAULT_PATH_PREFIX);
  }
}
