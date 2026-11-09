# Báo cáo async Phase 3 — số đo thật (`SL-3.8`)

> Mọi con số dưới đây lấy từ **một lần chạy** `bash tools/bench-async.sh` (in ra stdout dạng bảng,
> lưu raw ở `/tmp/bench-async-<ts>/`). Không có số nào chép tay từ trí nhớ; muốn kiểm thì chạy lại
> lệnh đó (§9).

## TL;DR

| Câu hỏi | Trả lời bằng số |
|---|---|
| Async có làm checkout nhanh hơn? | **Không.** p50 42,7 → 43,1 ms (**+0,9%**), p95 48,9 → 59,6 ms. Email **chưa bao giờ** nằm trên đường checkout ⇒ không có gì để tăng tốc |
| Vậy async mua được gì? | **Decoupling + độ trễ có trần.** Consumer chết ⇒ checkout vẫn p95 **44,6 ms**, 20 event chờ trong queue, bật lại ⇒ xử lý đủ **20/20** |
| Độ trễ event là bao nhiêu? | `confirm` → email: p50 **552 ms**, p95 **1011 ms**. `webhook` → order `CONFIRMED`: p50 **29 ms**, p95 **46 ms** |
| Relay gửi được bao nhiêu? | **63,7 msg/s** với 500 event — trần lý thuyết 100 msg/s (batch 100 × nhịp 1 s) ⇒ **nghẽn ở relay polling**, không phải broker |
| Có mất/trùng message không? | 100 checkout + 100 update trong 5 phút: **DLQ = 0**, phát = xử lý (**100/100** ở cả 3 luồng), **0** email trùng `event_id` |

## 1. Điều kiện đo

| Thông số | Giá trị |
|---|---|
| commit | `f1aee5e` (sau `SL-3.7`) |
| thời điểm | 2026-11-09T17:40:39Z (UTC) |
| máy | WSL2 · Intel i5-12600K (12 thread) · 19 GB RAM · Docker Engine 29.9.3 |
| stack | `docker compose` 13 container: Postgres 16, Redis 7, RabbitMQ 4 + 10 service Java (JVM mặc định) |
| n | 50 checkout tuần tự · 20 checkout đồng thời · 500 event cho relay · 100 checkout + 100 update cho tải |
| cửa sổ quan sát DLQ | 300 s |
| p95 | nearest-rank: `idx = ceil(0,95 × n) − 1` trên mẫu đã sort |

**"before" nghĩa là gì**: `docker stop shoplab-notification-service` — tức **không có consumer async
nào chạy**, event chỉ nằm trong queue. Đây không phải "code Phase 2" (không thể revert trong 1 ticket)
mà là trạng thái *không có ai tiêu thụ event* — đúng thứ cần so, và cũng chính là phép thử decoupling ở §6.

## 2. Latency checkout — before / after

| Chỉ số | before (không consumer) | after (có consumer) | chênh |
|---|---|---|---|
| tuần tự p50 | **42,7 ms** | **43,1 ms** | +0,4 ms (+0,9%) |
| tuần tự p95 | **48,9 ms** | **59,6 ms** | +10,7 ms (+21,9%) |
| đồng thời 20 p50 | **178,4 ms** | **182,2 ms** | +3,8 ms (+2,1%) |
| đồng thời 20 p95 | **197,6 ms** | **203,6 ms** | +6,0 ms (+3,0%) |

Đọc số cho đúng:

- p50 gần như **không đổi** (+0,4 ms) ⇒ không có lời gọi đồng bộ nào bị thêm vào đường checkout.
- p95 chênh +10,7 ms nhưng **cả hai trạng thái đều < 60 ms**; khi chạy "after" thì consumer đang bận ghi
  `notification_logs` xuống **cùng** Postgres ⇒ đây là nhiễu tải, không phải phụ thuộc. Bằng chứng ngược:
  khi consumer **chết** (§6) p95 còn **44,6 ms** — thấp hơn cả "after".
