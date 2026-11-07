# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (`SL-2.11` ✅ **Done** — soft delete product đã lên `main`) |
| **Ticket tiếp theo** | `SL-2.2` — `inventory-service` (reserve/commit/release, chống oversell) · `SL-2.1` — `cart-service` (Redis) |
| **Số ticket chi tiết hiện có** | 25 (Phase 0: 8 — archive · Phase 1: 15 — archive · Phase 2: 2) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | ✅ Done (tag `phase-1`) | 19 / 19 |
| 2 | Core commerce flow | 🟡 Đang làm — mô hình đã chốt ([ADR-0006](docs/adr/0006-checkout-model.md)) | 2 / 12 |
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
| 2 | `SL-2.1` | `cart-service` (8083, Redis): add/update/remove/get, TTL, **merge cart khi login** | M | Todo |
| 3 | `SL-2.2` | `inventory-service` (8084, DB `inventory`): schema `stock`/`reservations`/`stock_ledger` + API `reserve`/`commit`/`release`; **atomic** chống oversell | M | Todo |
| 4 | `SL-2.3` | inventory: **test đồng thời** (100 request cùng SKU) + reservation **TTL** & giải phóng hết hạn | M | Todo |
| 5 | `SL-2.4` | `promotion-service` (8086, DB `promotion`): voucher + rule giảm giá + `lock`/`commit`/`release` voucher | M | Todo |
| 6 | `SL-2.5` | `order-service` (8087, DB `order`): tạo order **idempotent** (idempotency key) + state machine + `order_items` **snapshot** giá | M | Todo |
| 7 | `SL-2.6` | **Checkout Saga** (orchestration): cart → reserve → lock voucher → payment → confirm; **compensating** cho mọi nhánh fail; lưu saga state | L → tách 2 | Todo |
| 8 | `SL-2.7` | `payment-service` (8088, DB `payment`): payment intent + **mock provider** + webhook **idempotent** (`idempotency_keys`) | M | Todo |
| 9 | `SL-2.8` | E2E checkout happy path (mở rộng smoke/script riêng) + OpenAPI + compose/CI matrix cho 5 service mới | M | Todo |
| 10 | `SL-2.9` | **Test nhánh fail**: hết hàng giữa chừng · payment fail · timeout ⇒ compensating chạy đúng (assert state thật) | M | Todo |
| 11 | `SL-2.10` | `docs/runbook-phase-2.md` + DoD Phase 2 (archive + tag `phase-2` + release) | S | Todo |
| 12 | `SL-2.11` | *(phát sinh từ `SL-2.0`)* `catalog-service`: **soft delete** product — `status = ARCHIVED` + `deleted_at`, `DELETE` không xoá row, list/get ẩn product đã xoá, `reserve` từ chối SKU archived | S | ✅ Done |

**Phụ thuộc & thứ tự** (cập nhật sau `SL-2.0`): `SL-2.0` ✅ chốt mô hình → **`SL-2.11`** (soft delete — `catalog` phải
xong trước để inventory biết "SKU archived" nghĩa là gì) → `SL-2.2` + `SL-2.3` (inventory — chỗ dễ oversell nhất,
làm sớm) song song được với `SL-2.1`/`SL-2.4` → `SL-2.7` (payment) → `SL-2.5`/`SL-2.6` (order + saga, phụ thuộc tất
cả) → `SL-2.8`/`SL-2.9` → `SL-2.10`.

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

---

## 7. Technical debt register

> Quy tắc ghi nhận: [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md). Mỗi món nợ **phải có
> điều kiện revisit verify được** — không phải "khi rảnh". Rà bảng này khi làm DoD mỗi phase.
> Trả xong thì giữ dòng, đổi `Mức độ` thành `✅ đã trả (<ticket>)`.

| ID | Mô tả | Nguồn | Điều kiện revisit | Mức độ |
|---|---|---|---|---|
| `TD-1` | Bảng `refresh_tokens` không có job dọn row hết hạn → phình dần | SL-1.4 | Khi bảng > 10⁶ row **hoặc** khi thêm scheduler | trung bình |
| `TD-2` | `GET /v1/admin/users` trả **toàn bộ** user, không phân trang | SL-1.4 | Khi số user > 1.000 hoặc có màn quản trị thật | thấp |
| `TD-3` | Handler map `AccessDeniedException` → 403 nằm trong **catalog**; service khác dùng `@PreAuthorize` phải copy | SL-1.5 | Khi có service **thứ hai** dùng `@PreAuthorize` (→ commons) | trung bình |
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
| `TD-14` | Bảng `idempotency_keys` không có job dọn row quá 24h ⇒ phình dần (1 row/request checkout) | SL-2.0 | Khi có scheduler chạy thật (sweeper reservation ở `SL-2.3`) ⇒ gộp 1 job dọn cho cả 2 bảng | trung bình |
| `TD-15` | Reservation sweeper là `@Scheduled` in-process ⇒ chạy trùng khi `inventory-service` > 1 instance | SL-2.0 | Khi `inventory-service` chạy ≥ 2 instance (→ `FOR UPDATE SKIP LOCKED` / advisory lock) | thấp |
| `TD-16` | Chưa có API **restore/unarchive** product đã xoá mềm; bán lại cùng product phải tạo mới | SL-2.0 | Khi seller ẩn nhầm cần khôi phục **hoặc** có UI quản trị product | thấp |

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






