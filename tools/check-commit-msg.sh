#!/usr/bin/env bash
# check-commit-msg.sh — enforce Conventional Commits (AGENTS.md §6, docs/git-flow.md)
#
# Dùng bởi lefthook (hook commit-msg): nhận $1 = đường dẫn file chứa commit message.
# Chạy tay để test:  bash tools/check-commit-msg.sh <file>

set -euo pipefail

msg_file="${1:-}"

if [ -z "$msg_file" ] || [ ! -f "$msg_file" ]; then
  echo "check-commit-msg: không tìm thấy file message ('$msg_file')" >&2
  exit 1
fi

# Dòng đầu tiên không phải comment và không rỗng
first_line="$({ grep -v '^#' "$msg_file" || true; } | sed '/^[[:space:]]*$/d' | head -n 1)"

# Cho qua các commit do git tự sinh
if printf '%s' "$first_line" | grep -Eq '^(Merge |Revert |fixup! |squash! )'; then
  exit 0
fi

pattern='^(feat|fix|docs|refactor|test|chore)(\([a-z0-9._/-]+\))?!?: .+'

if ! printf '%s' "$first_line" | grep -Eq "$pattern"; then
  cat >&2 <<EOF

❌ Commit message KHÔNG đúng Conventional Commits.

   Nhận được : ${first_line:-<rỗng>}
   Kỳ vọng   : <type>(<scope>): <mô tả>
   type      : feat | fix | docs | refactor | test | chore
   Ví dụ     : feat(order): add saga orchestration for checkout

   Xem AGENTS.md §6 và docs/git-flow.md.

EOF
  exit 1
fi

len=${#first_line}
if [ "$len" -gt 72 ]; then
  echo "⚠️  Dòng đầu commit dài $len ký tự (khuyến nghị ≤ 72)." >&2
fi

exit 0
