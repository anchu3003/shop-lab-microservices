-- =============================================================================
-- V1__create_payment.sql — schema thanh toán (SL-2.7, ADR-0006 D2 + architecture §6.2)
--
-- Nguyên tắc:
--   - **Hai nguồn trùng khác nhau ⇒ hai cơ chế khác nhau**:
--       client bấm 2 lần      → `idempotency_keys` (PK scope + key), scope = "<userId>:POST /v1/payment-intents"
--       provider gọi lại N lần → `payment_webhook_events` (PK provider_event_id) + `payments.provider_txn_id` UNIQUE
--     Provider không biết `Idempotency-Key` của client nên không thể dùng chung một khoá.
--   - `provider_intent_id` UNIQUE: id bên provider, tra ngược từ webhook (webhook chỉ biết id này).
--   - `ux_payments_live_order`: mỗi đơn tối đa **1 intent sống** (đang chờ, đang xử lý, hoặc đã thành
--     công). Có unique index ở DB chứ không chỉ kiểm trong code ⇒ 2 request song song cùng đơn vẫn
--     không tạo được 2 intent.
--   - `ck_payments_paid_at`: SUCCEEDED ⇔ có `paid_at`. Bất biến kế toán: đơn đã thu tiền mà không có
--     mốc thời gian thu tiền là dữ liệu không đối soát được.
--   - Phase 2 không có broker ⇒ không có `outbox` ở đây; `payment.succeeded` sẽ thêm ở Phase 3.
-- =============================================================================

CREATE TABLE payments (
    id                 UUID           PRIMARY KEY,
    order_id           UUID           NOT NULL,
    user_id            UUID           NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    currency           CHAR(3)        NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    provider           VARCHAR(20)    NOT NULL,
    -- Id bên provider (mock: `pi_mock_...`). Webhook chỉ biết id này để tra ngược.
    provider_intent_id VARCHAR(64)    NOT NULL,
    -- Mã giao dịch thật của provider, có sau khi thanh toán. UNIQUE = chốt chặn cộng tiền 2 lần.
    provider_txn_id    VARCHAR(128),
    -- Khoá client gửi (lưu để audit; dedupe thật nằm ở bảng idempotency_keys).
    idempotency_key    VARCHAR(64)    NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    paid_at            TIMESTAMPTZ,
    CONSTRAINT ux_payments_provider_intent UNIQUE (provider_intent_id),
    CONSTRAINT ux_payments_provider_txn UNIQUE (provider_txn_id),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('REQUIRES_PAYMENT', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_paid_at CHECK ((status = 'SUCCEEDED') = (paid_at IS NOT NULL))
);

-- Mỗi đơn tối đa 1 intent "sống" (FAILED/CANCELLED thì khách được tạo lại).
CREATE UNIQUE INDEX ux_payments_live_order
    ON payments (order_id)
 WHERE status IN ('REQUIRES_PAYMENT', 'PROCESSING', 'SUCCEEDED');

CREATE INDEX ix_payments_user_created ON payments (user_id, created_at DESC);
CREATE INDEX ix_payments_order ON payments (order_id);

-- Nhật ký webhook: khoá chính là id sự kiện bên provider ⇒ gửi lại 20 lần vẫn 1 row.
CREATE TABLE payment_webhook_events (
    provider_event_id VARCHAR(128)   PRIMARY KEY,
    payment_id        UUID           NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    provider_txn_id   VARCHAR(128),
    status            VARCHAR(20)    NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    -- SHA-256 của raw body: đối soát/điều tra khi có tranh chấp, KHÔNG lưu cả body (dữ liệu thẻ).
    payload_sha256    CHAR(64)       NOT NULL,
    received_at       TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_payment_webhook_events_payment ON payment_webhook_events (payment_id);

-- Cùng schema với order-service (ADR-0006 D2). Copy thay vì share code vì `packages/` chưa có
-- module idempotency; khi có service thứ ba cần thì đưa vào commons (TD-19).
CREATE TABLE idempotency_keys (
    scope         VARCHAR(120) NOT NULL,
    key           VARCHAR(64)  NOT NULL,
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

CREATE INDEX ix_idempotency_created ON idempotency_keys (created_at);
