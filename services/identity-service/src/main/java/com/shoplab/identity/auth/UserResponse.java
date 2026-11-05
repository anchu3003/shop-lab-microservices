package com.shoplab.identity.auth;

import com.shoplab.identity.user.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Thông tin user trả ra API.
 *
 * <p>Cố ý là record riêng, <b>không</b> trả thẳng entity: entity có {@code passwordHash} — trả
 * entity là lộ hash (và mọi field nội bộ thêm sau này) mà không ai để ý.
 */
public record UserResponse(
    UUID id, String email, String displayName, String role, Instant createdAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.id(), user.email(), user.displayName(), user.role().name(), user.createdAt());
  }
}
