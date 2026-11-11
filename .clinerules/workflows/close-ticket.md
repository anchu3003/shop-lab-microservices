# /close-ticket.md — Đóng ticket đã verify

<task_objective>
Đóng một ticket **đã verify thật**: cập nhật `Backlog.md` (status, Kết quả verify, Learning note của ticket).
</task_objective>

<detailed_sequence_of_steps>
1. Xác nhận ticket đã chạy verify thật (có output). Nếu chưa → **không đóng**, báo lại.
2. Đổi trạng thái ticket: `In Progress` → `Review` → `Done` (dùng `Review` nếu cần user review trước).
3. Điền "Kết quả verify": lệnh đã chạy + output tóm tắt.
4. Điền "Learning note": học được gì, cái gì bất ngờ, lần sau làm khác thế nào.
5. **Không** có Learning log tổng để ghi — bài học chỉ nằm ở "Learning note" của ticket (bước 4).
6. Cập nhật bảng "Trạng thái theo phase" + header (Ticket đang làm / Ticket tiếp theo).
7. Nếu design đổi → đảm bảo `architecture.md` + ADR đã update.
8. Nếu **phase đã xong** (DoD của phase — `AGENTS.md` §5): tách `Backlog-phase-<N>.md`, viết `docs/phases/phase-N.md`,
   thêm dòng vào bảng "Archive các phase đã xong". Các file này đi cùng PR đóng phase.
9. **Commit + push (flow 1 ticket = 1 commit)**: gộp mark done vào **commit cuối của ticket** —
   `git reset --soft origin/main && git commit` (hoặc `git rebase -i origin/main` → squash) → `git push --force-with-lease`
   (chỉ nhánh feature). **KHÔNG** `[skip ci]` cho commit đóng ticket (commit có code ⇒ CI phải chạy), **KHÔNG** để dành
   mark done sang PR sau. Docs lặt vặt không thuộc ticket nào → gộp vào nhánh ticket kế tiếp (`docs/git-flow.md`).
10. **Merge tự động** khi đủ 3 điều kiện (ticket `Done` · CI xanh · có comment tóm tắt trên PR):
    `bash tools/merge-pr.sh <pr-number> --summary-file /tmp/pr-summary.md`
    → script tự squash merge + xoá nhánh feature. Sau merge **kiểm `main` thật sự có nội dung** (`git show <sha> --stat`).
11. **Sau khi PR đóng phase đã merge**: gắn tag annotated `phase-N` lên **merge commit** + tạo **GitHub Release**
    (tag/release không cần PR, không trigger CI — ADR-0005).
</detailed_sequence_of_steps>
