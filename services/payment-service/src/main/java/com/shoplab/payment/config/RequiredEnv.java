package com.shoplab.payment.config;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;

/**
 * Biến môi trường bắt buộc của payment-service.
 *
 * <p>Validate ở {@code main()} qua {@code EnvValidator} — xem {@code PaymentServiceApplication}.
 *
 * <p>{@code PAYMENT_WEBHOOK_SECRET} là bắt buộc và <b>không</b> có default: webhook là endpoint
 * public duy nhất của hệ thống, chạy với secret rỗng nghĩa là ai cũng bắn được "đã thanh toán".
 * Thiếu ⇒ chết lúc boot, không phải lỗi 500 lúc có request.
 */
public final class RequiredEnv {

  /** Môi trường chạy — log level, endpoint debug, và cờ mock provider đều phụ thuộc. */
  public static final RequiredConfig APP_ENV =
      RequiredConfig.of("APP_ENV", "môi trường chạy: local | dev | staging | prod");

  /** Mật khẩu Postgres — thiếu thì HikariCP mới báo lỗi ở lần query đầu tiên (muộn, khó hiểu). */
  public static final RequiredConfig POSTGRES_PASSWORD =
      RequiredConfig.of("POSTGRES_PASSWORD", "mật khẩu Postgres (xem .env.example)");

  /** Secret HMAC-SHA256 để verify chữ ký webhook của provider. */
  public static final RequiredConfig PAYMENT_WEBHOOK_SECRET =
      RequiredConfig.of("PAYMENT_WEBHOOK_SECRET", "secret HMAC để verify webhook provider");

  public static final List<RequiredConfig> ALL =
      List.of(APP_ENV, POSTGRES_PASSWORD, PAYMENT_WEBHOOK_SECRET);

  private RequiredEnv() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
