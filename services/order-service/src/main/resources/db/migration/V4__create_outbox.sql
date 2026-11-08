-- ==============================================================================
-- V4__create_outbox.sql — transactional outbox (SL-3.3)
--
-- Vì sao có bảng này: publish event KHÔNG thể nằm trong transaction DB (broker không tham gia
-- transaction) ⇒ nếu publish fail sau commit thì mất event, còn publish trước commit thì có thể phát
-- event cho dữ liệu chưa tồn tại. Outbox biến "phát event" thành một dòng dữ liệu ghi CÙNG transaction
-- với thay đổi nghiệp vụ; relay đọc bảng này để publish (ADR-0007 D4).
--
-- `claimed_at` KHÔNG có trong danh sách cột tối thiểu của ticket — thêm để AC "2 relay chạy song song
-- không gửi trùng" là CHẮC CHẮN, không dựa vào việc 2 vòng quét lệch nhau: claim lấy row kèm
-- `FOR UPDATE SKIP LOCKED`, row đang bị claim thì relay khác bỏ qua (visibility timeout =
-- `claim-timeout`, sau đó coi như claim thất bại và cho retry).
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
    -- Bất biến: SENT thì PHẢI có sent_at (không có chuyện "đã gửi mà không biết lúc nào").
    CONSTRAINT outbox_sent_check CHECK (status <> 'SENT' OR sent_at IS NOT NULL)
);

-- Đường quét của relay: chỉ row PENDING, cũ nhất trước.
CREATE INDEX idx_outbox_pending ON outbox (status, created_at) WHERE status = 'PENDING';

-- Truy vết theo nghiệp vụ: "đơn này đã phát những event gì".
CREATE INDEX idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id);
