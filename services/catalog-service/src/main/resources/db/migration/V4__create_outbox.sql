-- ==============================================================================
-- V4__create_outbox.sql — transactional outbox của catalog (SL-3.6)
--
-- Vì sao có bảng này: `search-service` cần biết product đổi, nhưng catalog **không** được gọi thẳng
-- sang search (search là index, không phải nguồn sự thật). Publish thẳng lên broker trong request
-- thì broker chết = mất event vĩnh viễn ⇒ index lệch mà không ai biết. Ghi một dòng vào đây TRONG
-- cùng transaction với INSERT/UPDATE product; relay đọc bảng để publish (ADR-0007 D4).
--
-- DDL chép nguyên khuôn `order-service` V4 (`SL-3.3`) và `payment-service` V2 (`SL-3.5`) — **lần
-- copy thứ 3** là quyết định có ý thức, đóng `TD-32` bằng lý do ghi trong Backlog §7 (extract vào
-- commons khi có producer thứ 4 hoặc khi lên OpenSearch ở Phase 8, lúc đó hình dạng outbox có thể
-- phải đổi theo).
--
-- KHÔNG có unique index kiểu `payment`: product sửa nhiều lần là hợp lệ, mỗi lần là một event mới.
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
    -- NULL = không ai đang giữ; khác NULL = relay đang publish (visibility timeout khi quá hạn).
    claimed_at      timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    CONSTRAINT outbox_status_check CHECK (status IN ('PENDING', 'SENT')),
    CONSTRAINT outbox_sent_check CHECK (status <> 'SENT' OR sent_at IS NOT NULL)
);

-- Đường quét của relay: chỉ row PENDING, cũ nhất trước.
CREATE INDEX idx_outbox_pending ON outbox (status, created_at) WHERE status = 'PENDING';

-- Truy vết theo nghiệp vụ: "product này đã phát những event gì" (dùng khi điều tra index lệch).
CREATE INDEX idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id);
