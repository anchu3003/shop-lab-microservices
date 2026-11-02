package com.shoplab.commons.health;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Check mức TCP: mở được socket tới {@code host:port} trong khoảng {@code timeout} hay không.
 *
 * <p>Trade-off: TCP check <b>rẻ</b> và <b>không cần driver</b> (không phải nhét JDBC/Redis client
 * vào shared lib), nhưng nó chỉ nói "cổng có mở" — không nói DB có query được hay không. Khi có
 * service thật ở Phase 1, chỗ này nên được thay bằng check sâu hơn (query {@code SELECT 1});
 * interface {@link DependencyHealthCheck} tồn tại chính để việc thay thế đó không phải sửa
 * controller.
 */
public class TcpHealthCheck implements DependencyHealthCheck {

  private final String name;
  private final String host;
  private final int port;
  private final Duration timeout;

  public TcpHealthCheck(String name, String host, int port, Duration timeout) {
    this.name = name;
    this.host = host;
    this.port = port;
    this.timeout = timeout == null ? Duration.ofSeconds(1) : timeout;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public HealthCheckResult check() {
    long startedAtNanos = System.nanoTime();
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
      return HealthCheckResult.up(name, elapsed(startedAtNanos));
    } catch (IOException | RuntimeException e) {
      // Không ném ra ngoài: readiness phải trả 503 kèm lý do, chứ không được làm sập request.
      String detail = e.getClass().getSimpleName() + ": " + e.getMessage();
      return HealthCheckResult.down(name, detail, elapsed(startedAtNanos));
    }
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  private static Duration elapsed(long startedAtNanos) {
    return Duration.ofNanos(System.nanoTime() - startedAtNanos);
  }
}
