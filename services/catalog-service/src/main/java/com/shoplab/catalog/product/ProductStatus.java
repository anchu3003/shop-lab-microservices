package com.shoplab.catalog.product;

/**
 * Vòng đời hiển thị của một product (SL-2.11, ADR-0006 D5).
 *
 * <p>Lưu ở DB dạng chuỗi ({@code @Enumerated(STRING)}) — đọc DB bằng mắt được và thêm giá trị mới
 * (vd {@code DRAFT} khi có luồng duyệt seller) không làm lệch dữ liệu cũ như ordinal.
 */
public enum ProductStatus {
  /** Còn bán, hiện trong list/get. */
  ACTIVE,

  /** Đã xoá mềm: ẩn khỏi API nhưng row vẫn còn để tham chiếu/audit. */
  ARCHIVED
}
