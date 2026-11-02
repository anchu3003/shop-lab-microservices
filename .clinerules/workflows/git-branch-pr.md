# /git-branch-pr.md — Tách nhánh và tạo PR

<task_objective>
Tách nhánh mới cho ticket đang làm và tạo Pull Request vào nhánh `main` theo GitHub Flow
(`AGENTS.md` §6, `.clinerules/10-git-workflow.md`, `docs/git-flow.md`).
</task_objective>

<detailed_sequence_of_steps>
0. **Tiền đề**: kiểm tra `git remote -v` (có remote chưa) và `gh --version` (có GitHub CLI chưa).
   - Thiếu remote → báo user, đề xuất `git remote add origin <url>` rồi dừng.
   - Thiếu `gh` → vẫn push được, nhưng bước tạo PR chuyển sang thủ công (in link compare).
1. Xác định **ticket** (mặc định "Ticket đang làm"; nếu trống → hỏi) và **type** (feat/fix/docs/refactor/test/chore).
2. Đảm bảo base branch tên `main`: nếu repo đang ở `master` → `git branch -m master main`.
3. Cập nhật base: `git checkout main && git pull --ff-only`.
4. Tạo nhánh: `git checkout -b <type>/<ticket-id-lower>-<slug>` (ví dụ `feat/sl-0.2-repo-skeleton`).
5. Làm việc + commit theo Conventional Commits. **Commit sau khi hoàn thành ticket** (sau khi verify thật).
6. Push: `git push -u origin <branch>`.
7. Tạo PR:
   - Có `gh`: `gh pr create --base main --fill` (hoặc set `--title`/`--body` rõ ràng: tóm tắt + ticket + verify).
   - Không có `gh`: in hướng dẫn + link so sánh để tạo PR thủ công trên GitHub.
8. **Report**: link PR, tên nhánh, commit đã tạo, việc còn lại.
</detailed_sequence_of_steps>
