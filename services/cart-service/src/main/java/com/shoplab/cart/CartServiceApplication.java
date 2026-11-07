package com.shoplab.cart;

import com.shoplab.cart.config.RequiredEnv;
import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * cart-service — giỏ hàng (SL-2.1).
 *
 * <p>Service <b>duy nhất</b> trong Phase 2 không có Postgres: Redis là source of truth và TTL 30
 * ngày là cơ chế dọn dữ liệu (architecture §8.5 — cart chấp nhận mất, khác mọi service khác).
 *
 * <p>Hai đặc thù về identity: (1) khách <b>chưa đăng nhập vẫn có giỏ</b> ⇒ guest token do server
 * cấp; (2) mọi phép ghi là <b>read-modify-write</b> ⇒ chạy bằng Lua để nguyên tử.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CartServiceApplication {

  public static void main(String[] args) {
    // Validate TRƯỚC SpringApplication.run: thiếu env bắt buộc thì chết ngay (xem SL-0.4).
    try {
      EnvValidator.validate(System.getenv(), RequiredEnv.ALL);
    } catch (MissingConfigException ex) {
      System.err.println();
      System.err.println("[shoplab] KHỞI ĐỘNG THẤT BẠI — cấu hình không hợp lệ");
      System.err.println(ex.getMessage());
      System.err.println();
      System.exit(1);
    }
    SpringApplication.run(CartServiceApplication.class, args);
  }
}
