package com.shoplab.order.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test tính tiền — chỗ dễ sai nhất của service này (tiền là thật, sai 1 đồng là sai hoá đơn). */
class OrderPricingTest {

  private static OrderPricing.PricedItem item(String unitPrice, int qty) {
    return new OrderPricing.PricedItem(
        UUID.randomUUID(), "SKU-1", "Sản phẩm", new BigDecimal(unitPrice), "VND", qty, null);
  }

  @Test
  @DisplayName("line_total = unitPrice × qty, subtotal = tổng line, total = subtotal − discount")
  void shouldComputeTotals() {
    OrderPricing.Priced priced =
        OrderPricing.price(
            List.of(item("100000.00", 2), item("50000.50", 1)), new BigDecimal("20000"), "VND");

    assertThat(priced.items().get(0).lineTotal()).isEqualByComparingTo("200000.00");
    assertThat(priced.items().get(1).lineTotal()).isEqualByComparingTo("50000.50");
    assertThat(priced.subtotal()).isEqualByComparingTo("250000.50");
    assertThat(priced.discount()).isEqualByComparingTo("20000");
    assertThat(priced.total()).isEqualByComparingTo("230000.50");
  }

  @Test
  @DisplayName("discount null ⇒ 0; total = subtotal")
  void nullDiscountMeansZero() {
    OrderPricing.Priced priced = OrderPricing.price(List.of(item("10000.00", 3)), null, "VND");

    assertThat(priced.discount()).isEqualByComparingTo("0");
    assertThat(priced.total()).isEqualByComparingTo("30000.00");
  }

  @Test
  @DisplayName("discount > subtotal → 400 (không để tổng âm)")
  void discountAboveSubtotalIsRejected() {
    assertThatThrownBy(
            () -> OrderPricing.price(List.of(item("10000.00", 1)), new BigDecimal("20000"), "VND"))
        .isInstanceOf(com.shoplab.commons.error.AppException.class)
        .hasMessageContaining("không được lớn hơn subtotal");
  }

  @Test
  @DisplayName("discount âm → 400")
  void negativeDiscountIsRejected() {
    assertThatThrownBy(
            () -> OrderPricing.price(List.of(item("10000.00", 1)), new BigDecimal("-1"), "VND"))
        .isInstanceOf(com.shoplab.commons.error.AppException.class)
        .hasMessageContaining("discount");
  }

  @Test
  @DisplayName("items rỗng → 400 (đơn không có gì để mua)")
  void emptyItemsIsRejected() {
    assertThatThrownBy(() -> OrderPricing.price(List.of(), null, "VND"))
        .isInstanceOf(com.shoplab.commons.error.AppException.class)
        .hasMessageContaining("ít nhất 1 item");
  }

  @Test
  @DisplayName("giá lẻ được làm tròn HALF_UP 2 chữ số (không để 3 số trong tiền)")
  void unitPriceIsRoundedToTwoDecimals() {
    OrderPricing.Priced priced = OrderPricing.price(List.of(item("10000.555", 1)), null, "VND");

    assertThat(priced.items().get(0).unitPrice()).isEqualByComparingTo("10000.56");
    assertThat(priced.subtotal()).isEqualByComparingTo("10000.56");
  }
}
