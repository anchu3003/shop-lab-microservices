package com.shoplab.order.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.order.idempotency.IdempotencyRecord;
import com.shoplab.order.idempotency.IdempotencyService;
import com.shoplab.order.idempotency.IdempotencyStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

/** Test nghiệp vụ đơn hàng: tạo idempotent + state machine + quyền đọc. */
class OrderServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-000000000001");
  private static final UUID OTHER = UUID.fromString("11111111-1111-4111-8111-000000000002");

  private final OrderRepository orders = mock(OrderRepository.class);
  private final OrderWriter writer = mock(OrderWriter.class);
  private final IdempotencyService idempotency = mock(IdempotencyService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final OrderService service =
      new OrderService(orders, writer, idempotency, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

  private static CreateOrderRequest request() {
    return new CreateOrderRequest(
        List.of(
            new CreateOrderRequest.Line(
                UUID.randomUUID(), "SKU-1", "Áo thun", new BigDecimal("100000"), 2)),
        new BigDecimal("20000"),
        "VND");
  }

  private static OrderRecord order(UUID userId, OrderStatus status) {
    return new OrderRecord(
        UUID.randomUUID(),
        userId,
        status,
        new BigDecimal("200000"),
        new BigDecimal("20000"),
        new BigDecimal("180000"),
        "VND",
        "key-1",
        null,
        NOW,
        NOW);
  }

  @Test
  @DisplayName("tạo đơn: lưu snapshot, tính tiền ở server, ghi history, hoàn tất idempotency")
  void createShouldPersistSnapshotAndCompleteKey() {
    when(idempotency.begin(any(), eq("key-1"), any())).thenReturn(Optional.empty());

    OrderService.CreateResult result = service.create(USER, "key-1", request());

    assertThat(result.replayed()).isFalse();
    assertThat(result.response().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    assertThat(result.response().subtotal()).isEqualByComparingTo("200000");
    assertThat(result.response().total()).isEqualByComparingTo("180000");
    assertThat(result.response().items()).hasSize(1);
    assertThat(result.response().items().get(0).lineTotal()).isEqualByComparingTo("200000");

    // create uỷ quyền phần ghi cho OrderWriter (1 transaction: order + history + idempotency)
    verify(writer).create(any(OrderRecord.class), any(), any(), eq("key-1"), any());
    verify(orders, never()).insert(any(), any());
  }

  @Test
  @DisplayName("retry hợp lệ: đọc lại response đã lưu và đánh dấu replayed, KHÔNG tạo đơn mới")
  void createShouldReplayStoredResponse() throws Exception {
    OrderResponse stored = OrderResponse.from(order(USER, OrderStatus.PENDING_PAYMENT), List.of());
    IdempotencyRecord completed =
        new IdempotencyRecord(
            USER + ":POST /v1/orders",
            "key-1",
            "hash",
            IdempotencyStatus.COMPLETED,
            201,
            objectMapper.writeValueAsString(stored),
            stored.id(),
            NOW,
            NOW);
    when(idempotency.begin(any(), eq("key-1"), any())).thenReturn(Optional.of(completed));

    OrderService.CreateResult result = service.create(USER, "key-1", request());

    assertThat(result.replayed()).isTrue();
    assertThat(result.response().id()).isEqualTo(stored.id());
    verify(writer, never()).create(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("payload sai (discount > subtotal) → 400 và KHÔNG giữ khoá idempotency")
  void invalidPayloadShouldNotConsumeKey() {
    CreateOrderRequest bad =
        new CreateOrderRequest(
            List.of(
                new CreateOrderRequest.Line(
                    UUID.randomUUID(), "S", "T", new BigDecimal("1000"), 1)),
            new BigDecimal("5000"),
            "VND");

    assertThatThrownBy(() -> service.create(USER, "key-1", bad)).isInstanceOf(AppException.class);
    verify(idempotency, never()).begin(any(), any(), any());
  }

  @Test
  @DisplayName("confirm: PENDING_PAYMENT → CONFIRMED + ghi history")
  void confirmShouldTransitionAndRecordHistory() {
    OrderRecord pending = order(USER, OrderStatus.PENDING_PAYMENT);
    when(orders.findById(pending.id())).thenReturn(Optional.of(pending));
    when(orders.markStatus(pending.id(), OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, NOW))
        .thenReturn(1);
    when(orders.findItems(pending.id())).thenReturn(List.of());

    OrderResponse response =
        service.changeStatus(pending.id(), OrderStatus.CONFIRMED, "payment ok", USER, false);

    assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    verify(orders)
        .insertHistory(
            pending.id(), OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, "payment ok", NOW);
  }

  @Test
  @DisplayName("chuyển sang trạng thái hiện tại → 200, không ghi history lần 2 (idempotent)")
  void sameStatusIsIdempotent() {
    OrderRecord confirmed = order(USER, OrderStatus.CONFIRMED);
    when(orders.findById(confirmed.id())).thenReturn(Optional.of(confirmed));
    when(orders.findItems(confirmed.id())).thenReturn(List.of());

    assertThat(
            service.changeStatus(confirmed.id(), OrderStatus.CONFIRMED, null, USER, false).status())
        .isEqualTo(OrderStatus.CONFIRMED);
    verify(orders, never()).insertHistory(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("chuyển sai hướng (đã CONFIRMED mà cancel) → 409")
  void wrongTransitionIsConflict() {
    OrderRecord confirmed = order(USER, OrderStatus.CONFIRMED);
    when(orders.findById(confirmed.id())).thenReturn(Optional.of(confirmed));

    assertThatThrownBy(
            () -> service.changeStatus(confirmed.id(), OrderStatus.CANCELLED, "x", USER, false))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("trạng thái cuối");
  }

  @Test
  @DisplayName("đơn của người khác → 404 (không lộ sự tồn tại); admin đọc được")
  void ownershipIsEnforced() {
    OrderRecord mine = order(USER, OrderStatus.PENDING_PAYMENT);
    when(orders.findById(mine.id())).thenReturn(Optional.of(mine));

    assertThatThrownBy(() -> service.get(mine.id(), OTHER, false))
        .isInstanceOf(NotFoundException.class);
    when(orders.findItems(mine.id())).thenReturn(List.of());
    assertThat(service.get(mine.id(), OTHER, true).id()).isEqualTo(mine.id());
  }

  @Test
  @DisplayName("list: buyer chỉ thấy đơn của mình (ownerFilter = userId), admin thấy tất cả (null)")
  void listShouldScopeByRole() {
    when(orders.findByUser(any(), any(Integer.class), any(Integer.class))).thenReturn(List.of());
    when(orders.countByUser(any())).thenReturn(0L);

    service.list(USER, false, null, null);
    ArgumentCaptor<UUID> filter = ArgumentCaptor.forClass(UUID.class);
    verify(orders).findByUser(filter.capture(), eq(0), eq(20));
    assertThat(filter.getValue()).isEqualTo(USER);

    service.list(USER, true, "1", "5");
    verify(orders).findByUser(eq(null), eq(5), eq(5));
  }
}
