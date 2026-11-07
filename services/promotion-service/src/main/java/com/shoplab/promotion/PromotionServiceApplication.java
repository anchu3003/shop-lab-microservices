package com.shoplab.promotion;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.promotion.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * promotion-service — nguồn sự thật về <b>voucher / khuyến mãi</b> (SL-2.4).
 *
 * <p>Trách nhiệm Phase 2: quản lý voucher (mã, rule giảm giá, quota, thời gian hiệu lực) và <b>giữ
 * chỗ 1 lượt dùng</b> (lock → commit | release) để hai checkout đồng thời không cùng dùng được lượt
 * cuối.
 *
 * <p>Không sở hữu order/cart: chỉ biết {@code orderId} và {@code subtotal} do order-service gửi
 * (call nội bộ). Database riêng ({@code promotion}) — database per service (AGENTS.md §6).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PromotionServiceApplication {

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
    SpringApplication.run(PromotionServiceApplication.class, args);
  }
}
