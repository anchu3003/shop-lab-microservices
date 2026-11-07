package com.shoplab.order.order;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API đơn hàng.
 *
 * <p>Mọi endpoint cần token. {@code userId} lấy từ JWT {@code sub} — <b>không</b> nhận từ
 * body/query (nhận từ client thì ai cũng tạo được đơn dưới danh nghĩa người khác).
 *
 * <p>{@code POST /v1/orders} bắt buộc header {@code Idempotency-Key}; retry cùng key + cùng payload
 * trả lại **đúng response cũ** (200 + header {@code Idempotent-Replayed: true}) để client biết "đơn
 * đã tạo rồi".
 */
@Tag(name = "Order")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  /** Tạo đơn → 201; replay → 200 + {@code Idempotent-Replayed: true}. */
  @PostMapping
  public ResponseEntity<OrderResponse> create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    OrderService.CreateResult result = service.create(userId(jwt), idempotencyKey, request);
    ResponseEntity.BodyBuilder builder =
        result.replayed()
            ? ResponseEntity.ok().header("Idempotent-Replayed", "true")
            : ResponseEntity.created(URI.create("/v1/orders/" + result.response().id()));
    return builder.body(result.response());
  }

  /** Xem một đơn (chủ sở hữu hoặc admin). */
  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.get(id, userId(jwt), isAdmin(jwt));
  }

  /** Danh sách đơn (buyer: của mình; admin: tất cả). */
  @GetMapping
  public OrderPage list(
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String size,
      @AuthenticationPrincipal Jwt jwt) {
    return service.list(userId(jwt), isAdmin(jwt), page, size);
  }

  /** Xác nhận đã thanh toán: PENDING_PAYMENT → CONFIRMED. */
  @PostMapping("/{id}/confirm")
  public OrderResponse confirm(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.changeStatus(
        id, OrderStatus.CONFIRMED, "payment confirmed", userId(jwt), isAdmin(jwt));
  }

  /** Huỷ đơn: PENDING_PAYMENT → CANCELLED. */
  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(
      @PathVariable UUID id,
      @RequestBody(required = false) ChangeStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = request == null ? null : request.reason();
    return service.changeStatus(id, OrderStatus.CANCELLED, reason, userId(jwt), isAdmin(jwt));
  }

  /**
   * Đánh dấu hết hạn: PENDING_PAYMENT → EXPIRED.
   *
   * <p>Ticket này chỉ mở đường chuyển trạng thái — việc <b>gọi</b> nó kèm release stock/voucher
   * thuộc saga (`SL-2.6`) và các nhánh fail (`SL-2.9`), vì order-service không tự gọi service khác
   * trong Phase 2.
   */
  @PostMapping("/{id}/expire")
  public OrderResponse expire(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.changeStatus(
        id, OrderStatus.EXPIRED, "payment window expired", userId(jwt), isAdmin(jwt));
  }

  private static UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  private static boolean isAdmin(Jwt jwt) {
    return "ADMIN".equals(jwt.getClaimAsString("role"));
  }
}
