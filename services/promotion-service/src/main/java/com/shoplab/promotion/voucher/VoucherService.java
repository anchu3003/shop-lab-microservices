package com.shoplab.promotion.voucher;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ quản lý voucher (admin): tạo + xem. Việc giữ chỗ lượt nằm ở {@code VoucherLockService}.
 */
@Service
public class VoucherService {

  private final VoucherRepository vouchers;
  private final Clock clock;

  public VoucherService(VoucherRepository vouchers, Clock clock) {
    this.vouchers = vouchers;
    this.clock = clock;
  }

  /**
   * Tạo voucher.
   *
   * @throws DuplicateVoucherException khi mã đã tồn tại (409)
   * @throws AppException (400) khi rule nghiệp vụ sai: PERCENT > 100, {@code endAt <= startAt},
   *     {@code maxDiscount} dùng cho FIXED (mơ hồ: trần của một số tiền cố định là vô nghĩa)
   */
  @Transactional
  public VoucherResponse create(CreateVoucherRequest request) {
    String code = VoucherCode.normalize(request.code());
    BigDecimal minOrder = request.minOrder() == null ? BigDecimal.ZERO : request.minOrder();

    validateBusinessRules(request, code);

    if (vouchers.findByCode(code).isPresent()) {
      throw new DuplicateVoucherException(code);
    }

    Instant now = Instant.now(clock);
    VoucherRecord voucher =
        new VoucherRecord(
            UUID.randomUUID(),
            code,
            request.type(),
            request.value(),
            request.maxDiscount(),
            minOrder,
            request.quota(),
            0,
            0,
            request.startAt(),
            request.endAt(),
            now);
    vouchers.insert(voucher);
    return VoucherResponse.from(voucher);
  }

  /** Xem cấu hình + đếm lượt (admin) — dùng để verify/đối soát. */
  @Transactional(readOnly = true)
  public VoucherResponse get(String rawCode) {
    String code = VoucherCode.normalize(rawCode);
    return vouchers
        .findByCode(code)
        .map(VoucherResponse::from)
        .orElseThrow(() -> new NotFoundException("Voucher không tồn tại: " + code));
  }

  private void validateBusinessRules(CreateVoucherRequest request, String code) {
    if (!request.endAt().isAfter(request.startAt())) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR, "endAt phải sau startAt (voucher: " + code + ")");
    }
    if (request.type() == VoucherType.PERCENT) {
      if (request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
        throw new AppException(
            ErrorCode.VALIDATION_ERROR, "PERCENT value phải <= 100, nhận: " + request.value());
      }
    } else if (request.maxDiscount() != null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "maxDiscount chỉ dùng cho PERCENT — FIXED đã là số tiền cố định");
    }
  }
}
