package com.shoplab.payment.debug;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Trạng thái **bơm lỗi** cho `POST /v1/payment-intents` — chỉ tồn tại khi {@code
 * shoplab.debug-endpoints=true} (compose local / E2E), nên production **không có** bean này và
 * không có endpoint nào để bật nó.
 *
 * <p><b>Vì sao cần</b> (`SL-2.9`): nhánh compensating "payment fail / timeout" của checkout saga
 * chỉ chứng minh được nếu bước payment **thật sự** fail hoặc **thật sự** chậm. Bơm bằng `docker
 * stop` chỉ tạo ra connection-refused; muốn kiểm chứng **read timeout** (`SAGA_READ_TIMEOUT`) có
 * tác dụng thì phải có một lệnh chậm hơn timeout đó.
 *
 * <p><b>Hai công tắc, cố ý tách rời</b>:
 *
 * <ul>
 *   <li>{@code sleepMs} — giữ request mở (mô phỏng dependency chậm).
 *   <li>{@code failStatus} — trả lỗi 4xx/5xx ngay (mô phỏng dependency lỗi).
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class FaultInjection {

  /** Chặn trên: không để một lệnh debug giữ thread vô hạn và làm cạn pool của service thật. */
  static final long MAX_SLEEP_MS = 60_000L;

  /** Chỉ bơm lỗi cho đường này — không ảnh hưởng health/webhook/mock-provider. */
  static final String DEFAULT_PATH_PREFIX = "/v1/payment-intents";

  private volatile long sleepMs;
  private volatile int failStatus;

  /**
   * Đặt công tắc.
   *
   * @param sleepMs thời gian giữ request (kẹp về {@code [0, MAX_SLEEP_MS]})
   * @param failStatus status trả ngay; {@code 0} = không bơm lỗi
   * @throws IllegalArgumentException khi {@code failStatus} ngoài {@code 400..599}
   */
  public synchronized void apply(long sleepMs, int failStatus) {
    if (failStatus != 0 && (failStatus < 400 || failStatus > 599)) {
      throw new IllegalArgumentException("failStatus phải là 0 hoặc 400..599, nhận: " + failStatus);
    }
    this.sleepMs = clamp(sleepMs);
    this.failStatus = failStatus;
  }

  /** Trả về trạng thái "không bơm gì" — gọi ở đầu và cuối mỗi kịch bản E2E. */
  public synchronized void reset() {
    this.sleepMs = 0;
    this.failStatus = 0;
  }

  public long sleepMs() {
    return sleepMs;
  }

  public int failStatus() {
    return failStatus;
  }

  /** Request này có bị bơm lỗi không (chỉ đúng một path prefix). */
  public boolean matches(String path) {
    return path != null && path.startsWith(DEFAULT_PATH_PREFIX);
  }

  public boolean active() {
    return sleepMs > 0 || failStatus > 0;
  }

  static long clamp(long requested) {
    return Math.min(Math.max(requested, 0L), MAX_SLEEP_MS);
  }
}
