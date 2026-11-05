-- =============================================================================
-- V1__create_users.sql — bảng users của identity-service (SL-1.1)
--
-- Quy ước migration của repo:
--   - Mỗi service có schema riêng do Flyway quản lý (database per service).
--   - KHÔNG sửa file migration đã chạy (Flyway lưu checksum) → đổi schema = file V2__ mới.
--   - Tên file: V<số>__<mô tả>.sql
--
-- Lưu ý thiết kế:
--   - `email` lưu dạng CHỮ THƯỜNG (service normalize trước khi ghi) nên unique index thường
--     là đủ để chặn trùng "A@x.com" vs "a@x.com".
--   - `password_hash` là BCrypt (~60 ký tự) — varchar(100) để dư chỗ nếu đổi thuật toán sau này.
--   - `role` lưu dạng STRING (không dùng ordinal của enum: đổi thứ tự enum sẽ làm hỏng dữ liệu).
--   - Không đặt CHECK cho danh sách role: enum `Role` là nguồn sự thật, thêm role mới không nên
--     bắt buộc phải viết migration.
--   - Timestamp dùng `timestamptz` (Instant ở Java) — không lưu giờ "trần" để tránh lệch múi giờ.
-- =============================================================================

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

-- Email là định danh đăng nhập → phải duy nhất.
CREATE UNIQUE INDEX ux_users_email ON users (email);
