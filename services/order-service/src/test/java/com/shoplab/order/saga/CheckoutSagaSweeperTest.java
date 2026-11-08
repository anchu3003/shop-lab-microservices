package com.shoplab.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.order.config.SagaProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit test cho nhịp quét của {@link CheckoutSagaSweeper} (`SL-2.6b`). */
class CheckoutSagaSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

  private final CheckoutSagaRepository sagas = mock(CheckoutSagaRepository.class);
  private final CheckoutFinishService finish = mock(CheckoutFinishService.class);

  private final SagaProperties properties =
      new SagaProperties(
          "http://cart:8083",
          "http://catalog:8082",
          "http://inventory:8084",
          "http://promotion:8086",
          "http://payment:8088",
          Duration.ofSeconds(2),
          Duration.ofMillis(800),
          "VND",
          Duration.ofMinutes(15),
          Duration.ofSeconds(60),
          Duration.ofSeconds(30),
          10);

  private final CheckoutSagaSweeper sweeper =
      new CheckoutSagaSweeper(sagas, finish, properties, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("quét đúng cutoff = now - payment-window, đếm số đơn đã expire")
  void expiresDueSagas() {
    CheckoutSagaRecord first = saga();
    CheckoutSagaRecord second = saga();
    when(sagas.findExpirable(any(), anyInt())).thenReturn(List.of(first, second));
    when(finish.expire(first.orderId())).thenReturn(true);
    when(finish.expire(second.orderId())).thenReturn(false);

    int expired = sweeper.expireDue();

    assertThat(expired).isEqualTo(1);
    verify(sagas).findExpirable(eq(NOW.minus(Duration.ofMinutes(15))), eq(10));
  }

  @Test
  @DisplayName("1 đơn lỗi không làm chết cả nhịp quét")
  void oneFailureDoesNotStopTheSweep() {
    CheckoutSagaRecord broken = saga();
    CheckoutSagaRecord ok = saga();
    when(sagas.findExpirable(any(), anyInt())).thenReturn(List.of(broken, ok));
    when(finish.expire(broken.orderId())).thenThrow(new IllegalStateException("db chập chờn"));
    when(finish.expire(ok.orderId())).thenReturn(true);

    assertThat(sweeper.expireDue()).isEqualTo(1);
    verify(finish).expire(ok.orderId());
  }

  private static CheckoutSagaRecord saga() {
    return new CheckoutSagaRecord(
        UUID.randomUUID(),
        UUID.randomUUID(),
        CheckoutSagaState.COMPLETED_START,
        true,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "http://pay",
        null,
        NOW,
        NOW);
  }
}
