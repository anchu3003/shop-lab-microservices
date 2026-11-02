package com.shoplab.commons.health;

import java.time.Duration;

/**
 * Kết quả một lần check dependency.
 *
 * @param name tên dependency (vd {@code postgres})
 * @param status UP/DOWN
 * @param detail mô tả ngắn — với DOWN thì ghi lý do để debug ngay từ body của {@code /ready}
 * @param latency thời gian check, dùng để phát hiện dependency "sống nhưng chậm"
 */
public record HealthCheckResult(String name, HealthStatus status, String detail, Duration latency) {

  /** Dependency OK. */
  public static HealthCheckResult up(String name, Duration latency) {
    return new HealthCheckResult(name, HealthStatus.UP, "ok", latency);
  }

  /** Dependency chết/không kết nối được. */
  public static HealthCheckResult down(String name, String detail, Duration latency) {
    return new HealthCheckResult(name, HealthStatus.DOWN, detail, latency);
  }

  /**
   * Dependency này có OK không.
   *
   * <p>Cố ý đặt tên {@code healthy()} chứ <b>không</b> phải {@code isUp()}: Jackson coi mọi method
   * {@code isXxx()} là getter, nên {@code isUp()} sẽ lộ thêm field thừa {@code "up": true} trong
   * body {@code /ready} — API bị bẩn chỉ vì một cái tên method.
   */
  public boolean healthy() {
    return status == HealthStatus.UP;
  }
}
