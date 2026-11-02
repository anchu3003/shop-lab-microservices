package com.shoplab.commons.health;

import java.time.Duration;

/**
 * Một dependency cần kiểm tra trước khi service nhận traffic (dùng cho readiness probe).
 *
 * <p>Interface chỉ có 2 method nên service có thể tự implement check sâu hơn (vd chạy {@code SELECT
 * 1} qua JDBC) mà không phải sửa controller — Phase 1+ sẽ làm vậy khi có DB thật.
 */
public interface DependencyHealthCheck {

  /** Tên hiển thị trong body {@code /ready}. */
  String name();

  /** Chạy check. Không được ném exception — lỗi phải được trả về dưới dạng {@code DOWN}. */
  HealthCheckResult check();

  /** Check mức TCP đơn giản — đủ để biết port có mở không. */
  static DependencyHealthCheck tcp(String name, String host, int port, Duration timeout) {
    return new TcpHealthCheck(name, host, port, timeout);
  }
}
