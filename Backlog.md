# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (`SL-2.6a` ✅ **Done** — checkout saga start đã lên `main`) |
| **Ticket tiếp theo** | `SL-2.6b` — Checkout Saga **hoàn tất** (confirm · cancel · sweeper 15′) · `SL-2.8` — E2E checkout |
| **Số ticket chi tiết hiện có** | 33 (Phase 0: 8 — archive · Phase 1: 15 — archive · Phase 2: 10) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | ✅ Done (tag `phase-1`) | 19 / 19 |
| 2 | Core commerce flow | 🟡 Đang làm — mô hình đã chốt ([ADR-0006](docs/adr/0006-checkout-model.md)) | 8 / 13 |
| 3 | Async & event-driven | ⚪ Todo (epic) | — |
| 4 | Scale & performance | ⚪ Todo (epic) | — |
| 5 | Resilience | ⚪ Todo (epic) | — |
| 6 | Observability & SRE | ⚪ Todo (epic) | — |
| 7 | Deployment & infra | ⚪ Todo (epic) | — |
| 8 | Advanced | ⚪ Todo (epic) | — |

---

## 2. Convention ticket

- **ID**: `SL-<phase>.<số>` — ví dụ `SL-0.1`, `SL-2.4`. Không đổi ID sau khi tạo.
- **Trạng thái**: `Todo` / `In Progress` / `Blocked` / `Review` / `Done`
- **Ước lượng**: `S` (< nửa ngày) · `M` (1–2 ngày) · `L` (3–5 ngày). Nếu `L` thì **phải tách nhỏ**.
- Viết như **BA đã define xong**: mô tả được, verify được, không còn chỗ mơ hồ.
- Ticket ở phase xa chỉ cần mức **epic** — chỉ breakdown khi tới lượt.

### Template

```markdown
### SL-X.Y — <tên ticket>

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | X — <tên phase> |
| **Phụ thuộc** | SL-... / — |
| **Ước lượng** | S / M / L |

**Bối cảnh**
Vì sao ticket này tồn tại. Vấn đề đang gặp là gì.

**Mục tiêu**
Kết quả cuối cùng cần đạt.

**In scope**
- ...

**Out of scope**
- ...

**Acceptance Criteria**
- [ ] ...

**Ghi chú kỹ thuật**
Pattern / thư viện / trade-off đã chấp nhận.

**Kết quả verify** *(điền khi Done)*
- Lệnh đã chạy + output tóm tắt

**Learning note** *(điền khi Done)*
- Học được gì, cái gì bất ngờ, lần sau nên làm khác thế nào.
```

---

## 3. Phase 0 — Foundation (✅ Done — chi tiết đã tách sang file archive)

> Phase 0 hoàn thành **8/8 ticket** (2026-11-03): repo skeleton + convention · hạ tầng local bằng 1 lệnh ·
> service template `/health` 200 · **CI xanh trên `main`**.
>
> Toàn bộ chi tiết 8 ticket (bối cảnh · scope · Acceptance Criteria · quyết định khi implement ·
> **kết quả verify bằng output thật** · learning note) đã tách sang **[`Backlog-phase-0.md`](Backlog-phase-0.md)**.
> Lý do: file này chỉ giữ việc **đang làm / sắp làm**; phần đã xong vẫn cần tra cứu nên **archive chứ không xoá**.

### Archive các phase đã xong

| Phase | File archive | Doc snapshot | Tag / Release | Ticket | Trạng thái |
|---|---|---|---|---|---|
| 0 — Foundation | [`Backlog-phase-0.md`](Backlog-phase-0.md) | [`docs/phases/phase-0.md`](docs/phases/phase-0.md) | `phase-0` | `SL-0.1` … `SL-0.8` | ✅ Done (8/8) |

> Quy ước: mỗi phase xong → tách phần chi tiết sang `Backlog-phase-<N>.md`, viết `docs/phases/phase-N.md`,
> gắn **tag `phase-N` + GitHub Release**, rồi thêm một dòng vào bảng trên (chi tiết: `AGENTS.md` §5 — DoD của phase;
> lý do: [ADR-0005](docs/adr/0005-phase-snapshot-and-release.md)).
> Learning log + Changelog **vẫn ở file này** (chỉ mục xuyên phase).

---

## 4. Phase 1 — Walking skeleton (✅ Done — chi tiết đã tách sang file archive)

> **Mục tiêu phase**: từ client tới DB chạy **end-to-end** với 3 service (`api-gateway` →
> `identity-service` / `catalog-service` → Postgres), có auth JWT + contract OpenAPI.
>
> **Điều kiện hoàn thành phase**: register → login → lấy token → tạo/đọc product qua gateway · CI xanh ·
> có OpenAPI spec · tách archive + tag `phase-1` (ADR-0005).

**19/19 ticket Done** (`SL-1.0` … `SL-1.14` kế hoạch + 4 phát sinh `SL-1.15`–`SL-1.18`).

