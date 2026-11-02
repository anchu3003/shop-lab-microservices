#!/usr/bin/env bash
# protect-main.sh — chặn push trực tiếp lên nhánh 'main'
#
# Dùng bởi lefthook (hook pre-push).
# Lý do tồn tại: repo private + gói GitHub Free KHÔNG bật được branch protection /
# ruleset (API trả 403) → dùng git hook local thay thế. Xem Backlog.md → SL-0.8, SL-0.2.

set -euo pipefail

branch="$(git rev-parse --abbrev-ref HEAD)"

if [ "$branch" = "main" ]; then
  cat >&2 <<'EOF'

❌ Không push trực tiếp lên 'main'.

   Quy trình (docs/git-flow.md):
     1. git checkout -b <type>/<ticket-id>-<slug>
     2. git commit -m "<type>(<scope>): ..."
     3. git push -u origin <branch>
     4. gh pr create --base main

   Thật sự cần bỏ qua (rất hạn chế): git push --no-verify

EOF
  exit 1
fi

exit 0
