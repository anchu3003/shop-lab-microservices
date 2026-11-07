package com.shoplab.inventory.ledger;

/** Ghi {@code stock_ledger}. Chỉ có append — cố ý không có update/delete. */
public interface StockLedgerRepository {

  /**
   * Ghi 1 dòng. Gọi trong transaction của thao tác gây ra thay đổi (cùng commit hoặc cùng
   * rollback).
   */
  void append(StockLedgerEntry entry);
}
