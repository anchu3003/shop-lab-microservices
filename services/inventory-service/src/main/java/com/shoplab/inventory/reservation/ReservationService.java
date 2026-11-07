package com.shoplab.inventory.reservation;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.inventory.config.InventoryProperties;
import com.shoplab.inventory.ledger.LedgerReason;
import com.shoplab.inventory.ledger.StockLedgerEntry;
import com.shoplab.inventory.ledger.StockLedgerRepository;
import com.shoplab.inventory.stock.InsufficientStockException;
import com.shoplab.inventory.stock.StockRepository;
import com.shoplab.inventory.stock.StockSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ giữ chỗ tồn kho: {@code reserve → commit | release} (ADR-0006 D1).
 *
 * <p>Ba tính chất phải giữ, mỗi cái đều có test riêng:
 *
 * <ol>
 *   <li><b>All-or-nothing</b>: reserve nhiều SKU thì hoặc giữ đủ, hoặc không giữ cái nào — nhờ 1
 *       transaction + ném exception khi 1 SKU hết hàng (rollback xoá luôn header/items/ledger đã
 *       ghi).
 *   <li><b>Idempotent</b>: retry cùng {@code orderId} trả đúng reservation cũ (không trừ kho lần
 *       2); commit và release cũng idempotent.
 *   <li><b>Không double-commit</b>: chuyển trạng thái bằng {@code UPDATE … WHERE status =
 *       'PENDING'} ⇒ hai request đồng thời chỉ một cái thắng.
 * </ol>
 */
@Service
public class ReservationService {

  private final ReservationRepository reservations;
  private final StockRepository stock;
  private final StockLedgerRepository ledger;
  private final InventoryProperties properties;
  private final Clock clock;

