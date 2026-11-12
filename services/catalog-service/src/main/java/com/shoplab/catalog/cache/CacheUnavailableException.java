package com.shoplab.catalog.cache;

/**
 * Lỗi hạ tầng cache (Redis) — <b>không</b> phải lỗi nghiệp vụ.
 *
 * <p>Tồn tại để tầng trên phân biệt được 2 tình huống rất khác nhau khi không lấy được lock:
 *
 * <ul>
 *   <li>{@code lock(...) == false}: <b>có</b> request khác đang đọc DB ⇒ chờ cache (đúng ý đồ).
 *   <li>Ném {@code CacheUnavailableException}: Redis không dùng được ⇒ <b>phải</b> đi thẳng DB
 *       ngay, không chờ (chờ = cộng thêm {@code lock-wait} vào p99 của mọi request trong lúc sự
 *       cố).
 * </ul>
 *
 * <p>Gộp 2 trường hợp này thành {@code false} là bẫy: lúc Redis chết, toàn bộ request sẽ ngủ {@code
 * lock-wait} rồi mới đọc DB — cache "fail-open" trên giấy nhưng p99 tệ hơn không có cache.
 */
public class CacheUnavailableException extends RuntimeException {

  public CacheUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
