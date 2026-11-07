package com.shoplab.inventory.ledger;

/**
 * Lý do một dòng {@code stock_ledger} tồn tại.
 *
 * <p>Bảng ledger là <b>append-only</b>: sửa/xoá dòng là phá khả năng đối soát. Mỗi thao tác đổi tồn
 * kho phải ghi đúng 1 dòng với 2 delta:
 *
 * <pre>
 *   ADJUST   : delta_on_hand = ±n, delta_reserved =  0   (đặt lại tồn kho thật)
 *   RESERVE  : delta_on_hand =  0, delta_reserved = +q   (giữ chỗ)
 *   COMMIT   : delta_on_hand = -q, delta_reserved = -q   (hàng rời kho)
 *   RELEASE  : delta_on_hand =  0, delta_reserved = -q   (trả chỗ, hàng vẫn còn)
 *   EXPIRE   : delta_on_hand =  0, delta_reserved = -q   (hết hạn giữ chỗ ⇒ như RELEASE)
 * </pre>
 *
 * <p>Nhờ lưu 2 delta riêng mà đối soát được cả hai con số bằng 1 câu {@code SUM}, không phải suy
 * luận dấu theo {@code reason}.
 */
public enum LedgerReason {
  ADJUST,
  RESERVE,
  COMMIT,
  RELEASE,
  EXPIRE
}