- Đồng thời 20: p95 ~200 ms ở **cả hai** trạng thái ⇒ nghẽn nằm ở tầng dùng chung (1 Postgres, pool
  Hikari nhỏ, 1 saga transaction), **không** phải ở async. Đây là số của Phase 3, không phải số flash sale (Phase 4).

## 3. Độ trễ event (server-side, ghép 2 database bằng timestamp)

| Chỉ số | n | p50 | p95 | max | Cách đo |
|---|---|---|---|---|---|
| `confirm` → email | 70 | **551,9 ms** | **1010,6 ms** | 1057,6 ms | `order_status_history.created_at` (→ `CONFIRMED`) vs `notification_logs.sent_at` |
| `webhook` → order `CONFIRMED` | 70 | **28,6 ms** | **45,9 ms** | 49,9 ms | `payment_webhook_events.received_at` vs `order_status_history.created_at` |

- 552 ms p50 / 1011 ms p95 khớp đúng thiết kế: **relay polling 1 s** ⇒ trễ tối đa ~1 s cho mỗi chặng
  event, cộng thêm consumer xử lý. Không có "realtime", và đó là đánh đổi đã chốt ở ADR-0007 D4.
- 29 ms là chặng **ngắn nhất trong chuỗi**, không phải "async nhanh": webhook ghi outbox trong cùng
  transaction → relay publish → consumer `q.order` chốt đơn bằng CAS. Đo `received_at → CONFIRMED` nên
  phần lớn là CAS + HTTP nội bộ, không phải chờ broker.

## 4. Throughput relay

| Chỉ số | Số đo | Ghi chú |
|---|---|---|
| event bơm | 500 `product.updated.v1` | 65,7 request/s ở phía producer (`PUT /v1/products`) |
| thời gian relay gửi hết | ~7,85 s | `min(outbox.created_at)` → `max(outbox.sent_at)` |
| **throughput** | **63,7 msg/s** | 500 event / 7,85 s |
| độ trễ đuôi (event cuối) | **0,26 s** | `max(created_at)` vs `max(sent_at)` |
| PENDING còn lại | 0 | relay đẩy hết, không kẹt |

**Nghẽn ở đâu**: `batch-size = 100`, `interval = 1 s` ⇒ trần lý thuyết **100 msg/s**; 500 event cần
**≥ 5 nhịp = 5 s**. Đo được 7,85 s ⇒ phần dư ~2,8 s là publish + confirm trên từng nhịp. Nếu broker là
nghẽn thì độ trễ đuôi (0,26 s) đã không nhỏ như vậy. Kết luận: **nghẽn là relay polling** — muốn nhanh
hơn thì tăng `batch-size`, giảm `interval`, hoặc chuyển sang CDC; cả ba là việc của Phase 4/8 và phải đo lại.

## 5. Bền vững dưới tải: DLQ = 0, không mất / không trùng

Điều kiện: **100 checkout + 100 product update**, quan sát **300 s**.

| Kiểm tra | Kết quả |
|---|---|
| DLQ max trong 300 s (`q.notification.dlq`, `q.order.dlq`, `q.search.dlq`) | **0** |
| `order.confirmed.v1` phát (outbox SENT) / xử lý (`processed_events` notification) | **100 / 100** |
| `payment.succeeded.v1` phát / xử lý (order) | **100 / 100** |
| `product.updated.v1` phát / xử lý (search) | **100 / 100** |
| email ghi được / `event_id` duy nhất | **100 / 100** (không có email trùng) |

Đối chiếu theo **cửa sổ thời gian** chứ không JOIN xuyên database (3 DB riêng — `database per service`).

## 6. Degradation — bằng chứng của decoupling

| Bước | Số đo |
|---|---|
| `docker stop notification-service`, chạy 20 checkout | p50 **35,8 ms** · p95 **44,6 ms** — **không tệ hơn** lúc bình thường |
| event tồn lại | `q.notification ready = 20` · `unacked = 0` (không ai giữ, không ai nhận) |
| bật lại service | drain xong sau **9,3 s**, email **20/20** đơn |

