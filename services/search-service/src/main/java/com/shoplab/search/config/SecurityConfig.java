package com.shoplab.search.config;

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
 * Bảo mật của search-service (SL-3.6).
 *
 * <p><b>Vì sao service tự verify JWT</b> (defense in depth): compose publish port ra host để debug
 * ⇒ ai gọi thẳng cũng vào được nếu service tin header của gateway. Ở đây service chỉ tin
 * <b>token</b>.
 *
 * <p><b>Ai được gọi gì</b>: `GET /v1/search` public (như đọc catalog). `POST
 * /v1/admin/search/reindex` chỉ {@code admin} — nó đọc toàn bộ catalog và ghi toàn bộ index, chạy
 * đồng thời 2 lần là nhân đôi tải lên cả hai service.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Endpoint công khai: probe + debug (tự tắt ở môi trường khác) + spec OpenAPI + TÌM KIẾM.
   *
   * <p>`/v1/search` public vì guest cũng phải tìm được hàng (index chỉ chứa dữ liệu công khai của
   * product). Ngược lại `/v1/admin/search/**` cần token ADMIN — đó là vận hành, không phải nghiệp
   * vụ.
   */
  private static final String[] PUBLIC_PATHS = {
    "/health", "/ready", "/version", "/debug/**", "/v3/api-docs", "/v1/search", "/v1/search/**"
  };

  /**
   * {@link Clock} tiêm được để test kiểm soát thời gian (expiresAt, createdAt) — thay vì gọi {@code
   * Instant.now()} rải rác trong code, thứ không thể test tất định.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Decoder verify token bằng public key từ JWKS của identity + kiểm {@code iss}/{@code exp}/{@code
   * nbf}.
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
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
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
    // CorrelationIdFilter của commons chạy TRƯỚC security chain ⇒ traceId vẫn có trong ApiError.
    response.setStatus(code.httpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                ApiError.of(code, code.defaultMessage(), request.getRequestURI())));
  }
}
