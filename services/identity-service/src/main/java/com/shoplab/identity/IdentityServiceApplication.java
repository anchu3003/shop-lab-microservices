package com.shoplab.identity;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.identity.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * identity-service — nguồn định danh và token của ShopLab (SL-1.1).
 *
 * <p>Trách nhiệm: đăng ký, đăng nhập, phát hành <b>JWT RS256</b> và công bố <b>JWKS</b> để service
 * khác verify token mà không cần giữ khoá ký.
 *
 * <p>Database riêng ({@code identity}), schema do Flyway quản lý — database per service (AGENTS.md
 * §6). Service này <b>không</b> biết gì về catalog/order: giao tiếp qua API + event, không share
 * DB.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityServiceApplication {

  public static void main(String[] args) {
    // Validate TRƯỚC SpringApplication.run: thiếu env bắt buộc thì chết ngay, không đi được nửa
    // đường
    // rồi mới lăn ra lỗi ở chỗ khó hiểu. Xem Backlog.md → SL-0.4.
    try {
      EnvValidator.validate(System.getenv(), RequiredEnv.ALL);
    } catch (MissingConfigException ex) {
      // Chưa có logger (Spring còn chưa khởi động) và Spring Boot launcher bọc exception trong
      // InvocationTargetException rất khó đọc → in thẳng message sạch ra stderr rồi thoát.
      System.err.println();
      System.err.println("[shoplab] KHỞI ĐỘNG THẤT BẠI — cấu hình không hợp lệ");
      System.err.println(ex.getMessage());
      System.err.println();
      System.exit(1);
    }
    SpringApplication.run(IdentityServiceApplication.class, args);
  }
}
