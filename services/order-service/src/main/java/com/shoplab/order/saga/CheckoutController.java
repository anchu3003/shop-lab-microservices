package com.shoplab.order.saga;

import com.shoplab.order.order.ChangeStatusRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API checkout (saga orchestration, `SL-2.6a`).
 *
 * <p>Mọi endpoint cần token: `userId` lấy từ JWT `sub`, **không** nhận từ body (nhận từ client thì
 * ai cũng checkout được dưới danh nghĩa người khác). Token của chính user này cũng được **forward**
 * cho `cart`/`catalog`/`inventory`/`promotion`/`payment` — chúng verify lại độc lập (defense in
 * depth).
 */
@Tag(name = "Checkout")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/checkout")
public class CheckoutController {

  private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

  private final CheckoutService service;
  private final CheckoutFinishService finish;

  public CheckoutController(CheckoutService service, CheckoutFinishService finish) {
    this.service = service;
    this.finish = finish;
  }

  /**
   * Đặt hàng từ giỏ hiện tại → **201** (lần đầu) / **200** (replay cùng `Idempotency-Key`).
   *
   * <p>Body **không** chứa dòng hàng: dòng hàng lấy từ giỏ ở `cart-service` (một nguồn sự thật).
   */
  @PostMapping
  public ResponseEntity<CheckoutResponse> checkout(
      @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
      @RequestBody(required = false) @Valid CheckoutRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    CheckoutRequest body = request == null ? new CheckoutRequest(null) : request;
    CheckoutService.CheckoutResult result =
        service.checkout(UUID.fromString(jwt.getSubject()), bearer(jwt), idempotencyKey, body);
    ResponseEntity.BodyBuilder builder =
        result.replayed()
            ? ResponseEntity.ok().header("Idempotent-Replayed", "true")
            : ResponseEntity.created(URI.create("/v1/checkout/" + result.response().orderId()));
    return builder.body(result.response());
  }

  /** Trạng thái saga của một đơn (chủ sở hữu hoặc admin; người khác nhận 404). */
  @GetMapping("/{orderId}")
  public SagaStatusResponse status(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
    return service.status(orderId, UUID.fromString(jwt.getSubject()), isAdmin(jwt));
  }

  /**
   * Chốt đơn sau khi thanh toán → **200** `state=CONFIRMED` (idempotent: gọi lại trả 200).
   *
   * <p>Saga **tự** hỏi `payment-service` xem intent đã `SUCCEEDED` chưa — client nói "tôi trả rồi"
   * không đủ.
   */
  @PostMapping("/{orderId}/confirm")
  public SagaStatusResponse confirm(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
    return finish.confirm(
        orderId, UUID.fromString(jwt.getSubject()), isAdmin(jwt), jwt.getTokenValue());
  }

  /** Huỷ đơn còn chờ thanh toán → 200 `state=CANCELLED`; huỷ đơn đã `CONFIRMED` → **409**. */
  @PostMapping("/{orderId}/cancel")
  public SagaStatusResponse cancel(
      @PathVariable UUID orderId,
      @RequestBody(required = false) ChangeStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = request == null ? null : request.reason();
    return finish.cancel(
        orderId, UUID.fromString(jwt.getSubject()), isAdmin(jwt), jwt.getTokenValue(), reason);
  }

  /**
   * Token thô để forward. {@code getTokenValue()} là chuỗi đã verify — dùng lại chính token đó cho
   * service đích thay vì tự ký token mới (Phase 2 chưa có service-to-service auth).
   */
  private static String bearer(Jwt jwt) {
    return jwt.getTokenValue();
  }

  private static boolean isAdmin(Jwt jwt) {
    return "ADMIN".equals(jwt.getClaimAsString("role"));
  }
}
