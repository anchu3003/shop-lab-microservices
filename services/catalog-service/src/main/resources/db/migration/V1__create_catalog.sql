-- =============================================================================
-- V1__create_catalog.sql — schema catalog (SL-1.5)
--
-- Quy ước migration của repo:
--   - Mỗi service có schema riêng do Flyway quản lý (database per service).
--   - KHÔNG sửa file migration đã chạy (Flyway lưu checksum) → đổi schema = file V2__ mới.
--
-- Lưu ý thiết kế (đọc kỹ trước khi sửa):
--   - `price` là NUMERIC(12,2), KHÔNG phải double/float: double không biểu diễn chính xác số thập
--     phân ⇒ 0.1 + 0.2 != 0.3 ⇒ sai tiền. Java dùng BigDecimal tương ứng.
--   - `version` là optimistic locking của JPA (@Version). Bắt đầu từ 0; mỗi UPDATE +1. Không có nó
--     thì 2 request sửa cùng lúc sẽ "last write wins" âm thầm mất dữ liệu (Phase 2 sẽ cần).
--   - FK product.category_id → category.id: xoá category còn product thì DB chặn (RESTRICT mặc
--     định) — cố ý, để không có product mồ côi.
--   - Index trên category_id vì list product theo category là truy vấn phổ biến nhất
--     (Phase 4 sẽ đo lại bằng EXPLAIN khi có dữ liệu lớn).
--   - Timestamp dùng `timestamptz` (Instant ở Java) — không lưu giờ "trần" để tránh lệch múi giờ.
--   - `slug` unique: định danh đọc được của category (dùng cho URL/SEO sau này).
-- =============================================================================

CREATE TABLE category (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(140) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

-- Slug là định danh công khai của category → phải duy nhất.
CREATE UNIQUE INDEX ux_category_slug ON category (slug);

CREATE TABLE product (
    id          UUID           PRIMARY KEY,
    category_id UUID           NOT NULL REFERENCES category (id),
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(2000),
    price       NUMERIC(12, 2) NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    -- Chốt chặn cuối ở tầng dữ liệu: validation ở API đã chặn giá âm, đây là lưới an toàn
    -- cho mọi đường ghi khác (script, migration, service khác trong tương lai).
    CONSTRAINT ck_product_price_non_negative CHECK (price >= 0)
);

CREATE INDEX ix_product_category_id ON product (category_id);
