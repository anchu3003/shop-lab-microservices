-- =============================================================================
-- V2__create_refresh_tokens.sql — bảng refresh token của identity-service (SL-1.4)
--
-- Vì sao KHÔNG sửa V1: Flyway lưu checksum, sửa file đã chạy sẽ làm `validate` fail ở mọi môi
-- trường đã migrate. Đổi schema = file V2 mới (xem comment đầu V1).
--
-- Thiết kế:
--   - `token_hash` lưu SHA-256 (hex, 64 ký tự) của token thật — DB bị lộ cũng KHÔNG dùng lại được
--     token. Token thật là chuỗi random 256-bit, chỉ client giữ.
--   - `replaced_by` tạo chuỗi rotation: token cũ trỏ tới token thay thế nó ⇒ phát hiện "dùng lại
--     token đã rotate" và revoke được cả chuỗi (reuse detection).
--   - `revoked_at` NULL = còn hiệu lực. Logout / rotate đều set mốc này (không xoá row để còn
--     truy vết).
--   - `expires_at`: refresh token sống dài (14 ngày) — đổi được bằng env, không cần migration.
-- =============================================================================

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by) REFERENCES refresh_tokens (id)
);

-- Tra cứu theo hash là đường nóng nhất (mỗi lần refresh) → phải unique + index.
CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Dùng khi cần thu hồi mọi token của một user (Phase sau: logout all devices).
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
