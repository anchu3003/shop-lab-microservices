# Backlog Phase 2 — Core commerce flow (archive)

> **Archive** chi tiết của Phase 2 (đã đóng). `Backlog.md` chỉ giữ trạng thái + link tới đây.
> Snapshot code/doc tại thời điểm đóng phase: tag `phase-2` · [`docs/phases/phase-2.md`](docs/phases/phase-2.md) ·
> runbook [`docs/runbook-phase-2.md`](docs/runbook-phase-2.md). Quyết định archive: [ADR-0005](docs/adr/0005-phase-snapshot-and-release.md).

| | |
|---|---|
| **Phase** | 2 — Core commerce flow |
| **Ticket** | `SL-2.0` … `SL-2.13` — **15/15 Done** (10 kế hoạch + 5 phát sinh: `SL-2.6a`/`2.6b` tách từ `SL-2.6`, `SL-2.11`, `SL-2.12`, `SL-2.13`) |
| **Thời điểm đóng** | 2026-11-08 |
| **Stack** | Java 21 · Spring Boot 4.1.2 · PostgreSQL 16 · Redis 7 · RabbitMQ 4 (chưa dùng) · Docker Compose · GitHub Actions |
| **Service** | `api-gateway` (8080) · `identity` (8081) · `catalog` (8082) · `cart` (8083) · `inventory` (8084) · `promotion` (8086) · `order` (8087) · `payment` (8088) |
| **Kết quả** | checkout end-to-end: giỏ → saga (reserve/lock/payment intent) → confirm ⇒ `CONFIRMED`; nhánh fail có compensating; **100 request đồng thời không oversell**; OpenAPI 41 path / 50 operation |

**Mục tiêu phase**: luồng mua hàng hoàn chỉnh, **đúng dữ liệu kể cả khi fail** — trọng tâm *Saga + compensating
+ idempotency + chống oversell*, không phải UI.

**Điều kiện hoàn thành** (đã đạt): checkout end-to-end qua gateway · nhánh fail + compensating có test · 100 request
đồng thời cùng 1 SKU không oversell · OpenAPI + runbook cập nhật · tag `phase-2`.

## Danh sách ticket

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-2.0` | **ADR-0006** chốt mô hình checkout (reservation TTL · idempotency key · snapshot vs FK · sync Phase 2 / async Phase 3 · quyết định `TD-12` soft delete) | S | ✅ Done |
| 2 | `SL-2.1` | `cart-service` (8083, Redis): add/update/remove/get, TTL, **merge cart khi login** | M | ✅ Done |
| 3 | `SL-2.2` | `inventory-service` (8084, DB `inventory`): schema `stock`/`reservations`/`stock_ledger` + API `reserve`/`commit`/`release`; **atomic** chống oversell | M | ✅ Done |
| 4 | `SL-2.3` | inventory: **test đồng thời** (100 request cùng SKU) + reservation **TTL** & giải phóng hết hạn | M | ✅ Done |
| 5 | `SL-2.4` | `promotion-service` (8086, DB `promotion`): voucher + rule giảm giá + `lock`/`commit`/`release` voucher | M | ✅ Done |
| 6 | `SL-2.5` | `order-service` (8087, DB `order`): tạo order **idempotent** (idempotency key) + state machine + `order_items` **snapshot** giá | M | ✅ Done |
| 7 | `SL-2.6a` | **Checkout Saga — start** (`order-service`): `POST /v1/checkout` đọc **cart** → lấy giá bằng **batch SKU** của catalog → lock voucher → reserve stock → tạo order → payment intent; lưu **saga state**; compensating cho nhánh fail lúc start | M | ✅ Done |
| 8 | `SL-2.6b` | **Checkout Saga — hoàn tất**: `confirm` (commit stock + voucher ⇒ `CONFIRMED`), `cancel` + **sweeper hết hạn 15′** (⇒ `EXPIRED`), idempotent theo saga state | M | ✅ Done |
| 9 | `SL-2.7` | `payment-service` (8088, DB `payment`): payment intent + **mock provider** + webhook **idempotent** (`idempotency_keys`) | M | ✅ Done |
| 10 | `SL-2.8` | E2E checkout happy path (`tools/e2e-checkout.sh` — assert cả **tiền** lẫn trạng thái) + workflow `e2e.yml` (compose + 2 script, chạy dispatch/hằng tuần) + verify OpenAPI/matrix | M | ✅ Done |
| 11 | `SL-2.9` | **E2E nhánh fail**: hết hàng · provider từ chối · payment 5xx · **timeout thật** · dependency chết · retry sau fail — assert `reserved`/`locked`/`order.status`; thêm hook bơm lỗi (guarded) ở payment | M | ✅ Done |
| 12 | `SL-2.10` | `docs/runbook-phase-2.md` + DoD Phase 2 (archive + tag `phase-2` + release) | S | ✅ Done |
| 13 | `SL-2.11` | *(phát sinh từ `SL-2.0`)* `catalog-service`: **soft delete** product — `status = ARCHIVED` + `deleted_at`, `DELETE` không xoá row, list/get ẩn product đã xoá, `reserve` từ chối SKU archived | S | ✅ Done |
| 14 | `SL-2.12` | *(phát sinh từ `SL-2.2`)* `service-commons`: chuyển `AccessDeniedApiExceptionHandler` (403 cho `@PreAuthorize`) vào commons — xoá **6 bản copy** ở catalog/inventory/cart/order/payment/promotion (**trả `TD-3`**) | S | ✅ Done |
| 15 | `SL-2.13` | *(phát sinh từ `SL-2.6`, chốt phương án **B**)* `catalog-service`: `GET /v1/skus?ids=` — **batch** giá/tên SKU cho saga + cart (**điều kiện tiên quyết của `SL-2.6a`**) | S | ✅ Done |

**Phụ thuộc & thứ tự** (cập nhật sau `SL-2.1`): `SL-2.0` ✅ chốt mô hình → **`SL-2.11`** (soft delete — `catalog` phải
xong trước để inventory biết "SKU archived" nghĩa là gì) → `SL-2.2` + `SL-2.3` (inventory — chỗ dễ oversell nhất,
làm sớm) song song được với `SL-2.1`/`SL-2.4` → `SL-2.7` (payment) → `SL-2.5` (order) → **`SL-2.13`** (batch giá SKU —
điều kiện của saga) → `SL-2.6a` → `SL-2.6b` → `SL-2.8`/`SL-2.9` → `SL-2.10`.

**Debt đã trả trong phase này**: `TD-12` (soft delete product — **✅ đã trả ở `SL-2.11`**), `TD-4` (đưa
smoke vào CI khi đã có compose nhiều service — **✅ đã trả ở `SL-2.8`**), `TD-3` (handler 403 → `commons` khi có service
thứ hai dùng `@PreAuthorize` — **✅ đã trả ở `SL-2.12`**).
**Debt mới phát sinh ở `SL-2.0`**: `TD-14` (chưa có job dọn `idempotency_keys`), `TD-15` (sweeper reservation chạy
trùng khi > 1 instance), `TD-16` (chưa có API restore product đã xoá mềm).

> Mỗi ticket vẫn phải viết **đặc tả BA đầy đủ** (In/Out · AC verify được · ghi chú kỹ thuật) **ngay trước khi bắt
> đầu** — bảng trên là phạm vi + thứ tự, không thay thế đặc tả (`AGENTS.md` §4, §5).

---

## Chi tiết ticket (verify + learning note)

### SL-2.0 — ADR-0006: chốt mô hình checkout Phase 2

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | Phase 1 đã đóng (tag `phase-1`); **chặn** `SL-2.1` … `SL-2.11` |
| **Ước lượng** | S |

**Bối cảnh**

Phase 2 dựng 5 service mới và luồng tiền đầu tiên. Khác Phase 1 (CRUD — sai thì sửa), ở đây **sai là hỏng dữ liệu**:
bán quá tồn kho (oversell), trừ tiền 2 lần (double charge), hoặc hoá đơn **đổi số tiền hồi tố**. `architecture.md`
§6.1 mới phác luồng `reserve → commit` mà **chưa chốt cơ chế**; `TD-12` (soft delete product) đang treo mức `cao` và
chặn `SL-2.x`. Nếu 5 điểm dưới đây chốt sai thì `SL-2.2` … `SL-2.9` phải sửa lại từ gốc (migration + API + test):
D1 chống oversell ở đâu · D2 `Idempotency-Key` lưu ở đâu · D3 `order_items` snapshot hay FK · D4 Phase 2 sync hay
async · D5 soft delete product.

**Mục tiêu**

Có **1 tài liệu quyết định** để mọi ticket Phase 2 bám theo, và `architecture.md` **không lệch** với nó. Mỗi quyết
định phải nêu được: chọn gì · vì sao · loại phương án nào và **tại sao loại** · khi nào xem lại **bằng con số**.

**In scope**

- `docs/adr/0006-checkout-model.md` — 5 mục (Context · Options · Decision · Consequences · Revisit), phủ **D1–D5**.
- Cập nhật `architecture.md`: §6.1 (bảng cơ chế đã chốt) · §8.4 (DDL sketch khớp ADR) · header + Changelog.
- Chốt `TD-12` (→ soft delete product, **code ở ticket riêng** `SL-2.11` vì là thay đổi code — `AGENTS.md` §3.1).
- Sinh ticket `SL-2.11` trong bảng Phase 2 + ghi debt mới phát sinh (`TD-14` … `TD-16`).

**Out of scope**

- **Không** viết code service nào (thuộc `SL-2.1` … `SL-2.11`).
- **Không** dựng broker/outbox (Phase 3) · **không** circuit breaker/retry nâng cao (Phase 5).
- **Không** thiết kế refund/multi-warehouse thật (chỉ chừa chỗ trong schema).

**Acceptance Criteria**

- [x] ADR-0006 có đủ **5 mục** đúng thứ tự template, `Trạng thái = Accepted`.
- [x] Có bảng **Ưu / Nhược / Khi nào dùng** cho **cả 5** quyết định, và nêu **lý do loại** phương án không chọn.
- [x] Mục **Revisit** có điều kiện **định lượng** (p99 ms, RPS, số row, % fail), không viết "khi cần".
- [x] `architecture.md` §6.1 có bảng cơ chế đã chốt + link tới ADR-0006; §8.4 DDL khớp tên bảng trong ADR
      (`stock`, `reservations`, `stock_ledger`, `idempotency_keys`, `order_items` snapshot, `products.deleted_at`).
- [x] `TD-12` có quyết định + điều kiện revisit mới; ticket code `SL-2.11` có trong bảng Phase 2.
- [x] PR **docs-only** — không file code nào bị đổi (kiểm bằng `git diff --cached --name-only | grep -v '\.md$'`).

**Ghi chú kỹ thuật** (tóm tắt — chi tiết + bảng trade-off ở ADR-0006)

| # | Chốt | Lý do chính (1 câu) | Loại vì |
|---|---|---|---|
| D1 | `UPDATE stock SET reserved = reserved + :qty WHERE sku_id = :sku AND on_hand - reserved >= :qty`; `0` row ⇒ `409` | Điều kiện hết hàng nằm **trong** câu UPDATE ⇒ DB quyết định trong cùng row lock | Redis Lua (chưa có số đo, thêm nguồn sự thật thứ 2) · `SELECT FOR UPDATE` (giữ lock lâu, deadlock) · distributed lock (chỉ thêm failure mode) |
| D2 | `idempotency_keys` PK `(scope, key)`, `scope = userId:endpoint`, có `request_hash` + lưu response | Guard phải **bền hơn** dữ liệu nó bảo vệ; retry phải replay **cùng** kết quả | Redis SETNX (mất dedupe khi restart) · client sinh `orderId` (không replay được body) · không dedupe (bấm 2 lần = 2 order) |
| D3 | `order_items` **snapshot** `sku_code/title/unit_price/currency`, `sku_id` không FK | Hoá đơn phải **bất biến**; DB-per-service đã cấm FK xuyên service | read-model `skus` (Phase 2 không có broker để sync) · đọc live `catalog` (đơn cũ đổi giá) · FK cross-service (distributed monolith) |
| D4 | Sync REST + timeout từng bước, compensating cho mọi nhánh; async để Phase 3 | Không thêm hạ tầng (§3.4); compensating là bài học chính của phase | broker sớm (đi trước roadmap, mất baseline) · sync không compensating (cháy hàng ảo) |
| D5 | `products.status = ARCHIVED` + `deleted_at`; code ở `SL-2.11` | `stock_ledger`/`reservations` cần `sku_id` còn sống để reconcile tồn kho | hard delete (mất khả năng reconcile + audit) |

Con số đã chốt kèm theo: reservation **TTL 15′** (sweeper 60 s + lazy expire ở `commit`) · idempotency retention **24 h**
· timeout mỗi bước 300–800 ms · p99 checkout mục tiêu **~125 ms** (NFR 500 ms) · Phase 2 mặc định `warehouse_id='WH-1'`.

**Kết quả verify** *(docs-only — chạy thật, dán output thật)*

```text
$ git diff --cached --stat
 Backlog.md                      | 172 ++++++++++++++++++++++++++++++++------
 architecture.md                 |  35 ++++++--
 docs/adr/0006-checkout-model.md | 252 +++++++++++++++++++++++++++++++++++++++++++++++++++++++
 3 files changed, 425 insertions(+), 34 deletions(-)

$ git diff --cached --name-only | grep -v '\.md$'   # phải rỗng
(không có)

$ grep -n '^## ' docs/adr/0006-checkout-model.md    # 5 mục bắt buộc, đúng thứ tự
11:## Context   38:## Options   88:## Decision   194:## Consequences   241:## Revisit

$ grep -c '| Phương án | Ưu | Nhược | Khi nào dùng |' docs/adr/0006-checkout-model.md
5            # đủ 5 bảng trade-off cho D1…D5

$ sed -n '/^## Revisit/,$p' docs/adr/0006-checkout-model.md | grep '^| ' | grep -v '^|---' | wc -l
8            # 1 header + 7 điều kiện, trong đó 5 điều kiện có ngưỡng số (>50 ms · >1.000 reserve/s · >10⁶ row · >200 req/s · >0,1 %)
```

Kiểm **tính toàn vẹn bảng markdown** (script `python3` đếm số cột của từng bảng, so với bản `main`):

```text
docs/adr/0006-checkout-model.md   → 0 cảnh báo
architecture.md                   → 0 cảnh báo
Backlog.md                        → 12 cảnh báo (bản `main` cũng 12 — toàn bộ là dòng learning log/changelog
                                    có ký tự `|` bên trong inline code, có từ trước; ticket này không thêm cái nào)
```

Kiểm **tên bảng khớp giữa ADR ↔ `architecture.md` §8.4** (`stock`, `reservations`, `stock_ledger`, `idempotency_keys`,
`order_items`, `voucher_locks`, `products.deleted_at`): 0 lệch. Link `docs/adr/0006-checkout-model.md` xuất hiện 3 lần
trong `architecture.md`, 1 lần trong `Backlog.md` — file tồn tại (22.617 byte).

CI: PR **docs-only** ⇒ `paths-ignore` bỏ run (không tốn phút Actions); đây là trường hợp hợp lệ theo
`.clinerules/10-git-workflow.md`.

**Learning note**

- **"Kiểm tra rồi mới ghi" có khe hở theo thiết kế** — 2 bước `SELECT` xong mới `UPDATE` luôn có khoảng thời gian
  để request khác lọt vào. Điều kiện phải nằm **trong** `UPDATE` để chính DB quyết định trong cùng lock.
- **Idempotency guard ở nơi dễ mất hơn dữ liệu là vô nghĩa**: đặt key ở Redis trong khi order ở Postgres thì đúng
  lúc retry (sau restart) lại mất dedupe. Đổi lại phải trả giá bằng 1 bảng + job dọn (`TD-14`).
- **Snapshot không phải "làm biếng chuẩn hoá"** mà là quyết định nghiệp vụ: hoá đơn phải bất biến. Câu hỏi đúng là
  "đơn cũ có được phép đổi số tiền không?" chứ không phải "có nên join không".
- **ADR viết bằng số mới có tác dụng**: `Revisit` kiểu "khi cần" = lần sau tranh luận lại từ đầu. Viết "p99 reserve
  > 50 ms hoặc > 1.000 reserve/s trên 1 SKU" thì biết chính xác lúc nào phải mở lại.
- **Viết ADR trước khi code là rẻ nhất**: 1 file ~252 dòng thay cho việc sửa migration + API + test ở 5 service.

### SL-2.11 — `catalog-service`: soft delete product (thay hard delete + cascade SKU)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow *(phát sinh từ `SL-2.0`, ADR-0006 D5)* |
| **Phụ thuộc** | `SL-2.0` (chốt D5); **chặn** `SL-2.2` (inventory cần biết "SKU archived") |
| **Ước lượng** | S |

**Bối cảnh**

`TD-12` (mức `cao`): `DELETE /v1/products/{id}` hiện là **hard delete**, và `sku.product_id` có
`ON DELETE CASCADE` ⇒ xoá product là **xoá luôn mọi SKU**. Phase 2 bắt đầu tham chiếu `sku_id` từ
`inventory.stock` / `stock_ledger` / `order_items` — nếu SKU biến mất thì mất khả năng reconcile tồn
kho và audit "sản phẩm này từng bán chạy thế nào". ADR-0006 (D5) đã chốt: **soft delete ở product**,
`sku_id` phải còn sống.

**Mục tiêu**

`DELETE` product = **archive** (không xoá row): product + SKU của nó chuyển `ARCHIVED`, `deleted_at`
được ghi. API coi như product không còn tồn tại (list/get/ghi đều không thấy), nhưng dữ liệu vẫn ở
DB để Phase 2 tham chiếu.

**In scope**

- Migration `V3__add_soft_delete.sql`: `product.status` + `product.deleted_at`, `sku.status`, CHECK
  `status ∈ {ACTIVE, ARCHIVED}`, CHECK **nhất quán** `(status='ARCHIVED') = (deleted_at IS NOT NULL)`,
  partial index cho list (`WHERE deleted_at IS NULL`).
- Entity: `ProductStatus`/`SkuStatus` enum; `Product.archive(Instant)` (idempotent), `Sku.archive()`.
- Repository: `findByIdAndDeletedAtIsNull` / `existsByIdAndDeletedAtIsNull`; `search` thêm
  `p.deletedAt is null`; bulk `UPDATE sku SET status='ARCHIVED' WHERE product_id = :id` (1 query, không N+1).
- Service: `ProductService.delete` → archive; `SkuService.create` chỉ nhận product **ACTIVE**.
- DTO: `status` xuất hiện trong `ProductResponse`, `ProductDetailResponse`, `SkuResponse`.
- Test: unit (archive không gọi `delete`, archive idempotent, 404 khi đã xoá, SKU vào product đã xoá → 404)
  + IT (DELETE → 204 → GET 404 → list ẩn → **DB: row còn, `status=ARCHIVED`, `deleted_at` not null, SKU archived**).
- Re-export `docs/api/openapi.yaml` (schema có `status` mới).

**Out of scope**

- **Không** thêm `GET /v1/skus/{id}` (endpoint chưa tồn tại; đường đọc SKU theo id để `SL-2.2` làm khi inventory cần).
- **Không** có API restore/unarchive (giữ `TD-16`).
- **Không** đụng `category` (không có soft delete cho category; xoá category vẫn bị DB chặn nếu còn product).
- **Không** xoá cứng SKU hay dọn dữ liệu cũ.

**Acceptance Criteria**

- [x] `DELETE /v1/products/{id}` → **204**, row `product` **vẫn còn** với `status='ARCHIVED'` + `deleted_at` not null.
- [x] SKU của product đó chuyển `status='ARCHIVED'` (còn row, không bị cascade xoá).
- [x] `GET /v1/products/{id}` và `PUT /v1/products/{id}` trên product đã xoá → **404 NOT_FOUND**.
- [x] `GET /v1/products` (list/search) **không** trả product đã xoá (kể cả khi filter `categoryId`/`q` khớp).
- [x] `POST /v1/products/{id}/skus` vào product đã xoá → **404**; `DELETE` lần 2 → **404**.
- [x] Response tạo/đọc product có `"status":"ACTIVE"`; DB chặn được trạng thái không nhất quán (CHECK constraint).
- [x] Unit + IT xanh; `docs/api/openapi.yaml` cập nhật; không hard-code gì mới.

**Ghi chú kỹ thuật**

- Bất biến đặt ở **DB** (CHECK), không chỉ ở app: bug ở tầng service không được phép tạo ra row
  `status='ACTIVE'` mà `deleted_at` có giá trị (hoặc ngược lại).
- `archive()` **idempotent**: gọi lần 2 giữ nguyên `deleted_at` gốc ⇒ audit không bị ghi đè thời điểm xoá.
- Archive SKU bằng **bulk update** thay vì load từng SKU: 1 product có thể có hàng chục SKU, và Phase 2
  sẽ gọi đường này trong transaction checkout-adjacent — N+1 ở đây là không cần thiết.
- `status` là `@Enumerated(STRING)` (không ordinal): đọc DB bằng mắt được, và thêm giá trị mới không
  làm lệch dữ liệu cũ.
- Trade-off đã chấp nhận: mọi query list phải nhớ `deleted_at is null` ⇒ có IT riêng canh chuyện này
  (đúng loại bug im lặng: xoá xong vẫn thấy hàng).

**Kết quả verify** *(chạy thật trên Docker + Postgres 16, không phải suy luận)*

**1) Unit + IT + lint (local, Docker Desktop bật) — `mvn -B -ntp verify`**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.588 s]
[INFO] ShopLab :: Service Template ........................ SUCCESS [  5.657 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 19.869 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 18.428 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  9.696 s]
[INFO] BUILD SUCCESS    Total time: 57.695 s
```

