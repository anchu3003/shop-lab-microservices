package com.shoplab.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Parse payload event → row index. Payload hỏng phải ném lỗi (→ retry → DLQ), không ghi row nửa
 * vời.
 */
class SearchProductTest {

  private static Map<String, Object> payload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("productId", UUID.randomUUID().toString());
    payload.put("name", "Điện thoại Samsung");
    payload.put("description", "Màn hình lớn");
    payload.put("categoryId", UUID.randomUUID().toString());
    payload.put("price", 12990000.00);
    payload.put("status", "ACTIVE");
    payload.put("updatedAt", "2026-11-09T10:00:00Z");
    return payload;
  }

  @Test
  @DisplayName("payload đầy đủ ⇒ map đúng field, price là BigDecimal (không phải double)")
  void mapsCompletePayload() {
    Map<String, Object> payload = payload();

    SearchProduct product = SearchProduct.fromEvent(payload);

    assertThat(product.productId().toString()).isEqualTo(payload.get("productId"));
    assertThat(product.name()).isEqualTo("Điện thoại Samsung");
    assertThat(product.description()).isEqualTo("Màn hình lớn");
    // So sánh theo GIÁ TRỊ (isEqualByComparingTo) chứ không theo chuỗi: `double 1.299E7` và
    // `BigDecimal 12990000.00` là cùng một số tiền — kiểm scale là kiểm chi tiết serialize.
    assertThat(product.minPrice()).isEqualByComparingTo("12990000.00");
    assertThat(product.status()).isEqualTo(SearchProduct.ACTIVE);
    assertThat(product.updatedAt()).isEqualTo(Instant.parse("2026-11-09T10:00:00Z"));
  }

  @Test
  @DisplayName("price dạng chuỗi (BigDecimal serialize thành string) vẫn parse được")
  void parsesStringPrice() {
    Map<String, Object> payload = payload();
    payload.put("price", "500000.00");

    assertThat(SearchProduct.fromEvent(payload).minPrice().toPlainString()).isEqualTo("500000.00");
  }

  @Test
  @DisplayName("thiếu field bắt buộc ⇒ ném lỗi để message đi DLQ, KHÔNG ghi row rác")
  void rejectsIncompletePayload() {
    Map<String, Object> noId = payload();
    noId.remove("productId");
    assertThatThrownBy(() -> SearchProduct.fromEvent(noId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("productId");

    Map<String, Object> noPrice = payload();
    noPrice.remove("price");
    assertThatThrownBy(() -> SearchProduct.fromEvent(noPrice))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name/price/updatedAt");
  }

  @Test
  @DisplayName("description null giữ null; thiếu status ⇒ mặc định ACTIVE")
  void defaultsStatusAndKeepsNullDescription() {
    Map<String, Object> payload = payload();
    payload.remove("status");
    payload.remove("description");

    SearchProduct product = SearchProduct.fromEvent(payload);

    assertThat(product.status()).isEqualTo(SearchProduct.ACTIVE);
    assertThat(product.description()).isNull();
  }
}
