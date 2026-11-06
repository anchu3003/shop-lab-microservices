package com.shoplab.catalog.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body của {@code PUT /v1/products/{id}} — PUT là thay toàn bộ nên các field sửa được đều bắt buộc
 * (khác PATCH: chỉ gửi field cần đổi).
 *
 * <p>{@code categoryId} không nằm ở đây: đổi category của sản phẩm là nghiệp vụ riêng, chưa cần ở
 * Phase 1.
 */
public record UpdateProductRequest(
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
