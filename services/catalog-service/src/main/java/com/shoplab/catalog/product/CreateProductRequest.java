package com.shoplab.catalog.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body của {@code POST /v1/products}.
 *
 * <p>{@code price}: {@code @DecimalMin("0.00")} chặn giá âm ngay ở tầng API (400 + tên field),
 * {@code @Digits(integer=10, fraction=2)} khớp đúng cột {@code NUMERIC(12,2)} — nếu lệch thì DB mới
 * báo lỗi, muộn và khó hiểu.
 */
public record CreateProductRequest(
    @NotNull(message = "categoryId không được để trống") UUID categoryId,
    @NotBlank(message = "name không được để trống")
        @Size(max = 200, message = "name tối đa 200 ký tự")
        String name,
    @Size(max = 2000, message = "description tối đa 2000 ký tự") String description,
    @NotNull(message = "price không được để trống")
        @DecimalMin(value = "0.00", message = "price không được âm")
        @Digits(
            integer = 10,
            fraction = 2,
            message = "price tối đa 10 chữ số phần nguyên, 2 phần thập phân")
        BigDecimal price) {}
