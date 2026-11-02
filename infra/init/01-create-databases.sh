#!/bin/bash
# ==============================================================================
# 01-create-databases.sh — tạo database ban đầu cho Postgres (ticket SL-0.3)
#
# Được Postgres image gọi MỘT LẦN, khi data dir còn rỗng
# (mount tại /docker-entrypoint-initdb.d/). Muốn chạy lại → phải xoá volume
# (`bash tools/reset-infra.sh`).
#
# Vì sao là .sh chứ không phải .sql:
#   Tên database nằm trong `.env` (POSTGRES_DB_*). Một file .sql không đọc được env,
#   sẽ phải lặp tên DB ở 2 nơi → dễ lệch. Script này đọc env rồi sinh SQL.
#
# Lưu ý: `order` là reserved word của SQL → luôn quote tên DB bằng `%I`/`"..."`.
#
# Chỉ tạo DATABASE, chưa tạo table. Migration schema của từng service sẽ do
# Flyway quản lý từ Phase 1 (mỗi service 1 schema riêng trong DB của nó).
# ==============================================================================

set -euo pipefail

# `pg_isready`-style env: entrypoint của image đã export PGHOST/PGUSER/PGPASSWORD.
# Vẫn nhận trực tiếp POSTGRES_USER để log cho rõ.
db_user="${POSTGRES_USER:?POSTGRES_USER chưa được set}"
db_main="${POSTGRES_DB:-$db_user}"

# Danh sách DB per service — thứ tự không quan trọng.
databases=(
  "${POSTGRES_DB_IDENTITY:-identity}"
  "${POSTGRES_DB_CATALOG:-catalog}"
  "${POSTGRES_DB_INVENTORY:-inventory}"
  "${POSTGRES_DB_PROMOTION:-promotion}"
  "${POSTGRES_DB_ORDER:-order}"
  "${POSTGRES_DB_PAYMENT:-payment}"
  "${POSTGRES_DB_NOTIFICATION:-notification}"
)

create_database() {
  local db="$1"

  # Bỏ qua giá trị rỗng (env bị unset) để script không tạo DB tên rỗng.
  if [ -z "$db" ]; then
    return 0
  fi

  echo "init: tạo database '$db' (owner=$db_user)"

  # Idempotent: chỉ CREATE khi chưa tồn tại. `%I` quote identifier an toàn
  # (xử lý được `order`). `\gexec` chạy câu SQL vừa sinh ra.
  psql -v ON_ERROR_STOP=1 \
       --username "$db_user" \
       --dbname "$db_main" <<-EOSQL
	SELECT format('CREATE DATABASE %I OWNER %I', '$db', '$db_user')
	WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '$db')\gexec
EOSQL
}

for db in "${databases[@]}"; do
  create_database "$db"
done

echo "init: xong — $db_user có $(psql -tAc "SELECT count(*) FROM pg_database WHERE datistemplate = false" --username "$db_user" --dbname "$db_main") database."
