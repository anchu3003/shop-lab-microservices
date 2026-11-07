package com.shoplab.promotion.lock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body của {@code POST /v1/vouchers/lock}.
 *
 * <p>{@code orderId} là <b>khoá idempotency</b>: retry cùng orderId trả lại đúng lock cũ thay vì
 * giữ lượt lần 2.
 *
 * <p>{@code subtotal} do order-service gửi (call nội bộ, đã tính từ snapshot giá) — promotion
 * <b>không</b> đọc lại cart. Xem ghi chú đánh đổi trong ticket: nếu sau này endpoint lộ ra client
 * thì phải tính subtotal server-side.
 */
public record LockVoucherRequest(
    @NotBlank String code,
    @NotNull UUID orderId,
    @NotNull @DecimalMin(value = "0.00", message = "phải >= 0") BigDecimal subtotal) {}