| Module | Unit | IT (Testcontainers, Postgres thật) |
|---|---|---|
| commons | 42 | — |
| service-template | 35 | — |
| identity | 50 | 22 |
| **catalog** | **66** (+6: `ProductTest` 3 · `ProductServiceTest` 7 · `SkuServiceTest` 5) | **21** (+3: `deleteArchivesProductInsteadOfRemoving` · `archivedProductIsInvisibleToWrites` · `databaseRejectsInconsistentSoftDeleteState`) |
| gateway | 71 | — |

`checkstyle:check` + `spotless:check`: 0 vi phạm (đã chạy `spotless:apply` 2 lần trong lúc làm — lần đầu fail vì
comment quá dài 100 cột).

**2) Migration thật trên container (Flyway tự chạy khi service start)**

```text
$ docker exec shoplab-postgres psql -U shoplab -d catalog \
    -c "select installed_rank, version, script, success from flyway_schema_history"
 4 | 3 | V3__add_soft_delete.sql | t

$ \d product
 status      | character varying(16)    | not null | 'ACTIVE'::character varying
 deleted_at  | timestamp with time zone |          |
 "ix_product_active_created_at" btree (created_at) WHERE deleted_at IS NULL
Check constraints:
 ck_product_status                      CHECK (status IN ('ACTIVE','ARCHIVED'))
 ck_product_deleted_at_matches_status   CHECK ((status = 'ARCHIVED') = (deleted_at IS NOT NULL))
```

**3) Luồng soft delete qua gateway trên stack thật** (`bash /tmp/verify-soft-delete.sh`)

```text
== 2) GET product trước khi xoá            status=ACTIVE skus=1
== 3) DELETE product                       DELETE → HTTP 204
== 4) GET product sau khi xoá              GET → HTTP 404
   {"code":"NOT_FOUND","message":"Product không tồn tại: aa4de730-…","traceId":"86e16fd9-…"}
== 5) list theo category                   totalElements=0, chứa product đã xoá: False
== 6) DB: row còn, status ARCHIVED         ARCHIVED|t          ← deleted_at IS NOT NULL
== 7) DB: SKU còn row, đã ARCHIVED         ARCHIVED|1  · số row sku còn lại: 1
== 8) POST SKU vào product đã xoá          POST sku → HTTP 404
== 9) DELETE lần 2                         DELETE#2 → HTTP 404 (body NOT_FOUND)
```

**4) DB là chốt chặn cuối — CHECK constraint từ chối cả 2 chiều sai**

```text
-- product đang ACTIVE, thử set deleted_at:
ERROR:  new row for relation "product" violates check constraint "ck_product_deleted_at_matches_status"
-- thử set status='ARCHIVED' mà không set deleted_at:
ERROR:  new row for relation "product" violates check constraint "ck_product_deleted_at_matches_status"
```

**5) Regression e2e + contract**

```text
$ bash tools/e2e-smoke.sh
== Kết quả: 17 PASS / 0 FAIL · 13 request · 982 ms        ← 17/17 mục của Phase 1 vẫn xanh
$ bash tools/export-openapi.sh
VALIDATE PASS · 14 path · 18 operation · 7 operation cần token · 18 schema
 docs/api/openapi.yaml | 15 +++++++++++++++   (3 schema thêm `status: ACTIVE|ARCHIVED`)
$ md5sum (export lần 1 vs lần 2) → giống nhau ⇒ export vẫn deterministic (bài học SL-1.19)
```

**Sự cố trong lúc verify (ghi lại vì là bài học thật)**

- IT đầu tiên **fail**: `flywayMigratedCatalogSchema` assert `select count(*) from product where status <> 'ACTIVE'`
  = 0, nhưng test khác (`deleteArchivesProductInsteadOfRemoving`) đã archive 1 product trước đó — **thứ tự test
  không được đảm bảo**. Đã bỏ assertion aggregate toàn bảng, chỉ kiểm `DEFAULT 'ACTIVE'` của cột.
- Step kiểm CHECK bằng tay lần đầu chạy trên **row đã ARCHIVED** (`set deleted_at` lần nữa vẫn hợp lệ) ⇒ tưởng
  constraint không chặn. Làm lại trên product ACTIVE mới thấy DB chặn đúng — nhắc lại: **test âm phải đúng
  trạng thái**.

**Learning note**

- **Soft delete là quyết định về *đọc*, không phải về *xoá***: phần khó không phải `UPDATE status` mà là mọi
  đường đọc phải nhớ `deleted_at is null`. Cách chống quên hiệu quả nhất là **đặt tên method theo hành vi**
  (`findByIdAndDeletedAtIsNull`) để chỗ nào bỏ sót là lộ ra ngay khi review.
- **Bất biến nên nằm ở DB, không ở service**: CHECK `(status='ARCHIVED') = (deleted_at IS NOT NULL)` bắt được
  cả 2 chiều sai (đã chứng minh bằng 2 lệnh UPDATE bị từ chối). Service có bug thì dữ liệu vẫn không thành
  "nửa sống nửa chết".
- **Archive con bằng bulk UPDATE, không load entity**: 1 câu SQL cho N SKU, và `flushAutomatically` bảo đảm
  thay đổi của product được ghi trước — ít round-trip, không N+1.
- **Idempotent ở tầng domain ≠ idempotent ở tầng API**: `archive()` giữ nguyên `deleted_at` khi gọi lại (audit
  không bị ghi đè), nhưng API vẫn trả **404** ở lần xoá thứ 2 vì product đã bị `require()` lọc mất. Hai tầng
  có mục đích khác nhau, đừng gộp.
- **Test tích hợp không được assert aggregate toàn bảng** nếu dữ liệu do test khác tạo và thứ tự không đảm bảo
  — chỉ assert trên dữ liệu chính test đó tạo ra (hoặc trên metadata như `information_schema`).
- `DELETE` **không** còn là "xoá" ⇒ phải sửa cả README/OpenAPI, không chỉ code: người dùng API cần biết
  product đã xoá vẫn **còn trong DB** (để hiểu vì sao restore cần ticket riêng — `TD-16`).


### SL-2.2 — `inventory-service`: reserve / commit / release (atomic chống oversell)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.0` (ADR-0006 D1) · `SL-2.11` (`sku_id` còn sống sau khi xoá product); **chặn** `SL-2.3`, `SL-2.6` |
| **Ước lượng** | M |

**Bối cảnh**

Đây là chỗ dễ **oversell** nhất của hệ: nhiều buyer cùng mua 1 SKU trong cùng một giây. `architecture.md` §6.1 phác
`reserve → commit`, ADR-0006 D1 chốt cơ chế: **conditional `UPDATE`** trên Postgres (không Redis lock), reservation có
**TTL 15′**, ledger append-only. Chưa có service này thì `order-service` (SL-2.5/2.6) không có gì để gọi.

**Mục tiêu**

Service thứ tư chạy được end-to-end: giữ chỗ tồn kho **nguyên tử** (không bao giờ bán quá `on_hand`), commit khi tiền
về, release khi huỷ/hết hạn, và **mọi thay đổi đều có dấu vết** trong `stock_ledger`.

**In scope**

- Service mới `services/inventory-service` (port **8084**, DB **`inventory`**): pom + Dockerfile + application.yml +
  security (JWT resource server) + health/readiness + logback.
- Migration `V1__create_inventory.sql`: `stock` · `reservations` · `reservation_items` · `stock_ledger` + index/constraint.
- API: `PUT /v1/stock/{skuId}` (admin, set `on_hand`) · `GET /v1/stock/{skuId}` (auth) ·
  `POST /v1/stock/reserve|commit|release`.
- Atomic reserve nhiều SKU **all-or-nothing** (sort `sku_id`, 1 transaction) · idempotent theo `orderId` ·
  commit idempotent · release idempotent · commit khi hết hạn → **410** + release.
- Hạ tầng: compose + `.env.example` + gateway route `/v1/stock` + CI matrix + Dockerfile của 4 service cũ (mỗi cái
  thêm 1 dòng `COPY pom.xml` — bẫy module mới) + OpenAPI re-export.
- `service-commons`: thêm `GONE(410)` cho `RESERVATION_EXPIRED` (ADR ghi 410; commons chưa có mã này).

**Out of scope**

- **Sweeper** `@Scheduled` giải phóng reservation bị bỏ quên + **test 100 request đồng thời** → `SL-2.3`.
- Không multi-warehouse thật (`warehouse_id = 'WH-1'`), không Redis lock, không gRPC.
- Không seed dữ liệu dev (tạo tồn kho qua `PUT`, tránh phụ thuộc id seed của catalog).
- **Không** trả `TD-3` (handler 403 → commons) — theo rule nợ kỹ thuật, đó là ticket riêng; ở đây copy handler như
  catalog đã làm và ghi nhận `TD-3` đã đạt điều kiện revisit.

**Acceptance Criteria**

- [x] Reserve 2 SKU trong 1 order: hoặc **cả hai** được giữ, hoặc **không cái nào** (rollback sạch, `reserved` không đổi).
- [x] Reserve khi `available` không đủ → **409** + body nêu `skuId` và số còn lại; `reserved` **không** tăng.
- [x] Reserve cùng `orderId` lần 2 → trả **cùng** `reservationId` (không tạo reservation thứ hai, không trừ kho 2 lần).
- [x] Commit → `on_hand` giảm đúng `qty`, `reserved` giảm đúng `qty`; commit lần 2 → **200** (không trừ 2 lần).
- [x] Release → `reserved` giảm, `on_hand` **không** đổi; release lần 2 → **200**; release reservation đã COMMIT → **409**.
- [x] Commit reservation đã quá `expires_at` → **410 RESERVATION_EXPIRED** + `reserved` được trả lại.
- [x] `stock_ledger` có đủ dòng cho mỗi thao tác (`ADJUST`/`RESERVE`/`COMMIT`/`RELEASE`/`EXPIRE`), và
      `on_hand = Σ delta_on_hand`, `reserved = Σ delta_reserved` (đối soát được bằng SQL).
- [x] Bất biến DB: `reserved >= 0`, `on_hand >= 0`, `reserved <= on_hand` (CHECK constraint).
- [x] Unit + IT xanh; OpenAPI + README cập nhật; `e2e-smoke` Phase 1 vẫn 17/17.

**Ghi chú kỹ thuật**

- `reservations` = **1 row/order** + `reservation_items` (chốt với user): nhờ vậy unique `(order_id) WHERE status =
  'PENDING'` vừa là khoá idempotency tự nhiên vừa giữ API trả **một** `reservationId`. (ADR-0006 D1 được cập nhật theo.)
- Chuyển trạng thái bằng `UPDATE … WHERE id = :id AND status = 'PENDING'` → **không** cần lock ứng dụng, double-commit
  bất khả thi; `rowsAffected = 0` ⇒ đọc trạng thái hiện tại để trả 200 (đã xong) / 409 / 410.
- Ledger ghi **2 cột** `delta_on_hand` + `delta_reserved` (thay vì 1 cột `delta`): nhờ vậy đối soát được **cả hai**
  con số bằng 1 câu `SUM`, và không phải suy luận ý nghĩa của dấu theo `reason`.
- `SELECT`/`UPDATE` luôn kèm `warehouse_id` (dù Phase 2 chỉ có `WH-1`) để khi mở multi-warehouse không phải sửa SQL.
- Trade-off chấp nhận: write của 1 SKU hot bị serialise trên 1 row (đúng ý — chống oversell), điều kiện chuyển sang
**Kết quả verify** *(chạy thật: Docker Desktop + Postgres 16, không phải suy luận)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (01:13)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.713 s]
[INFO] ShopLab :: Service Template ........................ SUCCESS [  6.687 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 21.011 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 18.912 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 14.150 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.416 s]
[INFO] BUILD SUCCESS   Total time: 01:13 min
```

| Module | Unit | IT (Testcontainers, Postgres thật) |
|---|---|---|
| **inventory** | **40** (`ReservationServiceTest` 10 · `StockServiceTest` 5 · config 17 · health 8) | **12** (reserve/commit/release/all-or-nothing/idempotent/410/409/401/403/ledger) |
| commons / template / identity / catalog / gateway | 42 / 35 / 50 / 66 / 71 | — / — / 22 / 21 / — |

`checkstyle:check` + `spotless:check`: 0 vi phạm; JaCoCo gate (config + health ≥ 80% LINE) pass.

**2) Dựng container thật + Flyway**

```text
$ docker compose ... up -d --build inventory-service api-gateway
shoplab-inventory-service   Up 2 minutes (healthy)     ← probe /health OK, DB kết nối được
shoplab-api-gateway         Up 2 minutes (healthy)

$ docker exec shoplab-postgres psql -U shoplab -d inventory -c "\dt"
 stock | reservations | reservation_items | stock_ledger | flyway_schema_history
```

**3) Luồng thật qua gateway** (`bash /tmp/verify-inventory.sh`, user thật từ identity)

```text
== 1) admin PUT stock: A=10, B=1        {"onHand":10,...}  {"onHand":1,...}
== 2) GET stock (buyer)                 onHand=10 reserved=0 available=10
== 3) reserve A×3                       HTTP 201 · reservationId=c1d20550-… · expiresAt=+15'
== 4) retry CÙNG orderId                HTTP 200 · cùng reservationId · onHand=10 reserved=3 available=7
== 5) reserve B×5 khi còn 1             HTTP 409 · details=[{field: <skuB>, message: "còn 1, cần 5"}]
== 6) commit                            HTTP 200 · onHand=7 reserved=0
== 7) commit lần 2                      HTTP 200 · onHand=7  (không trừ 2 lần)
== 8) release sau commit                HTTP 409
== 9) commit reservation quá TTL        HTTP 410 · reservation status=EXPIRED · SKU_B onHand=1 reserved=0
== 10) đối soát ledger                  A: stock=(7,0) ledger=(7,0) · B: stock=(1,0) ledger=(1,0)
== 11) buyer PUT stock → 403 · không token → 401
```

**4) Regression**

```text
$ bash tools/e2e-smoke.sh      17 PASS / 0 FAIL · 13 request · 612 ms
$ bash tools/export-openapi.sh VALIDATE PASS · 18 path · 23 operation · 12 operation cần token · 24 schema
   (thêm 4 path + 5 operation của /v1/stock; script export đã thêm inventory vào danh sách fetch)
```

**Sự cố trong lúc làm (ghi lại vì là bài học thật)**

- **`@Transactional` + ném exception = mất phần bù trừ**: nhánh "commit khi đã hết hạn" vừa phải **release stock**
  vừa phải ném `410`. Mặc định Spring rollback ⇒ phần release bị xoá ⇒ hàng bị treo vĩnh viễn mà test HTTP vẫn
  "đúng" (410). Phải khai `@Transactional(noRollbackFor = ReservationExpiredException.class)`.
- **Editor/heredoc làm gãy cấu trúc file 2 lần**: chèn block lớn vào giữa method làm mất/ thừa `}` — `javac` báo
  "';' expected" ở mọi dòng `void …()`, đọc lên rất giống lỗi cú pháp vô nghĩa. Cách tìm nhanh: script đếm độ sâu
  `{}` để biết chính xác dòng nào làm depth về 0 (hoặc EOF lệch 1).
- **`mvn -pl <module> verify` không tự build module phụ thuộc** ⇒ cần `-am`, nếu không Maven báo thiếu
  `shoplab-service-commons:jar` (đã gặp ở `SL-2.11`).

**Learning note**

- **Điều kiện nằm trong câu `UPDATE` là toàn bộ cơ chế**: `on_hand - reserved >= :qty` khiến Postgres quyết định
  trong cùng row lock; `rowsAffected = 0` chính là tín hiệu hết hàng. Không cần `SELECT … FOR UPDATE`, không cần
  distributed lock — và vì thế **không có khe hở** giữa "kiểm tra" và "ghi".
- **Idempotency tự nhiên từ ràng buộc dữ liệu**: unique `(order_id) WHERE status='PENDING'` biến "retry cùng
  orderId" thành *tìm thấy row cũ* thay vì *giữ chỗ lần 2*; không phải tự viết cơ chế dedupe riêng như
  `idempotency_keys` của order-service.
- **Idempotent phải định nghĩa theo từng trạng thái, không chỉ "gọi lại được"**: commit lần 2 → 200, release lần 2 →
  200, nhưng release **sau commit** → 409 (hàng đã rời kho) và commit **sau release** → 409. Nếu gộp thành "mọi lần
  gọi lại đều 200" thì client mất khả năng phát hiện mình đang làm sai thứ tự.
- **Ledger 2 cột delta đáng giá hơn 1 cột `reason`**: `Σ delta_on_hand` và `Σ delta_reserved` cho phép đối soát bằng
  1 câu SQL, và công thức này **đúng cả khi** logic đổi (thêm lý do mới) — verify ở bước 10 đã chứng minh.
- **JDBC thắng JPA ở đúng bài toán này**: cần `rowsAffected` của câu UPDATE có điều kiện; JPA entity + dirty checking
  chỉ thêm tầng trung gian. Đổi lại phải tự viết mapping — chấp nhận được vì schema chỉ 4 bảng và SQL là phần cốt lõi.
- **Test cần "đo được trạng thái thật"**: 3 thứ chỉ IT làm được — rollback all-or-nothing (SKU còn hàng không bị giữ),
  CHECK constraint của DB, và ledger khớp `stock`. Unit test mock repo **không thể** chứng minh cả 3.


  Redis Lua đã ghi ở ADR-0006 Revisit (>1.000 reserve/s hoặc p99 > 50 ms).


### SL-2.3 — inventory: sweeper TTL + test 100 request đồng thời

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.2` (reserve/commit/release + lazy expire); chặn `SL-2.6` (saga cần hàng không bị treo) |
| **Ước lượng** | M |

**Bối cảnh**

`SL-2.2` đã có **lazy expire**: ai gọi `commit` muộn thì nhận 410 và hàng được trả lại. Nhưng trường hợp phổ biến nhất
lại là user **bỏ ngang hẳn** — không gọi commit, cũng không gọi release. Khi đó `reserved` bị giữ vô thời hạn ⇒ **"cháy
hàng ảo"**: kho còn hàng thật nhưng không ai mua được. Phải có job nền dọn.

Đồng thời phase này cần **bằng chứng bằng số liệu** cho tuyên bố "không oversell": 100 request đồng thời cùng 1 SKU thì
đúng bằng số suất còn lại được nhận, phần còn lại bị từ chối — không phải "tin vào code".

**Mục tiêu**

1. Reservation quá `expires_at` mà vẫn `PENDING` được **tự động** chuyển `EXPIRED`, trả chỗ và ghi ledger `EXPIRE` —
   không phụ thuộc việc client có gọi API hay không.
2. Có test chứng minh **không oversell** dưới 100 request song song, kèm số liệu đối soát (`stock` vs `stock_ledger`).

**In scope**

- `ReservationRepository.findExpiredPending(now, limit)` với `FOR UPDATE SKIP LOCKED` + `ORDER BY expires_at LIMIT n`.
- `ReservationService.expireOverdue(now)`: batch, mỗi reservation `markStatus(PENDING→EXPIRED)` = 1 thì mới
  `releaseStock(EXPIRE)`; trả số đã xử lý.
- `ReservationSweeper` (`@Scheduled`, interval/batch đọc từ env) + `@EnableScheduling`; lỗi ở 1 nhịp **không** làm chết
  scheduler.
- `InventoryProperties.sweepBatchSize` + `application.yml` / `.env.example` / compose: `sweep-interval-ms`,
  `sweep-initial-delay-ms`, `sweep-batch-size`.
- Test: unit (expire + skip khi đã bị xử lý + sweeper delegate) · IT (**100 request HTTP song song**, sweeper chạy thật
  với interval ngắn, sweeper không đụng reservation còn hạn).

**Out of scope**

