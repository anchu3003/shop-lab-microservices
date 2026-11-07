-- =============================================================================
-- V3__add_soft_delete.sql — soft delete cho product + sku (SL-2.11, ADR-0006 D5)
--
-- Vì sao KHÔNG hard delete nữa:
--   Phase 2 tham chiếu `sku_id` từ các service khác (inventory.stock / stock_ledger, order_items).
--   `sku.product_id` có ON DELETE CASCADE ⇒ hard delete product là xoá luôn SKU ⇒ mất luôn khả năng
--   reconcile tồn kho và audit "sản phẩm này từng bán thế nào". Soft delete giữ row, API thì coi
--   như đã xoá.
--
-- Lưu ý thiết kế:
--   - `status` lưu dạng CHUỖI ('ACTIVE' | 'ARCHIVED'), không phải số: đọc DB bằng mắt được, thêm
--     giá trị mới (vd 'DRAFT') không làm lệch dữ liệu cũ. Java dùng @Enumerated(STRING).
--   - CHECK `ck_product_deleted_at_matches_status` là **chốt chặn cuối**: không cho tồn tại row
--     status='ACTIVE' mà deleted_at NOT NULL (hoặc ngược lại). App có bug thì DB vẫn giữ bất biến.
--   - Partial index `WHERE deleted_at IS NULL`: list chỉ đọc product còn sống ⇒ index không phình
--     theo số row đã xoá. Vẫn giữ các index cũ cho truy vấn khác.
--   - DEFAULT 'ACTIVE' để 20 row seed + dữ liệu đang có không cần UPDATE thêm.
-- =============================================================================

ALTER TABLE product ADD COLUMN status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE product ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE product
    ADD CONSTRAINT ck_product_status CHECK (status IN ('ACTIVE', 'ARCHIVED'));

-- Bất biến: ARCHIVED ⟺ có deleted_at. Thiếu ràng buộc này thì bug ở tầng service sẽ tạo ra row
-- "nửa sống nửa chết" mà không ai phát hiện.
ALTER TABLE product
    ADD CONSTRAINT ck_product_deleted_at_matches_status
    CHECK ((status = 'ARCHIVED') = (deleted_at IS NOT NULL));

-- SKU theo product: product bị xoá mềm thì SKU của nó cũng ARCHIVED (vẫn còn row để tham chiếu).
ALTER TABLE sku ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE sku
    ADD CONSTRAINT ck_sku_status CHECK (status IN ('ACTIVE', 'ARCHIVED'));

-- List/sort mặc định là `created_at desc` và chỉ đọc product còn sống ⇒ partial index rẻ hơn.
CREATE INDEX ix_product_active_created_at ON product (created_at) WHERE deleted_at IS NULL;
