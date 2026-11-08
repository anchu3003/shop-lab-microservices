# ADR-0006 — Mô hình checkout Phase 2 (reservation · idempotency · snapshot)

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-07 |
| **Liên quan** | `Backlog.md` → `SL-2.0` (chặn `SL-2.1` … `SL-2.11`); ADR-0001 (tech stack); `architecture.md` §3 (NFR), §6.1 (checkout), §8.1–§8.4 (data); `TD-12` |

---

## Context

Phase 2 dựng 5 service mới (`cart`, `inventory`, `promotion`, `order`, `payment`) và luồng tiền thật đầu tiên.
Khác Phase 1 (CRUD, sai thì sửa), ở đây **sai là hỏng dữ liệu**: bán quá tồn kho (oversell) hoặc trừ tiền 2 lần
(double charge) — không rollback được bằng cách sửa code.

Ràng buộc đang có:

- **NFR** (`architecture.md` §3): p99 write (checkout) **< 500 ms**; `order` & `payment` cần **strong** consistency;
  chịu được spike flash sale (bài toán thật ở Phase 4, nhưng **chỗ cắm** phải đúng ngay từ Phase 2).
- **Database per service**, **không FK xuyên service**, không join cross-service (§8.1) ⇒ `order_items` **không thể**
  trỏ FK sang `skus` của `catalog-service`.
- **Phase 2 chưa có message broker** (§3.4, `architecture.md` §12): outbox/event để **Phase 3**. Vậy checkout
  Phase 2 là **synchronous** — kéo theo coupling thời gian và bắt buộc **compensating action** cho mọi nhánh fail.
- Hạ tầng local: Postgres 16 + Redis 7 đã có (Phase 0). Máy dev 15 GiB RAM ⇒ mỗi thành phần thêm vào phải xứng.
- Người làm: 1 dev + AI, đi cuốn chiếu ⇒ quyết định phải **viết ra trước khi code**, nếu không `SL-2.2` → `SL-2.9`
  sẽ phải sửa lại từ gốc.

`architecture.md` §6.1 đã phác luồng `reserve → commit` nhưng **chưa chốt cơ chế**. ADR này chốt 5 điểm, mỗi điểm
đều là ngã ba mà đổi sau sẽ tốn ≥ 1 ticket:

| # | Câu hỏi | Đổi sau thì tốn gì |
|---|---|---|
| **D1** | Tính nguyên tử chống oversell đặt ở đâu? | Migration + sửa API `reserve/commit/release` + viết lại test đồng thời (`SL-2.3`) |
| **D2** | `Idempotency-Key` lưu ở đâu, ngữ nghĩa khi trùng key? | Client thấy behaviour khác nhau ⇒ phải sửa cả `order` + `payment` |
| **D3** | `order_items` snapshot hay FK? | Nếu thiếu snapshot: hoá đơn **đổi số tiền hồi tố** — không sửa được về mặt nghiệp vụ |

## Options

### 1. (D1) Nơi đặt tính nguyên tử của `reserve`

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Conditional `UPDATE` 1 câu trên Postgres** — `UPDATE stock SET reserved = reserved + :qty, version = version + 1 WHERE sku_id = :sku AND on_hand - reserved >= :qty`; `rowsAffected = 0` nghĩa là hết hàng | Nguyên tử **thật** ở row lock của DB; **1 round-trip**; không cần Redis, không cần lock ứng dụng; đúng cả khi 100 request đồng thời; chống oversell ngay tại nơi dữ liệu sống | 1 row per SKU bị serialise (đúng ý, nhưng write của SKU hot bị giới hạn bởi 1 core DB); nhiều SKU trong 1 order ⇒ phải **sắp thứ tự `sku_id`** để tránh deadlock | **Chọn** — Phase 2, chưa có số đo và chưa cần > 1k reserve/s trên 1 SKU |
| **B. Redis Lua script (`DECRBY` + kiểm tra)** | Rất nhanh (10k+ op/s), giảm tải DB, sẵn cho flash sale | Redis **không phải source of truth**: restart/evict ⇒ drift với Postgres ⇒ phải có job reconcile; thêm 1 nơi có thể sai; Redis chết ⇒ reserve chết theo | Phase 4 khi **đo được** nghẽn (xem Revisit) |
| **C. `SELECT … FOR UPDATE` rồi kiểm tra trong app** | Đọc được dữ liệu trước khi quyết định, dễ viết business rule | 2 round-trip (SELECT + UPDATE); **lock giữ lâu hơn** (qua tầng app); nhiều SKU ⇒ deadlock nếu không sắp thứ tự; dễ quên commit transaction | Khi rule cần đọc nhiều bảng để quyết định (Phase 2 chưa cần) |
| **D. Distributed lock (Redis / Postgres advisory lock)** | Quen thuộc, dễ hình dung "khoá SKU" | Khoá ở ngoài DB ⇒ **vẫn phải** kiểm tra điều kiện trong DB ⇒ 2 lớp bảo vệ mà vẫn còn khe hở nếu lock hết hạn giữa chừng; thêm failure mode (lock timeout, process chết không nhả) | ❌ Không dùng — conditional UPDATE đã nguyên tử, lock chỉ thêm rủi ro |

