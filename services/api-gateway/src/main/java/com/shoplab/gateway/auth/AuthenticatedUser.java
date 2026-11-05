package com.shoplab.gateway.auth;

/**
 * Danh tính đã verify xong, lấy từ token.
 *
 * @param userId giá trị claim {@code sub} (UUID của user trong identity-service)
 * @param role giá trị claim {@code role} ({@code BUYER} / {@code SELLER} / {@code ADMIN})
 */
public record AuthenticatedUser(String userId, String role) {}
