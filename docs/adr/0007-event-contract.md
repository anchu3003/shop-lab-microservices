# ADR-0007 — Event contract Phase 3 (envelope · naming · outbox relay · DLQ)

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-08 |
| **Liên quan** | `Backlog.md` → `SL-3.0` (chặn `SL-3.1` … `SL-3.9`) · [ADR-0006](0006-checkout-model.md) (checkout sync) · [ADR-0001](0001-tech-stack.md) (broker) · `architecture.md` §3 (NFR), §6.2 (webhook), §9.4 (contract), §11.2 (broker) · `TD-17` |

---

## Context

Phase 2 chạy **hoàn toàn synchronous**: `order-service` gọi 5 dependency trong một request. Nó đúng cho luồng tiền
(p99 < 500 ms, kết quả ngay), nhưng để lại 3 hệ quả:

1. Phần **không critical** (email, index search) buộc phải nằm trên đường checkout, hoặc không tồn tại — hiện là
   **không tồn tại**.
2. Webhook `payment.succeeded` **không** cập nhật được order (`TD-17`, mức `cao`): khách trả tiền mà không gọi
   `/confirm` ⇒ tiền đã thu, đơn treo `PENDING_PAYMENT` rồi `EXPIRED`.
3. Không có cách nào phát một sự kiện "đã xảy ra" **mà không mất** khi có sự cố giữa 2 bước (ghi DB xong, publish
   chưa xong).

Ràng buộc khi chốt contract:

- **Broker đã chốt = RabbitMQ** (`architecture.md` §11.2); **Kafka để Phase 8**. RabbitMQ cho **at-least-once**
  (redeliver khi consumer chết trước `ack`), **không** hứa exactly-once.
- **Database per service** ⇒ không có transaction chung giữa DB và broker. Đây chính là lý do outbox tồn tại.
- Hạ tầng local: 1 máy dev 15 GiB RAM; mỗi thành phần thêm vào (Debezium, schema registry, Kafka) phải xứng với
  bài học nó mang lại — `AGENTS.md` §3.4.
- 1 dev + AI, đi cuốn chiếu ⇒ contract phải **viết ra trước khi code** `SL-3.1`…`SL-3.9`, nếu không 6 ticket sau
  phải sửa lại từ gốc.

Năm ngã ba cần chốt, mỗi cái đổi sau đều tốn ≥ 1 ticket:

| # | Câu hỏi | Đổi sau thì tốn gì |
|---|---|---|
| **D1** | Envelope: tự định nghĩa hay dùng chuẩn (CloudEvents)? | Đổi field ⇒ mọi consumer + `processed_events` + E2E phải sửa; message cũ trong DLQ không đọc được |
| **D2** | Đặt tên & topology: routing key / queue / DLQ thế nào? | Đổi tên exchange/queue ⇒ phải migrate message đang nằm trong queue, sửa cả `tools/*.sh` |
| **D3** | Ngữ nghĩa giao: at-least-once hay exactly-once? | Chọn exactly-once ⇒ đổi cả mô hình (Kafka transaction / idempotent producer), bỏ outbox — làm lại từ đầu |
| **D4** | Outbox relay: polling hay CDC (Debezium)? | Đổi relay ⇒ thêm hạ tầng + đổi cách đánh dấu `SENT`, ảnh hưởng mọi service có outbox |
| **D5** | DLQ: retry ở đâu, ai replay? | Không có DLQ ⇒ message lỗi chặn queue (hệ dừng im lặng); auto-replay ⇒ có thể lặp lỗi vô hạn |

## Options

### 1. (D1) Định dạng envelope

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Tự định nghĩa envelope 7 field** (`eventId`, `eventType`, `version`, `occurredAt`, `producer`, `traceId`, `payload`) | Đúng thứ hệ cần, **không** field thừa; `traceId` buộc phải có (yêu cầu log xuyên service — `AGENTS.md` §6); dễ serialize bằng Jackson có sẵn | Không phải chuẩn công khai; nếu sau này tích hợp bên ngoài phải map | **Chọn** — mọi consumer là service nội bộ |
| **B. CloudEvents 1.0 (JSON)** | Chuẩn công khai, có SDK, dễ tích hợp bên thứ ba; attribute chuẩn (`id`, `source`, `type`, `time`) | Thêm dependency + thêm khái niệm phải học; field `source`/`specversion` không mang thêm thông tin cho hệ 1 team; mapping sang khái niệm của mình vẫn phải làm | Khi có consumer bên ngoài hoặc nhiều team |
| **C. Không envelope — publish thẳng DTO nghiệp vụ** | Ít code nhất | Không có `eventId` ⇒ **không dedupe được** (⇒ double charge); không có `version` ⇒ không tiến hoá được; DTO đổi field là consumer cũ chết | ❌ Không dùng |

