package com.shoplab.inventory.ledger;

import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng {@code stock_ledger} — bằng chứng của một thay đổi tồn kho.
 *
 * <p>Vì sao cần ledger chứ không chỉ cột {@code on_hand}/{@code reserved}: khi số liệu sai
 * (oversell, reserved bị treo), câu hỏi luôn là <b>ai đã đổi, lúc nào, vì đơn nào</b>. Không có
 * ledger thì chỉ còn cách đoán. Đây cũng là dữ liệu để Phase 4 reconcile khi chuyển sang atomic
 * counter ở Redis.
 *
 * @param refId reservationId của thao tác gây ra thay đổi; {@code null} cho {@code ADJUST} (admin
 *     sửa tay)
 */
public record StockLedgerEntry(
    UUID skuId,
    String warehouseId,
    int deltaOnHand,
    int deltaReserved,
    LedgerReason reason,
    UUID refId,
    Instant createdAt) {}
