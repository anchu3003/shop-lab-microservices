package com.shoplab.gateway.web;

import com.shoplab.gateway.config.ServiceProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /version} — biết chính xác đang chạy build nào.
 *
 * <p>Khi debug trên môi trường thật, câu đầu tiên luôn là "đang chạy version nào, commit nào".
 * Không có endpoint này thì phải đi hỏi người deploy hoặc đọc log build.
 *
 * <p>{@link BuildProperties} do {@code spring-boot-maven-plugin:build-info} sinh ra ở {@code
 * META-INF/build-info.properties}. Dùng {@link ObjectProvider} vì khi chạy test (trước phase
 * package) file đó chưa tồn tại — thiếu bean không được làm app chết.
 */
@RestController
public class VersionController {

  private final String serviceName;
  private final String version;
  private final String buildTime;
  private final String gitCommit;

  public VersionController(
      @Value("${spring.application.name:unknown}") String serviceName,
      ObjectProvider<BuildProperties> buildProperties,
      ServiceProperties properties) {

    BuildProperties build = buildProperties.getIfAvailable();
    this.serviceName = serviceName;
    this.version = build == null ? "unknown" : build.getVersion();
    this.buildTime = build == null ? "unknown" : build.getTime().toString();
    this.gitCommit = properties.gitCommit();
  }

  @GetMapping("/version")
  public Map<String, String> version() {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("service", serviceName);
    body.put("version", version);
    body.put("gitCommit", gitCommit);
    body.put("buildTime", buildTime);
    return body;
  }
}
