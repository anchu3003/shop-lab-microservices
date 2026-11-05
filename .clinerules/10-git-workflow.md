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
- Trong lúc code có thể commit nhiều lần (local, chưa push). **Trước khi push để chạy CI → squash về 1 commit**
  (code + docs mark done); commit cuối phải có code, nhắc ticket ID trong subject/body.
- **Không** commit secret / token / connection string.

## Commit chỉ sửa docs → KHÔNG cần CI

- Commit **chỉ** đụng `*.md` (vd `Backlog.md`, `README.md`), `docs/**`, `.clinerules/**`, `.cline/**`, `LICENSE`
  → **không cần CI**: thêm `[skip ci]` vào **subject** commit.
- **Mặc định không phải nghĩ tới token**: mark done được gộp vào commit cuối **có code** (mục *Mark done* bên dưới)
  ⇒ CI chạy bình thường. Token chỉ dùng khi bạn cố ý push một commit docs **tách riêng**.
  ```bash
  git commit -m "docs(backlog): mark SL-1.2 done with CI evidence (SL-1.2) [skip ci]"
  ```
- Vì sao cần `[skip ci]` chứ `paths-ignore` chưa đủ: `paths-ignore` chỉ bỏ run khi **toàn bộ** diff là docs.
  Trong PR có code, mỗi commit docs vẫn kích hoạt run `pull_request` mới (PR diff có code) → tốn tiền vô ích.
- ⚠️ **GitHub đọc skip instruction ở commit HEAD của PR**: nếu commit docs là **commit cuối** của PR thì run
  `pull_request` bị skip → check trên PR treo "Pending" (repo này không bật required checks nên vẫn merge được,
  nhưng phải **hiểu rõ** là CI đã xanh ở commit code ngay trước đó).
  → `tools/merge-pr.sh` tự kiểm chuyện này: HEAD khác commit đã xanh thì phần diff phát sinh **phải là docs-only**.
- Push docs-only lên `main` (sau merge) đã được `paths-ignore` bỏ sẵn — không cần `[skip ci]`.
- ⚠️ **Đừng viết token skip dạng literal trong commit message nếu bạn MUỐN CI chạy** — GitHub đọc cả **body**, không chỉ
  subject, ở **HEAD commit của PR**. Đã gặp thật (2026-11-05): commit `docs(process)` sửa `ci.yml` + câu giải thích
  *"nên KHÔNG dùng `[skip ci]`"* ⇒ run `pull_request` **bị skip**, PR không có check nào. Khi cần nhắc tới token, viết
  **không có ngoặc** (vd *token skip CI*) hoặc để dạng khác.

## Mark done — gộp vào commit CUỐI của ticket (không để dành PR sau)

- Sau khi **verify thật**, sửa `Backlog.md` (status `Done` + Kết quả verify + Learning note + bảng phase + Changelog)
  **ngay trong nhánh của ticket** → rồi gộp vào commit cuối:
  ```bash
  git reset --soft origin/main && git commit -m "..."   # hoặc: git rebase -i origin/main → squash
  git push --force-with-lease                            # CHỈ nhánh feature — không bao giờ trên main
  ```
  ⇒ 1 push = **1 run CI trên PR** (+ 1 run trên `main` sau merge). **Không** tạo commit đóng-ticket rời, **không** mở
  PR phụ, **không** để ticket "chưa đóng" chờ sang PR sau.
- **Điều kiện bắt buộc**: commit cuối phải **có code** (hoặc cả PR là docs-only) — nếu commit HEAD là docs-only thì
  GitHub skip run `pull_request` ⇒ check treo `Pending`.
- **Evidence sau merge** (merge sha · run CI trên `main`) không thể nằm trong commit → `tools/merge-pr.sh`
  **post comment hậu merge** lên PR. Không cần commit cho việc này.
- **Docs không thuộc ticket nào** (sửa doc lặt vặt, tách `Backlog-phase-<N>.md`) → gộp vào **commit của ticket kế tiếp**;
  chỉ mở nhánh + PR riêng khi **không còn ticket kế tiếp** (hết phase / dừng dự án).
- Thay đổi **có code** thì **không** gộp — phải là ticket riêng (`AGENTS.md` §3.1).

## Branch

- Format: `<type>/<ticket-id>-<slug>` — ví dụ `feat/sl-0.2-repo-skeleton`.
- `type` ∈ {feat, fix, docs, refactor, test, chore}; `ticket-id` = `SL-X.Y`.
- Tạo nhánh khi **bắt đầu một ticket**; docs lặt vặt (không thuộc ticket nào) không tạo nhánh mới.
- Nhánh ngắn hạn; xoá sau khi merge.

## PR

- Mỗi nhánh → 1 PR vào `main`. PR body gồm: tóm tắt, ticket ID, lệnh verify + output.
- **Không** merge khi chưa verify thật (`AGENTS.md` §9).

## Merge tự động: squash + xoá nhánh

Điều kiện **đủ** để merge (không cần hỏi lại user):

1. Ticket `Done` (đã verify thật, output dán trong ticket).
2. CI **xanh** trên PR (`gh run list --branch <branch> --limit 1` → `success`).
3. Đã **comment tóm tắt** lên PR (người đọc PR biết đã verify cái gì).

→ Chạy:

```bash
bash tools/merge-pr.sh <pr-number> --summary-file /tmp/pr-summary.md
```

Script tự: kiểm PR mở/không draft → kiểm CI xanh (**PR chỉ-docs được miễn**: 0 run là hợp lệ khi **toàn bộ** diff là
docs-only — `SL-1.14`) → kiểm phần diff sau CI là docs-only (nếu có) → comment tóm tắt → `gh pr merge --squash
--delete-branch` → `git checkout main && git pull` → post **comment evidence hậu merge** (merge sha · run CI trên
`main` · token skip).

- ⚠️ Chạy script ở **foreground** (đừng `&`): bước comment evidence nằm **sau** merge — bị cắt tiến trình là **mất
  evidence** (đã gặp thật ở `SL-1.14`, phải post tay).
- Merge kiểu **squash** (1 commit sạch trên `main`) + **xoá nhánh feature** (cả remote).
- ⚠️ **Bẫy `[skip ci]` lọt vào merge commit** (`SL-1.10`): mặc định `gh pr merge --squash` ghép *title PR + subject của
  các commit trong PR* → `[skip ci]` của commit docs nằm luôn trong message của merge commit ⇒ **push lên `main` bị
  skip** (mất luôn lần kiểm chứng `main`). `tools/merge-pr.sh` xử lý bằng cách truyền tường minh
  `--subject`/`--body` và lọc sạch token skip khỏi body.
- Merge xong **bắt buộc kiểm** `main` thật sự có nội dung mới (`git show <merge-sha> --stat` hoặc `grep` file
  quan trọng) — bài học `SL-1.1`: commit cuối push race với lúc merge ⇒ mất nội dung khỏi `main`.
- Merge vào `main` sẽ **có** run CI trên `main` (đúng: `main` phải được kiểm chứng) — khác với docs-only push.
  Nếu **không** thấy run: kiểm `git log -1 --format=%B <merge-sha> | grep -c 'skip ci'` (xem bẫy trên).
- Ticket chỉ được coi là `Done` khi commit đóng ticket **đã có trên `main`**.

## An toàn

- **Không** force-push lên `main`.
- **Không** commit thẳng `main`.
- Nhánh chính phải tên `main` (nếu repo còn `master`: `git branch -m master main`).
