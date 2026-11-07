package com.shoplab.cart.cart;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Xác định chủ giỏ từ request.
 *
 * <p>Thứ tự ưu tiên: JWT trước (user đã đăng nhập luôn dùng giỏ của mình, kể cả khi client còn gửi
 * kèm {@code X-Cart-Token} cũ — không có chuyện đang đăng nhập mà lại ghi vào giỏ guest), sau đó
 * mới tới guest token.
 *
 * <p>Guest token <b>phải do server cấp</b> (kiểm bằng key đánh dấu trong Redis) ⇒ token bịa hoặc đã
 * bị thu hồi sau merge đều 401. Nếu client tự sinh token được thì ai giữ token cũ đều đọc được giỏ
 * người khác và không có cách thu hồi.
 */
@Component
public class OwnerResolver {

  private final CartService cartService;

  public OwnerResolver(CartService cartService) {
    this.cartService = cartService;
  }

  public CartOwner resolve(Jwt jwt, String guestToken) {
    if (jwt != null && jwt.getSubject() != null) {
      return CartOwner.user(UUID.fromString(jwt.getSubject()));
    }
    cartService.requireIssuedGuestToken(guestToken);
    return CartOwner.guest(guestToken.trim());
  }
}
