# Git flow — ShopLab

> Ngắn gọn, thực dụng. Nguồn sự thật convention: `AGENTS.md` §6. Rule behavioral: `.clinerules/10-git-workflow.md`.

## Mô hình: GitHub Flow (không dùng full GitFlow)

| Phương án | Ưu | Nhược | Chọn? |
|---|---|---|---|
| **GitHub Flow** (`main` + nhánh ngắn hạn + PR) | Đơn giản, `main` luôn chạy được, hợp CI/CD | Không có môi trường release tách biệt | ✅ |
| GitFlow (`develop`/`release`/`hotfix`) | Có release ổn định, hợp team lớn | Nặng, thừa cho lab 1 người | ❌ |
| Trunk-based (commit thẳng `main`) | Nhanh nhất | Không có chỗ review, `main` dễ vỡ | ❌ |

- **Lý do loại GitFlow**: dự án học 1 người, không có release train → thêm nhánh chỉ tăng ma sát.
- **Lý do loại trunk-based**: cần giữ `main` luôn xanh + có chỗ review trước khi merge.

## Nhánh

- Base: **`main`** (luôn ở trạng thái chạy được).
- Feature: `<type>/<ticket-id>-<slug>` — `feat/sl-0.2-repo-skeleton`, `fix/sl-3.1-order-double-charge`.
- `type` ∈ {feat, fix, docs, refactor, test, chore}.
- Xoá nhánh sau khi merge.

## Commit — Conventional Commits

```
<type>(<scope>): <mô tả ngắn>
```

- `type`: feat | fix | docs | refactor | test | chore
- `scope` (tuỳ chọn): tên service/module — ví dụ `order`, `gateway`
- Ví dụ: `feat(order): add saga orchestration for checkout`

**Quy tắc**: commit **sau khi hoàn thành mỗi ticket** (đã verify thật). Không gom nhiều ticket vào 1 commit.

## PR

- Mỗi nhánh → 1 PR vào `main`.
- PR body: tóm tắt, ticket ID, lệnh verify + output.
- Merge kiểu **squash** để `main` sạch.
- **Không** merge khi chưa verify thật.

## Merge tự động: squash + xoá nhánh

Ba điều kiện **đủ** — không cần hỏi lại user:

1. Ticket `Done` (đã verify thật, output nằm trong `Backlog.md`).
2. CI **xanh** trên PR.
3. Đã **comment tóm tắt** trên PR (tóm tắt + bảng AC + lệnh verify).

```bash
# 1) comment tóm tắt (chuẩn bị sẵn nội dung)
cat > /tmp/pr-summary.md <<'EOF'
## Tóm tắt verify
- `mvn verify`: BUILD SUCCESS — 37 + 39 + 16 IT + 71 test
- Container healthy · 401/200/503 đúng · traceId trùng ở log 2 service
- CI: 3/3 xanh (job `test` 1:49)
EOF

# 2) verify + comment + squash merge + xoá nhánh (cả remote) + cập nhật main local
bash tools/merge-pr.sh 10 --summary-file /tmp/pr-summary.md
#   thêm --dry-run nếu chỉ muốn xem script sẽ làm gì
```

Script **từ chối merge** khi: PR không OPEN, PR là draft, run CI mới nhất không `success`, hoặc commit sau
lần CI xanh có thay đổi **không phải docs**.

⚠️ **Bẫy `[skip ci]` lọt vào merge commit** (`SL-1.10`): mặc định `gh pr merge --squash` ghép *title PR + subject của
các commit trong PR* → `[skip ci]` của commit docs nằm luôn trong message của merge commit ⇒ **push lên `main` bị
skip** (mất lần kiểm chứng `main`). Script đã xử lý: truyền tường minh `--subject`/`--body` + lọc sạch token skip
khỏi body. Kiểm nhanh sau merge:

```bash
git log -1 --format=%B <merge-sha> | grep -c 'skip ci'   # mong đợi: 0
gh run list --branch main --limit 1                       # mong đợi: có run `push` mới
```

