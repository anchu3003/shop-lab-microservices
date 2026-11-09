package com.shoplab.payment;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.payment.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * payment-service — nguồn sự thật về <b>thanh toán</b> (SL-2.7).
 *
 * <p>Trách nhiệm Phase 2: tạo <b>payment intent</b> idempotent (Idempotency-Key), mô phỏng provider
 * bằng <b>mock</b> (chưa nối provider thật), và nhận <b>webhook</b> từ provider một cách idempotent
 * — khoá tự nhiên của provider ({@code provider_event_id}, {@code provider_txn_id}), không phải
 * khoá của client (`architecture.md` §6.2).
 *
 * <p><b>SL-3.5</b>: webhook chuyển intent sang {@code SUCCEEDED} sẽ ghi thêm một dòng <b>outbox</b>
 * ({@code payment.succeeded.v1}) trong <b>cùng transaction</b>; relay publish lên broker để {@code
 * order-service} tự chốt đơn. Payment vẫn <b>không gọi</b> service nào — nó chỉ phát sự kiện (đảo
 * chiều phụ thuộc: order biết payment, payment không cần biết order).
 *
 * <p>Database riêng ({@code payment}) — database per service (AGENTS.md §6).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PaymentServiceApplication {

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
    SpringApplication.run(PaymentServiceApplication.class, args);
  }
}
