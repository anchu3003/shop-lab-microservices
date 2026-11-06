-- =============================================================================
-- V2__create_sku.sql — SKU (biến thể) + index phục vụ list/filter (SL-1.6)
--
-- Vì sao cần SKU: một product "Áo thun" có nhiều biến thể (màu/size) — mỗi biến thể là một đơn vị
-- bán thật, có mã riêng và có thể có giá riêng. Phase 2 (đặt hàng) sẽ tham chiếu tới SKU, không
-- phải product.
--
-- Lưu ý thiết kế:
--   - `code` là mã SKU (vd "AT-DO-M") → UNIQUE toàn hệ: đó là thứ kho/nhãn vận chuyển dùng.
--   - `price_override` NULL = dùng giá của product (không copy giá, tránh 2 nguồn sự thật lệch nhau).
--   - `stock_hint` CHỈ là gợi ý hiển thị ("còn hàng / hết hàng"). Tồn kho thật + reserve/commit là
--     việc của inventory-service (Phase 2) — cố ý KHÔNG dựng bảng tồn kho ở đây.
--   - ON DELETE CASCADE: SKU vô nghĩa nếu product bị xoá (khác với product→category là RESTRICT).
--
-- Index thêm cho API list (đo bằng EXPLAIN ở bước verify):
--   - `product(created_at)` : sort mặc định `createdAt,desc`.
--   - `product(lower(name))` : tìm theo tên không phân biệt hoa/thường (`q`).
-- =============================================================================

CREATE TABLE sku (
    id             UUID           PRIMARY KEY,
    product_id     UUID           NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    code           VARCHAR(64)    NOT NULL,
    price_override NUMERIC(12, 2),
    stock_hint     INTEGER,
    created_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_sku_price_override_non_negative CHECK (price_override >= 0),
    CONSTRAINT ck_sku_stock_hint_non_negative CHECK (stock_hint >= 0)
);

-- Mã SKU là định danh toàn hệ (kho, vận chuyển) → phải duy nhất.
CREATE UNIQUE INDEX ux_sku_code ON sku (code);

-- Truy vấn phổ biến: lấy SKU của một product (trang chi tiết).
CREATE INDEX ix_sku_product_id ON sku (product_id);

-- Index cho list/sort/filter của product.
CREATE INDEX ix_product_created_at ON product (created_at);
CREATE INDEX ix_product_name_lower ON product (lower(name));
