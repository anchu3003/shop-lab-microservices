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
}
