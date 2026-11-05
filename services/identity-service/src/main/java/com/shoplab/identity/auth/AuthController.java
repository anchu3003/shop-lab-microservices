package com.shoplab.identity.auth;

import com.shoplab.identity.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API xác thực — đường vào công khai (gateway để {@code /v1/auth/**} là public, xem SL-1.2).
 *
 * <p>Version qua path {@code /v1/...} (AGENTS.md §6).
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    User user = authService.register(request.email(), request.password(), request.displayName());
    return UserResponse.from(user);
  }

  @PostMapping("/login")
  public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
    return AuthTokenResponse.bearer(authService.login(request.email(), request.password()));
  }

  /**
   * Đổi refresh token lấy cặp token mới (SL-1.4). Token cũ bị vô hiệu hoá ngay (rotation).
   *
   * <p>Dùng lại token đã rotate ⇒ 401 <b>và</b> toàn bộ chuỗi token của phiên đó bị thu hồi.
   */
  @PostMapping("/refresh")
  public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return AuthTokenResponse.bearer(authService.refresh(request.refreshToken()));
  }

  /**
   * Thu hồi refresh token hiện tại (SL-1.4).
   *
   * <p>Trả <b>204</b> (không nội dung) kể cả khi token không tồn tại — idempotent, và không tạo
   * oracle để dò token.
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.refreshToken());
  }
}
