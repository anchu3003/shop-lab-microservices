package com.shoplab.gateway.ratelimit;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.ApiError;
import com.shoplab.gateway.auth.UserHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
 * Rate limit ở biên — chặn <b>trước khi</b> request đi vào service (SL-4.3).
 *
 * <p>Đặt <b>sau</b> {@link com.shoplab.gateway.auth.JwtAuthFilter} có chủ ý: khi rate limit chạy,
 * {@code x-user-id} (nếu có) đã được verify chữ ký. Đọc {@code sub} từ token <b>chưa</b> verify để
 * đặt key là lỗ hổng: kẻ tấn công gửi mỗi request một {@code sub} bịa ⇒ mỗi request một bucket mới
 * ⇒ rate limit bằng 0.
 *
 * <p>Ba nhánh, và nhánh thứ ba mới là chỗ đáng bàn:
 *
 * <ol>
 *   <li>còn token ⇒ thêm {@code X-RateLimit-*} rồi cho qua,
 *   <li>hết token ⇒ <b>429</b> + {@code Retry-After}, request <b>không</b> chạm service đích (đây
 *       là điểm của cả ticket: chặn ở biên, không chặn ở DB),
 *   <li>không kiểm được (Redis lỗi) ⇒ <b>fail-open</b>: cho qua + WARN theo cửa sổ. Rate limit là
 *       lớp bảo vệ, không phải chức năng — Redis chết mà chặn hết traffic là biến sự cố nhỏ thành
 *       sự cố toàn hệ.
 * </ol>
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

  static final String HEADER_LIMIT = "X-RateLimit-Limit";
  static final String HEADER_REMAINING = "X-RateLimit-Remaining";
  static final String HEADER_RESET = "X-RateLimit-Reset";

  private final RateLimitProperties properties;
  private final List<PathPattern> bypassPatterns;
  private final RateLimitRules rules;
  private final RateLimiter limiter;
  private final ClientIpResolver ipResolver;
  private final RateLimitMetrics metrics;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public RateLimitFilter(
      RateLimitProperties properties,
      RateLimitRules rules,
      RateLimiter limiter,
      ClientIpResolver ipResolver,
      RateLimitMetrics metrics,
      Clock clock,
      ObjectMapper objectMapper) {
    this.properties = properties;
    PathPatternParser parser = new PathPatternParser();
    this.bypassPatterns = properties.bypass().stream().map(parser::parse).toList();
    this.rules = rules;
    this.limiter = limiter;
    this.ipResolver = ipResolver;
    this.metrics = metrics;
    this.clock = clock;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    if (!properties.enabled()) {
      chain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();
    if (isBypass(path)) {
      metrics.noteBypassed();
      chain.doFilter(request, response);
      return;
    }

    ResolvedRateLimit rule = rules.resolve(path, request.getMethod());
    String ipKey = key(rule.routeKey(), "ip", ipResolver.resolve(request));
    // Header danh tính do JwtAuthFilter gắn SAU khi verify — tin được. Khách (path public) không
    // có.
    String userId = request.getHeader(UserHeaders.USER_ID);
    String userKey =
        (userId == null || userId.isBlank()) ? null : key(rule.routeKey(), "u", userId);

    long now = clock.millis();
    RateLimitDecision decision;
    try {
      decision = limiter.consume(userKey, rule.perUser(), ipKey, rule.perIp(), now);
    } catch (RuntimeException e) {
      // Bắt RuntimeException (không chỉ RateLimitUnavailableException): bug parse/Lua cũng không
      // được biến thành 500 cho mọi request — xem onLimiterFailure.
      onLimiterFailure(request, response, chain, e, now);
      return;
    }

    if (!decision.allowed()) {
      if (metrics.noteRejected(now)) {
        LOG.warn(
            "rate-limit: chặn {} {} · xô={} · limit={} · retry sau {} ms",
            request.getMethod(),
            path,
            decision.scope(),
            decision.limit(),
            decision.retryAfterMs());
      }
      writeTooManyRequests(request, response, decision, now);
      return;
    }

    metrics.noteAllowed();
    response.setHeader(HEADER_LIMIT, Integer.toString(decision.limit()));
    response.setHeader(HEADER_REMAINING, Integer.toString(Math.max(0, decision.remaining())));
    response.setHeader(HEADER_RESET, Long.toString(resetAtSecond(decision, rule, now)));
    chain.doFilter(request, response);
  }

  /** Redis lỗi/không parse được ⇒ quyết theo {@code fail-open}. */
  private void onLimiterFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      RuntimeException failure,
      long now)
      throws ServletException, IOException {

    if (properties.failOpen()) {
      if (metrics.noteFailOpen(now)) {
        LOG.warn("rate-limit: redis lỗi, fail-open — cho request đi tiếp: {}", rootCause(failure));
      }
      chain.doFilter(request, response);
      return;
    }
    LOG.error("rate-limit: redis lỗi và fail-open=false ⇒ 503: {}", rootCause(failure));
    response.setStatus(ErrorCode.UNAVAILABLE.httpStatus().value());
    writeJson(response, ErrorCode.UNAVAILABLE, "Không kiểm được giới hạn request lúc này", request);
  }

  private void writeTooManyRequests(
      HttpServletRequest request,
      HttpServletResponse response,
      RateLimitDecision decision,
      long now)
      throws IOException {

    long retryAfter = Math.max(1, decision.retryAfterSeconds());
    response.setStatus(ErrorCode.TOO_MANY_REQUESTS.httpStatus().value());
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
    response.setHeader(HEADER_LIMIT, Integer.toString(decision.limit()));
    response.setHeader(HEADER_REMAINING, "0");
    response.setHeader(HEADER_RESET, Long.toString(now / 1000 + retryAfter));
    writeJson(
        response,
        ErrorCode.TOO_MANY_REQUESTS,
        "Vượt quá giới hạn request cho phép — thử lại sau " + retryAfter + " giây",
        request);
  }

  private void writeJson(
      HttpServletResponse response, ErrorCode code, String message, HttpServletRequest request)
      throws IOException {

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getOutputStream(), ApiError.of(code, message, request.getRequestURI()));
  }

  /** Thời điểm (epoch giây) xô đầy lại — chuẩn của {@code X-RateLimit-Reset}. */
  private static long resetAtSecond(
      RateLimitDecision decision, ResolvedRateLimit rule, long nowMs) {
    int rps =
        decision.scope() == RateLimitDecision.Bucket.USER
            ? rule.perUser().rps()
            : rule.perIp().rps();
    long missingTokens = Math.max(0, decision.limit() - decision.remaining());
    long refillMs = rps <= 0 ? 0 : (missingTokens * 1000L + rps - 1) / rps;
    return nowMs / 1000 + (refillMs + 999) / 1000;
  }

  private boolean isBypass(String path) {
    PathContainer container = PathContainer.parsePath(path);
    return bypassPatterns.stream().anyMatch(pattern -> pattern.matches(container));
  }

  private String key(String routeKey, String scope, String value) {
    return properties.keyPrefix() + ":" + routeKey + ":" + scope + ":" + value;
  }

  /** Gốc của chuỗi cause — log 1 dòng thay vì stack trace mỗi 5 giây. */
  private static String rootCause(Throwable failure) {
    Throwable cause = failure;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause.getClass().getSimpleName() + ": " + cause.getMessage();
  }
}
