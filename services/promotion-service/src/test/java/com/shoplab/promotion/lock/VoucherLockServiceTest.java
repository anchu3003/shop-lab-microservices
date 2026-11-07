package com.shoplab.promotion.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.NotFoundException;
import com.shoplab.promotion.config.PromotionProperties;
import com.shoplab.promotion.voucher.VoucherRecord;
import com.shoplab.promotion.voucher.VoucherRepository;
import com.shoplab.promotion.voucher.VoucherType;
import com.shoplab.promotion.voucher.VoucherUnavailableException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test nghiệp vụ lock/commit/release voucher — mock 2 repository.
 *
 * <p>Cái phải để IT kiểm: conditional UPDATE thật trên Postgres và tính nguyên tử khi 100 request
 * đồng thời.
 */
class VoucherLockServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String CODE = "SALE10";

  private final VoucherLockRepository locks = mock(VoucherLockRepository.class);
  private final VoucherRepository vouchers = mock(VoucherRepository.class);

  private final VoucherLockService service =
      new VoucherLockService(
          locks,
          vouchers,
          new PromotionProperties(Duration.ofMinutes(15), 100),
          Clock.fixed(NOW, ZoneOffset.UTC));

  private static VoucherRecord voucher(
      int quota, int used, int locked, String minOrder, Instant start, Instant end) {
    return new VoucherRecord(
        UUID.randomUUID(),
        CODE,
        VoucherType.PERCENT,
        new BigDecimal("10"),
        null,
        new BigDecimal(minOrder),
        quota,
        used,
        locked,
        start,
        end,
        NOW.minusSeconds(3600));
  }

  private static VoucherRecord validVoucher() {
    return voucher(10, 0, 0, "0", NOW.minusSeconds(60), NOW.plusSeconds(3600));
  }

  private static VoucherLockRecord lockedRecord(Instant expiresAt) {
    return new VoucherLockRecord(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        VoucherLockStatus.LOCKED,
        new BigDecimal("200000.00"),
        new BigDecimal("20000.00"),
        expiresAt,
        NOW.minusSeconds(60));
  }

  @Test
  @DisplayName("lock: giữ 1 lượt, tính discount theo subtotal, TTL 15 phút")
  void lockShouldHoldQuotaAndComputeDiscount() {
    UUID orderId = UUID.randomUUID();
    when(locks.findLockedByOrderId(orderId)).thenReturn(Optional.empty());
    when(vouchers.findByCode(CODE)).thenReturn(Optional.of(validVoucher()));
    when(vouchers.lock(CODE, NOW)).thenReturn(1);

    VoucherLockService.LockOutcome outcome =
        service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("200000.00")));

    assertThat(outcome.created()).isTrue();
    assertThat(outcome.response().discount()).isEqualByComparingTo("20000.00");
    assertThat(outcome.response().finalTotal()).isEqualByComparingTo("180000.00");
    assertThat(outcome.response().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    assertThat(outcome.response().voucherCode()).isEqualTo(CODE);
  }

  @Test
  @DisplayName("lock cùng orderId lần 2 → replay, KHÔNG giữ lượt lần 2")
  void lockShouldBeIdempotentByOrderId() {
    UUID orderId = UUID.randomUUID();
    VoucherLockRecord existing =
        new VoucherLockRecord(
            UUID.randomUUID(),
            UUID.randomUUID(),
            orderId,
            VoucherLockStatus.LOCKED,
            new BigDecimal("200000.00"),
            new BigDecimal("20000.00"),
            NOW.plusSeconds(600),
            NOW);
    when(locks.findLockedByOrderId(orderId)).thenReturn(Optional.of(existing));
    when(vouchers.findById(existing.voucherId())).thenReturn(Optional.of(validVoucher()));

    VoucherLockService.LockOutcome outcome =
        service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("200000.00")));

    assertThat(outcome.created()).isFalse();
    assertThat(outcome.response().lockId()).isEqualTo(existing.id());
    verify(vouchers, never()).lock(any(), any());
    verify(locks, never()).insert(any());
  }

  @Test
  @DisplayName("lock: đơn dưới minOrder → 409, không chiếm lượt")
  void lockShouldRejectBelowMinOrder() {
    UUID orderId = UUID.randomUUID();
    when(locks.findLockedByOrderId(orderId)).thenReturn(Optional.empty());
    when(vouchers.findByCode(CODE))
        .thenReturn(
            Optional.of(voucher(10, 0, 0, "200000", NOW.minusSeconds(60), NOW.plusSeconds(3600))));

    assertThatThrownBy(
            () -> service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("150000"))))
        .isInstanceOf(VoucherUnavailableException.class)
        .hasMessageContaining("tối thiểu");
    verify(vouchers, never()).lock(any(), any());
  }

  @Test
  @DisplayName("lock: ngoài cửa sổ thời gian → 409 (chưa tới / đã hết)")
  void lockShouldRejectOutsideWindow() {
    UUID orderId = UUID.randomUUID();
    when(locks.findLockedByOrderId(orderId)).thenReturn(Optional.empty());
    when(vouchers.findByCode(CODE))
        .thenReturn(
            Optional.of(voucher(10, 0, 0, "0", NOW.plusSeconds(60), NOW.plusSeconds(3600))));

    assertThatThrownBy(
            () -> service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("100000"))))
        .isInstanceOf(VoucherUnavailableException.class)
        .hasMessageContaining("chưa tới");

    when(vouchers.findByCode(CODE))
        .thenReturn(Optional.of(voucher(10, 0, 0, "0", NOW.minusSeconds(3600), NOW)));
    assertThatThrownBy(
            () -> service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("100000"))))
        .isInstanceOf(VoucherUnavailableException.class)
        .hasMessageContaining("hết thời gian");
  }

  @Test
  @DisplayName("lock: hết lượt (conditional UPDATE trả 0) → 409, nêu rõ lý do")
  void lockShouldReportExhausted() {
    UUID orderId = UUID.randomUUID();
    when(locks.findLockedByOrderId(orderId)).thenReturn(Optional.empty());
    when(vouchers.findByCode(CODE))
        .thenReturn(
            Optional.of(voucher(10, 8, 2, "0", NOW.minusSeconds(60), NOW.plusSeconds(3600))));
    when(vouchers.lock(CODE, NOW)).thenReturn(0);

    assertThatThrownBy(
            () -> service.lock(new LockVoucherRequest(CODE, orderId, new BigDecimal("100000"))))
        .isInstanceOf(VoucherUnavailableException.class)
        .hasMessageContaining("hết lượt");
    verify(locks, never()).insert(any());
  }

  @Test
  @DisplayName("lock không tồn tại → 404")
  void unknownLockShouldBe404() {
    UUID missing = UUID.randomUUID();
    when(locks.findById(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.commit(missing)).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("commit: LOCKED→COMMITTED, dùng thật 1 lượt (used+1, locked-1)")
  void commitShouldConsumeQuota() {
    VoucherLockRecord lock = lockedRecord(NOW.plusSeconds(600));
    when(locks.findById(lock.id())).thenReturn(Optional.of(lock));
    when(locks.markStatus(lock.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.COMMITTED, NOW))
        .thenReturn(1);
    when(vouchers.commit(lock.voucherId())).thenReturn(1);
    when(vouchers.findById(lock.voucherId())).thenReturn(Optional.of(validVoucher()));

    assertThat(service.commit(lock.id()).status()).isEqualTo(VoucherLockStatus.COMMITTED);
    verify(vouchers).commit(lock.voucherId());
  }

  @Test
  @DisplayName("commit lần 2 → 200, KHÔNG trừ quota lần nữa")
  void commitShouldBeIdempotent() {
    VoucherLockRecord committed =
        lockedRecord(NOW.plusSeconds(600)).withStatus(VoucherLockStatus.COMMITTED);
    when(locks.findById(committed.id())).thenReturn(Optional.of(committed));
    when(vouchers.findById(committed.voucherId())).thenReturn(Optional.of(validVoucher()));

    assertThat(service.commit(committed.id()).status()).isEqualTo(VoucherLockStatus.COMMITTED);
    verify(vouchers, never()).commit(any());
  }

  @Test
  @DisplayName("commit lock quá TTL → 410 + trả lượt ngay (lazy expire)")
  void commitShouldExpireWhenTtlPassed() {
    VoucherLockRecord expiredLock = lockedRecord(NOW.minusSeconds(1));
    when(locks.findById(expiredLock.id())).thenReturn(Optional.of(expiredLock));
    when(vouchers.release(expiredLock.voucherId())).thenReturn(1);

    assertThatThrownBy(() -> service.commit(expiredLock.id()))
        .isInstanceOf(VoucherLockExpiredException.class)
        .hasMessageContaining("hết hạn");
    verify(vouchers).release(expiredLock.voucherId());
    verify(locks)
        .markStatus(expiredLock.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.EXPIRED, NOW);
  }

  @Test
  @DisplayName("release: trả lượt; lần 2 idempotent; sau commit → 409")
  void releaseShouldBeIdempotentAndRejectCommitted() {
    VoucherLockRecord lock = lockedRecord(NOW.plusSeconds(600));
    when(locks.findById(lock.id())).thenReturn(Optional.of(lock));
    when(locks.markStatus(lock.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.RELEASED, NOW))
        .thenReturn(1);
    when(vouchers.release(lock.voucherId())).thenReturn(1);
    when(vouchers.findById(lock.voucherId())).thenReturn(Optional.of(validVoucher()));

    assertThat(service.release(lock.id()).status()).isEqualTo(VoucherLockStatus.RELEASED);

    when(locks.findById(lock.id()))
        .thenReturn(Optional.of(lock.withStatus(VoucherLockStatus.RELEASED)));
    assertThat(service.release(lock.id()).status()).isEqualTo(VoucherLockStatus.RELEASED);

    when(locks.findById(lock.id()))
        .thenReturn(Optional.of(lock.withStatus(VoucherLockStatus.COMMITTED)));
    assertThatThrownBy(() -> service.release(lock.id())).hasMessageContaining("đã commit");
  }

  @Test
  @DisplayName("expireOverdue: trả lượt cho lock quá hạn; BỎ QUA lock đã bị xử lý (markStatus = 0)")
  void expireOverdueShouldReleaseQuotaButSkipClaimed() {
    VoucherLockRecord first = lockedRecord(NOW.minusSeconds(60));
    VoucherLockRecord claimed = lockedRecord(NOW.minusSeconds(30));
    when(locks.findExpiredLocked(NOW, 100)).thenReturn(List.of(first, claimed));
    when(locks.markStatus(first.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.EXPIRED, NOW))
        .thenReturn(1);
    when(locks.markStatus(claimed.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.EXPIRED, NOW))
        .thenReturn(0);
    when(vouchers.release(first.voucherId())).thenReturn(1);

    assertThat(service.expireOverdue(NOW)).isEqualTo(1);
    verify(vouchers).release(first.voucherId());
    verify(vouchers, never()).release(claimed.voucherId());
  }
}
