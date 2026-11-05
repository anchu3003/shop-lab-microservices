package com.shoplab.identity.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Body của {@code POST /v1/auth/logout}.
 *
 * <p>Cố ý <b>không</b> nhận access token để revoke: access token là JWT stateless (không có DB để
 * tra), sống tối đa 15 phút. Thu hồi tức thời access token cần token version/blacklist — đánh đổi
 * lớn, đã ghi ở {@code Backlog.md → SL-1.4 → Revisit}.
 */
public record LogoutRequest(
    @NotBlank(message = "refreshToken không được để trống") String refreshToken) {}
