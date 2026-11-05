package com.shoplab.identity.auth;

import com.shoplab.identity.auth.jwt.IssuedToken;

/**
 * Response của login/refresh.
 *
 * @param accessToken chuỗi JWT
 * @param tokenType luôn là {@code Bearer} (theo RFC 6750)
 * @param expiresIn số <b>giây</b> token còn hiệu lực — client dùng để biết khi nào cần refresh
 */
public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn) {

  public static AuthTokenResponse bearer(IssuedToken token) {
    return new AuthTokenResponse(token.value(), "Bearer", token.expiresInSeconds());
  }
}
