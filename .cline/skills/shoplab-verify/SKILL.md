---
name: shoplab-verify
description: Verify một ticket ShopLab bằng lệnh thật (docker compose, curl /health, mvn/gradle test, lint) và dán output thật vào ticket. Dùng khi cần nghiệm thu / chứng minh một ticket đã chạy được, hoặc khi điền mục "Kết quả verify" trong Backlog.md.
---

# Verify ticket ShopLab

Nguyên tắc (`AGENTS.md` §4.4, §8, §9): **dán output thật**, không nói suông. Không claim "đã test" khi chưa chạy.

## Lệnh verify tham khảo theo phase

| Phase | Lệnh | Kỳ vọng |
|---|---|---|
| 0.3 | `docker compose up -d` rồi `docker compose ps` | các container `healthy` |
| 0.4 | `curl -i http://localhost:<port>/health` | `200` |
| 0.4 | `curl -i http://localhost:<port>/ready` | `200` khi dependency OK, `503` khi tắt dependency |
| 0.5 | push code / xem CI | pipeline xanh |
| chung | `mvn -q verify` (Java) · `go test ./...` (Go) | pass |

> Lệnh chính xác lấy theo `README.md` và ticket — skill này chỉ gợi ý, không hard-code port.

## Quy trình

1. Xác định lệnh verify từ Acceptance Criteria của ticket.
2. Chạy lệnh, copy **output thật** (không tóm tắt mơ hồ).
3. Nếu fail → ghi rõ fail ở đâu, **không** đánh dấu Done.
4. Điền vào mục **Kết quả verify** của ticket: `lệnh đã chạy + output tóm tắt`.
5. Nếu không chạy được (thiếu môi trường) → nói rõ lý do, chuyển ticket sang `Blocked`.

## Không được làm

- ❌ Ghi "đã test, chạy tốt" mà không có output.
- ❌ Đánh dấu `Done` khi chưa chạy verify thật.
