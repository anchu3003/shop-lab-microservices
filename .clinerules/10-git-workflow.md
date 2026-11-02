# 10 — Git workflow (bắt buộc)

Rule behavioral. Base convention ở `AGENTS.md` §6; hướng dẫn đầy đủ ở `docs/git-flow.md`.

## Mô hình: GitHub Flow

- `main` **luôn** ở trạng thái chạy được.
- **Không** commit trực tiếp lên `main`.
- Mỗi ticket → 1 nhánh ngắn hạn → PR → merge (squash) vào `main` → xoá nhánh.

## Commit

- Conventional Commits: `feat|fix|docs|refactor|test|chore(scope): mô tả`.
- **Commit sau khi hoàn thành mỗi ticket** — tức là sau khi đã verify thật, trước khi sang ticket tiếp theo.
  Không để thay đổi treo lơ lửng qua nhiều ticket.
- 1 ticket có thể có nhiều commit nhỏ; commit cuối đóng ticket (nhắc ticket ID trong body nếu cần).
- **Không** commit secret / token / connection string.

## Branch

- Format: `<type>/<ticket-id>-<slug>` — ví dụ `feat/sl-0.2-repo-skeleton`.
- `type` ∈ {feat, fix, docs, refactor, test, chore}; `ticket-id` = `SL-X.Y`.
- Nhánh ngắn hạn; xoá sau khi merge.

## PR

- Mỗi nhánh → 1 PR vào `main`. PR body gồm: tóm tắt, ticket ID, lệnh verify + output.
- **Không** merge khi chưa verify thật (`AGENTS.md` §9).

## An toàn

- **Không** force-push lên `main`.
- **Không** commit thẳng `main`.
- Nhánh chính phải tên `main` (nếu repo còn `master`: `git branch -m master main`).
