package com.shoplab.order.health;

import com.shoplab.commons.health.DependencyHealthCheck;
import com.shoplab.commons.health.HealthCheckResult;
import com.shoplab.order.config.ServiceProperties;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Biến cấu hình readiness thành danh sách check và chạy tất cả.
 *
 * <p>Danh sách dependency nằm trong config (không hard-code) nên thêm/bớt dependency chỉ cần sửa
 * {@code application.yml} hoặc env.
 */
@Service
public class ReadinessService {

  private final List<DependencyHealthCheck> checks;

  public ReadinessService(ServiceProperties properties) {
    this.checks =
        properties.readiness().dependencies().stream()
            .map(
                dependency ->
                    DependencyHealthCheck.tcp(
                        dependency.name(),
                        dependency.host(),
                        dependency.port(),
                        dependency.timeout()))
            .toList();
  }

  /** Chạy tất cả check (song song ở Phase 5 khi có nhiều dependency) và trả kết quả từng cái. */
  public List<HealthCheckResult> checkAll() {
    return checks.stream().map(DependencyHealthCheck::check).toList();
  }

  /** Số dependency đang được theo dõi — dùng cho log lúc boot và cho test. */
  public int dependencyCount() {
    return checks.size();
  }
}
