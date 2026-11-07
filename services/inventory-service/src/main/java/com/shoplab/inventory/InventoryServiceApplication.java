package com.shoplab.inventory;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.inventory.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * inventory-service — nguồn sự thật về <b>tồn kho</b> (SL-2.2).
 *
 * <p>Trách nhiệm: giữ chỗ hàng (reserve) một cách <b>nguyên tử</b> để không bao giờ bán quá tồn
 * kho, chốt hàng khi tiền về (commit), trả chỗ khi huỷ/hết hạn (release). Không biết gì về
 * product/order — chỉ giữ {@code sku_id} làm tham chiếu (database per service, AGENTS.md §6).
 *
 * <p>Cơ chế chống oversell: điều kiện "còn đủ hàng" nằm trong chính câu {@code UPDATE} (ADR-0006
 * D1), không dùng distributed lock ⇒ Postgres row lock là chốt chặn duy nhất và đủ.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class InventoryServiceApplication {

  public static void main(String[] args) {
    // Validate TRƯỚC SpringApplication.run: thiếu env bắt buộc thì chết ngay (xem SL-0.4).
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
    SpringApplication.run(InventoryServiceApplication.class, args);
  }
}
