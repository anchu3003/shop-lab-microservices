package com.shoplab.identity.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Body của {@code POST /v1/auth/login}.
 *
 * <p>Không ràng buộc {@code @Size(min=8)} cho password ở đây: ràng buộc độ dài là việc của lúc đăng
 * ký. Nếu chặn ở login thì vô tình tiết lộ "mật khẩu ngắn thế này chắc chắn sai".
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
