#!/usr/bin/env bash
# ==============================================================================
# merge-pr.sh — verify + comment tóm tắt + SQUASH MERGE + xoá nhánh feature
#
# Quy tắc (nguồn: .clinerules/10-git-workflow.md):
#   1. CHỈ merge khi PR đang mở (không draft), CI xanh, và CÓ comment tóm tắt trên PR.
#   2. Docs-only commit ở cuối PR (dùng token skip CI — GitHub coi commit HEAD của PR) KHÔNG cần
#      CI cho chính nó. Script kiểm: nếu commit HEAD khác commit mà CI đã xanh, thì phần diff
#      phát sinh phải là **docs-only**, nếu không → từ chối merge.
#   3. (SL-1.14) PR **chỉ-docs** bị `paths-ignore` bỏ hết run ⇒ **0 run là hợp lệ**, nhưng chỉ cho
#      merge khi TOÀN BỘ diff của PR là docs-only. 0 run mà có file không phải docs ⇒ vẫn CHẶN.
#   4. Merge kiểu SQUASH + xoá nhánh feature (cả remote).
#   5. (SL-1.14) Sau merge, post **comment evidence hậu merge** (merge sha · run CI trên `main` ·
#      token skip) — vì theo rule "1 ticket = 1 commit", evidence sinh sau merge không còn chỗ ghi
#      trong commit; comment trên PR là chỗ ghi đó (0 commit, 0 phút CI).
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
# `.[0]` trên mảng rỗng trả `null` → nói rõ "chưa có run" thay vì in 'null' (dễ bị hiểu là lỗi lạ).
#
# NGOẠI LỆ (SL-1.14): PR **chỉ-docs** bị `paths-ignore` bỏ hết run ⇒ 0 run là HỢP LỆ. Nhưng chỉ cho
# merge khi **toàn bộ** diff của PR là docs-only; 0 run mà có file không phải docs ⇒ vẫn CHẶN
# (CI bị skip/tắt không có nghĩa là code đã được kiểm).
docs_only_pr="false"
if [ -z "$conclusion" ] || [ "$conclusion" = "null" ]; then
  git fetch -q origin main "$branch"
  pr_changed="$(git diff --name-only "origin/main...$head_sha")"
  if [ -z "$pr_changed" ]; then
    echo "merge-pr: nhánh '$branch' không có run CI nào và diff so với 'main' rỗng — dừng để an toàn." >&2
    exit 1
  fi
  while IFS= read -r file; do
    if ! docs_only "$file"; then
      echo "merge-pr: nhánh '$branch' KHÔNG có run CI nào mà diff có file không phải docs: $file" >&2
      echo "           → không merge mù. Bật lại CI (gh variable delete CI_ENABLED) rồi push lại." >&2
      exit 1
    fi
  done <<< "$pr_changed"
  echo "merge-pr: nhánh '$branch' không có run CI nào — cả PR là docs-only ⇒ được phép merge:"
  echo "  ✓ $(echo "$pr_changed" | tr '\n' ' ')"
  docs_only_pr="true"
  verified_sha="$head_sha"
elif [ "$conclusion" != "success" ]; then
  echo "merge-pr: run CI mới nhất của '$branch' là '$conclusion' (cần 'success'). Không merge." >&2
  exit 1
fi

# --- 3) Nếu HEAD khác commit đã verify → phần thêm phải là docs-only ----------
if [ "$docs_only_pr" = "false" ] && [ "$verified_sha" != "$head_sha" ]; then
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

if [ "$docs_only_pr" = "true" ]; then
  echo "merge-pr: PR #$pr ($branch) — PR chỉ-docs (không có run CI) · HEAD $head_sha"
else
  echo "merge-pr: PR #$pr ($branch) — CI xanh tại $verified_sha · HEAD $head_sha"
fi
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
# Chỉ đếm TOKEN đúng chuẩn GitHub (`[skip ci]` …). Đếm substring 'skip ci' sẽ báo sai khi body có câu ví dụ
# (đã gặp ở SL-1.10: in `skip-ci tokens: 1` trong khi run trên `main` vẫn chạy bình thường).
skip_tokens="$(git log -1 --format=%B | grep -cE '\[(skip ci|ci skip|no ci|skip actions|actions skip)\]' || true)"
echo "merge-pr: token skip trong merge message: $skip_tokens (mong đợi 0 → push lên 'main' sẽ chạy CI)"

# --- 5) Comment EVIDENCE HẬU MERGE (SL-1.14) ----------------------------------
# Theo rule "1 ticket = 1 commit", mark done gộp vào commit CUỐI của ticket ⇒ evidence sinh SAU merge
# (merge sha · run CI trên `main`) không còn chỗ ghi trong commit. Comment trên PR là chỗ ghi đó:
# 0 commit, 0 phút CI, vẫn có dấu vết vĩnh viễn.
merge_sha="$(git rev-parse HEAD)"
run_info="chưa thấy run nào cho $merge_sha (kiểm lại: gh run list --branch main)"
for _ in $(seq 1 18); do
  line="$(gh run list --branch main --limit 10 --json databaseId,headSha,status,conclusion \
      --jq "[.[] | select(.headSha==\"$merge_sha\") | \"\(.databaseId) \(.status)/\(.conclusion)\"] | first // \"\"" \
      2>/dev/null | tr -d '\r' || true)"
  if [ -n "$line" ]; then
    run_info="$line"
    case "$line" in *completed/*) break ;; esac
  fi
  sleep 5
done
if [ "$docs_only_pr" = "true" ]; then
  run_expect="PR chỉ-docs ⇒ push lên \`main\` KHÔNG chạy CI (\`paths-ignore\`) — không thấy run là ĐÚNG, không phải lỗi"
else
  run_expect="mong đợi 1 run \`push\` trên \`main\` (không thấy ⇒ kiểm token skip ở dòng trên)"
fi
evidence_file="$(mktemp)"
cat > "$evidence_file" <<EOF
## Evidence hậu merge (tự động — \`tools/merge-pr.sh\`)

| Mục | Giá trị |
|---|---|
| Merge commit | \`$merge_sha\` |
| Run CI trên \`main\` | $run_info |
| Token skip trong merge message | **$skip_tokens** (mong đợi 0) |
| Ghi chú | $run_expect |

Kiểm lại: \`gh run list --branch main --limit 1\` · \`git show $merge_sha --stat\`
EOF
gh pr comment "$pr" --body-file "$evidence_file" >/dev/null \
  && echo "merge-pr: đã post comment evidence hậu merge (merge sha + run trên 'main')."
rm -f "$evidence_file"
