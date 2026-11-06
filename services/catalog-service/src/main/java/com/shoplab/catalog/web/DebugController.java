package com.shoplab.catalog.web;

import com.shoplab.commons.error.NotFoundException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint CHỈ để verify hành vi hạ tầng — <b>không phải</b> business logic.
 *
 * <p>Bật bằng {@code shoplab.debug-endpoints=true} — chỉ set trong docker-compose local. Ở môi
 * trường khác property này không được set nên controller không tồn tại, không có rủi ro lộ
 * endpoint.
 *
 * <p>Giữ lại ở catalog để kiểm chứng {@code ApiError} (404/500) và graceful shutdown trên service
 * thật thứ ba — nhất là khi debug "lỗi này do gateway hay do service?".
 */
@RestController
@ConditionalOnProperty(name = "shoplab.debug-endpoints", havingValue = "true")
public class DebugController {

  /** Chặn trên để không ai giữ request vô hạn và làm cạn thread pool. */
  static final long MAX_SLEEP_MS = 60_000L;

  /** Giữ request mở trong {@code ms} để test graceful shutdown. */
  @GetMapping("/debug/slow")
  public Map<String, Object> slow(@RequestParam(defaultValue = "1000") long ms)
      throws InterruptedException {
    long bounded = clamp(ms);
    Thread.sleep(bounded);
    return Map.of("sleptMs", bounded);
  }

  /** Kẹp về {@code [0, MAX_SLEEP_MS]}. Tách riêng để test được mà không phải chờ thật 60 giây. */
  static long clamp(long requestedMs) {
    return Math.min(Math.max(requestedMs, 0L), MAX_SLEEP_MS);
  }

  /** Lỗi nghiệp vụ có chủ đích → xem format {@code ApiError} + traceId. */
  @GetMapping("/debug/error")
  public Map<String, Object> error() {
    throw new NotFoundException("Lỗi cố ý từ /debug/error để xem format ApiError");
  }

  /** Lỗi không lường trước → kiểm tra nhánh 500 KHÔNG lộ chi tiết nội bộ ra client. */
  @GetMapping("/debug/boom")
  public Map<String, Object> boom() {
    throw new IllegalStateException("Chi tiết nội bộ này KHÔNG được xuất hiện trong response");
  }
}
