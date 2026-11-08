package com.shoplab.commons.serviceauth;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * Luật kiểm token ở **service đích** (SL-3.11): {@code iss}/{@code exp} như cũ, cộng thêm **{@code
 * aud}** cho service token.
 *
 * <p><b>Vì sao chỉ kiểm {@code aud} khi {@code role=SERVICE}</b>: token người dùng hiện **không**
 * có claim {@code aud} (hợp đồng token từ SL-1.4). Bắt token người phải có `aud` sẽ làm hỏng cả hệ
 * (8 service) trong một thay đổi — việc đó là ticket riêng khi cần. Đổi lại, phải hiểu rõ giới hạn
 * của cách làm này: một service token bị lộ thì dùng được ở mọi service **có audience tương ứng**,
 * và token người vẫn được chấp nhận ở mọi service (như trước).
 *
 * <p><b>Sai audience trả 403, không phải 401</b>: 401 = "chưa biết bạn là ai", 403 = "biết bạn là
 * ai nhưng không được vào đây" (`architecture.md` §6.2). Token hợp lệ nhưng phát cho service khác
 * là ca thứ hai — và phân biệt được hai ca này là điều kiện để debug nhanh khi tích hợp.
 */
public final class ServiceTokenRules {

  /**
   * Tên claim role — trùng {@code JwtService.ROLE_CLAIM} của identity (commons không phụ thuộc
   * identity).
   */
  public static final String ROLE_CLAIM = "role";

  /** Giá trị role của token máy. */
  public static final String SERVICE_ROLE = "SERVICE";

  private static final String INVALID_AUDIENCE = "invalid_token";

  /**
   * Validator dùng trong {@code NimbusJwtDecoder#setJwtValidator}: issuer + (aud nếu là service
   * token).
   *
   * @param issuer giá trị {@code iss} mong đợi (cấu hình {@code shoplab.jwt.issuer})
   * @param serviceName audience của **chính service này** (vd {@code inventory-service}) — thường
   *     là {@code spring.application.name}
   */
  public static OAuth2TokenValidator<Jwt> validator(String issuer, String serviceName) {
    OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
    return token -> {
      OAuth2TokenValidatorResult base = issuerValidator.validate(token);
      if (base.hasErrors()) {
        return base;
      }
      if (!SERVICE_ROLE.equals(token.getClaimAsString(ROLE_CLAIM))) {
        // Token người: giữ nguyên hành vi cũ (không yêu cầu aud).
        return OAuth2TokenValidatorResult.success();
      }
      List<String> audiences = token.getAudience();
      if (audiences != null && audiences.contains(serviceName)) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(
              INVALID_AUDIENCE,
              "service token có aud="
                  + audiences
                  + " nhưng service này là '"
                  + serviceName
                  + "' — xin token với audience đúng",
              null));
    };
  }

  private ServiceTokenRules() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