- Không thêm endpoint admin chạy sweeper tay (không đổi API surface ⇒ OpenAPI không đổi).
- Không đụng `TD-14` (job dọn `idempotency_keys` thuộc `order-service`, chưa tồn tại).
- Không tối ưu throughput write của 1 SKU hot (Redis Lua — điều kiện ở ADR-0006 Revisit, Phase 4).

**Acceptance Criteria**

- [x] Reservation `PENDING` quá hạn được sweeper chuyển `EXPIRED`, `reserved` giảm đúng `qty`, `on_hand` **không** đổi.
- [x] Mỗi lần expire ghi **đúng 1** dòng ledger `EXPIRE` với `delta_reserved = -qty`.
- [x] Sweeper **không** đụng reservation `PENDING` còn hạn, và không đụng `COMMITTED/RELEASED/EXPIRED`.
- [x] Sweeper chạy 2 vòng liên tiếp không trả chỗ 2 lần (idempotent theo trạng thái).
- [x] 100 request `POST /v1/stock/reserve` song song, cùng SKU, tồn kho 10, mỗi request qty=1 ⇒ **đúng 10 × 201** và
      **90 × 409**; `reserved = on_hand = 10`; không có `reserved < 0`; `Σ delta_reserved = 10`.
- [x] Sweeper chạy được trong container compose (log có số reservation đã dọn) và không làm chậm probe `/health`.
- [x] Unit + IT xanh; `e2e-smoke` 17/17 (không regress).

**Ghi chú kỹ thuật**

- **`FOR UPDATE SKIP LOCKED`** thay vì advisory lock: 2 instance cùng quét thì mỗi reservation chỉ bị **một** instance
  lấy lock ⇒ trả `TD-15` luôn trong ticket này thay vì để thành nợ. `ORDER BY expires_at` để cái quá hạn lâu nhất được
  dọn trước (giới hạn batch không làm reservation cũ bị bỏ đói).
- **Chốt chặn thứ hai**: `markStatus(PENDING→EXPIRED)` trả 0 ⇒ bỏ qua, **không** release. Nếu chỉ tin vào
  `SKIP LOCKED` thì một đường khác (commit của client) chen vào giữa vẫn có thể gây trả chỗ 2 lần.
- **Batch 100/nhịp, nhịp 60 s**: đủ dọn 100 reservation/phút; chậm hơn nữa thì hàng treo lâu, nhanh quá thì tốn query vô ích.
- Sweeper **bắt `RuntimeException`** trong `sweep()`: exception lọt ra khỏi `@Scheduled` làm nhịp đó chết và log đầy stack
  trace; bắt lại thì nhịp sau vẫn chạy.
- Test 100 request dùng **HTTP thật** (đúng nghĩa "100 request"): thread pool 20 gửi 100 request đồng thời; kỳ vọng đúng
  10 thành công — con số này **không phụ thuộc thứ tự**, chỉ phụ thuộc tồn kho.


**Kết quả verify** *(chạy thật: Docker + Postgres 16, không phải suy luận)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (01:13)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.396 s]
[INFO] ShopLab :: Service Template ........................ SUCCESS [  6.751 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 20.624 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 17.461 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 16.265 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.653 s]
[INFO] BUILD SUCCESS   Total time: 01:13 min
```

`inventory` unit **44** (+4: `expireOverdue` ×2 · `ReservationSweeperTest` ×2) · IT **15** (+3: 100 request song song ·
sweeper expire thật · sweeper không đụng reservation còn hạn). checkstyle + spotless sạch.

**2) Sweeper trên container thật** (`interval=2s` cho stack local, `bash /tmp/verify-sweeper.sh`)

```text
1) reserve 2/5 → reservationId=451fbfac…      trước: onHand=5 reserved=2 available=3
2) đẩy expires_at về quá khứ, chờ sweeper →  sau ~300ms: reserved=0
3) DB sau sweep:
   reservation_status=EXPIRED
   stock on_hand=5 reserved=0
   ledger EXPIRE rows=1
4) log trong container: "sweeper: đã expire 1 reservation quá hạn và trả chỗ"
```

**3) 100 request đồng thời cùng 1 SKU (tồn kho 10)**

```text
   bắn 100 curl song song, mỗi request qty=1 với orderId riêng:
        10 201
        90 409
   DB sau bắn:
   stock on_hand=10 reserved=10 available=0
   số reservation PENDING=10
   ledger sum(delta_reserved)=10
   bất biến reserved<=on_hand: OK
```

⇒ **Không oversell**: đúng 10 suất được cấp, 90 request bị từ chối, và ledger là bằng chứng độc lập (10 lần giữ chỗ).

**4) Regression**

```text
$ bash tools/e2e-smoke.sh   17 PASS / 0 FAIL · 13 request · 638 ms
```

**Sự cố trong lúc làm (bài học thật)**

- IT ban đầu log đầy `Connection is closed` + `JDBC rollback failed`: sweeper vẫn chạy sau khi test xong, trong khi
  Testcontainers **đã dừng Postgres** (context Spring được cache tới cuối JVM). Fix: `@DirtiesContext(AFTER_CLASS)` để
  đóng context ngay sau class + `spring.task.scheduling.shutdown.await-termination` để không cắt ngang transaction.
  Noise về **0** sau fix.
- Chạy 2 lệnh `mvn verify` chồng nhau (một cái còn trong pipe) ⇒ lần sau đọc log thấy `BUILD FAILURE` không rõ nguyên
  nhân. Bài học: **luôn redirect log ra file rồi grep**, đừng pipe trực tiếp khi còn job nền.

**Learning note**

- **Lazy expire và sweeper phục vụ 2 nhánh khác nhau, phải có cả hai**: lazy expire xử lý "client gọi muộn"; sweeper xử
  lý "client không bao giờ gọi" (bỏ ngang). Chỉ có lazy expire ⇒ hàng treo vĩnh viễn; chỉ có sweeper ⇒ user commit ở
  phút 15:01 nhận `200` sai. Hai đường, một bất biến.
- **`FOR UPDATE SKIP LOCKED` biến "chạy N instance" thành chuyện của DB, không phải của app**: không cần advisory lock,
  không cần leader election. Nhưng nó **không đủ một mình** — vẫn cần conditional `markStatus` làm chốt thứ hai cho
  trường hợp client commit chen vào giữa lúc sweeper đang xử lý.
- **Job nền phải "chết đúng cách"**: bắt `RuntimeException` trong `sweep()` để lỗi 1 nhịp không giết scheduler; và cấu
  hình `await-termination` để shutdown không cắt transaction đang chạy.
- **Bằng chứng oversell phải là số tuyệt đối, không phải tỉ lệ**: "10 thành công / 90 bị từ chối" + `Σ delta_reserved =
  reserved` kiểm được bằng SQL; nếu chỉ assert `reserved <= on_hand` thì code sai kiểu "giữ ít hàng hơn thực tế" vẫn qua.
- **Test đua phải bắn đồng thời thật**: dùng `CountDownLatch` cho 100 thread cùng nhả, và nới connection pool — nếu
  pool nhỏ, request xếp hàng nên không còn đua, test "xanh" mà không chứng minh gì.


### SL-2.4 — `promotion-service`: voucher + rule giảm giá + lock/commit/release

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.2`/`SL-2.3` (pattern reserve/commit/release + sweeper đã chứng minh); **chặn** `SL-2.6` (saga gọi lock voucher) |
| **Ước lượng** | M |

**Bối cảnh**

Luồng checkout §6.1 gọi **lock voucher trước** khi giữ stock — chưa có service này thì `order-service` không có gì để gọi.
Bài toán giống inventory nhưng ở dạng nhẹ hơn: một voucher có `quota` lượt, và **`used + locked <= quota`** phải được DB
canh, nếu chỉ đếm `used` thì hai checkout đồng thời cùng thấy "còn lượt" và bán quá quota.

**Mục tiêu**

Service thứ 5 (`8086`, DB `promotion`) chạy end-to-end: admin tạo voucher (PERCENT/FIXED, quota, cửa sổ thời gian), user
lock 1 lượt (idempotent theo `orderId`), commit khi tiền về, release khi huỷ/hết hạn, sweeper tự trả lượt bị bỏ quên.
Số tiền giảm giá được **snapshot** tại lúc lock.

**In scope**

- Service mới `services/promotion-service`: pom · Dockerfile · application.yml · security (JWT) · health/readiness ·
  logback · **JDBC (không JPA)** như inventory.
- Migration `V1__create_promotion.sql`: `vouchers` (code unique, type, value, max_discount, min_order, quota, used,
  locked, cửa sổ thời gian) + `voucher_locks` (1 row/order, snapshot subtotal/discount, TTL) + CHECK bất biến.
- API: `POST /v1/vouchers` (admin) · `GET /v1/vouchers/{code}` (admin) ·
  `POST /v1/vouchers/lock|commit|release` (user đã đăng nhập).
- Rule: `PERCENT` (có `max_discount` cap) + `FIXED`, HALF_UP 2 chữ số, giảm không vượt subtotal, `min_order`.
- Sweeper TTL cho voucher lock (cùng khuôn `SKIP LOCKED` của `SL-2.3`).
- Hạ tầng: compose · `.env.example` · gateway route `/v1/vouchers` · CI matrix · Dockerfile 6 service (thêm `COPY pom`)
  · `tools/export-openapi.sh` fetch promotion.

**Out of scope**

- `campaign` / flash sale / stacking nhiều voucher / voucher theo seller (Phase 4+).
- `FREESHIP` (chưa có khái niệm phí ship trong luồng Phase 2).
- Không có API "huỷ voucher" cho user (admin vẫn sửa DB được; cần thì ticket sau).

**Acceptance Criteria**

- [x] Tạo voucher: mã chuẩn hoá `UPPER + trim`; trùng mã → **409**; `PERCENT > 100` / `maxDiscount` cho FIXED /
      `endAt <= startAt` → **400**.
- [x] Lock: `PERCENT` tính đúng theo `subtotal`, cap bởi `maxDiscount`, giảm ≤ subtotal; trả `discount` + `finalTotal`.
- [x] Lock retry cùng `orderId` → **200** + **cùng `lockId`**, `locked` không tăng.
- [x] Lock khi chưa tới giờ / đã hết hạn / hết lượt / dưới `minOrder` → **409** (message nêu đúng lý do).
- [x] Commit: `used+1, locked-1`; commit lần 2 → **200** (không dùng 2 lượt); release sau commit → **409**.
- [x] Release: `locked-1`, `used` không đổi; release lần 2 → **200**.
- [x] Commit lock quá TTL → **410** + trả lượt ngay; sweeper cũng trả lượt cho lock bị bỏ quên.
- [x] **100 request song song** cùng voucher (quota 10, cùng lúc) ⇒ đúng **10 lock**, 90 bị **409**; `used+locked <= quota`.
- [x] Unit + IT xanh; OpenAPI + README cập nhật; `e2e-smoke` 17/17.

**Ghi chú kỹ thuật**

- **Cùng pattern với inventory** (ADR-0006 D1): điều kiện quota nằm trong `UPDATE … WHERE used + locked < quota AND now()
  BETWEEN start_at AND end_at`; `0 row` ⇒ service đọc lại voucher để nói rõ lý do (chưa tới / hết hạn / hết lượt) thay vì
  một câu "không dùng được" chung chung.
- **Snapshot `discount` + `subtotal`** vào `voucher_locks`: commit phải dùng đúng con số đã hứa với client, kể cả khi
  admin sửa `value` trong lúc user đang thanh toán.
- **`@Transactional(noRollbackFor = VoucherLockExpiredException.class)`** ở `commit`: nhánh hết hạn vừa trả lượt vừa ném
  410 — mặc định Spring rollback sẽ xoá phần trả lượt (bẫy đã gặp ở `SL-2.2`).
- **TTL = 15 phút, bằng inventory**: checkout giữ cả hai trong cùng phiên thanh toán; lệch nhau chỉ đẻ thêm nhánh
  compensating không cần thiết.
- Đánh đổi đã chấp nhận: `subtotal` do order-service gửi (call nội bộ). Nếu endpoint lộ ra client thì phải tính lại
  server-side — ghi vào Learning note để nhớ.



**Kết quả verify** *(chạy thật: Docker + Postgres 16, không phải suy luận)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (01:34)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.622 s]
[INFO] ShopLab :: Service Template ........................ SUCCESS [  5.930 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 20.954 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 19.403 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 16.656 s]
[INFO] ShopLab :: Promotion Service ....................... SUCCESS [ 15.xxx s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.249 s]
[INFO] BUILD SUCCESS   Total time: 01:34 min
```

`promotion` unit **48** (`VoucherCalculatorTest` 7 · `VoucherServiceTest` 6 · `VoucherLockServiceTest` 11 ·
`VoucherLockSweeperTest` 2 · config/health 22) · IT **13**. checkstyle + spotless sạch.

**2) Luồng thật qua gateway** (`bash /tmp/verify-promotion.sh`, token thật từ identity)

```text
1) tạo voucher PERCENT 10% (cap 50k, quota 5)   DB: used=0 locked=0 quota=5
2) lock đơn 200.000                             HTTP 201 · discount=20000 · finalTotal=180000 · DB: locked=1
3) retry CÙNG orderId                           HTTP 200 · CÙNG lockId · locked vẫn =1
4) commit                                       HTTP 200 · DB: used=1 locked=0
5) release sau commit                           HTTP 409
6) lock khác + release                          HTTP 200 · DB: used=1 locked=0 (used giữ nguyên)
7) commit lock quá TTL                          HTTP 410 · lock status=EXPIRED · DB: locked=0
8) sweeper (interval 2s)                        ~1500ms sau: locked=0, status=EXPIRED
   log container: "sweeper: đã expire 1 voucher lock quá hạn và trả lượt"
```

**3) 100 request đồng thời cùng voucher (quota 10)**

```text
        10 201
        90 409
   DB: used=0 locked=10 quota=10 · bất biến used+locked<=quota: OK
```

**4) Quyền + regression**

```text
POST /v1/vouchers (buyer) → 403 · POST /v1/vouchers/lock (no token) → 401
$ bash tools/e2e-smoke.sh      17 PASS / 0 FAIL
$ bash tools/export-openapi.sh VALIDATE PASS · 23 path · 28 operation · 29 schema
```

**Sự cố trong lúc làm**

- `docker build` của promotion **fail** ở `dependency:go-offline`: Dockerfile copy từ inventory nhưng `sed` đổi luôn dòng
  `COPY services/inventory-service/pom.xml` → thành promotion ⇒ **thiếu pom của inventory** ⇒ Maven báo
  "Child module … does not exist". Fix: thêm lại dòng COPY cho module inventory. Kiểm lại: **6/6 Dockerfile** đều copy đủ
  6 pom.
- `<name>` trong pom bị copy nguyên "ShopLab :: Inventory Service" ⇒ reactor in sai tên service (thấy 2 dòng Inventory trong
  báo cáo build). Fix + xác nhận lại.
- Thêm block test lớn bằng editor bị **cắt khung** làm gãy `{}` hai lần; cách xử lý hiệu quả: ghi cả file bằng heredoc
  (`cat > f <<'EOF'` / `cat >> f <<'EOF'`) thay vì chèn từng đoạn bằng editor.

**Learning note**

- **Copy service là con đường ngắn nhất để tạo nợ ẩn**: 3 lỗi trong ticket này đều từ copy (`COPY pom` thiếu, `<name>` sai,
  comment nói "inventory"). Checklist khi tạo service mới từ service cũ: pom (artifactId/**name**/description/jacoco
  include) · Dockerfile (đủ **mọi** pom module) · application.yml (DB + port + prefix property) · compose/gateway/CI/env.
- **Cùng một pattern, lần thứ hai rẻ hơn nhiều**: lock/commit/release của voucher chỉ mất ~1/2 thời gian so với inventory vì
  đã có khuôn (conditional `UPDATE`, compare-and-set status, `noRollbackFor` cho nhánh vừa-trả-vừa-ném, sweeper
  `SKIP LOCKED`). Đó là lợi ích thật của việc làm đúng ngay từ service đầu.
- **Bất biến "used + locked <= quota" phải ở DB**: nếu chỉ kiểm trong code, một đường ghi mới (script, migration, service
  khác) là bán quá quota. CHECK constraint là thứ duy nhất không thể quên.
- **Snapshot tiền tại lúc lock**: giảm giá đã hứa với client phải bất biến — commit dùng `discount` trong `voucher_locks`,
  không tính lại từ `vouchers.value` (admin sửa `value` giữa chừng sẽ làm hoá đơn sai).
- **Đánh đổi cần nhớ**: `subtotal` hiện do order-service gửi (call nội bộ). Nếu endpoint lộ ra client thì phải tính lại
  server-side — ghi vào đây để không quên khi mở API cho bên ngoài.

### SL-2.5 — `order-service`: tạo order idempotent + state machine + snapshot giá

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.2`/`SL-2.4` (inventory reserve, promotion lock); **chặn** `SL-2.6` (saga gọi order) |
| **Ước lượng** | M |

**Bối cảnh**

Đơn hàng là bản ghi pháp lý của giao dịch: sai một con số là sai hoá đơn, và tạo trùng một đơn là trừ tiền/trừ kho hai lần.
Checkout của user thật luôn có retry (mất mạng, bấm 2 lần, app tự gửi lại) ⇒ **idempotency là yêu cầu bắt buộc**, không phải
"nice to have". Ticket này dựng `order-service` với 3 trọng tâm: **idempotency** (ADR-0006 D2), **snapshot** (D3) và
**state machine** một chiều.

**Mục tiêu**

Service thứ 6 (`8087`, DB `order`): tạo đơn idempotent theo `Idempotency-Key`, lưu snapshot giá/tên SKU trong
`order_items`, và chỉ cho phép `PENDING_PAYMENT → CONFIRMED | CANCELLED | EXPIRED` (mỗi lần chuyển đều có history).

**In scope**

- Service mới (pom · Dockerfile · yml · security · health · logback · JDBC) + migration `V1__create_order.sql`
  (`orders`, `order_items`, `order_status_history`, `idempotency_keys` + CHECK bất biến tiền).
- API: `POST /v1/orders` (bắt buộc `Idempotency-Key`) · `GET /v1/orders/{id}` · `GET /v1/orders` (phân trang) ·
  `POST /v1/orders/{id}/confirm|cancel|expire`.
- Ngữ nghĩa idempotency đủ 4 nhánh: 201 mới · 200 replay (header `Idempotent-Replayed`) · 422 key dùng lại · 409 đang chạy.
- **Job dọn `idempotency_keys`** (COMPLETED > 24 h, IN_PROGRESS > 1 h) ⇒ **trả `TD-14`**.
- `commons`: thêm `UNPROCESSABLE_ENTITY(422)` + map `fromHttpStatus`.
- Hạ tầng: compose · `.env.example` · gateway route `/v1/orders` · CI matrix · Dockerfile 7 service · `export-openapi.sh`.

**Out of scope**

- **Saga** (gọi inventory/promotion/payment) → `SL-2.6`; **sweeper expire đơn** → `SL-2.9` (phải release stock+voucher kèm).
- Không gọi `catalog` để lấy giá: `GET /v1/skus?ids=` chưa tồn tại (thêm là ticket riêng của catalog, §3.1) và `carts` đã
  thiết kế lưu `price_snapshot`.

**Acceptance Criteria**

- [x] `POST /v1/orders` thiếu header → **400**; lần đầu → **201**; retry cùng key+payload → **200** + cùng `id` +
      header `Idempotent-Replayed: true`; cùng key payload khác → **422**; row `IN_PROGRESS` → **409**.
- [x] `order_items` giữ **snapshot** (`sku_code`, `title`, `unit_price`, `qty`, `line_total`); `line_total` và
      `total = subtotal - discount` bị **CHECK ở DB** canh (thử UPDATE sai → bị chặn).
- [x] `confirm` → CONFIRMED, **confirm lần 2 → 200** (không ghi history lần 2), `cancel` sau `confirm` → **409**.
- [x] `cancel`/`expire` trên đơn PENDING → 200 + `order_status_history` có `reason`.
- [x] Đơn của người khác → **404** (không lộ sự tồn tại); admin đọc được; list của buyer chỉ có đơn của mình.
- [x] **30 request song song cùng `Idempotency-Key` ⇒ đúng 1 đơn** trong DB (không tạo trùng).
- [x] Job dọn idempotency xoá row cũ (48 h) và giữ row mới; log `idempotency cleanup: đã xoá N row`.
- [x] Unit + IT xanh; OpenAPI + README cập nhật; `e2e-smoke` 17/17.

**Ghi chú kỹ thuật**

