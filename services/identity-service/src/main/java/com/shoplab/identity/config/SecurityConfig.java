package com.shoplab.identity.config;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import com.shoplab.identity.auth.jwt.JwtProperties;
import com.shoplab.identity.auth.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Bảo mật của identity-service (SL-1.1 dựng bean hash; SL-1.4 dựng filter chain đầy đủ).
 *
 * <p><b>Vì sao identity tự verify JWT</b>: nó vừa là issuer vừa là resource server. Verify tại chỗ
 * (bằng public key của chính nó) là <i>defense in depth</i> — ai gọi thẳng port service (compose
 * publish port để debug) vẫn phải có token hợp lệ, không thể tự phong role bằng header.
 *
 * <p><b>401 vs 403</b>: 401 = chưa xác thực (thiếu/sai/hết hạn token), 403 = đã xác thực nhưng
 * thiếu quyền. Cả hai trả {@link ApiError} — KHÔNG để Spring trả HTML mặc định, vì client phải
 * parse được cùng một format lỗi ở mọi service.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Endpoint công khai: probe, JWKS, và toàn bộ luồng auth (login/refresh/logout phải gọi được).
   */
  private static final String[] PUBLIC_PATHS = {
    "/health", "/ready", "/version", "/debug/**", "/v1/auth/**", "/.well-known/**"
  };

  /**
   * BCrypt: có salt sẵn trong hash, chậm có chủ ý (chống brute force) — lựa chọn mặc định an toàn.
   * Không dùng MD5/SHA-* (nhanh = dễ brute force, không salt = lộ rainbow table).
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * {@link Clock} tiêm được để test kiểm soát thời gian (hết hạn token) — thay vì gọi {@code
   * Instant.now()} rải rác trong code, thứ không thể test tất định.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Decoder verify token bằng public key của chính service + kiểm {@code iss}/{@code exp}/{@code
   * nbf}.
   *
   * <p>Khai validator tường minh: token do nơi khác phát hành (khác {@code iss}) phải bị từ chối,
   * kể cả khi chữ ký đúng.
   */
  @Bean
  public JwtDecoder jwtDecoder(JwtService jwtService, JwtProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtService.publicKey()).build();
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
                    .requestMatchers("/v1/admin/**")
                    .hasRole("ADMIN")
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
   * Map claim {@code role} → authority {@code ROLE_<role>} để {@code hasRole('ADMIN')} dùng được.
   *
   * <p>Role lấy từ token (không query DB mỗi request) — đánh đổi: đổi role cần token mới (tối đa 15
   * phút). Thu hồi quyền tức thời cần token version/blacklist, đã ghi ở {@code Backlog.md →
   * SL-1.4}.
   */
  private static JwtAuthenticationConverter roleConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
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
    // nên traceId vẫn có trong ApiError (điều kiện của AC SL-1.4).
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
