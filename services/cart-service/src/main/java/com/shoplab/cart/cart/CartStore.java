package com.shoplab.cart.cart;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Truy cập state giỏ hàng.
 *
 * <p>Tách interface khỏi Redis để unit test service bằng store giả (không cần Redis cho logic); còn
 * <b>Lua</b> — phần thực sự khó — chỉ chứng minh được trên Redis thật ở IT.
 *
 * <p>Mọi phép ghi phải <b>nguyên tử</b> và phải tự làm mới TTL: đọc-ở-Java-rồi-ghi-lại là lost
 * update khi hai request cùng SKU chạy song song.
 */
public interface CartStore {

  /** Ảnh chụp giỏ + TTL còn lại (giây). Giỏ chưa tồn tại ⇒ items rỗng, ttl 0. */
  Snapshot snapshot(CartOwner owner);

  /**
   * Đặt số lượng tuyệt đối cho một dòng (idempotent).
   *
   * @return {@code false} khi giỏ đã đủ {@code maxLines} dòng và đây là dòng mới
   */
  boolean setItem(CartOwner owner, UUID skuId, int qty, Instant now);

  void removeItem(CartOwner owner, UUID skuId);

  void clear(CartOwner owner);

  /** Ghi nhận guest token vừa cấp (đánh dấu "token này do server phát"). */
  void issueGuestToken(String token, Instant now);

  boolean isIssuedGuestToken(String token);

  void revokeGuestToken(String token);

  /**
   * Cộng dồn giỏ guest vào giỏ user rồi xoá giỏ guest — <b>một Lua script</b>.
   *
   * @return số dòng đã cộng vào giỏ user (dòng bị bỏ vì giỏ user đã đầy không tính)
   */
  int mergeGuestIntoUser(CartOwner user, CartOwner guest, Instant now);

  /** Ảnh chụp giỏ. */
  record Snapshot(Map<UUID, CartItem> items, long ttlSeconds) {}
}
