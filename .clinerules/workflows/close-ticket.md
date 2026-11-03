# /close-ticket.md — Đóng ticket đã verify

<task_objective>
Đóng một ticket **đã verify thật**: cập nhật `Backlog.md` (status, Kết quả verify, Learning note)
và ghi vào Learning log.
</task_objective>

<detailed_sequence_of_steps>
1. Xác nhận ticket đã chạy verify thật (có output). Nếu chưa → **không đóng**, báo lại.
2. Đổi trạng thái ticket: `In Progress` → `Review` → `Done` (dùng `Review` nếu cần user review trước).
3. Điền "Kết quả verify": lệnh đã chạy + output tóm tắt.
4. Điền "Learning note": học được gì, cái gì bất ngờ, lần sau làm khác thế nào.
5. Thêm dòng vào mục "Learning log" (ngày + ticket + học được).
6. Cập nhật bảng "Trạng thái theo phase" + header (Ticket đang làm / Ticket tiếp theo).
7. Nếu design đổi → đảm bảo `architecture.md` + ADR đã update.
8. Nếu **phase đã xong** (DoD của phase — `AGENTS.md` §5): tách `Backlog-phase-<N>.md`, viết `docs/phases/phase-N.md`,
   thêm dòng vào bảng "Archive các phase đã xong". Các file này đi cùng PR đóng phase.
9. **Commit**: còn ticket kế tiếp → gộp commit đóng ticket vào **nhánh của ticket kế tiếp**
   (không mở nhánh/PR riêng cho doc — xem `docs/git-flow.md`).
10. **Sau khi PR đóng phase đã merge**: gắn tag annotated `phase-N` lên **merge commit** + tạo **GitHub Release**
    (tag/release không cần PR, không trigger CI — ADR-0005).
</detailed_sequence_of_steps>
