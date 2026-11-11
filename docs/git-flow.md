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
| **`[skip ci]`** trong commit message | Commit docs **tách riêng** trong PR có code | GitHub đọc ở **HEAD commit của PR** → xem cảnh báo dưới |

**Mặc định KHÔNG cần token**: theo flow *1 ticket = 1 commit*, mark done được gộp vào commit cuối **có code** ⇒ CI
chạy bình thường, không phải nghĩ tới `[skip ci]`. Token chỉ còn dùng khi bạn cố ý push một commit docs riêng.

⚠️ **Bẫy**: nếu commit docs là **commit cuối** của PR, `[skip ci]` sẽ skip luôn run `pull_request` của cả PR →
check trên PR treo **Pending**. Repo không bật required checks nên vẫn merge được, nhưng phải hiểu: **CI đã xanh ở
commit code ngay trước đó**, và phần phát sinh chỉ là docs.
`tools/merge-pr.sh` kiểm chính xác điều này trước khi merge (diff sau commit xanh phải là docs-only).

⚠️ **Bẫy ngược lại — token nằm trong BODY**: GitHub đọc skip instruction ở **cả body** của HEAD commit, không chỉ subject.
Gặp thật 2026-11-05: một commit `docs(process)` sửa `.github/workflows/ci.yml` **và** có câu giải thích *"nên KHÔNG dùng
`[skip ci]`"* ⇒ run `pull_request` bị **skip**, PR **không có check nào** dù diff có file không phải docs.
⇒ Khi muốn CI chạy, **đừng viết token skip dạng literal** ở bất kỳ đâu trong commit message (viết *token skip CI*).

## Mark done — gộp vào commit CUỐI của ticket (không để dành PR sau)

Verify xong thì sửa `Backlog.md` (status + Kết quả verify + Learning note + bảng phase) **ngay trong nhánh
của ticket**, rồi gộp vào commit cuối:

```bash
git reset --soft origin/main && git commit -m "feat(x): ... (SL-1.3)"   # hoặc: git rebase -i origin/main → squash
git push --force-with-lease                                              # CHỈ nhánh feature
```

⇒ **1 push = 1 run CI trên PR** (+ 1 run trên `main` sau merge). Không sinh commit đóng-ticket rời, không mở PR phụ,
không để ticket "chưa đóng" chờ sang PR sau.

| Trường hợp | Cách làm |
|---|---|
| **Mark done của ticket** | Gộp vào **commit cuối của chính ticket** (commit đó phải **có code**) → cùng PR |
| **PR chỉ-docs** (ticket process, hoặc nhánh chỉ sửa doc) | Không có run nào (`paths-ignore`) — `tools/merge-pr.sh` **cho merge** khi toàn bộ diff là docs-only |
| **Docs không thuộc ticket nào** (sửa doc lặt vặt, tách `Backlog-phase-<N>.md`) | Gộp vào **commit của ticket kế tiếp**; chỉ mở nhánh + PR riêng khi **không còn ticket kế tiếp** |
| Thay đổi **có code** | ❌ Không gộp — phải là ticket riêng (`AGENTS.md` §3.1) |

**Evidence sau merge** (merge sha · run CI trên `main`): không thể nằm trong commit ⇒ `tools/merge-pr.sh` **post comment
hậu merge** lên PR (0 commit, 0 phút CI).

**Trade-off đã chấp nhận**: PR chỉ có 1 commit nên `git log` trên `main` không thấy tiến trình trung gian — đổi lại
**tiết kiệm 1 nhịp chờ** (không phải mở PR thứ hai chỉ để đóng ticket) và không dính bẫy `[skip ci]` ở HEAD.
Model "`main` luôn xanh + mọi thay đổi đi qua PR" **không** bị phá vì vẫn không commit thẳng `main`.

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
