package com.shoplab.order.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình của service, đọc từ env/application.yml (12-factor — AGENTS.md §6). Không hard-code gì
 * trong code; đổi hành vi = đổi env.
 *
 * @param env môi trường chạy, bind từ {@code APP_ENV}
 * @param debugEndpoints bật các endpoint {@code /debug/*} (chỉ nên bật ở local)
 * @param gitCommit git sha của build, do Docker build arg truyền vào
 * @param readiness cấu hình readiness probe
 */
@Validated
@ConfigurationProperties(prefix = "shoplab")
public record ServiceProperties(
    String env,
    @DefaultValue("false") boolean debugEndpoints,
    @DefaultValue("unknown") String gitCommit,
    @Valid @DefaultValue Readiness readiness) {

  public ServiceProperties {
    // Parse ngay khi bind: APP_ENV sai giá trị thì chết lúc boot, không đợi tới request đầu tiên.
    AppEnv.parse(env);
    if (readiness == null) {
      readiness = new Readiness(List.of());
    }
  }

  /** Môi trường đã được validate. */
  public AppEnv appEnv() {
    return AppEnv.parse(env);
  }

  /** Cấu hình readiness probe. */
  public record Readiness(@DefaultValue List<@Valid Dependency> dependencies) {

    public Readiness {
      if (dependencies == null) {
        dependencies = List.of();
      }
    }

    /** Một dependency cần check trước khi service nhận traffic. */
    public record Dependency(
        @NotBlank String name,
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        @DefaultValue("500ms") Duration timeout) {

      public Dependency {
        if (timeout == null) {
          timeout = Duration.ofMillis(500);
        }
      }
    }
  }
}
