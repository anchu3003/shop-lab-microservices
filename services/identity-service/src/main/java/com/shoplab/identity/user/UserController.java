package com.shoplab.identity.user;

import com.shoplab.identity.auth.jwt.JwtService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API user — endpoint protected của hệ thống.
 *
 * <p><b>Mô hình tin cậy từ SL-1.4</b>: identity <b>tự verify JWT</b> (resource server) và lấy danh
 * tính từ <b>token</b>, không còn tin header {@code x-user-*} của gateway. Header đó chỉ còn là hợp
 * đồng cho service chưa tự verify (catalog ở SL-1.5) — defense in depth.
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {

  /**
   * Thông tin user đang đăng nhập.
   *
   * <p>Lấy từ {@code Authentication} do Spring Security dựng sau khi verify chữ ký + {@code
   * exp}/{@code nbf}/{@code iss} — thiếu/sai token thì request không tới được method này (401).
   */
  @GetMapping("/me")
  public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return new CurrentUserResponse(
        UUID.fromString(jwt.getSubject()), jwt.getClaimAsString(JwtService.ROLE_CLAIM));
  }
}