- **Tiền do server tính** từ `unitPrice × qty`; client **không** gửi tổng (gửi tổng là mời gọi sửa 1 field để trả ít hơn).
- **Ghi khoá trước, ngoài transaction** (đúng ADR "transaction riêng") rồi mới mở transaction tạo đơn — lý do ở Learning
  note: `REQUIRES_NEW` lồng trong transaction + pool nhỏ = **deadlock pool** (đã gặp thật khi chạy 20 request song song).
- **`complete()` nằm trong transaction của việc tạo đơn** ⇒ crash giữa chừng không để lại khoá COMPLETED trỏ tới đơn ma.
- **So sánh payload bằng SHA-256** của body đã serialize: cùng key mà hash khác ⇒ 422 (client phải sửa request).
- Quyền đọc đơn của người khác trả **404 chứ không 403**: không xác nhận cho người ngoài biết đơn có tồn tại.

**Kết quả verify** *(chạy thật: Docker + Postgres 16)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (01:42)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.348 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 19.450 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 16.748 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 16.482 s]
[INFO] ShopLab :: Promotion Service ....................... SUCCESS [ 16.117 s]
[INFO] ShopLab :: Order Service ........................... SUCCESS [ 15.624 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.529 s]
[INFO] BUILD SUCCESS   Total time: 01:42 min
```

`order` unit **43** (`OrderPricingTest` 6 · `IdempotencyServiceTest` 7 · `OrderServiceTest` 8 · config/health 22) · IT **10**.

**2) Luồng thật qua gateway** (`bash /tmp/verify-order.sh`)

```text
1) tạo đơn: HTTP 201 · total=180000 · subtotal=200000 · status=PENDING_PAYMENT
   DB order_items: SKU-X | Ao thun | 100000.00 x 2 = 200000.00
   DB history: (null) -> PENDING_PAYMENT · idempotency: COMPLETED (201)