9,3 s là **tổng từ lúc gọi `docker compose up`**: phần lớn là Spring Boot khởi động lại (~6 s), không
phải xử lý 20 event (consumer xử lý 20 event trong < 2 s). Vì vậy **không** dùng số này làm "throughput".


## 7. Kết luận trung thực

1. **Async không làm checkout nhanh hơn** — và không nên kỳ vọng điều đó. Email/index chưa bao giờ nằm
   trên đường checkout, nên "chuyển sang async cho nhanh" là câu sai về bản chất.
2. **Cái async mua được** (đo được, không suy đoán):
   - **Isolation**: `notification-service` chết ⇒ checkout p95 **44,6 ms**, đơn vẫn `CONFIRMED`, event
     chờ trong queue rồi xử lý đủ khi bật lại (**20/20**).
   - **Độ trễ có trần**: ≤ ~1 s cho mỗi chặng event nhờ relay 1 s — trần này *biết trước và đo được*,
     khác với "gọi sync rồi hy vọng service kia nhanh".
   - **Hạ tầng retry/DLQ dùng chung**: 100 + 100 thao tác dưới tải ⇒ 0 message mất, 0 message trùng.
3. **Khi nào KHÔNG dùng async**: cần nhất quán mạnh ngay khi API trả về (vd trừ tiền và trả số dư trong
   cùng response). Ở repo này những chỗ đó vẫn sync: `reserve`/`commit` tồn kho, `lock`/`commit` voucher.

## 8. Giới hạn đã biết (số đo hoặc suy ra từ cấu hình)

| # | Giới hạn | Bằng chứng | Hệ quả |
|---|---|---|---|
| 1 | Trễ mỗi chặng event ≤ ~1 s | p95 confirm→email **1011 ms** | không dùng cho UI realtime ("đang gửi email…" phải chờ ~1 s) |
| 2 | Relay ~63,7 msg/s | §4 | event bùng nổ ⇒ `outbox` PENDING tăng (không mất, nhưng trễ tăng tuyến tính) |
| 3 | 1 consumer + `prefetch=1` mỗi queue | `concurrency: 1`, `prefetch: 1` | trần xử lý ~1 event/lần; **retry giữ `unacked`** ⇒ message kế tiếp chờ hết 4,2 s backoff |
| 4 | DLQ **không** auto-replay | ADR-0007 D5 | message lỗi cần người; `tools/dlq.sh` là đường duy nhất |
| 5 | Bảng chỉ tăng, chưa có job dọn | sau buổi đo: `processed_events` = 715 (search) · 359 (order) · 382 (notification); `outbox` = 715 (catalog) · 380 (order) · 359 (payment); `notification_logs` = 382 | `TD-30` (processed_events + notification_logs) · `TD-35` (outbox) |
| 6 | Index search trễ y như event | `SL-3.6` verify | cần `POST /v1/admin/search/reindex` khi lệch; `min_price` chưa tính SKU override (`TD-33`) |
| 7 | Không có refund cho ca "tiền đã thu, đơn đã EXPIRED" | `SL-3.5` verify | chỉ log ERROR + xử lý tay (`TD-31`) |
| 8 | Phân trang offset | `TD-6` | `OFFSET` lớn quét nhiều row |

## 9. Cách tái lập

```bash
# stack đang chạy (docker compose up -d --wait)
bash tools/bench-async.sh      # ~7 phút: M1→M5, in bảng markdown + lưu raw ở /tmp/bench-async-<ts>/

# bản nhanh để thử script (không dùng số của bản nhanh để báo cáo)
N_SEQ=5 N_CONC=5 N_RELAY=20 N_LOAD=5 WINDOW_S=30 bash tools/bench-async.sh
```

Số tuyệt đối chỉ có nghĩa trong điều kiện ở §1 — đổi máy hoặc có tải nền thì phải đo lại, **không** suy
diễn từ bảng này sang môi trường khác.

