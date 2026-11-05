package com.shoplab.identity.user;

/**
 * Vai trò của người dùng (RBAC cơ bản).
 *
 * <p>Lưu trong DB dạng <b>STRING</b> (xem {@code V1__create_users.sql}) — không dùng ordinal của
 * enum, vì thêm/đổi thứ tự hằng số sẽ làm hỏng dữ liệu đã ghi.
 *
 * <p>{@code SL-1.1} chỉ dùng role ở mức "có trong token"; endpoint phân quyền thật đến ở {@code
 * SL-1.4} và {@code SL-1.5}.
 */
public enum Role {

  /** Người mua — mặc định khi tự đăng ký. */
  BUYER,

  /** Người bán — được ghi vào catalog (SL-1.5); tạo qua seed, không tự đăng ký được. */
  SELLER,

  /** Quản trị — xem/quản lý user. */
  ADMIN
}
