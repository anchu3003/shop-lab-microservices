#!/usr/bin/env bash
# ==============================================================================
# merge-pr.sh — verify + comment tóm tắt + SQUASH MERGE + xoá nhánh feature
#
# Quy tắc (nguồn: .clinerules/10-git-workflow.md):
#   1. CHỈ merge khi PR đang mở (không draft), CI xanh, và CÓ comment tóm tắt trên PR.
#   2. Docs-only commit ở cuối PR (dùng `[skip ci]` — GitHub coi commit HEAD của PR) KHÔNG cần
#      CI cho chính nó. Script kiểm: nếu commit HEAD khác commit mà CI đã xanh, thì phần diff
#      phát sinh phải là **docs-only**, nếu không → từ chối merge.
#   3. Merge kiểu SQUASH + xoá nhánh feature (cả remote).
#
# Dùng:
#   bash tools/merge-pr.sh 10 --summary-file /tmp/pr-summary.md
#   bash tools/merge-pr.sh 10 --summary-file /tmp/pr-summary.md --dry-run
# ==============================================================================

set -euo pipefail

docs_only() {
  case "$1" in
    *.md|docs/*|.clinerules/*|.cline/*|LICENSE) return 0 ;;
    *) return 1 ;;
  esac
}

pr="${1:-}"
shift || true
summary_file=""
dry_run="false"

while [ $# -gt 0 ]; do
  case "$1" in
    --summary-file) summary_file="${2:-}"; shift 2 ;;
    --dry-run) dry_run="true"; shift ;;
    *) echo "merge-pr: tham số lạ: $1" >&2; exit 2 ;;
  esac
done

if [ -z "$pr" ]; then
  echo "Dùng: bash tools/merge-pr.sh <pr-number> --summary-file <file> [--dry-run]" >&2
  exit 2
fi
if [ -z "$summary_file" ] || [ ! -s "$summary_file" ]; then
  echo "merge-pr: thiếu --summary-file (PR phải có comment tóm tắt TRƯỚC khi merge)." >&2
  exit 2
fi
command -v gh >/dev/null 2>&1 || { echo "merge-pr: cần GitHub CLI (gh)." >&2; exit 2; }

# --- 1) PR phải đang mở và merge được -----------------------------------------
read -r state is_draft branch head_sha mergeable pr_url < <(
  gh pr view "$pr" --json state,isDraft,headRefName,headRefOid,mergeable,url \
    --jq '"\(.state) \(.isDraft) \(.headRefName) \(.headRefOid) \(.mergeable) \(.url)"'
)
[ "$state" = "OPEN" ] || { echo "merge-pr: PR #$pr không ở trạng thái OPEN ($state)." >&2; exit 1; }
[ "$is_draft" = "false" ] || { echo "merge-pr: PR #$pr đang là draft — bấm Ready for review trước." >&2; exit 1; }

# --- 2) CI phải xanh (run mới nhất của nhánh PR) ------------------------------
read -r conclusion verified_sha < <(
  gh run list --branch "$branch" --limit 1 --json conclusion,headSha --jq '.[0] | "\(.conclusion) \(.headSha)"'
)
if [ "$conclusion" != "success" ]; then
  echo "merge-pr: run CI mới nhất của '$branch' là '$conclusion' (cần 'success'). Không merge." >&2
  exit 1
fi

# --- 3) Nếu HEAD khác commit đã verify → phần thêm phải là docs-only ----------
if [ "$verified_sha" != "$head_sha" ]; then
  echo "merge-pr: CI xanh ở $verified_sha, HEAD hiện tại $head_sha — kiểm phần phát sinh..."
  git fetch -q origin "$branch"
  changed="$(git diff --name-only "$verified_sha" "$head_sha")"
  if [ -z "$changed" ]; then
    echo "merge-pr: diff rỗng (?) — dừng để an toàn." >&2
    exit 1
  fi
  while IFS= read -r file; do
    if ! docs_only "$file"; then
      echo "merge-pr: commit sau CI có thay đổi KHÔNG phải docs: $file → phải chạy CI lại. Không merge." >&2
      exit 1
    fi
  done <<< "$changed"
  echo "  ✓ phần phát sinh là docs-only (không cần CI): $(echo "$changed" | tr '\n' ' ')"
fi

echo "merge-pr: PR #$pr ($branch) — CI xanh tại $verified_sha · HEAD $head_sha"
if [ "$dry_run" = "true" ]; then
  echo "merge-pr: --dry-run → không comment, không merge. ($pr_url)"
  exit 0
fi

# --- 4) Comment tóm tắt rồi squash merge + xoá nhánh --------------------------
gh pr comment "$pr" --body-file "$summary_file" >/dev/null
echo "merge-pr: đã comment tóm tắt."

# Squash message phải ghi TƯỜNG MINH: mặc định `gh pr merge` ghép subject của các commit trong PR, nên
# `[skip ci]` của commit docs sẽ lọt vào message của merge commit ⇒ push lên `main` bị SKIP (đã gặp thật ở
# SL-1.9: merge xong `main` không có run CI nào). Body lấy từ summary đã lọc sạch mọi token skip.
pr_title="$(gh pr view "$pr" --json title --jq .title)"
sanitized_body="$(sed -E 's/\[(skip ci|ci skip|no ci|skip actions|actions skip)\]//g' "$summary_file")"
gh pr merge "$pr" --squash --delete-branch --subject "$pr_title" --body "$sanitized_body"

git checkout -q main && git pull -q --ff-only
echo "merge-pr: xong. main hiện tại:"
git --no-pager log --oneline -1
echo "merge-pr: kiểm tra (mong đợi 0 → squash message không có token skip):"
echo "  skip-ci tokens: $(git log -1 --format=%B | grep -c 'skip ci')"
