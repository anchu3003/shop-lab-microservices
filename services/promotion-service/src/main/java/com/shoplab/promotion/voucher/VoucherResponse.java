package com.shoplab.promotion.voucher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Voucher trả ra ngoài (admin xem cấu hình + đếm lượt). */
public record VoucherResponse(
    UUID id,
    String code,
    VoucherType type,
    BigDecimal value,
    BigDecimal maxDiscount,
    BigDecimal minOrder,
    int quota,
    int used,
    int locked,
    int available,
    Instant startAt,
    Instant endAt) {

  public static VoucherResponse from(VoucherRecord voucher) {
    return new VoucherResponse(
        voucher.id(),
        voucher.code(),
        voucher.type(),
        voucher.value(),
        voucher.maxDiscount(),
        voucher.minOrder(),
        voucher.quota(),
        voucher.used(),
        voucher.locked(),
        voucher.available(),
        voucher.startAt(),
        voucher.endAt());
  }
}
