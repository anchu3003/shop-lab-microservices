package com.shoplab.inventory.stock;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.inventory.config.InventoryProperties;
import com.shoplab.inventory.ledger.LedgerReason;
import com.shoplab.inventory.ledger.StockLedgerEntry;
import com.shoplab.inventory.ledger.StockLedgerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ tồn kho phía admin/đọc: đặt tồn kho thật và xem tồn kho.
 *
 * <p>Reserve/commit/release nằm ở {@code ReservationService} vì chúng luôn đi kèm vòng đời
 * reservation — tách ra đây chỉ để mỗi class một trách nhiệm.
 */
@Service
public class StockService {

  private final StockRepository stock;
  private final StockLedgerRepository ledger;
  private final InventoryProperties properties;
  private final Clock clock;

  public StockService(
      StockRepository stock,
      StockLedgerRepository ledger,
      InventoryProperties properties,
      Clock clock) {
    this.stock = stock;
    this.ledger = ledger;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Đặt tồn kho thật cho 1 SKU (admin) — tạo row nếu chưa có.
   *
   * <p>Dùng {@code SELECT … FOR UPDATE} để delta ghi vào ledger đúng cả khi 2 admin sửa cùng lúc:
   * số cũ được đọc trong cùng lock với lần ghi.
   *
   * @throws AppException 409 khi {@code onHand} nhỏ hơn số đang giữ chỗ (sẽ phá bất biến {@code
   *     reserved <= on_hand}) — báo rõ thay vì để DB ném lỗi 500
   */
  @Transactional
  public StockSnapshot setOnHand(UUID skuId, int onHand) {
    Instant now = Instant.now(clock);
    String warehouse = properties.warehouseId();

    int previousOnHand = 0;
    int reserved = 0;
    Optional<StockSnapshot> current = stock.findForUpdate(skuId, warehouse);
    if (current.isPresent()) {
      StockSnapshot existing = current.get();
      if (onHand < existing.reserved()) {
        throw new AppException(
            ErrorCode.CONFLICT,
            "on_hand không được nhỏ hơn số đang giữ chỗ (" + existing.reserved() + ")");
      }
      previousOnHand = existing.onHand();
      reserved = existing.reserved();
      stock.setOnHand(skuId, warehouse, onHand, now);
    } else {
      stock.insertStock(skuId, warehouse, onHand, now);
    }

    ledger.append(
        new StockLedgerEntry(
            skuId, warehouse, onHand - previousOnHand, 0, LedgerReason.ADJUST, null, now));
    return StockSnapshot.of(skuId, warehouse, onHand, reserved);
  }

  /** Xem tồn kho — {@code available} là con số client cần để hiển thị "còn hàng". */
  @Transactional(readOnly = true)
  public StockSnapshot snapshot(UUID skuId) {
    return stock
        .find(skuId, properties.warehouseId())
        .orElseThrow(() -> new NotFoundException("SKU chưa có tồn kho: " + skuId));
  }
}
