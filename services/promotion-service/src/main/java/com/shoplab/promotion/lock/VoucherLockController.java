package com.shoplab.promotion.lock;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API giữ chỗ / chốt / trả lượt voucher — đường mà luồng checkout gọi (architecture.md §6.1 bước
 * 1).
 *
 * <p>Không hẹp quyền theo role: mọi user đã đăng nhập đều có thể dùng voucher (buyer mua hàng).
 * "Voucher của ai" không phải khái niệm của Phase 2 — voucher là mã công khai.
 */
@Tag(name = "VoucherLock")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/vouchers")
public class VoucherLockController {

  private final VoucherLockService service;

  public VoucherLockController(VoucherLockService service) {
    this.service = service;
  }

  /**
   * Giữ 1 lượt → <b>201</b> khi tạo mới, <b>200</b> khi replay cùng {@code orderId}.
   *
   * <p>Phân biệt 2 mã để client retry (timeout mạng) biết "đã giữ rồi" — cùng body, khác status là
   * tín hiệu rẻ nhất.
   */
  @PostMapping("/lock")
  public ResponseEntity<VoucherLockResponse> lock(@Valid @RequestBody LockVoucherRequest request) {
    VoucherLockService.LockOutcome outcome = service.lock(request);
    return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(outcome.response());
  }

  /** Chốt dùng thật → 200 · 410 nếu quá TTL · 409 nếu đã release. */
  @PostMapping("/commit")
  public VoucherLockResponse commit(@Valid @RequestBody VoucherLockRefRequest request) {
    return service.commit(request.lockId());
  }

  /** Trả lượt (huỷ đơn / thanh toán fail) → 200, idempotent. */
  @PostMapping("/release")
  public VoucherLockResponse release(@Valid @RequestBody VoucherLockRefRequest request) {
    return service.release(request.lockId());
  }
}