> Điểm mấu chốt: **"kiểm tra rồi mới ghi"** ở 2 bước luôn có khe hở. Điều kiện hết hàng phải nằm **trong** câu
> `UPDATE` để chính DB quyết định bên trong cùng một lock.

### 2. (D2) Nơi lưu `Idempotency-Key` + ngữ nghĩa khi trùng key

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Bảng `idempotency_keys` + unique `(scope, key)`, lưu luôn response** | Dedupe **bền** (sống qua restart, không bị evict); replay **đúng response cũ** (client retry nhận 201 + `orderId` như lần đầu); có `request_hash` để phát hiện client **dùng lại key cho payload khác** | Thêm 1 bảng + 1 round-trip; cần job dọn row cũ (debt) | **Chọn** — vì thao tác cần bảo vệ (tạo order) ghi vào **chính Postgres** |
| **B. Redis `SETNX` + TTL** | Nhanh, TTL tự dọn, không đụng DB | Restart/evict mất dedupe **đúng lúc** cần; không replay được body ⇒ client retry nhận 409 dù đơn đã tạo; guard "sống" ở nơi dễ mất hơn dữ liệu được bảo vệ | Khi Redis là source of truth, hoặc làm **fast-path** trước bảng DB (Phase 4) |
| **C. Client tự sinh `orderId` (UUID) làm PK, insert-once** | Không cần bảng riêng, dedupe tự nhiên theo unique PK | Không cache response ⇒ retry nhận **lỗi** thay vì **cùng kết quả**; client phải sinh id ⇒ API khó dùng cho web; không áp dụng được cho webhook provider | API nội bộ do service khác gọi, đã có idempotency ở tầng trên |
| **D. Không dedupe, chỉ chống double charge ở `payment`** | Đơn giản nhất | User bấm 2 lần ⇒ **2 order**, 2 reservation, 2 payment intent; "chống double charge" đúng ở payment nhưng order đã nhân đôi | ❌ Không dùng |

### 3. (D3) `order_items`: snapshot hay FK

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Snapshot tại thời điểm đặt** — `order_items(sku_id, sku_code, title, unit_price, currency, qty, line_total)`; `sku_id` là **tham chiếu opaque**, không FK | Hoá đơn **bất biến** (đổi giá/xoá SKU không làm sai đơn cũ); không gọi `catalog` khi hiển thị đơn; **đúng với DB-per-service** (không FK xuyên service) | Dữ liệu trùng lặp; `catalog` sửa lỗi tên sản phẩm thì đơn cũ vẫn giữ tên cũ (chấp nhận — đó chính là điểm của snapshot) | **Chọn** |
| **B. FK nội bộ tới bảng read-model `skus` trong DB `order`** | Join được, "chuẩn hoá" | Phải **replicate** `skus` từ `catalog` — Phase 2 **chưa có broker** ⇒ phải sync lúc checkout (thêm phụ thuộc + 1 bảng không ai reconcile); vẫn không giải quyết giá **lịch sử** | Phase 3+ nếu thật sự cần join (read model từ event) |
| **C. Không lưu gì, đọc live từ `catalog` khi hiển thị** | Không trùng dữ liệu | Đơn cũ **đổi theo** giá hiện tại ⇒ sai hoá đơn; N+1 cross-service call mỗi lần xem đơn; `catalog` chết ⇒ không xem được đơn | ❌ Không dùng cho order |
| **D. FK cross-service (cùng Postgres instance ở local vẫn làm được)** | Ràng buộc toàn vẹn "miễn phí" | Phá **hard boundary** của DB-per-service: migration/xoá dữ liệu service này bị service khác chặn ⇒ distributed monolith | ❌ Cấm theo `architecture.md` §8.1 |

