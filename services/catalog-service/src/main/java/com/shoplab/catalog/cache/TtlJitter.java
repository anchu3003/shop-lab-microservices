package com.shoplab.catalog.cache;

import java.time.Duration;
import java.util.function.DoubleSupplier;

/**
 * Jitter cho TTL: {@code base × (1 ± percent%)}.
 *
 * <p><b>Vì sao cần</b>: nếu mọi key của cùng một loại được ghi trong cùng một khoảng thời gian (vd
 * sau khi deploy, hoặc sau khi bump generation) mà TTL giống hệt nhau thì chúng <b>hết hạn cùng
 * lúc</b> ⇒ đúng một thời điểm mọi key hot cùng miss ⇒ dồn toàn bộ tải xuống DB. Đó là cache
 * stampede ở quy mô tập thể, và single-flight chỉ chặn được trong phạm vi <b>một</b> key.
 *
 * <p>Hàm thuần (nhận nguồn ngẫu nhiên từ ngoài) để test được biên dưới/biên trên mà không phải chờ
 * thời gian thật.
 */
public final class TtlJitter {

  private TtlJitter() {
    throw new AssertionError("utility class — không khởi tạo");
  }

  /**
   * @param base TTL gốc, phải &gt; 0
   * @param percent biên jitter theo % (0..90). 0 ⇒ trả nguyên {@code base}
   * @param random nguồn ngẫu nhiên {@code [0,1)} — production dùng {@code ThreadLocalRandom}
   * @return TTL sau jitter, luôn ≥ 1 ms
   */
  public static Duration jitter(Duration base, int percent, DoubleSupplier random) {
    if (percent <= 0) {
      return base;
    }
    double factor = 1.0 + (random.getAsDouble() * 2.0 - 1.0) * (percent / 100.0);
    long millis = Math.round(base.toMillis() * factor);
    // Chặn sàn 1 ms: TTL 0 với Redis là PERSIST (không hết hạn) — đúng thứ ta không bao giờ muốn.
    return Duration.ofMillis(Math.max(1L, millis));
  }
}
