package com.shoplab.identity.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean dùng cho xác thực.
 *
 * <p>Ở SL-1.1 cố ý <b>không</b> kéo {@code spring-boot-starter-security} (tức là chưa có filter
 * chain): service chỉ cần hash mật khẩu và ký token. Security chain đầy đủ (401/403, phân quyền)
 * đến ở SL-1.2 (gateway) và SL-1.4 — làm sớm chỉ tạo thêm bề mặt phải bảo vệ mà chưa có endpoint
 * nào cần.
 */
@Configuration
public class SecurityConfig {

  /**
   * BCrypt: có salt sẵn trong hash, chậm có chủ ý (chống brute force) — lựa chọn mặc định an toàn.
   * Không dùng MD5/SHA-* (nhanh = dễ brute force, không salt = lộ rainbow table).
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * {@link Clock} tiêm được để test kiểm soát thời gian (hết hạn token) — thay vì gọi {@code
   * Instant.now()} rải rác trong code, thứ không thể test tất định.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