| **D4** | Phase 2 sync hay đợi async? | Quyết định kiến trúc của cả phase (saga vs choreography) |
| **D5** | `TD-12` soft delete product: làm hay bỏ? | `inventory`/`stock_ledger` tham chiếu `sku_id` ⇒ hard delete làm mất khả năng reconcile |

### 4. (D4) Phase 2 sync hay đợi async

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Sync REST `order → inventory/promotion/payment` + timeout + compensating** | Không thêm hạ tầng (§3.4); user thấy kết quả ngay; **compensating action** là bài học chính của phase, dễ quan sát bằng log/test; Phase 3 có **baseline** để so sánh trước/sau khi chuyển async | Coupling thời gian: 1 dependency chậm ⇒ checkout chậm; cascade failure nếu thiếu timeout (circuit breaker để Phase 5) | **Chọn** cho Phase 2 |
| **B. Dựng broker + outbox ngay ở Phase 2** | Chịu được mất kết nối, không giữ request lâu | Đi trước roadmap: Phase 3 tồn tại **để dạy** async/outbox/DLQ và đo trade-off; làm sớm thì mất bài học "sync vỡ ở đâu"; checkout thành **eventual** — user không nhận câu trả lời dứt điểm trong 1 request, phức tạp hơn hẳn | Phase 3 |
| **C. Sync nhưng bỏ compensating (chỉ log lỗi)** | Ít code | Để reservation/voucher treo ⇒ **cháy hàng ảo** — đúng thứ phase này phải chứng minh là không được phép | ❌ Không dùng |

### 5. (D5 / `TD-12`) Soft delete product

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Soft delete ở `products`** — thêm `status = ARCHIVED` + `deleted_at`; `DELETE` = set 2 cột đó, list/get ẩn đi, SKU của product chuyển `ARCHIVED` | `sku_id` **vẫn tồn tại** ⇒ `reservations`, `stock_ledger`, `cart_items` còn tham chiếu được ⇒ reconcile tồn kho **vẫn đúng**; hiển thị đơn cũ được; giữ lịch sử để audit | Bảng phình dần; mọi query list phải thêm điều kiện lọc (dễ quên ⇒ phải có test); phải quyết định "SKU archived có cho reserve không" | **Chọn** — code ở ticket riêng `SL-2.11` |
| **B. Giữ hard delete, tin vào snapshot của `order_items`** | Không thêm cột, không thêm ticket | Đơn cũ **hiển thị** được (nhờ snapshot) nhưng `stock`/`stock_ledger` mất nghĩa theo SKU (không biết dòng ledger trỏ vào sản phẩm nào); guard "SKU không tồn tại" ở reserve thành vô nghĩa; không audit được sản phẩm từng bán | Chỉ khi dữ liệu catalog là throw-away (không phải case này) |

## Decision

### D1 — Reserve/commit/release bằng **conditional `UPDATE`** trên Postgres (chọn A)

Bảng `stock` (DB `inventory`): 1 row = (`sku_id`, `warehouse_id`); Phase 2 mặc định `warehouse_id = 'WH-1'`
(schema sẵn cho multi-warehouse, logic chưa cần).

| Thao tác | Câu lệnh (rút gọn) | Bất biến được giữ |
|---|---|---|
| `reserve` | `UPDATE stock SET reserved = reserved + :qty, version = version + 1 WHERE sku_id = :sku AND on_hand - reserved >= :qty` | `0 <= reserved <= on_hand` |
| `commit` | `UPDATE stock SET on_hand = on_hand - :qty, reserved = reserved - :qty WHERE sku_id = :sku AND reserved >= :qty` | hàng thực sự rời kho |
| `release` | `UPDATE stock SET reserved = reserved - :qty WHERE sku_id = :sku AND reserved >= :qty` | trả chỗ đã giữ |

- `available = on_hand - reserved`. Mọi mutation ghi thêm 1 dòng `stock_ledger` (**append-only**;
  `reason` ∈ `RESERVE | COMMIT | RELEASE | EXPIRE | ADJUST`, `ref_id` = `reservationId`) **trong cùng transaction**.
- Reserve nhiều SKU = **1 transaction**, sắp `sku_id` tăng dần (tránh deadlock); 1 dòng fail ⇒ rollback cả cụm ⇒
  **all-or-nothing**, trả `409 INSUFFICIENT_STOCK` + `details { skuId, available }` của SKU thiếu.
