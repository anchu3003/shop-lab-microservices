# /run-ticket.md — Thực thi 1 ticket

<task_objective>
Thực thi đúng **1 ticket** trong `Backlog.md` theo vòng Read → Plan → Build → Verify → Document,
không làm tràn scope, không build một loạt.
</task_objective>

<detailed_sequence_of_steps>
1. **Read**: đọc `AGENTS.md`, `architecture.md`, và ticket được chỉ định (mặc định lấy "Ticket đang làm";
   nếu trống thì lấy "Ticket tiếp theo"). Đọc ADR liên quan trong `docs/adr/` nếu có.
2. **Plan**: tóm tắt cách hiểu ticket, list các bước, nêu **giả định** + **câu hỏi mở**. Đối chiếu
   checklist DoR. Nếu có điểm mơ hồ ảnh hưởng design → **hỏi user, không đoán**. Dừng và chờ xác nhận.
3. Sau khi user đồng ý: đổi trạng thái ticket sang `In Progress` trong `Backlog.md`.
4. **Build**: code theo convention (`AGENTS.md` §6). Chia nhỏ, giữ diff nhỏ để review được.
5. **Verify**: chạy lint + test + lệnh thật của ticket (xem skill `shoplab-verify`). **Dán output thật**.
6. **Document**: điền "Kết quả verify" + "Learning note"; đổi trạng thái ticket; update
   `architecture.md` + ADR nếu design đổi; thêm dòng vào Learning log.
7. **Report**: summary ngắn — đã làm gì, file nào, lệnh để user tự verify, phần chưa làm / technical debt.
</detailed_sequence_of_steps>
