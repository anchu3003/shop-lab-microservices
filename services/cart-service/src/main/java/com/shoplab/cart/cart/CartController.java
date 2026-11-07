package com.shoplab.cart.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API giỏ hàng.
 *
 * <p>Hai kiểu danh tính trên cùng một bộ endpoint: {@code Authorization: Bearer} (user) hoặc {@code
 * X-Cart-Token} (guest). Gateway để prefix này public vì guest không có JWT — quyết định "ai được
 * đọc giỏ nào" nằm ở {@link OwnerResolver}.
 */
@Tag(name = "Cart")
@RestController
@RequestMapping("/v1/carts")
public class CartController {

  private static final String GUEST_TOKEN_HEADER = "X-Cart-Token";

  private final CartService service;
  private final OwnerResolver owners;

  public CartController(CartService service, OwnerResolver owners) {
    this.service = service;
    this.owners = owners;
  }

  /** Cấp guest token — endpoint public duy nhất tạo state mới. */
  @Operation(summary = "Cấp guest cart token (khách chưa đăng nhập)")
  @PostMapping("/guest")
  public ResponseEntity<GuestCartResponse> guest() {
    CartService.GuestCart guest = service.issueGuestCart();
    return ResponseEntity.created(URI.create("/v1/carts"))
        .body(new GuestCartResponse(guest.cartToken(), guest.ttl().toSeconds()));
  }

  @Operation(summary = "Xem giỏ hiện tại")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping
  public CartResponse get(
      @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @AuthenticationPrincipal Jwt jwt) {
    return CartResponse.from(service.get(owners.resolve(jwt, guestToken)));
  }

  @Operation(summary = "Đặt số lượng cho một dòng (idempotent)")
  @SecurityRequirement(name = "bearerAuth")
  @PutMapping("/items/{skuId}")
  public CartResponse setItem(
      @PathVariable UUID skuId,
      @Valid @RequestBody SetItemRequest request,
      @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @AuthenticationPrincipal Jwt jwt) {
    return CartResponse.from(
        service.setItem(owners.resolve(jwt, guestToken), skuId, request.qty()));
  }

  @Operation(summary = "Xoá một dòng khỏi giỏ")
  @SecurityRequirement(name = "bearerAuth")
  @DeleteMapping("/items/{skuId}")
  public CartResponse removeItem(
      @PathVariable UUID skuId,
      @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @AuthenticationPrincipal Jwt jwt) {
    return CartResponse.from(service.removeItem(owners.resolve(jwt, guestToken), skuId));
  }

  @Operation(summary = "Xoá toàn bộ giỏ")
  @SecurityRequirement(name = "bearerAuth")
  @DeleteMapping
  public CartResponse clear(
      @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @AuthenticationPrincipal Jwt jwt) {
    return CartResponse.from(service.clear(owners.resolve(jwt, guestToken)));
  }

  /** Merge giỏ guest vào giỏ user — client gọi ngay sau khi login. */
  @Operation(
      summary = "Merge giỏ guest vào giỏ user (gọi sau khi login)",
      description =
          "Cộng dồn qty theo SKU, cap ở maxQty; xoá giỏ guest sau khi merge nên gọi lại không cộng lần nữa.")
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping("/merge")
  public CartResponse merge(
      @RequestHeader(value = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      throw new CartExceptions.Unauthenticated("merge cần đăng nhập (JWT)");
    }
    return CartResponse.from(
        service.mergeGuestIntoUser(UUID.fromString(jwt.getSubject()), guestToken));
  }

  /** Response riêng cho guest token (không phải giỏ). */
  public record GuestCartResponse(String cartToken, long expiresInSeconds) {}
}