- `reservations(id, order_id, status, expires_at, created_at)` = **1 row/ORDER** + bảng con
  `reservation_items(reservation_id, sku_id, qty)`; unique index `(order_id) WHERE status = 'PENDING'` ⇒ retry cùng
  `orderId` trả **cùng** reservation (200) thay vì tạo cái thứ 2 và vẫn giữ được 1 `reservationId` cho API.
  *(Cập nhật ở `SL-2.2`: bản đầu của mục này ghi `reservations(… sku_id, qty …)` — không tương thích với unique theo
  `order_id` khi 1 order có nhiều SKU; đã đổi sang header + items khi implement.)*
- **TTL 15 phút** (khớp §6.1). Hết hạn ⇒ `status = EXPIRED` + `release` + ghi ledger. Sweeper `@Scheduled` mỗi 60 s
  quét `status = 'PENDING' AND expires_at < now()`; **thêm** kiểm tra lazy ngay trong `commit`/`reserve`
  (`expires_at < now()` ⇒ `410 RESERVATION_EXPIRED`) để không phụ thuộc đồng hồ sweeper.
  *(Triển khai ở `SL-2.3`: batch 100/nhịp + `FOR UPDATE SKIP LOCKED` + conditional `markStatus` ⇒ chạy N instance
  vẫn không trả chỗ 2 lần — giải quyết `TD-15`.)*
- `promotion-service` dùng **cùng pattern** trên `vouchers.locked`:
  `UPDATE vouchers SET locked = locked + 1 WHERE code = :code AND locked + used < quota AND now() BETWEEN start_at AND end_at`.
  *(Triển khai ở `SL-2.4`: `voucher_locks` 1 row/order + unique `(order_id) WHERE status='LOCKED'`, snapshot `discount`
  và `subtotal` tại lúc lock, TTL 15 phút + sweeper `SKIP LOCKED`; bất biến `used + locked <= quota` do CHECK canh.)*

### D2 — `idempotency_keys` trong Postgres (chọn A), scope = `user_id + endpoint`

Bảng (owner `order-service`; `payment-service` có bảng cùng schema):

```
idempotency_keys(scope, key, request_hash, status, status_code, response_body,
                 resource_id, created_at, completed_at)
  PK (scope, key)          -- scope = "<userId>:POST /v1/checkout"
```

| Tình huống | Hành vi |
|---|---|
| Lần đầu (chưa có key) | `INSERT … ON CONFLICT DO NOTHING` **trong transaction riêng** → `rowsAffected = 1` ⇒ xử lý tiếp |
| Retry **cùng** `request_hash`, row `COMPLETED` | **Replay** `status_code` + `response_body` cũ + header `Idempotent-Replayed: true` |
| Retry cùng key nhưng `request_hash` **khác** | `422 IDEMPOTENCY_KEY_REUSED` (client dùng key cũ cho payload mới) |
| Row còn `IN_PROGRESS` (request trước chưa xong / đã crash) | `409 IDEMPOTENCY_IN_PROGRESS` + `Retry-After: 1` — **không** xử lý song song 2 lần |
| Thiếu header ở `POST /v1/checkout` | `400 MISSING_IDEMPOTENCY_KEY` (bắt buộc, `key` ≤ 64 ký tự) |

- Ghi row `IN_PROGRESS` **trước** khi làm việc ⇒ crash giữa chừng để lại row mồ côi (client retry nhận 409 tới khi
  có job dọn). **Đánh đổi có ý thức**: thà "kẹt 1 key" còn hơn tạo 2 order.
- `payment-service` webhook dùng **khoá tự nhiên của provider** (`provider_txn_id UNIQUE`) chứ không dùng
  `Idempotency-Key` của client — 2 cơ chế khác nhau vì nguồn gây trùng khác nhau.
