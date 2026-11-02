#!/usr/bin/env bash
# secret-scan.sh — quét thô các mẫu giống secret trong file staged
#
# Dùng bởi lefthook (hook pre-commit).
# KHÔNG thay thế secret scanner thật — chỉ là lưới an toàn cuối (AGENTS.md §9).
# Placeholder trong .env.example là hợp lệ; đừng đặt secret thật ở đó.

set -euo pipefail

# Các mẫu đặc trưng, ít false-positive
pattern='(-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|gho_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|sk-[A-Za-z0-9]{32,})'

found=0
for f in "$@"; do
  [ -f "$f" ] || continue
  if grep -nEI -- "$pattern" "$f" >/dev/null 2>&1; then
    echo "❌ Nghi vấn secret trong: $f" >&2
    grep -nEI -- "$pattern" "$f" >&2 || true
    found=1
  fi
done

if [ "$found" -ne 0 ]; then
  cat >&2 <<'EOF'

→ Xoá secret khỏi commit (AGENTS.md §9). Nếu chỉ là placeholder thì để trong .env.example.

EOF
  exit 1
fi

exit 0
