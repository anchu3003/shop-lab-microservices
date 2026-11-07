package com.shoplab.inventory.stock;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Cài đặt {@link StockRepository} bằng JDBC — <b>trái tim</b> của việc chống oversell.
 *
 * <p>Nguyên tắc: mọi thao tác đổi tồn kho là <b>một</b> câu {@code UPDATE} có điều kiện, và điều
 * kiện được DB kiểm trong cùng lock của row. Không đọc trước rồi ghi sau, không lock ứng dụng.
 *
 * <p>{@code warehouse_id} luôn nằm trong WHERE (dù Phase 2 chỉ có 1 kho): khi mở multi-warehouse
 * chỉ phải đổi cách chọn kho, không phải sửa SQL.
 */
@Repository
public class JdbcStockRepository implements StockRepository {

  private static final String SELECT =
      "select sku_id, warehouse_id, on_hand, reserved from stock where sku_id = :skuId and warehouse_id = :wh";

  private final JdbcClient jdbc;

  public JdbcStockRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<StockSnapshot> find(UUID skuId, String warehouseId) {
    return jdbc.sql(SELECT)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .query(
            (rs, rowNum) ->
                StockSnapshot.of(
                    rs.getObject("sku_id", UUID.class),
                    rs.getString("warehouse_id"),
                    rs.getInt("on_hand"),
                    rs.getInt("reserved")))
        .optional();
  }

  @Override
  public Optional<StockSnapshot> findForUpdate(UUID skuId, String warehouseId) {
    return jdbc.sql(SELECT + " for update")
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .query(
            (rs, rowNum) ->
                StockSnapshot.of(
                    rs.getObject("sku_id", UUID.class),
                    rs.getString("warehouse_id"),
                    rs.getInt("on_hand"),
                    rs.getInt("reserved")))
        .optional();
  }

  @Override
  public void insertStock(UUID skuId, String warehouseId, int onHand, Instant now) {
    jdbc.sql(
            """
            insert into stock (sku_id, warehouse_id, on_hand, reserved, version, updated_at)
            values (:skuId, :wh, :onHand, 0, 0, :now)
            """)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .param("onHand", onHand)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public void setOnHand(UUID skuId, String warehouseId, int onHand, Instant now) {
    jdbc.sql(
            """
            update stock set on_hand = :onHand, version = version + 1, updated_at = :now
             where sku_id = :skuId and warehouse_id = :wh
            """)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .param("onHand", onHand)
        .param("now", Timestamp.from(now))
        .update();
  }

  /**
   * Giữ chỗ. {@code on_hand - reserved >= :qty} là <b>toàn bộ</b> cơ chế chống oversell: Postgres
   * giữ row lock trong lúc đánh giá điều kiện và ghi, nên 2 request đồng thời không thể cùng thấy
   * "còn hàng".
   */
  @Override
  public int reserve(UUID skuId, String warehouseId, int qty, Instant now) {
    return jdbc.sql(
            """
            update stock
               set reserved = reserved + :qty, version = version + 1, updated_at = :now
             where sku_id = :skuId and warehouse_id = :wh and on_hand - reserved >= :qty
            """)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .param("qty", qty)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public int commit(UUID skuId, String warehouseId, int qty, Instant now) {
    return jdbc.sql(
            """
            update stock
               set on_hand = on_hand - :qty, reserved = reserved - :qty,
                   version = version + 1, updated_at = :now
             where sku_id = :skuId and warehouse_id = :wh and reserved >= :qty
            """)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .param("qty", qty)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public int release(UUID skuId, String warehouseId, int qty, Instant now) {
    return jdbc.sql(
            """
            update stock
               set reserved = reserved - :qty, version = version + 1, updated_at = :now
             where sku_id = :skuId and warehouse_id = :wh and reserved >= :qty
            """)
        .param("skuId", skuId)
        .param("wh", warehouseId)
        .param("qty", qty)
        .param("now", Timestamp.from(now))
        .update();
  }
}
