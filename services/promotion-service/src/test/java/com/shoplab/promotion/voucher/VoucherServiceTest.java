package com.shoplab.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Test nghiệp vụ tạo/xem voucher (rule nghiệp vụ + chuẩn hoá mã). */
class VoucherServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

  private final VoucherRepository vouchers = mock(VoucherRepository.class);
  private final VoucherService service =
      new VoucherService(vouchers, Clock.fixed(NOW, ZoneOffset.UTC));

  private static CreateVoucherRequest request(VoucherType type, String value, String maxDiscount) {
    return new CreateVoucherRequest(
        "  sale10 ",
        type,
        new BigDecimal(value),
        maxDiscount == null ? null : new BigDecimal(maxDiscount),
        null,
        10,
        NOW.minusSeconds(60),
        NOW.plusSeconds(3600));
  }

  @Test
  @DisplayName("tạo voucher: mã chuẩn hoá UPPER + trim, minOrder null ⇒ 0")
  void createShouldNormalizeCodeAndDefaults() {
    when(vouchers.findByCode("SALE10")).thenReturn(Optional.empty());

    VoucherResponse created = service.create(request(VoucherType.PERCENT, "10", "50000"));

    assertThat(created.code()).isEqualTo("SALE10");
    assertThat(created.minOrder()).isEqualByComparingTo("0");
    assertThat(created.used()).isZero();
    assertThat(created.locked()).isZero();
    assertThat(created.available()).isEqualTo(10);

    ArgumentCaptor<VoucherRecord> saved = ArgumentCaptor.forClass(VoucherRecord.class);
    verify(vouchers).insert(saved.capture());
    assertThat(saved.getValue().code()).isEqualTo("SALE10");
    assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("mã đã tồn tại (khác hoa/thường cũng tính) → 409 DuplicateVoucher")
  void createShouldRejectDuplicate() {
    when(vouchers.findByCode("SALE10")).thenReturn(Optional.of(mock(VoucherRecord.class)));

    assertThatThrownBy(() -> service.create(request(VoucherType.FIXED, "10000", null)))
        .isInstanceOf(DuplicateVoucherException.class)
        .hasMessageContaining("SALE10");
  }

  @Test
  @DisplayName("PERCENT > 100 → 400, KHÔNG ghi DB")
  void createShouldRejectPercentAbove100() {
    assertThatThrownBy(() -> service.create(request(VoucherType.PERCENT, "150", null)))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("<= 100");
    verify(vouchers, never()).insert(any());
  }

  @Test
  @DisplayName("maxDiscount cho FIXED là mơ hồ → 400")
  void createShouldRejectMaxDiscountOnFixed() {
    assertThatThrownBy(() -> service.create(request(VoucherType.FIXED, "10000", "5000")))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("maxDiscount");
  }

  @Test
  @DisplayName("endAt <= startAt → 400")
  void createShouldRejectInvalidWindow() {
    CreateVoucherRequest invalid =
        new CreateVoucherRequest(
            "SALE10", VoucherType.FIXED, new BigDecimal("10000"), null, null, 10, NOW, NOW);

    assertThatThrownBy(() -> service.create(invalid))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("endAt");
  }

  @Test
  @DisplayName("get: chuẩn hoá mã trước khi tìm; không có → 404")
  void getShouldNormalizeAndThrowWhenMissing() {
    when(vouchers.findByCode("SALE10")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(" sale10 "))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("SALE10");
  }
}
