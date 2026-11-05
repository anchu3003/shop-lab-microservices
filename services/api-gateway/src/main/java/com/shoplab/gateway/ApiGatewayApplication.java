package com.shoplab.gateway;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.gateway.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * api-gateway — cửa vào duy nhất của ShopLab (SL-1.2).
 *
 * <p>Ba việc, và chỉ ba việc (gateway phải <b>mỏng</b> — architecture.md §4.3):
 *
 * <ol>
 *   <li>route {@code /v1/**} tới service tương ứng theo config,
 *   <li>verify JWT một chỗ (thay vì mỗi service tự làm),
 *   <li>giữ correlation id xuyên suốt để log nhiều service ghép lại được một trace.
 * </ol>
 *
 * <p><b>Không</b> chứa business logic, <b>không</b> sở hữu dữ liệu nào (không DB).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

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
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
