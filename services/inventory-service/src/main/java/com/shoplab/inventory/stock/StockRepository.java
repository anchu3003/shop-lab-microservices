package com.shoplab.inventory.stock;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập bảng {@code stock}.
 *
 * <p><b>Vì sao các method mutate trả {@code int}</b>: đó là {@code rowsAffected} — với {@link
 * #reserve} nó chính là câu trả lời "có giữ được chỗ không". Không có nó thì phải SELECT trước rồi
 * UPDATE sau, tức là tạo lại đúng cái khe hở mà ADR-0006 D1 muốn xoá.
 *
 * <p>Tách interface khỏi impl JDBC để unit test service không cần DB (mock interface), còn phần SQL
 * thì được kiểm bằng integration test trên Postgres thật.
 */
public interface StockRepository {

  /** Đọc tồn kho (không lock). */
  Optional<StockSnapshot> find(UUID skuId, String warehouseId);

  /**
   * Đọc tồn kho và giữ row lock tới hết transaction — dùng cho đường admin sửa tay, nơi cần biết
   * {@code on_hand} cũ để ghi ledger {@code ADJUST} đúng delta.
   */
  Optional<StockSnapshot> findForUpdate(UUID skuId, String warehouseId);

  /** Tạo row tồn kho mới (chưa có). */
  void insertStock(UUID skuId, String warehouseId, int onHand, Instant now);

  /** Đặt lại {@code on_hand} (không đụng {@code reserved}). */
  void setOnHand(UUID skuId, String warehouseId, int onHand, Instant now);

  /**
   * Giữ chỗ: {@code reserved += qty} <b>chỉ khi</b> {@code on_hand - reserved >= qty}.
   *
   * @return 1 nếu giữ được, 0 nếu không đủ hàng (hoặc SKU chưa có trong kho)
   */
  int reserve(UUID skuId, String warehouseId, int qty, Instant now);

  /**
   * Chốt hàng: {@code on_hand -= qty} và {@code reserved -= qty} trong cùng 1 câu lệnh.
   *
   * @return 1 nếu thành công, 0 nếu dữ liệu không nhất quán ({@code reserved < qty})
   */
  int commit(UUID skuId, String warehouseId, int qty, Instant now);

  /**
   * Trả chỗ đã giữ: {@code reserved -= qty}, {@code on_hand} không đổi.
   *
   * @return 1 nếu thành công, 0 nếu {@code reserved < qty}
   */
  int release(UUID skuId, String warehouseId, int qty, Instant now);
}
