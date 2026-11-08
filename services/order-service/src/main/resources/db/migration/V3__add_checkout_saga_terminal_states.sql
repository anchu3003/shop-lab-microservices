-- =============================================================================
-- V3__add_checkout_saga_terminal_states.sql — trạng thái kết thúc của saga (SL-2.6b)
--
-- V2 chỉ có trạng thái của bước START (STARTED / COMPLETED_START / FAILED_*). Bước finish cần 3
-- trạng thái kết thúc:
--   CONFIRMED — tiền đã về (hoặc đơn 0 đồng): stock + voucher đã commit, order CONFIRMED
--   CANCELLED — khách huỷ: stock + voucher đã release, order CANCELLED
--   EXPIRED   — quá payment-window: order EXPIRED, hàng/voucher do TTL sweeper của
--               inventory/promotion tự trả (sweeper của order-service không có token user)
--
-- Vì sao ALTER thay vì tạo bảng mới: cùng bảng, cùng ý nghĩa — chỉ nới tập giá trị hợp lệ. Không có
-- state "đang xử lý" (CANCELLING/EXPIRING) vì commit/release của inventory & promotion đã idempotent
-- (gọi 2 lần trả 200), còn chốt chặn thật là CAS trạng thái `orders`.
-- =============================================================================

ALTER TABLE checkout_sagas DROP CONSTRAINT ck_checkout_saga_state;

ALTER TABLE checkout_sagas
    ADD CONSTRAINT ck_checkout_saga_state
    CHECK (state IN ('STARTED', 'COMPLETED_START', 'FAILED_STOCK', 'FAILED_ORDER', 'FAILED_PAYMENT',
                     'CONFIRMED', 'CANCELLED', 'EXPIRED'));

-- Sweeper (`SL-2.6b`) quét saga còn `COMPLETED_START` + order còn `PENDING_PAYMENT` theo tuổi.
-- Index sẵn có là (state, created_at) — đủ cho truy vấn này, không cần index mới.
