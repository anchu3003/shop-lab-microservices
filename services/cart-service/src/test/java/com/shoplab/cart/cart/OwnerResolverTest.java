package com.shoplab.cart.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoplab.cart.config.CartProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Test "ai được đọc giỏ nào" — chỗ dễ sai nhất của cart (endpoint public + hai loại danh tính).
 *
 * <p>Luật: JWT thắng guest token; không JWT thì guest token <b>bắt buộc do server cấp</b>.
 */
class OwnerResolverTest {

  private final CartStore store = mock(CartStore.class);
  private final CartService service =
      new CartService(
          store,
          new CartProperties(Duration.ofDays(30), 50, 999, "cart"),
          Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC));
  private final OwnerResolver resolver = new OwnerResolver(service);

  private static Jwt jwt(UUID userId) {
    return Jwt.withTokenValue("t")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("role", "BUYER")
        .build();
  }

  @Test
  @DisplayName("có JWT → giỏ user, kể cả khi client còn gửi kèm X-Cart-Token cũ")
  void jwtWinsOverGuestToken() {
    UUID userId = UUID.randomUUID();

    CartOwner owner = resolver.resolve(jwt(userId), "token-cu-con-sot");

    assertThat(owner.isGuest()).isFalse();
    assertThat(owner.scope()).isEqualTo("u:" + userId);
  }

  @Test
  @DisplayName("không JWT + guest token hợp lệ → giỏ guest")
  void guestTokenResolvesGuestCart() {
    when(store.isIssuedGuestToken("tok-hop-le")).thenReturn(true);

    CartOwner owner = resolver.resolve(null, "tok-hop-le");

    assertThat(owner.isGuest()).isTrue();
    assertThat(owner.scope()).isEqualTo("g:tok-hop-le");
  }

  @Test
  @DisplayName("không JWT + không token, hoặc token bịa → 401")
  void missingOrForgedTokenIs401() {
    when(store.isIssuedGuestToken("tok-bia")).thenReturn(false);

    assertThatThrownBy(() -> resolver.resolve(null, null))
        .isInstanceOf(CartExceptions.Unauthenticated.class);
    assertThatThrownBy(() -> resolver.resolve(null, "tok-bia"))
        .isInstanceOf(CartExceptions.Unauthenticated.class);
  }

  @Test
  @DisplayName("guest token trùng chuỗi với userId vẫn không đọc được giỏ user (khác tiền tố)")
  void guestScopeIsNamespaced() {
    UUID userId = UUID.randomUUID();
    assertThat(CartOwner.guest(userId.toString()).scope())
        .isNotEqualTo(CartOwner.user(userId).scope());
  }
}
