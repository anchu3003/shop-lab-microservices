package com.shoplab.order;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.order.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * order-service — nguồn sự thật về <b>đơn hàng</b> (SL-2.5).
 *
 * <p>Trách nhiệm Phase 2: tạo order **idempotent** (Idempotency-Key), lưu **snapshot** giá/tên SKU
 * trong {@code order_items} (hoá đơn bất biến), và quản lý state machine {@code PENDING_PAYMENT →
 * CONFIRMED | CANCELLED | EXPIRED}. Phần orchestrate checkout (gọi inventory/promotion/payment) là
 * `SL-2.6`.
 *
 * <p>Database riêng ({@code order}) — database per service (AGENTS.md §6). Không đọc DB của service
 * khác.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OrderServiceApplication {

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
    SpringApplication.run(OrderServiceApplication.class, args);
  }
}