**Sau khi merge — bắt buộc kiểm `main` thật sự có nội dung** (bài học `SL-1.1`: commit cuối push race với lúc
merge ⇒ squash merge lấy state cũ ⇒ mất nội dung, rồi nhánh bị xoá):

```bash
git show <merge-sha> --stat | head
grep -n "<dòng quan trọng>" Backlog.md
```

Merge vào `main` **sẽ** tạo run CI trên `main` (đúng — `main` phải được kiểm chứng); chỉ **push docs-only** mới
được bỏ qua.

## Commit chỉ sửa docs → KHÔNG cần CI

Commit **chỉ** đụng `*.md` (`Backlog.md`, `README.md`, …), `docs/**`, `.clinerules/**`, `.cline/**`, `LICENSE`
→ thêm **`[skip ci]`** vào subject:

```bash
git commit -m "docs(backlog): mark SL-1.2 done with CI evidence (SL-1.2) [skip ci]"
```

| Cách | Khi nào đủ | Ghi chú |
|---|---|---|
| `paths-ignore` trong `ci.yml` | diff của **push/PR** toàn là docs | Đã có; đủ cho **PR chỉ-docs** và push docs lên `main` |
| **`[skip ci]`** trong commit message | Commit docs **nằm trong PR có code** | GitHub đọc ở **HEAD commit của PR** → xem cảnh báo dưới |

⚠️ **Bẫy**: nếu commit docs là **commit cuối** của PR, `[skip ci]` sẽ skip luôn run `pull_request` của cả PR →
check trên PR treo **Pending**. Repo không bật required checks nên vẫn merge được, nhưng phải hiểu: **CI đã xanh ở
commit code ngay trước đó**, và phần phát sinh chỉ là docs.
`tools/merge-pr.sh` kiểm chính xác điều này trước khi merge (diff sau commit xanh phải là docs-only).

## Commit minor — KHÔNG tạo nhánh/PR riêng

Commit nhỏ (đóng ticket, update `Backlog.md`/learning note, sửa doc lặt vặt, tách `Backlog-phase-<N>.md`)
**không** cần nhánh + PR riêng: gộp vào **nhánh của ticket kế tiếp** (commit riêng, message riêng).

| Trường hợp | Cách làm |
|---|---|
| **Còn ticket kế tiếp** | Gộp vào nhánh `feat/<ticket-id>-slug` của ticket kế tiếp → **cùng PR** |
| **Không còn ticket kế tiếp** (hết phase / dừng dự án) | Tạo nhánh `docs/<mô-tả-ngắn>` + PR riêng |
| Thay đổi **có code** | ❌ Không gộp — phải là ticket riêng (`AGENTS.md` §3.1) |

**Trade-off đã chấp nhận**: phần "đóng ticket" trễ một nhịp (nằm trong PR của ticket kế tiếp) — đổi lại không sinh
PR chỉ chứa doc. Model "`main` luôn xanh + mọi thay đổi đi qua PR" **không** bị phá vì vẫn không commit thẳng `main`.

## Lệnh mẫu

```bash
# 0) một lần: đổi tên nhánh chính (nếu repo còn dùng master)
git branch -m master main

# 1) tách nhánh cho ticket
git checkout main && git pull --ff-only
git checkout -b feat/sl-0.2-repo-skeleton

# 2) làm việc + commit sau khi xong ticket
git add -A
git commit -m "feat(repo): bootstrap skeleton and dev conventions"

# 3) push + tạo PR
git push -u origin feat/sl-0.2-repo-skeleton
gh pr create --base main --fill
```

## Tiền đề (làm 1 lần)

- Có **remote** GitHub: `git remote add origin git@github.com:<user>/<repo>.git`
- Cài **GitHub CLI** (`gh`) để tạo PR bằng lệnh; nếu không, tạo PR thủ công trên web.
- (Tuỳ chọn) Branch protection cho `main`: cấm push trực tiếp, bắt PR + CI xanh.

## Liên quan

- `.clinerules/10-git-workflow.md` — rule behavioral
- `.clinerules/workflows/git-branch-pr.md` — workflow `/git-branch-pr.md`
- `SL-0.2` — git hooks (**Lefthook**) enforce Conventional Commits