2) retry cùng key+payload: HTTP 200 · id TRÙNG · số đơn có key này: 1
3) cùng key payload khác → HTTP 422 UNPROCESSABLE_ENTITY
4) thiếu header → HTTP 400
5) confirm → 200 · status=CONFIRMED
6) confirm lần 2 → 200 · history = 2 dòng (không ghi thêm)
7) cancel đơn đã CONFIRMED → HTTP 409
8) cancel đơn PENDING → 200 · reason trong history = "het tien" · expire → 200 · status=EXPIRED
9) admin GET → 200 · không token → 401
10) 20 request song song cùng key: 1×201 + 15×200 (replay) + 4×409 ⇒ số đơn trong DB: 1
```

**3) Job dọn idempotency trên container** (interval 3 s cho demo)

```text
1) tạo row tuổi 48h → sau ~3000ms: row đã bị xoá (cleanup chạy thật)
2) log: "idempotency cleanup: đã xoá 2 row (COMPLETED > PT24H, IN_PROGRESS > PT1H)"
```

**4) Regression + contract**

```text
$ bash tools/e2e-smoke.sh       17 PASS / 0 FAIL
$ bash tools/export-openapi.sh  VALIDATE PASS · 28 path · 34 operation · 34 schema
```

**Sự cố trong lúc verify (3 lỗi thật, đáng ghi)**

- **Deadlock connection pool** (nặng nhất): `begin()` dùng `REQUIRES_NEW` **lồng trong** `@Transactional` của tạo đơn ⇒ mỗi
  request cần 2 connection cùng lúc; pool 5 + 20 request song song ⇒ cả 5 thread giữ 1 và chờ 1 ⇒ `CannotCreateTransactionException`
  (HTTP 500), dù logic đúng. Fix: tách `OrderWriter` (@Transactional) và gọi `begin()` **trước** khi mở transaction.
- **OpenAPI trùng schema `Item`**: order-service và promotion-service đều có record lồng tên `Item` ⇒ script merge từ chối
  (đúng!). Đổi thành `Line`.
- **Test của tôi sai, không phải code**: mỗi `buyerToken()` sinh **user khác** ⇒ scope idempotency khác nhau (retry thành
  đơn mới). Và seed có `discount = 0` nên `total = subtotal` vẫn đúng CHECK ⇒ phải lệch thật (`+100`). Cùng bài học:
  **test sai cho kết quả "đỏ" trông giống bug production** — phải đọc kỹ thông điệp assertion.

**Learning note**

- **`REQUIRES_NEW` lồng trong transaction là bẫy pool**: nó cần connection **thứ hai** trong khi connection thứ nhất chưa
  nhả. Với pool nhỏ, N thread đồng thời = deadlock toàn phần. Cách đúng: ghi khoá **trước** khi mở transaction (hoặc dùng
  pool đủ lớn + timeout, nhưng đó là che bệnh).
- **Idempotency phải khoá theo `(user, endpoint)`, không theo key đơn thuần**: key do client sinh, hai user có thể trùng
  key; scope tách ra thì không đụng nhau.
- **CHECK constraint ở DB là thứ duy nhất không quên**: `total = subtotal − discount`, `line_total = unit_price × qty` —
  nếu chỉ tính trong code thì một script/migration sau này có thể tạo hoá đơn sai mà không ai biết.
- **404 thay vì 403 cho tài nguyên của người khác**: 403 xác nhận "có tồn tại nhưng bạn không được xem" — với đơn hàng,
  điều đó đã là rò rỉ thông tin.
- **Snapshot là quyết định nghiệp vụ**, không phải kỹ thuật: đơn đã đặt phải giữ giá lúc đặt, kể cả khi catalog đổi giá.
- **Job dọn phải có ngưỡng cho cả 2 loại row**: `COMPLETED` giữ 24 h (retry muộn vẫn dedupe), `IN_PROGRESS` quá 1 h coi là
  mồ côi — không dọn thì client nhận 409 **vĩnh viễn** cho một key đã crash.

### SL-2.7 — `payment-service`: payment intent + mock provider + webhook idempotent

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | Chốt ở `SL-2.0` ([ADR-0006](docs/adr/0006-checkout-model.md) D2/D4); **chặn** `SL-2.6` (saga cần tạo payment-intent) |
| **Ước lượng** | M |

**Bối cảnh**

Đây là chỗ **tiền thật** đi qua. Hai nguồn gây trùng khác nhau, nên phải chặn bằng hai cơ chế khác nhau:

1. **Client** bấm "thanh toán" 2 lần ⇒ `Idempotency-Key` (như `SL-2.5`).
2. **Provider** gọi webhook N lần cho cùng một giao dịch (retry, timeout mạng) ⇒ khoá **tự nhiên của provider**
   (`provider_txn_id`, `event_id`), `architecture.md` §6.2. Đây là lần đầu service nhận traffic **không có JWT**.

**Mục tiêu**

Service thứ 7 (`8088`, DB `payment`): tạo payment-intent idempotent, mô phỏng provider bằng **mock** (Phase 2 chưa nối
provider thật), và nhận webhook **idempotent** — gửi lại 1 lần hay 20 lần song song thì tiền chỉ được ghi nhận 1 lần.

**In scope**

- Service mới (pom · Dockerfile · yml · security · health · JDBC) + `V1__create_payment.sql`
  (`payments`, `payment_webhook_events`, `idempotency_keys`).
- `POST /v1/payment-intents` (buyer + **bắt buộc** `Idempotency-Key`) · `GET /v1/payment-intents/{id}` ·
  `GET /v1/payment-intents?orderId=`. Mỗi đơn tối đa **1 intent sống** (unique index có điều kiện).
- `POST /v1/webhooks/payment` — **public**, xác thực **HMAC-SHA256** (`X-Provider-Signature` + `X-Provider-Timestamp`,
  cửa sổ 5 phút), idempotent theo `provider_event_id` + `provider_txn_id UNIQUE`.
- **Mock provider** (`PAYMENT_MOCK_ENABLED`, fail-closed ngoài `local`): `POST /mock-provider/pay/{providerIntentId}`
  (+ `?fail=true` mô phỏng thẻ bị từ chối) — sinh `provider_txn_id` rồi đi qua **đúng** đường webhook.
- Hạ tầng: compose · `.env.example` · gateway route `/v1/payments` + `/v1/payment-intents` + public `/v1/webhooks/**` ·
  CI matrix · Dockerfile 7 service · `tools/export-openapi.sh`.

**Out of scope**

- **Không** gọi `order-service` để confirm đơn: Phase 2 chưa có broker, việc chuyển `CONFIRMED` thuộc saga `SL-2.6`
  (nó giữ token user). Hệ quả đã ghi nhận: `TD-17`.
- **Refund / `transactions`**: bảng trong `architecture.md` §8.4 để Phase 6, chưa có ticket ⇒ `TD-17`.
- **Cancel intent** + sweeper hết hạn intent: thuộc `SL-2.6`/`SL-2.9` (compensating).
- Broker/outbox `payment.succeeded` → Phase 3.

**Acceptance Criteria**

- [x] `POST /v1/payment-intents`: thiếu header → **400** · lần đầu → **201** · retry cùng key → **200** + cùng `id` +
      `Idempotent-Replayed: true` · cùng key payload khác → **422** · row `IN_PROGRESS` → **409**.
- [x] Đơn đã có intent sống, tạo intent thứ hai **cùng** amount ⇒ trả lại intent cũ (không tạo row mới); **khác** amount
      ⇒ **409**.
- [x] `GET /v1/payment-intents/{id}`: intent của người khác → **404**; admin xem được.
- [x] Webhook `succeeded` → payment `SUCCEEDED` + `provider_txn_id` + `paid_at`; `failed` → `FAILED` (không có `paid_at`).
- [x] **Webhook gửi lại 5 lần cùng `event_id` ⇒ `payment_webhook_events` = 1 row, payment không đổi, trả 200 (`duplicate: true`)**.
- [x] **20 webhook song song cùng `event_id` ⇒ 1 row** (kể cả trường hợp `event_id` khác nhưng cùng `provider_txn_id`).
- [x] Chữ ký sai → **401**; timestamp lệch > 5 phút → **401**; `amount` lệch intent → **409** (trạng thái không đổi).
- [x] `mock-pay` → payment `SUCCEEDED`; `?fail=true` → `FAILED`; gọi lại `mock-pay` khi đã terminal ⇒ 200, không đổi dữ liệu.
- [x] DB canh bất biến: `amount > 0`, `status = SUCCEEDED ⇔ paid_at IS NOT NULL` (thử UPDATE sai → bị chặn).
- [x] Unit + IT xanh; `e2e-smoke` 17/17; OpenAPI export pass.

**Ghi chú kỹ thuật**

- **Hai cơ chế idempotency, không trộn**: client ⇒ `idempotency_keys` (scope `"<userId>:POST /v1/payment-intents"`);
  provider ⇒ `payment_webhook_events.provider_event_id` (PK) + `payments.provider_txn_id` (UNIQUE). Provider **không**
  gửi `Idempotency-Key` của client — nó không biết key đó.
- **Webhook phải verify trên raw body** (không phải JSON đã parse): HMAC tính trên byte gốc, parse lại rồi ký thì không
  khớp. Controller nhận `String` thô, verify, rồi mới parse.
- **Dedupe insert và apply nằm CÙNG transaction**: event id lạ mà trỏ tới intent không tồn tại ⇒ rollback ⇒ row dedupe
  cũng biến mất (provider retry lại được), không tạo "đã nhận nhưng chưa xử lý".
- **Mock provider đi qua đúng đường webhook** (không gọi thẳng repository) để test/verify chứng minh cả chữ ký + dedupe.
- Fail-closed: `PAYMENT_MOCK_ENABLED=true` ở `APP_ENV != local` ⇒ **chết lúc boot**, không để endpoint giả tiền lọt lên prod.

**Kết quả verify** *(chạy thật: Docker + Postgres 16)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (02:01)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.350 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 20.527 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 18.403 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 16.685 s]
[INFO] ShopLab :: Promotion Service ....................... SUCCESS [ 16.672 s]
[INFO] ShopLab :: Order Service ........................... SUCCESS [ 15.932 s]
[INFO] ShopLab :: Payment Service ......................... SUCCESS [ 14.300 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.716 s]
[INFO] BUILD SUCCESS   Total time: 02:01 min
```

`payment` unit **59** (`PaymentServiceTest` 5 · `PaymentWriterTest` 3 · `PaymentWebhookServiceTest` 7 ·
`WebhookSignatureVerifierTest` 5 · `MockProviderServiceTest` 3 · `PaymentPropertiesTest` 5 · `RequiredEnvTest` 2 ·
config/health/idempotency 29) · IT **12** · jacoco `config/*`+`health/*` **88.6 %** LINE (gate 80 %).

**2) Luồng thật qua gateway** (`bash /tmp/verify-payment.sh`)

```text
 1) tạo intent (buyer)                  HTTP 201 · status=REQUIRES_PAYMENT · amount=180000 VND
                                       paymentUrl=…/mock-provider/pay/pi_mock_d51d0c48…
 2) retry cùng key+payload              HTTP 200 · id TRÙNG · Idempotent-Replayed: true
 3) cùng key payload khác               HTTP 422
 4) thiếu header Idempotency-Key        HTTP 400
 5) cùng đơn, KHÁC số tiền              HTTP 409   (một đơn một intent sống)
 6) GET bằng user khác → 404 · admin → 200 · không token → 401
 7) mock-pay (public, qua gateway)      HTTP 200 → GET intent: SUCCEEDED + providerTxnId=txn_mock_d420ea40…
 8) mock-pay lần 2 (đã terminal)        HTTP 200 · không đổi dữ liệu (eventId=null)
 9) webhook gửi lại 5 lần cùng event    cả 5 → HTTP 200; lần 1 `duplicate=false`, lần 2..5 `duplicate=true`
10) 20 webhook SONG SONG cùng event     {200: 20}  → DB: 1 row cho event đó
11) chữ ký sai → 401 · timestamp cũ 30 phút → 401   (không ghi gì)
12) webhook sai số tiền (1.00 vs 120000) HTTP 409 · payment vẫn REQUIRES_PAYMENT
13) mock-pay?fail=true                 HTTP 200 → status=FAILED, paid_at NULL
```

**3) Bằng chứng DB** (`psql -d payment`)

```text
 provider_event_id  | count     -- 5 lần gửi lại + 20 request song song
 evt_manual_7c72f055 |     1
 evt_race_e3d6e3d0   |     1

      status      |            provider_txn_id               | has_paid_at
 FAILED           | txn_mock_6d038030-…                      | f
 REQUIRES_PAYMENT | (null)                                   | f   ← webhook sai tiền/chữ ký không đổi gì
 SUCCEEDED        | txn_mock_d420ea40-…                      | t   ← bất biến SUCCEEDED ⇔ paid_at
```

**4) Regression + contract**

```text
$ bash tools/e2e-smoke.sh        17 PASS / 0 FAIL · 13 request · 621 ms
$ bash tools/export-openapi.sh   VALIDATE PASS · 32 path · 39 operation · 37 schema  (trước: 28/34/34)
```

**Sự cố trong lúc build/verify (2 lỗi thật)**

- **`Types.TIMESTAMP_WITH_TIMEZONE` + `java.sql.Timestamp`** ⇒ `PSQLException: Cannot cast an instance of
  java.sql.Timestamp to type Types.TIMESTAMP_WITH_TIMEZONE` ⇒ webhook/mock-pay trả 500. Chỉ IT (Postgres thật) bắt được;
  đổi sang `Types.TIMESTAMP` (TIMESTAMPTZ vẫn nhận `setTimestamp` đúng offset).
- **Checkstyle `MethodLength` 80 + jacoco gate 80 %**: `apply()` dài 84 dòng ⇒ tách `validate` / `requirePayment` /
  `recordEvent` / `transition`; coverage 76 % ⇒ **không** kê test rỗng mà viết test cho đúng phần thiếu
  (`PaymentProperties` các nhánh fail-closed, `RequiredEnv`, 401 không token) ⇒ 88,6 %.

**Learning note**

- **Hai nguồn trùng ⇒ hai cơ chế, không trộn**: client bấm 2 lần dùng `Idempotency-Key`; provider retry dùng khoá
  **tự nhiên của nó** (`provider_event_id` + `provider_txn_id`). Provider không biết key của client — dùng chung một
  khoá là bắt client chịu trách nhiệm cho lỗi của provider.
- **Ký HMAC trên raw body, và timestamp phải nằm trong chuỗi được ký**: parse JSON rồi serialize lại thay đổi thứ tự
  key/khoảng trắng ⇒ chữ ký không bao giờ khớp; timestamp ngoài chuỗi ký thì kẻ tấn công giữ body + chữ ký cũ bắn lại
  mãi mãi. So sánh phải constant-time (`MessageDigest.isEqual`).
- **Chốt chặn phải ở DB, không chỉ ở code**: `provider_txn_id UNIQUE` và unique index có điều kiện
  `(order_id) WHERE status IN (...)` diễn đạt bất biến nghiệp vụ bằng ràng buộc — 20 request song song vẫn đúng vì
  DB phân xử, không phải vì code chạy tuần tự.
- **Mock phải đi qua đúng đường thật**: nếu mock tự set `status = SUCCEEDED` thì test xanh mà production gãy ở đúng
  chỗ khó thấy nhất (chữ ký, dedupe, compare-and-set). Mock đi qua webhook ⇒ verify chứng minh luôn cả 3 thứ đó.
- **Cờ "chỉ dùng ở local" phải fail-closed lúc boot** (`MockProviderGuard`) và secret bắt buộc không default
  (`PAYMENT_WEBHOOK_SECRET`) — mock provider cho không hàng, secret rỗng cho ai cũng ký. Cả hai đều là lỗi không thể
  phát hiện bằng test chức năng.
- **`.env` phải bổ sung biến REQUIRED của compose** (`${VAR:?}`) trước khi `up` — nếu không compose dừng cả stack vì một
  service mới, dễ hiểu nhầm là lỗi image.
### SL-2.1 — `cart-service`: giỏ hàng trên Redis + TTL + merge khi login

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | Chốt ở `SL-2.0` ([ADR-0006](docs/adr/0006-checkout-model.md) D4 `cart (read)` là bước 1 của saga); **chặn** `SL-2.6` (saga đọc giỏ) |
| **Ước lượng** | M |

**Bối cảnh**

Giỏ hàng là thứ **duy nhất trong hệ thống chấp nhận mất dữ liệu**: mất giỏ = khách bỏ hàng vào lại, không ai mất tiền, không sai sổ
sách. Nhờ vậy nó là chỗ đúng để dùng **Redis TTL 30 ngày** thay Postgres (`architecture.md` §8.3/§8.5).

Hai đặc thù mới so với 6 service trước:

1. **Khách chưa đăng nhập vẫn có giỏ** ⇒ identity không chỉ đến từ JWT (guest token).
2. **Một dòng giỏ là read-modify-write** (cộng/đặt số lượng) ⇒ phải nguyên tử, nếu không hai request cùng SKU sẽ mất update.

**Mục tiêu**

Service thứ 8 (`8083`, **không có Postgres**): guest tạo giỏ, thêm/sửa/xoá dòng, xem giỏ, TTL 30 ngày tự làm mới khi có ghi, và
**merge giỏ guest vào giỏ user khi login**.

**In scope**

- Service mới (pom `spring-boot-starter-data-redis` · Dockerfile · yml · security · health · logback), **không** datasource/Flyway.
- Redis HASH `cart:<owner>` (owner = `u:<userId>` hoặc `g:<guestToken>`), field = `skuId`, value = `{qty, addedAt}`, TTL 30 ngày.
- API: `POST /v1/carts/guest` · `GET /v1/carts` · `PUT /v1/carts/items/{skuId}` (set qty, idempotent) ·
  `DELETE /v1/carts/items/{skuId}` · `DELETE /v1/carts` · `POST /v1/carts/merge`.
- **Lua script** cho mọi phép ghi (đặt số lượng, xoá, merge) — atomic + giữ bất biến `1..999` ngay trong Redis.
- Hạ tầng: compose (depends redis) · `.env.example` · gateway route `/v1/carts` + **public-path** · CI matrix ·
  Dockerfile 8 service · `tools/export-openapi.sh`.

**Out of scope**

- **Không** snapshot giá/title, **không** gọi `catalog`: quyết định A — giá hiển thị lấy từ product page, còn tiền thật do
  `order-service` tính lại từ payload lúc checkout (`SL-2.5`). Hệ quả ghi `TD-20`.
- Không kiểm tồn kho ở giỏ (inventory `reserve` quyết lúc checkout), không coupon/voucher trong giỏ (saga `SL-2.6`).
- Không merge user→user, không gộp giỏ khách vãng lai nhiều thiết bị, không gọi service nào (Phase 2 chỉ 1 chiều).

**Acceptance Criteria**

- [x] `POST /v1/carts/guest` (public) → **201** `{cartToken, expiresAt}`; token là UUID do **server** cấp.
- [x] Chưa login, có `X-Cart-Token`: thêm / sửa / xoá / xem giỏ đều **200**; token sai/không tồn tại → **401**.
- [x] Có JWT: giỏ của user (không cần `X-Cart-Token`); không JWT + không token → **401**.
- [x] `PUT /v1/carts/items/{skuId}` **idempotent** (gọi 2 lần cùng qty ⇒ cùng kết quả); `qty` ngoài `1..999` → **422**.
- [x] Giỏ quá **50 dòng** → **422** (chặn memory abuse ở endpoint public).
- [x] **20 request song song `POST /v1/carts/merge` cùng guest token ⇒ giỏ user cộng đúng 1 lần** (không nhân 20) —
      chứng minh Lua cộng dồn + `DEL` giỏ guest là một bước nguyên tử.
- [x] **20 request song song `PUT` khác qty cùng SKU ⇒ giá trị cuối là một trong các giá trị đã gửi** và dữ liệu
      không hỏng (JSON đọc lại được) — không có ghi xen kẽ.
- [x] TTL luôn > 0 sau mỗi lần ghi (Redis `TTL` kiểm thật).
- [x] `POST /v1/carts/merge` (JWT + `X-Cart-Token`): cộng dồn qty theo SKU, **cap 999**, xoá giỏ guest; merge lần 2 → giỏ
      guest trống nên **không cộng lần nữa** (idempotent theo dữ liệu).
- [x] `DELETE /v1/carts` xoá cả giỏ; `GET` sau đó trả giỏ rỗng (không 404 — giỏ rỗng là trạng thái hợp lệ).
- [x] Unit + IT xanh; `e2e-smoke` 17/17; OpenAPI export pass.

**Ghi chú kỹ thuật**

- **Vì sao Lua, không phải GET-then-SET**: value của field là JSON (`qty` + `addedAt`) nên `HINCRBY` không dùng được. Đọc ở
  Java rồi ghi lại = hai lệnh rời ⇒ hai request song song đọc cùng giá trị cũ và cái sau ghi đè cái trước (**lost update**).
  Lua chạy trong Redis nên cả read+write là một bước.
- **Guest token do server cấp**: nếu để client tự sinh, bất kỳ ai đoán/giữ token cũ đều đọc được giỏ người khác và không có
  cách thu hồi. Token server cấp còn cho phép đặt TTL + rate limit sau này.
- **`/v1/carts/**` là public ở gateway** (guest không có JWT) ⇒ authorization nằm ở cart-service: có JWT thì dùng giỏ user,
  không có thì **bắt buộc** token hợp lệ. Cùng pattern defense-in-depth với public GET của catalog.
- **Bất biến `1..999` kiểm trong Lua**, không kiểm ở Java rồi ghi — kiểm ngoài là TOCTOU (đã đúng lúc đọc, sai lúc ghi).
- Không có Postgres ⇒ **không** datasource, **không** Flyway; readiness check TCP tới Redis.

**Kết quả verify** *(chạy thật: Docker + Redis 7)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (03:12)**

```text
[INFO] ShopLab :: Order Service ........................... SUCCESS [ 17.933 s]
[INFO] ShopLab :: Payment Service ......................... SUCCESS [ 18.654 s]
[INFO] ShopLab :: Cart Service ............................ SUCCESS [ 16.218 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  8.756 s]
[INFO] BUILD SUCCESS   Total time: 03:12 min
```

`cart` unit **34** (`CartServiceTest` 8 · `OwnerResolverTest` 4 · config/health 22) · IT **6** (Redis Testcontainers,
gồm 2 test đồng thời) · jacoco `config/*`+`health/*` **80.8 %** (gate 80 %).

**2) Luồng thật qua gateway** (`bash /tmp/verify-cart.sh`)

```text
 1) cấp guest token (public)        HTTP 201 · expiresIn=2592000s (30 ngày)
 2) xem giỏ guest                   HTTP 200 · owner=g:2199951b-… · items=0 · ttl=0
 3) thêm SKU A qty=2                HTTP 200 · totalQty=2 · ttl=2592000s (TTL làm mới khi ghi)
 4) PUT lại cùng qty (idempotent)   HTTP 200 · totalQty=2 (KHÔNG nhân đôi)
 5) qty=0 → 422 · qty=1000 → 422
 6) thêm SKU B qty=3                totalQty=5
 7) không token → 401 · token bịa → 401
 8) giỏ user (JWT)                  owner=u:04bbc511-… · qty SKU A trước merge = 1
 9) merge                           HTTP 200 · qty SKU A = 3  (1 + 2, cộng dồn)
10) guest token sau merge           HTTP 401 (bị thu hồi)
11) merge lần 2                     HTTP 401 ⇒ không cộng lần nữa
12) **10 merge SONG SONG cùng token** {200: 2, 401: 8} ⇒ qty SKU C = **5**, không phải 50
13) xoá 1 dòng → 200 · 14) xoá cả giỏ → 200 · totalQty=0 (giỏ rỗng là hợp lệ)
```

**3) Kiểm trực tiếp Redis**

```text
$ TYPE cart:g:8cecc7a2-…   → hash
$ HLEN cart:g:8cecc7a2-…   → 1
$ TTL  cart:g:8cecc7a2-…   → 2592000   (= 30 ngày)
$ TTL  cart:guest:8cecc7a2-… → 2592000  (marker token do server cấp)
$ HGETALL cart:g:8cecc7a2-… → {"addedAt":"2026-11-07T15:01:47.224819732Z","qty":4}
$ curl /ready → {"status":"UP","dependencies":[{"name":"redis","status":"UP","latency":"PT0.0048S"}]}
```

**4) Regression + contract**

```text
$ bash tools/e2e-smoke.sh        17 PASS / 0 FAIL · 13 request · 744 ms
$ bash tools/export-openapi.sh   VALIDATE PASS · 36 path · 45 operation · 41 schema  (trước: 32/39/37)
```

**Sự cố trong lúc verify (2 lỗi thật)**

- **Bẫy copy-service lần 3**: `services/cart-service/Dockerfile` copy từ payment rồi `sed payment-service → cart-service`
  ⇒ **mất** dòng `COPY services/payment-service/pom.xml` ⇒ `docker build` sẽ chết với "Child module does not exist". Phát
  hiện bằng cách grep danh sách `COPY services` thay vì tin vào sed.
- **OpenAPI trùng schema `Line` lần 2**: cart có `CartResponse.Line`, order (từ `SL-2.5`) đã chiếm tên `Line` ⇒ script merge
  từ chối export. Đổi thành `LineItem`. Bài học: **đặt tên DTO lồng theo domain**, đừng đặt tên chung chung.

**Learning note**

- **Khi value là JSON thì `HINCRBY` vô dụng — phải Lua**: đọc ở Java rồi ghi lại là *lost update* thật (khách bấm "+" 2 lần
  nhanh sẽ mất 1 lần). Lua chạy trong Redis nên read-modify-write là một bước; bất biến (`qty 1..999`, `maxLines`) cũng
  phải kiểm **trong script** — kiểm ở Java là TOCTOU (đúng lúc đọc, sai lúc ghi).
- **Guest token phải do server cấp + có marker**: nếu client tự sinh, ai giữ token cũ vẫn đọc được giỏ người khác và không
  có cách thu hồi. Marker `cart:guest:<token>` là thứ duy nhất phân biệt "token thật" với "token bịa" mà không cần bảng DB.
- **Merge phải nguyên tử cả hai vế** (cộng dồn **và** xoá giỏ guest): tách 2 bước thì 10 request song song cộng 10 lần.
  Verify thật cho đúng kết quả đó: `{200: 2, 401: 8}` ⇒ qty 5 (không phải 50).
- **Public path ở gateway ⇒ authorization chuyển vào service**: gateway chỉ biết public theo *path*, mà `/v1/carts/**` vừa
  có guest vừa có user ⇒ cart phải tự phân xử (JWT thắng, không JWT thì bắt buộc token hợp lệ). Đánh đổi đã ghi: gateway
  không còn là chốt 401 cho prefix này.
- **Service không có Postgres là chuyện bình thường nếu dữ liệu chịu mất**: cart bỏ Flyway/datasource, readiness chỉ check
  Redis. Ngược lại, không có DB nghĩa là **không có ràng buộc DB canh bất biến** ⇒ phải dồn hết vào Lua và test đồng thời.
### SL-2.13 — `catalog-service`: `GET /v1/skus?ids=` (batch giá + tên SKU)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow *(phát sinh từ `SL-2.6`, chốt phương án **B**: saga đọc cart + giá từ catalog)* |
| **Phụ thuộc** | `SL-2.11` (SKU `ARCHIVED` không được bán); **chặn** `SL-2.6a` (saga cần giá thật để tính tiền) |
| **Ước lượng** | S |

**Bối cảnh**

Saga checkout phải biết **giá thật** của từng dòng để tính subtotal → lock voucher → tạo payment intent. Nhưng
`cart-service` cố ý **không** lưu giá (`TD-20`), còn `catalog` chỉ có 2 đường đọc: `GET /v1/products` (list) và
`GET /v1/products/{id}` (kèm SKU của **một** product). **Không có đường đọc theo `skuId`** ⇒ hoặc order-service phải tin
giá do client gửi, hoặc gọi N lần cho giỏ N dòng khác product.

Đây là chỗ chặn thật của `SL-2.6a`: phương án B (saga đọc cart + giá từ catalog) chỉ khả thi khi có endpoint tra **nhiều
SKU trong 1 request**.

**Mục tiêu**

`GET /v1/skus?ids=a,b,c` trả giá bán thật + tên + mã SKU cho tối đa 100 SKU trong **1 request**, và nói rõ SKU nào
**không mua được** (không tồn tại / đã archive).

**In scope**

- Endpoint public `GET /v1/skus?ids=<uuid>,<uuid>...` (giá là thông tin công khai — guest xem giỏ cũng cần, giống
  `GET /v1/products`).
- `SkuLookupQuery`: parse + validate `ids` (bắt buộc, ≤ 100, UUID hợp lệ, **dedupe**, giữ thứ tự client gửi).
- `SkuLookupService`: `unitPrice = sku.priceOverride ?? product.price`, `title = product.name`; **loại** SKU `ARCHIVED` và
  SKU của product đã xoá mềm.
- Response `{items[], missingIds[]}` — caller biết chính xác SKU nào không mua được (để trả 409/422 đúng chỗ).
- Gateway: route `/v1/skus` → `CATALOG_SERVICE_URL` + public-path; catalog `SecurityConfig` mở `GET /v1/skus`.
- Test: unit (query parse + service) · IT (HTTP thật + Postgres). Export lại `docs/api/openapi.yaml`.

**Out of scope**

- Không trả tồn kho (inventory là nguồn sự thật duy nhất), không biến `stockHint` thành tồn thật.
- Không sửa `cart-service` để lưu giá (giữ `TD-20` — UI gọi endpoint này khi cần hiển thị giá, việc đó ở `SL-2.8`).
- Không thêm `currency`: catalog Phase 2 chưa có khái niệm tiền tệ, `order-service` mặc định `VND`
  (`CreateOrderRequest.currencyOrDefault()`). Thêm khi làm multi-currency.
- Không cache Redis (cache-aside để Phase 4).

**Acceptance Criteria**

- [ ] `GET /v1/skus?ids=<2 SKU hợp lệ>` → **200**, `items` có `skuId/productId/skuCode/title/unitPrice`; giá =
      `price_override` nếu có, ngược lại = giá product.
- [ ] SKU `ARCHIVED` (product bị xoá mềm) → **không** có trong `items`, **có** trong `missingIds`.
- [ ] `skuId` không tồn tại → `missingIds` (không 500, không 404 cả request).
- [ ] `ids` trùng lặp → 1 dòng/SKU, thứ tự = thứ tự client gửi.
- [ ] Thiếu `ids` / rỗng / > 100 id / id sai định dạng UUID → **400** `VALIDATION_ERROR` nêu rõ field.
- [ ] Public: không token vẫn **200**; qua gateway cũng 200.
- [ ] Unit + IT xanh; `mvn -B -ntp verify` toàn repo **BUILD SUCCESS**; `e2e-smoke` không regress.

**Ghi chú kỹ thuật**

- **Batch thay vì N+1**: giỏ 10 dòng khác product = 10 round-trip nếu không có endpoint này; cộng vào p99 checkout
  (< 500 ms theo NFR) thì đó là chi phí thật, không phải tối ưu sớm.
- **2 câu query, không join**: `SkuRepository.findByIdInAndStatus` rồi `ProductRepository.findByIdInAndDeletedAtIsNull`
  — giữ nguyên tắc "mỗi repository một aggregate", và cả hai đường đều đi index sẵn có (PK + `ix_sku_product_id`).
- **`missingIds` thay vì 404 cả request**: một SKU chết không nên làm hỏng cả giỏ; caller cần biết *cái nào* để báo user.
- **Giá = `priceOverride ?? product.price`**: đúng bất biến của `Sku` ("null = dùng giá product", cố ý không copy giá) ⇒
  không sinh nguồn sự thật thứ hai.
- **Không trả `status`**: endpoint chỉ trả SKU **mua được**; trả thêm trạng thái là mời client tự quyết điều server đã
  quyết.

**Kết quả verify** *(chạy thật 2026-11-08 — Docker compose 10 container healthy, không phải suy luận)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS (10 module)**

```text
[INFO] ShopLab :: Service Commons ......................... SUCCESS [  2.278 s]
[INFO] ShopLab :: Service Template ........................ SUCCESS [  6.872 s]
[INFO] ShopLab :: Identity Service ........................ SUCCESS [ 19.790 s]
[INFO] ShopLab :: Catalog Service ......................... SUCCESS [ 16.297 s]
[INFO] ShopLab :: Inventory Service ....................... SUCCESS [ 16.240 s]
[INFO] ShopLab :: Promotion Service ....................... SUCCESS [ 15.889 s]
[INFO] ShopLab :: Order Service ........................... SUCCESS [ 13.850 s]
[INFO] ShopLab :: Payment Service ......................... SUCCESS [ 13.983 s]
[INFO] ShopLab :: Cart Service ............................ SUCCESS [ 16.821 s]
[INFO] ShopLab :: API Gateway ............................. SUCCESS [  7.986 s]
[INFO] BUILD SUCCESS
```

`catalog` unit **79** (+13: `SkuLookupQueryTest` 7 · `SkuLookupServiceTest` 6) · IT **25** (+4) · checkstyle **0 violation**,
jacoco gate pass.

**2) Batch giá thật qua gateway, KHÔNG token** (`bash /tmp/verify-sku.sh`)

```text
== 1) Batch 2 SKU (public, KHÔNG token) qua gateway ==
{"items":[
   {"skuId":"0ca7ab7a-…","productId":"d2717762-…","skuCode":"BATCH-A-1794109819",
    "title":"Ao batch SL-2.13","unitPrice":150000.00},      ← SKU có priceOverride
   {"skuId":"21bbfc8f-…","skuCode":"BATCH-B-1794109819","unitPrice":180000.00}],  ← giá product
 "missingIds":[]}
   HTTP 200 · 0.031111s
== 2) Gọi THẲNG catalog :8082, không token (service tự mở, không nhờ gateway) ==
   HTTP 200
== 3) Trùng lặp + id không tồn tại ⇒ items không nhân bản, missingIds đúng ==
{"items":[{"skuId":"0ca7ab7a-…"}],"missingIds":["56050635-8262-4c86-83f2-fdf5f6f468ef"]}
   HTTP 200
== 4) Xoá mềm product ⇒ SKU vào missingIds (HTTP vẫn 200) ==
   DELETE product → HTTP 204
{"items":[],"missingIds":["0ca7ab7a-…","21bbfc8f-…"]}
   HTTP 200
== 5) Validate → 400 ==
   thiếu ids      → HTTP 400 ids là bắt buộc, dạng ?ids=<uuid>,<uuid>
   sai UUID       → HTTP 400 ids phải là UUID, không đọc được: khong-phai-uuid
   101 id         → HTTP 400 ids tối đa 100 phần tử, nhận: 101
== 6) 100 id (biên trên) ⇒ HTTP 200
```

**3) Regression + contract**

```text
$ bash tools/e2e-smoke.sh       17 PASS / 0 FAIL · 13 request · 669 ms
$ bash tools/export-openapi.sh  VALIDATE PASS · 37 path · 46 operation · 43 schema  (trước: 36 path)
```

**Sự cố trong lúc verify (1 lỗi thật — không thuộc ticket này)**

- **CI run `42444551383` đỏ** ở `inventory-service` IT `commitAfterExpiryReturnsGone` — **không phải** do thay đổi của
  `SL-2.13` (catalog/gateway): test đẩy `expires_at` về quá khứ rồi gọi `commit`, nhưng IT cấu hình sweeper chạy mỗi
  **300 ms** nên trên runner chậm sweeper expire trước ⇒ `commit` trả **409** thay vì **410**. Local xanh, CI đỏ ⇒
  **flaky test**, ghi `TD-23` (không sửa trong ticket này — `AGENTS.md` §3.1: sửa code ngoài scope phải là ticket riêng).

**Learning note**

- **Endpoint batch phải nằm ở service sở hữu dữ liệu, không phải ở nơi cần dùng**: nếu `order-service` tự cache giá thì
  nó phải có job làm mới + tự xử lý "giá đã đổi" — tức dựng nguồn sự thật thứ hai. `catalog` trả lời 1 câu, đúng chỗ.
- **`missingIds` rẻ hơn và đúng hơn 404**: một SKU chết không nên làm hỏng cả giỏ; 404 cả request buộc caller phải
  tự đoán *cái nào* hỏng (hoặc gọi lại từng id ⇒ mất luôn ý nghĩa batch).
- **Giá là `priceOverride ?? product.price`, không copy**: đúng bất biến của `Sku` — null nghĩa là "dùng giá product".
  Copy giá sang SKU sẽ tạo 2 nguồn sự thật cho 1 con số, kiểu gì cũng lệch.
- **Tên DTO lồng phải dài, đừng đặt `Item`/`Line`**: spec OpenAPI merge toàn hệ; `Item` đã bị inventory chiếm, `Line` bị
  order chiếm (sự cố thật ở `SL-2.5`/`SL-2.1`) ⇒ dùng `SkuLine`.
- **`String.valueOf` trên số JSON là bẫy**: `ObjectMapper.readValue(..., Map.class)` cho `Double` ⇒ `150000.00` đọc ra
  `"150000.0"` và assertion kiểu chuỗi fail dù API trả đúng. So sánh theo `Number.doubleValue()`.
- **Đếm phần tử trước khi dedupe**: gửi 500 id trùng nhau vẫn là 500 phần tử phải parse — chặn `> MAX_IDS` ở bước dedupe
  là chặn sai chỗ (client lách được bằng cách lặp id).
- **Mở public + batch 100 id = bề mặt tấn công mới** ⇒ ghi `TD-22` (chưa rate limit, chưa cache) thay vì coi như xong.


### SL-2.6a — Checkout Saga **start**: `POST /v1/checkout` (orchestration + saga state + compensating)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow *(tách từ `SL-2.6` — `L` → 2 × `M`)* |
| **Phụ thuộc** | `SL-2.1` (đọc giỏ) · `SL-2.13` (**batch giá SKU**) · `SL-2.2`/`SL-2.3` (reserve) · `SL-2.4` (lock voucher) · `SL-2.5` (order) · `SL-2.7` (payment intent); **chặn** `SL-2.6b`, `SL-2.8`, `SL-2.9` |
| **Ước lượng** | M |

**Bối cảnh**

5 service mảnh đã có nhưng **chưa ai nối chúng lại**: hiện client phải tự gọi lock voucher → reserve → tạo order → tạo
payment intent và tự bù khi fail. Đó là orchestration đặt sai chỗ (client không thể biết cần release cái gì), và là lý do
`order-service` được giao vai **orchestrator** (architecture.md §5, §6.1).

**Mục tiêu**

`POST /v1/checkout` (`Idempotency-Key`) trong `order-service` chạy trọn bước **start**: đọc giỏ → lấy giá thật → lock
voucher → reserve stock → tạo order `PENDING_PAYMENT` → tạo payment intent; lưu **saga state**; fail ở bất kỳ bước nào thì
**bù** (release những gì đã giữ) trước khi trả lỗi.

**In scope**

- `RestClient` (forward `Authorization`, timeout connect/read cấu hình được) + `SagaProperties` (URL 3 service).
- Migration `V2__create_checkout_saga.sql`: `checkout_sagas` (`order_id` PK, `user_id`, `state`, `lock_id`,
  `reservation_id`, `payment_intent_id`, `failure_code`, timestamps) + `checkout_saga_steps` (audit từng bước: tên, trạng
  thái, http status, thời gian).
- `POST /v1/checkout` + `GET /v1/checkout/{orderId}` (trạng thái saga) · gateway route `/v1/checkout`.
- Compensating: reserve fail ⇒ release voucher; tạo order fail ⇒ release stock + voucher; payment fail ⇒ release stock +
  voucher + order `CANCELLED`.
- Test: unit (bảng compensating từng nhánh) · IT (Postgres thật cho saga state + 10 request song song cùng key ⇒ 1 saga).

**Out of scope**

- Confirm/cancel/sweeper hết hạn → **`SL-2.6b`**; test nhánh fail sâu (fault injection: timeout, 500) → `SL-2.9`.
- Không retry/circuit breaker (Phase 5) — Phase 2 chỉ **fail-fast + timeout**.
- Không outbox/event (Phase 3).

**Acceptance Criteria**

- [x] `POST /v1/checkout` (body chỉ có `voucherCode` **tuỳ chọn** — dòng hàng lấy từ giỏ) đọc giỏ của user từ
      `cart-service`; giỏ rỗng → **422**.
- [x] Happy path → **201** `{orderId, paymentIntentId, paymentUrl, status: PENDING_PAYMENT}`; `checkout_sagas.state =
      COMPLETED_START`; `checkout_saga_steps` ghi đủ **6 bước** (`CART · PRICE · VOUCHER_LOCK · STOCK_RESERVE ·
      ORDER_CREATE · PAYMENT_INTENT` — con số "4 bước" ban đầu đếm thiếu bước giá và bước ghi order).
- [x] **Đơn 0 đồng** (voucher giảm 100%, chốt phương án **B**): không tạo payment intent → **201** với
      `paymentRequired=false`, `paymentIntentId=null`; bước `PAYMENT_INTENT` = `SKIPPED`; `checkout_sagas.payment_required
      = false` để `SL-2.6b` chốt đơn mà không cần verify payment.
- [x] Giỏ có SKU đã archive/không tồn tại → **409** + message nêu `skuId`; **không** tạo order, **không** giữ chỗ voucher.
- [x] Hết hàng ở bước reserve → **409**; voucher đã lock được **release** (DB promotion: `status = RELEASED`).
- [x] Payment-service lỗi/timeout ⇒ release stock + voucher, order `CANCELLED`, trả **503**; saga ghi `FAILED_PAYMENT` +
      `failure_code`.
- [x] Retry cùng `Idempotency-Key` ⇒ **200** + cùng `orderId` (không tạo order/intent thứ hai).
- [x] 10 request song song cùng key ⇒ **1** order, **1** reservation, **1** voucher lock.
- [x] Unit + IT xanh; OpenAPI + `docs/api/openapi.yaml` cập nhật; `e2e-smoke` không regress.

**Ghi chú kỹ thuật**

- **Saga state ở `order-service`** (không phải service riêng): nó là orchestrator và đã sở hữu `orders`; thêm 1 service
  saga ở Phase 2 là over-engineer (§3.4).
- **Ghi saga row TRƯỚC khi gọi service ngoài** (`STARTED`) rồi update sau mỗi bước: crash giữa chừng vẫn biết đã giữ gì để
  bù — nếu chỉ ghi sau khi xong thì compensating không có dữ liệu.
- **Compensating gọi lại endpoint idempotent** của service đích (`release` idempotent) ⇒ bù 2 lần không phá dữ liệu.
- **Forward JWT của user** cho service đích (chúng tự verify — defense in depth); service token để Phase 3.
- **`orderId` sinh trước bước reserve** vì nó là khoá idempotency của inventory/promotion.

**Kết quả verify** *(chạy thật 2026-11-08 — compose 10 container healthy + Postgres thật)*

**1) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS, checkstyle 0 violation**

`order` unit **55** (+12: `CheckoutServiceTest` 6 · `SagaPropertiesTest` 4 · +2 cũ) · IT **18** (+8 `CheckoutSagaIT`) ·
jacoco gate pass.

**2) Luồng thật qua gateway** (`bash /tmp/verify-checkout.sh`)

```text
== 1) Buyer thêm 2 món vào giỏ ==
   PUT cart → HTTP 200
== 2) POST /v1/checkout (Idempotency-Key + voucher 20k) ==
   HTTP 201 · {"orderId":"b3c4ff2a-…","status":"PENDING_PAYMENT","subtotal":240000.00,
              "discount":20000.00,"total":220000.00,"currency":"VND","paymentRequired":true,
              "paymentIntentId":"ee6be4f3-…",
              "paymentUrl":"http://localhost:8080/mock-provider/pay/pi_mock_ee6be4f3-…"}
== 3) GET /v1/checkout/{orderId} ==
{"orderId":"b3c4ff2a-…","state":"COMPLETED_START","paymentRequired":true,
 "lockId":"cfbc5ddb-…","reservationId":"d8e96dc0-…","paymentIntentId":"ee6be4f3-…", …}   HTTP 200
== 4) DB thật ==
   orders:    PENDING_PAYMENT total=220000.00 intent=ee6be4f3-…
   saga:      COMPLETED_START payment_required=true
   steps:     CART:OK → PRICE:OK → VOUCHER_LOCK:OK → STOCK_RESERVE:OK → ORDER_CREATE:OK → PAYMENT_INTENT:OK
   inventory: on_hand=10 reserved=2
   voucher:   LOCKED
== 5) Retry cùng key → replay, không tạo đơn thứ 2 ==
   HTTP 200 · Idempotent-Replayed: true · orderId trùng: YES
== 6) Hết hàng ở reserve → 409 + voucher RELEASE ==
   HTTP 409 · {"code":"CONFLICT","message":"[inventory] Không đủ hàng cho SKU d28ad316-…: còn 0, cần 1"}
   voucher vừa lock: RELEASED      · saga vừa rồi: FAILED_STOCK
== 7) Giỏ rỗng → 422 · UNPROCESSABLE_ENTITY        == 8) Thiếu Idempotency-Key → 400
```

**3) Regression + contract**

```text
$ bash tools/e2e-smoke.sh       17 PASS / 0 FAIL · 13 request · 745 ms
$ bash tools/export-openapi.sh  VALIDATE PASS · 39 path · 48 operation · 46 schema  (trước: 37 path)
```

**Sự cố trong lúc verify (3 lỗi thật, đáng ghi)**

- **Tên field sai khi gọi inventory**: gửi `{"orderId":…,"lines":[…]}` nhưng `ReserveRequest` của inventory là
  `items` ⇒ inventory trả `400 VALIDATION_ERROR`, saga bù đúng (release voucher, `FAILED_STOCK`) nhưng happy path chết.
  **IT stub không bắt được** vì stub nhận mọi body. Fix: đổi sang `items` **và** stub giờ trả 400 nếu body thiếu
  `items` — test phải khó tính ngang service thật.
- **Boot 4 không có bean `RestClient.Builder`** (tách module http-client) ⇒ context fail
  `required a bean of type RestClient$Builder`. Fix: tự dựng `RestClient.builder()` trong gateway, không kéo thêm
  starter chỉ để lấy 1 bean.
- **Service đích trả 5xx mà ta trả 500**: `ErrorCode.fromHttpStatus(500)` map sang `INTERNAL_ERROR`. Nhưng 5xx của
  *dependency* là lỗi hạ tầng **của hệ**, không phải của request ⇒ đổi sang **503** (`UNAVAILABLE`) để client biết
  "thử lại có ích". Phát hiện bằng IT (`expected 503 but was 500`).

**Learning note**

- **Compensating phải đọc state bền, không đọc biến trong RAM**: saga ghi `lock_id`/`reservation_id` xuống DB **ngay sau
  mỗi bước**, nên hàm bù chỉ cần `select` lại là biết phải trả gì. Nếu giữ trong biến local thì crash giữa chừng là mất
  dấu, và hàng/voucher treo tới hết TTL.
- **Fail thì trả khoá idempotency, đừng để `IN_PROGRESS`**: job dọn chỉ chạy mỗi giờ ⇒ để nguyên nghĩa là user vừa
  "hết hàng", sửa giỏ, bấm lại **vẫn** 409 trong 1 giờ. Đánh đổi đã ghi: mất chốt "cùng key khác payload" cho lần fail.
- **"Test xanh" chỉ đúng bằng mức khó tính của stub**: stub nhận mọi body ⇒ sai tên field vẫn pass. Stub phải **validate
  tối thiểu** như service thật (đây là lỗi đã gặp, không phải giả định).
- **Đơn 0 đồng phải là bất biến của saga, không phải suy ra từ `orders.total`**: cột `payment_required` nói thẳng ý định
  cho `SL-2.6b`; suy ra từ số tiền thì mỗi chỗ đọc phải nhớ quy ước "total = 0 nghĩa là không cần thanh toán".
- **5xx của dependency ≠ 500 của mình**: map sang 503. Client retry theo 503, còn 500 thì client coi là bug của ta.
- **Sync saga = p99 là tổng 4 round-trip**: đo được ~**0,42 s** cho cả saga trên máy dev (log `updatedAt - createdAt` của
  saga). Đây chính là baseline để Phase 3 (event) và Phase 5 (circuit breaker) so sánh.



| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow *(tách từ `SL-2.6`)* |
| **Phụ thuộc** | `SL-2.6a` (saga state + HTTP client tới service khác); **chặn** `SL-2.8`/`SL-2.9` |
| **Ước lượng** | M |

**Bối cảnh**

Sau `POST /v1/checkout`, hàng và voucher mới chỉ **giữ chỗ**. Tiền về qua webhook provider, nhưng Phase 2 chưa có broker
nên `payment-service` **không** gọi ngược `order-service` (`TD-17`) ⇒ phải có đường do saga điều khiển để chốt (`commit`)
hoặc bù (`release`) khi user bỏ ngang.

**Mục tiêu**

Hoàn tất vòng đời checkout: `confirm` (tiền đã về ⇒ commit stock + voucher ⇒ order `CONFIRMED`), `cancel` (bù ngay), và
**sweeper** dọn saga quá hạn 15′ (⇒ `EXPIRED` + release) — tất cả **idempotent** theo saga state.

**In scope**

- `POST /v1/checkout/{orderId}/confirm` — **xác minh với `payment-service`** rằng intent `SUCCEEDED` **rồi** mới `commit`
  stock + voucher + order `CONFIRMED`; gọi lại ⇒ **200**, không commit lần 2.
- `POST /v1/checkout/{orderId}/cancel` — release stock + voucher + order `CANCELLED` (idempotent).
- `@Scheduled` sweeper: saga đã start, order còn `PENDING_PAYMENT`, quá `payment-window` (15′) ⇒ release + `EXPIRED`;
  `FOR UPDATE SKIP LOCKED` + conditional update (khuôn `SL-2.3`) để nhiều instance không bù 2 lần.
- Test: unit (state machine saga + sweeper) · IT (confirm 2 lần ⇒ 1 commit; sweep ⇒ `EXPIRED` + stock về).

**Out of scope**

- Refund khi tiền đã vào mà đơn hết hạn → `TD-17` (Phase 6 mới có `refunds`).
- Test nhánh fail đầy đủ (payment fail, timeout, hết hàng giữa chừng) → `SL-2.9`.

**Acceptance Criteria**

- [x] `confirm` khi payment `SUCCEEDED` ⇒ **200**, order `CONFIRMED`, reservation `COMMITTED`, voucher lock `COMMITTED`.
- [x] `confirm` trên saga có `payment_required = false` (đơn 0 đồng, chốt ở `SL-2.6a`) ⇒ **200** và commit bình thường
      **không** cần gọi payment-service (không có intent để verify).
- [x] `confirm` khi payment **chưa** `SUCCEEDED` ⇒ **409**, **không** commit gì (không tin lời client).
- [x] `confirm` lần 2 ⇒ **200**, `stock_ledger`/voucher **không** thêm dòng commit thứ hai.
- [x] `cancel` ⇒ reservation/voucher `RELEASED`, order `CANCELLED`; `cancel` sau `confirm` ⇒ **409**.
- [x] Sweeper: saga quá 15′ ⇒ order `EXPIRED`, voucher/hàng được **TTL sweeper** của inventory/promotion trả (chốt
      phương án **A** — sweeper không có token user nên không gọi `release`); chạy 2 vòng không bù 2 lần.
- [x] Unit + IT xanh; `e2e-smoke` không regress.

**Ghi chú kỹ thuật**

- **Verify payment ở server, không tin client**: client gọi `/confirm` sau khi thanh toán, nhưng "đã trả tiền" phải đọc từ
  `payment-service` — nếu không, ai cũng confirm được đơn mà không trả đồng nào.
- **Commit theo thứ tự stock → voucher**: stock là tài nguyên khan hiếm (hết hàng = mất đơn), voucher sai thì sửa được.
- **Idempotent theo trạng thái saga**, không theo `Idempotency-Key`: confirm/cancel là thao tác trên **cùng** một đơn nên
  khoá tự nhiên là `orderId` + state hiện tại.

**Kết quả verify** *(chạy thật 2026-11-08 — compose 10 container healthy, Postgres thật)*

**1) `mvn -B -ntp verify` — BUILD SUCCESS, checkstyle 0**

`order` unit **65** (+12: `CheckoutFinishServiceTest` 8 · `CheckoutSagaSweeperTest` 2 · `SagaPropertiesTest` +2) ·
IT **25** (+7 `CheckoutSagaIT`) · jacoco gate pass.

**2) Luồng thật qua gateway** (`bash /tmp/verify-checkout-finish.sh`)

```text
== 1) confirm TRƯỚC khi trả tiền ==
   order=2a6d1c1a-… · paymentUrl=http://localhost:8080/mock-provider/pay/pi_mock_10750d6e-…
   confirm → HTTP 409 · code CONFLICT
== 2) mock-pay rồi confirm ==
   mock-pay → HTTP 200 · confirm → HTTP 200 · state CONFIRMED
   DB order:   CONFIRMED
   inventory:  on_hand=8 reserved=0                  ← 10 − 2 (commit thật)
   voucher:    COMMITTED
   steps:      CART:OK → PRICE:OK → VOUCHER_LOCK:OK → STOCK_RESERVE:OK → ORDER_CREATE:OK
               → PAYMENT_INTENT:OK → PAYMENT_VERIFY:FAILED → PAYMENT_VERIFY:OK
               → STOCK_COMMIT:OK → VOUCHER_COMMIT:OK
   history:    ∅→PENDING_PAYMENT | PENDING_PAYMENT→CONFIRMED
== 3) confirm#2 → 200 · history vẫn 2 dòng · cancel sau confirm → 409
== 4) cancel đơn 2 → 200 CANCELLED · reservation RELEASED · voucher RELEASED · cancel#2 → 200
== 5) Đơn 3 bỏ ngang (payment-window 20s trong .env để demo):
   sau 30s:  order EXPIRED · saga EXPIRED/PAYMENT_WINDOW_EXCEEDED · reserved=2 (sweeper KHÔNG gọi release)
   confirm sau expire → 409
   sau TTL 45s: on_hand=8 reserved=0 · voucher EXPIRED   ← TTL sweeper của inventory/promotion trả
```

**3) Regression + contract**

```text
$ bash tools/e2e-smoke.sh       17 PASS / 0 FAIL · 13 request · 949 ms
$ bash tools/export-openapi.sh  VALIDATE PASS · 41 path · 50 operation  (trước: 39 path)
```

**Sự cố trong lúc verify (2 lỗi thật)**

- **`@ConfigurationProperties` record chỉ được có MỘT constructor**: thêm constructor "tiện cho test" ⇒ Boot báo
  `No default constructor found` và **context chết**. Fix: bỏ constructor phụ, đưa helper `defaults()` vào test.
- **Checkstyle bắt `switch` thiếu `default`** dù đã cover hết enum (`MissingSwitchDefault`): thêm `default -> throw`
  (đúng ý: trạng thái lạ phải nổ, không im lặng bỏ qua).

**Learning note**

- **Tách bước finish ra service riêng thay vì nhồi vào `CheckoutService`**: start và finish có bất biến khác nhau (start
  *bù* khi fail; finish *không* bù vì `commit` đã xong thì "un-commit" vô nghĩa). Một class làm cả hai sẽ phải rẽ nhánh
  theo "đang ở pha nào" — đúng loại state ngầm dễ sai.
- **Không cần state "đang xử lý" khi service đích đã idempotent**: `commit`/`release` gọi 2 lần vẫn 200, nên 2 request
  confirm/cancel đồng thời không phá dữ liệu; chốt chặn thật là **compare-and-set trên `orders`** (1 lần chuyển, 1 dòng
  history). Thêm `CANCELLING`/`EXPIRING` chỉ làm state machine phình ra mà không thêm bảo vệ nào.
- **Verify payment ở server, luôn luôn**: client gọi `/confirm` sau khi thanh toán nhưng "đã trả tiền" phải đọc từ
  `payment-service`. Ca `confirm` trước khi trả tiền trả **409** — nếu tin client thì ai cũng có đơn free.
- **Sweeper không có token ⇒ đừng cố gọi API cần token**: chọn "chỉ ghi DB + để TTL sweeper của 2 service kia trả hàng"
  (đã chứng minh bằng số: sau 45 s `reserved 2 → 0`, voucher `EXPIRED`). Đánh đổi ghi vào `TD-25`, và khi Phase 3 có
  service-to-service auth thì sweeper gọi `release` ngay để không phải chờ TTL.
- **`payment-window` phải bằng TTL của reservation/lock** (15′): dài hơn ⇒ tồn tại cửa sổ "đơn đã `EXPIRED` mà tiền vẫn
  vào được"; ngắn hơn ⇒ khách đang trả tiền thì đơn bị expire. Cấu hình này là **ràng buộc chéo service**, ghi rõ trong
  `application.yml` để người sau không sửa lẻ.

### SL-2.6b — Checkout Saga **hoàn tất**: confirm · cancel · sweeper hết hạn

> ⚠️ **Section viết bổ sung khi archive (2026-11-08)** — ticket này `Done` mà **không có đặc tả riêng** trong
> `Backlog.md` (chỉ được nhắc trong `SL-2.6a`/`SL-2.8`/`SL-2.9`). Nội dung dưới đây dựng lại từ commit `5da0cc6`
> (message có bảng verify đầy đủ) + Changelog + learning note — **không** bịa thêm số liệu.

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow *(tách từ `SL-2.6`)* |
| **Phụ thuộc** | `SL-2.6a` (saga state + HTTP client) · `SL-2.7` (payment intent) · `SL-2.2`/`SL-2.4` (commit/release); **chặn** `SL-2.8`/`SL-2.9` |
| **Ước lượng** | M |

**Mục tiêu**

Đóng vòng đời checkout sau khi saga start xong: `confirm` (chốt đơn) · `cancel` (bù) · **sweeper** dọn đơn bỏ ngang.

**In scope**

- `POST /v1/checkout/{orderId}/confirm`: verify payment **ở server** (không tin client) → commit stock → commit voucher
  ⇒ order/saga `CONFIRMED`; đơn 0 đồng bỏ qua verify (ADR-0006 **D6**).
- `POST /v1/checkout/{orderId}/cancel`: release stock + voucher ⇒ `CANCELLED`.
- `CheckoutSagaSweeper`: quá `payment-window` 15′ ⇒ order/saga `EXPIRED` (hàng/voucher do **TTL sweeper** của
  inventory/promotion trả — chốt phương án **A**, ghi `TD-25`).
- State saga thêm `CONFIRMED`/`CANCELLED`/`EXPIRED` (migration `V3`); step audit thêm `PAYMENT_VERIFY`/`STOCK_COMMIT`/
  `VOUCHER_COMMIT`/`STOCK_RELEASE`/`VOUCHER_RELEASE`/`EXPIRE`.

**Kết quả verify** *(chạy thật 2026-11-08 — compose 10 container healthy + Postgres thật)*

| Luồng | Kết quả |
|---|---|
| `mvn -B -ntp verify` | **BUILD SUCCESS** — order **65 unit** + **25 IT**, checkstyle **0** |
| confirm **trước** khi trả tiền | **409** `CONFLICT` (không tin client) |
| mock-pay → confirm | **200** · order **CONFIRMED** · `on_hand 10→8`, `reserved 0` · voucher **COMMITTED** · history **2 dòng** |
| steps | `PAYMENT_VERIFY:FAILED → PAYMENT_VERIFY:OK → STOCK_COMMIT:OK → VOUCHER_COMMIT:OK` |
| confirm lần 2 | **200**, history vẫn 2 dòng · cancel sau confirm → **409** |
| cancel | **200** `CANCELLED` · reservation **RELEASED** · voucher **RELEASED** · cancel#2 → **200** |
| bỏ ngang (window 20 s demo) | order **EXPIRED** · saga `EXPIRED/PAYMENT_WINDOW_EXCEEDED` · `reserved` **vẫn 2** (sweeper không gọi release) |
| sau TTL 45 s | `reserved 2→0` · voucher **EXPIRED** ⇒ TTL sweeper của inventory/promotion tự trả (ADR-0006 **D7**) |
| confirm sau expire | **409** |
| `e2e-smoke` / `export-openapi` | **17/17** · **VALIDATE PASS** · 39 → **41 path** |

**Sự cố & learning note**

- Record `@ConfigurationProperties` chỉ được **một** constructor; checkstyle bắt `switch` thiếu `default`.
- **Tách finish khỏi start**: start *bù* khi fail, finish *không* bù (commit xong thì "un-commit" vô nghĩa) — một class
  làm cả hai sẽ phải rẽ nhánh theo pha.
- **Không cần state "đang xử lý"** khi service đích idempotent: chốt chặn thật là compare-and-set trên `orders`.
- **Sweeper không có token user ⇒ đừng gọi API cần token**: để TTL sweeper của inventory/promotion trả hàng (`TD-25`).
- `SAGA_PAYMENT_WINDOW` **phải bằng** TTL reservation/lock, nếu không có cửa sổ "đơn `EXPIRED` mà tiền vẫn vào".


### SL-2.8 — E2E checkout happy path + OpenAPI + CI matrix cho service mới

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.1` · `SL-2.5` · `SL-2.6a` · `SL-2.6b` · `SL-2.7` (đủ 5 mảnh của luồng tiền); **chặn** `SL-2.9`/`SL-2.10` |
| **Ước lượng** | M |

**Bối cảnh**

Tới `SL-2.6b`, từng mảnh đã được verify riêng lẻ, nhưng **chưa có một lệnh nào chứng minh cả luồng tiền chạy end-to-end**
qua cửa vào duy nhất (client → gateway → cart → order saga → catalog/promotion/inventory → payment → confirm). Thiếu nó
thì mỗi lần merge vẫn phải tự dựng lại chuỗi `curl` bằng tay — và người mới không biết bắt đầu từ đâu.

**Mục tiêu**

1. Một script **một lệnh** chạy happy path checkout trên stack thật, assert cả **số tiền** lẫn **trạng thái cuối**.
2. Chứng minh OpenAPI + CI matrix đã phủ đủ service mới (không sửa nếu đã đúng).
3. Trả `TD-4`: smoke/E2E có đường chạy trong CI (không chỉ local).

**In scope**

- `tools/e2e-checkout.sh` (**mới**): register buyer → seller tạo category/product/SKU → admin set stock + voucher →
  cart → `POST /v1/checkout` → `GET /v1/checkout/{id}` → retry cùng `Idempotency-Key` → mock-pay → `confirm` →
  `GET /v1/orders/{id}` → `GET /v1/stock/{skuId}`. PASS/FAIL counter, mỗi bước in gợi ý điều tra.
- `.github/workflows/e2e.yml` (**mới**): `workflow_dispatch` + `schedule` hằng tuần; `docker compose up -d --build --wait`
  rồi chạy `e2e-smoke.sh` + `e2e-checkout.sh`; dump log khi đỏ; `down -v` luôn.
- README: mục E2E + cách chạy; bảng CI workflow.

**Out of scope**

- Không chạy E2E trên **mỗi PR** (lý do chi phí ở Ghi chú kỹ thuật) — `ci.yml` vẫn là hàng rào mỗi PR.
- Không push image lên registry / không cache layer (Phase 7: registry + buildx cache sẽ làm `e2e` rẻ hơn nhiều).
- Nhánh **fail** của checkout (payment fail, hết hàng giữa chừng, timeout) → `SL-2.9`.
- Không đụng `e2e-smoke.sh` (giữ vai trò walking skeleton Phase 1, 17 check ổn định).

**Acceptance Criteria**

- [x] `bash tools/e2e-checkout.sh` (stack local đang chạy) → **27 PASS / 0 FAIL**, exit 0.
- [x] Script assert **tiền**: `subtotal = 2 × 120000`, `total = subtotal − 20000` (voucher), `total` không đổi sau confirm.
- [x] Script assert **trạng thái**: saga `COMPLETED_START` → sau pay + confirm = `CONFIRMED`; order (góc nhìn client)
      `CONFIRMED`; tồn kho `onHand 10 → 8`, `reserved = 0`.
- [x] Script assert **idempotency**: retry cùng key → **200** + cùng `orderId`.
- [x] Script hỏng thì **fail thật**: `exit 1` + gợi ý (`docker compose logs …`); bước sau phụ thuộc bước trước bị skip có
      đếm lỗi (không "xanh giả").
- [x] `bash tools/e2e-smoke.sh` vẫn **17/17** (không regress).
- [x] `tools/export-openapi.sh` → **41 path / 50 operation**, `VALIDATE PASS`.
- [x] `ci.yml` matrix `build-image` **đủ 9 service** (gồm 5 service mới) — đã có từ `SL-1.12`/`SL-2.x`, chỉ verify.
- [x] Workflow `e2e.yml` parse hợp lệ (`python3 yaml.safe_load`), trigger `workflow_dispatch` + `schedule`, không thêm
      chi phí cho PR.

**Ghi chú kỹ thuật**

- **Script riêng thay vì nhồi vào `e2e-smoke.sh`**: smoke là "health check nhanh" của Phase 1 (17 check, ~1 s). Nhồi
  Phase 2 vào đó làm nó chậm và khi đỏ thì không biết hỏng tầng nào; tách ra thì mỗi script một vai trò.
- **`parse_float=str` khi đọc JSON**: `json.load` đổi `220000.00` thành float `220000.0` ⇒ so sánh tiền fail giả. Đây là
  cùng một bẫy với `String.valueOf(double)` đã gặp ở `SL-2.13` — lần này ở phía shell.
- **E2E chạy theo `schedule` + dispatch, không theo PR**: mỗi image build trong CI phải `mvn package` từ đầu (không cache
  Maven giữa job) ⇒ 9 image ≈ 30 phút/run. Gói Free 2.000 phút/tháng ⇒ chạy mỗi PR là tự bắn vào chân. `TD-4` vẫn **đóng**
  vì đã có đường chạy trong CI + bằng chứng cơ chế (`compose config` OK, `--wait` hỗ trợ, env prep chạy được).
- **`down -v` ở bước `always()`**: volume cũ làm `init/01-create-databases.sh` không chạy lại (bài học `SL-0.3`) ⇒ lần
  chạy sau sẽ gặp "database không tồn tại" rất khó hiểu.
- **`--wait --wait-timeout 420`**: để runner chờ healthcheck thay vì `sleep` mò; fail sớm nếu service không lên.

**Kết quả verify** *(chạy thật 2026-11-08 — stack compose 10 container healthy)*

```text
$ bash tools/e2e-checkout.sh
== E2E checkout happy path (Phase 2) — http://localhost:8080
  PASS [1] 1 login admin + seller                  PASS [15] 11b saga đã xong bước start
  PASS [2] 2 register buyer → 201                  PASS [16] 12 retry cùng Idempotency-Key → 200 (replay)
  PASS [3] 3 login buyer                           PASS [17] 12b replay trả cùng orderId
  PASS [4] 4 seller tạo category → 201             PASS [18] 12c replay có body checkout hợp lệ
  PASS [5] 5 seller tạo product → 201              PASS [19] 13 mock provider báo đã thanh toán → 200
  PASS [6] 6 seller tạo SKU → 201                  PASS [20] 14 confirm → 200
  PASS [7] 7 admin set tồn kho = 10 → 200          PASS [21] 14b saga CONFIRMED
  PASS [8] 8 admin tạo voucher FIXED 20000 → 201   PASS [22] 15 GET order (client) → 200
  PASS [9] 9 buyer thêm 2 món vào giỏ → 200        PASS [23] 15b order CONFIRMED
  PASS [10] 10 checkout → 201                      PASS [24] 15c order giữ total đã chốt
  PASS [11] 10b total = subtotal − voucher         PASS [25] 16 GET tồn kho → 200
  PASS [12] 10c subtotal đúng                      PASS [26] 16b onHand 10 → 8
  PASS [13] 10d có paymentUrl                      PASS [27] 16c reserved về 0
  PASS [14] 11 GET trạng thái saga → 200
== Kết quả: 27 PASS / 0 FAIL · 14 request · 931 ms
E2E checkout: OK
```

```text
$ bash tools/e2e-smoke.sh       17 PASS / 0 FAIL · 13 request · 949 ms
$ bash tools/export-openapi.sh  VALIDATE PASS · 41 path · 50 operation
$ .github/workflows/ci.yml      matrix build-image: 9 service (template, identity, catalog, inventory,
                                promotion, order, payment, cart, gateway)
$ .github/workflows/e2e.yml     yaml OK · jobs: [compose-e2e] · triggers: workflow_dispatch, schedule
$ docker compose ... config -q  compose config OK (Compose v5.7.0, hỗ trợ --wait)
$ env prep (sed 4 secret)       POSTGRES/REDIS/RABBITMQ/PAYMENT_WEBHOOK → giá trị CI, không còn 'change_me'
```

**Sự cố trong lúc verify (2 lỗi thật)**

- **Fail giả vì Python đổi số**: `json.load` trả `float` ⇒ `220000.00` thành `"220000.0"` ⇒ 3 assertion tiền đỏ dù API
  đúng. Fix: `parse_float=str, parse_int=str` để giữ nguyên chuỗi số của API.
- **CI run `42451712993` đỏ ở `build-image (promotion-service)`** — không phải do code: Maven Central tạm thời không trả
  `spring-boot-dependencies:4.1.2` ("absent in central") trong khi 8 job khác resolve bình thường; rerun thì **xanh**.
  Ghi `TD-26` (build-image flaky vì không cache Maven — trả nợ ở Phase 7 khi có registry + cache layer).

**Learning note**

- **E2E phải assert *giá trị*, không chỉ status**: 3/27 check đỏ ngay lần chạy đầu đều là check tiền — nếu chỉ check `201`
  thì script đã "xanh" trong khi con số trên hoá đơn có thể sai. Cùng lý do `SL-2.5` đặt CHECK constraint ở DB, còn script
  này đặt assertion ở tầng HTTP.
- **Script E2E là tài liệu sống**: chuỗi bước (cart → checkout → mock-pay → confirm → kiểm kho) chính là luồng nghiệp vụ
  viết bằng lệnh chạy được — người mới đọc là hiểu hệ, không phải đọc 5 service.
- **Chi phí CI là quyết định thiết kế**: cùng một workflow, chạy mỗi PR (≈30 phút/lần) sẽ đốt 2.000 phút trong ~1 tuần.
  `schedule` + `workflow_dispatch` giữ được giá trị "chứng minh chạy trong CI" mà không phá budget.
- **Trigger theo `schedule` không chạy trên PR** ⇒ workflow này **không** tạo cảm giác an toàn giả: nó không chặn merge,
  phần chặn vẫn là `ci.yml`. Hai thứ khác vai, đừng gộp.
- **`parse_float=str` lần thứ hai trong repo** (`SL-2.13` là `String.valueOf`): mọi ngôn ngữ trung gian đều có xu hướng
  "chuẩn hoá" số — assert tiền phải so sánh **chuỗi gốc**.

### SL-2.9 — E2E nhánh fail: hết hàng · payment fail · timeout ⇒ compensating đúng

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | `SL-2.2`/`SL-2.4` (release idempotent) · `SL-2.6a` (compensating) · `SL-2.6b` (confirm/cancel) · `SL-2.7` (mock provider `?fail=true`) · `SL-2.8` (khuôn script E2E); **chặn** `SL-2.10` |
| **Ước lượng** | M |

**Bối cảnh**

`SL-2.8` chỉ chứng minh **happy path**. Nhưng thứ đắt nhất trong saga là **compensating**: nếu nó sai thì hàng bị treo, voucher bị
giữ, đơn treo `PENDING_PAYMENT` — không ai thấy qua test happy path. Cần một lệnh chạy được **các nhánh fail thật** và assert
**state thật** (`stock.reserved`, `voucher.locked`, `order.status`), không chỉ assert status HTTP.

**Mục tiêu**

`tools/e2e-checkout-fail.sh` chạy 6 kịch bản fail trên stack thật, mỗi kịch bản assert **hệ đã trở về trạng thái sạch**.

**In scope**

- Script mới `tools/e2e-checkout-fail.sh` (F1…F6, PASS/FAIL/SKIP + exit code).
- `payment-service`: **hook bơm lỗi** (`debug/FaultInjection` + `FaultInjectionFilter` + `DebugFaultsController`) — chỉ tồn tại khi
  `shoplab.debug-endpoints=true`, ẩn khỏi OpenAPI (`@Hidden`), chỉ tác động `/v1/payment-intents`.
- `e2e.yml`: thêm bước chạy script fail (sau happy path).
- README: mục E2E nhánh fail + cảnh báo `/debug/**` **không** có route ở gateway.

**Out of scope**

- Không bơm lỗi ở inventory/promotion/catalog (chỉ payment — nơi compensating nhiều nhánh nhất); mở rộng để dành khi cần.
- Không test ca "khách bỏ ngang" (đã verify ở `SL-2.6b`: sweeper + TTL, `reserved 2 → 0` sau 45 s).
- Không circuit breaker/retry (Phase 5) — Phase 2 chỉ fail-fast + timeout.

**Acceptance Criteria**

- [x] **F1 hết hàng** (`onHand = 0`) → checkout **409** · `voucher.locked = 0` · `stock.reserved = 0`.
- [x] **F2 provider từ chối** (mock `?fail=true`) → `confirm` **409** (không commit) · `cancel` **200** · saga `CANCELLED` ·
      `reserved 0` · `locked 0`.
- [x] **F3 payment trả 5xx** (hook `failStatus=500`) → checkout **503** · `reserved 0` · `locked 0` · order mới nhất
      `CANCELLED`.
- [x] **F4 payment chậm hơn timeout** (hook `sleepMs=2000` > `SAGA_READ_TIMEOUT` 800 ms) → checkout **503** · thời gian chờ
      **≥ 700 ms** (chứng minh timeout thật, không phải fail tức thì) · `reserved 0` · `locked 0`.
- [x] **F5 payment chết hẳn** (`docker stop`) → checkout **503** · service tự sống lại · `reserved 0` · `locked 0`; thiếu
      docker thì **SKIP có đếm** (không im lặng).
- [x] **F6 retry cùng `Idempotency-Key` sau fail** → **201** (khoá đã được trả, không kẹt `IN_PROGRESS`) · `reserved 1`.
- [x] Script exit 0 khi tất cả pass; exit 1 + gợi ý `docker compose logs …` khi có FAIL.
- [x] `e2e-checkout.sh` (27/27) + `e2e-smoke.sh` (17/17) **không regress**; `mvn -B -ntp verify` **BUILD SUCCESS**.

**Ghi chú kỹ thuật**

- **Bơm lỗi bằng filter, không sửa controller**: `FaultInjectionFilter` chỉ tồn tại khi bật debug endpoints ⇒ production **không
  có** một dòng `if debug` nào trong đường nghiệp vụ; filter chạy sau Spring Security nên request đã xác thực (giống
  dependency thật).
- **`/debug/**` cố ý không có route ở gateway**: debug là nội bộ, không expose ra biên. Script gọi thẳng `:8088` (compose
  publish port) — đây cũng là cách con người debug thật.
- **Timeout phải là "chậm hơn ngưỡng"**: `sleepMs=2000` > `SAGA_READ_TIMEOUT` 800 ms; nếu ai set timeout 10 phút thì F4 sẽ
  đỏ (chậm quá) — đó là mục đích (canh cấu hình, không chỉ canh code).
- **`docker stop` (F5) khác `failStatus=500` (F3)**: refused = TCP chết ngay (~0 ms), 500 = service sống nhưng lỗi. Cả hai vào
  cùng nhánh `ResourceAccessException`/5xx → 503, nhưng F5 chứng minh checkout không treo khi dependency biến mất.
- **`cleanup` trap luôn reset hook + `docker start`**: script chết giữa đường mà để `sleepMs=2000` thì mọi checkout sau
  (kể cả `e2e-checkout.sh`) sẽ timeout oan.

**Kết quả verify** *(chạy thật 2026-11-08 — stack compose 10 container healthy, `order-service` env mặc định 15′/800 ms)*

```text
$ bash tools/e2e-checkout-fail.sh
-- F1 hết hàng giữa chừng
  PASS F1.1 checkout hết hàng → 409      PASS F1.2 voucher locked = 0      PASS F1.3 reserved = 0
-- F2 payment fail (provider từ chối)
  PASS F2.1 checkout → 201 · F2.2 reserved = 1 · F2.3 provider FAILED → 200
  PASS F2.4 confirm khi payment FAILED → 409 · F2.5 cancel → 200 · F2.6 saga CANCELLED
  PASS F2.7 reserved = 0 · F2.8 locked = 0
-- F3 payment lỗi 5xx (debug hook)
  PASS F3.1 bật failStatus=500 → 200 · F3.2 checkout → 503 · F3.3 tắt hook → 200
  PASS F3.4 reserved = 0 · F3.5 locked = 0 · F3.6 order mới nhất CANCELLED
-- F4 payment chậm hơn read-timeout (2000 ms)
  PASS F4.1 bật sleepMs=2000 → 200 · F4.2 checkout → 503
  PASS F4.3 đã chờ tới timeout rồi mới bỏ (921 ms ≥ 700 ms)
  PASS F4.4 reserved = 0 · F4.5 locked = 0
-- F5 payment chết hẳn (docker stop)
  PASS F5.1 checkout → 503 · F5.2 payment sống lại · F5.3 reserved = 0 · F5.4 locked = 0
-- F6 retry cùng key sau fail
  PASS F6.1 retry key của F3 → 201 · F6.2 reserved = 1 · F6.3 huỷ đơn → 200 · F6.4 reserved = 0

== Kết quả: 41 PASS / 0 FAIL / 0 SKIP · 37 request · 12118 ms
E2E checkout fail-branches: OK
```

```text
$ bash tools/e2e-checkout.sh   27 PASS / 0 FAIL · 956 ms     (không regress)
$ bash tools/e2e-smoke.sh      17 PASS / 0 FAIL · 484 ms     (không regress)
$ mvn -B -ntp verify           BUILD SUCCESS — payment 64 unit (+5 FaultInjectionTest) + 12 IT,
                               toàn repo 11 module xanh, checkstyle 0
$ python3 yaml.safe_load(e2e.yml)  steps: 9 (thêm bước chạy script fail)
```

**Sự cố trong lúc verify (3 lỗi thật — 1 trong code test, 2 trong script)**

- **`/debug/faults` qua gateway → 401**: gateway **không** có route `/debug/**` (đúng thiết kế — debug không expose ra biên) ⇒
  script phải gọi thẳng `:8088`. Đây là phát hiện tốt: nếu route đó tồn tại thì mới là lỗi bảo mật.
- **Assert sai field**: `POST /v1/checkout/{id}/cancel` trả `SagaStatusResponse` (`state`), không phải `OrderResponse`
  (`status`) ⇒ F2.6 đọc `status` ra rỗng.
- **`$STATUS` bị ghi đè**: gọi `faults 0 0` (cũng dùng `http_do`) **trước** khi assert F4.2 ⇒ assert đọc status của lệnh reset
  (200) thay vì 503 của checkout. Fix: chốt `F4_STATUS="$STATUS"` ngay sau lời gọi cần đo.

**Learning note**

- **Compensating chỉ đáng tin khi assert *state*, không phải status**: 503 trả về client **không** chứng minh hàng đã được
  trả — phải đọc `stock.reserved` và `voucher.locked` (qua API, không cần psql). Cùng triết lý `SL-2.5` (CHECK ở DB) nhưng ở
  tầng E2E.
- **Bơm lỗi phải nằm ở hạ tầng, không ở nghiệp vụ**: một `Filter` có `@ConditionalOnProperty` giữ `PaymentController` sạch;
  production không có endpoint nào để bật nó, và filter chạy **sau** security nên request đã xác thực như thật.
- **Timeout chỉ được chứng minh khi dependency *chậm hơn* ngưỡng**: `docker stop` (refused ~0 ms) và `failStatus=500` đi cùng
  một nhánh code, nhưng **không** chứng minh `SAGA_READ_TIMEOUT` có tác dụng. F4 đo được 921 ms ≈ 800 ms timeout ⇒ cấu hình
  là thật.
- **Script E2E cũng là code**: `$STATUS` là biến toàn cục bị mọi `http_do` ghi đè ⇒ phải chốt giá trị ngay sau lời gọi cần đo.
  Đây là loại bug làm test "xanh/đỏ sai" — nguy hiểm hơn bug trong sản phẩm vì nó làm mất niềm tin vào bộ test.
- **`cleanup` trap là bắt buộc khi test bơm lỗi**: không reset thì một lần script chết sẽ làm **mọi** checkout sau đó timeout —
  triệu chứng hoàn toàn không liên quan tới nguyên nhân.
### SL-2.12 — `service-commons`: 403 handler vào commons (trả `TD-3`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 2 — Core commerce flow *(phát sinh từ `SL-2.2`)* |
| **Phụ thuộc** | `SL-2.2`/`SL-2.4`/`SL-2.5`/`SL-2.7` (đã copy handler 4 lần) · `SL-2.1`; **chặn** `SL-2.10` (DoD phase cần mọi ticket xong) |
| **Ước lượng** | S |

**Bối cảnh**

`TD-3` ghi từ Phase 1: handler map `AccessDeniedException` → 403 nằm **trong** `catalog-service`, service nào dùng
`@PreAuthorize` cũng phải copy. Đến hết Phase 2 thì con số không còn là 2 mà là **6**: `catalog` · `inventory` · `cart` ·
`order` · `payment` · `promotion` — 6 file **giống nhau từng byte** (javadoc vẫn ghi "ứng viên chuyển vào commons khi
service thứ tư cần"). Đây đúng là loại nợ mà `AGENTS.md` §6 cấm: hành vi **của toàn hệ** (mã lỗi 403) lại nằm ở 6 chỗ.

**Mục tiêu**

Một bản duy nhất ở `service-commons`, mọi service dùng `@PreAuthorize` tự có 403 `ApiError` mà **không viết dòng nào**.

**In scope**

- `packages/service-commons`: `web/AccessDeniedApiExceptionHandler` (bản duy nhất) + `web/AccessDeniedAutoConfiguration`
  (`@ConditionalOnClass(AccessDeniedException)` · `@ConditionalOnWebApplication(SERVLET)` · `@ConditionalOnMissingBean`)
  đăng ký trong `AutoConfiguration.imports`.
- `service-commons/pom.xml`: `spring-security-core` để **`optional = true`** (cùng khuôn `webmvc`/`springdoc`) — consumer
  không khai security thì không kéo gì thêm.
- Xoá **6 bản copy**: `services/{catalog,inventory,cart,order,payment,promotion}-service/src/main/java/**/web/AccessDeniedApiExceptionHandler.java`.
- Test mới `AccessDeniedApiExceptionHandlerTest` ở commons (403 + `code = FORBIDDEN` + `path` + `traceId` có mặt).

**Out of scope**

- Không đụng `SecurityConfig`/`accessDeniedHandler` của filter chain (đó là nhánh **trước** controller, đã trả `ApiError`
  từ Phase 1) — ticket này chỉ xử lý nhánh **method security** (`@PreAuthorize`).
- Không gộp `GlobalExceptionHandler` và advice này thành một class: `GlobalExceptionHandler` **không** cần security trên
  classpath, gộp lại là ép mọi service kéo spring-security.
- Không thêm `@PreAuthorize` cho cart/order/payment để "cho đủ bộ" (không đổi API surface).

**Acceptance Criteria**

- [x] `service-commons` là **nơi duy nhất** có handler 403: `git grep -l "class AccessDeniedApiExceptionHandler"` = **1 file**.
- [x] `grep -c AccessDeniedApiExceptionHandler` trong `services/**` = **0** (không service nào còn copy).
- [x] `@PreAuthorize` bị từ chối vẫn trả **403** + body `ApiError` (`code=FORBIDDEN`, có `traceId`) — kiểm bằng IT sẵn có
      của `catalog`/`inventory`/`promotion` **và** 1 lần `curl` trên stack compose đã rebuild.
- [x] `service-template` (không có `spring-security` trên classpath) vẫn **boot được** và test xanh ⇒ guard
      `@ConditionalOnClass` có tác dụng, không `NoClassDefFoundError`.
- [x] `mvn -B -ntp verify` toàn repo **BUILD SUCCESS**; 3 script E2E không regress.
- [x] `TD-3` → `✅ đã trả (SL-2.12)`.

**Ghi chú kỹ thuật**

- **`@ConditionalOnClass` phải đặt ở cấp class của một auto-config riêng**, không đặt trên `@Bean` method: khi đặt trên
  method, Spring vẫn phải resolve **return type** (class có import `AccessDeniedException`) ⇒ service **không** có
  spring-security sẽ chết lúc khởi động. Class-level condition được đọc bằng ASM metadata nên class không bị load.
- **`optional = true` cho `spring-security-core`**: cùng lý do với `webmvc` — commons compile được, consumer không bị ép.
  Hệ quả tích cực: `api-gateway` (chỉ có `spring-security-oauth2-jose`) và `service-template` **không** kéo thêm gì.
- **Không đổi hành vi**: copy nguyên nội dung handler (log `warn` vì từ chối quyền là hành vi đúng, không phải sự cố) —
  ticket này là **refactor**, mọi thay đổi hành vi sẽ bị IT 403 bắt.

**Kết quả verify** *(chạy thật 2026-11-08 — Docker 12 container healthy)*

**1) Một bản duy nhất, không còn copy**

```console
$ grep -rl 'class AccessDeniedApiExceptionHandler' --include=*.java .
./packages/service-commons/src/main/java/com/shoplab/commons/web/AccessDeniedApiExceptionHandler.java
./packages/service-commons/src/test/java/com/shoplab/commons/web/AccessDeniedApiExceptionHandlerTest.java
$ grep -rl 'AccessDeniedApiExceptionHandler' --include=*.java services | wc -l
0
$ git diff --stat origin/main | tail -1
 9 files changed, 85 insertions(+), 296 deletions(-)   # 6 file × 48 dòng copy bị xoá
```

**2) `mvn -B -ntp verify` toàn repo — BUILD SUCCESS · 655 test (537 unit + 118 IT) · 0 fail**

```text
ShopLab :: Parent / Service Commons / Service Template / Identity / Catalog /
Inventory / Promotion / Order / Payment / Cart / API Gateway ......... SUCCESS
537 unit + 118 IT = 655 test, failures 0
```

`service-commons`: **47 unit** (+5 so với Phase 1: `AccessDeniedApiExceptionHandlerTest` 3 ·
`AccessDeniedAutoConfigurationTest` 2). `service-template` (**không** có `spring-security` trên
classpath) vẫn xanh ⇒ guard `@ConditionalOnClass` đúng, không `NoClassDefFoundError`.

**3) 403 trên stack thật** (rebuild image `catalog` + `inventory`)

```console
$ curl -X POST localhost:8080/v1/categories -H "Authorization: Bearer $BUYER" -d '{"name":"SL212 Buyer",...}'
{"code":"FORBIDDEN","message":"Không có quyền truy cập","traceId":"11da09f4-…","path":"/v1/categories"}
status=403
$ curl -X PUT localhost:8080/v1/stock/$SKU -H "Authorization: Bearer $BUYER" -d '{"onHand":5}'
{"code":"FORBIDDEN",…}   status=403
$ curl -X POST localhost:8080/v1/categories -H "Authorization: Bearer $SELLER"   → 201   (không chặn oan)
$ curl -X PUT  localhost:8080/v1/stock/$SKU     -H "Authorization: Bearer $ADMIN"  → 200   (không chặn oan)

$ docker logs shoplab-catalog-service | grep 11da09f4
{"msg":"từ chối quyền POST /v1/categories: Access Denied",
 "logger":"com.shoplab.commons.web.AccessDeniedApiExceptionHandler","level":"WARN","service":"catalog-service"}
```

Dòng log trên là **bằng chứng quan trọng nhất**: logger là class **trong commons**, không phải bản copy
của service ⇒ advice được auto-config cắm vào thật.

**4) 3 script E2E — không regress**

```text
$ bash tools/e2e-smoke.sh           17 PASS / 0 FAIL · 13 request · 552 ms   · E2E smoke: OK
$ bash tools/e2e-checkout.sh        27 PASS / 0 FAIL · 14 request · 2481 ms  · E2E checkout: OK
$ bash tools/e2e-checkout-fail.sh   41 PASS / 0 FAIL · 37 request · 11,4 s   · 2 lần liên tiếp
```

⚠️ Lần chạy `e2e-checkout-fail.sh` **đầu tiên** ngay sau `up -d --force-recreate` cho **38 PASS / 3 FAIL**;
2 lần sau **41 PASS / 0 FAIL**. Nghi do container mới chưa ấm (script không chờ `healthy`) ⇒ ghi `TD-27`,
không giấu.

**Sự cố trong lúc verify (1 bug thật — suýt ship 403 → 500)**

- Lần chạy `verify` đầu tiên: `CatalogServiceApplicationIT` **đỏ 2 test** (`expected: 403 but was: 500`).
  Conditions report in ra `AccessDeniedAutoConfiguration matched` + bean được đăng ký, nhưng log có
  `GlobalExceptionHandler — unhandled exception: AuthorizationDeniedException`, tức catch-all
  `@ExceptionHandler(Exception.class)` bắt trước advice cụ thể.
- Nguyên nhân: `ExceptionHandlerExceptionResolver` lấy advice **đầu tiên** khớp trong cache, và một
  advice khớp cả **tổ tiên** của exception. Trước đây bản copy là `@Component` (component scan đăng ký
  **trước** bean auto-config) nên thắng; chuyển sang commons thì thứ tự **đảo lại** ⇒ `Exception` thắng.
- Fix: `@Order(Ordered.HIGHEST_PRECEDENCE)` trên advice + test canary
  `shouldStayOrderedBeforeCatchAllAdvice` (mất `@Order` là test đỏ ngay).

**Learning note**

- **Refactor "chỉ move file" vẫn có thể đổi hành vi** — vì **thứ tự đăng ký bean** là một phần hành vi.
  Cùng một class, cùng annotation, chỉ khác nơi khai báo (`@Component` trong service → `@Bean` ở
  auto-config) mà 403 thành 500. Nếu chỉ tin `mvn verify` của module commons thì bug này đi thẳng ra
  production; thứ bắt được nó là **IT 403 có sẵn** của catalog — lại đúng bài học Phase 1: "hành vi liên
  quan thứ tự/transaction chỉ lộ khi gọi HTTP thật".
- **`@ExceptionHandler(Exception.class)` là cái bẫy im lặng**: catch-all của commons rất tiện nhưng nó
  nuốt mọi thứ *chưa* có advice cụ thể, và `ExceptionHandlerExceptionResolver` **không** chọn handler
  "gần nhất" — nó chọn **advice đầu tiên** khớp. Muốn chắc chắn thì advice cụ thể phải có `@Order` +
  test canary, đừng dựa vào thứ tự bean.
- **Guard `@ConditionalOnClass` phải ở cấp class của một auto-config riêng**: đặt trên `@Bean` method
  thì Spring vẫn resolve return type ⇒ service không có security sẽ chết lúc khởi động. Test bằng
  `ApplicationContextRunner` + `FilteredClassLoader` chứng minh được nhánh này trong **0,25 s**, không
  cần dựng service.
- **Ticket ghi "2 bản copy" nhưng thực tế 6**: số liệu trong ticket cũ **mục ruỗng theo thời gian** (viết
  từ lúc mới có catalog/inventory). Trước khi làm, đếm lại bằng `grep -rl` — rẻ hơn nhiều so với việc
  sửa 2 file rồi phát hiện còn 4 bản nữa.
- **`td;dr` cho service mới**: khai `spring-boot-starter-security` là tự có 403 chuẩn `ApiError`, không
  phải copy file. Đây là giá trị thật của việc trả nợ `TD-3` (nợ "copy-paste" luôn đắt hơn nợ "thiếu
  feature").

### SL-2.10 — `docs/runbook-phase-2.md` + DoD Phase 2 (archive · tag `phase-2` · release)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | In Progress |
| **Phase** | 2 — Core commerce flow |
| **Phụ thuộc** | mọi ticket Phase 2 `Done` (`SL-2.0` … `SL-2.13`, kể cả `SL-2.12`); runbook Phase 1 làm khuôn |
| **Ước tính** | S |

**Bối cảnh**

Phase 2 xong về code nhưng **chưa có tài liệu vận hành**: `docs/runbook-phase-1.md` chỉ nói về auth + catalog. Khi checkout
fail, người trực cần biết đọc **state nào** (`stock.reserved` · `voucher.locked` · `orders.status` · `checkout_sagas.state`)
chứ không chỉ `curl` endpoint. Đồng thời phase 2 chưa đóng theo DoD của phase (`AGENTS.md` §5, ADR-0005) nên chưa có
snapshot để tra cứu về sau.

**Mục tiêu**

1. `docs/runbook-phase-2.md` debug được luồng checkout **từ ngoài vào trong**, kèm output debug thật.
2. Đóng Phase 2 theo đúng DoD: archive chi tiết, doc snapshot `phase-2`, tag + release, cập nhật index/roadmap.

**In scope**

- `docs/runbook-phase-2.md`: sơ đồ luồng saga · bảng tra nhanh (port · health · DB · log · env knob) · kiểm tra từng tầng
  (gateway → order → inventory/promotion/payment/cart → DB/Redis) · bảng lỗi thường gặp · **§5 output debug thật**.
- Tách chi tiết 15 ticket Phase 2 sang `Backlog-phase-2.md`; `Backlog.md` chỉ giữ trạng thái + link archive.
- `docs/phases/phase-2.md` (stack · có gì · cách chạy ở tag · số liệu · **chưa có** · bài học).
- Verify `docker compose` **từ volume rỗng** (`down -v` → `up -d --build`) + 3 script E2E.
- Tag annotated `phase-2` trên **merge commit đóng phase** + GitHub Release; update `docs/phases/README.md`, bảng archive
  trong `Backlog.md`, `architecture.md` §12.

**Out of scope**

- Không sửa code service (phase đã đóng code); mọi bug phát hiện thêm → ticket mới.
- Không thêm kịch bản E2E mới (`SL-2.9` đã chốt 41 check).
- Không mở `maint/phase-2` (chỉ mở khi cần patch snapshot — ADR-0005).
- Không làm runbook sự cố đầy đủ (dashboard/alert/escalation) — thuộc Phase 5/6.

**Acceptance Criteria**

- [x] `docs/runbook-phase-2.md` có: sơ đồ luồng · bảng port/health/DB/log · ≥ 8 lệnh kiểm tra copy-paste · bảng **≥ 6 lỗi**
      (triệu chứng → lệnh kiểm tra → nguyên nhân → xử lý) · **một** ví dụ debug thật dán nguyên văn output.
- [x] Mỗi lệnh trong runbook **chạy được thật** trên stack compose (dán output ở §5).
- [x] `Backlog-phase-2.md` có đủ **15 ticket** + header (tag · stack · service · kết quả); `Backlog.md` không còn `### SL-2.x`.
- [x] `docs/phases/phase-2.md` có số liệu **đo thật**: test · OpenAPI · E2E · số container · image size · port.
- [x] `docker compose down -v` → `up -d --build` → **12 container healthy** từ volume rỗng; 3 script E2E xanh.
- [x] Tag annotated `phase-2` + Release có notes; `docs/phases/README.md` + bảng archive + `architecture.md` §12 đã update.

**Ghi chú kỹ thuật**

- Runbook **không** lặp lại nội dung Phase 1 (401/403/JWKS) — chỉ link, tránh 2 nguồn sự thật.
- Bảng lỗi ưu tiên **triệu chứng người trực thấy được** ("đơn treo `PENDING_PAYMENT`", "hàng không về") thay vì tên
  exception.
- DoD phase: tag gắn **sau merge** (tag trên merge commit = commit đóng phase), release notes tái dùng `Backlog-phase-2.md`.
- Phase 2 là phase **toàn doc** ⇒ PR docs-only, không tốn phút CI (điều kiện `merge-pr.sh` đã hỗ trợ từ `SL-1.14`).

**Kết quả verify** *(chạy thật 2026-11-08)*

__FILL__

**1) Stack dựng lại từ **volume rỗng** (`down -v` → `up -d --build`)** — đúng thứ người clone mới sẽ làm:

```console
$ docker compose -f infra/docker-compose.yml --env-file .env down -v
$ docker compose -f infra/docker-compose.yml --env-file .env up -d --build
$ docker compose -f infra/docker-compose.yml --env-file .env ps
shoplab-adminer            Up (healthy)      shoplab-inventory-service   Up (healthy)
shoplab-api-gateway        Up (healthy)      shoplab-order-service       Up (healthy)
shoplab-cart-service       Up (healthy)      shoplab-payment-service     Up (healthy)
shoplab-catalog-service    Up (healthy)      shoplab-postgres            Up (healthy)
shoplab-identity-service   Up (healthy)      shoplab-promotion-service   Up (healthy)
shoplab-rabbitmq           Up (healthy)      shoplab-redis               Up (healthy)
→ 12 / 12 container healthy (Flyway + seed local tự chạy trên volume mới: 5 category · 20 product · 24 SKU)
```

**2) 3 script E2E trên stack đó — không regress:**

```text
$ bash tools/e2e-smoke.sh          17 PASS / 0 FAIL · 13 request · 536 ms
$ bash tools/e2e-checkout.sh       27 PASS / 0 FAIL · 14 request · 1133 ms
$ bash tools/e2e-checkout-fail.sh  41 PASS / 0 FAIL · 37 request · 11522 ms
```

**3) Mọi lệnh trong runbook §3 chạy được thật:**

```console
$ curl -s -o /dev/null -w 'gateway=%{http_code} ' localhost:8080/health
gateway=200 8083=200 8084=200 8086=200 8087=200 8088=200
$ docker exec shoplab-postgres psql -U shoplab -d inventory -c 'select sku_id, on_hand, reserved, version from stock …'
 82e85eba-…  |      10 |        2 |       5
$ docker exec shoplab-postgres psql -U shoplab -d order -c 'select order_id, state, failure_code from checkout_sagas …'
 9186eafa-…  | COMPLETED_START |
 ead28426-…  | FAILED_PAYMENT  | DOWNSTREAM_UNAVAILABLE_PAYMENT
$ docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern 'cart:*'
cart:u:712cd801-… · cart:u:ff18d654-…
```

**4) `docs/runbook-phase-2.md` §5 = output debug thật** (không viết tay): tắt `payment-service` giữa saga ⇒
`503` + `reserved` **vẫn 0** + `RESERVE`/`RELEASE` khớp trong `stock_ledger` + `saga = FAILED_PAYMENT /
DOWNSTREAM_UNAVAILABLE_PAYMENT`; khôi phục ⇒ `201` + `COMPLETED_START`. Kèm bẫy thật: `docker inspect` báo
`healthy` **cũ** sau `docker start` ⇒ phải chờ `/ready` 200.

**5) `mvn -B -ntp verify`** — chạy lại trong lượt này: **BUILD SUCCESS · 11 module · 655 test (537 unit + 118 IT) ·
0 fail** (commons 47 · template 35 · identity 50+22 · catalog 79+25 · inventory 44+15 · promotion 48+13 · order 65+25 ·
payment 64+12 · cart 34+6 · gateway 71).

**6) OpenAPI**: `bash tools/export-openapi.sh` → `VALIDATE PASS · 41 path · 50 operation · 35 operation cần token ·
46 schema`.

**7) DoD phase (AGENTS.md §5)**: archive 15/15 ticket → `Backlog-phase-2.md` · `docs/phases/phase-2.md` ·
tag annotated `phase-2` trên merge commit + GitHub Release · `docs/phases/README.md` + bảng archive + `architecture.md` §12
đã update.

**Sự cố trong lúc làm**

- **`SL-2.6b` không có đặc tả riêng** trong Backlog (Done nhưng chỉ được nhắc ở ticket khác) ⇒ khi archive phải **viết
  bổ sung** từ commit `5da0cc6` + changelog, có ghi rõ nguồn. Đây là lỗ hổng tài liệu của phase, không phải của ticket này.
- `docs/phases/README.md` còn ghi phase 2 "🟡 Breakdown xong (11 ticket)" và `Backlog.md` ghi "8 / 13", "14 / 15" lệch
  thực tế ⇒ sửa luôn trong lượt này (số liệu doc cũng mục như code).

**Learning note**

- **Đóng phase là việc *đọc lại toàn bộ* tài liệu của phase**, không phải chỉ chạy `git tag`: ba chỗ lệch số liệu
  (README index, bảng trạng thái, header backlog) và **một ticket thiếu đặc tả** chỉ lộ ra khi rà lại để archive.
- **Verify "snapshot chạy được" phải là `down -v` + `up --build`**, không phải `up -d` trên volume đang có: volume cũ
  che mất lỗi Flyway/seed — đúng lớp lỗi mà người clone mới sẽ gặp đầu tiên.
- **Runbook chỉ đáng tin khi chính mình dùng nó để debug**: §5 là output của lần tắt `payment-service` thật, và nó lộ
  ngay một điều không ai viết trong code — health check của Docker **trễ 1 nhịp** sau restart.
- **Tag sau merge, không tag trước**: tag trên merge commit thì `git checkout phase-2` mới đúng là cây code đã qua CI
  (ADR-0005); release notes tái dùng archive, không viết lại.

