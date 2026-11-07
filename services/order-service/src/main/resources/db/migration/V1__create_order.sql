-- =============================================================================
-- V1__create_order.sql — schema đơn hàng + idempotency (SL-2.5, ADR-0006 D2/D3)
--
-- Nguyên tắc:
--   - `order_items` là **snapshot** tại thời điểm đặt: copy sku_code/title/unit_price/currency, KHÔNG
--     FK sang catalog (database per service). Nhờ vậy đổi giá hay xoá SKU ở catalog KHÔNG làm sai hoá
--     đơn cũ — đây là điểm của ADR-0006 D3.
--   - `line_total = unit_price * qty` là CHECK ở DB: nếu chỉ tính trong code thì một đường ghi khác
--     (script, migration) có thể tạo ra hoá đơn cộng sai tiền mà không ai biết.
--   - `total = subtotal - discount` cũng là CHECK: bất biến kế toán, không phụ thuộc code đúng.
--   - `order_status_history` ghi **mọi** lần chuyển trạng thái (kể cả lần từ chối? KHÔNG — chỉ ghi
--     chuyển thành công; lần bị từ chối trả 409 và không đổi dữ liệu).
--   - `idempotency_keys` PK (scope, key): scope = "<userId>:POST /v1/orders" ⇒ 2 user khác nhau dùng
--     cùng một key KHÔNG đụng nhau (key do client sinh, không đảm bảo unique toàn hệ).
-- =============================================================================

CREATE TABLE orders (
    id                UUID           PRIMARY KEY,
    user_id           UUID           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    subtotal          NUMERIC(12, 2) NOT NULL,
    discount          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total             NUMERIC(12, 2) NOT NULL,
    currency          CHAR(3)        NOT NULL,
    -- Khoá idempotency client gửi (lưu lại để tra cứu/audit; dedupe thật nằm ở bảng idempotency_keys).
    idempotency_key   VARCHAR(64)    NOT NULL,
    payment_intent_id UUID,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_orders_status
        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_orders_amounts_non_negative CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0),
    CONSTRAINT ck_orders_discount_within_subtotal CHECK (discount <= subtotal),
    CONSTRAINT ck_orders_total_consistent CHECK (total = subtotal - discount)
);

-- "Đơn của tôi, mới nhất trước" — truy vấn phổ biến nhất của buyer.
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at DESC);

CREATE TABLE order_items (
    id          UUID           PRIMARY KEY,
    order_id    UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    -- Tham chiếu opaque tới catalog: không FK, không join (ADR-0006 D3).
    sku_id      UUID           NOT NULL,
    sku_code    VARCHAR(64)    NOT NULL,
    title       VARCHAR(200)   NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,
    currency    CHAR(3)        NOT NULL,
    qty         INTEGER        NOT NULL,
    line_total  NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_order_items_qty_positive CHECK (qty > 0),
    CONSTRAINT ck_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_line_total_consistent CHECK (line_total = unit_price * qty)
);

CREATE INDEX ix_order_items_order ON order_items (order_id);

CREATE TABLE order_status_history (
    id          UUID         PRIMARY KEY,
    order_id    UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    -- NULL = trạng thái đầu tiên (lúc tạo đơn).
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    reason      VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_order_status_history_order ON order_status_history (order_id, created_at);

CREATE TABLE idempotency_keys (
    scope         VARCHAR(120) NOT NULL,
    key           VARCHAR(64)  NOT NULL,
    -- SHA-256 của body đã canonical hoá: cùng key mà hash khác ⇒ client dùng lại key cho payload mới.
    request_hash  VARCHAR(64)  NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    status_code   INTEGER,
    response_body TEXT,
    resource_id   UUID,
    created_at    TIMESTAMPTZ  NOT NULL,
    completed_at  TIMESTAMPTZ,
    PRIMARY KEY (scope, key),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
);

-- Job dọn (TD-14) quét theo tuổi row.
CREATE INDEX ix_idempotency_created ON idempotency_keys (created_at);