- Retention **24 h**; dọn row cũ = `TD-14` (đã trả ở `SL-2.5`: job xoá `COMPLETED > 24h` + `IN_PROGRESS > 1h`).
  *(Triển khai ở `SL-2.7`: `payment-service` dùng **đúng** bảng + ngữ nghĩa này, scope `"<userId>:POST
  /v1/payment-intents"`; webhook provider thì **không** dùng `Idempotency-Key` mà dùng khoá tự nhiên
  (`provider_event_id` PK + `provider_txn_id UNIQUE`) + HMAC-SHA256 trên raw body & timestamp window. Phase 2
  chưa có broker ⇒ webhook chỉ ghi `payments`, không phát `payment.succeeded`; việc confirm order do saga
  `SL-2.6` gọi với token user — ghi nhận ở `TD-17`.)*
  *(Triển khai ở `SL-2.5`: ghi khoá **trước khi** mở transaction tạo đơn — nếu lồng `REQUIRES_NEW` trong transaction thì mỗi
  request cần 2 connection và pool nhỏ bị deadlock; `complete()` nằm trong transaction của đơn để crash không tạo "đơn ma".)*

### D3 — Snapshot trong `order_items` (chọn A)

```
orders(id, user_id, status, subtotal, discount, total, currency, idempotency_key,
       payment_intent_id, created_at, updated_at)
  └─ order_items(order_id, sku_id, sku_code, title, unit_price, currency, qty, line_total)  -- snapshot, KHÔNG FK
  └─ order_status_history(order_id, from_status, to_status, reason, created_at)
```

- `sku_id` lưu như **giá trị tham chiếu**, không FK, không join. Hiển thị đơn **không** gọi `catalog`.
- Giá lấy từ `catalog` tại thời điểm checkout (`GET /v1/skus?ids=`), giảm giá từ `promotion` ⇒
  `subtotal/discount/total` tính ở `order-service` và **lưu luôn** (không tính lại khi đọc).

### D4 — Sync cho Phase 2 (chọn A); async để Phase 3

Thứ tự gọi (đúng §6.1, đổi sang **lock voucher trước reserve** để không giữ stock rồi mới phát hiện voucher hết):

```
cart (read) → voucher lock → stock reserve → INSERT order PENDING_PAYMENT + idempotency row

*(Triển khai bước `cart (read)` ở `SL-2.1`: saga đọc `GET /v1/carts` bằng **token user**. Giỏ **guest** không
checkout được — client phải gọi `POST /v1/carts/merge` sau khi login trước; nhờ vậy saga chỉ cần một loại
danh tính. Cart **không** giữ giá (`price_snapshot`) — bước INSERT order lấy giá từ payload rồi tính lại ở
`order-service`, nên giá trong giỏ không bao giờ là nguồn sự thật về tiền.)*
            → payment-intent → 201 {orderId, paymentUrl}
```

Ngân sách latency (p99 mục tiêu là **tổng thực tế**; timeout chỉ là chốt chặn cascade):

| Bước | Timeout (chốt chặn) | p99 mục tiêu |
|---|---|---|
| đọc cart (Redis) | 100 ms | ~5 ms |
| voucher lock | 300 ms | ~20 ms |
| stock reserve | 500 ms | ~30 ms |
| payment-intent (mock) | 800 ms | ~50 ms |
| DB ghi order | 500 ms | ~20 ms |
| **Tổng** | worst case ~2,2 s (chỉ khi mọi bước cùng chậm) | **~125 ms** ✅ dưới NFR 500 ms |

Compensating (mỗi nhánh fail đều có test ở `SL-2.9`): reserve fail ⇒ release voucher; payment-intent fail ⇒ release
stock + voucher + order `CANCELLED`; quá 15 phút chưa thanh toán ⇒ sweeper `EXPIRED` + release cả hai.

### D5 — Soft delete product (`TD-12`) ⇒ tách ticket `SL-2.11`

Chọn A: `products.deleted_at timestamptz NULL` + `status` mở rộng (`ACTIVE | ARCHIVED`). `DELETE /v1/products/{id}`
= set `deleted_at` + `ARCHIVED`, SKU con chuyển `ARCHIVED` (**không** xoá row). List/get ẩn product đã xoá
(`WHERE deleted_at IS NULL`); `GET /v1/skus/{id}` vẫn trả SKU archived (tra cứu nội bộ, hiển thị đơn cũ) nhưng
`reserve` từ chối SKU archived (`409 SKU_UNAVAILABLE`).

- Đây là **thay đổi code** (`catalog-service`) ⇒ theo `AGENTS.md` §3.1 **không gộp** vào ticket ADR: sinh ticket
  **`SL-2.11`** (S), làm **trước** `SL-2.2` (inventory cần biết "SKU archived" nghĩa là gì).
- `TD-12`: mức độ `cao` → `trung bình`, điều kiện revisit → "code xong ở `SL-2.11`".

**Lý do loại các phương án khác**:

