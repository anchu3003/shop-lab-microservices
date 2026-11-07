package com.shoplab.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test rule giảm giá — hàm thuần, chạy mọi biên (đây là chỗ dễ sai tiền nhất của service). */
class VoucherCalculatorTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

  private static VoucherRecord percent(String value, String maxDiscount) {
    return voucher(VoucherType.PERCENT, value, maxDiscount);
  }

  private static VoucherRecord fixed(String value) {
    return voucher(VoucherType.FIXED, value, null);
  }

  private static VoucherRecord voucher(VoucherType type, String value, String maxDiscount) {
    return new VoucherRecord(
        UUID.randomUUID(),
        "TEST",
        type,
        new BigDecimal(value),
        maxDiscount == null ? null : new BigDecimal(maxDiscount),
        BigDecimal.ZERO,
        10,
        0,
        0,
        NOW,
        NOW.plusSeconds(3600),
        NOW);
  }

  @Test
  @DisplayName("PERCENT: 10% của 500.000 = 50.000")
  void percentShouldApplyRate() {
    assertThat(VoucherCalculator.discountFor(percent("10", null), new BigDecimal("500000.00")))
        .isEqualByComparingTo("50000.00");
  }

  @Test
  @DisplayName("PERCENT có trần: 50% của 500.000 = 250.000 nhưng cap 100.000 ⇒ 100.000")
  void percentShouldRespectMaxDiscount() {
    assertThat(VoucherCalculator.discountFor(percent("50", "100000.00"), new BigDecimal("500000")))
        .isEqualByComparingTo("100000.00");
  }

  @Test
  @DisplayName("PERCENT làm tròn HALF_UP 2 số: 10% của 99.999 = 9.999,90")
  void percentShouldRoundHalfUp() {
    assertThat(VoucherCalculator.discountFor(percent("10", null), new BigDecimal("99999.00")))
        .isEqualByComparingTo("9999.90");
    // 33,333% của 100 = 33.333 -> 3 chữ số => làm tròn HALF_UP ở mức 2 số
    assertThat(VoucherCalculator.discountFor(percent("33.333", null), new BigDecimal("100.00")))
        .isEqualByComparingTo("33.33");
  }

  @Test
  @DisplayName("FIXED: giảm đúng số tiền")
  void fixedShouldApplyAmount() {
    assertThat(VoucherCalculator.discountFor(fixed("30000.00"), new BigDecimal("500000")))
        .isEqualByComparingTo("30000.00");
  }

  @Test
  @DisplayName("Giảm giá không bao giờ vượt subtotal (không để tổng đơn âm)")
  void discountShouldNotExceedSubtotal() {
    assertThat(VoucherCalculator.discountFor(fixed("100000.00"), new BigDecimal("30000.00")))
        .isEqualByComparingTo("30000.00");
    assertThat(VoucherCalculator.discountFor(percent("80", null), new BigDecimal("10.00")))
        .isEqualByComparingTo("8.00");
  }

  @Test
  @DisplayName("subtotal 0 ⇒ giảm 0 (biên, không chia cho ai, không âm)")
  void zeroSubtotalGivesZeroDiscount() {
    assertThat(VoucherCalculator.discountFor(fixed("30000.00"), BigDecimal.ZERO))
        .isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("subtotal âm là lỗi lập trình → IllegalArgumentException")
  void negativeSubtotalIsRejected() {
    assertThatThrownBy(() -> VoucherCalculator.discountFor(fixed("1.00"), new BigDecimal("-1")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
