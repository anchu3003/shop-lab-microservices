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
