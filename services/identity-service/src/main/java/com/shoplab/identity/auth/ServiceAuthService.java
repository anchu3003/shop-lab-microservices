package com.shoplab.identity.auth;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.identity.auth.jwt.IssuedToken;
import com.shoplab.identity.auth.jwt.JwtService;
import com.shoplab.identity.config.ServiceAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Cấp service token (SL-3.11) — phần `client_credentials` của OAuth2, làm gọn cho 1 issuer nội bộ.
 *
 * <p><b>Ba điều kiện phải đúng, theo thứ tự</b>:
 *
 * <ol>
 *   <li><b>Client tồn tại</b> — tra registry trong config.
 *   <li><b>Secret khớp</b> — so sánh <b>hằng thời gian</b> ({@link MessageDigest#isEqual}). So sánh
 *       {@code equals} thoát sớm ở byte đầu khác nhau ⇒ đo thời gian phản hồi có thể dò ra secret
 *       từng byte; đây là lỗi kinh điển và ở đây sửa chỉ mất 1 dòng.
 *   <li><b>Audience nằm trong allowlist của client</b> — nếu không, một service bị lộ secret xin
 *       được token đi <b>mọi</b> nơi, tức là {@code aud} trở thành trang trí.
 * </ol>
 *
 * <p><b>Vì sao client lạ và sai secret trả <i>cùng</i> một lỗi</b>: nếu khác nhau, kẻ dò biết được
 * client id nào tồn tại (user/tenant enumeration — cùng lý do với {@link
 * InvalidCredentialsException} ở luồng login).
 */
@Service
@EnableConfigurationProperties(ServiceAuthProperties.class)
public class ServiceAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceAuthService.class);

  private final ServiceAuthProperties properties;
  private final JwtService jwtService;
  private final Clock clock;

  public ServiceAuthService(ServiceAuthProperties properties, JwtService jwtService, Clock clock) {
    this.properties = properties;
    this.jwtService = jwtService;
    this.clock = clock;
  }

  /**
   * Cấp token cho service.
   *
   * @throws AppException 401 khi client/secret không đúng · 400 khi audience không được phép
   */
  public ServiceTokenResponse issue(ServiceTokenRequest request) {
    ServiceAuthProperties.Client client =
        properties.find(request.clientId()).orElseThrow(ServiceAuthService::badClient);
    requireSecretMatch(client.secret(), request.clientSecret());
    requireAllowedAudience(request.clientId(), client, request.audience());

    Instant now = Instant.now(clock);
    IssuedToken token =
        jwtService.issueServiceToken(
            request.clientId(), request.audience(), properties.serviceTokenTtl(), now);
    LOG.info(
        "cấp service token: client={} audience={} exp={}",
        request.clientId(),
        request.audience(),
        token.expiresAt());
    return ServiceTokenResponse.bearer(
        token.value(), token.expiresInSeconds(), request.audience(), request.clientId());
  }

  private static void requireSecretMatch(String expected, String actual) {
    boolean matches =
        MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    if (!matches) {
      throw badClient();
    }
  }

  private static void requireAllowedAudience(
      String clientId, ServiceAuthProperties.Client client, String audience) {
    if (!client.allowedAudiences().contains(audience)) {
      // Message nêu rõ audience xin gì + được phép gì: lỗi cấu hình phải đọc ra được ngay, không
      // phải đoán.
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "client "
              + clientId
              + " không được phép xin token cho audience '"
              + audience
              + "' (cho phép: "
              + client.allowedAudiences()
              + ")");
    }
  }

  private static AppException badClient() {
    return new AppException(ErrorCode.UNAUTHORIZED, "clientId hoặc clientSecret không đúng");
  }
}
