package com.shoplab.gateway.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình rate limit ở biên (SL-4.3) — <b>token bucket trong Redis</b>.
 *
 * <p>Vì sao là config chứ không hard-code: ngưỡng là thứ phải chỉnh khi vận hành thật (flash sale
 * cần chặn chặt hơn ngày thường), và chỉnh ngưỡng không được bắt build lại image. Cùng lý do với
 * bảng route ở {@code GatewayProperties}.
 *
 * <p><b>Hai bucket, không phải một</b>: với request đã có danh tính (JWT verify xong) ta kiểm
 * <i>cả</i> hạn mức theo user <i>và</i> theo IP. Chỉ per-user thì kẻ lạm dụng xoay vài chục account
 * là vượt trần; chỉ per-IP thì một NAT/văn phòng làm cả nhóm bị chặn oan. Cả hai bucket được kiểm
 * trong <b>một</b> {@code EVAL} nên không tốn thêm round-trip.
 *
 * <p>Request <b>không</b> có danh tính (path public, khách) chỉ có bucket IP — không có gì để định
 * danh thì đừng bịa ra định danh.
 *
 * @param enabled công tắc tổng. {@code false} ⇒ filter không chạm Redis lần nào (dùng cho load test
 *     đo điểm vỡ và cho ca sự cố — xem {@code docs/runbook})
 * @param failOpen Redis lỗi thì <b>cho qua</b> + WARN. Mặc định {@code true}: rate limit là bảo vệ,
 *     không phải chức năng — chặn hết traffic khi Redis chết là tự bắn vào chân
 * @param keyPrefix tiền tố key Redis, tách namespace với cache của service khác
 * @param defaultPerUser hạn mức user cho route không có rule riêng
 * @param defaultPerIp hạn mức IP cho route không có rule riêng
 * @param bypass path KHÔNG rate limit (hạ tầng + endpoint debug). Rỗng ⇒ dùng danh sách mặc định
 *     trong code
 * @param trustedProxies IP của proxy/LB tin cậy — chỉ khi peer nằm trong danh sách này mới đọc
 *     {@code X-Forwarded-For}. Rỗng (mặc định) = <b>không tin ai</b>
 * @param routes rule theo route; khớp prefix dài nhất thắng
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.gateway.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("true") boolean failOpen,
    @DefaultValue("rl") String keyPrefix,
    @Valid @DefaultValue Limit defaultPerUser,
    @Valid @DefaultValue Limit defaultPerIp,
    @DefaultValue List<String> bypass,
    @DefaultValue List<String> trustedProxies,
    @Valid @DefaultValue List<Rule> routes) {

  /**
   * Path hạ tầng không bao giờ bị chặn: probe sống/chết mà 429 thì orchestrator tưởng service chết,
   * còn {@code /mock-provider/**} là trang thanh toán giả của provider (không phải traffic người
   * dùng thật).
   */
  static final List<String> DEFAULT_BYPASS =
      List.of(
          "/health",
          "/ready",
          "/version",
          "/v3/api-docs",
          "/v3/api-docs/**",
          "/mock-provider/**",
          "/debug/**");

  public RateLimitProperties {
    if (keyPrefix == null || keyPrefix.isBlank()) {
      keyPrefix = "rl";
    }
    if (defaultPerUser == null) {
      defaultPerUser = new Limit(100, 200);
    }
    if (defaultPerIp == null) {
      defaultPerIp = new Limit(200, 400);
    }
    if (bypass == null || bypass.isEmpty()) {
      bypass = DEFAULT_BYPASS;
    }
    if (trustedProxies == null) {
      trustedProxies = List.of();
    }
    if (routes == null) {
      routes = List.of();
    }
  }

  /**
   * Một hạn mức token bucket.
   *
   * @param rps tốc độ nạp token (token/giây) — chính là throughput <b>ổn định</b> được phép
   * @param burst số token tối đa (kích thước xô) — chính là burst được phép. {@code burst = rps}
   *     nghĩa là "không cho phép burst"
   */
  public record Limit(@DefaultValue("100") int rps, @DefaultValue("200") int burst) {

    public Limit {
      // Sai cấu hình phải chết lúc khởi động, không phải chia 0 lúc 3 giờ sáng.
      if (rps <= 0) {
        throw new IllegalArgumentException("rate-limit rps phải > 0 (nhận " + rps + ")");
      }
      if (burst < 1) {
        throw new IllegalArgumentException("rate-limit burst phải >= 1 (nhận " + burst + ")");
      }
    }
  }

  /**
   * Rule theo route.
   *
   * @param pathPrefix prefix khớp theo ranh giới segment (dùng chung luật với routing)
   * @param method HTTP method cần khớp; {@code null}/rỗng = mọi method. Cần method vì {@code GET
   *     /v1/checkout/...} không nên chịu hạn mức của {@code POST /v1/checkout}
   * @param perUser hạn mức theo user cho route này; {@code null} ⇒ dùng {@code defaultPerUser}
   * @param perIp hạn mức theo IP cho route này; {@code null} ⇒ dùng {@code defaultPerIp}
   */
  public record Rule(
      @NotBlank String pathPrefix, String method, @Valid Limit perUser, @Valid Limit perIp) {}
}