### 2. (D2) Đặt tên & topology

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. 1 topic exchange `shoplab.events`, routing key `<aggregate>.<event>.v<N>`, queue **theo consumer** (`q.notification`, `q.order`, `q.search`) + `<queue>.dlq` qua `shoplab.dlx`** | Exchange tách producer khỏi consumer (thêm consumer = thêm binding, không sửa producer); routing key đọc là hiểu; queue theo consumer ⇒ mỗi service retry/DLQ **riêng**, không giẫm nhau | Nhiều binding phải khai báo đúng (sai routing key = message rơi vào hư không) ⇒ phải có `tools/rabbit-topology.sh` để kiểm | **Chọn** |
| **B. 1 queue cho mỗi event, nhiều consumer chia việc (competing consumers)** | Đơn giản, ít queue | 2 service khác nhau **chia** message ⇒ mỗi service chỉ nhận **một phần** sự kiện; thêm consumer thứ 2 cho cùng event là phải sửa topology | Khi consumer là **bản sao** của cùng một worker (scale out) |
| **C. Fanout exchange, mỗi consumer 1 queue tự sinh (anonymous)** | Thêm consumer không cần khai báo | Queue anonymous mất khi restart ⇒ **mất message**; không đặt được DLQ; không quan sát được | ❌ Không dùng |
| **D. Direct exchange, routing key = tên queue** | Dễ hiểu | Producer phải **biết** consumer (coupling ngược lại mục tiêu của event) | ❌ Không dùng |

### 3. (D3) Ngữ nghĩa giao

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. At-least-once + consumer idempotent theo `eventId`** | Đúng bản chất RabbitMQ; **không** cần transaction phân tán; mất mát = 0, trùng lặp được dedupe bằng unique key | Consumer **bắt buộc** có dedupe (nếu quên ⇒ gửi email 2 lần / cộng tiền 2 lần); dedupe store phình dần | **Chọn** |
| **B. Exactly-once (Kafka transaction / idempotent producer + read-process-write)** | Không phải dedupe ở consumer | Cần Kafka (Phase 8) + transaction giữa DB và broker vẫn **không** có ⇒ thực tế vẫn phải dedupe; đắt về vận hành | Phase 8 khi có Kafka và bài toán streaming thật |
| **C. At-most-once (không retry, ack trước khi xử lý)** | Không bao giờ trùng | **Mất message** khi consumer chết — đúng thứ phase này phải tránh | ❌ Không dùng cho sự kiện nghiệp vụ |

### 4. (D4) Outbox relay

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Polling relay** (`@Scheduled` 1 s, batch 100, `FOR UPDATE SKIP LOCKED`, publish + **publisher confirm** rồi đánh `SENT`) | Không thêm hạ tầng (1 bảng + 1 scheduler); nhiều instance an toàn nhờ `SKIP LOCKED`; dễ debug (nhìn thấy row `PENDING`/`SENT`/`attempts`); đo được bằng SQL | Trễ ≤ 1 interval (mặc định 1 s); quét bảng liên tục (rẻ khi có index `(status, created_at)`); batch là điểm nghẽn khi tăng tải | **Chọn** — quy mô Phase 3–7 |
| **B. CDC (Debezium → Kafka Connect) đọc WAL** | Trễ thấp (~ms), không polling; relay "miễn phí" khi đã có Kafka | Thêm 2 hạ tầng (Debezium + Kafka) chỉ để chuyển outbox; vận hành nặng trên máy dev 15 GiB; khó debug hơn một câu SQL | Phase 8+ khi đã có Kafka **và** cần latency thấp |
| **C. Publish ngay trong transaction (không outbox)** | Ít code nhất, "có vẻ" đúng | **Sai bản chất**: broker không tham gia transaction DB ⇒ hoặc mất event (publish fail sau commit) hoặc bịa event (commit fail sau publish); chính là vấn đề ADR này tồn tại để giải | ❌ Không dùng |
| **D. `LISTEN/NOTIFY` của Postgres + worker** | Không polling, gần realtime | Payload NOTIFY giới hạn 8 KB, **không** bền (mất notification khi worker chết) ⇒ vẫn phải fallback polling; thêm 1 cơ chế để hiểu | Khi đã có polling và cần giảm độ trễ |