- **Redis Lua (D1-B)**: chưa có số đo nào cho thấy Postgres không theo kịp; thêm nguồn sự thật thứ hai là món nợ
  lớn hơn lợi ích ở mức tải Phase 2 (flash sale §3 cần ~10k reserve/phút ≈ 170/s — 1 row Postgres thừa sức).
- **Distributed lock (D1-D)**: conditional UPDATE đã nguyên tử; lock ngoài DB chỉ thêm failure mode.
- **Client tự sinh id (D2-C)**: mất khả năng replay response — thứ client cần nhất khi retry.
- **Read-model `skus` trong DB order (D3-B)**: Phase 2 không có broker ⇒ dữ liệu trôi mà vẫn không có giá lịch sử.
- **Broker sớm (D4-B)**: phá thứ tự học của roadmap, làm `SL-2.6` (saga orchestration) khó debug hơn nhiều.
- **Hard delete (D5-B)**: cắt đứt khả năng reconcile `stock_ledger` với sản phẩm — đúng loại dữ liệu phải giữ 7 năm.

## Consequences

**Được:**

- **Chống oversell đúng ở tầng dữ liệu**: 100 request đồng thời cùng 1 SKU ⇒ DB tự serialise trên 1 row, không
  cần lock ứng dụng ⇒ `SL-2.3` chỉ cần chứng minh bằng test, không phải "tin vào code".
- **Bất biến kiểm tra được bằng SQL** (dùng luôn trong verify của `SL-2.3`):
  ```sql
  SELECT count(*) FROM stock WHERE reserved < 0 OR reserved > on_hand;      -- phải = 0
  SELECT count(*) FROM reservations WHERE status = 'PENDING' AND expires_at < now();  -- phải ~ 0
  ```
- **Retry an toàn**: cùng `Idempotency-Key` ⇒ cùng `201 {orderId}` (không phải 409 khó hiểu), nên user bấm 2 lần
  hay client retry sau timeout mạng đều không sinh order thứ 2.
- **Hoá đơn không đổi hồi tố**: snapshot trong `order_items` ⇒ đổi giá ở `catalog` không làm sai đơn cũ.
- **Không thêm hạ tầng**: đúng §3.4 — Phase 2 chỉ dùng Postgres + Redis đã có; broker để Phase 3.

**Mất / chấp nhận:**

- **Coupling thời gian**: checkout sống chết theo `inventory`/`promotion`/`payment`. Phase 2 chỉ có timeout
  (fail-fast), **chưa có** circuit breaker/retry ⇒ 1 dependency chết làm checkout fail toàn bộ. Đúng chủ đề Phase 5
  sẽ xử lý, và đó là **baseline** để đo.
- **Write của 1 SKU bị giới hạn bởi 1 row**: SKU hot (flash sale) sẽ hàng đợi trên row đó. Chấp nhận ở Phase 2,
  có điều kiện chuyển (Revisit).
- **Bảng `idempotency_keys` phình** (1 row/request checkout) — chưa có job dọn ⇒ `TD-14`.
- **Sweeper `@Scheduled` chạy trong process**: 2 instance inventory sẽ quét trùng (vô hại nhờ `UPDATE … WHERE
  status = 'PENDING'`, nhưng tốn query) ⇒ `TD-15`.
- **Row `IN_PROGRESS` mồ côi** khi process chết giữa chừng: client retry nhận `409` cho tới khi có job dọn —
  chấp nhận vì an toàn hơn tạo 2 order.
- **`stock.reserved` có thể drift** nếu `commit`/`release` fail giữa chừng ⇒ phải có query đối soát
  `reserved = Σ PENDING reservations` (thêm vào verify `SL-2.3`).

**Debt mới (ghi vào `Backlog.md` §7):**

| ID | Mô tả | Điều kiện revisit | Mức độ |
|---|---|---|---|
| `TD-14` | `idempotency_keys` không có job dọn row quá 24 h ⇒ phình dần | Khi có scheduler chạy thật (sweeper reservation `SL-2.3`) ⇒ gộp 1 job | trung bình |
| `TD-15` | Reservation sweeper là `@Scheduled` in-process, chạy trùng khi > 1 instance | Khi `inventory-service` chạy ≥ 2 instance | thấp |
| `TD-16` | Chưa có API **restore/unarchive** product đã xoá mềm; bán lại cùng product phải tạo mới | Khi có nhu cầu khôi phục (seller ẩn nhầm) hoặc có UI quản trị | thấp |

