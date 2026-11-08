package com.shoplab.order.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình gọi service khác của saga checkout (`SL-2.6a`).
 *
 * <p><b>Vì sao URL nằm ở config chứ không hard-code</b> (12-factor, `AGENTS.md` §6): compose dùng
 * hostname nội bộ (`http://cart-service:8083`), máy dev chạy ngoài Docker dùng `localhost`, CI test
 * dùng stub HTTP ở port ngẫu nhiên. Cùng một code, ba môi trường.
 *
 * <p><b>Vì sao timeout bắt buộc phải cấu hình được</b>: ADR-0006 chốt checkout Phase 2 là
 * <b>sync</b> ⇒ không có timeout nghĩa là một service chậm kéo sập luôn checkout (và giữ connection
 * của order-service). Mặc định 2 s connect / 800 ms read là mức "fail-fast" đã ghi trong ADR.
 *
 * @param cartUrl `cart-service` — đọc giỏ của user
 * @param catalogUrl `catalog-service` — batch giá SKU (`SL-2.13`)
 * @param inventoryUrl `inventory-service` — reserve / release
 * @param promotionUrl `promotion-service` — lock / release voucher
 * @param paymentUrl `payment-service` — tạo payment intent
 * @param connectTimeout timeout mở connection tới service đích
 * @param readTimeout timeout chờ response của service đích
 * @param currency tiền tệ của đơn — catalog Phase 2 chưa có khái niệm tiền tệ nên saga quyết định
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.saga")
public record SagaProperties(
    @DefaultValue("http://localhost:8083") String cartUrl,
    @DefaultValue("http://localhost:8082") String catalogUrl,
    @DefaultValue("http://localhost:8084") String inventoryUrl,
    @DefaultValue("http://localhost:8086") String promotionUrl,
    @DefaultValue("http://localhost:8088") String paymentUrl,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("800ms") Duration readTimeout,
    @DefaultValue("VND") String currency) {

  public SagaProperties {
    cartUrl = requireUrl(cartUrl, "cart-url");
    catalogUrl = requireUrl(catalogUrl, "catalog-url");
    inventoryUrl = requireUrl(inventoryUrl, "inventory-url");
    promotionUrl = requireUrl(promotionUrl, "promotion-url");
    paymentUrl = requireUrl(paymentUrl, "payment-url");
    connectTimeout = requirePositive(connectTimeout, "connect-timeout");
    readTimeout = requirePositive(readTimeout, "read-timeout");
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      throw new IllegalArgumentException(
          "shoplab.saga.currency phải là 3 ký tự (vd VND), nhận: " + currency);
    }
    currency = currency.toUpperCase(Locale.ROOT);
  }

  /**
   * URL phải có scheme http(s) — sai thì lỗi lộ ra ngay lúc khởi động, thay vì đổi thành {@code
   * 503} khó hiểu ở request checkout đầu tiên.
   */
  private static String requireUrl(String url, String field) {
    if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
      throw new IllegalArgumentException(
          "shoplab.saga." + field + " phải bắt đầu bằng http:// hoặc https://, nhận: " + url);
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static Duration requirePositive(Duration value, String field) {
    if (value == null || value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException("shoplab.saga." + field + " phải > 0, nhận: " + value);
    }
    return value;
  }
}
