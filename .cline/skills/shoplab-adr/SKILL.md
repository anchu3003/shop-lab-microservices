---
name: shoplab-adr
description: Viết hoặc cập nhật Architecture Decision Record (ADR) cho ShopLab theo template 5 mục (Context / Options / Decision / Consequences / Revisit). Dùng khi cần chốt một quyết định kiến trúc — tech stack, message broker, database, protocol, pattern — hoặc khi tạo/sửa file trong docs/adr/.
---

# Viết ADR cho ShopLab

Mục đích: biến một quyết định kiến trúc thành tài liệu để sau này không phải tranh luận lại.
ADR là **bắt buộc** khi thay đổi lớn: thêm service, đổi broker, đổi DB, đổi protocol (`AGENTS.md` §3.3).

## Vị trí & đặt tên

- Thư mục: `docs/adr/`
- Tên file: `NNNN-slug.md`, ví dụ `0001-tech-stack.md`, `0002-broker-choice.md`
- Đánh số tăng dần, không tái sử dụng số đã dùng.

## Template (đúng 5 mục, giữ thứ tự)

```
# ADR-NNNN — <tên quyết định>

| | |
|---|---|
| **Trạng thái** | Proposed / Accepted / Superseded by ADR-XXXX |
| **Ngày** | YYYY-MM-DD |
| **Liên quan** | <ticket / ADR khác> |

## Context
Vấn đề đang gặp, ràng buộc (NFR, ngân sách, kỹ năng team, môi trường local).

## Options
Bảng so sánh từng phương án: Ưu / Nhược / Khi nào dùng. Nêu cả phương án KHÔNG chọn.

## Decision
Chọn phương án nào, vì sao — dựa trên tiêu chí nào (đối chiếu Context).

## Consequences
Hệ quả: được gì, mất gì, technical debt phát sinh, ảnh hưởng tới service khác.

## Revisit
Điều kiện cụ thể để xem lại quyết định (định lượng được, không ghi "khi cần").
```

## Quality bar (checklist trước khi coi ADR là xong)

- [ ] Nêu **lý do loại** phương án không chọn, không chỉ lý do chọn.
- [ ] Có bảng trade-off cho từng hạng mục.
- [ ] Mục **Revisit** có điều kiện định lượng được.
- [ ] Nếu quyết định thay `architecture.md` → đã update file đó, không để doc lệch ADR.

## Ví dụ Revisit tốt vs kém

| Kém | Tốt |
|---|---|
| "Revisit khi cần" | "Revisit khi p99 checkout > 500ms hoặc khi cần CDC (Phase 8)" |
| "Khi hệ thống lớn hơn" | "Khi write throughput > 5k RPS hoặc 1 bảng > 100M row" |
