package com.shoplab.identity.admin;

import com.shoplab.identity.auth.UserResponse;
import com.shoplab.identity.user.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API quản trị (SL-1.4) — tồn tại để <b>chứng minh phân quyền có tác dụng thật</b>.
 *
 * <p>Không có endpoint cần quyền thì RBAC chỉ là "có field role trong token": không ai biết nó có
 * chặn đúng hay không. Endpoint này cho phép kiểm chứng cặp 200 (admin) / 403 (buyer).
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminUserController {

  private final UserRepository users;

  public AdminUserController(UserRepository users) {
    this.users = users;
  }

  /**
   * Danh sách user — <b>chỉ</b> {@code ROLE_ADMIN}.
   *
   * <p>Buyer gọi → 403 (đã xác thực nhưng không đủ quyền), khác hẳn 401 (chưa xác thực). Phân biệt
   * này là quy ước ở {@code ErrorCode}.
   */
  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserResponse> list() {
    return users.findAll().stream().map(UserResponse::from).toList();
  }
}