**Ảnh hưởng tới file khác:**

- `architecture.md` §6.1: thêm ghi chú cơ chế (conditional UPDATE, TTL, thứ tự lock voucher trước reserve).
- `architecture.md` §8.4: DDL sketch đổi theo (`stock`, `reservations`, `order_items` snapshot, `idempotency_keys`,
  `products.deleted_at`).
- `SL-2.2` (schema/API inventory), `SL-2.5` (order + idempotency), `SL-2.7` (payment webhook) phải bám đúng bảng dưới đây.
- `SL-2.11` (mới): soft delete product ở `catalog-service`.

## Bổ sung D6 — đơn 0 đồng (chốt 2026-11-08 khi làm `SL-2.6a`)

**Vấn đề**: voucher giảm 100% ⇒ `total = 0`, nhưng `payment-service` yêu cầu `amount ≥ 0.01`
(`CreatePaymentIntentRequest`). Phải chọn: chặn đơn 0 đồng, hay cho phép đơn 0 đồng không qua thanh toán.

**Chốt (phương án B)**: **cho phép đơn 0 đồng**, cố ý **không** tạo payment intent.

| Điểm | Chốt | Hệ quả |
|---|---|---|
| Cờ trong saga | `checkout_sagas.payment_required` (bất biến của saga, **không** suy ra từ `orders.total`) | `SL-2.6b` đọc cờ này để chốt đơn mà không verify payment |
| Response | `CheckoutResponse.paymentRequired = false`, `paymentIntentId = null` | Client biết "không cần redirect đi thanh toán" |
| Audit | bước `PAYMENT_INTENT` = `SKIPPED` (không phải `OK`) | Nhìn bảng step là biết đơn 0 đồng, không phải payment bị bỏ quên |
| Ràng buộc DB | `CHECK (state <> 'COMPLETED_START' OR payment_intent_id IS NOT NULL OR NOT payment_required)` | DB không cho tồn tại saga "xong bước start cần thanh toán nhưng không có intent" |

**Vì sao không chặn đơn 0 đồng**: chặn thì một voucher 100% (hoàn tiền, đền bù, khuyến mãi) trở thành **không dùng
được** — tức là cấm một nghiệp vụ hợp lệ bằng một ràng buộc kỹ thuật của payment-service. Cờ `payment_required` rẻ hơn
nhiều và giữ nguyên ý nghĩa cho `SL-2.6b`.

**Mất / chấp nhận**: có thêm một nhánh trạng thái (`payment_required = false`) phải xử lý ở confirm/cancel/sweeper —
`SL-2.6b` đã ghi vào AC.

---

## Revisit

| Điều kiện (định lượng được) | Chuyển sang | Vì sao |
|---|---|---|
| p99 `POST /stock/reserve` **> 50 ms** hoặc 1 SKU nhận **> 1.000 reserve/s** (đo ở Phase 4 load test) | Redis Lua/atomic counter (D1-B) + job reconcile `stock` | Lúc đó row lock Postgres mới thật sự là điểm nghẽn |
| `stock` có **> 10⁶ row** hoặc cần multi-warehouse thật | Partition theo `sku_id` hash / tách `stock_per_warehouse` | Row count lớn làm index & sweep chậm |
| Bảng `idempotency_keys` **> 10⁶ row** hoặc checkout **> 200 req/s** | Fast-path Redis `SETNX` (TTL 24 h) **trước** bảng DB | Giảm 1 round-trip/request khi volume cao |
| `SL-2.3` đo được `commit` fail **> 0,1 %** do reservation hết hạn giữa chừng | Tăng TTL 15′ → 20′ | TTL quá ngắn ⇒ user bị `410` dù đang thanh toán |
| Phase 3 có broker | Chuyển `order.created` / `payment.succeeded` sang **event + outbox**, bỏ sync cho phần không cần kết quả ngay | Giảm coupling thời gian (D4) — đo lại p99 trước/sau |
| Cần audit hoàn tiền từng dòng (refund nhiều bước) | Thêm bảng `order_item_adjustments` (không sửa `order_items` snapshot) | Snapshot là bất biến; điều chỉnh phải là dòng mới |
| `inventory-service` chạy **≥ 2 instance** | Sweeper dùng lock (advisory lock / `SELECT … FOR UPDATE SKIP LOCKED`) | Tránh N instance quét cùng lúc (`TD-15`) |

