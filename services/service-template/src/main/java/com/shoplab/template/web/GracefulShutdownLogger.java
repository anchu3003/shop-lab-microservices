package com.shoplab.template.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Log ra những request còn đang chạy khi service nhận SIGTERM ({@code docker stop}).
 *
 * <p><b>Vì sao là {@link SmartLifecycle} chứ không phải
 * {@code @EventListener(ContextClosedEvent)}:</b> với {@code server.shutdown=graceful}, Spring Boot
 * đợi request xong <i>trước khi</i> đóng context — nên nghe {@code ContextClosedEvent} thì đã quá
 * muộn, lúc đó không còn request nào để log.
 *
 * <p>Phase ở đây là {@link Integer#MAX_VALUE}, cao hơn {@code WebServerStartStopLifecycle} ({@code
 * Integer.MAX_VALUE - 1}), nên {@link #stop()} chạy <b>trước</b> khi Tomcat bắt đầu drain → vẫn
 * thấy được request đang chạy.
 */
@Component
public class GracefulShutdownLogger implements SmartLifecycle {

  private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdownLogger.class);

  private final RequestTracker requestTracker;
  private volatile boolean running;

  public GracefulShutdownLogger(RequestTracker requestTracker) {
    this.requestTracker = requestTracker;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void start() {
    running = true;
  }

  @Override
  public void stop() {
    running = false;
    List<String> inFlight = requestTracker.inFlightRequests();
    if (inFlight.isEmpty()) {
      LOG.info("graceful shutdown: không có request nào đang chạy, tắt ngay");
      return;
    }
    LOG.info("graceful shutdown: chờ {} request đang chạy hoàn tất: {}", inFlight.size(), inFlight);
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
