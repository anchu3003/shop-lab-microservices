package com.shoplab.identity.user;

import java.util.UUID;

/**
 * Danh tính của người đang gọi API, do <b>gateway</b> xác thực và truyền xuống qua header.
 *
 * @param userId id user (claim {@code sub} của token)
 * @param role vai trò (claim {@code role})
 */
public record CurrentUserResponse(UUID userId, String role) {}
