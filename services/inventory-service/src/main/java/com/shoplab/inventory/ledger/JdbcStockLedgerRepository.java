package com.shoplab.inventory.ledger;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi ledger bằng JDBC thuần — bảng này chỉ có INSERT, không cần tầng ORM nào.
 *
 * <p>{@code id} sinh ở đây (UUID ngẫu nhiên) thay vì {@code gen_random_uuid()} của Postgres: id do
 * app sinh thì test đọc lại được ngay sau khi insert mà không phải query thêm.
 */
@Repository
public class JdbcStockLedgerRepository implements StockLedgerRepository {

  private final JdbcClient jdbc;

  public JdbcStockLedgerRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void append(StockLedgerEntry entry) {
    jdbc.sql(
            """
            insert into stock_ledger
              (id, sku_id, warehouse_id, delta_on_hand, delta_reserved, reason, ref_id, created_at)
            values
              (:id, :skuId, :warehouseId, :deltaOnHand, :deltaReserved, :reason, :refId, :createdAt)
            """)
        .param("id", UUID.randomUUID())
        .param("skuId", entry.skuId())
        .param("warehouseId", entry.warehouseId())
        .param("deltaOnHand", entry.deltaOnHand())
        .param("deltaReserved", entry.deltaReserved())
        .param("reason", entry.reason().name())
        .param("refId", entry.refId(), java.sql.Types.OTHER)
        .param("createdAt", java.sql.Timestamp.from(entry.createdAt()))
        .update();
  }
}
