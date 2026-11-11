# /new-ticket.md — Breakdown epic thành ticket

<task_objective>
Breakdown một epic (hoặc một mô tả tính năng) thành **ticket chi tiết** đúng format `Backlog.md`,
viết như BA đã define xong.
</task_objective>

<detailed_sequence_of_steps>
1. Đọc `AGENTS.md` §7 + template ticket trong `Backlog.md` §2 + epic liên quan.
2. Nếu thiếu thông tin để viết Acceptance Criteria verify được → **hỏi user**, không đoán.
3. Sinh ticket theo đúng thứ tự mục: Bối cảnh → Mục tiêu → In scope → Out of scope →
   Acceptance Criteria → Ghi chú kỹ thuật → Kết quả verify → Learning note.
4. ID dạng `SL-<phase>.<số>`, không đổi ID cũ; ước lượng `S`/`M`/`L`; nếu `L` → tách nhỏ.
5. Đặt ticket vào đúng phase trong `Backlog.md`; cập nhật bảng "Trạng thái theo phase" + header (Ticket tiếp theo).
   **Không** còn Changelog/Learning log tổng trong `Backlog.md` (`§6`) — đừng thêm dòng vào đó.
6. Không breakdown phase xa thành ticket chi tiết khi chưa tới lượt.
</detailed_sequence_of_steps>
