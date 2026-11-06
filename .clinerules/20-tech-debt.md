# 20 — Tech debt register (bắt buộc ghi nhận)

Rule behavioral. Mục tiêu: **không để nợ kỹ thuật sống trong đầu ai** — mọi món nợ phải nằm ở một chỗ,
có **điều kiện revisit**, để phase sau biết cái gì phải trả.

## Nguồn sự thật

`Backlog.md` → **§ 7. Technical debt register** (`TD-<số>`). Không tạo file riêng, không ghi trong
comment code (comment code bị đọc 1 lần rồi quên; bảng thì còn).

## Khi nào PHẢI ghi một mục nợ

Ghi ngay trong **cùng ticket** phát hiện ra nó (không để "lát nữa"), khi gặp một trong các trường hợp:

- Chọn giải pháp **đơn giản hơn nhưng biết là chưa đủ** (vd phân trang offset thay vì keyset).
- Bỏ qua một việc **đáng ra nên làm** vì ngoài scope (vd chưa có job dọn `refresh_tokens`).
- Phát hiện **giới hạn đã biết** của giải pháp (vd `LIKE '%q%'` không dùng được index).
- Cố ý **hoãn** một rủi ro vận hành (vd `merge-pr.sh` chưa retry khi GitHub API lỗi).
- Copy-paste một đoạn code đáng ra phải ở `service-commons` (vd handler 403).

**Không** ghi: việc đã có ticket mở cho nó (trùng lặp), hoặc sở thích cá nhân ("thích dùng record hơn
class").

## Format một mục (bảng trong `Backlog.md` § 7)

| Cột | Nội dung | Ví dụ |
|---|---|---|
| `ID` | `TD-<số>` — tăng dần, **không tái sử dụng** số đã xoá | `TD-3` |
| `Mô tả` | 1 câu nêu **vấn đề**, không nêu giải pháp dài dòng | "Bảng `refresh_tokens` không có job dọn row hết hạn" |
| `Nguồn` | Ticket phát hiện | `SL-1.4` |
| `Điều kiện revisit` | **Bắt buộc**. Phải verify được bằng dữ liệu/sự kiện, không phải "khi rảnh" | "Khi bảng > 10^6 row hoặc khi thêm job scheduler" |
| `Mức độ` | `thấp` / `trung bình` / `cao` (cao = có thể gây sự cố/không thể vận hành) | `trung bình` |

## Khi nào trả nợ

- **Không** gộp việc trả nợ vào ticket khác nếu nó là **thay đổi code** (`AGENTS.md` §3.1) — phải ticket riêng.
- Được gộp khi đó là **docs/config 1 dòng** và cùng vùng code.
- Trả xong → đổi `Mức độ` thành `✅ đã trả (<ticket>)` và **giữ dòng** trong bảng (để còn dấu vết quyết định).
- Rà bảng **cuối mỗi phase** (khi làm DoD phase): món nào đã tới điều kiện revisit → tạo ticket cho phase kế.

## Liên quan

- `AGENTS.md` §4 (quy trình ticket) · §9 (việc không được làm)
- `Backlog.md` § 7 (bảng nợ) · Learning log § 6 (bài học, khác mục đích: bài học = điều đã hiểu, nợ = việc còn phải làm)
