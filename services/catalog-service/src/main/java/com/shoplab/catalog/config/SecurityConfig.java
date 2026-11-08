package com.shoplab.catalog.config;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Bảo mật của catalog-service (SL-1.5).
 *
 * <p><b>Vì sao service tự verify JWT</b> (defense in depth): nếu chỉ tin gateway thì service trở
 * thành "tin tuyệt đối" vào một lớp — ai gọi thẳng port service (compose publish port để debug)
 * hoặc đi qua một gateway cấu hình sai là ghi được dữ liệu. Verify tại chỗ làm điều đó bất khả thi:
 * catalog chỉ tin <b>token</b>, không tin header {@code x-user-*} ai cũng gửi được.
 *
 * <p><b>401 vs 403</b>: 401 = chưa xác thực (thiếu/sai/hết hạn token), 403 = đã xác thực nhưng
 * thiếu quyền (buyer cố tạo product). Sai quy ước này là nguồn bug khó tìm ở client.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** Endpoint công khai: probe + debug (debug tự tắt ở môi trường khác) + spec OpenAPI (SL-1.8). */
  private static final String[] PUBLIC_PATHS = {
    "/health", "/ready", "/version", "/debug/**", "/v3/api-docs"
  };

  /**
   * Đọc: guest xem được (đây là lý do catalog là service read-heavy đầu tiên). Ghi: chỉ
   * seller/admin — khai ở từng method bằng {@code @PreAuthorize} vì cùng path, khác HTTP method.
   *
   * <p>SL-2.13: {@code /v1/skus} cũng public — tra giá nhiều SKU là đường đọc, khách chưa đăng nhập
   * vẫn phải thấy giá trong giỏ.
   */
  private static final String[] PUBLIC_READ_PATHS = {
    "/v1/categories", "/v1/products", "/v1/products/**", "/v1/skus"
  };

  /**
   * {@link Clock} tiêm được để test kiểm soát thời gian (createdAt/updatedAt) — thay vì gọi {@code
   * Instant.now()} rải rác trong code, thứ không thể test tất định.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Decoder verify token bằng public key lấy từ JWKS của identity-service + kiểm {@code iss}/{@code
   * exp}/{@code nbf}.
   *
   * <p>Nimbus tự cache JWKS và tự nạp lại khi gặp {@code kid} lạ — không cần code thêm.
   */
  @Bean
  public JwtDecoder jwtDecoder(JwtProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
      throws Exception {
    http
        // API stateless (Bearer token): không cookie ⇒ CSRF không áp dụng.
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_READ_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter()))
                    .authenticationEntryPoint(unauthorized(objectMapper))
                    .accessDeniedHandler(forbidden(objectMapper)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(unauthorized(objectMapper))
                    .accessDeniedHandler(forbidden(objectMapper)));
    return http.build();
  }

  /**
   * Map claim {@code role} → authority {@code ROLE_<role>} để {@code hasRole('SELLER')} dùng được.
   *
   * <p>Role lấy từ token (không query identity mỗi request) — đánh đổi: đổi role cần token mới (tối
   * đa 15 phút). Catalog <b>không</b> có DB user nên đây là lựa chọn duy nhất ở Phase 1.
   */
  private static JwtAuthenticationConverter roleConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          String role = jwt.getClaimAsString("role");
          if (role == null) {
            return List.<GrantedAuthority>of();
          }
          return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
    return converter;
  }

  private static AuthenticationEntryPoint unauthorized(ObjectMapper objectMapper) {
    return (request, response, ex) ->
        writeApiError(objectMapper, ErrorCode.UNAUTHORIZED, request, response);
  }

  private static AccessDeniedHandler forbidden(ObjectMapper objectMapper) {
    return (request, response, ex) ->
        writeApiError(objectMapper, ErrorCode.FORBIDDEN, request, response);
  }

  private static void writeApiError(
      ObjectMapper objectMapper,
      ErrorCode code,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    // CorrelationIdFilter của commons đăng ký ở HIGHEST_PRECEDENCE ⇒ chạy TRƯỚC security chain,
    // nên traceId vẫn có trong ApiError.
    response.setStatus(code.httpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    // Message chi tiết của Spring (vd "Jwt expired at ...") không trả ra ngoài: vừa khó hiểu vừa là
    // thông tin cho kẻ tấn công.
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                ApiError.of(code, code.defaultMessage(), request.getRequestURI())));
  }
}
