package com.shoplab.identity.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body của {@code POST /v1/auth/register}.
 *
 * <p>{@code password} tối đa <b>72 ký tự</b> không phải con số tuỳ ý: BCrypt chỉ dùng 72 byte đầu
 * tiên, phần dư bị bỏ qua im lặng — cho phép dài hơn là tạo cảm giác an toàn giả.
 */
public record RegisterRequest(
    @NotBlank(message = "email không được để trống")
        @Email(message = "email không hợp lệ")
        @Size(max = 255, message = "email tối đa 255 ký tự")
        String email,
    @NotBlank(message = "password không được để trống")
        @Size(min = 8, max = 72, message = "password phải từ 8 đến 72 ký tự")
        String password,
    @NotBlank(message = "displayName không được để trống")
        @Size(max = 120, message = "displayName tối đa 120 ký tự")
        String displayName) {}
