package com.shoplab.promotion.voucher;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API quản lý voucher (admin).
 *
 * <p>Vì sao chỉ admin: voucher là <b>tiền</b> — seller tự tạo mã giảm giá của mình thì phải có khái
 * niệm "voucher thuộc seller" (Phase 2 chưa có, giống lý do inventory không cho seller sửa tồn
 * kho).
 */
@Tag(name = "Voucher")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/vouchers")
public class VoucherController {

  private final VoucherService service;

  public VoucherController(VoucherService service) {
    this.service = service;
  }

  /** Tạo voucher (admin) → 201 + {@code Location}. */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<VoucherResponse> create(@Valid @RequestBody CreateVoucherRequest request) {
    VoucherResponse created = service.create(request);
    return ResponseEntity.created(URI.create("/v1/vouchers/" + created.code())).body(created);
  }

  /** Xem cấu hình + đếm lượt (admin). */
  @GetMapping("/{code}")
  @PreAuthorize("hasRole('ADMIN')")
  public VoucherResponse get(@PathVariable String code) {
    return service.get(code);
  }
}
