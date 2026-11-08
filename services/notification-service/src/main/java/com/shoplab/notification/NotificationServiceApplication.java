package com.shoplab.notification;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.notification.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * notification-service — gửi thông báo cho khách khi đơn đổi trạng thái (SL-3.4).
 *
 * <p>Trách nhiệm: consumer {@code order.confirmed.v1} / {@code order.cancelled.v1} từ {@code
 * q.notification} → render template → "gửi" (mock provider: ghi {@code notification_logs}).
 * Idempotent theo {@code eventId}, retry 3 lần rồi vào DLQ.
 *
 * <p><b>Vì sao là service riêng, không nhét vào order</b>: gửi email là phần **không critical** —
 * nó không được phép làm chậm hay làm fail checkout. Tách service + async chính là cách mua sự độc
 * lập đó (ADR-0007). Database riêng ({@code notification}) — database per service (AGENTS.md §6).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NotificationServiceApplication {

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
    SpringApplication.run(NotificationServiceApplication.class, args);
  }
}
