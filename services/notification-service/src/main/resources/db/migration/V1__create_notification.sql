-- ==============================================================================
-- V1__create_notification.sql — schema của notification-service (SL-3.4)
--
-- Nguyên tắc: bảng `processed_events` do CHÍNH service này khai (không để commons
-- auto-create) vì DDL là việc của service sở hữu DB ⇒ set
-- `shoplab.events.dedup.auto-create-schema=false` trong application.yml.
-- ==============================================================================

-- Mỗi event xử lý xong đẻ ra 1 dòng. `event_id` UNIQUE ⇒ chốt chặn thứ hai chống gửi 2 lần
-- (chốt thứ nhất là `processed_events`; đây là bất biến ở tầng dữ liệu, không tin vào code).
CREATE TABLE notification_logs (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    uuid         NOT NULL UNIQUE,
    user_id     uuid         NOT NULL,
    order_id    uuid,
    channel     varchar(16)  NOT NULL DEFAULT 'EMAIL',
    template    varchar(64)  NOT NULL,
    recipient   varchar(255) NOT NULL,
    subject     varchar(255) NOT NULL,
    body        text         NOT NULL,
    status      varchar(16)  NOT NULL DEFAULT 'SENT',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    sent_at     timestamptz,
    CONSTRAINT notification_status_check CHECK (status IN ('SENT', 'FAILED'))
);

-- Truy vấn chính của API: "thông báo của tôi", mới nhất trước.
CREATE INDEX idx_notification_logs_user ON notification_logs (user_id, created_at DESC);

-- Template đơn giản (placeholder {{var}}) — mock provider, không SMTP. Seed ở migration để template
-- KHÔNG phải deploy cùng code: sửa câu chữ là sửa dữ liệu, không build lại image.
CREATE TABLE notification_templates (
    code       varchar(64) PRIMARY KEY,
    subject    varchar(255) NOT NULL,
    body       text         NOT NULL,
    updated_at timestamptz  NOT NULL DEFAULT now()
);

INSERT INTO notification_templates (code, subject, body)
VALUES
    ('order.confirmed',
     'Đơn hàng {{orderId}} đã được xác nhận',
     'Chào bạn, đơn {{orderId}} trị giá {{total}} {{currency}} ({{itemCount}} sản phẩm) đã được xác nhận lúc {{confirmedAt}}.'),
    ('order.cancelled',
     'Đơn hàng {{orderId}} đã bị huỷ',
     'Chào bạn, đơn {{orderId}} đã bị huỷ. Lý do: {{reason}}. Thời điểm: {{cancelledAt}}.');

-- Dedupe của consumer (commons dùng đúng bảng/kiểu này — xem `JdbcProcessedEventStore`).
CREATE TABLE processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(64)  NOT NULL,
    event_type   varchar(128) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
