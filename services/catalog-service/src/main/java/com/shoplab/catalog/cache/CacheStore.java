package com.shoplab.catalog.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cổng duy nhất tới cache — tách khỏi Redis để unit test được toàn bộ logic cache-aside mà không
 * cần container (IT mới dùng Redis thật).
 *
 * <p><b>Hợp đồng lỗi</b>: mọi sự cố hạ tầng phải ném {@link CacheUnavailableException}, không được
 * nuốt thành "miss". Nuốt lỗi là bẫy: lúc Redis chết, {@code lock()} trả {@code false} sẽ bị hiểu
 * là "có người đang đọc DB" ⇒ mọi request ngủ chờ vô ích.
 */
public interface CacheStore {

  /** Đọc 1 key. Rỗng = không có (hoặc đã hết hạn), <b>không</b> phải lỗi. */
  Optional<String> get(String key);

  /**
   * Đọc nhiều key trong <b>một</b> round-trip (MGET). Trả list cùng thứ tự với {@code keys} — phần
   * tử rỗng ở vị trí miss.
   */
  List<Optional<String>> getAll(List<String> keys);

  /** Ghi key với TTL. */
  void put(String key, String value, Duration ttl);

  /**
   * Thử giữ lock single-flight ({@code SET NX PX}).
   *
   * @return {@code true} nếu giành được lock; {@code false} nếu người khác đang giữ
   */
  boolean lock(String key, String token, Duration ttl);

  /**
   * Nhả lock <b>chỉ khi</b> token còn khớp (compare-and-delete bằng Lua).
   *
   * <p>Không có bước so token thì một request chậm có thể xoá lock của request khác đã giành được
   * sau khi lock cũ hết hạn ⇒ 2 request cùng đọc DB (đúng thứ single-flight muốn chặn).
   */
  void unlock(String key, String token);

  /** Xoá 1 key. Trả {@code true} nếu key tồn tại và đã bị xoá. */
  boolean delete(String key);

  /** Xoá nhiều key trong một lệnh (DEL nhận nhiều tham số). */
  void deleteAll(Collection<String> keys);

  /** Tăng key đếm (dùng cho generation của list) và trả giá trị mới. */
  long increment(String key);
}
