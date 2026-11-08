package com.shoplab.commons.serviceauth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client xin **service token** từ identity-service và **cache** nó (SL-3.11).
 *
 * <p><b>Vì sao phải cache</b>: mỗi request nội bộ mà đi xin token sẽ biến identity-service thành
 * điểm nghẽn và điểm chết của toàn hệ (mọi call nội bộ đều phụ thuộc nó) — đúng lúc hệ cần chịu
 * tải. Token sống 5 phút nên cache là hiển nhiên đúng, chỉ cần xử lý đúng phần khó:
 *
 * <ul>
 *   <li>Refresh <b>sớm hơn</b> hạn {@code skew} (mặc định 30 s): token hết hạn giữa đường bay
 *       (request đang chạy) làm call nội bộ 401 — lỗi khó lần vì nó phụ thuộc thời gian.
 *   <li>Nhiều thread cùng thấy token hết hạn ⇒ chỉ <b>một</b> lần xin (khoá quanh lần nạp), tránh
 *       "token stampede".
 *   <li>Lỗi xin token <b>không</b> được cache: ném ra ngoài để caller quyết định (thà lỗi rõ còn
 *       hơn dùng token cũ đã hết hạn).
 * </ul>
 *
 * <p>Test được mà không cần identity: nguồn token là {@link TokenSource} (hàm {@code audience → kết
 * quả}), production dùng HTTP, test dùng lambda đếm số lần gọi.
 */
public class ServiceTokenClient {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceTokenClient.class);

  /** Token lấy được + hạn của nó. */
  public record CachedToken(String value, Instant validUntil) {}

  /** Nguồn token thật (HTTP tới identity) — tách ra để test được phần cache. */
  @FunctionalInterface
  public interface TokenSource {
    CachedToken fetch(String audience);
  }

  private final TokenSource source;
  private final Duration skew;
  private final Supplier<Instant> now;
  private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

  /** Một khoá cho cả lần nạp: số lần xin token là rất nhỏ, đơn giản hơn khoá-theo-audience. */
  private final Object lock = new Object();

  public ServiceTokenClient(TokenSource source, Duration skew) {
    this(source, skew, Instant::now);
  }

  public ServiceTokenClient(TokenSource source, Duration skew, Supplier<Instant> now) {
    this.source = source;
    this.skew = skew == null ? Duration.ofSeconds(30) : skew;
    this.now = now;
  }

  /**
   * Token dùng được cho {@code audience} — lấy từ cache nếu còn hạn (trừ {@code skew}).
   *
   * @throws ServiceTokenException nguồn token lỗi (lỗi <b>không</b> được cache)
   */
  public String tokenFor(String audience) {
    CachedToken cached = cache.get(audience);
    if (cached != null && stillValid(cached)) {
      return cached.value();
    }
    synchronized (lock) {
      // Kiểm lại trong khoá: thread khác có thể vừa nạp xong.
      CachedToken current = cache.get(audience);
      if (current != null && stillValid(current)) {
        return current.value();
      }
      CachedToken fresh = source.fetch(audience);
      cache.put(audience, fresh);
      LOG.debug("lấy service token mới cho audience={} hạn={}", audience, fresh.validUntil());
      return fresh.value();
    }
  }

  /** Xoá cache (dùng khi service đích trả 401 — token có thể đã bị rotate/thu hồi sớm). */
  public void invalidate(String audience) {
    cache.remove(audience);
  }

  private boolean stillValid(CachedToken token) {
    return token.value() != null
        && !token.value().isBlank()
        && now.get().isBefore(token.validUntil().minus(skew));
  }
}
