-- ==============================================================================
-- V5__index_outbox_sent.sql — index cho job retention (SL-4.0, TD-29)
--
-- Vì sao cần: job retention chạy `DELETE … WHERE status = 'SENT' AND sent_at < ?`
-- mỗi nhịp. Không có index thì mỗi nhịp là một **seq scan toàn bảng** — càng dọn
-- càng chậm, và trên bảng lớn thì job biến thành nguồn tải thay vì giảm tải.
--
-- Index **partial** (`WHERE status = 'SENT'`): row PENDING không bao giờ bị dọn
-- (chúng là event chưa gửi) ⇒ không cần nằm trong index. Index nhỏ hơn, và không
-- tranh chấp với đường quét của relay (`idx_outbox_pending`).
-- ==============================================================================

CREATE INDEX idx_outbox_sent ON outbox (sent_at) WHERE status = 'SENT';