### 5. (D5) Retry & DLQ

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Retry trong consumer 3 lần (200 ms / 1 s / 3 s) rồi `nack(requeue=false)` ⇒ `<queue>.dlq`; replay bằng tay (`tools/dlq.sh`)** | Log được từng lần thử, thấy `unacked` khi đang retry; DLQ chỉ chứa message **đã** cạn kiên nhẫn ⇒ tín hiệu sạch để alert | Consumer giữ thread ~4,2 s khi retry (giảm xử lý song song); lỗi **vĩnh viễn** (payload sai) vẫn tốn 3 lần thử | **Chọn** |
| **B. Không retry, đẩy DLQ ngay lần đầu lỗi** | Queue không bị chặn, DLQ là "hộp thư lỗi" | Lỗi tạm thời (mất mạng 1 nhịp, DB restart) cũng phải can thiệp tay ⇒ tăng việc vận hành | Khi lỗi luôn là vĩnh viễn (validation) |
| **C. Retry vô hạn (requeue mãi)** | Không bao giờ mất | Message lỗi **quay vòng** chiếm consumer ⇒ **chặn cả queue**, đúng thứ DLQ sinh ra để tránh | ❌ Không dùng |
| **D. DLQ auto-replay theo lịch** | Tự phục hồi | Replay lặp lại lỗi ⇒ vòng lặp vô hạn giữa DLQ và queue chính; không ai biết message đó có nên replay | ❌ Không dùng — replay phải có người quyết định |


## Decision

Chọn **A** cho cả 5 điểm.

**D1 — envelope (JSON, 7 field; **không** đặt field nghiệp vụ ở tầng envelope):**

```json
{
  "eventId": "0f0b1a2c-9d1e-4b3f-9a6b-6f0e1d2c3b4a",
  "eventType": "order.confirmed.v1",
  "version": 1,
  "occurredAt": "2026-11-08T09:15:04Z",
  "producer": "order-service",
  "traceId": "6b1c9f2e-…",
  "payload": { "orderId": "…", "userId": "…", "total": 200000.00, "currency": "VND" }
}
```

**D2 — topology & đặt tên:**

| Thành phần | Tên | Ghi chú |
|---|---|---|
| Exchange sự kiện | `shoplab.events` (topic, durable) | producer publish với `routingKey = eventType` |
| Exchange chết | `shoplab.dlx` (direct, durable) | DLQ bind vào đây |
| Queue | `q.notification` · `q.order` · `q.search` | **mỗi consumer một queue**; durable, **không** `auto-delete` |
| DLQ | `q.notification.dlq` · `q.order.dlq` · `q.search.dlq` | bind `shoplab.dlx`, routing key `<queue>` |
| Routing key | `<aggregate>.<event>.v<major>` | vd `order.confirmed.v1` · `payment.succeeded.v1` · `product.updated.v1` |

**D3 — ngữ nghĩa giao:** **at-least-once**. Mọi consumer **bắt buộc** dedupe theo `eventId` bằng bảng
`processed_events(event_id, consumer, event_type, processed_at)` với `PRIMARY KEY (event_id, consumer)` và
`insert … on conflict do nothing` ⇒ **`0 row` = đã xử lý**. **Ordering chỉ hứa trong 1 queue + 1 consumer** (single
active consumer mặc định của Spring AMQP) — **không** hứa ordering toàn cục.

**D4 — relay:** polling `@Scheduled` (mặc định 1 s, batch 100):
`SELECT … ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT :batch` → publish → **chờ publisher confirm** → `SENT`.
Lỗi ⇒ `attempts++` + `last_error`, **giữ `PENDING`** (event chưa gửi thì vẫn còn nghĩa vụ gửi). `attempts` chỉ để quan sát —
**không** tự chuyển `FAILED` (một event kẹt là tín hiệu cần người xem, không phải thứ để bỏ).

**D5 — retry & DLQ:** retry 3 lần (200 ms / 1 s / 3 s) **trong consumer**; cạn ⇒ `nack(requeue=false)` ⇒ DLQ. **Không**
auto-replay: `tools/dlq.sh list|move` để người vận hành xem payload, sửa nguyên nhân rồi replay. Trước khi replay **phải**
kiểm: (1) lỗi gốc đã hết chưa · (2) dedupe store — nếu message đã xử lý xong trước khi lỗi thì replay sẽ bị dedupe chặn
(đúng như mong đợi) · (3) replay **1** message hay cả queue.

**Bảng 5 event khởi điểm:**

