package com.shoplab.identity.auth;

import com.shoplab.identity.auth.jwt.IssuedToken;

/**
 * Kết quả đăng nhập/refresh: cặp token đầy đủ (SL-1.4).
 *
 * @param accessToken JWT ngắn hạn (15 phút) — stateless, gateway/service verify bằng public key
 * @param refreshToken chuỗi opaque 256-bit — đổi lấy cặp mới qua {@code POST /v1/auth/refresh}
 */
public record AuthSession(IssuedToken accessToken, String refreshToken) {}