- 📦 **Archive chi tiết** (verify + learning note từng ticket): [`Backlog-phase-1.md`](Backlog-phase-1.md)
- 🏷️ **Snapshot**: tag [`phase-1`](docs/phases/phase-1.md) · doc [`docs/phases/phase-1.md`](docs/phases/phase-1.md) ·
  [GitHub Release](https://github.com/anchu3003/shop-lab-microservices/releases/tag/phase-1)
- 📄 **Contract**: [`docs/api/openapi.yaml`](docs/api/openapi.yaml) (14 path / 18 operation)
- 🧪 **Verify nhanh**: `bash tools/e2e-smoke.sh` (17 mục) · `mvn -B -ntp verify`

| # | Ticket | Epic item | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-1.0` | *(phát sinh)* tối ưu chi phí CI | S | ✅ Done |
| 2 | `SL-1.1` | `identity-service`: register/login + JWT | M | ✅ Done |
| 3 | `SL-1.2` | `api-gateway` bản mỏng | M | ✅ Done |
| 4 | `SL-1.3` | **E2E sớm**: register → login qua gateway | S | ✅ Done |
| 5 | `SL-1.4` | `identity-service`: refresh token + RBAC | M | ✅ Done |
| 6 | `SL-1.5` | `catalog-service`: CRUD category/product | M | ✅ Done |
| 7 | `SL-1.6` | `catalog-service`: SKU + phân trang/filter + seed | M | ✅ Done |
| 8 | `SL-1.7` | E2E smoke đầy đủ + runbook | S | ✅ Done |
| 9 | `SL-1.8` | OpenAPI spec Phase 1 | S | ✅ Done |
| 10–15 | `SL-1.9` … `SL-1.14` | *(phát sinh)* process/CI/compose/merge script | S/M | ✅ Done |
| 16 | `SL-1.15` | *(phát sinh)* seed chỉ ở local + slug tiếng Việt | S | ✅ Done |
| 17 | `SL-1.16` | *(phát sinh)* tech debt register + rule | S | ✅ Done |
| 18 | `SL-1.17` | *(phát sinh)* ẩn endpoint debug khỏi spec | S | ✅ Done |
| 19 | `SL-1.18` | *(phát sinh)* grace window 30s cho refresh reuse | S | ✅ Done |
| 20 | `SL-1.19` | *(phát sinh sau DoD)* fix: export OpenAPI deterministic | S | ✅ Done |



---

### SL-1.19 — *(phát sinh sau DoD)* fix: export OpenAPI deterministic

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton *(phát sinh khi làm DoD)* |
| **Phụ thuộc** | SL-1.8 |
| **Ước lượng** | S |

**Bối cảnh**

`bash tools/export-openapi.sh` chạy 2 lần cho ra 2 file **khác nhau** dù code không đổi: 56 dòng đổi chỗ
giữa mỗi lần export. Nguyên nhân gốc: `ERROR_RESPONSES` trong `OpenApiAutoConfiguration` dùng
`Map.copyOf(...)` — map bất biến **không giữ thứ tự** ⇒ thứ tự mã lỗi (400/401/403/404/409) đổi mỗi lần
khởi động JVM. Phát hiện khi chuẩn bị tag `phase-1` (spec trong snapshot phải ổn định).

**Mục tiêu**

Export **idempotent**: chạy lại bao nhiêu lần cũng ra cùng một file.

**In scope**

- `OpenApiAutoConfiguration`: giữ thứ tự bằng `Collections.unmodifiableMap(new LinkedHashMap<>(...))`
- `tools/export-openapi.sh`: **sort `responses`** của mọi operation (phòng thủ lớp 2)
- Export lại `docs/api/openapi.yaml`

**Acceptance Criteria**

- [x] Export 2 lần liên tiếp → **md5 giống nhau**, `git diff` rỗng
- [x] Spec vẫn `VALIDATE PASS` (14 path / 18 operation / 7 cần token)
- [x] `mvn verify` + CI xanh

**Kết quả verify**

```console
$ bash tools/export-openapi.sh          # lần 1
VALIDATE PASS · 14 path · 18 operation · 7 operation cần token · 18 schema
$ md5sum docs/api/openapi.yaml
23e5751c77eb753320cc7817352c64d7  docs/api/openapi.yaml
$ bash tools/export-openapi.sh          # lần 2
$ md5sum docs/api/openapi.yaml
23e5751c77eb753320cc7817352c64d7  docs/api/openapi.yaml   ✅ idempotent
```

**Learning note**

- **`Map.copyOf` / `Map.of` không giữ thứ tự** — chỉ dùng khi thứ tự không quan trọng. Với artifact **diff
  được trong git** (spec, generated code) thì thứ tự là một phần của artifact: phải dùng
  `LinkedHashMap` **và** sort ở chỗ ghi file (phòng thủ 2 lớp: nguồn + nơi serialize).
- **Test "idempotent" phải so hash hai lần chạy**, không phải đọc code thấy `sorted(...)` là tin. Bug này
  chỉ lộ khi chạy 2 lần liên tiếp — đúng thứ tự tự nhiên của một script export.

---

## 5. Phase 2 → 8 — Epic outline

> Các phase này **chỉ ở mức epic**. Chỉ breakdown thành ticket chi tiết khi tới lượt — breakdown sớm sẽ phải viết lại
> vì hiểu biết thay đổi sau mỗi phase (`AGENTS.md` §3.1).

### Phase 2 — Core commerce flow (breakdown `SL-2.x`)

> **Mục tiêu**: luồng mua hàng hoàn chỉnh, **đúng dữ liệu kể cả khi fail** — trọng tâm là *Saga + compensating
> action + idempotency + chống oversell*, không phải UI.
> **Điều kiện hoàn thành**: checkout end-to-end qua gateway (cart → order → inventory reserve → promotion lock →
> payment → confirm) · có nhánh fail + compensating chạy đúng (có test) · **100 request đồng thời cùng 1 SKU
> không oversell** (có số liệu) · OpenAPI + runbook cập nhật · tag `phase-2`.

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-2.0` | **ADR-0006** chốt mô hình checkout (reservation TTL · idempotency key · snapshot vs FK · sync Phase 2 / async Phase 3 · quyết định `TD-12` soft delete) | S | ✅ Done |
| 2 | `SL-2.1` | `cart-service` (8083, Redis): add/update/remove/get, TTL, **merge cart khi login** | M | ✅ Done |
| 3 | `SL-2.2` | `inventory-service` (8084, DB `inventory`): schema `stock`/`reservations`/`stock_ledger` + API `reserve`/`commit`/`release`; **atomic** chống oversell | M | ✅ Done |
| 4 | `SL-2.3` | inventory: **test đồng thời** (100 request cùng SKU) + reservation **TTL** & giải phóng hết hạn | M | ✅ Done |
| 5 | `SL-2.4` | `promotion-service` (8086, DB `promotion`): voucher + rule giảm giá + `lock`/`commit`/`release` voucher | M | ✅ Done |
| 6 | `SL-2.5` | `order-service` (8087, DB `order`): tạo order **idempotent** (idempotency key) + state machine + `order_items` **snapshot** giá | M | ✅ Done |
| 7 | `SL-2.6a` | **Checkout Saga — start** (`order-service`): `POST /v1/checkout` đọc **cart** → lấy giá bằng **batch SKU** của catalog → lock voucher → reserve stock → tạo order → payment intent; lưu **saga state**; compensating cho nhánh fail lúc start | M | ✅ Done |
| 8 | `SL-2.6b` | **Checkout Saga — hoàn tất**: `confirm` (commit stock + voucher ⇒ `CONFIRMED`), `cancel` + **sweeper hết hạn 15′** (⇒ `EXPIRED`), idempotent theo saga state | M | Todo |
| 9 | `SL-2.7` | `payment-service` (8088, DB `payment`): payment intent + **mock provider** + webhook **idempotent** (`idempotency_keys`) | M | ✅ Done |
| 10 | `SL-2.8` | E2E checkout happy path (mở rộng smoke/script riêng) + OpenAPI + compose/CI matrix cho 5 service mới | M | Todo |
| 11 | `SL-2.9` | **Test nhánh fail**: hết hàng giữa chừng · payment fail · timeout ⇒ compensating chạy đúng (assert state thật) | M | Todo |
| 12 | `SL-2.10` | `docs/runbook-phase-2.md` + DoD Phase 2 (archive + tag `phase-2` + release) | S | Todo |
| 13 | `SL-2.11` | *(phát sinh từ `SL-2.0`)* `catalog-service`: **soft delete** product — `status = ARCHIVED` + `deleted_at`, `DELETE` không xoá row, list/get ẩn product đã xoá, `reserve` từ chối SKU archived | S | ✅ Done |
| 14 | `SL-2.12` | *(phát sinh từ `SL-2.2`)* `service-commons`: chuyển `AccessDeniedApiExceptionHandler` (403 cho `@PreAuthorize`) vào commons — xoá 2 bản copy ở catalog/inventory (**trả `TD-3`**) | S | Todo |
| 15 | `SL-2.13` | *(phát sinh từ `SL-2.6`, chốt phương án **B**)* `catalog-service`: `GET /v1/skus?ids=` — **batch** giá/tên SKU cho saga + cart (**điều kiện tiên quyết của `SL-2.6a`**) | S | ✅ Done |

**Phụ thuộc & thứ tự** (cập nhật sau `SL-2.1`): `SL-2.0` ✅ chốt mô hình → **`SL-2.11`** (soft delete — `catalog` phải
xong trước để inventory biết "SKU archived" nghĩa là gì) → `SL-2.2` + `SL-2.3` (inventory — chỗ dễ oversell nhất,
làm sớm) song song được với `SL-2.1`/`SL-2.4` → `SL-2.7` (payment) → `SL-2.5` (order) → **`SL-2.13`** (batch giá SKU —
điều kiện của saga) → `SL-2.6a` → `SL-2.6b` → `SL-2.8`/`SL-2.9` → `SL-2.10`.

**Debt sẽ trả trong phase này**: `TD-12` (soft delete product — **✅ đã trả ở `SL-2.11`**), `TD-4` (đưa
smoke vào CI khi đã có compose nhiều service — cân nhắc ở `SL-2.8`), `TD-3` (handler 403 → `commons` khi có service
thứ hai dùng `@PreAuthorize` — nhiều khả năng ở `SL-2.5`/`SL-2.7`).
**Debt mới phát sinh ở `SL-2.0`**: `TD-14` (chưa có job dọn `idempotency_keys`), `TD-15` (sweeper reservation chạy
trùng khi > 1 instance), `TD-16` (chưa có API restore product đã xoá mềm).

> Mỗi ticket vẫn phải viết **đặc tả BA đầy đủ** (In/Out · AC verify được · ghi chú kỹ thuật) **ngay trước khi bắt
> đầu** — bảng trên là phạm vi + thứ tự, không thay thế đặc tả (`AGENTS.md` §4, §5).
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
| **Trạng thái** | Todo |
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

- [ ] `confirm` khi payment `SUCCEEDED` ⇒ **200**, order `CONFIRMED`, reservation `COMMITTED`, voucher lock `COMMITTED`.
- [ ] `confirm` trên saga có `payment_required = false` (đơn 0 đồng, chốt ở `SL-2.6a`) ⇒ **200** và commit bình thường
      **không** cần gọi payment-service (không có intent để verify).
- [ ] `confirm` khi payment **chưa** `SUCCEEDED` ⇒ **409**, **không** commit gì (không tin lời client).
- [ ] `confirm` lần 2 ⇒ **200**, `stock_ledger`/voucher **không** thêm dòng commit thứ hai.
- [ ] `cancel` ⇒ reservation/voucher `RELEASED`, order `CANCELLED`; `cancel` sau `confirm` ⇒ **409**.
- [ ] Sweeper: saga quá 15′ ⇒ order `EXPIRED`, `reserved` giảm đúng `qty`, voucher trả lượt; chạy 2 vòng không bù 2 lần.
- [ ] Unit + IT xanh; `e2e-smoke` không regress.

**Ghi chú kỹ thuật**

- **Verify payment ở server, không tin client**: client gọi `/confirm` sau khi thanh toán, nhưng "đã trả tiền" phải đọc từ
  `payment-service` — nếu không, ai cũng confirm được đơn mà không trả đồng nào.
- **Commit theo thứ tự stock → voucher**: stock là tài nguyên khan hiếm (hết hàng = mất đơn), voucher sai thì sửa được.
- **Idempotent theo trạng thái saga**, không theo `Idempotency-Key`: confirm/cancel là thao tác trên **cùng** một đơn nên
  khoá tự nhiên là `orderId` + state hiện tại.

### Phase 3 — Async & event-driven
Mục tiêu: phần không critical chạy async, event không bị mất.

- `SL-3.x` Dựng message broker trong compose + convention đặt tên event
- `SL-3.x` **Transactional outbox** ở `order-service` và `payment-service` + relay
- `SL-3.x` Event envelope chuẩn + versioning + dedupe theo `eventId`
- `SL-3.x` `notification-service`: consumer gửi email/SMS (mock), retry + **DLQ**
- `SL-3.x` `search-service`: index product từ event, reconcile job khi index lệch
- `SL-3.x` Báo cáo: so sánh trước/sau khi chuyển sang async (latency checkout)

### Phase 4 — Scale & performance
Mục tiêu: có số liệu chứng minh mỗi pattern giúp gì.

- `SL-4.x` Load test harness bằng **k6** + baseline khi chưa tối ưu (p99, throughput, điểm vỡ)
- `SL-4.x` Cache-aside cho catalog + TTL + jitter + xử lý **cache stampede**
- `SL-4.x` **Rate limit** ở gateway (token bucket theo user/IP) + trả 429 có `Retry-After`
- `SL-4.x` **Read replica** + routing read/write, đo **replica lag**
- `SL-4.x` **Partition** bảng `orders` theo thời gian, đo cải thiện query
- `SL-4.x` Kịch bản **flash sale** 500k request/60s → báo cáo before/after
- `SL-4.x` Connection pooling: đo điểm gãy khi số connection vượt pool size

---

### Phase 5 — Resilience
Mục tiêu: hệ vẫn phục vụ luồng critical khi một phần chết.

- `SL-5.x` Timeout + **retry với exponential backoff + jitter** (chỉ cho operation idempotent)
- `SL-5.x` **Circuit breaker** cho `payment` và `inventory` client
- `SL-5.x` **Bulkhead**: pool riêng theo dependency
- `SL-5.x` **Backpressure** + queue depth limit + load shedding
- `SL-5.x` **Graceful degradation**: feature flag tắt phần non-critical khi quá tải
- `SL-5.x` Chaos test: kill dependency / inject latency → đo ảnh hưởng lên luồng checkout
- `SL-5.x` Runbook cho các sự cố phổ biến

### Phase 6 — Observability & SRE
Mục tiêu: nhìn thấy hệ thống đang làm gì mà không cần đọc log bằng mắt.

- `SL-6.x` **OpenTelemetry** tracing xuyên gateway → service → DB → broker
- `SL-6.x` Prometheus metrics (RED cho endpoint, USE cho hạ tầng) + Grafana dashboard
- `SL-6.x` Log tập trung (Loki) + query theo `traceId`
- `SL-6.x` Định nghĩa **SLI/SLO** + tính **error budget** tự động
- `SL-6.x` Alert symptom-based + link tới runbook
- `SL-6.x` Diễn tập: chạy load test rồi tìm nguyên nhân chậm chỉ bằng trace/metric

### Phase 7 — Deployment & infra
Mục tiêu: deploy lên K8s không downtime, rollback nhanh.

- `SL-7.x` Dựng K8s local bằng **kind** + Helm chart chung cho service
- `SL-7.x` Probe (liveness/readiness) + resource request/limit + **HPA**
- `SL-7.x` **Blue-green / canary** deploy + graceful shutdown hoàn chỉnh
- `SL-7.x` Config & secret management (ConfigMap/Secret), không nhúng secret vào image
- `SL-7.x` CD pipeline: build → push → deploy staging → approve → deploy prod
- `SL-7.x` Diễn tập rollback < 2 phút + báo cáo

### Phase 8 — Advanced
Mục tiêu: các pattern nâng cao, chỉ làm khi đã vững nền.

- `SL-8.x` **CQRS** + read model cho danh sách đơn của user
- `SL-8.x` `search-service` đầy đủ trên OpenSearch (filter, sort, facet, typo tolerance)
- `SL-8.x` **Sharding** dữ liệu theo `user_id` — thiết kế + demo
- `SL-8.x` **CDC** (Debezium) → `analytics-service` / warehouse
- `SL-8.x` Thêm **Kafka** cho event streaming, so sánh trade-off với RabbitMQ bằng số liệu
- `SL-8.x` Design doc **multi-region / DR** (không triển khai thật)
- `SL-8.x` `review-service`, `shipping-service`
- `SL-8.x` Security hardening: OAuth2/OIDC, mTLS nội bộ, secret rotation

---

## 6. Learning log

Ghi lại những thứ học được trong quá trình làm — đây là **output chính** của repo này.

| Ngày | Ticket | Học được |
|---|---|---|
| 2026-10-25 | — | Khởi tạo repo, viết `AGENTS.md`, `architecture.md`, `Backlog.md`. Chốt cách làm: cuốn chiếu theo ticket, không build một loạt. |
| 2026-11-02 | SL-0.6 | Cline Hooks/Plugins không chạy trên VS Code extension → dùng Rule + Skill + Workflow; skill auto-trigger nhờ `description` cụ thể. |
| 2026-11-02 | SL-0.1 | Chốt polyglot staged (Java-first + Go); trade-off rõ: 2 template/shared lib/CI. Lint/test/git-hooks phải đổi theo ngôn ngữ, không dùng tooling Node. |
| 2026-11-02 | SL-0.7 | Dev env = WSL2: Docker Desktop vốn đã dùng WSL2 backend → cần đưa source sang Linux fs (`~`), không để `/mnt/d/...`. |
| 2026-11-02 | SL-0.8 | Chọn GitHub Flow (không GitFlow) cho lab 1 người; repo đang ở `master` cần rename → `main`; PR cần remote + `gh`. |
| 2026-11-02 | SL-0.8 | Diff nhiễu **CRLF** (1253/1253 dòng) do thiếu `.gitattributes` + `core.autocrlf` unset → chuẩn hoá LF. **Branch protection/rulesets không dùng được trên repo private + GitHub Free (403)** → phải thay bằng git hook local. |
| 2026-11-02 | SL-0.2 | Verify trên repo **rỗng là vô nghĩa** — phải làm **test âm** mới biết config có tác dụng. `maven-checkstyle-plugin` **không** nhận `-Dcheckstyle.version` (dependency của plugin resolve từ POM của plugin) → phải khai báo `<plugin><dependencies>`; mặc định là checkstyle 9.3 (2022). Lefthook `pre-push` chỉ bảo vệ **local**. |
| 2026-11-02 | SL-0.3 | `docker compose` nạp `.env` theo **project directory** (= thư mục chứa compose file), **không** theo CWD → compose ở `infra/` mà `.env` ở gốc thì **buộc** `--env-file .env`. |
| 2026-11-02 | SL-0.3 | **Test âm chỉ đáng tin khi môi trường sạch**: biến đã export vào shell (`set -a; . ./.env`) làm Compose resolve từ env → test "bỏ `--env-file`" cho kết quả sai. Phải chạy `env -i`. |
| 2026-11-02 | SL-0.3 | Postgres `docker-entrypoint-initdb.d` **chỉ chạy khi volume rỗng** → sửa init script phải `down -v` mới thấy tác dụng. Redis `requirepass` ⇒ `redis-cli ping` trần trả `NOAUTH` (AC gốc cần ghi rõ điều kiện auth). RAM baseline cả stack ≈ **150 MiB** (RabbitMQ chiếm ~106 MiB — nặng nhất). |
| 2026-11-02 | SL-0.2 | Verify bằng tooling **cài tạm** không chứng minh được gì: `which mvn` vẫn trỏ bản Windows → phải cài trong WSL rồi kiểm tra `which mvn`. Git hook nằm trong `.git/hooks` (không commit) → người clone mới phải tự `lefthook install`, nếu không mọi bảo vệ **im lặng** mất tác dụng. Test âm bằng file bị `.gitignore` ignore là test **vô hiệu**. |
| 2026-11-02 | SL-0.4 | **`BUILD SUCCESS` không có nghĩa là đã test**: surefire mặc định 2.17 không hỗ trợ JUnit Platform → `Tests run: 0` mà vẫn xanh. Pin `3.6.1`. Bỏ `spring-boot-starter-parent` (import BOM) thì phải tự bật `-parameters` — thiếu nó là **lỗi runtime**, không phải lỗi compile. |
| 2026-11-02 | SL-0.4 | Spotless (`{}`) và Checkstyle `WhitespaceAround` (`{ }`) **xung đột trực tiếp** ở block rỗng → phải bật `allowEmpty*`. Lint/format chỉ được kiểm chứng trên **code thật**. |
| 2026-11-02 | SL-0.4 | Graceful shutdown: nghe `ContextClosedEvent` là **quá muộn** (Boot drain xong mới đóng context) → phải dùng `SmartLifecycle` phase `Integer.MAX_VALUE`. Record trả ra API: tên method `isXxx()` bị Jackson coi là getter → lộ field thừa. |
| 2026-11-02 | SL-0.4 | Log JSON: logstash encoder mặc định nhét **mọi** property logger context + field `tags` không nhất quán → phải tắt `includeContext`/`includeTags` để schema log đồng nhất. Health check TCP **không chặn được DNS resolution** (timeout chỉ áp cho `connect`). |
| 2026-11-02 | SL-0.4 | **AC "đã thử làm theo và chạy được" phải thực thi thật**: copy template theo README → build đỏ vì test hard-code tên service. Sửa thành đọc từ `@Value("${spring.application.name}")`. Bug `-parameters` và bug này đều **không lộ** qua 72 test tự động — chỉ lộ khi curl tay / làm theo hướng dẫn. |
| 2026-11-03 | SL-0.5 | **CI cũng phải test âm mới tin**: run xanh chỉ chứng minh pipeline *chạy*, không chứng minh nó *chặn*. Phải cố tình vi phạm Checkstyle (→ `lint` đỏ, log hiện `file:line`) và cố tình fail test (→ `test` đỏ) mới thấy `needs:` hoạt động (`build-image` = `skipped`). |
| 2026-11-03 | SL-0.5 | **Chọn vi phạm cho test âm phải sống sót qua tooling phía trước**: `UnusedImports` bị Spotless (google-java-format) tự xoá ngay ở hook `pre-commit` → test âm tự chết trước khi tới CI. Dùng `NeedBraces` (formatter không sửa được) mới tới được CI. |
| 2026-11-03 | SL-0.5 | **Job `test` phải skip lint để không chạy 2 lần**: `mvn verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` — đo trước khi viết workflow (log hết "Starting audit..." nhưng `jacoco:check` vẫn chạy). Không suy đoán "chắc là skip được". |
| 2026-11-03 | SL-0.5 | **Hai token một máy, scope khác nhau**: token VS Code cấp qua `GIT_ASKPASS` có `workflow` (push được `.github/workflows/*`), token `gh` CLI **không**. Kiểm tra bằng `git credential fill` + `curl -D - -H "Authorization: token $TOKEN" https://api.github.com/user` → header `x-oauth-scopes` (không in token ra). |
| 2026-11-03 | SL-0.5 | Actions trên repo **private** tính theo phút (Free 2.000/tháng) → ràng buộc `needs:` giúp 2 run đỏ chỉ tốn 1m12s / 35s thay vì ~2m35s mỗi run. Badge repo private chỉ render khi đã login GitHub. |
| 2026-11-03 | SL-0.5 | **`concurrency` gom push + PR chung một group là bẫy**: run PR huỷ run push của **cùng commit** → commit có check `cancelled` (thấy bằng `gh api .../commits/<sha>/check-runs`), PR hiện như fail dù code sạch. Gom theo `github.ref` (push = `refs/heads/x`, PR = `refs/pull/N/merge`) thì 2 run độc lập, không huỷ nhau. **Tối ưu "ít run" có thể đánh đổi bằng tín hiệu sai trên PR.** |
| 2026-11-03 | SL-0.5 | **Badge workflow resolve theo default branch**: `actions/workflows/ci.yml/badge.svg` trả **404** khi `ci.yml` chưa nằm trên `main` (thử cả Bearer lẫn basic auth đều 404) → AC badge chỉ verify được **sau merge**. Đừng hứa "badge xanh" trước khi merge. |
| 2026-11-03 | SL-0.5 | **AC phụ thuộc trạng thái `main` (badge, branch protection…) thì phải merge mới verify được** → ticket phải để `Review` chứ không `Done` trước merge. Và repo **private** thì SVG badge không lấy được bằng curl (chỉ browser đã login) — trạng thái phải suy từ API run mới nhất của `main`. |
| 2026-11-03 | process | **Backlog dài → tách archive theo phase**: ticket đã xong vẫn có giá trị (verify output + learning note) nhưng làm loãng backlog sống → tách sang `Backlog-phase-<N>.md`, giữ **Learning log + Changelog** ở `Backlog.md` làm chỉ mục xuyên phase. Kết quả: `Backlog.md` 1267 → **260 dòng**, archive 1057 dòng, không mất nội dung (8/8 `### SL-`). |
| 2026-11-03 | process | **Commit minor không cần nhánh/PR riêng**: đóng ticket / update backlog / sửa doc → gộp vào nhánh ticket kế tiếp, tránh PR chỉ chứa doc. Rủi ro cần để ý: phần đóng ticket "trễ" một nhịp, nên **ticket chỉ được coi là `Done` khi PR chứa commit đóng ticket đã merge**. |
| 2026-11-03 | process | **Lưu trữ snapshot theo phase = tag + Release + doc snapshot, KHÔNG giữ nhánh thường trực**: tag cho phép `git switch -c maint/phase-N phase-N` bất cứ lúc nào ⇒ nhánh thường trực **không thêm khả năng nào**, chỉ thêm rot + phân kỳ doc. Gọi là "lazy D" (ADR-0005). Tag **không** trigger CI (workflow chỉ khai `on.push.branches`) ⇒ 0 phút Actions; `git diff phase-N phase-M --stat` là công cụ học rất tốt (thấy đúng phase sau thêm gì). |
| 2026-11-03 | SL-1.0 | **`paths` filter trên `pull_request` đánh giá theo TOÀN BỘ diff của PR** (không phải commit cuối) → commit docs trong PR có code vẫn kích hoạt workflow. Muốn commit docs "miễn phí" thì phải là PR chỉ-docs hoặc `[skip ci]`. |
| 2026-11-03 | SL-1.0 | **Job `skipped` = 0 phút, nhưng REST jobs API trả `started_at` GIẢ cho job skipped** (started == completed) → đo chi phí bằng REST sẽ tính dư ~1 phút/job. Dùng GraphQL (`gh run view --json jobs`, `started_at = null`) hoặc lọc `conclusion == 'skipped'`. |
| 2026-11-03 | SL-1.0 | **Đo trước khi tối ưu, đừng đoán**: 71 phút/18 run với 62% là run docs-only — trực giác nghĩ "build-image đắt nhất" (đúng 46%) nhưng thứ **đáng bỏ** nhất là run docs-only. Re-run cũng **đánh giá lại `vars`** → test kill-switch không cần commit giả. |
| 2026-11-05 | SL-1.1 | **Spring Boot 4 tách auto-configuration theo công nghệ**: khai `flyway-core` trần ⇒ Flyway **không chạy** (im lặng!) → Hibernate `validate` báo `missing table [users]`. Phải dùng `spring-boot-starter-flyway`. **Dependency mới phải kiểm chứng bằng log, không chỉ "build xanh".** |
| 2026-11-05 | SL-1.1 | **`spring-boot:repackage` chạy TRƯỚC failsafe** ⇒ failsafe lấy classpath từ jar "fat" → `NoClassDefFoundError` cho class của chính module; fix bằng `<classesDirectory>${project.build.outputDirectory}</classesDirectory>`. Kèm: **Jackson 3** đổi package (`tools.jackson.databind`). |
| 2026-11-05 | SL-1.1 | **Non-root container + bind-mount secret = uid mismatch**: private key mode 600 của host (uid 1000) không đọc được bởi uid hệ thống trong image → chạy container bằng `user: "${SHOPLAB_UID}:${SHOPLAB_GID}"`. Và **mọi Dockerfile phải copy pom của TẤT CẢ module** — thêm module thứ 3 làm build image của `service-template` chết với `Child module ... does not exist`. |
| 2026-11-05 | SL-1.1 | **Spring Security 7 coi `iss` là URL** (`Jwt.getIssuer()` ném lỗi nếu không parse được) → đặt `iss` dạng URI. **Test với `Clock` cố định**: decoder phải dùng cùng clock, nếu không test "đỏ vì đồng hồ" chứ không phải vì logic sai. |
| 2026-11-05 | SL-1.2 | **Gateway tự viết: khớp route phải theo RANH GIỚI segment**, không dùng `startsWith` thô — prefix `/v1/auth` mà khớp `/v1/authz` là bug kinh điển. Test riêng case này rẻ hơn debug sau. |
| 2026-11-05 | SL-1.2 | **Header danh tính (`x-user-*`) phải bị XOÁ chứ không chỉ ghi đè**: chỉ `setHeader` ở chỗ forward thì client vẫn lọt được trên route public. Bọc request (`UserHeaderRequest`) để **mọi** đường đọc header thấy đúng giá trị do gateway quyết định. |
| 2026-11-05 | SL-1.2 | **Cache JWKS cần cả TTL lẫn "nạp lại khi gặp `kid` lạ" — và phải CHẶN DỘI**: token với `kid` rác là input miễn phí, không chặn thì mỗi request thành 1 lần gọi identity (tự DoS). Lần viết đầu tôi quên chặn dội; test `shouldThrottleForcedRefresh` mới lộ. |
| 2026-11-05 | SL-1.2 | **401 và 503 khác nhau về ngữ nghĩa**: token sai = lỗi client (đừng retry); không lấy được JWKS / service đích chết = lỗi hạ tầng (retry được). Gộp thành 401 khiến client đăng nhập lại vô ích khi identity chỉ đang khởi động. |
| 2026-11-05 | SL-1.2 | **Algorithm confusion**: chặn `alg != RS256` **trước** khi tra khoá — token HS256 ký bằng chính public key có thể lọt qua thư viện verify lỏng. |
| 2026-11-05 | SL-1.2 | **Bẫy Dockerfile lặp lần 2**: thêm module Maven ⇒ **mọi** Dockerfile phải thêm 1 dòng `COPY …/pom.xml` (lần này `identity-service` + `service-template` cùng vỡ). Kiểm nhanh: `grep -c 'COPY services/' services/*/Dockerfile`. |
| 2026-11-06 | SL-1.6 | **Planner bỏ index trên bảng nhỏ KHÔNG có nghĩa index sai**: 20 row ⇒ seq scan rẻ hơn index scan. Muốn chứng minh index dùng được thì `SET enable_seqscan=off` + EXPLAIN và **ghi rõ đã ép**; còn `order by created_at desc limit 5` thì planner **tự** chọn `Index Scan Backward`. EXPLAIN luôn phải kèm bối cảnh dữ liệu + query cụ thể. |
| 2026-11-06 | SL-1.6 | **`LIKE '%q%'` không dùng được btree** — functional index `lower(name)` chỉ cứu **prefix** search. Search giữa chuỗi ở scale lớn cần `tsvector`/trigram hoặc search engine (Phase 3/8); thêm index nhưng ghi rõ giới hạn thay vì tuyên bố "đã tối ưu". |
| 2026-11-06 | SL-1.6 | **Repeatable migration (`R__`) phải idempotent**: Flyway chạy lại khi checksum đổi ⇒ `ON CONFLICT DO NOTHING` + UUID cố định. Verify bằng **restart thật** rồi so số row (5/20/24 → 5/20/24), không tin "đọc code thấy đúng" — và phải test cả `down -v && up -d`. |
| 2026-11-06 | SL-1.6 | **Whitelist sort = chặn SQL injection ở dạng khó thấy** (`sort` đi thẳng vào `ORDER BY`). Value client gửi chỉ được **tra map**, không ghép chuỗi; test `sort=drop table` phải assert **400**, không phải "không crash". |
| 2026-11-06 | SL-1.6 | **List và detail phải khác DTO**: list trả product **không** kèm SKU (tránh N+1: 20 product × 1 query), detail trả kèm `skus`. Dùng chung DTO là tự tạo N+1 mà không ai thấy cho tới khi tải cao. |
| 2026-11-06 | SL-1.6 | **Validate query param ở tầng parse (`ProductQuery`) thay vì để Spring bind**: bind sai kiểu của Spring trả 400 nhưng body **không phải `ApiError`** ⇒ client phải xử lý 2 format lỗi. Tách record còn cho 20 unit test chạy 0,2s thay vì test qua HTTP. |
| 2026-11-06 | SL-1.7 | **Smoke test phải random "đủ mạnh"**: timestamp **giây** không đủ khi test chạy <1s ⇒ 2 lần chạy cùng giây đụng unique index (`slug`) ⇒ **flaky** (đo được 9 PASS/4 FAIL). Mọi unique constraint trong test (email/slug/code) phải có entropy thật (`$RANDOM`/UUID). |
| 2026-11-06 | SL-1.7 | **`curl -o <file>` KHÔNG mở file khi connection bị từ chối** ⇒ file giữ nội dung lần trước ⇒ bước fail in **body cũ** (`{"status":"UP"}` trong khi service đã chết). Luôn `: > "$TMP_BODY"` trước mỗi request — bug "báo sai chỗ" nguy hiểm hơn bug báo đúng. |
| 2026-11-06 | SL-1.7 | **`status=000` và `503` chỉ ra 2 tầng khác nhau**: `000` = client không kết nối được (service chết/cổng sai) · `503` = gateway sống nhưng không tới được service đích. Runbook phải dạy đọc **cặp mã** này thay vì chỉ nói "không có 200". |
| 2026-11-06 | SL-1.7 | **Test âm phải nằm chung script với happy path**: bước "buyer tạo product → **403**" là PASS khi trả 403. Tách ra script riêng thì không ai chạy ⇒ phân quyền hỏng mà không ai biết. |
| 2026-11-06 | SL-1.7 | **Runbook chỉ đáng tin khi đã dùng nó để debug thật**: `docs/runbook-phase-1.md` §5 dán **nguyên văn** output của lần giả lập lỗi (stop catalog → fail 1c/8 → `docker ps` → `up -d`), không phải ví dụ viết tay. |


| 2026-11-06 | SL-1.8 | **Version springdoc phải tra bảng tương thích, không đoán**: Boot **4.1.x** ⇒ springdoc **3.x** (3.1–3.2); dòng 2.x là cho Boot 3. Kiểm nhanh: `maven-metadata.xml` trên Maven Central + bảng Boot↔springdoc trong FAQ. Đoán sai tốn cả vòng build image (~5 phút). |
| 2026-11-06 | SL-1.8 | **Tài liệu lỗi sinh bằng `OperationCustomizer`, không rải annotation**: 400/401/403/404/409 là hành vi của *toàn hệ* (`GlobalExceptionHandler` + security chain) ⇒ một customizer ở commons làm endpoint mới **tự** có, không phụ thuộc dev nhớ annotation. |
| 2026-11-06 | SL-1.8 | **`$ref` không tự sinh schema**: phải tự thêm `ApiError` vào `components.schemas` (`ModelConverters.readAll`) thì ref mới resolve. Ref treo = spec trông đúng nhưng generate client sẽ vỡ. |
| 2026-11-06 | SL-1.8 | **Bật/tắt spec phải fail-closed + verify bằng env thật**: dùng `EnvironmentPostProcessor` (chạy SAU config data, nếu không bị yml ghi đè) ép `false` khi `APP_ENV != local`; verify bằng **chạy container `APP_ENV=prod`** rồi curl `/v3/api-docs` → 404, không chỉ đọc code. |
| 2026-11-06 | SL-1.8 | **Export phải deterministic mới diff được**: sort `paths`/`schemas`/`tags` trước khi ghi YAML — nếu không, lần export nào cũng đổi thứ tự ⇒ git diff rác ⇒ không ai review spec. |
| 2026-11-06 | SL-1.8 | **Gateway không nằm trong spec nghiệp vụ**: nó là proxy catch-all ⇒ `@Hidden` + export chỉ merge identity/catalog. Vẫn giữ `/v3/api-docs` của gateway (200) để debug route, nhưng không trộn vào contract client. |
| 2026-11-06 | SL-1.8 | **Thêm annotation qua script phải chạy `spotless:apply` ngay**: import/annotation chèn bằng python làm `spotless:check` fail **bên trong `docker build`** (image đỏ, không phải CI đỏ) — sửa bằng `mvn spotless:apply` trước khi build lại. |

| 2026-11-06 | SL-1.15 | **Comment "không dùng cho prod" không phải chốt chặn**: seed nằm chung `db/migration` ⇒ Flyway chạy ở MỌI môi trường. Fail-closed = **mặc định không có** (image chỉ trỏ `db/migration`), dev **tự thêm** location; verify bằng container `APP_ENV=prod` trên **DB mới** rồi đếm row. |
| 2026-11-06 | SL-1.15 | **Đổi định dạng dữ liệu sinh ra (slug) làm đỏ test phụ thuộc dữ liệu sẵn có**: `"Điện thoại"` → `dien-thoai` trùng category seed ⇒ 2 IT fail. Test tạo entity có unique constraint phải **tự sinh hậu tố ngẫu nhiên**. |
| 2026-11-06 | SL-1.15 | **Transliterate tiếng Việt: NFD không đủ** — `đ/Đ` là chữ riêng, không tách được bằng `\p{M}` nên phải map tay; test phải có `Điện`/`Đồ`/`Đỏ`. |
| 2026-11-06 | SL-1.17 | **Contract ≠ danh sách endpoint kỹ thuật**: `/debug/*` có thật nhưng không phải API cho client ⇒ `@Hidden`. Phân biệt "hạ tầng để verify" (ẩn) với "hạ tầng để vận hành" (`/health`, `/ready`, `/version` — GIỮ, runbook dùng). |
| 2026-11-06 | SL-1.16 | **Nợ kỹ thuật phải có điều kiện revisit verify được** ("khi đo được p99 ở dữ liệu lớn", không phải "khi rảnh") và phải nằm ở **một bảng**. Learning note = đã hiểu gì; debt = còn phải làm gì — để chung thì không ai lọc được. |
| 2026-11-06 | SL-1.18 | **Grace window phải nối chuỗi, không tạo nhánh**: cấp token mới mà không set `replaced_by` thì `revokeChain` sau này không đi hết chuỗi ⇒ mất khả năng thu hồi. Grace rotate **token cuối chuỗi** (có chặn hop). |
| 2026-11-06 | SL-1.18 | **Grace không được hồi sinh phiên đã logout**: điều kiện phải là "đã rotate" (`replacedBy != null`), không phải "đã revoke" — logout cũng set `revokedAt`. Test thời gian: unit dùng `Clock.fixed` + grace tham số (0s/31s, không sleep), IT cố định `grace=0s`, chỉ stack thật mới chờ 31s. |
| 2026-11-05 | process | **Push race với lúc merge ⇒ mất commit**: commit cuối của `SL-1.1` (`feda53a` — status Done + block CI evidence + changelog) push lên đúng lúc user merge; squash merge dùng state **trước** commit đó, rồi nhánh bị xoá ⇒ thay đổi biến mất khỏi `main`. **Bài học**: sau khi merge phải `git show <merge-sha> --stat`/`grep` kiểm nội dung trên `main`, đừng tin "đã push là xong"; commit bị mất vẫn cứu được qua reflog nếu chưa GC. Đã khôi phục trong nhánh `SL-1.2`. |
| 2026-11-05 | SL-1.9 | **`paths-ignore` và `[skip ci]` bổ sung nhau, không thay thế nhau**: `paths-ignore` đánh giá theo **toàn bộ diff của PR** → commit docs trong PR **có code** vẫn tốn 1 run. `[skip ci]` xử lý đúng case đó. Nhưng GitHub đọc skip instruction ở **HEAD commit của PR** ⇒ docs commit ở cuối PR skip luôn run của cả PR (check treo `Pending`) → đừng tin nó, hãy kiểm bằng **diff thật**. |
| 2026-11-05 | SL-1.9 | **Guard cũng phải test theo đúng thứ tự**: lần đầu test guard "PR draft" **sau khi** đã chạy `gh pr ready` ⇒ tưởng guard hỏng, thực ra kịch bản test sai. Test guard cần kịch bản cẩn thận y như test code. |
| 2026-11-05 | process | **Merge tay là chỗ dễ sai nhất**: PR #9/#10 merge tay ⇒ không tái lập được, và đã một lần mất nội dung (push race ở `SL-1.1`). Từ `SL-1.9` mọi lần merge đi qua **một** chốt: `tools/merge-pr.sh` (kiểm CI xanh + comment tóm tắt + diff sau CI là docs-only) → squash + xoá nhánh + **kiểm `main` có nội dung**. |
| 2026-11-05 | SL-1.10 | **`gh pr merge --squash` ghép message từ PR, KHÔNG phải từ commit HEAD** (title PR + subject của mọi commit trong PR) ⇒ token `[skip ci]` của commit docs **lọt vào merge commit** → push lên `main` bị skip, mất luôn lần kiểm chứng `main`. Fix: truyền `--subject`/`--body` tường minh + lọc token skip. |
| 2026-11-05 | process | **Rule mới phải kiểm cả tác dụng phụ**: `[skip ci]` (SL-1.9) tiết kiệm CI đúng thiết kế nhưng đồng thời tắt luôn CI của `main`. Checklist sau merge giờ có **2 mục**: squash message không chứa token skip **và** `main` có run mới. |
| 2026-11-05 | process | **Sửa bảng markdown: đừng dùng đoạn ĐẦU của dòng làm `old_text`** — công cụ thay thế khớp prefix, để lại phần đuôi dính vào dòng mới (dính 2 lần ở Changelog). Dùng **cả dòng** hoặc `insert_line`, rồi kiểm lại bằng `grep -c '^| <ngày> | \`<ticket>\`'`. |
| 2026-11-05 | SL-1.11 | **Tín hiệu sai tệ hơn không có tín hiệu**: dòng chẩn đoán đếm substring `skip ci` in `1` khi body chỉ chứa **câu ví dụ** → tôi tưởng fix `SL-1.10` hỏng, tốn 1 vòng kiểm. Công cụ phải in đúng **định nghĩa** (token `[skip ci]`), và bằng chứng thật là `gh run list --branch main`. |
| 2026-11-05 | process | **Chạy script/test từ đúng working tree**: 2 lần test guard bằng bản script ở nhánh khác (chưa có fix) → kết quả cũ, lại tưởng guard sai. Kiểm `git branch --show-current` trước khi kết luận. |
| 2026-11-05 | process | **Chốt chặn CI phải rà lại mỗi khi thêm service**: job `build-image` viết ở Phase 0 (khi chỉ có `service-template`) → thêm 2 service thật mà không cập nhật ⇒ CI xanh nhưng **Dockerfile của 2 service thật không được kiểm** = **chốt chặn giả**. Thêm service ⇒ rà **mọi** job CI (cả `test` *và* `build-image`). |
| 2026-11-05 | process | **Đóng ticket ngay trong PR của nó** (mark done gộp vào commit cuối) thay vì để dành sang PR sau: bớt 1 nhịp chờ, bớt 1 commit, không dính bẫy `[skip ci]` ở HEAD. Đánh đổi: `main` chỉ thấy 1 commit/ticket. |
| 2026-11-05 | SL-1.14 | **Phân biệt "0 run vì hợp lệ" vs "0 run vì bị skip"**: cùng là *không có run*, nhưng PR chỉ-docs là bình thường còn code bị skip là nguy hiểm. Phân biệt bằng **diff của PR**, không bằng "có run hay không". |
| 2026-11-05 | SL-1.14 | **Tự giẫm bẫy mình vừa viết ra**: commit message giải thích *"nên KHÔNG dùng …"* lại chứa **token skip dạng literal** ⇒ GitHub skip run của chính commit đó (dù diff có `ci.yml`). Tài liệu hoá một cơ chế thì phải biết nó **đọc ở đâu** — ở đây là **cả body**, không chỉ subject. |
| 2026-11-05 | SL-1.14 | **GitHub API không đáng tin tuyệt đối**: trong ~15 phút gặp 3 lỗi tạm thời (`gh pr create` GraphQL *Something went wrong*, `gh pr close` 502, `gh pr merge` GraphQL) — chạy lại là được. **Debt**: `merge-pr.sh` chưa có retry, hiện phải chạy lại tay. |
| 2026-11-05 | SL-1.12 | **Chốt chặn giả sống được 2 ticket**: `build-image` "xanh" suốt `SL-1.1`+`SL-1.2` mà chỉ kiểm 1/3 service — không ai đọc lại job CI khi thêm service. **Song song ≠ rẻ**: 3 image chạy song song giữ nguyên thời gian *chờ* nhưng nhân 3 phút *billable* — đo bằng số, không bằng cảm giác. |
| 2026-11-05 | SL-1.13 | **Service optional ⇒ dùng compose profile chứ đừng xoá/để mặc định**: profile giữ được "chốt chặn sống" (template vẫn build/chạy được khi cần) mà `up -d` không tốn RAM/build cho nó. |
| 2026-11-05 | SL-1.3 | **Smoke script phải chịu được hạ tầng chết**: `set -e` + curl connection-refused (exit 7) kill script trước khi báo lỗi → `curl \|\| true` để lấy status `000`, guard token rỗng để `python3` không KeyError. Bonus: **GNU date `%3N` ra 9 chữ số** (nanosec) trên hệ này → dùng `%s%N`/1e6. |
| 2026-11-05 | SL-1.4 | **`@Transactional` + throw = rollback xoá luôn việc vừa ghi**: reuse detection revoke cả chuỗi rồi throw 401 ⇒ mất revoke. Phải `noRollbackFor` ở **mọi tầng** `@Transactional` mà exception đi qua (tầng ngoài mới quyết định commit). **IT bắt được, unit test mock thì không.** |
| 2026-11-05 | SL-1.4 | **Endpoint mới phải verify qua CỬA VÀO DUY NHẤT**: `/v1/admin/users` 200 khi gọi thẳng identity nhưng **404 qua gateway** (thiếu route) — chỉ lộ khi test qua 8080. Và security filter đổi hành vi path lạ 404 → **401** (có chủ ý: không lộ route). |
| 2026-11-06 | SL-1.5 | **Method security ném exception SAU DispatcherServlet** ⇒ `@PreAuthorize` từ chối thành **500** (không phải 403) vì `GlobalExceptionHandler` bắt `Exception`. Mỗi lớp chặn quyền phải có handler riêng. IT bắt được, curl tay dễ bỏ qua. |
| 2026-11-06 | SL-1.5 | **`@Version` chỉ tăng lúc flush** ⇒ trả DTO trước flush là trả version CŨ (0) ⇒ client update lần sau bằng version sai. Giá trị do DB sinh (version/default/trigger) phải đọc **sau flush** — `saveAndFlush`, và phải test bằng IT vì mock không mô phỏng flush. |
| 2026-11-06 | SL-1.5 | **`docker build` không có Docker socket ⇒ `-DskipITs` cũng đổi kết quả gate coverage** (image build đỏ dù code đúng). Tách vai: gate chất lượng ở job `test` (chạy đủ IT), image build `-Djacoco.skip=true`. |
| 2026-11-06 | SL-1.5 | **Gateway `public-paths` theo PATH, không theo METHOD**: service có "GET public + POST cần token" trên cùng prefix thì gateway thả cả prefix và **service tự chặn** phần ghi. Chỉ chấp nhận được vì service tự verify JWT; nếu tin header gateway thì đó là lỗ hổng. |
| 2026-11-06 | SL-1.5 | **Verify script là code**: bash heredoc + 3 lớp escaping làm token bị cắt → **17 FAIL giả**. FAIL hàng loạt cùng kiểu (401/None) ⇒ nghi script trước, sản phẩm sau. Python `urllib` cho 36 PASS. |
| 2026-11-05 | process | **Đừng dùng backtick trong `--body` của `gh` khi gõ trực tiếp**: shell thực thi phần trong backtick (`tools/merge-pr.sh` → `Permission denied`, `null: command not found`) và **body PR bị hỏng**. Luôn dùng `--body-file` với heredoc (`<<'EOF'`). |
| 2026-11-07 | SL-2.0 | **"Kiểm tra rồi mới ghi" luôn có khe hở** ⇒ chống oversell phải nhét điều kiện vào chính câu `UPDATE` (`WHERE on_hand - reserved >= :qty`) để DB quyết định trong cùng row lock. `SELECT ... FOR UPDATE` + `if` trong app vẫn đúng nhưng giữ lock lâu hơn, và deadlock nếu không sắp thứ tự `sku_id`. |
| 2026-11-07 | SL-2.0 | **Idempotency guard phải "bền" hơn dữ liệu nó bảo vệ**: guard ở Redis trong khi order ở Postgres ⇒ restart/evict mất dedupe đúng lúc client retry. Retry nên **replay response cũ** (cùng `orderId`) chứ không trả lỗi; đổi lại phải trả giá 1 bảng + job dọn (`TD-14`). |
| 2026-11-07 | SL-2.0 | **`order_items` snapshot không phải chuyện "chuẩn hoá hay không" mà là "hoá đơn có bất biến hay không"**: DB-per-service đã cấm FK xuyên service, nhưng kể cả được phép thì đọc live từ `catalog` vẫn làm đơn cũ đổi giá theo thời gian. |
| 2026-11-07 | SL-2.0 | **Chốt ADR trước khi code là rẻ nhất**: 5 quyết định trong 1 file ~250 dòng thay cho việc sửa migration + API + test ở 5 service. Mục **Revisit viết bằng số** (RPS, số row, % fail) — nếu không thì lần sau lại tranh luận từ đầu. |
| 2026-11-07 | SL-2.11 | **Soft delete là quyết định về *đọc*, không phải về *xoá***: khó nhất là mọi đường đọc phải nhớ `deleted_at is null`. Đặt tên method theo hành vi (`findByIdAndDeletedAtIsNull`) để chỗ bỏ sót lộ ra ngay khi review, thay vì `if` rải rác trong service. |
| 2026-11-07 | SL-2.11 | **Bất biến nên nằm ở DB**: CHECK `(status='ARCHIVED') = (deleted_at IS NOT NULL)` từ chối cả 2 chiều sai — đã chứng minh bằng 2 lệnh `UPDATE` bị Postgres chặn. Service có bug thì dữ liệu vẫn không thành "nửa sống nửa chết". |
| 2026-11-07 | SL-2.11 | **Idempotent ở domain ≠ idempotent ở API**: `archive()` gọi lại vẫn giữ `deleted_at` gốc (audit), nhưng API trả 404 ở lần xoá thứ 2 vì `require()` đã lọc product đã xoá. Hai tầng phục vụ 2 mục đích — đừng gộp thành một. |
| 2026-11-07 | SL-2.11 | **IT không được assert aggregate toàn bảng**: `count(*) where status<>'ACTIVE'` = 0 fail vì test khác đã archive product trước đó (thứ tự test không đảm bảo). Chỉ assert trên dữ liệu chính test đó tạo, hoặc trên metadata (`information_schema`). |
| 2026-11-07 | SL-2.11 | **Test âm phải đúng trạng thái**: thử CHECK constraint trên row *đã* ARCHIVED ⇒ `set deleted_at` lần nữa vẫn hợp lệ, tưởng constraint không chặn. Phải tạo product **ACTIVE** rồi mới thử. |
| 2026-11-07 | SL-2.2 | **Điều kiện nằm trong câu `UPDATE` là toàn bộ cơ chế chống oversell**: `WHERE on_hand - reserved >= :qty` để Postgres quyết định trong cùng row lock; `rowsAffected = 0` là tín hiệu hết hàng. Không cần `SELECT FOR UPDATE`, không cần distributed lock ⇒ không có khe hở giữa kiểm tra và ghi. |
| 2026-11-07 | SL-2.2 | **Idempotency có thể đến từ ràng buộc dữ liệu**: unique `(order_id) WHERE status='PENDING'` biến retry cùng `orderId` thành "tìm thấy row cũ" thay vì phải tự viết bảng dedupe như `idempotency_keys`. |
| 2026-11-07 | SL-2.2 | **`@Transactional` mặc định rollback ⇒ mất phần bù trừ**: nhánh "commit khi hết hạn" vừa release stock vừa ném 410; nếu không khai `noRollbackFor` thì phần release bị xoá ⇒ hàng treo vĩnh viễn mà test HTTP vẫn thấy 410 "đúng". |
| 2026-11-07 | SL-2.2 | **`mvn -pl <module> verify` không build module phụ thuộc** ⇒ phải `-am`; nếu không sẽ báo thiếu `shoplab-service-commons:jar` — lỗi trông như lỗi cấu hình repo chứ không phải lỗi code. |
| 2026-11-07 | SL-2.3 | **Lazy expire và sweeper phục vụ 2 nhánh khác nhau — phải có cả hai**: lazy xử lý "client gọi muộn", sweeper xử lý "client không bao giờ gọi" (bỏ ngang). Chỉ lazy ⇒ hàng treo vĩnh viễn; chỉ sweeper ⇒ commit ở phút 15:01 bị trả 200 sai. |
| 2026-11-07 | SL-2.3 | **`FOR UPDATE SKIP LOCKED` biến "chạy N instance" thành chuyện của DB**: không cần advisory lock / leader election. Nhưng **không đủ một mình** — vẫn cần conditional `markStatus` làm chốt thứ hai cho lúc client commit chen vào giữa. |
| 2026-11-07 | SL-2.3 | **Job nền phải "chết đúng cách"**: bắt `RuntimeException` để 1 nhịp lỗi không giết scheduler, và `spring.task.scheduling.shutdown.await-termination` để shutdown không cắt transaction đang chạy (nếu cắt: `Connection is closed` + `JDBC rollback failed`). IT còn cần `@DirtiesContext(AFTER_CLASS)` để context đóng **trước** khi Testcontainers dừng Postgres. |
| 2026-11-07 | SL-2.3 | **Bằng chứng oversell phải là số tuyệt đối**: "10 thành công / 90 bị từ chối" + `Σ delta_reserved = reserved` kiểm bằng SQL. Chỉ assert `reserved <= on_hand` thì code "giữ thiếu hàng" vẫn qua. Và test đua phải dùng `CountDownLatch` + pool đủ lớn, nếu không request xếp hàng thì không còn đua. |
| 2026-11-07 | SL-2.4 | **Copy service là con đường ngắn nhất để tạo nợ ẩn**: 3 lỗi của ticket này đều từ copy (`COPY pom` thiếu trong Dockerfile, `<name>` pom vẫn là Inventory, comment nói "inventory"). Checklist khi tạo service mới: pom (artifactId/**name**/jacoco include) · Dockerfile (đủ **mọi** pom module) · application.yml (DB + port + prefix property) · compose/gateway/CI/env. |
| 2026-11-07 | SL-2.4 | **Cùng một pattern, lần thứ hai rẻ hơn nhiều**: lock/commit/release của voucher chỉ tốn ~1/2 thời gian so với inventory vì đã có khuôn (conditional `UPDATE`, compare-and-set status, `noRollbackFor` cho nhánh vừa-trả-vừa-ném, sweeper `SKIP LOCKED`). Đó là lợi ích thật của việc làm đúng ngay từ service đầu. |
| 2026-11-07 | SL-2.4 | **Bất biến `used + locked <= quota` phải ở DB**: kiểm trong code thì một đường ghi mới (script/migration/service khác) là bán quá quota; CHECK constraint là thứ duy nhất không thể quên. |
| 2026-11-07 | SL-2.4 | **Snapshot tiền tại lúc lock**: giảm giá đã hứa với client phải bất biến — commit dùng `discount` trong `voucher_locks`, không tính lại từ `vouchers.value` (admin sửa `value` giữa chừng sẽ làm hoá đơn sai). Đánh đổi cần nhớ: `subtotal` hiện do order-service gửi; nếu API lộ ra client thì phải tính lại server-side. |
| 2026-11-07 | SL-2.5 | **`REQUIRES_NEW` lồng trong transaction là bẫy pool**: nó cần connection **thứ hai** trong khi connection thứ nhất chưa nhả ⇒ pool 5 + 20 request song song = deadlock (HTTP 500 dù logic đúng). Cách đúng: ghi khoá **trước** khi mở transaction (tách `OrderWriter`). |
| 2026-11-07 | SL-2.5 | **Idempotency khoá theo `(user, endpoint)` chứ không theo key đơn thuần**: key do client sinh, hai user có thể trùng key; scope tách ra thì không đụng nhau (và test phải dùng **cùng user** mới kiểm được replay). |
| 2026-11-07 | SL-2.5 | **CHECK constraint ở DB là thứ duy nhất không quên**: `total = subtotal − discount`, `line_total = unit_price × qty` — tính trong code thì script/migration sau này vẫn tạo được hoá đơn sai. |
| 2026-11-07 | SL-2.5 | **404 thay vì 403 cho tài nguyên của người khác**: 403 xác nhận "có tồn tại nhưng bạn không được xem" — với đơn hàng đó đã là rò rỉ thông tin. |
| 2026-11-07 | SL-2.5 | **Job dọn phải có ngưỡng cho cả 2 loại row**: `COMPLETED` giữ 24 h (retry muộn vẫn dedupe), `IN_PROGRESS` quá 1 h coi là mồ côi — không dọn thì client nhận 409 **vĩnh viễn** cho key đã crash. |
| 2026-11-07 | SL-2.7 | **Hai nguồn trùng ⇒ hai cơ chế idempotency, không trộn**: client bấm 2 lần dùng `Idempotency-Key`; provider retry dùng khoá **tự nhiên của nó** (`provider_event_id` + `provider_txn_id`). Provider không biết key của client — bắt nó dùng key của client là bắt nó chịu lỗi của mình. |
| 2026-11-07 | SL-2.7 | **Ký HMAC trên raw body + timestamp nằm trong chuỗi ký**: parse JSON rồi serialize lại là đổi thứ tự key/khoảng trắng ⇒ chữ ký không bao giờ khớp; thiếu timestamp trong chuỗi ký thì body + chữ ký cũ bắn lại được mãi. So sánh phải constant-time. |
| 2026-11-07 | SL-2.7 | **Bất biến nghiệp vụ nên là ràng buộc DB**: `provider_txn_id UNIQUE` + unique index có điều kiện `(order_id) WHERE status IN (…)` ("một đơn một intent sống") ⇒ 20 request song song vẫn đúng vì DB phân xử, không nhờ code chạy tuần tự. |
| 2026-11-07 | SL-2.7 | **Mock phải đi qua đúng đường thật**: mock tự set `SUCCEEDED` thì test xanh mà production gãy ở chỗ khó thấy nhất (chữ ký, dedupe, compare-and-set). Cho mock đi qua webhook ⇒ một lần verify chứng minh cả ba. |
| 2026-11-07 | SL-2.7 | **Cờ "chỉ local" + secret bắt buộc phải fail-closed lúc boot**: mock provider cho không hàng, secret rỗng cho ai cũng ký — cả hai là lỗi mà test chức năng không bao giờ phát hiện. |
| 2026-11-07 | SL-2.7 | **`Types.TIMESTAMP_WITH_TIMEZONE` + `java.sql.Timestamp` bị PSQLException**: chỉ IT với Postgres thật bắt được (unit test mock repo thì không). Lỗi kiểu này là lý do IT bắt buộc cho mọi service có I/O. |
| 2026-11-07 | SL-2.1 | **Value là JSON thì `HINCRBY` vô dụng — phải Lua**: đọc ở Java rồi ghi lại là lost update thật; bất biến (`qty 1..999`, `maxLines`) cũng phải kiểm **trong script** (kiểm ngoài = TOCTOU). |
| 2026-11-07 | SL-2.1 | **Guest token phải do server cấp + có marker key**: client tự sinh token thì ai giữ token cũ vẫn đọc được giỏ người khác và không thu hồi được; marker `cart:guest:<token>` thay cho một bảng DB. |
| 2026-11-07 | SL-2.1 | **Merge phải nguyên tử cả cộng-dồn và xoá-giỏ-guest**: tách 2 bước thì N request song song cộng N lần. Verify thật: 10 merge song song → `{200: 2, 401: 8}`, qty = 5 (không phải 50). |
| 2026-11-07 | SL-2.1 | **Public path ở gateway ⇒ authorization chuyển vào service**: gateway chỉ public theo path, mà `/v1/carts/**` vừa guest vừa user ⇒ service tự phân xử (JWT thắng, không JWT thì bắt buộc token hợp lệ). |
| 2026-11-07 | SL-2.1 | **Service không Postgres là hợp lý khi dữ liệu chịu mất** — nhưng cũng mất luôn ràng buộc DB canh bất biến ⇒ dồn hết vào Lua + test đồng thời. |
| 2026-11-08 | SL-2.13 | **Endpoint batch thuộc về service sở hữu dữ liệu, không phải nơi cần dùng**: `catalog` trả giá, `order` không cache giá. `missingIds` đúng hơn 404 cả request — một SKU chết không nên làm hỏng cả giỏ. Đếm kích thước request **trước** khi dedupe, không thì client lách trần bằng cách lặp id. Tên DTO lồng phải dài (`SkuLine`) vì spec OpenAPI merge toàn hệ. Và: `String.valueOf` trên số JSON (`Double`) là bẫy assertion. |
| 2026-11-08 | SL-2.6a | **Compensating phải đọc state bền**: saga ghi `lock_id`/`reservation_id` xuống DB ngay sau mỗi bước ⇒ hàm bù chỉ `select` lại là đủ; giữ trong biến RAM thì crash giữa chừng là mất dấu, hàng treo tới hết TTL. Fail thì **trả lại idempotency key** (xoá row) chứ không để `IN_PROGRESS` (job dọn 1 h ⇒ user sửa giỏ xong vẫn 409). **Stub test phải khó tính ngang service thật** — stub nhận mọi body nên sai tên field (`lines` vs `items`) vẫn xanh, chỉ chạy compose thật mới lộ. **5xx của dependency là 503 của mình**, không phải 500. Và: đơn 0 đồng là **bất biến của saga** (`payment_required`), không suy ra từ `orders.total`. |