| Routing key | Producer | Consumer | Payload | Phát khi |
|---|---|---|---|---|
| `order.confirmed.v1` | `order-service` (outbox) | `notification-service` | `orderId · userId · total · currency · itemCount · confirmedAt` | order → `CONFIRMED` (`SL-3.3`) |
| `order.cancelled.v1` | `order-service` (outbox) | `notification-service` | `orderId · userId · reason · cancelledAt` | order → `CANCELLED` (compensating / client cancel) |
| `payment.succeeded.v1` | `payment-service` (outbox) | `order-service` | `paymentIntentId · orderId · amount · currency · succeededAt` | webhook chuyển intent sang `SUCCEEDED` (`SL-3.5`, trả `TD-17`) |
| `product.updated.v1` | `catalog-service` (outbox) | `search-service` | `productId · name · description · categoryId · minPrice · status` | product tạo/sửa (`SL-3.6`) |
| `product.archived.v1` | `catalog-service` (outbox) | `search-service` | `productId · archivedAt` | soft delete (`SL-2.11`) |

**Versioning:** `.v<N>` nằm trong routing key **và** field `version`. **Không** sửa payload `.v1` (thêm field optional thì
được; đổi nghĩa/xoá field thì không). Thay đổi không tương thích ⇒ phát `.v2` **song song**, consumer hỗ trợ cả hai, chỉ bỏ
`.v1` khi không còn consumer nào bind. Message trong DLQ được coi là **bất biến**: replay sau khi đổi schema thì consumer
phải chịu được payload cũ.

## Consequences

**Được:**

- `order`/`payment` phát sự kiện **không mất** dù broker chết đúng lúc: state đã commit thì outbox còn đó, relay gửi lại.
- Consumer lỗi **không chặn** luồng chính và không chặn queue: retry → DLQ; `ready`/`unacked` là chỉ số quan sát được.
- Thêm consumer mới = thêm binding + thêm queue, **không** sửa producer (đúng mục tiêu decoupling).
- Đổi engine search (Phase 8: OpenSearch) **không** đổi contract ⇒ đổi storage, không đổi tích hợp.

**Mất / chấp nhận:**

- **Trễ ≥ 1 s** trên đường outbox → broker (polling interval). Với email/index là không đáng kể, nhưng **không** dùng
  outbox cho thứ cần realtime.
- **Trùng lặp là bình thường** ⇒ mọi consumer phải có `processed_events`; quên là bug im lặng (email gửi 2 lần).
- **Thêm 2 bảng phải dọn** ở mỗi service: `outbox` (row `SENT`) và `processed_events` — chưa có job retention ⇒ **ghi `TD`**
  khi làm `SL-3.3`/`SL-3.4`.
- **Không có schema registry**: contract giữ bằng kỷ luật + version trong routing key. Consumer nhận `version` lạ **phải**
  `nack` vào DLQ (không cố parse).
- **Ordering không hứa toàn cục**: `order.confirmed` và `payment.succeeded` của cùng một đơn có thể tới khác thứ tự ở 2
  queue khác nhau ⇒ consumer phải idempotent **và** chịu được thứ tự bất kỳ (lý do `SL-3.5` dùng compare-and-set).

**Ảnh hưởng file khác:**

- `architecture.md` §9.4: bỏ câu "schema registry từ Phase 3" → thay bằng "version trong envelope + link ADR-0007".
- `architecture.md` §11.2: link ADR-0007.
- `Backlog.md` §7: `TD-17` trả ở `SL-3.5`; debt mới (retention `outbox`/`processed_events`) ghi khi làm ticket đó.

## Revisit

- **Đổi relay sang CDC (`LISTEN/NOTIFY` hoặc Debezium)** khi: độ trễ event p95 > **5 s** · hoặc relay chiếm > **30% CPU**
  của service có outbox · hoặc throughput > **500 msg/s** (đo ở `SL-3.8`).
- **Thêm schema registry** khi: có consumer **ngoài** hệ (bên thứ ba) · hoặc số loại event > **20** · hoặc có **≥ 2 lần**
  "consumer chết vì payload đổi" trong một phase.
- **Đổi sang CloudEvents** khi phải tích hợp event với hệ ngoài (đối tác/analytics) — map 7 field sang attribute chuẩn,
  **không** phá routing key.
- **Exactly-once / Kafka** khi Phase 8 cần event streaming/CDC → analytics (kèm ADR riêng cho việc thêm Kafka).
- **Tự động hoá replay DLQ** khi số message vào DLQ > **50/tuần** **và** đã xác định được nguyên nhân lặp lại — lúc đó tự
  động hoá mới có nghĩa.

