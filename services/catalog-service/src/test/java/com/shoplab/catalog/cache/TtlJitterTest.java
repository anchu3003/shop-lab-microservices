package com.shoplab.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Jitter phải nằm trong biên và <b>không bao giờ</b> cho ra TTL ≤ 0.
 *
 * <p>Vì sao test này quan trọng hơn vẻ ngoài của nó: TTL 0 với Redis là {@code PERSIST} (không hết
 * hạn) — một key rác sống mãi, đúng loại bug chỉ lộ ra sau vài tuần khi memory tăng mà không ai
 * hiểu tại sao.
 */
class TtlJitterTest {

  private static final Duration BASE = Duration.ofSeconds(60);

  @Test
  @DisplayName("percent = 0 → trả nguyên TTL gốc (không phụ thuộc ngẫu nhiên)")
  void zeroPercentReturnsBase() {
    assertThat(TtlJitter.jitter(BASE, 0, () -> 0.0)).isEqualTo(BASE);
    assertThat(TtlJitter.jitter(BASE, 0, () -> 0.99)).isEqualTo(BASE);
  }

  @Test
  @DisplayName("random = 0 → biên dưới 54 s; random → 1 → biên trên 66 s (jitter 10%)")
  void staysWithinBounds() {
    assertThat(TtlJitter.jitter(BASE, 10, () -> 0.0)).isEqualTo(Duration.ofSeconds(54));
    assertThat(TtlJitter.jitter(BASE, 10, () -> 1.0)).isEqualTo(Duration.ofSeconds(66));
  }

  @Test
  @DisplayName("random = 0.5 → đúng TTL gốc (không lệch)")
  void middleIsBase() {
    assertThat(TtlJitter.jitter(BASE, 10, () -> 0.5)).isEqualTo(BASE);
  }

  @Test
  @DisplayName("TTL rất nhỏ + jitter lớn vẫn ≥ 1 ms (không bao giờ ra 0 = PERSIST)")
  void neverReturnsZero() {
    Duration tiny = Duration.ofMillis(1);
    assertThat(TtlJitter.jitter(tiny, 90, () -> 0.0)).isEqualTo(Duration.ofMillis(1));
  }

  @Test
  @DisplayName("jitter phân tán: 200 lần lấy mẫu cho ra nhiều giá trị khác nhau")
  void actuallySpreads() {
    java.util.Set<Duration> seen = new java.util.HashSet<>();
    java.util.Random random = new java.util.Random(42);
    for (int i = 0; i < 200; i++) {
      seen.add(TtlJitter.jitter(BASE, 10, random::nextDouble));
    }
    // Mục tiêu của jitter là PHÁ ĐỒNG PHA: nếu chỉ ra 1-2 giá trị thì nó không làm được việc đó.
    assertThat(seen).hasSizeGreaterThan(50);
  }
}
