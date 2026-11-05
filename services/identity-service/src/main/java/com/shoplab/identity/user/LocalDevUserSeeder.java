package com.shoplab.identity.user;

import com.shoplab.identity.config.ServiceProperties;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed user dev {@code admin}/{@code seller} (SL-1.4) — <b>chỉ chạy khi {@code APP_ENV=local}</b>.
 *
 * <p>Vì sao là code chứ không phải migration: migration phải nhúng sẵn BCrypt hash vào SQL, ai đọc
 * repo cũng thấy hash của mật khẩu dev và không đổi được mật khẩu mà không viết migration mới. Chạy
 * bằng {@link PasswordEncoder} thì idempotent, đổi mật khẩu dev = sửa 1 hằng số.
 *
 * <p><b>Không bao giờ</b> chạy ở dev/staging/prod: user admin với mật khẩu biết trước ở môi trường
 * thật là lỗ hổng, không phải tiện ích.
 */
@Component
public class LocalDevUserSeeder implements ApplicationRunner {

  /** Mật khẩu dev — cũng là mật khẩu trong ví dụ README. KHÔNG dùng ở môi trường thật. */
  static final String DEV_PASSWORD = "matkhau123";

  private static final Logger LOG = LoggerFactory.getLogger(LocalDevUserSeeder.class);

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final ServiceProperties properties;
  private final Clock clock;

  public LocalDevUserSeeder(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      ServiceProperties properties,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.appEnv().isLocal()) {
      return;
    }
    seed("admin@shoplab.local", "ShopLab Admin", Role.ADMIN);
    seed("seller@shoplab.local", "ShopLab Seller", Role.SELLER);
  }

  private void seed(String email, String displayName, Role role) {
    if (users.existsByEmail(email)) {
      return;
    }
    users.save(
        User.register(
            email, passwordEncoder.encode(DEV_PASSWORD), displayName, role, clock.instant()));
    LOG.info(
        "seed user dev: {} (role={}) — mật khẩu dev xem README, KHÔNG dùng ở môi trường thật",
        email,
        role);
  }
}
