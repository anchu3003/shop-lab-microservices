package com.shoplab.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình của gateway — <b>toàn bộ</b> routing table nằm ở config, không hard-code trong code.
 *
 * <p>Vì sao route phải ở config: đổi địa chỉ service hoặc thêm service mới không được bắt client
 * đổi, cũng không nên bắt phải sửa code + build lại. Ở Phase 7 sẽ thay bằng route động (từ DB/UI).
 *
 * @param routes bảng route: path prefix → base URL của service đích
 * @param publicPaths path KHÔNG cần token (Ant-style, vd {@code /v1/auth/**})
 * @param jwt cấu hình verify token
 * @param timeouts timeout gọi service đích
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.gateway")
public record GatewayProperties(
    @Valid @DefaultValue List<Route> routes,
    @DefaultValue List<String> publicPaths,
    @Valid @DefaultValue Jwt jwt,
    @Valid @DefaultValue Timeouts timeouts) {

  private static final String DEFAULT_JWKS_URI = "http://localhost:8081/.well-known/jwks.json";

  public GatewayProperties {
    if (routes == null) {
      routes = List.of();
    }
    if (publicPaths == null) {
      publicPaths = List.of();
    }
    if (jwt == null) {
      jwt = new Jwt(DEFAULT_JWKS_URI, "https://identity.shoplab.local", null, null, null);
    }
    if (timeouts == null) {
      timeouts = new Timeouts(null, null);
    }
    // Gateway không có route thì vô dụng mà lại chạy như thường → fail-fast lúc bind config.
    if (routes.isEmpty()) {
      throw new IllegalArgumentException(
          "shoplab.gateway.routes rỗng: gateway phải có ít nhất 1 route (xem application.yml)");
    }
  }

  /**
   * Một route.
   *
   * @param pathPrefix prefix so khớp theo kiểu "đường dẫn", vd {@code /v1/auth} — khớp {@code
   *     /v1/auth} và {@code /v1/auth/...} nhưng KHÔNG khớp {@code /v1/authz}
   * @param baseUrl base URL của service đích, vd {@code http://identity-service:8081} (không có dấu
   *     {@code /} ở cuối)
   */
  public record Route(@NotBlank String pathPrefix, @NotBlank String baseUrl) {}

  /**
   * Cấu hình verify JWT.
   *
   * @param jwksUri URL JWKS của identity-service (qua mạng nội bộ, không qua chính gateway)
   * @param issuer giá trị {@code iss} bắt buộc trong token
   * @param jwksCacheTtl thời gian cache JWKS — rotate khoá không cần restart gateway
   * @param minRefreshInterval khoảng cách tối thiểu giữa 2 lần nạp lại JWKS "do gặp kid lạ": chặn
   *     việc token có kid rác biến mỗi request thành 1 lần gọi identity
   * @param clockSkew dung sai lệch đồng hồ giữa các máy khi check {@code exp}
   */
  public record Jwt(
      @NotBlank String jwksUri,
      @NotBlank String issuer,
      @DefaultValue("5m") Duration jwksCacheTtl,
      @DefaultValue("10s") Duration minRefreshInterval,
      @DefaultValue("30s") Duration clockSkew) {

    public Jwt {
      if (jwksCacheTtl == null) {
        jwksCacheTtl = Duration.ofMinutes(5);
      }
      if (minRefreshInterval == null) {
        minRefreshInterval = Duration.ofSeconds(10);
      }
      if (clockSkew == null) {
        clockSkew = Duration.ofSeconds(30);
      }
    }
  }

  /**
   * Timeout khi gọi service đích — <b>bắt buộc phải có</b>: không timeout nghĩa là một service đích
   * treo sẽ giữ hết thread của gateway và kéo sập cả hệ thống.
   */
  public record Timeouts(@DefaultValue("2s") Duration connect, @DefaultValue("5s") Duration read) {

    public Timeouts {
      if (connect == null) {
        connect = Duration.ofSeconds(2);
      }
      if (read == null) {
        read = Duration.ofSeconds(5);
      }
    }
  }
}
