package com.shoplab.promotion.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Body của {@code POST /v1/vouchers} (admin tạo voucher).
 *
 * <p>Validate ở đây chỉ là lớp <b>cú pháp</b>; rule nghiệp vụ (PERCENT <= 100, end > start,
 * maxDiscount chỉ cho PERCENT) nằm ở {@link VoucherService} vì cần đọc cùng lúc nhiều field.
 */
public record CreateVoucherRequest(
    @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "chỉ chữ, số, '_' và '-'")
        String code,
    @NotNull VoucherType type,
    @NotNull @DecimalMin(value = "0.01", message = "phải > 0") BigDecimal value,
    @DecimalMin(value = "0.00", message = "phải >= 0") BigDecimal maxDiscount,
    @DecimalMin(value = "0.00", message = "phải >= 0") BigDecimal minOrder,
    @NotNull @Min(1) Integer quota,
    @NotNull Instant startAt,
    @NotNull Instant endAt) {}
