package com.shoplab.identity.auth;

/**
 * Response của login/refresh.
 *
 * @param accessToken chuỗi JWT ngắn hạn
 * @param refreshToken chuỗi opaque để đổi cặp token mới (SL-1.4) — CHỈ trả qua HTTPS ở môi trường
 *     thật, không log ra
 * @param tokenType luôn là {@code Bearer} (theo RFC 6750)
 * @param expiresIn số <b>giây</b> access token còn hiệu lực — client dùng để biết khi nào cần
 *     refresh
 */
public record AuthTokenResponse(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {

  public static AuthTokenResponse bearer(AuthSession session) {
    return new AuthTokenResponse(
        session.accessToken().value(),
        session.refreshToken(),
        "Bearer",
        session.accessToken().expiresInSeconds());
  }
}
