package com.shoplab.catalog;

import com.shoplab.catalog.config.RequiredEnv;
import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * catalog-service — nguồn sự thật về category và product (SL-1.5).
 *
 * <p>Trách nhiệm: ai cũng <b>đọc</b> được (guest xem sản phẩm), chỉ {@code seller}/{@code admin}
 * <b>ghi</b> được. Service này <b>tự verify JWT</b> thay vì tin header gateway gắn — defense in
 * depth: gọi thẳng port service vẫn phải có token hợp lệ.
 *
 * <p>Database riêng ({@code catalog}), schema do Flyway quản lý — database per service (AGENTS.md
 * §6). Không biết gì về order/cart: giao tiếp qua API + event.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogServiceApplication {

  public static void main(String[] args) {
    // Validate TRƯỚC SpringApplication.run: thiếu env bắt buộc thì chết ngay, không đi được nửa
    // đường rồi mới lăn ra lỗi ở chỗ khó hiểu (xem Backlog.md → SL-0.4).
    try {
      EnvValidator.validate(System.getenv(), RequiredEnv.ALL);
    } catch (MissingConfigException ex) {
      // Chưa có logger (Spring còn chưa khởi động) → in thẳng message sạch ra stderr rồi thoát.
      System.err.println();
      System.err.println("[shoplab] KHỞI ĐỘNG THẤT BẠI — cấu hình không hợp lệ");
      System.err.println(ex.getMessage());
      System.err.println();
      System.exit(1);
    }
    SpringApplication.run(CatalogServiceApplication.class, args);
  }
}
