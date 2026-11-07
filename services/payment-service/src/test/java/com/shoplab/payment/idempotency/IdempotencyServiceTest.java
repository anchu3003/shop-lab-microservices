package com.shoplab.payment.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.payment.config.PaymentProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test ngữ nghĩa idempotency (ADR-0006 D2) — 4 nhánh: mới, replay, key dùng lại, đang chạy. */
class IdempotencyServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String SCOPE = "user-1:POST /v1/payment-intents";
  private static final String KEY = "key-1";
  private static final String HASH = "hash-A";

  private final IdempotencyRepository repository = mock(IdempotencyRepository.class);
  private final IdempotencyService service =
      new IdempotencyService(
          repository,
          new PaymentProperties(
              Duration.ofMinutes(15),
              "mock",
              "http://localhost:8080/mock-provider/pay",
              "test-secret",
              Duration.ofMinutes(5),
              true,
              Duration.ofMillis(1),
              Duration.ofHours(24),
              Duration.ofHours(1),
              200),
          Clock.fixed(NOW, ZoneOffset.UTC));

  private static IdempotencyRecord record(IdempotencyStatus status, String hash) {
    return new IdempotencyRecord(
        SCOPE,
        KEY,
        hash,
        status,
        status == IdempotencyStatus.COMPLETED ? 201 : null,
        status == IdempotencyStatus.COMPLETED ? "{\"id\":\"x\"}" : null,
        UUID.randomUUID(),
        NOW,
        status == IdempotencyStatus.COMPLETED ? NOW : null);
  }

  @Test
  @DisplayName("lần đầu: insert được → empty (xử lý tiếp)")
  void firstRequestShouldProceed() {
    when(repository.insertIfAbsent(SCOPE, KEY, HASH, NOW)).thenReturn(1);

    assertThat(service.begin(SCOPE, KEY, HASH)).isEmpty();
  }

  @Test
  @DisplayName("retry cùng hash + COMPLETED → trả record để replay response cũ")
  void retrySamePayloadShouldReplay() {
    when(repository.insertIfAbsent(SCOPE, KEY, HASH, NOW)).thenReturn(0);
    when(repository.find(SCOPE, KEY))
        .thenReturn(Optional.of(record(IdempotencyStatus.COMPLETED, HASH)));

    Optional<IdempotencyRecord> result = service.begin(SCOPE, KEY, HASH);

    assertThat(result).isPresent();
    assertThat(result.get().statusCode()).isEqualTo(201);
  }

  @Test
  @DisplayName("cùng key nhưng payload khác → 422 (client phải sửa request)")
  void keyReuseWithDifferentPayloadShouldFail422() {
    when(repository.insertIfAbsent(SCOPE, KEY, "hash-B", NOW)).thenReturn(0);
    when(repository.find(SCOPE, KEY))
        .thenReturn(Optional.of(record(IdempotencyStatus.COMPLETED, HASH)));

    assertThatThrownBy(() -> service.begin(SCOPE, KEY, "hash-B"))
        .isInstanceOf(IdempotencyExceptions.KeyReused.class)
        .hasMessageContaining(KEY);
  }

  @Test
  @DisplayName("row còn IN_PROGRESS → 409 (không xử lý song song 2 lần)")
  void inProgressShouldFail409() {
    when(repository.insertIfAbsent(SCOPE, KEY, HASH, NOW)).thenReturn(0);
    when(repository.find(SCOPE, KEY))
        .thenReturn(Optional.of(record(IdempotencyStatus.IN_PROGRESS, HASH)));

    assertThatThrownBy(() -> service.begin(SCOPE, KEY, HASH))
        .isInstanceOf(IdempotencyExceptions.InProgress.class);
  }

  @Test
  @DisplayName("thiếu key / key quá 64 ký tự → 400, không đụng DB")
  void missingKeyShouldFail400() {
    assertThatThrownBy(() -> service.begin(SCOPE, "  ", HASH))
        .isInstanceOf(IdempotencyExceptions.MissingIdempotencyKey.class);
    assertThatThrownBy(() -> service.begin(SCOPE, "k".repeat(65), HASH))
        .isInstanceOf(IdempotencyExceptions.MissingIdempotencyKey.class);
    verify(repository, never()).insertIfAbsent(any(), any(), any(), any());
  }

  @Test
  @DisplayName("hash ổn định: cùng payload → cùng SHA-256; khác payload → khác hash")
  void hashShouldBeStable() {
    assertThat(IdempotencyService.hash("{\"a\":1}"))
        .isEqualTo(IdempotencyService.hash("{\"a\":1}"));
    assertThat(IdempotencyService.hash("{\"a\":1}"))
        .isNotEqualTo(IdempotencyService.hash("{\"a\":2}"));
    assertThat(IdempotencyService.hash("x")).hasSize(64);
  }

  @Test
  @DisplayName("cleanup: truyền đúng 2 ngưỡng tuổi (retention 24h, stale 1h) + batch size")
  void cleanupShouldUseConfiguredWindows() {
    when(repository.deleteOlderThan(
            NOW.minus(Duration.ofHours(24)), NOW.minus(Duration.ofHours(1)), 200))
        .thenReturn(3);

    assertThat(service.cleanup()).isEqualTo(3);
    verify(repository)
        .deleteOlderThan(
            eq(NOW.minus(Duration.ofHours(24))), eq(NOW.minus(Duration.ofHours(1))), eq(200));
  }
}
