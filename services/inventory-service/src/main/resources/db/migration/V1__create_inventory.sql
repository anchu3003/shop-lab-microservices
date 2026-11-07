-- =============================================================================
-- V1__create_inventory.sql — schema tồn kho (SL-2.2, ADR-0006 D1)
--
-- Bài toán: KHÔNG được bán quá tồn kho khi nhiều buyer tranh cùng 1 SKU.
-- Cách chống (chốt ở ADR-0006): điều kiện "còn đủ hàng" nằm TRONG câu UPDATE
--   UPDATE stock SET reserved = reserved + :qty
--    WHERE sku_id = :sku AND warehouse_id = :wh AND on_hand - reserved >= :qty;
-- 0 row ⇒ hết hàng. Row lock của Postgres làm việc serialise, không cần distributed lock.
--
-- Lưu ý thiết kế (đọc kỹ trước khi sửa):
--   - `on_hand` = hàng thật trong kho; `reserved` = đang giữ chỗ cho đơn chưa thanh toán.
--     `available = on_hand - reserved`. CHECK reserved <= on_hand là bất biến cuối ở tầng DB.
--   - `version` tăng mỗi lần ghi — KHÔNG dùng cho compare-and-set (conditional UPDATE đã đủ),
--     chỉ để debug/audit "row này bị sửa bao nhiêu lần".
--   - `reservations` = 1 row/ORDER + `reservation_items` (chốt ở SL-2.2): nhờ vậy unique
--     `(order_id) WHERE status='PENDING'` vừa là khoá idempotency (retry cùng orderId không tạo
--     reservation thứ 2) vừa cho API trả đúng 1 `reservationId`.
--   - `stock_ledger` append-only, ghi 2 cột delta: nhờ vậy đối soát được CẢ on_hand và reserved:
--       SELECT sum(delta_on_hand), sum(delta_reserved) FROM stock_ledger WHERE sku_id = ?;
--     RESERVE (0, +q) · COMMIT (-q, -q) · RELEASE/EXPIRE (0, -q) · ADJUST (+/-n, 0)
--   - Không FK xuyên service: `sku_id` chỉ là tham chiếu tới catalog (database per service).
-- =============================================================================

CREATE TABLE stock (
    sku_id       UUID         NOT NULL,
    warehouse_id VARCHAR(32)  NOT NULL,
    on_hand      INTEGER      NOT NULL,
    reserved     INTEGER      NOT NULL DEFAULT 0,
    version      BIGINT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (sku_id, warehouse_id),
    -- Bất biến cuối ở tầng dữ liệu: bug ở service không được phép tạo tồn kho âm.
    CONSTRAINT ck_stock_on_hand_non_negative CHECK (on_hand >= 0),
    CONSTRAINT ck_stock_reserved_non_negative CHECK (reserved >= 0),
    CONSTRAINT ck_stock_reserved_within_on_hand CHECK (reserved <= on_hand)
);

CREATE TABLE reservations (
    id         UUID        PRIMARY KEY,
    -- order_id do order-service sinh; inventory KHÔNG sở hữu order, chỉ giữ tham chiếu.
    order_id   UUID        NOT NULL,
    status     VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_reservations_status
        CHECK (status IN ('PENDING', 'COMMITTED', 'RELEASED', 'EXPIRED'))
);

-- Idempotency tự nhiên: 1 order chỉ được có 1 reservation đang giữ chỗ. Retry cùng orderId
-- (client timeout, network) sẽ đụng unique này thay vì trừ kho lần 2.
CREATE UNIQUE INDEX ux_reservations_pending_order
    ON reservations (order_id) WHERE status = 'PENDING';

-- Sweeper (SL-2.3) quét theo (status, expires_at) ⇒ index đúng thứ tự đó.
CREATE INDEX ix_reservations_status_expires ON reservations (status, expires_at);

CREATE TABLE reservation_items (
    reservation_id UUID    NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    sku_id         UUID    NOT NULL,
    qty            INTEGER NOT NULL,
    PRIMARY KEY (reservation_id, sku_id),
    CONSTRAINT ck_reservation_items_qty_positive CHECK (qty > 0)
);

CREATE TABLE stock_ledger (
    id              UUID        PRIMARY KEY,
    sku_id          UUID        NOT NULL,
    warehouse_id    VARCHAR(32) NOT NULL,
    delta_on_hand   INTEGER     NOT NULL,
    delta_reserved  INTEGER     NOT NULL,
    reason          VARCHAR(16) NOT NULL,
    -- Tham chiếu tới thứ gây ra thay đổi: reservationId (RESERVE/COMMIT/RELEASE/EXPIRE) hoặc null (ADJUST).
    ref_id          UUID,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_stock_ledger_reason
        CHECK (reason IN ('ADJUST', 'RESERVE', 'COMMIT', 'RELEASE', 'EXPIRE'))
);

-- Truy vấn đối soát / hiển thị lịch sử của 1 SKU theo thời gian.
CREATE INDEX ix_stock_ledger_sku_created ON stock_ledger (sku_id, created_at DESC);