---

## 7. Technical debt register

> Quy tắc ghi nhận: [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md). Mỗi món nợ **phải có
> điều kiện revisit verify được** — không phải "khi rảnh". Rà bảng này khi làm DoD mỗi phase.
> Trả xong thì giữ dòng, đổi `Mức độ` thành `✅ đã trả (<ticket>)`.

| ID | Mô tả | Nguồn | Điều kiện revisit | Mức độ |
|---|---|---|---|---|
| `TD-1` | Bảng `refresh_tokens` không có job dọn row hết hạn → phình dần | SL-1.4 | Khi bảng > 10⁶ row **hoặc** khi thêm scheduler | trung bình |
| `TD-2` | `GET /v1/admin/users` trả **toàn bộ** user, không phân trang | SL-1.4 | Khi số user > 1.000 hoặc có màn quản trị thật | thấp |
| `TD-3` | Handler map `AccessDeniedException` → 403 nằm trong **catalog**; service khác dùng `@PreAuthorize` phải copy | SL-1.5 | **Đã đạt**: `SL-2.2` là service thứ hai dùng `@PreAuthorize` → ticket `SL-2.12` | trung bình (chờ `SL-2.12`) |
| `TD-4` | `tools/e2e-smoke.sh` phụ thuộc tài khoản seed + `docker logs` ⇒ chỉ chạy local, chưa vào CI | SL-1.7 | Phase 2 (D6: smoke trong CI) | trung bình |
| `TD-5` | `tools/merge-pr.sh` chưa retry khi GitHub API lỗi tạm thời | SL-1.14 | Lần tới script fail vì API (retry + backoff ~15 dòng) | thấp |
| `TD-6` | Phân trang **offset** (`OFFSET 100000` quét bỏ rất nhiều row) | SL-1.6 | Phase 4 khi đo được p99 của list (→ keyset) | trung bình |
| `TD-7` | `q` dùng `LIKE '%…%'` (leading wildcard) ⇒ không dùng được btree, full scan | SL-1.6 | Phase 3/8 khi cần search thật (→ `tsvector`/trigram) | trung bình |
| `TD-8` | Sort whitelist chỉ `price`, `createdAt` | SL-1.6 | Khi client cần và có index tương ứng | thấp |
| `TD-9` | Verify fail-closed của `/v3/api-docs` đang làm **bằng tay**, chưa có test trong CI | SL-1.8 | Khi CI có bước compose (D6) | thấp |
| `TD-10` | `ProductService.findById` không còn dùng ở main (chỉ test gọi) | SL-1.6 | Lần sửa `ProductService` tiếp theo → **đã trả ở `SL-2.11`** | ✅ đã trả (SL-2.11) |
| `TD-11` | `page`/`size`/`sort` sai → 400 nhưng không trả `details` theo field | SL-1.6 | Khi client cần hiển thị lỗi theo field | thấp |
| `TD-12` | Chưa có **soft delete** cho product; `DELETE` là hard delete + cascade SKU | SL-1.6 | **Chốt ở `SL-2.0` · code xong ở `SL-2.11`** (ARCHIVED + `deleted_at` + CHECK constraint) | ✅ đã trả (SL-2.11) |
| `TD-13` | **Reuse detection revoke cả chuỗi** ⇒ client hợp lệ vẫn có thể bị logout toàn phiên: 2 tab cùng refresh gần như đồng thời, mất mạng sau khi server đã rotate, hoặc retry tự động gửi lại token cũ | SL-1.4 (rà soát 2026-11-06) | Khi có phản ánh "tự nhiên bị logout" **hoặc** khi làm client thật (Phase 2) → cân nhắc **grace window ~30s** (trả token mới nhất trong chuỗi thay vì revoke) + client single-flight refresh | ✅ đã trả (SL-1.18) |
| `TD-14` | Bảng `idempotency_keys` không có job dọn row quá 24h ⇒ phình dần (1 row/request checkout) | SL-2.0 | **Đã trả ở `SL-2.5`**: `IdempotencySweeper` (COMPLETED > 24h, IN_PROGRESS > 1h, batch 200) | ✅ đã trả (SL-2.5) |
| `TD-15` | Reservation sweeper là `@Scheduled` in-process ⇒ chạy trùng khi `inventory-service` > 1 instance | SL-2.0 | **Đã trả ở `SL-2.3`**: `FOR UPDATE SKIP LOCKED` + conditional `markStatus` ⇒ N instance vẫn không trả chỗ 2 lần | ✅ đã trả (SL-2.3) |
| `TD-16` | Chưa có API **restore/unarchive** product đã xoá mềm; bán lại cùng product phải tạo mới | SL-2.0 | Khi seller ẩn nhầm cần khôi phục **hoặc** có UI quản trị product | thấp |
| `TD-17` | Webhook `payment.succeeded` **không** cập nhật order (Phase 2 chưa có broker) ⇒ nếu saga không chạy, tiền đã thu mà đơn vẫn `PENDING_PAYMENT`; chưa có `refund`/`transactions` để hoàn tiền ca này | SL-2.7 | Khi làm `SL-2.6`/`SL-2.9` (test compensating thấy ca này) **hoặc** khi Phase 3 có event `payment.succeeded` | cao |
| `TD-18` | Webhook dùng **một** HMAC secret tĩnh, không key id/rotation; không lưu raw body (chỉ sha256) nên điều tra tranh chấp phải xin lại provider | SL-2.7 | Khi nối provider thật (Stripe/VNPay) — họ có key id + rotation riêng | thấp |
| `TD-19` | Idempotency store (`idempotency_keys` + `IdempotencyService`) **copy lần 2** (order, payment) chưa có module chung ở `packages/` | SL-2.7 | Khi có service thứ 3 cần idempotency (dự kiến không có ở Phase 2 ⇒ quyết ở `SL-2.12` hoặc Phase 3) | thấp |
| `TD-20` | Cart **không** lưu `price_snapshot` và không gọi catalog (quyết định `SL-2.1`) ⇒ hiển thị giá trong giỏ phải gọi catalog riêng; giỏ không phát hiện được SKU đã archive/đổi giá | SL-2.1 | **`SL-2.13` đã có `GET /v1/skus?ids=` (batch)** ⇒ phần còn lại chỉ là UI/gateway gọi endpoint đó (làm ở `SL-2.8`) | trung bình |
| `TD-22` | `GET /v1/skus?ids=` là **public**, nhận tới 100 id, **chưa rate limit** và **chưa cache** ⇒ 1 client có thể dội tải catalog (100 id/request) hoặc dùng làm đòn bộc phát | SL-2.13 | Khi bật rate limit ở gateway (Phase 5) **hoặc** khi đo được p99 list/get của catalog tăng do traffic `/v1/skus` (Phase 4 — cache-aside) | thấp |
| `TD-23` | IT `inventory`: `commitAfterExpiryReturnsGone` **flaky** — test đẩy `expires_at` về quá khứ rồi `commit`, nhưng sweeper trong IT chạy mỗi **300 ms** nên có lúc nó expire trước ⇒ `commit` trả **409** thay vì **410** (đỏ trên CI chậm, xanh ở máy local) | SL-2.13 (CI run `42444551383`) | Lần tới khi sửa test của inventory: tắt sweeper (`sweep-interval-ms` rất lớn) cho test lazy-expire, hoặc assert `status ∈ {410, 409}` + kiểm `reserved = 0` (điều thực sự quan trọng) | trung bình |
| `TD-24` | `checkout_saga_steps` + `checkout_sagas` **không có job dọn** — 1 dòng/bước/lần checkout ⇒ phình mãi (audit nhưng không ai đọc sau 30 ngày) | SL-2.6a | Khi bảng > 10⁶ row hoặc khi `SL-2.6b` thêm sweeper (gộp 1 `@Scheduled` dọn saga `COMPLETED_START`/`FAILED_*` cũ > 30 ngày) | thấp |
| `TD-21` | `/v1/carts/**` là public ở gateway và **chưa có rate limit** ⇒ có thể spam tạo guest token/giỏ (mỗi token = 2 key Redis, TTL 30 ngày) | SL-2.1 | Khi bật rate limit ở gateway (Phase 5) **hoặc** khi thấy số key Redis tăng bất thường | trung bình |

