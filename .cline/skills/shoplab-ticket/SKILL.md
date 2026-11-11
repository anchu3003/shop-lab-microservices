---
name: shoplab-ticket
description: Tạo mới hoặc cập nhật ticket trong Backlog.md của ShopLab theo format BA-style (Bối cảnh → Mục tiêu → In scope → Out of scope → Acceptance Criteria → Ghi chú kỹ thuật → Kết quả verify → Learning note) kèm checklist DoR/DoD. Dùng khi cần breakdown epic thành ticket chi tiết hoặc khi tạo/sửa ticket.
---

# Ticket ShopLab (BA-style)

## Convention (AGENTS.md §7)

- **ID**: `SL-<phase>.<số>` — không đổi ID sau khi tạo.
- **Trạng thái**: `Todo` → `In Progress` → `Review` → `Done` (hoặc `Blocked` + lý do).
- **Ước lượng**: `S` (< nửa ngày) / `M` (1–2 ngày) / `L` (3–5 ngày). Nếu `L` → **phải tách nhỏ**.
- Viết như **BA đã define xong**: mô tả được, verify được, không mơ hồ.
- Phase xa chỉ để mức **epic**; chỉ breakdown thành ticket chi tiết khi tới lượt.

## Thứ tự mục bắt buộc

1. Bảng `Trường | Giá trị` (Trạng thái / Phase / Phụ thuộc / Ước lượng)
2. Bối cảnh
3. Mục tiêu
4. In scope
5. Out of scope
6. Acceptance Criteria (checklist `- [ ]`, verify được)
7. Ghi chú kỹ thuật (pattern / thư viện / trade-off đã chấp nhận)
8. Kết quả verify *(điền khi Done)*
9. Learning note *(điền khi Done)*

## Checklist DoR — chỉ bắt đầu khi đủ

- [ ] Bối cảnh & mục tiêu (why) rõ
- [ ] Scope rõ: in / out
- [ ] Acceptance Criteria dạng checklist, verify được
- [ ] Phụ thuộc: ticket nào phải xong trước
- [ ] Ghi chú kỹ thuật: pattern + trade-off chấp nhận

## Checklist DoD (AGENTS.md §5)

- [ ] Code chạy, pass lint + format
- [ ] Có test (unit bắt buộc; integration nếu có I/O)
- [ ] Đã verify bằng lệnh thật + dán output vào ticket
- [ ] Không hard-code secret / connection string; config qua env
- [ ] `Backlog.md` update status + learning note
- [ ] Doc/README update nếu cách chạy thay đổi

## Khi tạo / sửa ticket

- Cập nhật bảng **Trạng thái theo phase** (số Done / tổng).
- Cập nhật header: **Ticket đang làm** / **Ticket tiếp theo**.
- **Không** thêm dòng Changelog/Learning log: `Backlog.md` §6 đã tách (xem `Backlog-log-archive.md`).
- Mơ hồ ảnh hưởng design → **hỏi user trước**, không đoán.
