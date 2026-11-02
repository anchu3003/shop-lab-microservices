#!/usr/bin/env bash
# ==============================================================================
# reset-infra.sh — reset hạ tầng local về trạng thái ban đầu (ticket SL-0.3)
#
# Việc làm:  docker compose down --volumes  →  up -d  →  chờ tất cả healthy
# Dùng khi:  init SQL đổi, dữ liệu test rác, volume hỏng, muốn "máy sạch".
#
# Chạy (từ bất kỳ đâu):  bash tools/reset-infra.sh
#
# ⚠️  XOÁ SẠCH dữ liệu trong volume (Postgres / Redis / RabbitMQ). Không dùng cho
#     môi trường có dữ liệu cần giữ.
#
# Ghi chú: ticket gốc ghi `tools/reset-infra.ps1` (Windows). Đã đổi sang `.sh` vì
# ADR-0002 chốt dev env = WSL2 và mọi script khác trong `tools/` đều là bash.
# ==============================================================================

set -euo pipefail

# --- về thư mục gốc repo, để đường dẫn tương đối luôn đúng -------------------
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

compose_file="infra/docker-compose.yml"
env_file=".env"

# --- kiểm tra điều kiện ------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "❌ Không tìm thấy 'docker' trên PATH. Cài Docker trong WSL2 (README.md → Yêu cầu môi trường)." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "❌ Không có Docker Compose v2 ('docker compose')." >&2
  exit 1
fi

if [ ! -f "$compose_file" ]; then
  echo "❌ Không thấy '$compose_file'." >&2
  exit 1
fi

# Compose nạp `.env` từ project directory (thư mục chứa compose file = infra/),
# nên phải trỏ tường minh bằng --env-file tới `.env` ở gốc repo.
if [ ! -f "$env_file" ]; then
  if [ -f .env.example ]; then
    echo "⚠️  Chưa có '.env' → tạo từ '.env.example' (nhớ sửa password cho máy mình)."
    cp .env.example "$env_file"
  else
    echo "❌ Thiếu cả '.env' lẫn '.env.example'." >&2
    exit 1
  fi
fi

compose() {
  docker compose -f "$compose_file" --env-file "$env_file" "$@"
}

# Container mong đợi có mặt (khớp `container_name` trong compose file).
containers=(shoplab-postgres shoplab-redis shoplab-rabbitmq shoplab-adminer)

wait_healthy() {
  local timeout="${1:-180}" elapsed=0 status all_ok
  echo "→ Chờ container healthy (tối đa ${timeout}s)..."
  while :; do
    all_ok=1
    for c in "${containers[@]}"; do
      status="$(docker inspect -f \
        '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
        "$c" 2>/dev/null || echo missing)"
      case "$status" in
        healthy | running) ;;
        *)
          all_ok=0
          echo "   - $c: $status"
          ;;
      esac
    done

    if [ "$all_ok" -eq 1 ]; then
      echo "✅ Tất cả container đã healthy."
      return 0
    fi

    elapsed=$((elapsed + 3))
    if [ "$elapsed" -ge "$timeout" ]; then
      echo "❌ Timeout sau ${timeout}s. Xem log: docker compose -f $compose_file --env-file $env_file logs" >&2
      return 1
    fi
    sleep 3
  done
}

# --- thực thi ----------------------------------------------------------------
echo "→ Dừng container + xoá volume (mất sạch dữ liệu local)..."
compose down --volumes --remove-orphans

echo "→ Dựng lại hạ tầng..."
compose up -d

wait_healthy 180

echo "→ Trạng thái:"
compose ps
