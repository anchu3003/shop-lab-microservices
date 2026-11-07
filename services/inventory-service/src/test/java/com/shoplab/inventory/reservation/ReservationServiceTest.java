package com.shoplab.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.inventory.config.InventoryProperties;
import com.shoplab.inventory.ledger.LedgerReason;
import com.shoplab.inventory.ledger.StockLedgerEntry;
import com.shoplab.inventory.ledger.StockLedgerRepository;
import com.shoplab.inventory.stock.InsufficientStockException;
import com.shoplab.inventory.stock.StockRepository;
import com.shoplab.inventory.stock.StockSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test nghiệp vụ reserve/commit/release — mock 3 repository, không cần DB.
 *
 * <p>Cái <b>không</b> kiểm được ở đây (phải để IT): rollback thật của transaction và tính nguyên tử
 * khi nhiều request đồng thời. Ở đây kiểm <b>quyết định</b>: khi nào gọi cái gì, khi nào ném lỗi
 * gì.
 */
class ReservationServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String WAREHOUSE = "WH-1";
  private static final UUID SKU_A = UUID.fromString("11111111-1111-4111-8111-000000000001");
  private static final UUID SKU_B = UUID.fromString("11111111-1111-4111-8111-000000000002");

  private final ReservationRepository reservations = mock(ReservationRepository.class);
  private final StockRepository stock = mock(StockRepository.class);
  private final StockLedgerRepository ledger = mock(StockLedgerRepository.class);

  private final ReservationService service =
      new ReservationService(
          reservations,
          stock,
          ledger,
          new InventoryProperties(WAREHOUSE, Duration.ofMinutes(15), 50),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("reserve: giữ đủ item, TTL 15', ghi ledger RESERVE cho từng SKU, trả created=true")
  void reserveShouldHoldStockAndWriteLedger() {
    UUID orderId = UUID.randomUUID();
    when(reservations.findPendingByOrderId(orderId)).thenReturn(Optional.empty());
    when(stock.reserve(eq(SKU_A), eq(WAREHOUSE), eq(2), any())).thenReturn(1);
    when(stock.reserve(eq(SKU_B), eq(WAREHOUSE), eq(1), any())).thenReturn(1);

    ReservationService.ReserveOutcome outcome =
        service.reserve(
            orderId,
            List.of(new ReservationItemRecord(SKU_B, 1), new ReservationItemRecord(SKU_A, 2)));

    assertThat(outcome.created()).isTrue();
    assertThat(outcome.reservation().status()).isEqualTo(ReservationStatus.PENDING);
    assertThat(outcome.reservation().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    // Sắp theo skuId để tránh deadlock khi 2 order giữ cùng bộ SKU theo thứ tự khác nhau.
    assertThat(outcome.items())
        .containsExactly(new ReservationItemRecord(SKU_A, 2), new ReservationItemRecord(SKU_B, 1));

    ArgumentCaptor<StockLedgerEntry> entries = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger, times(2)).append(entries.capture());
    assertThat(entries.getAllValues())
        .allSatisfy(
            entry -> {
              assertThat(entry.reason()).isEqualTo(LedgerReason.RESERVE);
              assertThat(entry.deltaOnHand()).isZero();
              assertThat(entry.deltaReserved()).isPositive();
              assertThat(entry.refId()).isEqualTo(outcome.reservation().id());
            });
  }

  @Test
  @DisplayName("reserve cùng orderId lần 2 → trả reservation cũ, KHÔNG giữ chỗ lần nữa")
  void reserveShouldBeIdempotentByOrderId() {
    UUID orderId = UUID.randomUUID();
    ReservationRecord existing =
        new ReservationRecord(
            UUID.randomUUID(), orderId, ReservationStatus.PENDING, NOW.plusSeconds(600), NOW);
    when(reservations.findPendingByOrderId(orderId)).thenReturn(Optional.of(existing));
    when(reservations.findItems(existing.id()))
        .thenReturn(List.of(new ReservationItemRecord(SKU_A, 3)));

    ReservationService.ReserveOutcome outcome =
        service.reserve(orderId, List.of(new ReservationItemRecord(SKU_A, 3)));

    assertThat(outcome.created()).isFalse();
    assertThat(outcome.reservation().id()).isEqualTo(existing.id());
    verifyNoInteractions(stock);
    verify(reservations, never()).insert(any(), any());
  }

  @Test
  @DisplayName("reserve thiếu hàng ở SKU thứ 2 → 409 kèm skuId + available (cả order fail)")
  void reserveShouldFailWhenAnySkuIsShort() {
    UUID orderId = UUID.randomUUID();
    when(reservations.findPendingByOrderId(orderId)).thenReturn(Optional.empty());
    when(stock.reserve(eq(SKU_A), eq(WAREHOUSE), anyInt(), any())).thenReturn(1);
    when(stock.reserve(eq(SKU_B), eq(WAREHOUSE), anyInt(), any())).thenReturn(0);
    when(stock.find(SKU_B, WAREHOUSE))
        .thenReturn(Optional.of(StockSnapshot.of(SKU_B, WAREHOUSE, 1, 0)));

    assertThatThrownBy(
            () ->
                service.reserve(
                    orderId,
                    List.of(
                        new ReservationItemRecord(SKU_A, 1), new ReservationItemRecord(SKU_B, 5))))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining(SKU_B.toString())
        .extracting(ex -> ((InsufficientStockException) ex).available())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("reserve trùng skuId trong items → 400 (qty cộng lại hay ghi đè? phải trả lỗi)")
  void reserveShouldRejectDuplicateSku() {
    assertThatThrownBy(
            () ->
                service.reserve(
                    UUID.randomUUID(),
                    List.of(
                        new ReservationItemRecord(SKU_A, 1), new ReservationItemRecord(SKU_A, 2))))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("lặp");
  }

  @Test
  @DisplayName("commit: chuyển PENDING→COMMITTED, trừ on_hand + reserved, ghi ledger COMMIT")
  void commitShouldMoveStockAndWriteLedger() {
    ReservationRecord pending = pendingReservation(600);
    when(reservations.findById(pending.id())).thenReturn(Optional.of(pending));
    when(reservations.markStatus(
            pending.id(), ReservationStatus.PENDING, ReservationStatus.COMMITTED, NOW))
        .thenReturn(1);
    when(reservations.findItems(pending.id()))
        .thenReturn(List.of(new ReservationItemRecord(SKU_A, 2)));
    when(stock.commit(SKU_A, WAREHOUSE, 2, NOW)).thenReturn(1);

    ReservationRecord result = service.commit(pending.id());

    assertThat(result.status()).isEqualTo(ReservationStatus.COMMITTED);
    ArgumentCaptor<StockLedgerEntry> entry = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger).append(entry.capture());
    assertThat(entry.getValue().reason()).isEqualTo(LedgerReason.COMMIT);
    assertThat(entry.getValue().deltaOnHand()).isEqualTo(-2);
    assertThat(entry.getValue().deltaReserved()).isEqualTo(-2);
  }

  @Test
  @DisplayName("commit lần 2 (đã COMMITTED) → 200, KHÔNG trừ kho lần nữa")
  void commitShouldBeIdempotent() {
    ReservationRecord committed =
        new ReservationRecord(
            UUID.randomUUID(), UUID.randomUUID(), ReservationStatus.COMMITTED, NOW, NOW);
    when(reservations.findById(committed.id())).thenReturn(Optional.of(committed));

    assertThat(service.commit(committed.id()).status()).isEqualTo(ReservationStatus.COMMITTED);
    verifyNoInteractions(stock);
    verify(ledger, never()).append(any());
  }

  @Test
  @DisplayName("commit reservation quá hạn → 410 + trả chỗ ngay (lazy expire, ledger EXPIRE)")
  void commitShouldExpireWhenTtlPassed() {
    ReservationRecord expiredTtl = pendingReservation(-1); // hết hạn 1 giây trước NOW
    when(reservations.findById(expiredTtl.id())).thenReturn(Optional.of(expiredTtl));
    when(reservations.findItems(expiredTtl.id()))
        .thenReturn(List.of(new ReservationItemRecord(SKU_A, 2)));
    when(stock.release(SKU_A, WAREHOUSE, 2, NOW)).thenReturn(1);

    assertThatThrownBy(() -> service.commit(expiredTtl.id()))
        .isInstanceOf(ReservationExpiredException.class)
        .hasMessageContaining("hết hạn");

    verify(reservations)
        .markStatus(expiredTtl.id(), ReservationStatus.PENDING, ReservationStatus.EXPIRED, NOW);
    ArgumentCaptor<StockLedgerEntry> entry = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger).append(entry.capture());
    assertThat(entry.getValue().reason()).isEqualTo(LedgerReason.EXPIRE);
    assertThat(entry.getValue().deltaReserved()).isEqualTo(-2);
  }

  @Test
  @DisplayName("commit reservation đã RELEASED → 409")
  void commitShouldRejectReleasedReservation() {
    ReservationRecord released =
        new ReservationRecord(
            UUID.randomUUID(), UUID.randomUUID(), ReservationStatus.RELEASED, NOW, NOW);
    when(reservations.findById(released.id())).thenReturn(Optional.of(released));

    assertThatThrownBy(() -> service.commit(released.id()))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("đã release");
    verifyNoInteractions(stock);
  }

  @Test
  @DisplayName("release: trả chỗ, on_hand KHÔNG đổi; lần 2 idempotent; đã commit → 409")
  void releaseShouldReturnStockAndBeIdempotent() {
    ReservationRecord pending = pendingReservation(600);
    when(reservations.findById(pending.id())).thenReturn(Optional.of(pending));
    when(reservations.markStatus(
            pending.id(), ReservationStatus.PENDING, ReservationStatus.RELEASED, NOW))
        .thenReturn(1);
    when(reservations.findItems(pending.id()))
        .thenReturn(List.of(new ReservationItemRecord(SKU_A, 2)));
    when(stock.release(SKU_A, WAREHOUSE, 2, NOW)).thenReturn(1);

    assertThat(service.release(pending.id()).status()).isEqualTo(ReservationStatus.RELEASED);

    ArgumentCaptor<StockLedgerEntry> entry = ArgumentCaptor.forClass(StockLedgerEntry.class);
    verify(ledger).append(entry.capture());
    assertThat(entry.getValue().deltaOnHand()).isZero();
    assertThat(entry.getValue().deltaReserved()).isEqualTo(-2);

    // release lần 2: đã RELEASED ⇒ trả luôn, không gọi stock.release lần nữa
    when(reservations.findById(pending.id()))
        .thenReturn(Optional.of(withStatus(pending, ReservationStatus.RELEASED)));
    assertThat(service.release(pending.id()).status()).isEqualTo(ReservationStatus.RELEASED);
    verify(stock, times(1)).release(any(), any(), anyInt(), any());

    // release reservation đã commit ⇒ 409 (hàng đã rời kho, không thể "trả chỗ")
    when(reservations.findById(pending.id()))
        .thenReturn(Optional.of(withStatus(pending, ReservationStatus.COMMITTED)));
    assertThatThrownBy(() -> service.release(pending.id()))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("đã commit");
  }

  @Test
  @DisplayName("reservation không tồn tại → 404")
  void shouldThrowNotFound() {
    UUID missing = UUID.randomUUID();
    when(reservations.findById(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.commit(missing)).isInstanceOf(NotFoundException.class);
  }

  private ReservationRecord pendingReservation(long ttlSeconds) {
    return new ReservationRecord(
        UUID.randomUUID(),
        UUID.randomUUID(),
        ReservationStatus.PENDING,
        NOW.plusSeconds(ttlSeconds),
        NOW.minusSeconds(60));
  }

  private static ReservationRecord withStatus(ReservationRecord source, ReservationStatus status) {
    return new ReservationRecord(
        source.id(), source.orderId(), status, source.expiresAt(), source.createdAt());
  }
}
