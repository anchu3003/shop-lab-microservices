package com.shoplab.catalog.sku;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body của {@code POST /v1/products/{productId}/skus}.
 *
 * <p>{@code priceOverride} để trống = dùng giá product. {@code stockHint} chỉ là gợi ý hiển thị.
 */
public record CreateSkuRequest(
    @NotBlank(message = "code không được để trống")
        @Size(max = 64, message = "code tối đa 64 ký tự")
        String code,
    @DecimalMin(value = "0.00", message = "priceOverride không được âm")
        @Digits(
            integer = 10,
            fraction = 2,
            message = "priceOverride tối đa 10 chữ số phần nguyên, 2 phần thập phân")
        BigDecimal priceOverride,
    @Min(value = 0, message = "stockHint không được âm") Integer stockHint) {}
