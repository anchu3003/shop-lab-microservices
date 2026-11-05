package com.shoplab.identity.auth;

import com.shoplab.identity.auth.jwt.JwtService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Công bố public key để service khác verify token mà <b>không</b> cần khoá ký.
 *
 * <p>Đường dẫn {@code /.well-known/jwks.json} là quy ước chuẩn (RFC 8615 + JWK Set RFC 7517) — đặt
 * đúng chỗ để client/thư viện khác tự tìm được mà không phải cấu hình.
 */
@RestController
public class JwksController {

  private final JwtService jwtService;

  public JwksController(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    return jwtService.jwks();
  }
}
