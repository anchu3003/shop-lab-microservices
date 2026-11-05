package com.shoplab.identity.user;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API user — endpoint <b>protected</b> đầu tiên của hệ thống (SL-1.2 cần nó để chứng minh gateway
 * chặn/cho qua đúng và có gắn {@code x-user-*}).
 *
 * <p><b>Mô hình tin cậy ở Phase 1</b>: service tin header {@code x-user-id}/{@code x-user-role} do
 * gateway gắn. Điều này chỉ an toàn khi service <b>không</b> expose ra ngoài (chỉ gateway gọi được)
 * — trong compose, port của service được publish ra host để tiện debug nên thực tế vẫn gọi thẳng
 * được. {@code SL-1.5} sẽ verify JWT ngay tại service (defense in depth) và từ đó header không còn
 * là nguồn tin duy nhất.
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {

  /**
   * Thông tin user đang đăng nhập.
   *
   * <p>Đọc từ header gateway gắn — <b>không</b> đọc từ token vì service này chưa verify token
   * (Phase 1). Thiếu header nghĩa là request không đi qua gateway (hoặc gọi thẳng service) → 401.
   */
  @GetMapping("/me")
  public CurrentUserResponse me(HttpServletRequest request) {
    String userId = request.getHeader("x-user-id");
    String role = request.getHeader("x-user-role");
    if (userId == null || role == null) {
      throw new AppException(
          ErrorCode.UNAUTHORIZED, "Thiếu danh tính: request phải đi qua api-gateway");
    }
    return new CurrentUserResponse(parseUserId(userId), role);
  }

  private static UUID parseUserId(String rawUserId) {
    try {
      return UUID.fromString(rawUserId);
    } catch (IllegalArgumentException e) {
      // Header do gateway gắn nên sai định dạng = lỗi hệ thống, không phải lỗi client.
      throw new AppException(ErrorCode.INTERNAL_ERROR, "x-user-id không phải UUID hợp lệ");
    }
  }
}
