package com.shoplab.gateway.auth;

/**
 * Header danh tính do <b>gateway</b> gắn vào request đi xuống service.
 *
 * <p>Đây là "hợp đồng" giữa gateway và service: service đọc 2 header này thay vì tự verify token
 * (Phase 1). SL-1.5 sẽ thêm verify JWT tại service (defense in depth) — lúc đó 2 header này vẫn
 * dùng, nhưng không còn là nguồn tin duy nhất.
 *
 * <p>⚠️ Giá trị do client gửi cho 2 header này <b>luôn bị ghi đè</b> (xem {@link
 * UserHeaderRequest}).
 */
public final class UserHeaders {

  public static final String USER_ID = "x-user-id";
  public static final String USER_ROLE = "x-user-role";

  /** Có phải header danh tính do gateway quản lý không (không phân biệt hoa/thường). */
  public static boolean isManaged(String headerName) {
    return USER_ID.equalsIgnoreCase(headerName) || USER_ROLE.equalsIgnoreCase(headerName);
  }

  private UserHeaders() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
