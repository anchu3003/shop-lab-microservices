package com.shoplab.identity.auth;

/**
 * Phản hồi cấp service token (SL-3.11).
 *
 * <p>Không có {@code refreshToken} (khác {@link AuthTokenResponse}): service tự xin token mới khi
 * gần hết hạn — refresh cho máy-với-máy chỉ thêm trạng thái để rò rỉ (`client_credentials` chuẩn
 * cũng không phát refresh token).
 *
 * @param accessToken JWT dùng ở header {@code Authorization: Bearer …}
 * @param tokenType luôn {@code Bearer} (giữ field để client parse chung khuôn OAuth2)
 * @param expiresIn giây còn sống của token
 * @param audience service đích mà token này dùng được
 * @param serviceId service đã xin token (client id) — để log/audit phía gọi
 */
public record ServiceTokenResponse(
    String accessToken, String tokenType, long expiresIn, String audience, String serviceId) {

  public static ServiceTokenResponse bearer(
      String accessToken, long expiresIn, String audience, String serviceId) {
    return new ServiceTokenResponse(accessToken, "Bearer", expiresIn, audience, serviceId);
  }
}
