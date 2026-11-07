package com.shoplab.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.inventory.config.InventoryProperties;
import com.shoplab.inventory.ledger.LedgerReason;
import com.shoplab.inventory.ledger.StockLedgerEntry;
import com.shoplab.inventory.ledger.StockLedgerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit test đường admin/đọc của tồn kho (SL-2.2). */
class StockServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String WAREHOUSE = "WH-1";
  private static final UUID SKU = UUID.fromString("11111111-1111-4111-8111-000000000001");

  private final StockRepository stock = mock(StockRepository.class);
  private final StockLedgerRepository ledger = mock(StockLedgerRepository.class);

  private final StockService service =
      new StockService(
          stock,
          ledger,
          new InventoryProperties(WAREHOUSE, Duration.ofMinutes(15), 50),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("setOnHand SKU chưa có → tạo row + ledger ADJUST bằng đúng onHand")
  void shouldCreateStockAndLedger() {
    when(stock.findForUpdate(SKU, WAREHOUSE)).thenReturn(Optional.empty());

    StockSnapshot snapshot = service.setOnHand(SKU, 10);

    assertThat(snapshot.onHand()).isEqualTo(10);
    assertThat(snapshot.reserved()).isZero();
    assertThat(snapshot.available()).isEqualTo(10);
    verify(stock).insertStock(SKU, WAREHOUSE, 10, NOW);

    ArgumentCaptor<StockLedgerEntry> entry = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger).append(entry.capture());
    assertThat(entry.getValue().reason()).isEqualTo(LedgerReason.ADJUST);
    assertThat(entry.getValue().deltaOnHand()).isEqualTo(10);
    assertThat(entry.getValue().refId()).isNull();
  }

  @Test
  @DisplayName("setOnHand SKU đã có → ledger ghi đúng delta (mới - cũ), giữ nguyên reserved")
  void shouldUpdateStockAndRecordDelta() {
    when(stock.findForUpdate(SKU, WAREHOUSE))
        .thenReturn(Optional.of(StockSnapshot.of(SKU, WAREHOUSE, 5, 2)));

    StockSnapshot snapshot = service.setOnHand(SKU, 8);

    assertThat(snapshot.onHand()).isEqualTo(8);
    assertThat(snapshot.reserved()).isEqualTo(2);
    assertThat(snapshot.available()).isEqualTo(6);
    verify(stock).setOnHand(SKU, WAREHOUSE, 8, NOW);

    ArgumentCaptor<StockLedgerEntry> entry = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger).append(entry.capture());
    assertThat(entry.getValue().deltaOnHand()).isEqualTo(3); // 8 - 5
  }

  @Test
  @DisplayName("setOnHand nhỏ hơn số đang giữ chỗ → 409 (giữ bất biến reserved <= on_hand)")
  void shouldRejectOnHandBelowReserved() {
    when(stock.findForUpdate(SKU, WAREHOUSE))
        .thenReturn(Optional.of(StockSnapshot.of(SKU, WAREHOUSE, 5, 3)));

    assertThatThrownBy(() -> service.setOnHand(SKU, 1))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("không được nhỏ hơn");
    verify(stock, never()).setOnHand(any(), any(), any(Integer.class), any());
    verify(ledger, never()).append(any());
  }

  @Test
  @DisplayName("snapshot SKU chưa có tồn kho → 404")
  void snapshotShouldThrowWhenMissing() {
    when(stock.find(SKU, WAREHOUSE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.snapshot(SKU))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("chưa có tồn kho");
  }

  @Test
  @DisplayName("snapshot trả available = on_hand - reserved")
  void snapshotShouldExposeAvailable() {
    when(stock.find(SKU, WAREHOUSE))
        .thenReturn(Optional.of(StockSnapshot.of(SKU, WAREHOUSE, 10, 4)));

    assertThat(service.snapshot(SKU).available()).isEqualTo(6);
  }
}
