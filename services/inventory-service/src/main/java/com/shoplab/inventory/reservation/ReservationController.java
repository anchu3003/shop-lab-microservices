package com.shoplab.inventory.reservation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API giữ chỗ tồn kho — đường mà luồng checkout gọi (architecture.md §6.1).
 *
 * <p>Không có {@code @PreAuthorize} hẹp: mọi user đã đăng nhập đều được reserve (buyer mua hàng).
 * Việc "đơn này có phải của user này không" thuộc trách nhiệm của {@code order-service} — inventory
 * chỉ biết {@code orderId}, cố ý không kiểm chủ sở hữu (nếu không sẽ phải gọi ngược sang order, tạo
 * phụ thuộc vòng).
 */
@Tag(name = "Reservation")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/stock")
public class ReservationController {

  private final ReservationService service;

  public ReservationController(ReservationService service) {
    this.service = service;
  }

  /**
   * Giữ chỗ → <b>201</b> khi tạo mới, <b>200</b> khi đây là replay của cùng {@code orderId}.
   *
   * <p>Vì sao phân biệt 2 mã: client retry (timeout mạng) cần biết "đã giữ chỗ rồi" để không tạo
   * đơn mới; cùng body nhưng status khác là tín hiệu rẻ nhất cho việc đó.
   */
  @PostMapping("/reserve")
  public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest request) {
    List<ReservationItemRecord> items =
        request.items().stream()
            .map(item -> new ReservationItemRecord(item.skuId(), item.qty()))
            .toList();

    ReservationService.ReserveOutcome outcome = service.reserve(request.orderId(), items);
    ReservationResponse body = ReservationResponse.from(outcome.reservation(), outcome.items());
    return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK).body(body);
  }

  /** Chốt hàng (tiền đã về) → 200 · 410 nếu hết hạn · 409 nếu đã release. */
  @PostMapping("/commit")
  public ReservationResponse commit(@Valid @RequestBody ReservationRefRequest request) {
    ReservationRecord committed = service.commit(request.reservationId());
    return ReservationResponse.from(committed, service.itemsOf(committed.id()));
  }

  /** Trả chỗ (huỷ đơn / thanh toán fail) → 200, idempotent. */
  @PostMapping("/release")
  public ReservationResponse release(@Valid @RequestBody ReservationRefRequest request) {
    ReservationRecord released = service.release(request.reservationId());
    return ReservationResponse.from(released, service.itemsOf(released.id()));
  }

  /** Id của reservation — dùng chung cho commit/release. */
  public record ReservationRefRequest(@NotNull UUID reservationId) {}
}
