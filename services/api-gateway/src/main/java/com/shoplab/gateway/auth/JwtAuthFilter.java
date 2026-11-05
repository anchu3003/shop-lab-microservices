package com.shoplab.gateway.auth;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import com.shoplab.gateway.config.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.ObjectMapper;

/**
 * Cửa gác duy nhất của hệ thống: request vào path <b>không</b> public thì phải có token hợp lệ.
 *
 * <p>Luồng:
 *
 * <ol>
 *   <li>path public ({@code /v1/auth/**}, {@code /.well-known/**}, {@code /health}, {@code /ready})
 *       → cho qua nhưng <b>vẫn xoá</b> header danh tính client gửi,
 *   <li>verify token → gắn {@code x-user-id}/{@code x-user-role} (ghi đè),
 *   <li>token sai → <b>401</b>; không lấy được JWKS → <b>503</b> (lỗi hạ tầng, client retry được).
 * </ol>
 *
 * <p>Filter này tự ghi {@code ApiError} vì nó chạy <b>trước</b> Spring MVC — {@code
 * GlobalExceptionHandler} của commons không thấy exception ở đây. Body vẫn cùng format để client
 * chỉ phải xử lý một kiểu lỗi.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthFilter.class);
  private static final String BEARER = "Bearer";

  private final JwtVerifier verifier;
  private final List<PathPattern> publicPatterns;
  private final ObjectMapper objectMapper;

  public JwtAuthFilter(
      JwtVerifier verifier, GatewayProperties properties, ObjectMapper objectMapper) {
    this.verifier = verifier;
    PathPatternParser parser = new PathPatternParser();
    this.publicPatterns = properties.publicPaths().stream().map(parser::parse).toList();
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    if (isPublic(request.getRequestURI())) {
      // Public KHÔNG có nghĩa là "tin client": xoá sạch header danh tính để không endpoint nào đọc
      // phải x-user-* do client tự khai.
      chain.doFilter(new UserHeaderRequest(request, null), response);
      return;
    }

    try {
      AuthenticatedUser user = verifier.verify(bearerToken(request));
      chain.doFilter(new UserHeaderRequest(request, user), response);
    } catch (InvalidTokenException e) {
      LOG.warn("từ chối {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
      writeError(request, response, ErrorCode.UNAUTHORIZED, e.getMessage());
    } catch (JwksUnavailableException e) {
      LOG.error("không verify được token: {}", e.getMessage());
      writeError(request, response, ErrorCode.UNAVAILABLE, "Không xác thực được token lúc này");
    }
  }

  private boolean isPublic(String path) {
    PathContainer container = PathContainer.parsePath(path);
    return publicPatterns.stream().anyMatch(pattern -> pattern.matches(container));
  }

  private static String bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || header.isBlank()) {
      throw new InvalidTokenException("Thiếu token");
    }
    // Scheme không phân biệt hoa/thường (RFC 6750), nhưng phải đúng dạng "Bearer <token>".
    boolean hasScheme =
        header.regionMatches(true, 0, BEARER, 0, BEARER.length())
            && header.length() > BEARER.length()
            && header.charAt(BEARER.length()) == ' ';
    if (!hasScheme) {
      throw new InvalidTokenException("Authorization phải có dạng 'Bearer <token>'");
    }
    String token = header.substring(BEARER.length() + 1).trim();
    if (token.isEmpty()) {
      throw new InvalidTokenException("Thiếu token");
    }
    return token;
  }

  private void writeError(
      HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message)
      throws IOException {

    response.setStatus(code.httpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getOutputStream(), ApiError.of(code, message, request.getRequestURI()));
  }
}
