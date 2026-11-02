package com.shoplab.template;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.template.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Service mẫu của ShopLab.
 *
 * <p>Copy thư mục này để tạo service mới — xem {@code README.md} → "Tạo service mới từ template".
 *
 * <p>Service này cố ý <b>không</b> có business logic: nó chỉ chứng minh hạ tầng dùng chung chạy
 * được (log JSON, error response thống nhất, correlation id, readiness, graceful shutdown).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ServiceTemplateApplication {

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
    SpringApplication.run(ServiceTemplateApplication.class, args);
  }
}
