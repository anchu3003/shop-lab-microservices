package com.shoplab.promotion.lock;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.promotion.config.PromotionProperties;
import com.shoplab.promotion.voucher.VoucherCalculator;
import com.shoplab.promotion.voucher.VoucherCode;
import com.shoplab.promotion.voucher.VoucherRecord;
import com.shoplab.promotion.voucher.VoucherRepository;
import com.shoplab.promotion.voucher.VoucherUnavailableException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giữ chỗ / chốt / trả lượt voucher — bản sao có chủ ý của pattern {@code reserve/commit/release} ở
 * inventory.
 *
 * <p>Ba tính chất phải giữ:
 *
 * <ol>
 *   <li><b>Không bán quá quota</b>: {@code UPDATE … WHERE used + locked < quota AND cửa sổ thời
 *       gian} — điều kiện nằm trong câu lệnh, không đọc-rồi-ghi.
 *   <li><b>Idempotent</b>: retry cùng {@code orderId} trả lại lock cũ; commit/release lần 2 trả
 *       200.
 *   <li><b>Không double-commit</b>: chuyển trạng thái bằng compare-and-set {@code WHERE
 *       status='LOCKED'}.
 * </ol>
 */
@Service
public class VoucherLockService {

  private final VoucherLockRepository locks;
  private final VoucherRepository vouchers;
  private final PromotionProperties properties;
  private final Clock clock;

  public VoucherLockService(
      VoucherLockRepository locks,
      VoucherRepository vouchers,
      PromotionProperties properties,
      Clock clock) {
    this.locks = locks;
    this.vouchers = vouchers;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * {@code created=false} nghĩa là replay của lần lock trước đó (cùng {@code orderId}) ⇒ controller
   * trả 200.
   */
  public record LockOutcome(VoucherLockResponse response, boolean created) {}

  /**
   * Giữ một lượt voucher cho order.
   *
   * @throws VoucherUnavailableException (409) khi voucher chưa tới giờ / hết hạn / hết lượt / chưa
   *     đủ đơn tối thiểu
   */
  @Transactional
  public LockOutcome lock(LockVoucherRequest request) {
    String code = VoucherCode.normalize(request.code());
    Instant now = Instant.now(clock);

    // Idempotency: retry cùng orderId trả lại đúng lock, KHÔNG giữ lượt lần 2.
    VoucherLockRecord existing = locks.findLockedByOrderId(request.orderId()).orElse(null);
    if (existing != null) {
      return new LockOutcome(VoucherLockResponse.from(existing, codeOf(existing)), false);
    }

    VoucherRecord voucher =
        vouchers
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("Voucher không tồn tại: " + code));
    requireUsable(voucher, request.subtotal(), now);

    BigDecimal discount = VoucherCalculator.discountFor(voucher, request.subtotal());

    if (vouchers.lock(code, now) == 0) {
      // Vừa bị người khác lấy mất lượt cuối (hoặc voucher vừa ra ngoài cửa sổ) — đọc lại để báo
      // đúng lý do.
      throw explainLockFailure(code, now);
    }

    VoucherLockRecord created =
        new VoucherLockRecord(
            UUID.randomUUID(),
            voucher.id(),
            request.orderId(),
            VoucherLockStatus.LOCKED,
            request.subtotal(),
            discount,
            now.plus(properties.lockTtl()),
            now);
    locks.insert(created);
    return new LockOutcome(VoucherLockResponse.from(created, code), true);
  }

  /**
   * Chốt: lượt voucher được dùng thật ({@code used+1, locked-1}).
   *
   * <p>{@code noRollbackFor}: khi lock đã hết TTL, hàm vừa <b>trả lượt</b> vừa ném 410 — mặc định
   * Spring rollback sẽ xoá phần trả lượt ⇒ voucher bị treo. Cùng lý do và cùng cách xử lý như
   * {@code ReservationService.commit} ở inventory.
   */
  @Transactional(noRollbackFor = VoucherLockExpiredException.class)
  public VoucherLockResponse commit(UUID lockId) {
    VoucherLockRecord lock = require(lockId);
    Instant now = Instant.now(clock);

    if (lock.status() == VoucherLockStatus.COMMITTED) {
      return VoucherLockResponse.from(lock, codeOf(lock));
    }
    if (lock.status() == VoucherLockStatus.RELEASED) {
      throw conflict("Voucher lock đã release, không commit được: " + lockId);
    }
    if (lock.status() == VoucherLockStatus.EXPIRED) {
      throw expired(lock);
    }

    if (!now.isBefore(lock.expiresAt())) {
      releaseQuota(lock, VoucherLockStatus.EXPIRED, now);
      throw expired(lock);
    }

    if (locks.markStatus(lockId, VoucherLockStatus.LOCKED, VoucherLockStatus.COMMITTED, now) == 0) {
      return commitAfterLosingRace(lockId);
    }
    if (vouchers.commit(lock.voucherId()) == 0) {
      // locked < 1: dữ liệu đã lệch từ trước ⇒ fail toàn bộ transaction thay vì ghi tiếp.
      throw new IllegalStateException(
          "vouchers.locked không đủ để commit voucher=" + lock.voucherId());
    }
    return VoucherLockResponse.from(lock.withStatus(VoucherLockStatus.COMMITTED), codeOf(lock));
  }

