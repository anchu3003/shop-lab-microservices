package com.shoplab.cart.cart;

import java.util.UUID;

/**
 * Chủ sở hữu một giỏ hàng — hoặc user đã đăng nhập, hoặc khách vãng lai có guest token.
 *
 * <p>{@code redisId()} sinh ra phần đuôi của key Redis ({@code cart:u:<uuid>} / {@code
 * cart:g:<token>}). Hai loại tách bằng tiền tố để một guest token trùng chuỗi với userId cũng không
 * bao giờ đọc được giỏ của user.
 */
public record CartOwner(Kind kind, String token) {

  public enum Kind {
    USER,
    GUEST
  }

  public static CartOwner user(UUID userId) {
    return new CartOwner(Kind.USER, userId.toString());
  }

  public static CartOwner guest(String guestToken) {
    return new CartOwner(Kind.GUEST, guestToken);
  }

  /** Phần đuôi key: {@code u:<userId>} hoặc {@code g:<guestToken>}. */
  public String scope() {
    return (kind == Kind.USER ? "u:" : "g:") + token;
  }

  public boolean isGuest() {
    return kind == Kind.GUEST;
  }
}
