package com.shoplab.identity.auth;

import jakarta.validation.constraints.NotBlank;

/** Body của {@code POST /v1/auth/refresh}. */
public record RefreshTokenRequest(
    @NotBlank(message = "refreshToken không được để trống") String refreshToken) {}
