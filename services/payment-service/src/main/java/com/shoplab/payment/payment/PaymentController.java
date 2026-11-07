package com.shoplab.payment.payment;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API payment intent.
 *
 * <p>Mọi endpoint ở đây cần token; {@code userId} lấy từ JWT {@code sub} — không nhận từ body (nhận
 * từ client thì ai cũng tạo được intent dưới danh nghĩa người khác).
 *
 * <p>{@code POST} bắt buộc header {@code Idempotency-Key}: retry cùng key + cùng payload trả lại
 * đúng response cũ (200 + {@code Idempotent-Replayed: true}).
 */
@Tag(name = "Payment")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/payment-intents")
public class PaymentController {

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  /** Tạo intent → 201; replay (hoặc đơn đã có intent cùng số tiền) → 200 + header. */
  @PostMapping
  public ResponseEntity<PaymentIntentResponse> create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreatePaymentIntentRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    PaymentService.CreateResult result = service.create(userId(jwt), idempotencyKey, request);
    ResponseEntity.BodyBuilder builder =
        result.replayed()
            ? ResponseEntity.ok().header("Idempotent-Replayed", "true")
            : ResponseEntity.created(URI.create("/v1/payment-intents/" + result.response().id()));
    return builder.body(result.response());
  }

  /** Xem một intent (chủ sở hữu hoặc admin). */
  @GetMapping("/{id}")
  public PaymentIntentResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.get(id, userId(jwt), isAdmin(jwt));
  }

  /** Intent của một đơn — buyer chỉ thấy của mình. */
  @GetMapping
  public List<PaymentIntentResponse> listByOrder(
      @RequestParam UUID orderId, @AuthenticationPrincipal Jwt jwt) {
    return service.listByOrder(orderId, userId(jwt), isAdmin(jwt));
  }

  private static UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  private static boolean isAdmin(Jwt jwt) {
    return "ADMIN".equals(jwt.getClaimAsString("role"));
  }
}