  public ReservationService(
      ReservationRepository reservations,
      StockRepository stock,
      StockLedgerRepository ledger,
      InventoryProperties properties,
      Clock clock) {
    this.reservations = reservations;
    this.stock = stock;
    this.ledger = ledger;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Kết quả reserve. {@code created = false} nghĩa là đây là <b>replay</b> của một lần reserve
   * trước đó (cùng {@code orderId}) — controller dùng nó để trả 200 thay vì 201.
   */
  public record ReserveOutcome(
      ReservationRecord reservation, List<ReservationItemRecord> items, boolean created) {}

  /**
   * Giữ chỗ cho toàn bộ item của một order.
   *
   * @throws InsufficientStockException (409) khi có SKU không đủ hàng — cả order rollback
   * @throws AppException (400) khi items có SKU trùng nhau (dữ liệu mơ hồ: 2 dòng cùng SKU thì qty
   *     cộng lại hay dòng sau ghi đè?)
   */
  @Transactional
  public ReserveOutcome reserve(UUID orderId, List<ReservationItemRecord> requested) {
    List<ReservationItemRecord> items = normalize(requested);
    Instant now = Instant.now(clock);

    // Idempotency: retry cùng orderId ⇒ trả đúng reservation đang PENDING, KHÔNG giữ chỗ lần 2.
    ReservationRecord existing = reservations.findPendingByOrderId(orderId).orElse(null);
    if (existing != null) {
      return new ReserveOutcome(existing, reservations.findItems(existing.id()), false);
    }

    UUID reservationId = UUID.randomUUID();
    Instant expiresAt = now.plus(properties.reservationTtl());
    ReservationRecord created =
        new ReservationRecord(reservationId, orderId, ReservationStatus.PENDING, expiresAt, now);
    reservations.insert(created, items);

    String warehouse = properties.warehouseId();
    for (ReservationItemRecord item : items) {
      if (stock.reserve(item.skuId(), warehouse, item.qty(), now) == 0) {
        int available = stock.find(item.skuId(), warehouse).map(StockSnapshot::available).orElse(0);
        // Ném ra ⇒ rollback toàn bộ transaction: header + items + ledger của các SKU trước đó biến
        // mất.
        throw new InsufficientStockException(item.skuId(), available, item.qty());
      }
      ledger.append(
          new StockLedgerEntry(
              item.skuId(), warehouse, 0, item.qty(), LedgerReason.RESERVE, reservationId, now));
    }
    return new ReserveOutcome(created, items, true);
  }

  /** Item của reservation — dùng để dựng response cho commit/release. */
  @Transactional(readOnly = true)
  public List<ReservationItemRecord> itemsOf(UUID reservationId) {
    return reservations.findItems(reservationId);
  }

  /**
   * Chốt hàng: hàng rời kho thật ({@code on_hand} và {@code reserved} cùng giảm).
   *
   * <p><b>Idempotent</b>: commit lần 2 trả lại reservation ở trạng thái COMMITTED, không trừ kho
   * lần nữa.
   *
   * <p>{@code noRollbackFor}: khi reservation đã hết hạn, hàm này <b>vừa</b> trả chỗ (release +
   * ledger EXPIRE) <b>vừa</b> ném 410. Mặc định Spring sẽ rollback ⇒ phần trả chỗ bị xoá và hàng bị
   * treo vĩnh viễn. Khai {@code noRollbackFor} để transaction <b>commit</b> phần bù trừ rồi mới trả
   * lỗi ra ngoài.
   *
   * @throws ReservationExpiredException (410) khi quá {@code expiresAt}
   * @throws AppException (409) khi reservation đã release
   */
  @Transactional(noRollbackFor = ReservationExpiredException.class)
  public ReservationRecord commit(UUID reservationId) {
    ReservationRecord reservation = require(reservationId);
    Instant now = Instant.now(clock);

    if (reservation.status() == ReservationStatus.COMMITTED) {
      return reservation;
    }
    if (reservation.status() == ReservationStatus.RELEASED) {
      throw conflict("Reservation đã release, không commit được: " + reservationId);
    }
    if (reservation.status() == ReservationStatus.EXPIRED) {
      throw expired(reservation);
    }

    if (!now.isBefore(reservation.expiresAt())) {
      // Lazy expire: sweeper (SL-2.3) chưa có ở ticket này, và không thể để hàng treo tới lúc nó
      // chạy.
      releaseStock(reservation, now, LedgerReason.EXPIRE);
      reservations.markStatus(
          reservationId, ReservationStatus.PENDING, ReservationStatus.EXPIRED, now);
      throw expired(reservation);
    }

    if (reservations.markStatus(
            reservationId, ReservationStatus.PENDING, ReservationStatus.COMMITTED, now)
        == 0) {
      // Request khác vừa đổi trạng thái trước ta — đọc lại và trả kết quả theo trạng thái thật.
      return commitAfterLosingRace(reservationId);
    }

    String warehouse = properties.warehouseId();
    for (ReservationItemRecord item : reservations.findItems(reservationId)) {
      if (stock.commit(item.skuId(), warehouse, item.qty(), now) == 0) {
        // reserved < qty: dữ liệu đã lệch từ trước. Fail to (rollback cả transaction) thay vì ghi
        // tiếp.
        throw new IllegalStateException(
            "stock.reserved không đủ để commit sku=" + item.skuId() + " qty=" + item.qty());
      }
      ledger.append(
          new StockLedgerEntry(
              item.skuId(),
              warehouse,
              -item.qty(),
              -item.qty(),
              LedgerReason.COMMIT,
              reservationId,
              now));
    }
    return new ReservationRecord(
        reservation.id(),
        reservation.orderId(),
        ReservationStatus.COMMITTED,
        reservation.expiresAt(),
        reservation.createdAt());
  }

  /**
   * Trả chỗ đã giữ ({@code on_hand} không đổi). Dùng khi huỷ đơn hoặc thanh toán fail.
   *
   * <p><b>Idempotent</b>: release lần 2 trả 200 với trạng thái hiện tại. Release reservation <b>đã
   * commit</b> → 409: hàng đã rời kho, muốn trả thì phải là luồng nhập kho (chưa có ở Phase 2).
   */
  @Transactional
  public ReservationRecord release(UUID reservationId) {
    ReservationRecord reservation = require(reservationId);
    Instant now = Instant.now(clock);

    if (reservation.status() == ReservationStatus.RELEASED
        || reservation.status() == ReservationStatus.EXPIRED) {
      return reservation;
    }
    if (reservation.status() == ReservationStatus.COMMITTED) {
      throw conflict("Reservation đã commit, không release được: " + reservationId);
    }

    if (reservations.markStatus(
            reservationId, ReservationStatus.PENDING, ReservationStatus.RELEASED, now)
        == 0) {
      return require(reservationId);
    }
    releaseStock(reservation, now, LedgerReason.RELEASE);
    return new ReservationRecord(
        reservation.id(),
        reservation.orderId(),
        ReservationStatus.RELEASED,
        reservation.expiresAt(),
        reservation.createdAt());
  }

  private ReservationRecord commitAfterLosingRace(UUID reservationId) {
    ReservationRecord current = require(reservationId);
    if (current.status() == ReservationStatus.COMMITTED) {
      return current;
    }
    if (current.status() == ReservationStatus.EXPIRED) {
      throw expired(current);
    }
    if (current.status() == ReservationStatus.RELEASED) {
      throw conflict("Reservation vừa được release bởi request khác: " + reservationId);
    }
    throw new IllegalStateException(
        "Không chuyển được reservation " + reservationId + " sang COMMITTED");
  }

  /** Trả chỗ cho toàn bộ item + ghi ledger. Dùng chung cho release và expire. */
  private void releaseStock(ReservationRecord reservation, Instant now, LedgerReason reason) {
    String warehouse = properties.warehouseId();
    for (ReservationItemRecord item : reservations.findItems(reservation.id())) {
      if (stock.release(item.skuId(), warehouse, item.qty(), now) == 0) {
        throw new IllegalStateException(
            "stock.reserved không đủ để trả chỗ sku=" + item.skuId() + " qty=" + item.qty());
      }
      ledger.append(
          new StockLedgerEntry(
              item.skuId(), warehouse, 0, -item.qty(), reason, reservation.id(), now));
    }
  }

  private ReservationRecord require(UUID reservationId) {
    return reservations
        .findById(reservationId)
        .orElseThrow(() -> new NotFoundException("Reservation không tồn tại: " + reservationId));
  }

  /**
   * Sắp theo {@code skuId} (tránh deadlock khi 2 order giữ cùng bộ SKU theo thứ tự khác nhau), kiểm
   * trùng + trần số item.
   */
  private List<ReservationItemRecord> normalize(List<ReservationItemRecord> requested) {
    if (requested.size() > properties.maxItemsPerReservation()) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "reserve tối đa " + properties.maxItemsPerReservation() + " SKU mỗi lần");
    }
    Set<UUID> seen = new HashSet<>();
    for (ReservationItemRecord item : requested) {
      if (!seen.add(item.skuId())) {
        throw new AppException(
            ErrorCode.VALIDATION_ERROR, "skuId bị lặp trong items: " + item.skuId());
      }
    }
    return requested.stream().sorted(Comparator.comparing(ReservationItemRecord::skuId)).toList();
  }

  private static AppException conflict(String message) {
    return new AppException(ErrorCode.CONFLICT, message);
  }

  private static ReservationExpiredException expired(ReservationRecord reservation) {
    return new ReservationExpiredException(
        "Reservation đã hết hạn lúc " + reservation.expiresAt() + ": " + reservation.id());
  }
}
