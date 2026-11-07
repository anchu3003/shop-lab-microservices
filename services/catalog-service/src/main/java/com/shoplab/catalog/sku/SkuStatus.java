package com.shoplab.catalog.sku;

/**
 * Vòng đời của SKU (SL-2.11, ADR-0006 D5).
 *
 * <p>SKU {@code ARCHIVED} vẫn <b>còn row</b> — đó là điểm của soft delete: Phase 2 tham chiếu
 * {@code sku_id} từ {@code inventory.stock}/{@code stock_ledger}/{@code order_items} và cần tra
 * ngược lại được. Nhưng SKU archived <b>không được bán</b>.
 */
public enum SkuStatus {
  /** Bán được. */
  ACTIVE,

  /** Product cha đã bị xoá mềm: giữ row, không tạo mới/không bán. */
  ARCHIVED
}
