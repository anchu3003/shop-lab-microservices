-- =============================================================================
-- V2__create_checkout_saga.sql — state của checkout saga (SL-2.6a, ADR-0006 D4)
--
-- Vì sao lưu saga state ở DB của order-service (không phải một service saga riêng):
--   order-service là orchestrator và đã sở hữu `orders`; tách service thứ 9 ở Phase 2 là
--   over-engineer (AGENTS.md §3.4). Nhưng state PHẢI nằm trong DB chứ không chỉ trong RAM:
--   crash giữa chừng thì lần đọc sau / sweeper (`SL-2.6b`) vẫn biết đã giữ chỗ những gì để bù.
--
-- Nguyên tắc:
--   - Ghi row `STARTED` TRƯỚC khi gọi service ngoài đầu tiên, rồi update từng bước ⇒ không bao giờ
--     tồn tại trạng thái "đã giữ hàng mà không ai biết".
--   - `payment_required` là cột riêng (không suy ra từ `orders.total = 0`) vì nó là BẤT BIẾN của
--     saga: đơn 0 đồng (voucher 100%) cố ý không tạo payment intent, và `SL-2.6b` phải biết điều đó
--     để chốt đơn mà không cần verify payment.
--   - `checkout_saga_steps` là audit: mỗi bước một dòng (OK/FAILED/SKIPPED + http status + thời
--     gian). Thiếu nó thì khi saga sai chỉ còn log để đoán.
-- =============================================================================

CREATE TABLE checkout_sagas (
    -- 1-1 với order: `order_id` là khoá tự nhiên (và cũng là khoá idempotency của
    -- inventory/promotion, xem ADR-0006 D1/D3).
    order_id           UUID         PRIMARY KEY,
    user_id            UUID         NOT NULL,
    state              VARCHAR(20)  NOT NULL,
    payment_required   BOOLEAN      NOT NULL,
    lock_id            UUID,
    reservation_id     UUID,
    payment_intent_id  UUID,
    payment_url        VARCHAR(500),
    failure_code       VARCHAR(64),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_checkout_saga_state
        CHECK (state IN ('STARTED', 'COMPLETED_START', 'FAILED_STOCK', 'FAILED_ORDER', 'FAILED_PAYMENT')),
    -- Bất biến: saga xong bước start thì phải có payment intent, TRỪ khi đơn 0 đồng.
    CONSTRAINT ck_checkout_saga_payment_intent
        CHECK (state <> 'COMPLETED_START' OR payment_intent_id IS NOT NULL OR NOT payment_required)
);

-- "Saga của tôi, mới nhất trước" — cùng khuôn index với `orders`.
CREATE INDEX ix_checkout_sagas_user_created ON checkout_sagas (user_id, created_at DESC);

-- `SL-2.6b` (sweeper) quét saga quá hạn theo state + tuổi.
CREATE INDEX ix_checkout_sagas_state_created ON checkout_sagas (state, created_at);

CREATE TABLE checkout_saga_steps (
    id          UUID         PRIMARY KEY,
    order_id    UUID         NOT NULL REFERENCES checkout_sagas (order_id) ON DELETE CASCADE,
    step        VARCHAR(32)  NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    http_status INTEGER,
    duration_ms INTEGER      NOT NULL,
    detail      VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_checkout_saga_step_status CHECK (status IN ('OK', 'FAILED', 'SKIPPED'))
);

CREATE INDEX ix_checkout_saga_steps_order ON checkout_saga_steps (order_id, created_at);
