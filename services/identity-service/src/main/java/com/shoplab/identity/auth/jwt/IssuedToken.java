package com.shoplab.identity.auth.jwt;

import java.time.Instant;

/**
 * Access token vừa phát hành.
 *
 * @param value chuỗi JWT (đã ký RS256)
 * @param expiresAt thời điểm hết hiệu lực (UTC)
 * @param expiresInSeconds số giây còn hiệu lực — để trả cho client dạng {@code expiresIn}
 */
public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {}