  /** Trả lượt (huỷ đơn / thanh toán fail). Idempotent; trả lock đã commit → 409. */
  @Transactional
  public VoucherLockResponse release(UUID lockId) {
    VoucherLockRecord lock = require(lockId);
    Instant now = Instant.now(clock);

    if (lock.status() == VoucherLockStatus.RELEASED || lock.status() == VoucherLockStatus.EXPIRED) {
      return VoucherLockResponse.from(lock, codeOf(lock));
    }
    if (lock.status() == VoucherLockStatus.COMMITTED) {
      throw conflict("Voucher lock đã commit, không release được: " + lockId);
    }

    if (locks.markStatus(lockId, VoucherLockStatus.LOCKED, VoucherLockStatus.RELEASED, now) == 0) {
      VoucherLockRecord current = require(lockId);
      return VoucherLockResponse.from(current, codeOf(current));
    }
    releaseQuota(lock, VoucherLockStatus.RELEASED, now);
    return VoucherLockResponse.from(lock.withStatus(VoucherLockStatus.RELEASED), codeOf(lock));
  }

  /**
   * Dọn lock quá TTL (gọi từ {@code VoucherLockSweeper}) — batch, idempotent theo trạng thái.
   *
   * @return số lock đã expire trong lần chạy này
   */
  @Transactional
  public int expireOverdue(Instant now) {
    List<VoucherLockRecord> overdue = locks.findExpiredLocked(now, properties.sweepBatchSize());
    int expired = 0;
    for (VoucherLockRecord lock : overdue) {
      if (locks.markStatus(lock.id(), VoucherLockStatus.LOCKED, VoucherLockStatus.EXPIRED, now)
          == 0) {
        continue; // request/instance khác đã xử lý — không trả lượt 2 lần
      }
      if (vouchers.release(lock.voucherId()) == 0) {
        throw new IllegalStateException(
            "vouchers.locked không đủ để release voucher=" + lock.voucherId());
      }
      expired++;
    }
    return expired;
  }

  /** Trả lượt cho voucher và đánh dấu lock sang trạng thái kết thúc. */
  private void releaseQuota(VoucherLockRecord lock, VoucherLockStatus to, Instant now) {
    if (vouchers.release(lock.voucherId()) == 0) {
      throw new IllegalStateException(
          "vouchers.locked không đủ để release voucher=" + lock.voucherId());
    }
    locks.markStatus(lock.id(), VoucherLockStatus.LOCKED, to, now);
  }

  private VoucherLockResponse commitAfterLosingRace(UUID lockId) {
    VoucherLockRecord current = require(lockId);
    return switch (current.status()) {
      case COMMITTED -> VoucherLockResponse.from(current, codeOf(current));
      case EXPIRED -> throw expired(current);
      case RELEASED -> throw conflict("Voucher lock vừa được release bởi request khác: " + lockId);
      case LOCKED ->
          throw new IllegalStateException("Không chuyển được lock " + lockId + " sang COMMITTED");
    };
  }

  /**
   * Kiểm cửa sổ thời gian + {@code minOrder} trước khi chiếm lượt (trả lỗi rõ thay vì "hết lượt"
   * chung chung).
   */
  private void requireUsable(VoucherRecord voucher, BigDecimal subtotal, Instant now) {
    if (now.isBefore(voucher.startAt())) {
      throw new VoucherUnavailableException(
          VoucherUnavailableException.Reason.NOT_STARTED, voucher.startAt().toString());
    }
    if (!now.isBefore(voucher.endAt())) {
      throw new VoucherUnavailableException(
          VoucherUnavailableException.Reason.ENDED, voucher.endAt().toString());
    }
    if (subtotal.compareTo(voucher.minOrder()) < 0) {
      throw new VoucherUnavailableException(
          VoucherUnavailableException.Reason.BELOW_MIN_ORDER,
          "cần tối thiểu " + voucher.minOrder() + ", nhận " + subtotal);
    }
  }

  /**
   * {@code lock()} trả 0: đọc lại voucher để nói rõ là hết lượt hay vừa ra ngoài cửa sổ thời gian.
   */
  private AppException explainLockFailure(String code, Instant now) {
    VoucherRecord voucher =
        vouchers
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("Voucher không tồn tại: " + code));
    if (now.isBefore(voucher.startAt())) {
      return new VoucherUnavailableException(VoucherUnavailableException.Reason.NOT_STARTED, code);
    }
    if (!now.isBefore(voucher.endAt())) {
      return new VoucherUnavailableException(VoucherUnavailableException.Reason.ENDED, code);
    }
    return new VoucherUnavailableException(
        VoucherUnavailableException.Reason.EXHAUSTED,
        "còn " + voucher.available() + "/" + voucher.quota() + " lượt");
  }

  private VoucherLockRecord require(UUID lockId) {
    return locks
        .findById(lockId)
        .orElseThrow(() -> new NotFoundException("Voucher lock không tồn tại: " + lockId));
  }

  /** Mã voucher để trả về client — cần cho hiển thị/đối soát (lock chỉ lưu {@code voucherId}). */
  private String codeOf(VoucherLockRecord lock) {
    return vouchers.findById(lock.voucherId()).map(VoucherRecord::code).orElse("UNKNOWN");
  }

  private static AppException conflict(String message) {
    return new AppException(ErrorCode.CONFLICT, message);
  }

  private static VoucherLockExpiredException expired(VoucherLockRecord lock) {
    return new VoucherLockExpiredException(
        "Voucher lock đã hết hạn lúc " + lock.expiresAt() + ": " + lock.id());
  }
}
