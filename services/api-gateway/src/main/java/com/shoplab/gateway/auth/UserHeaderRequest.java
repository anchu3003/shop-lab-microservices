package com.shoplab.gateway.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Bọc request để <b>kiểm soát tuyệt đối</b> 2 header danh tính {@code x-user-id} / {@code
 * x-user-role}.
 *
 * <p>Vì sao cần bọc thay vì chỉ {@code setHeader} khi forward:
 *
 * <ul>
 *   <li>Header do client gửi phải bị <b>xoá</b>, không chỉ bị ghi đè "ở chỗ ta nhớ ra" — nếu
 *       service đích đọc header gốc (hoặc code forward quên ghi đè) thì client tự phong mình làm
 *       admin.
 *   <li>Request public (vd {@code /v1/auth/**}) cũng phải sạch header danh tính: client không được
 *       gửi {@code x-user-id} cho endpoint không cần token.
 * </ul>
 *
 * <p>{@code user == null} nghĩa là "không có danh tính" (request public) — khi đó 2 header này luôn
 * trả về null/không tồn tại.
 */
public class UserHeaderRequest extends HttpServletRequestWrapper {

  private final String userId;
  private final String role;

  public UserHeaderRequest(HttpServletRequest request, AuthenticatedUser user) {
    super(request);
    this.userId = user == null ? null : user.userId();
    this.role = user == null ? null : user.role();
  }

  @Override
  public String getHeader(String name) {
    if (UserHeaders.USER_ID.equalsIgnoreCase(name)) {
      return userId;
    }
    if (UserHeaders.USER_ROLE.equalsIgnoreCase(name)) {
      return role;
    }
    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (UserHeaders.isManaged(name)) {
      String value = UserHeaders.USER_ID.equalsIgnoreCase(name) ? userId : role;
      return value == null
          ? Collections.emptyEnumeration()
          : Collections.enumeration(List.of(value));
    }
    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    List<String> names = new ArrayList<>();
    Enumeration<String> original = super.getHeaderNames();
    if (original != null) {
      for (String name : Collections.list(original)) {
        if (!UserHeaders.isManaged(name)) {
          names.add(name);
        }
      }
    }
    if (userId != null) {
      names.add(UserHeaders.USER_ID);
    }
    if (role != null) {
      names.add(UserHeaders.USER_ROLE);
    }
    return Collections.enumeration(names);
  }
}
