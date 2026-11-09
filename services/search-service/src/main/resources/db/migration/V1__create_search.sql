-- ==============================================================================
-- V1__create_search.sql — schema của search-service (SL-3.6)
--
-- Nguyên tắc: đây là INDEX, không phải nguồn sự thật. Bảng chỉ cần trả lời 3 câu:
--   (1) khách gõ `q` thì product nào khớp, (2) filter theo category/giá, (3) row nào còn ACTIVE.
-- Mọi thứ khác (SKU, tồn kho, mô tả dài) thuộc service khác.
--
-- Vì sao lưu `search_vector` (cột thật) thay vì generated column:
--   `unaccent()` của Postgres KHÔNG phải IMMUTABLE (nó phụ thuộc dictionary file) ⇒
--   `GENERATED ALWAYS AS (to_tsvector('simple', unaccent(name))) STORED` bị từ chối. Cách đúng là
--   app ghi `search_vector` trong chính câu UPSERT (xem SearchRepository) + GIN index trên cột đó.
--   Đánh đổi: thêm một cột phải tự tính đúng ở mọi chỗ ghi — chấp nhận, vì chỉ có MỘT chỗ ghi
--   (consumer + reindex dùng chung một câu UPSERT).
--
-- `unaccent` + `simple`: Postgres không có config `vietnamese`. `simple` (không stemming, không
-- stop-word) + unaccent cho ta "dien thoai" khớp "Điện thoại". Giới hạn phải nói thật: KHÔNG xử lý
-- từ đồng nghĩa, KHÔNG stem tiếng Việt, KHÔNG xếp hạng theo relevance thông minh (chỉ ts_rank).
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE search_product (
    product_id    uuid         PRIMARY KEY,
    name          text         NOT NULL,
    description   text,
    category_id   uuid         NOT NULL,
    -- Giá niêm yết của product (nguồn: catalog). KHÔNG phải min qua các SKU override — xem
    -- Learning note của SL-3.6 (giới hạn đã biết, không giấu).
    min_price     numeric(12,2) NOT NULL,
    status        varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    updated_at    timestamptz  NOT NULL,
    search_vector tsvector,
    CONSTRAINT search_product_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

-- Đường query chính: `search_vector @@ plainto_tsquery(...)`.
CREATE INDEX ix_search_product_vector ON search_product USING gin (search_vector);

-- Filter theo category + khoảng giá (kèm status để index còn dùng được khi chỉ lọc giá).
CREATE INDEX ix_search_product_filter ON search_product (status, category_id, min_price);

-- Reconcile so row theo trạng thái + đọc row "cũ" để phát hiện index mồ côi.
CREATE INDEX ix_search_product_status_updated ON search_product (status, updated_at DESC);

-- Dedupe của consumer (commons dùng đúng bảng/kiểu này — xem `JdbcProcessedEventStore`).
CREATE TABLE processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(64)  NOT NULL,
    event_type   varchar(128) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