## 8. Changelog


| Ngày | Thay đổi |
|---|---|
| 2026-10-25 | Tạo backlog. Phase 0 có 5 ticket chi tiết, Phase 1–8 ở mức epic. |
| 2026-11-02 | Thêm `SL-0.6` — Bootstrap AI tooling (rules/skills/workflows), đặt làm trước `SL-0.1`. |
| 2026-11-02 | Implement `SL-0.6`: tạo `.clinerules/00-context.md`, 3 skills `.cline/skills/`, 3 workflows `.clinerules/workflows/`; update `AGENTS.md` §10. Trạng thái `Review` (chờ user test slash command). |
| 2026-11-02 | Đóng `SL-0.6` → `Done` (user xác nhận slash command + skill trigger). |
| 2026-11-02 | Đóng `SL-0.1` → `Done`: tạo `docs/adr/0001-tech-stack.md`, cập nhật `architecture.md` §10.2/§11. |
| 2026-11-02 | Thêm `SL-0.7` → `Done`: ADR-0002 (dev env = WSL2); cập nhật `architecture.md` §10; điều chỉnh `SL-0.2`. |
| 2026-11-02 | `SL-0.2`: bổ sung `.gitattributes` + git config (`core.autocrlf`/`core.fileMode`) để tránh nhiễu khi chuyển sang WSL. |
| 2026-11-02 | Thêm `SL-0.8` (Review): git workflow — rule `.clinerules/10-git-workflow.md`, workflow `/git-branch-pr.md`, `docs/git-flow.md`. |
| 2026-11-02 | Đóng `SL-0.8` → `Done` (user gọi `/git-branch-pr.md` xác nhận workflow nạp đúng). |
| 2026-11-02 | Tạo PR [#1](https://github.com/anchu3003/shop-lab-microservices/pull/1) cho `SL-0.1/0.6/0.7/0.8` (đã merge vào `main` bằng **merge commit** `368009e` — lệch quy ước **squash** của `docs/git-flow.md`, ghi nhận để lần sau merge đúng). Commit `a881a01` chuẩn hoá line-ending bằng `.gitattributes` (`* text=auto eol=lf`) — sớm hơn `SL-0.2`. |
| 2026-11-02 | Ghi learning note `SL-0.8`: diff nhiễu CRLF + kết quả thử branch protection (**403 — private + GitHub Free**, không bật được). |
| 2026-11-02 | `SL-0.2` → `In Progress`: tạo nhánh `chore/sl-0.2-repo-skeleton`; thêm `.editorconfig`, `.gitignore` (mở rộng), `.env.example`, `README.md`, `lefthook.yml`, `tools/*.sh`, `pom.xml` + `config/checkstyle/checkstyle.xml`, thư mục `services/ packages/ infra/`. |
| 2026-11-02 | `SL-0.2` → `Review`: pin checkstyle **12.3.1** (plugin mặc định kéo 9.3); verify thật `mvn verify` BUILD SUCCESS + test âm cho cả Spotless và Checkstyle; PR [#2](https://github.com/anchu3003/shop-lab-microservices/pull/2). Còn chờ user cài JDK 21 + Maven trong WSL (sudo). |
| 2026-11-02 | `SL-0.3` → `In Progress`: tạo nhánh `feat/sl-0.3-local-infra-compose`. |
| 2026-11-02 | `SL-0.3` → `Done`: `infra/docker-compose.yml` (Postgres 16 · Redis 7 · RabbitMQ 4 + management UI · Adminer), `infra/init/01-create-databases.sh` (7 DB per service, idempotent), `tools/reset-infra.sh` (bash, thay `reset-infra.ps1` theo ADR-0002), `.env.example` (+`POSTGRES_DB`, `ADMINER_PORT`), README (port table + RAM baseline + bẫy `--env-file`). Verify thật: 4/4 container `healthy`, psql 7/7 DB OK, `redis-cli -a … ping` → `PONG`, RabbitMQ UI + Adminer HTTP 200, reset script OK, volume bền qua `down`/`up`, test âm `${VAR:?}` fail-fast. RAM baseline ≈ **150 MiB**. |
| 2026-11-02 | Ghi nhận (ngoài scope `SL-0.3`): JDK **21.0.12.1** + Maven **3.9.12** đã có trong WSL → `mvn -B verify` **BUILD SUCCESS** (0 Checkstyle violations) ⇒ điều kiện cuối của `SL-0.2` đã đạt, `SL-0.2` có thể chuyển `Done`. `lefthook` **2.2.2** cũng đã cài (`~/.local/bin`, hooks active — thấy chạy thật khi commit `eef58a3`). Sửa lại bảng "Yêu cầu môi trường" trong `README.md` cho khớp. |
| 2026-11-02 | Commit `eef58a3` trên nhánh `feat/sl-0.3-local-infra-compose` (lefthook `pre-commit` + `commit-msg` chạy và pass). |
| 2026-11-02 | Đóng `SL-0.2` → `Done`: điều kiện cuối đã đạt — JDK **21.0.12.1** + Maven **3.9.12** (`/usr/bin/mvn`, không còn bản Windows) + Lefthook **2.2.2** cài trong WSL; `mvn -B verify` **BUILD SUCCESS**; test âm lặp lại cho `commit-msg` + `secret-scan` đều **bị chặn thật**. Phase 0 → **6 / 8**. |
| 2026-11-02 | Tạo PR [#3](https://github.com/anchu3003/shop-lab-microservices/pull/3) cho `SL-0.3` + đóng `SL-0.2` (nhánh `feat/sl-0.3-local-infra-compose`, 3 commit). **Nhắc lại quy ước**: merge bằng **squash** (PR #1 đã lệch — dùng merge commit). |
| 2026-11-02 | `SL-0.4` → `In Progress`: nhánh `feat/sl-0.4-service-template`. Chốt với user: **Spring Boot 4.1.2**. |
| 2026-11-02 | `SL-0.4` → `Done`: Maven multi-module (`packages/service-commons` + `services/service-template`), shared lib auto-configuration (log JSON · error response thống nhất · correlation id · health check · env validator fail-fast), service mẫu `/health` `/ready` `/version` + graceful shutdown + Dockerfile multi-stage non-root, thêm vào compose, JaCoCo gate ≥80% cho config/health, README "tạo service mới từ template". ADR-[0003](docs/adr/0003-spring-boot-version-and-module-layout.md) + ADR-[0004](docs/adr/0004-service-commons-shared-library.md). Verify thật: 72 test pass, 5/5 container healthy, `/ready` 200→503 khi tắt Postgres, fail-fast khi thiếu `APP_ENV`, log JSON đủ 4 field, graceful shutdown hoàn tất request 8s. Phase 0 → **7 / 8**. |
| 2026-11-02 | Sửa 3 bug tiềm ẩn lộ ra khi có code Java thật: **surefire 2.17** (Tests run: 0 mà vẫn xanh) → pin `3.6.1`; thiếu **`-parameters`** → lỗi runtime ở `@RequestParam`; **Spotless ↔ Checkstyle xung đột** ở block rỗng → bật `allowEmpty*` trong `checkstyle.xml`. |
| 2026-11-02 | Verify AC "tạo service mới từ template" bằng cách **thực sự** copy ra `smoke-service` theo README → phát hiện test hard-code tên service (template không copy-safe) → sửa test đọc tên từ config; chạy lại `BUILD SUCCESS` rồi xoá `smoke-service`. |
| 2026-11-05 | Tạo 3 ticket phát sinh sau khi rà CI/compose: `SL-1.12` (job `build-image` hiện **chỉ** build `service-template` — bằng chứng log run `42054583987`; 2 service thật không được CI kiểm Dockerfile), `SL-1.13` (`service-template` vào **compose profile** `template` để `up -d` chỉ dựng hệ thống thật), `SL-1.14` (`merge-pr.sh`: cho phép PR **docs-only** + post comment evidence **hậu merge**). **Đổi process**: mark done **gộp vào commit cuối của ticket** (1 ticket = 1 commit = 1 run CI trên PR), bỏ "fold commit minor sang PR sau"; evidence hậu merge ghi bằng PR comment. Chốt **giữ monorepo** (ghi vào ADR-0003 → Revisit). |
| 2026-11-05 | `SL-1.14` → `Done`: `tools/merge-pr.sh` (a) cho phép merge PR **chỉ-docs** (0 run là hợp lệ khi **toàn bộ** diff là docs; 0 run mà có file không phải docs → **vẫn chặn**) và (b) tự post **comment evidence hậu merge** (merge sha · run CI trên `main` · token skip · ghi chú chống hiểu nhầm). Verify: **TEST A** PR docs-only #17 → cho phép (`exit=0`); **TEST B** 0 run + `tools/secret-scan.sh` → chặn (`exit=1`); **merge thật** PR #17 → `main` = `4afb3da` + comment evidence tự post ✓. Kèm dọn 2 nhánh temp + xoá file test. |
| 2026-11-05 | `SL-1.12` → `Done`: job `build-image` chuyển sang **matrix** (`service-template` · `identity-service` · `api-gateway`, `fail-fast: false`) + `--build-arg SKIP_TESTS=true` (bỏ test trùng trong image — job `test` đã chạy cả reactor). **Test âm thật**: phá `api-gateway/Dockerfile` → run `42064872154` **chỉ** job gateway đỏ (`Child module … does not exist`), 2 job image kia xanh; revert → run `42065751051` **5/5 xanh**. Số đo: billable 194s → 343s (**+2,5 phút/run**, wall-clock gần như không đổi vì chạy song song). Cập nhật `README.md` + `architecture.md` §10.3 + comment `ci.yml`. PR [#20](https://github.com/anchu3003/shop-lab-microservices/pull/20). |
| 2026-11-05 | `SL-1.13` → `Done`: `service-template` vào **compose profile** `template` ⇒ `up -d` mặc định chỉ dựng hệ thống thật (không tốn RAM/build cho service mẫu). Verify: `config --services` (không profile) **không** có template · `up -d` không tạo `shoplab-service-template` · `--profile template up -d service-template` → Started, `/ready` 200 · gateway/identity vẫn healthy. Cập nhật `README.md` (bảng port + lệnh) + comment `infra/docker-compose.yml`. |
| 2026-11-05 | `SL-1.3` → `Done`: `tools/e2e-smoke.sh` — health gateway+identity → register (email random) → login → `/v1/users/me` có token → 200 · thiếu token → **401** · đối chiếu `traceId` trong log identity; `PASS/FAIL` từng bước + exit code. Verify: **7/7 PASS** · exit 0 · chạy lại 3 lần pass · **test âm** (stop identity) → fail đúng bước `1b` + `exit=1`. Thêm mục README "E2E smoke". Sửa 2 bug: `date +%s%3N` ra nanosec → `%s%N`/1e6; curl connection-refused + `set -e` → `\| true`. |
| 2026-11-06 | `SL-1.5` → `Done`: `catalog-service` (port **8082**, DB `catalog`, Flyway `V1__create_catalog.sql` với `price NUMERIC(12,2)` + `@Version` + FK/index) — `POST/GET /v1/categories`, `POST/GET/PUT/DELETE /v1/products`; guest **đọc** được, chỉ `seller`/`admin` **ghi**. Service **tự verify JWT qua JWKS của identity** (defense in depth: gọi thẳng :8082 không token vẫn 401, `x-user-role` client gửi bị bỏ qua). Error **400/401/403/404/409** đều là `ApiError` có `traceId`. Hạ tầng: module mới ở parent pom, 4 Dockerfile `COPY` pom mới, CI matrix **4 service**, compose + `.env.example` + gateway route `/v1/products` `/v1/categories`. Verify: **36 unit + 12 IT** (+37 commons) xanh; **36/36 mục AC** trên stack thật (seller 201 · buyer 403 · guest 200 · 404 · 400 details · 401 gọi thẳng · không leo quyền · `version` +1); `e2e-smoke` vẫn 7/7. Sửa 3 bug thật: `@PreAuthorize` → **500** thay vì 403, `@Version` trả số cũ trong response `PUT`, image build đỏ vì JaCoCo gate bị `-DskipITs`. |
| 2026-11-06 | `SL-1.6` → `Done`: `catalog-service` có **SKU** (`V2__create_sku.sql`: `code` unique, `price_override` NULL = dùng giá product, `stock_hint` chỉ là gợi ý — tồn kho thật là việc của inventory Phase 2) + **list phân trang/filter/sort** (`GET /v1/products?page=&size=&categoryId=&q=&sort=` → `{content,page,size,totalElements,totalPages}`, public cho guest) + **seed dev** (`R__seed_dev_data.sql`: 5 category / 20 product / 24 SKU, idempotent bằng `ON CONFLICT`). Tham số sai (`size` ngoài 1..100 · `page<0` · `sort` lạ · `categoryId` không UUID) → **400 `VALIDATION_ERROR`**, **không clamp**; sort whitelist `price,asc|desc` · `createdAt,asc|desc`. Verify: **60 unit + 18 IT** xanh; **35/35 mục AC** trên stack thật (paging không trùng id · filter · `q` không phân biệt hoa/thường · sort đúng thứ tự · 400 cho mọi input xấu · SKU trong chi tiết · **`down -v && up -d` → 5/20/24 seed** · restart **không** nhân đôi · `EXPLAIN` dùng `ix_product_created_at`); `e2e-smoke` 7/7. |
| 2026-11-06 | `SL-1.7` → `Done`: `tools/e2e-smoke.sh` mở rộng thành E2E **cả Phase 1** — **17 mục**: health 3 service · register · login · `/v1/users/me` (200/401) · `traceId` xuyên service · **seller** tạo category+product (201, in id) · **guest** list + xem chi tiết (**200, không token**, có `skus`) · **buyer** tạo product (**403**, case mong đợi lỗi) · body lỗi là `ApiError`. Fail ở đâu in **gợi ý lệnh kiểm tra** ở đó. Thêm `docs/runbook-phase-1.md` (sơ đồ luồng · bảng port · kiểm tra **từng tầng** · **5 lỗi**: 401/403/404/503/timeout · §5 là output debug thật). Verify: **17/17 PASS** · chạy **6/6 lần** pass (~430 ms) · `docker stop` catalog → fail đúng **1c (000)** + **8 (503)**, các bước khác vẫn PASS. Sửa 2 bug thật: category trùng slug khi 2 lần chạy cùng giây (**flaky**), và script in **body cũ** khi connection refused. |
| 2026-11-06 | `SL-1.8` → `Done`: OpenAPI spec Phase 1 — thêm `springdoc-openapi-starter-webmvc-api` **3.2.0** (Boot 4.1.x ⇒ springdoc 3.x) cho 3 service, expose `/v3/api-docs`; khung spec (info + security scheme `bearerAuth`) + **bộ response lỗi 400/401/403/404/409 tự gắn cho MỌI operation** (`$ref → ApiError`) đặt ở `service-commons` (`OpenApiAutoConfiguration`), nên endpoint Phase 2 tự có tài liệu lỗi. `OpenApiEnvironmentPostProcessor` ép tắt spec khi `APP_ENV != local` (**fail-closed**, override bằng `SHOPLAB_OPENAPI_ENABLED`). `tools/export-openapi.sh` merge spec identity + catalog → **`docs/api/openapi.yaml`** (`servers` = gateway, sort key để diff ổn định) + **tự validate** bằng `python3`+`pyyaml`. Verify: 3/3 service `/v3/api-docs` **200**; export **VALIDATE PASS · 17 path · 21 operation · 7 operation cần token · 18 schema**; `APP_ENV=prod` → `/v3/api-docs` **404** (service vẫn 200); e2e-smoke 17/17; cả reactor `mvn verify` xanh (254 unit + 40 IT). |
| 2026-11-06 | `SL-1.15` → `Done`: **seed dev không còn chạy ở prod** — chuyển `R__seed_dev_data.sql` sang `db/seed/`, `spring.flyway.locations: ${SHOPLAB_FLYWAY_LOCATIONS:classpath:db/migration}` (**fail-closed**); compose local + IT tự thêm `,classpath:db/seed`. **Slug tiếng Việt** bỏ dấu (NFD + map `đ/Đ`): `"Điện thoại X"` → `dien-thoai-x`, `"Ốp lưng đẹp"` → `op-lung-dep`. Verify: local `down -v && up -d` → **5/20/24** seed · kiểu prod `APP_ENV=prod` + DB mới → **category=0 product=0 sku=0** (history chỉ V1+V2) · **42 + 61 unit + 18 IT** xanh · e2e-smoke 17/17. Sửa kèm 2 IT bị đỏ do slug mới trùng seed (dùng hậu tố UUID). |
| 2026-11-06 | `SL-1.17` → `Done`: ẩn endpoint debug khỏi OpenAPI spec bằng `@Hidden` (identity + catalog + gateway) — spec **17 → 14 path**, **21 → 18 operation**, `grep -c '/debug' = 0`, validate vẫn PASS; probe `/health` `/ready` `/version` **giữ** vì là hợp đồng vận hành thật. Docs: `docs/api/README.md` thêm mục "cái gì không nằm trong spec". Verify: export lại + đếm path trước/sau · `mvn verify -DskipITs` xanh toàn reactor · CI xanh. |
| 2026-11-06 | `SL-1.18` → `Done`: **grace window 30s** cho refresh reuse detection (`TD-13`) — token đã rotate trong ≤30s mà bị dùng lại ⇒ cấp token mới nối tiếp chuỗi (không revoke) để client hợp lệ không bị logout oan; quá 30s ⇒ revoke cả chuỗi như cũ; **logout không có grace** (dùng lại token cũ sau logout luôn 401). Cấu hình `IDENTITY_REFRESH_REUSE_GRACE` (`0s` = tắt). Verify: **50 unit (+4) + 22 IT** xanh; stack thật: reuse trong grace **200** · sau logout **401** · sau 31s **401 + cả chuỗi chết**. |
| 2026-11-06 | `SL-1.16` → `Done`: rule [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md) + **`Backlog.md` §7 Technical debt register** với **12 mục** `TD-1`…`TD-12` (job dọn `refresh_tokens` · admin list không phân trang · handler 403 nên ở commons · smoke chưa vào CI · `merge-pr.sh` chưa retry · offset pagination · `LIKE '%q%'` full scan · sort whitelist hẹp · verify OpenAPI fail-closed bằng tay · dead code · 400 không `details` · **soft delete chưa có — chặn `SL-2.x`**). Changelog đổi thành §8. |



| 2026-11-05 | `SL-1.4` → `Done`: refresh token (**opaque 256-bit, lưu SHA-256**) + **rotate + reuse detection** (dùng lại token đã rotate ⇒ 401 **và revoke cả chuỗi**), logout, `GET /v1/admin/users` (ADMIN 200 / buyer **403**), security filter chain (identity **tự verify JWT** — defense in depth) + 401/403 trả **ApiError** có `traceId`. Seed `admin@`/`seller@` bằng `LocalDevUserSeeder` (chỉ `APP_ENV=local`). Verify: **46 unit + 22 IT** xanh; E2E **9 mục** trên stack thật (rotate/reuse/logout/RBAC/hash-trong-DB/**qua gateway**); `tools/e2e-smoke.sh` vẫn 7/7. Sửa 3 lỗi thật: `noRollbackFor` (revoke bị rollback), thiếu route `/v1/admin` ở gateway, path lạ 404→**401**. |


| 2026-11-02 | Tạo PR [#4](https://github.com/anchu3003/shop-lab-microservices/pull/4) cho `SL-0.4` (nhánh `feat/sl-0.4-service-template`, 5 commit). Merge bằng **squash** theo `docs/git-flow.md`. |
| 2026-11-03 | `SL-0.5` → `In Progress`: nhánh `feat/sl-0.5-ci-pipeline`. Chốt với user: **giữ remote HTTPS** (token VS Code có scope `workflow`, không cần đổi SSH). |
| 2026-11-03 | `SL-0.5` → `Review`: `.github/workflows/ci.yml` (job `lint` ∥ `test` → `build-image`; cache Maven; `concurrency` theo tên nhánh; `timeout-minutes`; `permissions: contents: read`), badge CI + mục **CI (GitHub Actions)** trong `README.md`, `architecture.md` §10.3 + Changelog. **Baseline run đầu: 2m35s** (3/3 job xanh). Test âm thật: `lint` đỏ (`HealthController.java:54:5 ... [NeedBraces]`), `test` đỏ (`AppEnvTest.shouldReportLocalOnly:48`) — cả hai lần `build-image` **skipped**. PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5). |
| 2026-11-03 | `SL-0.5`: **sửa lỗi thiết kế `concurrency`** — gom push+PR chung group làm run PR huỷ run push, để lại check `cancelled` trên commit; đổi sang `group: ${{ github.workflow }}-${{ github.ref }}`. Cập nhật `README.md` + ticket + learning log. |
| 2026-11-03 | Merge PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5) bằng **squash** (`60e8afe`). Run trên `main` xanh: 3/3 job, 2m13s → badge CI `passing`. |
| 2026-11-03 | Đóng `SL-0.5` → `Done`: **6/6 AC** đạt. **Phase 0 → 8 / 8 ✅ Done** — điều kiện hoàn thành phase đã đủ: `docker compose up` infra `healthy` · service template `/health` 200 · CI xanh · `architecture.md` §11 không còn chữ "cần chốt". Việc tiếp theo: breakdown **Phase 1** (epic → ticket chi tiết). |
| 2026-11-05 | `SL-1.11` → `Done`: fix 2 dòng chẩn đoán gây hiểu nhầm của `tools/merge-pr.sh` (nhánh chưa có run → nói rõ thay vì in `'null'`; đếm token skip bằng regex `\[(skip ci|…)\]` thay vì substring). Verify: message mới trên nhánh tạm (`exit=1`); `grep -c 'skip ci'` = **1** (cách cũ, SAI) vs `grep -cE '\[(skip ci|…)\]'` = **0** (cách mới). PR [#14](https://github.com/anchu3003/shop-lab-microservices/pull/14). Kèm fold bằng chứng merge của `SL-1.9` + `SL-1.10`. |

| 2026-11-03 | **Tách archive**: chuyển toàn bộ Phase 0 (8 ticket + bối cảnh/AC/quyết định/verify/learning note) sang [`Backlog-phase-0.md`](Backlog-phase-0.md); `Backlog.md` còn **260 dòng** (bảng phase · convention · pointer + bảng archive · epic Phase 1–8 · Learning log · Changelog). Cập nhật tham chiếu ở `README.md`, `architecture.md`, ADR-0001…0004, `.clinerules/00-context.md`, `AGENTS.md` §1/§6. |
| 2026-11-03 | **Sửa rule git**: commit minor (đóng ticket / update backlog / sửa doc / tách archive) **không** tạo nhánh + PR riêng — gộp vào nhánh của **ticket kế tiếp**; chỉ tách PR riêng khi hết ticket kế tiếp. Ghi vào `AGENTS.md` §6 · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` · workflow `/close-ticket.md` (bước 8–9). |
| 2026-11-03 | Chốt **ADR-[0005](docs/adr/0005-phase-snapshot-and-release.md)** + dựng `docs/phases/` ([index](docs/phases/README.md) + [`phase-0.md`](docs/phases/phase-0.md)): tag `phase-N` + **GitHub Release** + doc snapshot, **không** dùng release branch ("lazy D"). `AGENTS.md` §5 thêm **DoD của một phase**; `/close-ticket.md` thêm bước 8 & 10 (tách archive, tag/release sau merge). |
| 2026-11-03 | Tạo **tag + GitHub Release `phase-0`** trên merge commit `259a7e9` (ADR-0005). Verify: push tag **không** trigger CI (workflow chỉ khai `on.push.branches`) → 0 phút Actions; `git show phase-0:<file>` đọc được snapshot. |
| 2026-11-03 | `SL-1.0` → `Done`: tối ưu chi phí CI — `paths-ignore` (bỏ run chỉ-docs), `push` chỉ trên `main` (hết run trùng), kill-switch `vars.CI_ENABLED`, bỏ qua draft PR (`ready_for_review`). Verify thật: push nhánh feature **0 run**, draft → run `skipped` (0 phút), `CI_ENABLED=false` + re-run → `skipped`, `true` → 3/3 xanh, merge → run `main` xanh, **PR chỉ-docs → 0 run**. Cùng kịch bản: **8 phút vs ~19,5 phút (−59%)**. Merge PR [#7](https://github.com/anchu3003/shop-lab-microservices/pull/7) (squash). **Phase 1 → 1 / 1**. |
| 2026-11-03 | **Breakdown Phase 1** → 9 ticket chi tiết: `SL-1.0` Done · `SL-1.1`…`SL-1.8` Todo (identity register/login + JWT RS256 + JWKS · gateway routing/verify JWT · **E2E sớm** · refresh token + RBAC · catalog CRUD · SKU/paging/seed · E2E đầy đủ + runbook · OpenAPI spec). Chốt 7 quyết định khi breakdown (gateway **MVC tự viết** · JWT **RS256 + JWKS** · **JPA + Flyway** · **có Testcontainers** · OpenAPI **sinh từ code** · smoke chạy **local** · register luôn là `buyer`). |
| 2026-11-05 | `SL-1.1` → `Done`: nhánh `feat/sl-1.1-identity-service`. `identity-service` (port 8081) — **Flyway** + register/login + **JWT RS256** + **JWKS**; `service-commons` thêm `UNAUTHORIZED`/`FORBIDDEN`; `tools/gen-dev-keys.sh` (khoá dev, không commit); `tools/secret-scan.sh` fix false positive; compose + `.env.example`. Verify thật: container `healthy` · Flyway v1 áp dụng · BCrypt trong DB (60 ký tự) · **chữ ký verify bằng public key từ JWKS** · 401 không lộ email · fail-fast khi thiếu khoá · `/ready` 503 khi DB chết. `mvn verify`: 37 + 39 unit + **16 IT** (Testcontainers); **CI xanh 3/3**, job `test` chạy IT thật (1:24). PR [#9](https://github.com/anchu3003/shop-lab-microservices/pull/9). |
| 2026-11-05 | `SL-1.2` → `Done`: nhánh `feat/sl-1.2-api-gateway`. `api-gateway` (port 8080) — routing theo config (prefix dài nhất thắng) · verify **JWT RS256 qua JWKS** (cache 5m + nạp lại khi `kid` lạ, có chặn dội) · gắn/ghi đè `x-user-id`/`x-user-role` · propagate `x-correlation-id` · **401** token sai / **503** service đích chết. `service-commons` thêm `UNAVAILABLE` (503). `identity-service` thêm `GET /v1/users/me` (protected). Verify thật: container healthy · register/login qua gateway giống hệt gọi thẳng · 401/200/503 đúng · traceId **trùng** ở log 2 service. `mvn verify`: 37 + 39 + **16 IT** + **71 test gateway**; **CI xanh 3/3** (job `test` 1:49). PR [#10](https://github.com/anchu3003/shop-lab-microservices/pull/10). Đồng thời khôi phục 3 thay đổi doc của `SL-1.1` bị mất do push race lúc merge (xem Learning log). |
| 2026-11-05 | `SL-1.9` → `Done`: rule **commit docs-only → `[skip ci]`** (không tốn CI) + **auto-merge** `tools/merge-pr.sh` (verify → comment tóm tắt → squash → xoá nhánh). Verify thật: PR chỉ-docs **0 run**; commit docs trong PR có code **0 run mới**; script **từ chối đúng 4 case** (draft · không OPEN · chưa có CI xanh · thiếu comment tóm tắt); merge PR [#11](https://github.com/anchu3003/shop-lab-microservices/pull/11) bằng chính script.
| 2026-11-05 | `SL-1.10` → `Done`: fix tác dụng phụ của `SL-1.9` — `gh pr merge --squash` ghép *title + subject các commit trong PR* nên `[skip ci]` của commit docs **lọt vào merge commit** ⇒ push `main` bị skip (mất kiểm chứng `main`). Fix: `tools/merge-pr.sh` truyền tường minh `--subject`/`--body` + lọc token skip; thêm cảnh báo vào rule + `docs/git-flow.md`. Verify: `a7d9482` có `1` token skip & **không** có run trên `main`; sau fix merge PR [#13](https://github.com/anchu3003/shop-lab-microservices/pull/13) → `skip-ci tokens: 0` + **có run `push` trên `main`**. |
| 2026-11-07 | `SL-2.0` → `Done`: **ADR-0006** chốt mô hình checkout Phase 2 — (D1) chống oversell bằng **conditional `UPDATE`** trên `stock` (không Redis lock) · (D2) `idempotency_keys` ở Postgres, scope `userId:endpoint`, retry **replay response** · (D3) `order_items` **snapshot** giá/tên · (D4) **sync REST** Phase 2, async để Phase 3 · (D5) `TD-12` → soft delete product, sinh ticket **`SL-2.11`**. Cập nhật `architecture.md` §6.1/§8.4 + debt mới `TD-14`…`TD-16`. Docs-only, không đổi code. |
| 2026-11-07 | `SL-2.11` → `Done`: `catalog-service` **soft delete** product (ADR-0006 D5, trả `TD-12` + `TD-10`). Migration `V3__add_soft_delete.sql` (`product.status`/`deleted_at`, `sku.status`, CHECK `ARCHIVED ⟺ deleted_at`, partial index) · `DELETE` = archive + bulk archive SKU (1 câu UPDATE) · mọi đường đọc/ghi lọc `deleted_at is null` (404) · `status` trả ra API + OpenAPI (3 schema). Verify thật: `mvn verify` **BUILD SUCCESS** (catalog 66 unit + **21 IT** trên Postgres Testcontainers; toàn repo xanh) · Flyway V3 trên container `success=t` · curl qua gateway: DELETE 204 → GET 404 → list 0 → DB `status=ARCHIVED` + `deleted_at` not null, SKU còn row và ARCHIVED · CHECK chặn cả 2 chiều sai · `e2e-smoke` 17/17 · OpenAPI export deterministic. |
| 2026-11-07 | `SL-2.2` → `Done`: thêm **`inventory-service`** (8084, DB `inventory`) — `reserve`/`commit`/`release` chống oversell bằng **conditional UPDATE** (ADR-0006 D1), reservation **TTL 15′** + lazy expire, idempotent theo `orderId`, `stock_ledger` append-only 2 cột delta. Migration `V1__create_inventory.sql` (4 bảng + CHECK bất biến). JDBC thay JPA (cần `rowsAffected`). Hạ tầng: compose + gateway route `/v1/stock` + CI matrix + 4 Dockerfile cũ. `commons` thêm `GONE(410)`. Verify: `mvn verify` **BUILD SUCCESS** (inventory **40 unit + 12 IT** postgres thật, toàn repo xanh) · container `healthy` · luồng curl qua gateway: 201/200 idempotent · 409 + details · commit 10→7 · commit#2 không trừ 2 lần · release sau commit 409 · quá TTL **410** + trả chỗ · ledger khớp `(7,0)`/`(1,0)` · 401/403 đúng · `e2e-smoke` 17/17 · OpenAPI 18 path/23 operation. Sinh `SL-2.12` để trả `TD-3`. |
| 2026-11-07 | `SL-2.3` → `Done`: `inventory-service` thêm **sweeper TTL** (`@Scheduled` fixedDelay 60 s, batch 100, `FOR UPDATE SKIP LOCKED` + conditional `markStatus`) — reservation `PENDING` quá hạn tự `EXPIRED` + trả chỗ + ledger `EXPIRE`; trả `TD-15`. Thêm test **100 request HTTP song song** cùng SKU: **đúng 10×201 / 90×409**, `reserved=10`, `Σ delta_reserved=10`, bất biến `reserved<=on_hand` OK. Verify: `mvn verify` **BUILD SUCCESS** (inventory 44 unit + 15 IT) · container thật: sweeper dọn trong ~300 ms, log `đã expire 1 reservation` · `e2e-smoke` 17/17. |
| 2026-11-07 | `SL-2.4` → `Done`: thêm **`promotion-service`** (8086, DB `promotion`) — voucher `PERCENT` (cap `max_discount`) + `FIXED`, `lock`/`commit`/`release` cùng pattern conditional `UPDATE` của inventory; `voucher_locks` 1 row/order + snapshot `discount`/`subtotal`; bất biến `used + locked <= quota` do CHECK canh; sweeper TTL (`SKIP LOCKED`) trả lượt bị bỏ quên. Hạ tầng: compose + gateway route `/v1/vouchers` + CI matrix + Dockerfile 6 service. Verify: `mvn verify` **BUILD SUCCESS** (promotion 48 unit + 13 IT) · container healthy · flow thật: lock 201 → retry 200 cùng id → commit (used=1) → release sau commit 409 → quá TTL 410 + trả lượt → sweeper tự trả trong ~1,5 s · **100 request song song: 10×201 / 90×409**, `used+locked<=quota` OK · `e2e-smoke` 17/17 · OpenAPI 23 path. |
| 2026-11-07 | `SL-2.5` → `Done`: thêm **`order-service`** (8087, DB `order`) — tạo đơn **idempotent** (`Idempotency-Key`: 201 · 200 replay + header · 422 key dùng lại · 409 đang chạy · 400 thiếu header), `order_items` **snapshot** giá/tên, state machine `PENDING_PAYMENT → CONFIRMED|CANCELLED|EXPIRED` + history, bất biến tiền bằng CHECK ở DB; job dọn `idempotency_keys` (**trả `TD-14`**); `commons` thêm `422`. Verify: `mvn verify` **BUILD SUCCESS** (order 43 unit + 10 IT, toàn repo xanh) · flow thật: 201 → retry 200 cùng id → 422 → 400 · confirm 200 · confirm#2 không ghi history · cancel sau confirm 409 · cancel/expire 200 + reason · **20 request song song cùng key ⇒ 1 đơn** · job dọn xoá row 48h trong 3 s · `e2e-smoke` 17/17 · OpenAPI 28 path. **Sự cố thật**: deadlock pool do `REQUIRES_NEW` lồng transaction (đã fix bằng `OrderWriter`). |
| 2026-11-07 | `SL-2.7` → `Done`: thêm **`payment-service`** (8088, DB `payment`) — payment intent **idempotent** (`Idempotency-Key`: 201/200 replay/422/409/400, mỗi đơn 1 intent sống), **mock provider** (fail-closed ngoài local), webhook **idempotent** theo khoá tự nhiên (`provider_event_id` PK + `provider_txn_id` UNIQUE) + **HMAC-SHA256 trên raw body** & timestamp window (chống replay), bất biến tiền bằng CHECK (`amount > 0`, `SUCCEEDED ⇔ paid_at`); gateway public-path `/v1/webhooks/**` + `/mock-provider/**`. Verify: `mvn verify` **BUILD SUCCESS** (payment 59 unit + 12 IT, toàn repo xanh, jacoco 88.6 %) · flow thật: 201 → retry 200 cùng id → 422 → 400 → 409 khác tiền · mock-pay → SUCCEEDED · **webhook gửi lại 5 lần + 20 request song song ⇒ 1 event row** · chữ ký sai/timestamp cũ 401 · sai tiền 409 không đổi trạng thái · `?fail=true` → FAILED · `e2e-smoke` 17/17 · OpenAPI 32 path/39 operation. **Sự cố**: `Types.TIMESTAMP_WITH_TIMEZONE` + `Timestamp` ⇒ 500 (chỉ IT bắt được); checkstyle `MethodLength` + jacoco gate ⇒ tách method + viết test cho phần fail-closed. Debt mới: `TD-17`…`TD-19`. |
| 2026-11-07 | `SL-2.1` → `Done`: thêm **`cart-service`** (8083) — service **đầu tiên không có Postgres**: Redis HASH `cart:<owner>` (owner = `u:<userId>` | `g:<guestToken>`), TTL **30 ngày** refresh mỗi lần ghi; mọi phép ghi chạy **Lua** (read-modify-write nguyên tử + bất biến `qty 1..999`/`maxLines` trong script); **guest cart** với token do **server** cấp (marker `cart:guest:<token>`); **merge khi login** cộng dồn + cap + xoá giỏ guest trong cùng script; gateway để `/v1/carts/**` public và cart tự phân xử danh tính. Verify: `mvn verify` **BUILD SUCCESS** (cart 34 unit + 6 IT, toàn repo xanh, jacoco 80.8 %) · flow thật: guest 201 → PUT idempotent → 422 biên → 401 không token/token bịa → merge cộng dồn 1+2=3 → token bị thu hồi → **10 merge song song ⇒ qty 5** · Redis: type=hash, TTL 2592000s · `e2e-smoke` 17/17 · OpenAPI 36 path/45 operation. **Sự cố**: Dockerfile mất `COPY payment pom` (bẫy copy-service lần 3); OpenAPI trùng schema `Line` ⇒ đổi `LineItem`. Debt mới: `TD-20`, `TD-21`. |
| 2026-11-08 | `SL-2.13` → `Done`: `catalog-service` có **`GET /v1/skus?ids=`** — **batch** giá/tên cho ≤ 100 SKU trong 1 request để saga `SL-2.6a` + giỏ hàng lấy giá **thật** thay vì tin giá client gửi; `unitPrice = price_override ?? product.price`; SKU archived/không tồn tại → `missingIds` (không 404 cả request); public như `GET /v1/products` (catalog tự mở `GET /v1/skus` + gateway thêm route & public-path `/v1/skus/**`). Verify: `mvn -B -ntp verify` **BUILD SUCCESS** toàn repo (catalog **79 unit** (+13) · **25 IT** (+4) · checkstyle 0 · jacoco pass) · flow thật qua gateway **không token**: 200 + giá đúng (`150000.00` override / `180000.00` giá product) trong **31 ms** · gọi thẳng `:8082` cũng 200 · trùng lặp ⇒ 1 dòng · xoá mềm product ⇒ `missingIds` (HTTP vẫn 200) · **400** cho thiếu ids / sai UUID / 101 id, **200** cho 100 id · `e2e-smoke` **17/17** · OpenAPI 36 → **37 path**. **Tách ticket**: `SL-2.6` (`L`) → **`SL-2.6a`** (saga start) + **`SL-2.6b`** (confirm/cancel/sweeper) — chốt **phương án B** (saga đọc cart + giá batch từ catalog), cập nhật `architecture.md` §5 + §6.1. Debt: **`TD-22`** (endpoint public chưa rate limit/cache) · `TD-20` cập nhật điều kiện revisit. |
| 2026-11-08 | `SL-2.6a` → `Done`: **checkout saga start** trong `order-service` — `RestClient` sync (timeout 2 s/800 ms, forward JWT user) + `POST /v1/checkout` (`Idempotency-Key`, body chỉ `{voucherCode?}`) chạy `cart → price (batch SL-2.13) → lock voucher → reserve → order PENDING_PAYMENT → payment intent`; state ở `checkout_sagas` + audit `checkout_saga_steps` (6 bước, http status, ms); **compensating theo state bền** (release stock/voucher, cancel order); fail ⇒ trả lại idempotency key; **đơn 0 đồng** (voucher 100%) không tạo payment intent (`payment_required=false`, step `SKIPPED` — ADR-0006 **D6**); 5xx của dependency ⇒ **503**. Gateway route `/v1/checkout`; compose + `.env.example` thêm 5 URL + timeout. Verify: `mvn verify` **BUILD SUCCESS** (order 55 unit + 18 IT, checkstyle 0) · chạy thật: **201** total 220000 + paymentUrl → saga `COMPLETED_START` + 6 step OK + `reserved=2` + voucher `LOCKED` · retry cùng key **200** replay cùng orderId · hết hàng ⇒ **409** + voucher `RELEASED` + saga `FAILED_STOCK` · giỏ rỗng **422** · thiếu key **400** · `e2e-smoke` 17/17 · OpenAPI 37 → **39 path**. **Sự cố**: sai tên field `lines`/`items` (stub không bắt được → stub giờ validate); Boot 4 không có bean `RestClient.Builder`; 5xx map sai sang 500. Debt mới: `TD-24`. |