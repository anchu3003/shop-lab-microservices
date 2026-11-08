package com.shoplab.identity.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint cấp **service token** (SL-3.11) — đường dành cho máy, không dành cho người.
 *
 * <p><b>Vì sao không đặt cạnh `/login`</b>: hai đường này có mô hình đe doạ khác nhau. `/login` xác
 * thực <i>người</i> bằng mật khẩu đã hash (BCrypt) và trả refresh token; `/service-token` xác thực
 * <i>service</i> bằng shared secret đọc từ env và chỉ trả access token ngắn hạn. Gộp chung một
 * endpoint sẽ phải phân nhánh bằng `grant_type` — thêm trạng thái mà không thêm giá trị.
 *
 * <p>Endpoint nằm dưới `/v1/auth/**` nên **đã** public ở gateway và ở filter chain của service này:
 * việc bảo vệ là <b>secret của client</b>, chứ không phải "đường này kín". Đúng như mọi token
 * endpoint của OAuth2 — chúng luôn công khai, thứ bảo vệ là credential.
 */
@Tag(name = "Auth", description = "Đăng ký / đăng nhập / refresh / logout")
@RestController
@RequestMapping("/v1/auth")
public class ServiceTokenController {

  private final ServiceAuthService serviceAuth;

  public ServiceTokenController(ServiceAuthService serviceAuth) {
    this.serviceAuth = serviceAuth;
  }

  /** Đổi `clientId` + `clientSecret` (+ `audience`) lấy access token `role=SERVICE`. */
  @PostMapping("/service-token")
  public ServiceTokenResponse serviceToken(@Valid @RequestBody ServiceTokenRequest request) {
    return serviceAuth.issue(request);
  }
}
