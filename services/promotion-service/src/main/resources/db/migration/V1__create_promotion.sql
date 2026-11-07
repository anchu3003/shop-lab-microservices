-- =============================================================================
-- V1__create_promotion.sql — schema khuyến mãi (SL-2.4, ADR-0006 D1)
--
-- Bài toán: một voucher có `quota` lượt. Hai checkout đồng thời không được cùng dùng lượt cuối ⇒ phải
-- "giữ chỗ" (lock) rồi mới commit khi tiền về, đúng pattern reserve/commit/release của inventory.
--
-- Lưu ý thiết kế:
--   - BẤT BIẾN QUAN TRỌNG NHẤT: `used + locked <= quota`. Nếu chỉ đếm `used` thì 2 request đồng thời
--     cùng thấy "còn lượt" (đọc-rồi-ghi) ⇒ bán quá quota. Lock chiếm chỗ bằng conditional UPDATE.
--   - `locked` = số lượt đang bị giữ bởi lock chưa commit; `used` = số lượt đã dùng thật.
--   - `max_discount` chỉ có nghĩa với PERCENT (chặn trần giảm giá); NULL với FIXED.
--   - `voucher_locks` = 1 row/ORDER (giống reservations): unique `(order_id) WHERE status='LOCKED'`
--     là khoá idempotency tự nhiên — retry cùng orderId không giữ lượt lần 2.
--   - `discount` + `subtotal` lưu SNAPSHOT tại lúc lock: commit phải dùng đúng con số đã hứa với client,
--     kể cả khi admin sửa `value` của voucher trong lúc user đang thanh toán.
--   - Không FK xuyên service: `order_id` chỉ là tham chiếu tới order-service.
-- =============================================================================

CREATE TABLE vouchers (
    id           UUID           PRIMARY KEY,
    code         VARCHAR(40)    NOT NULL,
    type         VARCHAR(16)    NOT NULL,
    value        NUMERIC(12, 2) NOT NULL,
    max_discount NUMERIC(12, 2),
    min_order    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    quota        INTEGER        NOT NULL,
    used         INTEGER        NOT NULL DEFAULT 0,
    locked       INTEGER        NOT NULL DEFAULT 0,
    start_at     TIMESTAMPTZ    NOT NULL,
    end_at       TIMESTAMPTZ    NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_vouchers_type CHECK (type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_vouchers_value_positive CHECK (value > 0),
    CONSTRAINT ck_vouchers_max_discount_non_negative CHECK (max_discount >= 0),
    CONSTRAINT ck_vouchers_min_order_non_negative CHECK (min_order >= 0),
    CONSTRAINT ck_vouchers_quota_positive CHECK (quota > 0),
    CONSTRAINT ck_vouchers_used_non_negative CHECK (used >= 0),
    CONSTRAINT ck_vouchers_locked_non_negative CHECK (locked >= 0),
    -- Chốt chặn cuối ở tầng dữ liệu: bug ở service không được phép bán quá quota.
    CONSTRAINT ck_vouchers_within_quota CHECK (used + locked <= quota),
    CONSTRAINT ck_vouchers_window_valid CHECK (end_at > start_at)
);

-- Mã voucher là định danh client gõ vào ⇒ unique toàn hệ (so sánh sau khi chuẩn hoá UPPER ở service).
CREATE UNIQUE INDEX ux_vouchers_code ON vouchers (code);

CREATE TABLE voucher_locks (
    id         UUID           PRIMARY KEY,
    voucher_id UUID           NOT NULL REFERENCES vouchers (id),
    order_id   UUID           NOT NULL,
    status     VARCHAR(16)    NOT NULL,
    subtotal   NUMERIC(12, 2) NOT NULL,
    discount   NUMERIC(12, 2) NOT NULL,
    expires_at TIMESTAMPTZ    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    updated_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_voucher_locks_status
        CHECK (status IN ('LOCKED', 'COMMITTED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_voucher_locks_amounts CHECK (subtotal >= 0 AND discount >= 0 AND discount <= subtotal)
);

-- Idempotency: 1 order chỉ giữ 1 lượt voucher tại một thời điểm.
CREATE UNIQUE INDEX ux_voucher_locks_locked_order
    ON voucher_locks (order_id) WHERE status = 'LOCKED';

-- Sweeper quét theo (status, expires_at).
CREATE INDEX ix_voucher_locks_status_expires ON voucher_locks (status, expires_at);
