-- ==============================================================================
-- V2__create_outbox.sql — transactional outbox của payment (SL-3.5)
--
-- Vì sao có bảng này: webhook chuyển intent sang SUCCEEDED là **sự kiện nghiệp vụ thật** (tiền đã
-- về). Publish thẳng lên broker ngay trong webhook thì broker chết = mất event ⇒ `order-service`
-- không bao giờ tự confirm ⇒ tiền thu rồi mà đơn treo (đúng `TD-17`). Ghi 1 dòng vào bảng này TRONG
-- cùng transaction với UPDATE payments; relay đọc bảng để publish (ADR-0007 D4).
--
-- DDL chép nguyên khuôn `order-service` V4 (`SL-3.3`) — cùng ngữ nghĩa, cùng cách vận hành, để
-- không phải học lại một mô hình thứ hai. `claimed_at` giữ nguyên vai trò: visibility timeout cho
-- claim của relay.
-- ==============================================================================

CREATE TABLE outbox (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  varchar(32)  NOT NULL,
    aggregate_id    uuid         NOT NULL,
    event_type      varchar(128) NOT NULL,
    payload         jsonb        NOT NULL,
    status          varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempts        integer      NOT NULL DEFAULT 0,
    last_error      text,
    claimed_at      timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    CONSTRAINT outbox_status_check CHECK (status IN ('PENDING', 'SENT')),
    CONSTRAINT outbox_sent_check CHECK (status <> 'SENT' OR sent_at IS NOT NULL)
);

CREATE INDEX idx_outbox_pending ON outbox (status, created_at) WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id);

-- Truy vết thanh toán: một intent chỉ được phát **một** event `succeeded` — chốt chặn thứ hai ở tầng
-- dữ liệu cho AC "webhook lặp ⇒ outbox vẫn 1 row" (chốt thứ nhất là `payment_webhook_events`).
CREATE UNIQUE INDEX ux_outbox_payment_succeeded
    ON outbox (aggregate_id)
 WHERE event_type = 'payment.succeeded.v1';
