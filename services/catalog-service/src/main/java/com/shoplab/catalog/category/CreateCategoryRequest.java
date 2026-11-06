package com.shoplab.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body của {@code POST /v1/categories}.
 *
 * <p>Chỉ nhận {@code name}: {@code slug} do server sinh từ tên (chuẩn hoá chữ thường, thay khoảng
 * trắng bằng {@code -}). Không cho client tự đặt slug để tránh trùng/khó đọc.
 */
public record CreateCategoryRequest(
    @NotBlank(message = "name không được để trống")
        @Size(max = 120, message = "name tối đa 120 ký tự")
        String name) {}
