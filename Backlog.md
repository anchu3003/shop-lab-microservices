# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (`SL-3.5` ✅ **Done** — `payment.succeeded.v1` ⇒ auto-confirm; **trả `TD-17`, `TD-25`**) |
| **Ticket tiếp theo** | `SL-3.6` — `catalog` outbox + **`search-service`** (8090, Postgres FTS) + reconcile + `GET /v1/search` *(merge bằng `--skip-ci-check` — `TD-28`)* |
| **Số ticket chi tiết hiện có** | 52 (Phase 0: 8 · Phase 1: 19 · Phase 2: 15 — đều đã archive · Phase 3: **10** — đang mở) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | ✅ Done (tag `phase-1`) | 19 / 19 |
| 2 | Core commerce flow | ✅ Done (tag `phase-2`) | 15 / 15 |
| 3 | Async & event-driven | 🟡 Đang làm (12 ticket) | 7 / 12 |
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
| 1 — Walking skeleton | [`Backlog-phase-1.md`](Backlog-phase-1.md) | [`docs/phases/phase-1.md`](docs/phases/phase-1.md) | `phase-1` | `SL-1.0` … `SL-1.19` | ✅ Done (19/19) |
| 2 — Core commerce flow | [`Backlog-phase-2.md`](Backlog-phase-2.md) | [`docs/phases/phase-2.md`](docs/phases/phase-2.md) | `phase-2` | `SL-2.0` … `SL-2.13` | ✅ Done (15/15) |

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

## 5. Phase 2 → 8 — Phase đang/đã làm + epic outline

> Phase 2 **đã đóng** (chi tiết ở [`Backlog-phase-2.md`](Backlog-phase-2.md)). Phase 3–8 **chỉ ở mức epic** — chỉ
> breakdown thành ticket chi tiết khi tới lượt, vì hiểu biết thay đổi sau mỗi phase (`AGENTS.md` §3.1).

### Phase 2 — Core commerce flow (✅ Done 2026-11-08 — chi tiết đã tách sang file archive)

> **Mục tiêu phase**: luồng mua hàng hoàn chỉnh, **đúng dữ liệu kể cả khi fail** — trọng tâm là *Saga +
> compensating action + idempotency + chống oversell*, không phải UI.
> **Điều kiện hoàn thành** (đã đạt): checkout end-to-end qua gateway · nhánh fail + compensating có test ·
> **100 request đồng thời cùng 1 SKU không oversell** · OpenAPI + runbook cập nhật · tag `phase-2`.

**15/15 ticket Done** (`SL-2.0` … `SL-2.13` — 10 kế hoạch + 5 phát sinh: `SL-2.6a`/`2.6b` tách từ `SL-2.6`,
`SL-2.11`, `SL-2.12`, `SL-2.13`).

- 📦 **Archive chi tiết** (verify + learning note từng ticket): [`Backlog-phase-2.md`](Backlog-phase-2.md)
- 🏷️ **Snapshot**: tag [`phase-2`](docs/phases/phase-2.md) · doc [`docs/phases/phase-2.md`](docs/phases/phase-2.md) ·
  [GitHub Release](https://github.com/anchu3003/shop-lab-microservices/releases/tag/phase-2)
- 📄 **Contract**: [`docs/api/openapi.yaml`](docs/api/openapi.yaml) — **41 path / 50 operation / 46 schema**
- 🚑 **Ops**: [`docs/runbook-phase-2.md`](docs/runbook-phase-2.md) (checkout saga · 7 lỗi · debug thật)
- 🧪 **Verify nhanh**: `bash tools/e2e-checkout.sh` (27) · `bash tools/e2e-checkout-fail.sh` (41) ·
  `bash tools/e2e-smoke.sh` (17) · `mvn -B -ntp verify` (**655 test**)

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-2.0` | **ADR-0006** chốt mô hình checkout | S | ✅ Done |
| 2 | `SL-2.1` | `cart-service` (8083, Redis) + merge khi login | M | ✅ Done |
| 3 | `SL-2.2` | `inventory-service`: reserve/commit/release chống oversell | M | ✅ Done |
| 4 | `SL-2.3` | inventory: sweeper TTL + **100 request đồng thời** | M | ✅ Done |
| 5 | `SL-2.4` | `promotion-service`: voucher + lock/commit/release | M | ✅ Done |
| 6 | `SL-2.5` | `order-service`: tạo order idempotent + snapshot giá | M | ✅ Done |
| 7 | `SL-2.6a` | Checkout **saga start** + compensating | M | ✅ Done |
| 8 | `SL-2.6b` | Checkout **saga finish**: confirm · cancel · sweeper | M | ✅ Done |
| 9 | `SL-2.7` | `payment-service`: intent + mock provider + webhook idempotent | M | ✅ Done |
| 10 | `SL-2.8` | E2E happy path + workflow `e2e.yml` + OpenAPI | M | ✅ Done |
| 11 | `SL-2.9` | E2E nhánh fail (F1…F6) + hook bơm lỗi | M | ✅ Done |
| 12 | `SL-2.10` | `docs/runbook-phase-2.md` + **DoD Phase 2** | S | ✅ Done |
| 13 | `SL-2.11` | *(phát sinh)* catalog soft delete (`TD-12`) | S | ✅ Done |
| 14 | `SL-2.12` | *(phát sinh)* 403 handler → `commons` (`TD-3`) | S | ✅ Done |
| 15 | `SL-2.13` | *(phát sinh)* `GET /v1/skus?ids=` batch giá | S | ✅ Done |

**Debt đã trả trong phase**: `TD-12` (`SL-2.11`) · `TD-4` (`SL-2.8`) · `TD-3` (`SL-2.12`) · `TD-14` (`SL-2.5`) ·
`TD-15` (`SL-2.3`).
**Debt còn mở từ phase**: `TD-17` (webhook không cập nhật order — **cao**) · `TD-21`/`TD-22` (chưa rate limit) ·
`TD-23` (IT inventory flaky) · `TD-25` (sweeper saga không có token user) · `TD-27` (E2E fail-branch flaky) — xem §7.

### Phase 3 — Async & event-driven (breakdown `SL-3.x`)

> **Mục tiêu**: phần **không critical** chạy async, và **event không bị mất** khi có sự cố — trọng tâm là
> *transactional outbox + at-least-once + consumer idempotent + DLQ*, không phải "dùng được Kafka".
> **Điều kiện hoàn thành**: `order.confirmed` phát ra **từ outbox** → `notification-service` gửi email (mock)
> async · `payment.succeeded` tự confirm đơn (**trả `TD-17`**) · index search cập nhật sau khi product đổi ·
> message lỗi vào **DLQ** (không chặn queue) · có báo cáo số liệu async · runbook + tag `phase-3`.

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-3.0` | **ADR-0007** — contract event: envelope · đặt tên · **outbox relay** (polling) · **DLQ policy** · dedupe | S | ✅ Done |
| 2 | `SL-3.1` | **RabbitMQ**: topology (exchange/queue/DLQ) + healthcheck + `tools/rabbit-topology.sh` | S | ✅ Done |
| 3 | `SL-3.2` | `service-commons`: `EventEnvelope` + publisher (confirm) + consumer base (retry → DLQ) + dedupe `processed_events` | M | ✅ Done |
| 4 | `SL-3.3` | `order-service`: **transactional outbox** + relay ⇒ `order.confirmed.v1` / `order.cancelled.v1` | M | ✅ Done |
| 5 | `SL-3.4` | **`notification-service`** (8089): consumer `order.confirmed` → email mock + retry + **DLQ** + `tools/dlq.sh` | M | ✅ Done |
| 6 | `SL-3.5` | `payment-service` outbox + `payment.succeeded.v1` ⇒ `order-service` **auto-confirm** (**trả `TD-17`** + `TD-25`) | M | ✅ Done |
| 7 | `SL-3.6` | `catalog-service` outbox product event + **`search-service`** (8090, Postgres **FTS**) index + reconcile + `GET /v1/search` | M | Todo |
| 8 | `SL-3.7` | E2E async (`tools/e2e-async.sh`) + bước trong `e2e.yml` | S | Todo |
| 9 | `SL-3.8` | **Báo cáo async**: latency checkout · độ trễ confirm→email · throughput relay · DLQ = 0 dưới tải | S | Todo |
| 10 | `SL-3.9` | `docs/runbook-phase-3.md` + **DoD Phase 3** (archive · tag `phase-3` · release) | S | Todo |
| 12 | `SL-3.11` | **Service-to-service auth**: `client_credentials` (identity) + `aud`/`exp` + validator ở commons (**tách từ `SL-3.5`**) | M | ✅ Done |
| 11 | `SL-3.10` | *(phát sinh 2026-11-08, quyết định của human)* **process**: tắt CI tạm thời (`CI_ENABLED=false`) để tăng tốc merge Phase 3 + `merge-pr.sh --skip-ci-check` | S | ✅ Done |

**Quyết định đã chốt khi breakdown** *(2026-11-08)*:

- **Broker = RabbitMQ** (đã chốt ở `architecture.md` §11.2): routing + DLQ có sẵn, học concept nhanh; **Kafka để Phase 8**
  (event streaming/CDC) — so sánh 2 broker là bài học trade-off, không làm sớm.
- **`search-service` dùng Postgres FTS ở Phase 3** (DB riêng `search`, `tsvector` + `unaccent`), **OpenSearch để Phase 8**.
  Lý do: docs cũ mâu thuẫn (§5 ghi OpenSearch Phase 3, §7/§12 ghi Phase 8); Phase 3 cần học **event-driven index +
  reconcile**, thứ đó không phụ thuộc engine. Đổi storage ở Phase 8 mà **không đổi event contract** ⇒ đã sửa `architecture.md`.
- **Outbox relay = polling** (`@Scheduled` 1 s, batch 100, `FOR UPDATE SKIP LOCKED`) chứ không dùng CDC/Debezium:
  thêm 1 hạ tầng nữa là over-engineer ở quy mô này. Revisit khi throughput > 500 msg/s hoặc relay trễ p99 > 5 s.
- **At-least-once, không hứa exactly-once**: mọi consumer **bắt buộc** idempotent theo `eventId` (`processed_events`).
- **DLQ không auto-replay**: chỉ vào khi retry cạn; replay bằng `tools/dlq.sh` (có người quyết định).

**Phụ thuộc & thứ tự**: `SL-3.0` (chốt contract) → `SL-3.1` (broker) → `SL-3.2` (commons: envelope/publisher/consumer) →
`SL-3.3` (outbox ở order) → `SL-3.4` (consumer đầu tiên: notification — vertical slice đầu tiên chạy end-to-end) →
`SL-3.5` (payment event + auto-confirm, trả `TD-17`) → `SL-3.6` (catalog outbox + search index) → `SL-3.7` (E2E) →
`SL-3.8` (báo cáo) → `SL-3.9` (DoD). `SL-3.2` và `SL-3.6` (phần catalog outbox) có thể làm song song sau `SL-3.3`.

**Debt sẽ trả trong phase này**: `TD-17` (**cao** — webhook không cập nhật order ⇒ `SL-3.5`).
**Debt mới dự kiến**: outbox/search table chưa có job dọn (ghi `TD` khi làm), `TD-23`/`TD-27` (flaky) vẫn mở.

> Mỗi ticket vẫn phải viết **đặc tả BA đầy đủ** (In/Out · AC verify được · ghi chú kỹ thuật) **ngay trước khi bắt đầu**
> — bảng trên là phạm vi + thứ tự, không thay thế đặc tả (`AGENTS.md` §4, §5).

### SL-3.0 — ADR-0007: contract event (envelope · naming · outbox relay · DLQ)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-2.10` (Phase 2 đóng); chặn **mọi** ticket `SL-3.x` |
| **Ước lượng** | S |

**Bối cảnh**

Phase 2 gọi sync 5 dependency: nhanh, dễ debug, nhưng **sai một cái là cả checkout fail** và không có cách nào để phần
"không critical" (email, index search) chạy sau. Phase 3 thêm broker — nhưng nếu mỗi service tự đặt tên event, tự chọn
kiểu payload thì 3 tháng sau không ai đọc được message trong DLQ. Contract phải chốt **trước** dòng code đầu tiên
(`AGENTS.md` §3.3).

**Mục tiêu**

`docs/adr/0007-event-contract.md` chốt 5 quyết định: **envelope**, **đặt tên**, **topology**, **relay outbox**, **DLQ**.

**In scope**

- Envelope JSON: `eventId` (uuid) · `eventType` (string, có `.vN`) · `version` (int) · `occurredAt` (ISO-8601 UTC) ·
  `producer` (tên service) · `traceId` · `payload` (object). **Không** đặt field nghiệp vụ ở tầng envelope.
- Đặt tên: routing key `<aggregate>.<event>.v<major>` (vd `order.confirmed.v1`) trên exchange **topic**
  `shoplab.events`; queue theo **consumer** (`q.notification`, `q.order`, `q.search`) + `<queue>.dlq`; exchange chết `shoplab.dlx`.
- Ngữ nghĩa giao: **at-least-once** + consumer idempotent theo `eventId`; **không** hứa exactly-once/ordering toàn cục
  (ordering chỉ trong 1 queue + 1 consumer).
- Relay outbox: polling `@Scheduled` (mặc định 1 s, batch 100, `FOR UPDATE SKIP LOCKED`) + **publisher confirm** rồi mới
  đánh `SENT`. Nêu rõ vì sao **không** CDC/Debezium ở phase này và **điều kiện revisit**.
- DLQ: retry trong consumer 3 lần (backoff 200 ms/1 s/3 s) → `nack(requeue=false)` vào `<queue>.dlq`; **không auto-replay**;
  replay bằng tay (`tools/dlq.sh`). Ghi rõ ai được replay và cần kiểm gì trước khi replay.
- Versioning: **không** sửa payload `.v1`; thay đổi không tương thích ⇒ phát `.v2` song song, consumer hỗ trợ cả hai rồi
  mới bỏ `.v1`. Bảng 5 event khởi điểm: `order.confirmed.v1` · `order.cancelled.v1` · `payment.succeeded.v1` ·
  `product.updated.v1` · `product.archived.v1` (kèm producer/consumer/payload field).

**Out of scope**

- Không viết code broker/publisher/consumer (đó là `SL-3.1`/`SL-3.2`).
- Không bàn Kafka (đã chốt ở §11.2 — Phase 8), không bàn schema registry (không cần với 5 event).
- Không thiết kế event cho shipping/review (Phase 8).

**Acceptance Criteria**

- [x] `docs/adr/0007-event-contract.md` có đủ Context · Options · Decision · Consequences · **Revisit** (đúng skill `shoplab-adr`).
- [x] Có **bảng 5 event** với: routing key · producer · consumer · payload field · khi nào phát.
- [x] Có câu trả lời tường minh cho: *vì sao polling relay, không CDC* · *vì sao at-least-once* · *DLQ replay do ai*.
- [x] Mỗi quyết định có **điều kiện revisit verify được** (số liệu/ngưỡng), không phải "khi cần".
- [x] `architecture.md` §9.4 (API & contract) + §11.2 link tới ADR-0007.

**Ghi chú kỹ thuật**

- ADR này **cố ý ngắn** (không quá ~150 dòng): nó chốt *hợp đồng*, không dạy lại lý thuyết messaging.
- Ghi rõ **cái đã bỏ**: exactly-once (đắt, cần transactional broker), schema registry, outbox CDC — kèm điều kiện revisit.

**Kết quả verify** *(2026-11-08 — ticket docs-only, không có code)*

```console
$ ls docs/adr/
0001-tech-stack.md  0002-dev-environment.md  0003-spring-boot-version-and-module-layout.md
0004-service-commons-shared-library.md  0005-phase-snapshot-and-release.md
0006-checkout-model.md  0007-event-contract.md          ← mới

$ grep -n '^## ' docs/adr/0007-event-contract.md
11:## Context   43:## Options   89:## Decision   147:## Consequences   174:## Revisit
$ wc -l docs/adr/0007-event-contract.md
185 docs/adr/0007-event-contract.md                      (trong ngưỡng "~150 dòng" của ticket, +35 cho bảng event/topology)

# 5 quyết định đều có bảng Options (≥ 3 phương án, có phương án bị loại) + Revisit định lượng:
$ grep -c '^### [0-9]\. (D' docs/adr/0007-event-contract.md
5
$ grep -c '| **A\.' docs/adr/0007-event-contract.md     # mỗi D có phương án A (được chọn)
5
```

**Nội dung đã chốt** (đối chiếu AC):

| AC | Bằng chứng trong ADR |
|---|---|
| Bảng 5 event | § Decision → bảng `Routing key · Producer · Consumer · Payload · Phát khi` (5 dòng) |
| Vì sao polling relay | § Options D4 (4 phương án, loại CDC + publish-in-transaction + `LISTEN/NOTIFY` vì lý do cụ thể) |
| Vì sao at-least-once | § Options D3 (loại exactly-once vì broker không tham gia transaction DB; loại at-most-once vì mất message) |
| DLQ replay do ai | § Decision D5 (không auto-replay + 3 điều phải kiểm trước khi replay) + § Options D5 (loại auto-replay) |
| Revisit định lượng | § Revisit: relay p95 > 5 s **hoặc** > 30% CPU **hoặc** > 500 msg/s · schema registry khi > 20 event / ≥ 2 sự cố · DLQ > 50 msg/tuần |
| `architecture.md` link | §9.4 (bỏ "schema registry từ Phase 3") + §11.2 + header `DRAFT v0.5` + Changelog §8 |

**Sự cố trong lúc làm (2 lỗi do chính mình, đã sửa)**

- Chèn section bằng `insert_line` vào **giữa** bảng Options D3 ⇒ dòng `At-most-once` bị đẩy xuống cuối file. Phát hiện
  bằng `grep -n '^## '` thấy `## Decision` nằm trước nội dung bảng. Sửa bằng script di chuyển dòng về đúng chỗ.
- Lọt **1 từ tiếng Trung** (chữ Hán của "bên thứ ba") trong ô so sánh CloudEvents — `grep` theo ký tự Hán mới thấy. Đã đổi thành
  "bên thứ ba".

**Learning note**

- **ADR là chỗ *loại* phương án, không phải chỗ khoe phương án đã chọn**: 4/5 hạng mục có ≥ 1 phương án ghi rõ
  "❌ Không dùng" + lý do (publish-trong-transaction: broker không tham gia transaction DB ⇒ hoặc mất event hoặc bịa
  event). Nếu chỉ viết "chọn A vì A tốt" thì 3 tháng sau sẽ có người đề xuất lại đúng phương án đã loại.
- **Quyết định nào cũng phải kéo theo một *điều kiện bỏ* định lượng được**: `Revisit` ở đây toàn số (p95 > 5 s, > 30% CPU,
  > 500 msg/s, > 20 event, > 50 msg DLQ/tuần). Không có con số thì "khi cần" = không bao giờ.
- **Chốt contract trước khi code là rẻ nhất**: ADR này ~185 dòng nhưng nó khoá 6 ticket phía sau (`SL-3.1`…`SL-3.6`);
  đổi tên routing key hay bỏ outbox sau khi có code = migration message + sửa 3 service + sửa E2E.
- **Doc cũ có thể mâu thuẫn với chính nó**: §9.4 ghi "schema registry từ Phase 3" trong khi quyết định là **không** dùng
  ⇒ phải sửa §9.4 ngay trong ticket này, không để "lát nữa" (bài học `SL-2.10`: doc cũng mục như code).
- **Công cụ soạn doc cũng cần verify**: 2 lỗi (chèn sai vị trí, lọt chữ Trung) đều **không** bị lint/CI bắt — chỉ
  `grep -n '^## '` và đọc lại mới thấy. Với PR docs-only (không có CI) thì bước tự đọc lại là **bắt buộc**.

### SL-3.1 — RabbitMQ: topology + healthcheck + công cụ kiểm tra

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.0` (tên exchange/queue chốt ở ADR-0007); chặn `SL-3.2`+ |
| **Ước lượng** | S |

**Bối cảnh**

`rabbitmq` nằm trong compose **từ Phase 0** nhưng chưa một dòng code nào publish/consume — tức là chưa ai chứng minh nó
thật sự chạy được, chưa có healthcheck thật, và chưa có topology. Đây là "hạ tầng trang trí", đúng thứ `AGENTS.md` §3.4
muốn tránh nếu kéo sang phase sau.

**Mục tiêu**

Broker chạy được, có topology khai báo bằng code, và có **một lệnh** để xem exchange/queue/DLQ/consumer.

**In scope**

- Compose: user/password lấy từ `.env` (`RABBITMQ_DEFAULT_USER/PASS`), healthcheck `rabbitmq-diagnostics -q ping`,
  publish port 5672 + management **15672** (chỉ local), volume dữ liệu (queue durable sống qua restart).
- Topology khai báo bằng code trong `service-commons` (`Declarables`): exchange topic `shoplab.events`, exchange
  `shoplab.dlx`, 3 queue consumer + 3 `.dlq`, binding theo routing key (danh sách từ ADR-0007).
- `tools/rabbit-topology.sh`: in exchange/queue (ready, unacked, consumer count) qua management API, **không** cần UI.
- README: mục "Broker" — mở UI `localhost:15672`, lệnh xem queue, ý nghĩa `ready`/`unacked`.

**Out of scope**

- Không viết publisher/consumer nghiệp vụ (`SL-3.2`+). Không sharding/cluster broker (single node là đủ để học).
- Không đổi `.env.example` của service nào (chỉ thêm biến broker).

**Acceptance Criteria**

- [x] `docker compose ps` → `shoplab-rabbitmq` **healthy**; `docker restart` xong queue + binding **vẫn còn**.
- [x] `bash tools/rabbit-topology.sh` in ra: 1 exchange `shoplab.events` · 1 `shoplab.dlx` · 3 queue + 3 DLQ · binding đúng routing key.
- [x] Publish 1 message test (không có consumer) → `ready = 1`; consume bằng UI/script → `ready = 0`, `ack` thành công.
- [x] Message vào queue khi **không** có consumer vẫn còn sau `docker restart` broker (durable + persistent).
- [x] `mvn verify` không đổi (chưa có code nghiệp vụ); `e2e-smoke`/`e2e-checkout` không regress.

**Ghi chú kỹ thuật**

- **Khai topology bằng code** (không dùng `definitions.json`) để nó luôn khớp tên trong ADR-0007 và không phải import tay
  khi dựng máy mới.
- `unacked` là chỉ số hay bị bỏ qua: nó là **consumer đang giữ message** (đang retry) — DLQ chỉ tăng khi message bị
  `nack` hẳn.

**Kết quả verify** *(chạy thật 2026-11-08 — 12 container healthy)*

**1) Topology có thật trên broker** (`bash tools/rabbit-topology.sh`):

```console
== Exchanges (shoplab.*)
  shoplab.dlx          type=direct   durable=True
  shoplab.events       type=topic    durable=True
== Queues (ready · unacked · consumers)
  q.notification / q.notification.dlq / q.order / q.order.dlq / q.search / q.search.dlq   (6 queue, ready=0)
== Bindings
  shoplab.dlx    --q.notification       --> q.notification.dlq
  shoplab.dlx    --q.order              --> q.order.dlq
  shoplab.dlx    --q.search             --> q.search.dlq
  shoplab.events --order.cancelled.v1   --> q.notification
  shoplab.events --order.confirmed.v1   --> q.notification
  shoplab.events --payment.succeeded.v1 --> q.order
  shoplab.events --product.archived.v1  --> q.search
  shoplab.events --product.updated.v1   --> q.search
```

**2) Publish → restart broker → message vẫn còn → drain ack:**

```console
$ bash tools/rabbit-topology.sh publish product.updated.v1
publish product.updated.v1 -> {"routed":true}          →  q.search ready=1
$ docker restart shoplab-rabbitmq                      # → healthy lại sau ~15 s
$ curl …/api/queues/%2F/q.search | …                   # messages=2 ready=2  ← KHÔNG mất
$ bash tools/rabbit-topology.sh drain q.search 2
  ack  routing_key=product.updated.v1  payload={"ping": true}
  ack  routing_key=product.updated.v1  payload={"ping": true}
$ bash tools/rabbit-topology.sh | grep 'q.search '     # ready=0  (ack xong)
```

**3) Bẫy im lặng `routed=false`** (gõ sai routing key) — tool in cảnh báo ngay:

```console
$ bash tools/rabbit-topology.sh publish product.updated.v9
publish product.updated.v9 -> {"routed":false}
  (routed=false nghĩa là KHÔNG queue nào bind routing key đó — sai tên key!)
```

**4) Test + không regress:**

```text
$ mvn -B -ntp verify          BUILD SUCCESS · 11 module · 662 test (544 unit + 118 IT) · 0 fail
                              service-commons 54 unit (+7: EventTopologyAutoConfigurationTest 5 · EventTopologyDeclarerTest 2)
$ e2e-smoke 17/17 · e2e-checkout 27/27 · e2e-checkout-fail 41/41
```

**Sự cố trong lúc verify (1 lỗi thiết kế + 2 lỗi phụ)**

1. **Topology KHÔNG xuất hiện dù code đã khai** — image có đủ `spring-rabbit`, nhưng script in rỗng. Nguyên nhân:
   `RabbitAdmin` khai **lazy**, chỉ khi có connection đầu tiên; SL-3.1 chưa có publisher/consumer ⇒ **không ai mở
   connection** ⇒ không khai. → Thêm `EventTopologyDeclarer` gọi `amqpAdmin.initialize()` lúc `SmartInitializingSingleton`.
2. **Message "mất" sau restart** (đọc `ready=0` ngay sau khi broker healthy): thực ra **không mất** — `/api/queues`
   tổng hợp số liệu ~5 s/lần nên trả số cũ. Kiểm bằng `messages` + `drain` mới đúng.
3. **Tool sai escaping**: `python3 -c '…'` trong single quote làm `\"` thành literal ⇒ `SyntaxError`; chuyển sang
   `.format()` + double quote, `publish` nhận body qua heredoc.

**Learning note**

- **"Khai bằng code" không có nghĩa là "đã khai"**: `Declarables` chỉ là *ý định*; việc khai thật do `RabbitAdmin`
  làm **lazy** khi mở connection. Service chỉ-khai-topology (chưa publish/consume) sẽ im lặng không có queue nào ⇒
  phải kiểm bằng **trạng thái của hệ ngoài** (management API), không kiểm bằng "code đã chạy".
- **Fail mềm là quyết định, không phải tiện tay**: broker chết lúc start **không** được làm service chết (async không
  nằm trên đường critical — ADR-0007). Nhưng fail mềm có giá: lỗi chỉ còn 1 dòng `WARN` ⇒ phải có tool để *nhìn thấy*
  hậu quả.
- **Monitoring có độ trễ riêng**: `ready/unacked` lag ~5 s sau restart ⇒ đọc "0 message" **không** chứng minh mất dữ
  liệu. Trước khi kết luận, kiểm bằng đường thứ hai (`messages` hoặc `drain` để ack thật).
- **Tool vận hành phải tự bảo vệ người dùng**: `routed=false` của RabbitMQ là lỗi **im lặng** (message rơi vào hư
  không, không exception) ⇒ tool in cảnh báo ngay cạnh kết quả và có `dlq`/`drain`/`purge` để không phải mở UI làm tay.
- **Test topology không cần broker**: `Declarables` là dữ liệu thuần ⇒ unit test khẳng định tên/durability/DLX-args/
  binding. Đây là test **hợp đồng** với ADR-0007 — routing key sai 1 ký tự đỏ ngay trong CI thay vì lặng lẽ mất message.


### SL-3.2 — `service-commons`: envelope + publisher + consumer base + dedupe

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.0` (contract) · `SL-3.1` (topology); chặn `SL-3.3`·`SL-3.4`·`SL-3.5`·`SL-3.6` |
| **Ước lượng** | M |

**Bối cảnh**

Nếu mỗi service tự gọi `rabbitTemplate.convertAndSend(...)` thì sẽ lặp lại đúng 3 lỗi: quên `traceId` (log không nối được),
quên dedupe (xử lý 2 lần khi message redeliver), và retry vô hạn (queue treo). Cùng bài học `SL-2.12`: hành vi **của toàn hệ**
thì phải nằm ở `service-commons`, không copy 6 lần.

**Mục tiêu**

Service mới chỉ cần khai dependency là có: publish đúng envelope, consume có retry → DLQ, dedupe theo `eventId`.

**In scope**

- `commons/events/EventEnvelope` (record) + `EventEnvelopeCodec` (JSON, `occurredAt` ISO-8601, **reject** `version` không hỗ trợ).
- `EventPublisher`: publish lên `shoplab.events` với `routingKey = eventType`; bật **publisher confirm**
  (`spring.rabbitmq.publisher-confirm-type=correlated`), message `persistent`, `traceId` lấy từ MDC nếu caller không truyền.
- `EventConsumer` (base class + dispatcher): parse envelope → `MDC.put(traceId)` → **dedupe** → gọi handler → `ack`;
  lỗi ⇒ retry **3 lần** (200 ms / 1 s / 3 s) ⇒ `nack(requeue=false)` ⇒ DLQ; mọi nhánh log đều có `eventId`.
- `ProcessedEventStore` (interface) + `JdbcProcessedEventStore` (`insert into processed_events(event_id, consumer, event_type)
  on conflict do nothing` ⇒ `0 row` = đã xử lý): kèm **SQL mẫu** trong javadoc, **không** ship Flyway migration
  (mỗi service tự thêm vào DB của mình).
- `EventAutoConfiguration`: `@ConditionalOnClass(RabbitTemplate)` + `@ConditionalOnProperty(shoplab.events.enabled)` ⇒
  service **không** dùng event (template, gateway) không bị ảnh hưởng.
- Unit test: codec (round-trip · version lạ) · dedupe (handler chỉ chạy 1 lần với cùng `eventId`) · retry → DLQ (mock `Channel`) ·
  auto-config back-off (`FilteredClassLoader`).

**Out of scope**

- Không có event nghiệp vụ nào trong commons (chỉ hạ tầng). Không schema registry. Không bàn ordering/partition key.
- Không làm outbox ở đây — outbox ghi vào DB của **từng service** (`SL-3.3`/`SL-3.5`/`SL-3.6`).

**Acceptance Criteria**

- [x] Publish 1 event từ test ⇒ message có đủ 7 field envelope; `traceId` khớp MDC của caller.
- [x] Consumer nhận **cùng `eventId` 2 lần** ⇒ handler chạy **1 lần**, lần 2 `ack` luôn (không vào DLQ).
- [x] Handler ném exception ⇒ retry 3 lần rồi **1 message** ở `<queue>.dlq`, `<queue>` rỗng, `unacked = 0`.
- [x] Service **không** khai `spring-boot-starter-amqp` vẫn boot bình thường (test `FilteredClassLoader` + `service-template` xanh).
- [x] `commons` unit test xanh **không cần broker thật** (mock `RabbitTemplate`/`Channel`); `mvn -B -ntp verify` toàn repo xanh.

**Ghi chú kỹ thuật**

- **Dedupe ở DB của consumer, không ở Redis**: Redis có thể mất dữ liệu (cart chấp nhận được, dedupe thì không), và dedupe
  phải nằm cùng transaction với việc xử lý. Bảng `processed_events` phình dần ⇒ ghi `TD` về job dọn.
- **Retry trong consumer** (blocking, giữ `unacked`) khác **retry ở transport** (RabbitMQ không tự retry): chọn retry trong
  consumer để còn log/inspect được — trade-off đã biết là consumer giữ thread ~4,2 s.
- `@ConditionalOnClass` phải ở **cấp class auto-config** (bài học `SL-2.12`: đặt trên `@Bean` method ⇒ `NoClassDefFoundError`).

**Kết quả verify** *(chạy thật 2026-11-08 — 12 container healthy)*

**1) `service-commons` 71 unit test xanh — 15 test mới cho hạ tầng event:**

```text
EventEnvelopeCodecTest          5   round-trip 7 field · version lạ ⇒ EventFormatException ·
                                    version payload ≠ hậu tố eventType ⇒ từ chối · JSON hỏng/thiếu field
EventPublisherTest              2   envelope đủ field + traceId từ MDC · broker nack ⇒ EventPublishException
AbstractEventConsumerTest       5   ack khi xong · trùng eventId ⇒ handler 1 lần · retry cạn ⇒ nack(false)
                                    · payload hỏng ⇒ nack ngay · event lạ ⇒ ack + bỏ qua
EventAutoConfigurationTest      3   có AMQP ⇒ codec/publisher/container MANUAL ack · JdbcTemplate ⇒ store
                                    · enabled=false ⇒ không bean · không AMQP ⇒ back-off
```

**2) Toàn repo:** `mvn -B -ntp verify` **BUILD SUCCESS · 679 test (561 unit + 118 IT) · 0 fail**.

**3) Trên stack thật** (rebuild `order-service` — service duy nhất có AMQP):

```console
$ docker ps | grep order        → shoplab-order-service Up (healthy)
$ docker exec shoplab-postgres psql -U shoplab -d order -c '\d processed_events'
                     Table "public.processed_events"
    Column    |           Type        | Nullable | Default
 event_id     | uuid                 | not null |
 consumer     | character varying(64)| not null |
 event_type   | character varying(128)| not null |
 processed_at | timestamp with time zone | not null | now()
Indexes: "processed_events_pkey" PRIMARY KEY, btree (event_id, consumer)

$ bash tools/e2e-smoke.sh 17/17 · e2e-checkout 27/27 · e2e-checkout-fail 41/41
```

Bảng do commons tạo hộ (`shoplab.events.dedup.auto-create-schema=true`) — bằng chứng store dedupe **có**
được tạo và **có** DDL đúng khoá `(event_id, consumer)`.

**Sự cố trong lúc verify (1 bug thật — loại bug im lặng nguy hiểm nhất)**

`processed_events` **không được tạo** dù service chạy healthy và log sạch. Nguyên nhân: `@ConditionalOnBean
(JdbcTemplate.class)` chỉ thấy bean **đã đăng ký tại thời điểm nó được đánh giá**; không khai thứ tự thì
auto-config của commons chạy **trước** `JdbcTemplateAutoConfiguration` ⇒ điều kiện luôn `false` ⇒ bean store
không bao giờ được tạo, **không một dòng lỗi**. Fix: `@AutoConfiguration(after = RabbitAutoConfiguration.class,
afterName = "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration")` + 2 test khoá lại
(có `JdbcTemplate` ⇒ có store; không có ⇒ không store, không lỗi).
Lưu ý phụ: Boot 4 đặt `JdbcTemplateAutoConfiguration` ở `org.springframework.boot.jdbc.autoconfigure` (module
`spring-boot-jdbc`) nên phải dùng `afterName` (string) — dùng `after = …class` sẽ buộc commons kéo thêm
dependency chỉ để tham chiếu một cái tên.

**Learning note**

- **`@ConditionalOnBean` là điều kiện *theo thời điểm*, không phải "bean này sẽ có"**: auto-config chạy theo
  thứ tự, và điều kiện chỉ thấy những gì đã đăng ký trước đó. Không khai `after`/`afterName` ⇒ điều kiện
  âm thầm `false`, bean không được tạo, **không có lỗi nào để lần theo**. Đây là họ bug giống `SL-3.1`
  (`RabbitAdmin` khai lazy): code đúng, hệ không làm gì, log sạch.
- **Cách phát hiện**: kiểm bằng **trạng thái của hệ ngoài** (bảng trong DB, exchange trên broker), không kiểm
  bằng "service healthy". Cả 2 lần trong phase 3 bug đều nằm ở khoảng giữa "app khởi động xong" và "hệ có
  thứ mình cần".
- **Marker dedupe phải xoá khi xử lý fail**: nếu chỉ `insert` trước khi handler chạy thì DLQ replay sẽ bị
  chính marker đó chặn ⇒ message nằm trong DLQ mà **không bao giờ** chạy lại được. Thiết kế đúng: mark →
  handle → (fail) unmark → `nack(requeue=false)`; test `shouldNackAfterRetriesExhausted` chốt điều này.
- **`AcknowledgeMode.MANUAL` là điều kiện của DLQ**: auto-ack nghĩa là message bị coi "xong" ngay khi
  listener trả về, nên không còn cơ hội `nack` khi handler fail ⇒ mất message trong im lặng. Test kiểm
  **container thật** (`createListenerContainer`) chứ không chỉ "bean tồn tại".
- **Thứ tự cũng là hợp đồng**: `markProcessed` phải nguyên tử (`insert … on conflict do nothing` trả
  `rowsAffected`) — kiểu "select rồi insert" vẫn có khe hở khi 2 lần redeliver tới cùng lúc.


### SL-3.3 — `order-service`: transactional outbox + relay ⇒ `order.confirmed.v1`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` (envelope/publisher); chặn `SL-3.4`/`SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

Đây là bài toán gốc của outbox: `order` chuyển `CONFIRMED` (commit DB) rồi mới publish — nếu process chết **giữa** hai bước
thì đơn đã confirmed mà **không ai biết** để gửi email. Ngược lại publish trước rồi ghi DB thì có thể phát event cho đơn
không tồn tại. Phase 2 giải quyết bằng cách **không** phát event; Phase 3 phải phát mà **không mất, không bịa**.

**Mục tiêu**

Mọi thay đổi state quan trọng của đơn ghi **outbox trong cùng transaction**, relay lo việc publish → `order.confirmed.v1` /
`order.cancelled.v1` tới được consumer **ít nhất một lần**.

**In scope**

- Migration `V4__create_outbox.sql`: `outbox(id, aggregate_type, aggregate_id, event_type, payload jsonb, status
  PENDING/SENT, attempts, last_error, created_at, sent_at)` + index `(status, created_at)` + CHECK status.
- Ghi outbox **trong cùng `@Transactional`** ở `CheckoutFinishService` (confirm) và luồng cancel.
- `OutboxRelay` (`@Scheduled`, env `SHOPLAB_OUTBOX_INTERVAL_MS`, `…BATCH_SIZE`): `FOR UPDATE SKIP LOCKED` → publish +
  **chờ confirm** → `SENT`; lỗi ⇒ `attempts++`, `last_error`, **giữ `PENDING`** (không mất event).
- Payload `order.confirmed.v1`: `{orderId, userId, total, currency, itemCount, confirmedAt}` (đủ để consumer hiểu, **không**
  copy toàn bộ đơn — payload lớn là nợ của tương lai).
- Test: unit (relay gửi + đánh SENT; lỗi ⇒ giữ PENDING, attempts tăng) · IT (confirm ⇒ đúng **1** row outbox; broker chết
  ⇒ order vẫn `CONFIRMED`, row `PENDING`; broker sống lại ⇒ `SENT`) · IT hai relay chạy song song ⇒ không gửi trùng.

**Out of scope**

- Không làm notification (`SL-3.4`), không làm event của payment (`SL-3.5`), không dọn row `SENT` (job retention ⇒ ghi `TD`).
- Không đổi payload/state machine của order (Phase 2 đã chốt).

**Acceptance Criteria**

- [x] `POST /v1/checkout/{id}/confirm` thành công ⇒ **1 row** `outbox` `PENDING` cùng transaction, sau ≤ 2 s thành `SENT`
      và có message `order.confirmed.v1` trên `q.notification` (kiểm bằng `tools/rabbit-topology.sh` / psql).
- [x] `docker stop shoplab-rabbitmq` rồi confirm ⇒ order **vẫn `CONFIRMED`** (không 503), row giữ `PENDING`,
      `attempts > 0`; `docker start` ⇒ message được gửi, row `SENT`.
- [x] Cancel ⇒ `order.cancelled.v1` phát ra tương tự; confirm#2 (idempotent) ⇒ **không** sinh row outbox thứ hai.
- [x] Chạy 2 instance relay (hoặc 2 vòng song song trong IT) ⇒ mỗi row chỉ publish **1 lần**, không message trùng.
- [x] `mvn -B -ntp verify` xanh; `e2e-checkout` 27/27 + `e2e-checkout-fail` 41/41 không regress.

**Ghi chú kỹ thuật**

- **Outbox là bảng, không phải queue**: publish phải là *hệ quả* của transaction đã commit, không phải một phần của nó.
- **Giữ `PENDING` khi lỗi** (không `FAILED`): event chưa gửi thì vẫn còn nghĩa vụ gửi; `attempts` + `last_error` đủ để biết
  cái nào đang kẹt.
- `FOR UPDATE SKIP LOCKED` (đúng khuôn `SL-2.3`): nhiều instance relay không giẫm nhau, không cần leader election.

**Kết quả verify** *(chạy thật 2026-11-08 — CI đang tắt theo `SL-3.10`, verify local thay CI)*

**1) `mvn -B -ntp verify` (toàn repo): BUILD SUCCESS · 687 test (566 unit + 121 IT) · 0 fail**
`order-service`: **70 unit** (+5 `OutboxRelayTest`) + **28 IT** (+3 outbox).

**2) Stack thật — confirm ⇒ outbox `PENDING` → `SENT` → message trên `q.notification`:**

```console
### 1) checkout + thanh toán + confirm
order=a5c51d63-…   confirm status=200
### 2) outbox NGAY sau confirm
PENDING|0                         ← ghi cùng transaction, relay chưa chạy
### 3) sau ~3 s (relay 1 s/nhịp)
SENT|1|t                          ← status=SENT, attempts=1, có sent_at
### 4) order CONFIRMED + payload
{"total": 100000.00, "userId": "fa72fd12-…", "orderId": "a5c51d63-…",
 "currency": "VND", "itemCount": 1, "confirmedAt": "2026-11-08T11:56:33…"}
### 5) message tới queue
  q.notification         ready= 14 unacked=  0 consumers=0
```

**3) Broker chết ⇒ order VẪN CONFIRMED, event không mất:**

```console
$ docker stop shoplab-rabbitmq
$ bash … confirm            → confirm status=200        ← KHÔNG 503 (publish không nằm trên đường commit)
PENDING|3|f                 ← vẫn PENDING, attempts tăng mỗi nhịp
$ (sau 12 s) orders.status=CONFIRMED · outbox.status=PENDING · attempts=11
$ docker start shoplab-rabbitmq   → healthy
SENT|21                     ← tự gửi lại thành công, KHÔNG mất event
```

**4) Cancel ⇒ `order.cancelled.v1`:**

```console
$ POST /v1/checkout/{id}/cancel → 200
$ psql: order.cancelled.v1|SENT
```

**5) Không regress:** `e2e-smoke` 17/17 · `e2e-checkout` 27/27 · `e2e-checkout-fail` 41/41.

**Sự cố trong lúc verify (2 bug thật — cả 2 đều bị outbox "che" an toàn)**

1. **Publisher confirm chưa bật** ⇒ `CorrelationData.getFuture()` **không bao giờ** complete ⇒ mỗi lần publish treo
   đúng 5 s (confirm timeout) rồi ném lỗi. Triệu chứng: outbox row `PENDING|1` mãi, log
   `không nhận được publisher confirm … sau 5000 ms`. Fix: `spring.rabbitmq.publisher-confirm-type: correlated`
   (+ `publisher-returns: true`) trong `order-service`. Đây là cấu hình **bắt buộc** của ADR-0007 D4 mà tôi đã
   quên — chính outbox giúp bug này **không mất event**, chỉ làm chậm 21 nhịp.
2. **`JdbcOutboxRepository` đòi bean `ObjectMapper`** ⇒ `order-service` IT đỏ **28/28** (`No qualifying bean of
   type ObjectMapper`). Fix: tự tạo mapper trong repository (như `EventEnvelopeCodec`) — dịch vụ này không phải
   tầng serialization web nên đừng phụ thuộc bean đó.

**Learning note**

- **`eventId` phải ổn định qua các lần thử** — chi tiết quyết định cả pattern: nếu relay sinh `eventId` mới mỗi lần
  publish lại thì `processed_events` của consumer **không chặn được** gì (mỗi lần là event "mới") ⇒ gửi email 2 lần.
  Dùng `outbox.id` làm `eventId` ⇒ mọi lần retry mang cùng id, dedupe có tác dụng đúng như ADR-0007 D3.
- **Claim ≠ publish trong cùng transaction**: claim là **transaction ngắn** (UPDATE … `SKIP LOCKED`) rồi mới publish
  ngoài transaction. Nếu publish trong transaction thì broker chậm (hoặc confirm 5 s như bug ở trên) sẽ giữ row lock
  của Postgres — biến sự cố broker thành sự cố DB. Thêm `claimed_at` (visibility timeout) để 2 relay không gửi trùng
  **chắc chắn**, không dựa vào việc 2 vòng quét lệch nhau.
- **Outbox biến "mất event" thành "trễ event"**: bug confirm ở trên làm mọi lần publish fail trong ~2 phút, nhưng
  không message nào mất và không đơn nào bị ảnh hưởng. Đây đúng là thứ outbox mua được — và cũng là lý do **không**
  được coi `attempts` cao là "chuyện nhỏ": nó là tín hiệu cấu hình sai.
- **`@Transactional` phải đi qua proxy**: tách `CheckoutOutbox` thành bean riêng vì gọi method `@Transactional` từ
  method khác **cùng class** thì không có transaction nào được mở — outbox sẽ ghi ngoài transaction và mất toàn bộ
  ý nghĩa (bug loại này không lộ ra ở happy path).
- **Thanh toán/HTTP nằm ngoài transaction**: transaction chỉ chứa 3 câu SQL local (order + saga + outbox). Giữ
  transaction qua lời gọi mạng là tự bắn vào chân — một dependency chậm sẽ khoá row hàng giây.


### SL-3.4 — `notification-service` (8089): consumer `order.confirmed` + retry + DLQ

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` (consumer base) · `SL-3.3` (event thật để consume); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

Đây là **vertical slice đầu tiên** của phase 3: một event đi từ outbox của `order` tới một service hoàn toàn mới. Nó là
chỗ để chứng minh "async thật sự chạy" — và cũng là chỗ để chứng minh **retry + DLQ** hoạt động, thứ mà nếu không test
thì chỉ là cấu hình.

**Mục tiêu**

`order.confirmed` ⇒ một email (mock) được "gửi" **async**, idempotent, lỗi thì retry rồi vào DLQ — **không** chặn queue.

**In scope**

- Service mới: `notification-service` (port **8089**), DB **`notification`** riêng, Flyway (`templates`, `notification_logs`,
  `processed_events`), Dockerfile + compose + CI matrix + gateway route `/v1/notifications`, OpenAPI tự sinh.
- Consumer `q.notification` bind `order.confirmed.v1` + `order.cancelled.v1` → render template → ghi `notification_logs`
  (`SENT`, `sent_at`) → log "đã gửi" (mock provider, không SMTP).
- Idempotent theo `eventId`; handler lỗi cố ý qua env `SHOPLAB_NOTIFICATION_FAIL_MODE` (chỉ local) để test retry/DLQ.
- `tools/dlq.sh`: liệt kê message trong `<queue>.dlq` và **move** 1 message về queue chính (replay có kiểm soát).
- `GET /v1/notifications/me` (buyer) — xem log của chính mình (verify được bằng mắt, không cần psql).

**Out of scope**

- Không SMTP/SMS thật, không template engine (placeholder `{{name}}` là đủ), không notification cho event khác.
- Không retry vô hạn, không dead-letter **process** riêng (replay bằng tay qua `tools/dlq.sh`).

**Acceptance Criteria**

- [x] Confirm đơn ⇒ trong ≤ **2 s** có row `notification_logs` `SENT` + `GET /v1/notifications/me` thấy nó; log có `eventId`.
- [x] Publish lại **cùng `eventId`** ⇒ vẫn **1** row (dedupe), không gửi lần 2.
- [x] Bật `SHOPLAB_NOTIFICATION_FAIL_MODE=true` ⇒ retry **3 lần** rồi message sang `q.notification.dlq`, queue chính
      `ready = 0`, `unacked = 0` (không treo).
- [x] `bash tools/dlq.sh move q.notification` (sau khi tắt fail-mode) ⇒ message được xử lý, `notification_logs` +1, DLQ rỗng.
- [x] Checkout vẫn xanh khi `notification-service` **chết** (async đúng nghĩa): confirm **200**, không 503.

**Ghi chú kỹ thuật**

- **Idempotent trước, retry sau**: nếu dedupe sai thì retry sẽ thành gửi email trùng — dedupe là điều kiện của retry.
- Env bơm lỗi **chỉ local** (`@ConditionalOnProperty(app.env=local)`) — bài học `SL-2.9`: hook debug không được tồn tại
  ở production.
- Port 8089 (8085 đã là Adminer).

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng dưới đây là `mvn verify` + e2e **chạy local**, dán nguyên output.

**1. Test tự động** — `mvn -B -ntp verify` (toàn repo, **12 module**):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 730, Failures: 0, Errors: 0   (600 unit + 130 IT)
  · riêng notification-service: 34 unit + 9 IT
  · NotificationServiceApplicationIT (5): Flyway tạo 3 bảng + seed 2 template · event_id UNIQUE chặn ghi trùng ·
    me không token → 401 · me chỉ trả log của chính mình (mới nhất trước) · ?limit bị chặn bởi trần cấu hình
  · NotificationConsumerIT (4): send + ack · publish lại cùng eventId → 1 row · fail-mode → nack(requeue=false) +
    xoá marker dedupe · fail-mode bị bỏ qua khi env != local
  · TemplateRendererTest (5) + NotificationServiceTest (7)
```

**2. Async end-to-end (stack thật)** — publish `order.confirmed.v1` (envelope thật) rồi poll DB:

```
publish: {"routed":true}
row xuất hiện sau 239 ms          <-- AC "≤ 2 s"
SENT|order.confirmed|Đơn hàng 4fe873b0-…-a52c6effed47 đã được xác nhận
```

`GET /v1/notifications/me` qua gateway (buyer token):

```
status=200
[{"id":"f8f27889-…","eventId":"50681af7-…","orderId":"b5a0d2ff-…","channel":"EMAIL",
  "template":"order.confirmed","recipient":"user-abb11f6d@shoplab.local",
  "subject":"Đơn hàng b5a0d2ff-… đã được xác nhận", …}]
```

**3. Dedupe** — publish **cùng `eventId`** lần 2: `rows = 1`, log consumer `đã xử lý trước đó`.

**4. Retry → DLQ** (`SHOPLAB_NOTIFICATION_FAIL_MODE=true`, restart service):

```
q.notification        ready=  0 unacked=  1 consumers=1     <-- đang retry
   … sau ~4 s (200ms + 1s + 3s) …
q.notification        ready=  0 unacked=  0 consumers=1     <-- KHÔNG treo
q.notification.dlq    ready=  1
docker logs … | grep -c 'lỗi xử lý|→ DLQ'  →  4             <-- 3 lần retry + 1 lần sang DLQ
```

**5. Replay có kiểm soát** — tắt fail-mode rồi `bash tools/dlq.sh move q.notification`:

```
  replay 1 message từ q.notification.dlq → shoplab.events (routing key GỐC = order.confirmed.v1) → {"routed":true}
rows cho eventId đó: 1        <-- message được xử lý lại, notification_logs +1
q.notification.dlq   ready=  0
```

**6. Decoupling** — `docker stop shoplab-notification-service` rồi confirm 1 đơn mới:

```
notification ĐANG TẮT → confirm: 200        <-- AC: không 503
bật lại → notification_logs có thêm: 1     <-- message chờ trong queue được xử lý khi consumer sống lại
```

**7. Regression** — e2e local (3 script):

```
== Kết quả: 17 PASS / 0 FAIL · 13 request · 1145 ms     (e2e-smoke)
== Kết quả: 27 PASS / 0 FAIL · 14 request · 3854 ms     (e2e-checkout)
== Kết quả: 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **Replay không phải "publish lại payload"** — bẫy này làm **mất message thật** ngay trong ticket này. Trong DLQ,
  `routing_key` của message là **tên queue** (`q.notification`) vì DLX đặt routing key = tên queue (ADR-0007 D2).
  Bản đầu của `tools/dlq.sh move` publish lại bằng `routing_key` hiện tại ⇒ `routed=false` ⇒ message bị **ack khỏi
  DLQ rồi rơi vào hư không**, không một lỗi nào. Routing key gốc nằm ở header `x-death[0].routing-keys`. Bài học
  kép: (1) đọc `x-death` khi replay; (2) **peek trước, consume sau** — validate xong mới `ack`, và script giờ exit
  **không consume** khi không parse được.
- **Quoting bash giết message**: bản `move` đầu tiên parse bằng `python3 -c "…nhiều dòng…"` trong `"$( … )"` — parse
  trả rỗng, mà message đã bị `get` mất rồi mới phát hiện. Chuyển sang **file python tạm + heredoc quoted** (không
  expansion) là hết hẳn lớp lỗi này. Với script vận hành, "parse lỗi" phải là **no-op**, không được là "mất dữ liệu".
- **`spring.rabbitmq.listener.simple.auto-startup` không áp cho factory tự khai**: service dùng
  `eventListenerContainerFactory` riêng (cần `AcknowledgeMode.MANUAL`) nên property của Boot **không** chạm tới ⇒ IT
  (không có broker) chết với `FatalListenerStartupException: Authentication failure`, **đỏ cả 17 test** dù logic
  đúng. Phải thêm cờ riêng của commons (`shoplab.events.consumer-auto-startup`). Tổng quát: **property của Boot chỉ
  có tác dụng với bean của Boot** — tự khai bean thì phải tự khai công tắc.
- **Dedupe là điều kiện của retry**, không phải tính năng phụ: retry gửi lại đúng message đó, nên marker
  `processed_events` **phải bị xoá khi handler fail** — xoá sai thứ tự thì retry thành "đã xử lý" (mất thông báo),
  không xoá thì retry vô nghĩa. `notification_logs.event_id UNIQUE` là chốt thứ hai ở tầng DB.
- **Async giải quyết "không chặn" nhưng đẻ ra "im lặng"**: khi service chết, confirm vẫn 200 (đúng), message **chờ
  trong queue** rồi được xử lý lúc consumer sống lại — nhưng nếu consumer chết **mãi** thì không ai biết thông báo
  đã không gửi. Đây là lý do `SL-3.8`/`SL-3.9` cần metric **queue depth + consumer down**, không chỉ log.
- 16 unit + 9 IT cho service mới, nhưng **0 dòng** retry/dedupe/DLQ trong code service — tất cả nằm ở
  `AbstractEventConsumer`. Đó là giá trị thật của `SL-3.2`: service thứ 2 trở đi chỉ viết ~60 dòng.

### SL-3.5 — `payment.succeeded` ⇒ `order-service` auto-confirm (trả `TD-17`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-09) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` · `SL-3.3` (khuôn outbox) · `SL-3.4` (khuôn consumer) · **`SL-3.11`** (token để gọi `commit` không cần user); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

`TD-17` (**mức `cao`**) — hệ quả của quyết định ở `SL-2.7`: webhook `payment.succeeded` **không** cập nhật order (Phase 2 chưa
có broker, chỉ saga giữ token user mới confirm được). Ca thật: khách trả tiền xong, webhook tới, nhưng **không** gọi
`/confirm` (đóng tab, mất mạng) ⇒ tiền đã thu mà đơn treo `PENDING_PAYMENT` đến khi sweeper `EXPIRED` ⇒ **hàng bị trả lại
dù tiền đã vào**. Phase 3 là lúc trả món nợ này đúng như điều kiện revisit đã ghi.

**Mục tiêu**

`payment-service` phát `payment.succeeded.v1` **từ outbox**; `order-service` consume và **tự confirm** đơn ⇒ hai đường
(webhook → event, và client gọi `/confirm`) đều dẫn tới **một** trạng thái, idempotent, không commit 2 lần.

**In scope**

- `payment-service`: migration outbox (`V?__create_outbox.sql`), ghi outbox **trong cùng transaction** khi webhook chuyển
  intent sang `SUCCEEDED`; relay riêng (cùng khuôn `SL-3.3`); payload `{paymentIntentId, orderId, amount, currency, succeededAt}`.
- `order-service`: consumer `q.order` bind `payment.succeeded.v1` → nếu saga `COMPLETED_START` + order `PENDING_PAYMENT` ⇒
  dùng lại `CheckoutFinishService` để commit stock + voucher ⇒ `CONFIRMED` (step audit `PAYMENT_EVENT_CONFIRM`).
- Ca không confirm được (order đã `EXPIRED`/`CANCELLED`): **ack + log WARN** có `paymentIntentId` (không ném lại vào DLQ
  vô ích) và ghi `TD` phần **refund** (chưa có `refunds`/`transactions`).
- Giữ `POST /v1/checkout/{id}/confirm` (client chủ động) — không đổi API surface.

**Out of scope**

- Không làm refund/hoàn tiền (Phase 6). Không bỏ endpoint `confirm`. Không đổi luồng saga start.

**Acceptance Criteria**

- [x] Mock-pay **không** gọi `/confirm` ⇒ ≤ **2 s** sau: order `CONFIRMED`, `saga = CONFIRMED`, `stock.on_hand` giảm đúng,
      voucher `COMMITTED`, `checkout_saga_steps` có `PAYMENT_EVENT_CONFIRM:OK`.
- [x] Gọi `/confirm` **sau** khi event đã confirm ⇒ **200** (idempotent), `stock`/voucher **không** bị trừ 2 lần.
- [x] Webhook lặp (cùng `provider_event_id`) ⇒ **không** sinh message `payment.succeeded.v2`, outbox vẫn 1 row.
- [x] `payment.succeeded` cho đơn đã `EXPIRED` ⇒ `ack`, log WARN, state **không** đổi, DLQ rỗng.
- [x] Broker chết lúc webhook ⇒ payment `SUCCEEDED` vẫn ghi DB + outbox `PENDING`; broker sống lại ⇒ event đi, order tự confirm.
- [x] `TD-17` → `✅ đã trả (SL-3.5)` (phần refund tách `TD` mới); `e2e-checkout`/`e2e-checkout-fail` không regress.

**Ghi chú kỹ thuật**

- **Hai đường confirm** là chấp nhận có chủ ý (client chủ động + hệ tự chốt) ⇒ **mọi** nhánh phải đi qua cùng một hàm
  compare-and-set trên `orders` (đã có từ `SL-2.6b`), nếu không sẽ commit 2 lần.
- Event này là **hệ quả của webhook**, không phải thay thế webhook: idempotency của webhook (`provider_event_id`) vẫn là
  chốt chặn thứ nhất; dedupe consumer (`eventId`) là chốt thứ hai.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng là `mvn verify` + e2e **local** + kịch bản chạy trên stack thật.

**1. Test tự động** — `mvn -B -ntp verify` (12 module):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 756, Failures: 0, Errors: 0   (624 unit + 132 IT)   <-- +10 so với baseline SL-3.11 (746)
  · order:   CheckoutFinishServiceEventTest (6 nhánh quyết định) · expire re-test (3: release bằng token ·
             release lỗi vẫn expire · đơn không còn PENDING_PAYMENT thì không release)
  · payment: PaymentWebhookServiceTest (+3: ghi outbox 1 lần · webhook lặp không ghi · FAILED không ghi)
```

**2. AC1 — mock-pay, KHÔNG gọi `/confirm`**

```
mock-pay → order CONFIRMED sau 1031 ms   (đơn thường)  ·  909 ms (đơn có voucher 10%)
saga = CONFIRMED
steps: STOCK_COMMIT:OK · VOUCHER_COMMIT:OK · PAYMENT_VERIFY:SKIPPED · PAYMENT_EVENT_CONFIRM:OK
stock: 5 → 3 available (mua 2)          voucher: used=1 locked=0 (COMMITTED)
outbox(payment): payment.succeeded.v1|SENT|attempts=1
```

**3. AC2 — `/confirm` sau khi event đã chốt**

```
confirm status = 200 · state = CONFIRMED   ·   stock vẫn 3 (KHÔNG trừ 2 lần)   ·   history 2 dòng (không thêm)
```

**4. AC3 — webhook lặp (cùng `provider_event_id`)**

```
lần 1: {"duplicate":false,"applied":false}      (intent đã SUCCEEDED ⇒ no-op, không ghi gì)
lần 2: {"duplicate":true,"applied":false}
outbox rows cho intent này = 1   ·   không có message `payment.succeeded.v2`
```

**5. AC4 — event cho đơn đã `EXPIRED`**

```
order status = EXPIRED (không đổi) · saga = EXPIRED · payment = SUCCEEDED · outbox = SENT
q.order.dlq ready = 0
log: "đơn 5ef18dca… ở saga EXPIRED — không chốt theo event … (cần xử lý tay)"
```

**6. AC5 — broker chết lúc webhook**

```
broker TẮT:  payment = SUCCEEDED · outbox = PENDING|attempts=3 · order = PENDING_PAYMENT
broker BẬT:  order CONFIRMED sau 7.8 s · outbox = SENT|attempts=11 · saga = CONFIRMED · stock 3 → 2
```

**7. `TD-25` — sweeper release ngay (không chờ TTL)**

```
checkout (payment-window 20 s): reserved = 2
order EXPIRED sau ~22 s  ⇒  HÀNG VỀ KHO sau ~0 s (reserved = 0)   <-- trước đây phải chờ TTL 15 phút
steps: STOCK_RELEASE:OK · EXPIRE:OK
```

**8. Regression e2e**

```
== 17 PASS / 0 FAIL · 13 request · 683 ms      (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 1059 ms     (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **"Hai đường confirm" không phải vấn đề idempotency, mà là vấn đề *thứ tự kiểm tra***. Chốt đơn theo event phải loại
  dần 5 khả năng trước khi chạm downstream: không có saga · saga đã `CONFIRMED` (bình thường, hai đường gặp nhau) · saga
  chưa `COMPLETED_START` · `paymentIntentId` lệch · đơn không còn `PENDING_PAYMENT`. Bỏ bất kỳ bước nào thì hệ **chốt
  nhầm đơn** — loại lỗi không sửa được bằng code (tiền đã thu, hàng đã giao). Vì thế test là **ma trận 6 nhánh**, không
  phải "một happy path + một lỗi".
- **Bằng chứng thanh toán đổi chỗ, không mất đi**: đường client gọi `/confirm` phải *hỏi* payment (`verifyPayment`);
  đường event **không hỏi lại** vì event được phát ra ngay sau CAS `SUCCEEDED` của chính payment. Nếu vẫn gọi lại thì
  vừa thừa (round-trip), vừa không làm được (không có token user) — và nếu cố tình dùng service token để gọi thì lại
  biến một luồng *đã có bằng chứng* thành phụ thuộc runtime vào payment. Ghi `PAYMENT_VERIFY:SKIPPED` vào audit để
  người đọc sau không tưởng là bước đó bị bỏ quên.
- **Payload event phải có "khoá đối chiếu"**: `paymentIntentId` trong payload không phải để trang trí — nó là thứ duy
  nhất phân biệt "event của đơn này" với "event bị route/replay sai". `orderId` một mình là chưa đủ.
- **Ack cho kết cục nghiệp vụ, retry cho lỗi hạ tầng**: đơn `EXPIRED` mà tiền đã thu là chuyện phải **hoàn tiền tay**
  (ack + log ERROR kèm `eventId`/`paymentIntentId`), còn inventory 503 là chuyện retry. Trộn hai loại này làm DLQ đầy
  message "không retry được" và che mất sự cố thật.
- **Sweeper release là chỗ dễ tạo bug bán quá tồn kho**: release *trước* khi CAS sang `EXPIRED` (đúng yêu cầu `TD-25`:
  hàng về ngay) mở một cửa sổ rất hẹp — nếu một confirm chen vào giữa thì hàng đã trả cho đơn vẫn được bán. Chặn bằng
  cách kiểm `orders.status = PENDING_PAYMENT` **trước** khi release, và log WARN nếu CAS sau đó trả `false` (nghĩa là
  release đã chạy nhưng đơn không expire được ⇒ phải kiểm tay). Cửa sổ không về 0, nhưng nó **nhìn thấy được**.
- **Bài học cũ lặp lại y nguyên**: `@ConfigurationProperties` inject một bean không tồn tại (`ObjectMapper` của Jackson 2)
  làm **context fail** — lần này ở IT của order-service. Cùng họ với `SL-3.3` (`JdbcOutboxRepository` tự tạo mapper).
  Với hạ tầng dùng chung trong `service-commons`, **không được giả định bean nào có mặt**: hoặc tự tạo, hoặc
  `@ConditionalOnBean` + fail mềm.
- **Copy khuôn outbox lần 2 là quyết định có ý thức, không phải lười**: 2 service cần đúng 500 dòng giống nhau. Nhưng
  lần thứ 3 (`SL-3.6` catalog) là lúc phải extract vào commons — đã ghi `TD-32` với điều kiện revisit rõ ràng, để "lần
  sau" không trôi thành "không bao giờ".

### SL-3.11 — Service-to-service auth: `client_credentials` + `aud`/`exp` (tách từ `SL-3.5`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-1.4` (JWT/JWKS của identity) · `SL-3.2` (commons); chặn `SL-3.5` · trả một phần `TD-25` |
| **Ước lượng** | M (tách ra từ `SL-3.5` vì `SL-3.5` + phần auth = `L` — `AGENTS.md` §7) |

**Bối cảnh**

`SL-3.5` cần `order-service` gọi `inventory`/`promotion` `commit` **từ consumer** — nhưng consumer không có token
người dùng (token 15 phút, khách đã đóng tab). Cùng vấn đề với `checkout` sweeper (`TD-25`: không có token ⇒ không
gọi được `release`, phải nhờ TTL của service khác). Hệ hiện **chỉ** có 1 loại caller: user cuối. Ba lựa chọn đã
được so sánh trong phiên plan: internal token dùng chung (A) · endpoint internal riêng (B) · **service token thật
qua identity, `client_credentials` + `aud`/`exp` (C)** → chốt **C** vì repo là learning lab, ưu tiên hiểu đúng
(`AGENTS.md` §1): token có hạn, có audience, thu hồi/rotate được, và log biết **service nào** đang gọi.

**Mục tiêu**

Service (không có user) xin được **token ngắn hạn** từ `identity-service`, mang `role=SERVICE` + `aud=<service
đích>`, và mỗi service đích **từ chối** token phát cho service khác — mà **không** đổi hành vi của token user hiện
tại.

**In scope**

- `identity-service`: `POST /v1/auth/service-token` (`clientId` + `clientSecret` + `audience`) → JWT RS256
  `role=SERVICE`, `aud=[audience]`, `exp=5m`, `sub=svc:<clientId>`; registry client theo cấu hình
  (`shoplab.service-auth.clients.<id>.secret` + `.allowed-audiences`), secret so sánh **hằng thời gian**.
- `service-commons`: `ServiceTokenClient` (cache token tới `exp - skew`, không xin lại mỗi request) +
  `ServiceTokenRules.validator(issuer, serviceName)` (kiểm `aud` **chỉ khi** `role=SERVICE`).
- `inventory-service` + `promotion-service`: dùng validator đó ⇒ token của `order-service` gọi `inventory` bị
  `promotion` từ chối và ngược lại.
- `ADR-0008` (quyết định + trade-off + điều kiện revisit) + `architecture.md` §bảo mật.
- Env/compose cho client secret (12-factor, không hard-code).

**Out of scope**

- Chưa nối `order-service` dùng token này (việc của `SL-3.5`), chưa cho sweeper gọi `release` (chỉ đổi khi
  `SL-3.5` xong — `TD-25` đóng ở đó).
- Không dùng Spring Authorization Server, không mTLS (Phase 7), không refresh token cho service, không UI quản lý
  client.

**Acceptance Criteria**

- [x] `POST /v1/auth/service-token` đúng client + audience ⇒ **200**, token decode được: `role=SERVICE`,
      `aud=[audience]`, `exp-iat = 5m`, `iss` = issuer của hệ.
- [x] Sai secret / client lạ ⇒ **401**; `audience` không nằm trong `allowed-audiences` của client ⇒ **400**; thiếu
      field ⇒ **400**.
- [x] Gọi `inventory-service` bằng token `aud=inventory-service` ⇒ **404** (auth qua, chỉ thiếu resource — chứng minh
      token được chấp nhận); bằng token `aud=promotion-service` ⇒ **401 `invalid_token`** (chuẩn OAuth2: token không hợp
      lệ cho resource này là *invalid token*, không phải 403); token user thường vẫn hoạt động như cũ (không hồi quy).
- [x] `ServiceTokenClient` cache: 2 lần gọi liên tiếp chỉ **1** request tới identity; token gần hết hạn ⇒ xin mới.
- [x] `mvn -B -ntp verify` xanh toàn repo + e2e local 3 script không hồi quy.

**Ghi chú kỹ thuật**

- `aud` chỉ kiểm **khi** `role=SERVICE` — token user hiện chưa có `aud`; thêm `aud` cho token user là thay đổi hợp
  đồng token với 8 service ⇒ tách ticket riêng khi cần.
- Registry client để **trong config** (không thêm bảng DB): Phase 3 chỉ có 1–2 client; bảng + API quản lý client là
  hạ tầng của Phase 6/7 (`AGENTS.md` §3.4).
- `sub=svc:<clientId>` (không phải UUID) ⇒ service nào lỡ coi `sub` là userId sẽ **nổ ngay** chứ không âm thầm ghi
  dữ liệu sai.
- **Sai `aud` trả 401, không phải 403** (đo thật trên stack): Spring resource server map lỗi validator thành
  `invalid_token` ⇒ 401. Đúng chuẩn OAuth2 (token không hợp lệ cho resource này), và 403 để dành cho "đã xác thực nhưng
  thiếu quyền". Ghi lại vì AC ban đầu mình đoán 403 — đoán sai thì sửa AC, không uốn code cho khớp giấy.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng là `mvn verify` + e2e **chạy local** + curl trên stack thật.

**1. Test tự động** — `mvn -B -ntp verify` (12 module):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 746, Failures: 0, Errors: 0   (614 unit + 132 IT)   <-- +16 so với baseline SL-3.4 (730)
  · commons: ServiceTokenClientTest (5) + ServiceTokenRulesTest (5)
  · identity: ServiceAuthServiceTest (4) + IT +2 (claims của service token · reject sai secret/audience/thiếu field)
```

**2. Cấp token (stack thật)**

```
POST /v1/auth/service-token {order-service, <secret>, inventory-service}
tokenType= Bearer expiresIn= 300 audience= inventory-service
claims: role= SERVICE aud= inventory-service sub= svc:order-service iss= https://identity.shoplab.local
```

**3. Sai secret / audience ngoài allowlist**

```
sai secret            → status=401  {"code":"UNAUTHORIZED","message":"clientId hoặc clientSecret không đúng"}
aud=payment-service   → status=400  {"code":"VALIDATION_ERROR","message":"client order-service không được phép xin
                                    token cho audience 'payment-service' (cho phép: [inventory-service, promotion-service])"}
```

**4. Service đích kiểm `aud`**

```
inventory  + token aud=inventory-service   → 404   (qua auth, chỉ thiếu resource — token ĐƯỢC chấp nhận)
inventory  + token aud=promotion-service   → 401   {"code":"UNAUTHORIZED"}  (WWW-Authenticate: invalid_token)
promotion  + token aud=promotion-service   → 403   (qua auth, thiếu role ADMIN — token hợp lệ)
promotion  + token aud=inventory-service   → 401   (sai audience)
```

**5. Không hồi quy token người**

```
admin (token user) + inventory  → 404   ·   admin (token user) + promotion → 404      <-- vẫn qua auth như trước
```

**6. Regression e2e**

```
== 17 PASS / 0 FAIL · 13 request · 657 ms      (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 1343 ms     (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **"Biết ai đang gọi" mới là phần khó của service-to-service auth**, không phải "làm sao để gọi được". Bản A (shared
  secret) mất 30 phút là xong nhưng hệ quả là *mọi* service dùng chung một danh tính: log không phân biệt được ai gọi,
  và một chỗ rò rỉ = toàn bộ endpoint chỉ-cần-`authenticated()` bị mở. `aud` + `sub=svc:<name>` biến sự kiện "có
  request nội bộ" thành "**order-service** gọi **inventory** trong 5 phút tới" — đó mới là thứ debug được lúc 3h sáng.
- **`@ConfigurationProperties` record lồng record = sai đường dẫn bind, và triệu chứng rất dối**: mình bọc
  `record Clients(Map<String, Client> clients)` trong `ServiceAuthProperties` ⇒ path thật thành
  `clients.clients.<id>` ⇒ map **rỗng** ⇒ mọi request trả 401 "clientId hoặc clientSecret không đúng". Nhìn log thì
  tưởng sai secret, thực ra là bind sai. Map phải là **field trực tiếp** của record (hoặc dùng `@Name`). Bài học kèm:
  với map key có dấu `-`, đặt bằng property phẳng phải dùng `clients[order-service].secret`.
- **Kiểm `exp`/`aud` trong test phải dùng đồng hồ thật**: `JwtValidators.createDefaultWithIssuer` so `exp` với
  *now*, nên token dựng bằng mốc cố định (`Instant.parse("2026-11-08T14:00:00Z")`) **hết hạn ngay khi tạo** ⇒ 3 test đỏ
  mà thông báo chỉ nói "Jwt expired" — đọc dễ tưởng lỗi logic. Mốc thời gian trong test JWT phải là `Instant.now()`.
- **Chuẩn thắng kỳ vọng của mình**: mình viết AC là "sai audience ⇒ 403", đo thật thì Spring trả **401
  `invalid_token`** — và nó **đúng** (token không hợp lệ cho resource này; 403 dành cho "đã xác thực nhưng thiếu
  quyền"). Sửa AC theo thực tế, không uốn code cho khớp giấy. Đo được cả hai ca trên cùng endpoint `promotion`
  (`hasRole('ADMIN')`): token đúng `aud` ⇒ 403, token sai `aud` ⇒ 401.
- **Token không bao giờ được gửi kèm message**: nhét bearer vào payload event là biến broker thành kho token và biến
  mọi consumer thành "có quyền của user". Event chỉ mang `paymentIntentId`/`orderId`; consumer tự xin token của
  **chính nó**.
- **Fail-closed ở config rẻ hơn fail-closed ở code**: `secret: ${ORDER_SERVICE_CLIENT_SECRET}` **không có default** ⇒
  thiếu env là identity không khởi động. Đổi lại phải thêm biến vào `.env.example` + compose; nếu để default
  `dev-secret` thì mọi máy dev chạy được nhưng production có thể vô tình dùng secret ai cũng biết.

### SL-3.6 — `catalog` outbox + `search-service` (8090) Postgres FTS + reconcile

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 3 — Async & event-driven *(quyết định storage: Postgres FTS — xem ghi chú ở đầu phase)* |
| **Phụ thuộc** | `SL-3.2` (consumer/publisher) · `SL-3.3` (khuôn outbox); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

`catalog` hiện là nguồn sự thật về product, nhưng tìm kiếm (`LIKE '%q%'`, `TD-7`) là full scan — không lên được khi dữ
liệu lớn. Bài học của phase 3 **không** phải "cài OpenSearch" mà là **index lệch thì phải phát hiện và sửa được**: event
cập nhật index + **reconcile job** khi lệch. Engine đổi ở Phase 8 mà contract event giữ nguyên.

**Mục tiêu**

Product đổi ⇒ index của `search-service` cập nhật ≤ 2 s; index lệch ⇒ **một lệnh** dựng lại được; tìm tiếng Việt có dấu /
không dấu đều ra.

**In scope**

- `catalog-service`: outbox + relay (cùng khuôn `SL-3.3`) phát `product.updated.v1` (created/updated gộp) và
  `product.archived.v1` khi soft delete (`SL-2.11`).
- `search-service` (port **8090**), DB **`search`**: bảng `search_product(product_id PK, name, description, category_id,
  min_price, status, updated_at, search_vector tsvector)` + GIN index + extension `unaccent`; consumer `q.search` upsert/archive.
- `GET /v1/search?q=&categoryId=&minPrice=&maxPrice=&page=&size=` — chỉ trả product `ACTIVE`, body `ApiError` chuẩn, phân
  trang offset (ghi `TD` về keyset nếu cần, đồng bộ `TD-6`).
- **Reconcile**: `POST /v1/admin/search/reindex` (ADMIN) đọc catalog theo batch (`GET /v1/products?size=…&page=…`) và upsert;
  trả `{scanned, upserted, archived}`. Kèm `@Scheduled` so số row (log WARN khi lệch > 1%).
- Dockerfile + compose + CI matrix + gateway route `/v1/search` (public) + `/v1/admin/search/**` (ADMIN).

**Out of scope**

- **OpenSearch/Elasticsearch (Phase 8)** · relevance tuning, synonym, facet, highlight · search SKU (chỉ product).
- Không sửa `catalog` để phục vụ search (vẫn là nguồn sự thật, không biết gì về index).

**Acceptance Criteria**

- [ ] Tạo/sửa product ⇒ ≤ **2 s** `GET /v1/search?q=<tên>` thấy product với `minPrice` đúng.
- [ ] Soft delete product ⇒ ≤ 2 s search **không** còn; row index chuyển `ARCHIVED` (không xoá cứng).
- [ ] `q=dien thoai` khớp `"Điện thoại"` (unaccent) và `q=dien%` không làm lỗi (escape wildcard).
- [ ] Xoá tay 5 row index rồi `POST /v1/admin/search/reindex` ⇒ `upserted ≥ 5`, search trả lại đủ; gọi 2 lần liên tiếp
      ⇒ **idempotent** (không nhân bản row).
- [ ] Broker chết ⇒ catalog **vẫn** tạo/sửa product bình thường (chỉ index trễ); reconcile vẫn dựng lại được index.
- [ ] `product.archived` phát từ **outbox** (không phải gọi thẳng search) — kiểm bằng `outbox` row + message.

**Ghi chú kỹ thuật**

- **Đổi engine không đổi contract** là mục tiêu của phase này: payload event + reconcile là thứ giữ cho việc "lên
  OpenSearch" ở Phase 8 thành việc **thay storage**, không phải viết lại tích hợp.
- `unaccent` + `tsvector` cho tiếng Việt: dùng `unaccent(simple)` thay vì `vietnamese` config (Postgres không có sẵn) và
  ghi rõ giới hạn (không xử lý từ đồng nghĩa) — trung thực về chất lượng search.
- Reconcile là **đường lùi bắt buộc** của mọi index: index sai mà không reconcile được thì không được coi là "event-driven xong".

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-3.7 — E2E async (`tools/e2e-async.sh`) + bước trong `e2e.yml`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.3`·`SL-3.4`·`SL-3.5`·`SL-3.6` (cần đủ 4 luồng để assert); chặn `SL-3.9` |
| **Ước lượng** | S |

**Bối cảnh**

Async không nhìn thấy được bằng một `curl` duy nhất: API trả `200` **trước khi** việc thật xảy ra. Test phải **chờ có điều
kiện** (poll tới timeout) và assert **state cuối** — nếu không sẽ có test "xanh" vì... chưa kịp chạy.

**Mục tiêu**

Một lệnh chạy được **4 luồng async** trên stack thật: event → email · event → auto-confirm · event → index search ·
message lỗi → DLQ → replay.

**In scope**

- `tools/e2e-async.sh` (bash + `python3`, cùng khuôn `e2e-checkout.sh`): các bước `A1…A12`:
  confirm ⇒ `notification_logs`; mock-pay **không** confirm ⇒ order tự `CONFIRMED`; sửa product ⇒ search thấy; archive ⇒
  search mất; bật fail-mode ⇒ DLQ có message; `tools/dlq.sh move` ⇒ xử lý lại; queue `ready = 0` sau khi sạch.
- Mỗi bước `PASS/FAIL` + gợi ý lệnh kiểm tra; exit code `1` khi có FAIL; **cleanup trap** reset fail-mode (bài học `SL-2.9`).
- `.github/workflows/e2e.yml`: thêm bước chạy script (sau 2 script hiện có); README mục "E2E async".

**Out of scope**

- Không thay thế 3 script cũ (smoke/checkout/fail vẫn là chuẩn riêng). Không đo hiệu năng (đó là `SL-3.8`).

**Acceptance Criteria**

- [ ] `bash tools/e2e-async.sh` ⇒ **≥ 12 PASS / 0 FAIL**, chạy **2 lần liên tiếp** đều xanh (không phụ thuộc state cũ).
- [ ] Mọi assert async đều **poll có timeout** (≤ 5 s) chứ không `sleep` mù; FAIL in `ready`/`unacked`/`dlq` tại thời điểm fail.
- [ ] Script tự dọn state (fail-mode off, DLQ rỗng) kể cả khi chết giữa đường (`trap`).
- [ ] `e2e.yml` có bước mới; YAML parse được (`python3 yaml.safe_load`).
- [ ] 3 script cũ vẫn xanh (không regress).

**Ghi chú kỹ thuật**

- **Poll, không sleep**: async + máy chậm ⇒ `sleep 1` là flaky; poll state với timeout là cách duy nhất ổn định.
- DLQ test **phải** dùng fail-mode có guard (local/E2E only) — không có đường nào để bơm lỗi ở production.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-3.8 — Báo cáo before/after async (số liệu đo thật)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.3`…`SL-3.7` (phải có luồng async chạy được mới đo) |
| **Ước lượng** | S |

**Bối cảnh**

"Chuyển sang async cho nhanh hơn" là câu nói không kiểm chứng được. Ở hệ này, **email vốn không nằm trên đường checkout**
⇒ async không làm checkout nhanh hơn; cái async mang lại là **decoupling** (notification chết không làm checkout fail) và
**độ trễ có kiểm soát**. Báo cáo phải nói đúng như vậy, bằng số đo — không bịa "before/after" của một thứ chưa từng chạy sync.

**Mục tiêu**

`docs/reports/async-phase-3.md` trả lời bằng số: async **được gì**, **mất gì**, **giới hạn ở đâu**.

**In scope**

- Đo `latency checkout` (p50/p95, n = 50 request tuần tự + 20 đồng thời) **trước/sau** khi bật notification (để chứng minh
  không đổi, hoặc đổi bao nhiêu).
- Đo **độ trễ event**: từ `confirm` → `notification_logs.sent_at` (p50/p95) và từ webhook → order `CONFIRMED`.
- Đo **throughput relay**: số message/s gửi được khi bơm 500 sự kiện (báo rõ bottleneck: relay batch hay broker).
- Đo **DLQ = 0** trong 5 phút chạy 100 checkout + 100 cập nhật product (không mất message, không message trùng).
- Đo ca **degradation**: `docker stop notification-service` ⇒ checkout p95 **không** đổi (bằng chứng của decoupling) và
  message tồn trong queue rồi được xử lý khi bật lại.

**Out of scope**

- Không load test ở mức flash sale (Phase 4 — k6). Không tối ưu throughput (Phase 4).
- Không so sánh broker (Kafka ở Phase 8).

**Acceptance Criteria**

- [ ] Báo cáo có **bảng số liệu** với: chỉ số · before · after · cách đo · điều kiện (máy, số request, commit).
- [ ] Có **kết luận trung thực**: cái gì async **không** cải thiện (latency checkout) và vì sao vẫn nên làm (decoupling).
- [ ] Có phần **giới hạn**: `unacked` khi retry giữ thread, relay polling trễ ≤ interval, `processed_events` phình.
- [ ] Số liệu trong báo cáo **khớp** với output đã dán trong ticket (không chép tay từ trí nhớ).

**Ghi chú kỹ thuật**

- Đo bằng `curl -w '%{time_total}'` (không cần k6 ở phase này) + `psql` cho độ trễ event; ghi rõ **n** và cách tính p95.
- Nếu số liệu xấu (relay trễ, DLQ > 0) thì **ghi nguyên** — mục đích là biết hệ thật sự ở đâu, không phải báo cáo đẹp.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-3.9 — `docs/runbook-phase-3.md` + DoD Phase 3 (archive · tag `phase-3` · release)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | **mọi** ticket `SL-3.x` trước đó `Done` |
| **Ước lượng** | S |

**Bối cảnh**

Debug async khác debug sync ở một điểm cốt tử: **lỗi không nằm trong response HTTP**. Người trực phải biết nhìn
`ready`/`unacked`/DLQ/outbox `PENDING`, biết message nào đã đi và message nào còn kẹt. Không có runbook thì mỗi lần
"email không tới" là một cuộc điều tra từ đầu.

**Mục tiêu**

1. `docs/runbook-phase-3.md` đủ để người khác debug luồng async **mà không cần đọc code**.
2. Đóng Phase 3 theo DoD (`AGENTS.md` §5, ADR-0005): archive · snapshot doc · tag `phase-3` · release.

**In scope**

- Runbook: sơ đồ event (producer → exchange → queue → consumer → DB) · bảng tra nhanh (queue/DLQ/outbox/log theo service) ·
  lệnh kiểm tra từng tầng (`tools/rabbit-topology.sh`, `psql` outbox + `processed_events` + `notification_logs`, search index) ·
  bảng **≥ 6 lỗi** (email không tới · DLQ tăng · order không auto-confirm · index lệch · relay không chạy · message trùng) ·
  **§5 một ví dụ debug thật** dán nguyên văn.
- Archive `SL-3.x` → `Backlog-phase-3.md`; `docs/phases/phase-3.md`; cập nhật `docs/phases/README.md` + bảng archive +
  `architecture.md` §12; tag annotated `phase-3` + GitHub Release (sau merge).

**Out of scope**

- Không sửa code service; không mở `maint/phase-3`; không làm alert/dashboard (Phase 6).

**Acceptance Criteria**

- [ ] Runbook có đủ mục như trên; **mỗi lệnh chạy được thật** trên stack compose.
- [ ] §5 dán output debug thật của **một** ca async fail (DLQ hoặc relay kẹt), không viết tay.
- [ ] `Backlog-phase-3.md` đủ ticket + header; `Backlog.md` không còn `### SL-3.x`; `docs/phases/phase-3.md` có số liệu của `SL-3.8`.
- [ ] `docker compose down -v` → `up -d --build` ⇒ 14 container healthy (thêm notification + search), 4 script E2E xanh.
- [ ] Tag `phase-3` + Release; `architecture.md` §12 đánh dấu Phase 3 ✅ Done.

**Ghi chú kỹ thuật**

- Bảng lỗi ưu tiên **triệu chứng người trực thấy** ("email không tới", "DLQ tăng") thay vì tên exception.
- Số liệu snapshot lấy từ báo cáo `SL-3.8` — **một** nguồn, không chép lại nhiều nơi.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-3.10 — Process: tắt CI tạm thời + `merge-pr.sh --skip-ci-check`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven *(phát sinh — quyết định của human 2026-11-08)* |
| **Phụ thuộc** | — (áp dụng từ `SL-3.2` xong trở đi); **chặn** không ticket nào, nhưng đổi cách mọi ticket sau được merge |
| **Ước lượng** | S |

**Bối cảnh**

Đo thật: mỗi PR tốn **~5–6 phút** CI (`test` ~5 min là phần lớn) + thêm ~5 phút run trên `main` ⇒ ~10 phút/ticket chỉ để
chờ. Trong khi đó `mvn verify` local chạy **đúng nội dung job `test`** (unit + IT Testcontainers + JaCoCo gate). Phase 3
còn 7 ticket ⇒ ~70 phút chờ. Quyết định: **tắt CI tạm thời** để đẩy tốc độ, chấp nhận mất hàng rào tự động — nhưng phải
tắt **có ghi chép và có đường bật lại**, không để rule "CI xanh mới merge" mục âm thầm.

**Mục tiêu**

1. CI tắt bằng **kill-switch có sẵn** (`vars.CI_ENABLED=false`) — không sửa file workflow, bật lại bằng 1 lệnh.
2. `tools/merge-pr.sh` có cờ `--skip-ci-check` để merge được khi không có run CI, **kèm cảnh báo rõ** trong output và
   trong comment evidence hậu merge (người đọc PR sau vẫn biết PR này **không** qua CI).
3. Rule + nợ kỹ thuật được ghi lại: `AGENTS.md` §9, `.clinerules/10-git-workflow.md`, `Backlog.md` §7 (`TD`).

**In scope**

- `tools/merge-pr.sh`: thêm `--skip-ci-check` (bỏ qua bước đòi CI xanh; vẫn **bắt buộc** PR mở/không draft + có comment
  tóm tắt; in `CẢNH BÁO` + ghi vào evidence `CI: bỏ qua (--skip-ci-check)`).
- `gh variable set CI_ENABLED --body false` (và hướng dẫn bật lại).
- `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + `README.md` (mục CI): ghi trạng thái đang tắt + điều kiện bật lại.
- `Backlog.md` §7: `TD-28` — "CI đang tắt" với **điều kiện revisit verify được**.

**Out of scope**

- Không sửa `.github/workflows/ci.yml` (kill-switch đã có từ `SL-1.0`; sửa workflow lúc này là thêm rủi ro vô ích).
- Không bỏ hẳn rule "CI xanh mới merge" — đây là **ngoại lệ tạm thời**, hết Phase 3 phải bật lại.
- Không đổi `e2e.yml` (vốn chỉ chạy `schedule`/`dispatch`).

**Acceptance Criteria**

- [x] `gh variable list` cho thấy `CI_ENABLED=false`.
- [x] Push 1 nhánh bất kỳ ⇒ **0 run** CI mới (kiểm bằng `gh run list --branch <branch>`).
- [x] `bash tools/merge-pr.sh <pr> --summary-file f --skip-ci-check` merge được PR **không có run**, in cảnh báo, và
      comment evidence có dòng `CI: BỎ QUA`.
- [x] Không có `--skip-ci-check` ⇒ script **vẫn chặn** PR code không có run CI (hành vi cũ giữ nguyên).
- [x] `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + README + `TD-28` đã ghi, kèm câu lệnh bật lại.
- [x] Mỗi ticket còn lại của Phase 3 vẫn **dán output `mvn verify` + e2e local** vào ticket (thay cho CI).
      *(ràng buộc quy trình — áp dụng từ `SL-3.3`; đã ghi vào header "Ticket tiếp theo" + `AGENTS.md` §9)*

**Ghi chú kỹ thuật**

- Kill-switch nằm ở `if:` của cả 3 job (`vars.CI_ENABLED != 'false'`) ⇒ job bị **skip**, không đốt phút. Đổi lại: PR không
  có check nào ⇒ GitHub UI không có dấu hiệu gì; vì vậy **comment evidence** là chỗ duy nhất còn dấu vết.
- `--skip-ci-check` **không** được là mặc định: mặc định vẫn chặn, phải gõ cờ mới bỏ qua — tránh việc "quên bật lại CI"
  mà vẫn merge ầm ầm.
- Rủi ro đã chấp nhận: `build-image` matrix (9 image) là chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module ⇒
  trong giai đoạn này, service nào đổi thì **tự build image local** (đã làm ở `SL-3.1`/`SL-3.2`).

**Kết quả verify** *(2026-11-08 — đo thật trên repo này)*

**1) Kill-switch đã tắt đúng — job `skipping`, 0 phút:**

```console
$ gh variable list
CI_ENABLED  false  less than a minute ago

$ git push -u origin chore/sl-3.10-ci-off-burst        # push 1 nhánh mới
$ gh run list --branch chore/sl-3.10-ci-off-burst
(empty)                                                ← không có run mới

$ gh pr checks 55
build-image (${{ matrix.service }})   skipping   0
lint (checkstyle + spotless)          skipping   0
test (unit + jacoco gate)             skipping   0
```

**2) `merge-pr.sh` giữ đúng hàng rào khi KHÔNG truyền cờ:**

```console
$ bash tools/merge-pr.sh 55 --summary-file /tmp/pr-310.md
merge-pr: run CI mới nhất của 'chore/sl-3.10-ci-off-burst' là 'skipped' (cần 'success'). Không merge.
```

**3) Có cờ ⇒ cảnh báo rõ + đi tiếp (dry-run):**

```console
$ bash tools/merge-pr.sh 55 --summary-file /tmp/pr-310.md --skip-ci-check --dry-run
merge-pr: ⚠️  CẢNH BÁO — BỎ QUA kiểm tra CI (--skip-ci-check).
merge-pr:    Chỉ dùng khi CI đang tắt (gh variable get CI_ENABLED = false).
merge-pr:    Trách nhiệm verify chuyển về ticket: phải có output 'mvn verify' + e2e local.
merge-pr: PR #55 (chore/sl-3.10-ci-off-burst) — ⚠️ CI BỎ QUA (--skip-ci-check) · HEAD 92ff2b9c…
merge-pr: --dry-run → không comment, không merge.
```

**4) Rule + nợ đã ghi:** `AGENTS.md` §9 (ngoại lệ + lệnh bật lại) · `.clinerules/10-git-workflow.md` (điều kiện merge
thay thế) · `README.md` mục CI · `Backlog.md` §7 **`TD-28`** (mức `cao`, điều kiện revisit = **bắt buộc** bật lại khi
đóng Phase 3).

**Learning note**

- **Tắt CI không phải "không ảnh hưởng"**: mất 3 hàng rào (test độc lập + JaCoCo gate, lint môi trường sạch,
  `build-image` matrix — chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module). Đổi lại được ~5–6 phút/PR.
  Cách đúng: tắt bằng **kill-switch có sẵn** (`vars.CI_ENABLED`) + **ghi nợ có điều kiện bật lại**, không sửa workflow.
- **Một cờ "bỏ qua hàng rào" phải mặc định TẮT và phải nói to**: `--skip-ci-check` chỉ chạy khi gõ tường minh; script in
  cảnh báo + ghi `CI: BỎ QUA` vào comment evidence hậu merge ⇒ người đọc PR 3 tháng sau vẫn biết PR đó **không** qua CI.
  Nếu ghi ở nơi dễ mất (chat, trí nhớ) thì "tạm thời" sẽ thành vĩnh viễn mà không ai biết.
- **Kill-switch để lại một khoảng trống quan sát**: PR không có check nào ⇒ GitHub UI **không** hiện dấu hiệu gì. Bù lại bằng
  kỷ luật *dán output local vào ticket*; đây là chỗ dễ trượt nhất khi vội — nên ghi thẳng vào AC của mọi ticket còn lại.
- **Verify tắt-xong-phải-thử-cả-hai-nhánh**: kiểm "có cờ thì chạy" thôi là chưa đủ; phải kiểm **"không cờ vẫn chặn"** —
  nếu không, một lần refactor sau có thể làm bỏ qua kiểm CI trở thành mặc định.


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
| 2026-11-08 | SL-2.6b | **Tách finish khỏi start**: start *bù* khi fail, finish *không* bù (commit xong thì "un-commit" vô nghĩa) — một class làm cả hai sẽ phải rẽ nhánh theo pha. **Không cần state "đang xử lý"** khi service đích idempotent: chốt chặn thật là compare-and-set trên `orders`. **Sweeper không có token ⇒ đừng gọi API cần token** — để TTL sweeper của inventory/promotion trả hàng (đo được `reserved 2 → 0` sau 45 s). Và `payment-window` **phải bằng** TTL reservation/lock, nếu không có cửa sổ "đơn EXPIRED mà tiền vẫn vào". |
| 2026-11-08 | SL-2.8 | **E2E phải assert giá trị, không chỉ status**: 3 check tiền đỏ dù API đúng (qua `status 201` thì đã "xanh"); và mọi ngôn ngữ trung gian "chuẩn hoá" số — Python `json.load` biến `220000.00` thành `220000.0` ⇒ dùng `parse_float=str` (cùng bẫy với `String.valueOf` ở `SL-2.13`). **Chi phí CI là quyết định thiết kế**: 9 image × `mvn package` ≈ 30 phút/run ⇒ E2E chạy `schedule` + `workflow_dispatch`, không chạy mỗi PR; workflow không chạy trên PR **không** chặn merge — `ci.yml` mới là hàng rào. |
| 2026-11-08 | SL-2.12 | **Refactor \"chỉ move file\" vẫn đổi hành vi, vì thứ tự đăng ký bean là một phần hành vi**: cùng một advice, khi là `@Component` trong service thì nó đứng **trước** `GlobalExceptionHandler` (catch-all `Exception`) và 403 đúng; chuyển thành `@Bean` ở auto-config thì thành **sau** ⇒ `AuthorizationDeniedException` bị catch-all bắt ⇒ **500**. `ExceptionHandlerExceptionResolver` chọn advice **đầu tiên khớp**, *không* phải \"handler gần nhất\" ⇒ advice cụ thể **phải** có `@Order(HIGHEST_PRECEDENCE)` + test canary, đừng tin thứ tự bean. Guard `@ConditionalOnClass` cho dependency optional **phải ở cấp class của auto-config riêng** (đặt trên `@Bean` method ⇒ Spring vẫn resolve return type ⇒ `NoClassDefFoundError`), và `ApplicationContextRunner` + `FilteredClassLoader` chứng minh được nhánh đó trong 0,25 s. Cuối cùng: **số liệu trong ticket cũ sẽ mục** — ticket ghi \"2 bản copy\", thực tế **6**; đếm lại bằng `grep -rl` trước khi làm. |
| 2026-11-08 | SL-2.10 | **Đóng phase là việc *đọc lại toàn bộ* tài liệu của phase, không phải chỉ `git tag`**: rà để archive mới lộ ra 3 chỗ ghi số liệu lệch (`docs/phases/README.md` "🟡 Breakdown xong (11 ticket)", bảng trạng thái "8 / 13" → "14 / 15", header backlog "Phase 2: 10") và **một ticket Done mà không có đặc tả** (`SL-2.6b`) — phải viết bổ sung từ commit message, ghi rõ nguồn. Verify "snapshot chạy được" phải là `down -v` + `up --build` chứ không phải `up -d` trên volume cũ (volume cũ che lỗi Flyway/seed — đúng lỗi người clone mới gặp đầu tiên). Runbook chỉ đáng tin khi chính mình dùng nó để debug: §5 lộ ra **health check của Docker trễ 1 nhịp** sau `docker start` (báo `healthy` cũ) ⇒ phải chờ `/ready`. Và: tag phải gắn **sau merge** (trên merge commit) thì `git checkout phase-2` mới là cây code đã qua CI. |



---

| 2026-11-08 | SL-3.0 | **ADR là chỗ *loại* phương án, không phải chỗ khoe phương án đã chọn**: 4/5 hạng mục có phương án ghi rõ “❌ Không dùng” kèm lý do (publish-trong-transaction: broker **không** tham gia transaction DB ⇒ hoặc mất event, hoặc bịa event). Mỗi quyết định phải kéo theo một **điều kiện bỏ định lượng được** (p95 > 5 s · > 30% CPU · > 500 msg/s · > 20 event · > 50 msg DLQ/tuần) — không có số thì “khi cần” = không bao giờ. Chốt contract trước khi code là rẻ nhất: ~185 dòng ADR khoá 6 ticket phía sau; đổi routing key hay bỏ outbox sau khi có code = migrate message + sửa 3 service + sửa E2E. Và: **doc cũ có thể mâu thuẫn với chính nó** (§9.4 ghi “schema registry từ Phase 3” trong khi quyết định là không dùng) ⇒ sửa ngay trong ticket, không để “lát nữa”. Cuối cùng: soạn doc cũng cần verify — 2 lỗi (chèn section vào giữa bảng, lọt 1 từ tiếng Trung) **không** bị lint/CI bắt, chỉ `grep -n '^## '` + đọc lại mới thấy ⇒ PR docs-only không có CI thì bước tự đọc lại là **bắt buộc**. |

| 2026-11-08 | SL-3.1 | **"Khai bằng code" không có nghĩa là "đã khai"**: `Declarables` chỉ là *ý định*, `RabbitAdmin` khai **lazy** — chỉ khi có connection đầu tiên. Service chưa publish/consume (đúng trạng thái SL-3.1) ⇒ không ai mở connection ⇒ **không có queue nào**, log sạch, app healthy. Phải kiểm bằng **trạng thái của hệ ngoài** (management API), không kiểm bằng "code đã chạy". Fix: gọi `amqpAdmin.initialize()` ở `SmartInitializingSingleton` — nhưng **fail mềm** (broker chết không làm service chết, ADR-0007), và cái giá của fail mềm là lỗi chỉ còn 1 dòng WARN ⇒ bắt buộc phải có tool để *nhìn thấy* hậu quả. Thêm nữa: **monitoring có độ trễ riêng của nó** — `ready/unacked` của RabbitMQ management API lag ~5 s sau restart, nên đọc "0 message" ngay sau restart **không** chứng minh mất dữ liệu; phải kiểm bằng đường thứ hai (`messages`, hoặc `drain` để ack thật). Và: `routed=false` khi publish sai routing key là lỗi **im lặng** (message rơi vào hư không, không exception) ⇒ tool phải cảnh báo ngay cạnh kết quả. |

| 2026-11-08 | SL-3.2 | **`@ConditionalOnBean` là điều kiện *theo thời điểm*, không phải "bean này sẽ có"**: auto-config chạy theo thứ tự, điều kiện chỉ thấy bean đã đăng ký **trước** nó. Thiếu `after`/`afterName` ⇒ điều kiện âm thầm `false` ⇒ store dedupe không được tạo, bảng `processed_events` không xuất hiện, **không một dòng lỗi** — cùng họ với `SL-3.1` (`RabbitAdmin` khai lazy): code đúng, hệ không làm gì, log sạch. Cách phát hiện duy nhất: kiểm **trạng thái của hệ ngoài** (bảng trong DB, exchange trên broker), không kiểm "service healthy". Phụ: Boot 4 đặt `JdbcTemplateAutoConfiguration` ở module `spring-boot-jdbc` ⇒ dùng `afterName` (string) để commons không phải kéo dependency chỉ vì một cái tên. Hai bài học thiết kế: (1) **marker dedupe phải xoá khi xử lý fail** — nếu chỉ `insert` trước handler thì DLQ replay bị chính marker chặn, message nằm trong DLQ mà không bao giờ chạy lại; (2) **`AcknowledgeMode.MANUAL` là điều kiện của DLQ** — auto-ack nghĩa là message "xong" ngay khi listener trả về ⇒ không còn cơ hội `nack` khi fail ⇒ mất message im lặng. |

| 2026-11-09 | SL-3.5 | **Hai đường confirm: cái khó là *thứ tự kiểm tra*, không phải idempotency** — chốt đơn theo `payment.succeeded.v1` phải loại dần 5 khả năng (không saga · saga đã `CONFIRMED` · saga chưa `COMPLETED_START` · `paymentIntentId` lệch · đơn không còn `PENDING_PAYMENT`) trước khi chạm downstream; bỏ bước nào cũng là **chốt nhầm đơn đã trả tiền** (lỗi không sửa được bằng code). Bằng chứng thanh toán *đổi chỗ*: đường client hỏi payment (`verifyPayment`), đường event không hỏi lại (event phát ngay sau CAS `SUCCEEDED`) nhưng vẫn ghi `PAYMENT_VERIFY:SKIPPED` vào audit. `paymentIntentId` trong payload là **khoá đối chiếu**, không phải trang trí. **Ack cho kết cục nghiệp vụ, retry cho lỗi hạ tầng** (đơn `EXPIRED` + tiền đã thu = hoàn tiền tay ⇒ ack + log ERROR; inventory 503 = retry). `TD-25` release-trước-CAS mở cửa sổ bán quá tồn kho ⇒ chặn bằng kiểm `PENDING_PAYMENT` trước khi release + WARN khi CAS trả `false`. Và bài học cũ lặp lại: `@ConfigurationProperties` inject bean `ObjectMapper` (Jackson 2) không tồn tại ⇒ **context fail cả IT** — hạ tầng commons **không được giả định bean nào có mặt**. |

| 2026-11-08 | SL-3.11 | **Service-to-service auth: phần khó là *biết ai gọi*, không phải *gọi được*** — shared secret (phương án A) xong trong 30 phút nhưng mọi service dùng chung một danh tính: log không phân biệt được ai, và một chỗ rò rỉ mở toàn bộ endpoint chỉ-cần-`authenticated()`. `aud` + `sub=svc:<name>` biến "có request nội bộ" thành "**order-service** gọi **inventory** trong 5 phút tới". Hai bug thật: (1) `@ConfigurationProperties` **record lồng record** (`record Clients(Map…)`) làm path bind thành `clients.clients.<id>` ⇒ map rỗng ⇒ mọi request 401 "sai secret" (triệu chứng dối, đọc log tưởng sai secret); map phải là field trực tiếp, và map key có `-` đặt bằng property phẳng phải viết `clients[order-service].secret`. (2) Test JWT dùng mốc thời gian cố định ⇒ `JwtValidators` so `exp` với *now* ⇒ token "hết hạn ngay khi tạo", 3 test đỏ với thông báo chỉ nói "Jwt expired" ⇒ mốc phải là `Instant.now()`. Và: **chuẩn thắng kỳ vọng** — AC mình viết "sai audience ⇒ 403", đo thật Spring trả **401 `invalid_token`** (đúng: 403 dành cho "đã xác thực thiếu quyền") ⇒ sửa AC, không uốn code cho khớp giấy. |

| 2026-11-08 | SL-3.4 | **Replay DLQ không phải "publish lại payload"** — trong DLQ, `routing_key` của message là **tên queue** (`q.notification`) vì DLX đặt routing key = tên queue (ADR-0007 D2); routing key gốc nằm ở header `x-death[0].routing-keys`. Bản đầu `tools/dlq.sh move` publish lại bằng `routing_key` hiện tại ⇒ `routed=false` ⇒ message **bị ack khỏi DLQ rồi rơi vào hư không**, không một lỗi nào (đúng họ "lỗi im lặng" của `SL-3.1`/`SL-3.2`). Nguyên tắc cho tool vận hành: **peek trước, consume sau** — validate xong mới `ack`, và parse lỗi phải là **no-op**. Kèm bài học bash: `python3 -c "…nhiều dòng…"` trong `"$( … )"` là bẫy thật — parse trả rỗng *sau khi* message đã bị `get` mất; chuyển sang **file python tạm + heredoc quoted** là hết lớp lỗi này. Thứ hai: **property của Boot chỉ áp cho bean của Boot** — `spring.rabbitmq.listener.simple.auto-startup=false` không chạm tới `eventListenerContainerFactory` tự khai (cần `AcknowledgeMode.MANUAL`) ⇒ IT không có broker chết với `FatalListenerStartupException: Authentication failure`, **đỏ cả 17 test** dù logic đúng; phải tự khai công tắc (`shoplab.events.consumer-auto-startup`). Thứ ba, về thiết kế: **dedupe là điều kiện của retry** (marker phải bị xoá khi handler fail, nếu không DLQ replay bị chính marker chặn) và **async đổi "chặn" lấy "im lặng"** — service chết thì confirm vẫn 200 (đúng), message chờ trong queue rồi xử lý khi consumer sống lại, nhưng consumer chết **mãi** thì không ai biết ⇒ phải có metric queue depth (§`SL-3.8`/`SL-3.9`). |

| 2026-11-08 | SL-3.3 | **`eventId` phải ổn định qua các lần thử** — chi tiết quyết định cả pattern: relay sinh `eventId` mới mỗi lần publish lại thì dedupe của consumer **không chặn được gì** (mỗi lần là event "mới") ⇒ gửi email 2 lần. Dùng `outbox.id` làm `eventId` thì mọi lần retry mang cùng id. Hai điều nữa: (1) **claim ≠ publish trong cùng transaction** — claim là transaction ngắn (`UPDATE … SKIP LOCKED` + `claimed_at`) rồi publish **ngoài** transaction, nếu không broker chậm sẽ giữ row lock Postgres (biến sự cố broker thành sự cố DB); (2) **`@Transactional` phải đi qua proxy** — tách `CheckoutOutbox` thành bean riêng vì gọi method `@Transactional` cùng class là **không có transaction nào**, outbox ghi ngoài transaction và mất toàn bộ ý nghĩa. Bug thật đáng nhớ: **thiếu `publisher-confirm-type: correlated`** ⇒ `CorrelationData.getFuture()` không bao giờ complete ⇒ mỗi publish treo đúng 5 s rồi fail; outbox đã biến "mất event" thành "trễ event" (21 lần thử, 0 message mất) — đúng thứ pattern này mua được, nhưng `attempts` cao phải được đọc là **cấu hình sai**. |

## 7. Technical debt register

> Quy tắc ghi nhận: [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md). Mỗi món nợ **phải có
> điều kiện revisit verify được** — không phải "khi rảnh". Rà bảng này khi làm DoD mỗi phase.
> Trả xong thì giữ dòng, đổi `Mức độ` thành `✅ đã trả (<ticket>)`.

| ID | Mô tả | Nguồn | Điều kiện revisit | Mức độ |
|---|---|---|---|---|
| `TD-1` | Bảng `refresh_tokens` không có job dọn row hết hạn → phình dần | SL-1.4 | Khi bảng > 10⁶ row **hoặc** khi thêm scheduler | trung bình |
| `TD-2` | `GET /v1/admin/users` trả **toàn bộ** user, không phân trang | SL-1.4 | Khi số user > 1.000 hoặc có màn quản trị thật | thấp |
| `TD-3` | Handler map `AccessDeniedException` → 403 nằm trong **catalog**; service khác dùng `@PreAuthorize` phải copy | SL-1.5 | **Đã trả ở `SL-2.12`**: `AccessDeniedApiExceptionHandler` + `AccessDeniedAutoConfiguration` (`@ConditionalOnClass`) trong `service-commons`, xoá **6 bản copy** ở service | ✅ đã trả (SL-2.12) |
| `TD-4` | `tools/e2e-smoke.sh` phụ thuộc tài khoản seed + `docker logs` ⇒ chỉ chạy local, chưa vào CI | SL-1.7 | **Đã trả ở `SL-2.8`**: workflow `e2e.yml` dựng compose + chạy `e2e-smoke.sh` + `e2e-checkout.sh` (dispatch/hằng tuần) | ✅ đã trả (SL-2.8) |
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
| `TD-17` | Webhook `payment.succeeded` **không** cập nhật order (Phase 2 chưa có broker) ⇒ nếu saga không chạy, tiền đã thu mà đơn vẫn `PENDING_PAYMENT`; chưa có `refund`/`transactions` để hoàn tiền ca này | SL-2.7 | Khi làm `SL-2.6`/`SL-2.9` (test compensating thấy ca này) **hoặc** khi Phase 3 có event `payment.succeeded` | ✅ đã trả (SL-3.5) |
| `TD-18` | Webhook dùng **một** HMAC secret tĩnh, không key id/rotation; không lưu raw body (chỉ sha256) nên điều tra tranh chấp phải xin lại provider | SL-2.7 | Khi nối provider thật (Stripe/VNPay) — họ có key id + rotation riêng | thấp |
| `TD-19` | Idempotency store (`idempotency_keys` + `IdempotencyService`) **copy lần 2** (order, payment) chưa có module chung ở `packages/` | SL-2.7 | Khi có service thứ 3 cần idempotency (dự kiến không có ở Phase 2 ⇒ quyết ở `SL-2.12` hoặc Phase 3) | thấp |
| `TD-20` | Cart **không** lưu `price_snapshot` và không gọi catalog (quyết định `SL-2.1`) ⇒ hiển thị giá trong giỏ phải gọi catalog riêng; giỏ không phát hiện được SKU đã archive/đổi giá | SL-2.1 | **`SL-2.13` đã có `GET /v1/skus?ids=` (batch)** ⇒ phần còn lại chỉ là UI/gateway gọi endpoint đó (làm ở `SL-2.8`) | trung bình |
| `TD-22` | `GET /v1/skus?ids=` là **public**, nhận tới 100 id, **chưa rate limit** và **chưa cache** ⇒ 1 client có thể dội tải catalog (100 id/request) hoặc dùng làm đòn bộc phát | SL-2.13 | Khi bật rate limit ở gateway (Phase 5) **hoặc** khi đo được p99 list/get của catalog tăng do traffic `/v1/skus` (Phase 4 — cache-aside) | thấp |
| `TD-23` | IT `inventory`: `commitAfterExpiryReturnsGone` **flaky** — test đẩy `expires_at` về quá khứ rồi `commit`, nhưng sweeper trong IT chạy mỗi **300 ms** nên có lúc nó expire trước ⇒ `commit` trả **409** thay vì **410** (đỏ trên CI chậm, xanh ở máy local) | SL-2.13 (CI run `42444551383`) | Lần tới khi sửa test của inventory: tắt sweeper (`sweep-interval-ms` rất lớn) cho test lazy-expire, hoặc assert `status ∈ {410, 409}` + kiểm `reserved = 0` (điều thực sự quan trọng) | trung bình |
| `TD-24` | `checkout_saga_steps` + `checkout_sagas` **không có job dọn** — 1 dòng/bước/lần checkout ⇒ phình mãi (audit nhưng không ai đọc sau 30 ngày) | SL-2.6a | Khi bảng > 10⁶ row hoặc khi `SL-2.6b` thêm sweeper (gộp 1 `@Scheduled` dọn saga `COMPLETED_START`/`FAILED_*` cũ > 30 ngày) | thấp |
| `TD-25` | Checkout sweeper **không gọi được `release`** (không có token user) ⇒ đơn `EXPIRED` nhưng hàng/voucher chỉ được trả khi **TTL sweeper** của inventory/promotion chạy (trễ tối đa ~1 nhịp, 60 s) | SL-2.6b | Khi Phase 3 có **service-to-service auth** (hoặc API internal riêng) ⇒ sweeper gọi `release` ngay trong cùng nhịp | ✅ đã trả (SL-3.5) |
| `TD-26` | `build-image` (CI) **flaky** khi Maven Central tạm thời không trả artifact: mỗi image `mvn package` từ đầu KHÔNG cache Maven ⇒ gặp hiccup là đỏ oan (gặp thật ở `SL-2.8`: `spring-boot-dependencies:4.1.2 … absent in central`, rerun thì xanh) | SL-2.8 (run `42451712993`) | Phase 7: registry + **cache layer Maven** (`--mount=type=cache` hoặc image builder có `.m2` ấm) ⇒ build nhanh và bớt phụ thuộc Central | thấp |
| `TD-21` | `/v1/carts/**` là public ở gateway và **chưa có rate limit** ⇒ có thể spam tạo guest token/giỏ (mỗi token = 2 key Redis, TTL 30 ngày) | SL-2.1 | Khi bật rate limit ở gateway (Phase 5) **hoặc** khi thấy số key Redis tăng bất thường | trung bình |
| `TD-29` | Bảng `outbox` **không có job dọn** row `SENT` ⇒ 1 row/event/lần checkout, phình dần. Hệ quả phụ: đường quét `WHERE status = 'PENDING'` vẫn dùng index riêng nên không chậm, nhưng backup/disk thì lớn lên | SL-3.3 | **Đã trả ở `SL-3.7`** (chưa) — revisit khi bảng > 10⁶ row hoặc khi `notification-service` (`SL-3.4`) xong: gộp 1 job retention cho `outbox` + `processed_events` (giữ 7 ngày để còn điều tra) | trung bình |
| `TD-28` | **CI đang TẮT tạm thời** (`vars.CI_ENABLED=false`, từ `SL-3.10`) để tăng tốc merge Phase 3 ⇒ mất 3 hàng rào tự động: `test` + JaCoCo gate độc lập, `lint` trong môi trường sạch, và **`build-image` matrix** (chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module/service). Verify chuyển sang **local** (`mvn verify` + e2e) do người/AI tự khai | SL-3.10 | **Bắt buộc** bật lại khi đóng Phase 3 (`gh variable delete CI_ENABLED`) — hoặc sớm hơn nếu có ≥ 1 lần `main` đỏ mà local xanh | cao |
| `TD-30` | `notification_logs` + `processed_events` **không có job dọn**: `processed_events` tăng 1 row/event/consumer **vĩnh viễn** (không xoá được vì dedupe cần), `notification_logs` tăng 1 row/đơn đã confirm — sau 1 năm là chục triệu row ở cả 2 bảng | SL-3.4 | Gộp vào **cùng job retention** với `TD-29`: `processed_events` giữ ≥ 7 ngày (đủ dài hơn max retry + replay tay), `notification_logs` giữ theo yêu cầu nghiệp vụ (≥ 90 ngày). Revisit khi bảng > 10⁶ row **hoặc** khi làm `SL-3.7`/`SL-3.9` (job/runbook) | trung bình |
| `TD-31` | **Không có refund**: ca `payment.succeeded` cho đơn đã `EXPIRED`/`CANCELLED` (tiền đã thu, hàng đã trả) hiện chỉ log ERROR `TIỀN ĐÃ THU … cần hoàn tiền tay`. Chưa có bảng `refunds`/`transactions`, chưa có API hoàn tiền | SL-3.5 | Phase 6 (refund) hoặc ngay khi có ≥ 1 ca thật cần hoàn tiền trong môi trường chạy thật | cao |
| `TD-32` | **Outbox bị copy 2 lần** (`order` SL-3.3, `payment` SL-3.5) và `SL-3.6` (catalog) sẽ là lần thứ 3: ~500 dòng gần giống nhau (record/repository/relay/properties/DDL) ở mỗi service | SL-3.5 | Khi làm `SL-3.6` — extract `OutboxAutoConfiguration` vào `service-commons` (bảng + relay + properties dùng chung, service chỉ khai `outbox.append(...)`) hoặc chấp nhận copy lần 3 và đóng nợ bằng lý do rõ ràng | trung bình |


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
| 2026-11-08 | `SL-2.6b` → `Done`: **checkout saga hoàn tất** — `POST /v1/checkout/{orderId}/confirm` (verify payment ở **server** rồi commit stock → voucher ⇒ order `CONFIRMED`; đơn 0 đồng bỏ qua verify — ADR-0006 D6), `POST /v1/checkout/{orderId}/cancel` (release stock + voucher ⇒ `CANCELLED`), `CheckoutSagaSweeper` (quá `payment-window` 15′ ⇒ order/saga `EXPIRED`; hàng/voucher do **TTL sweeper** của inventory/promotion trả — chốt phương án A, ghi `TD-25`); state saga thêm `CONFIRMED`/`CANCELLED`/`EXPIRED` (migration `V3`), step audit thêm `PAYMENT_VERIFY`/`STOCK_COMMIT`/`VOUCHER_COMMIT`/`STOCK_RELEASE`/`VOUCHER_RELEASE`/`EXPIRE`. Verify: `mvn verify` **BUILD SUCCESS** (order 65 unit + 25 IT, checkstyle 0) · chạy thật: confirm trước khi trả tiền → **409**; mock-pay → confirm **200** `CONFIRMED`, `on_hand 10→8`, voucher `COMMITTED`, history 2 dòng; confirm#2 200 (history vẫn 2); cancel → `RELEASED` + `CANCELLED`, cancel#2 200, cancel sau confirm 409; bỏ ngang 20 s → sweeper `EXPIRED`/`PAYMENT_WINDOW_EXCEEDED` rồi TTL 45 s trả `reserved 2→0` + voucher `EXPIRED` · `e2e-smoke` 17/17 · OpenAPI 39 → **41 path**. **Sự cố**: record `@ConfigurationProperties` chỉ được 1 constructor; checkstyle bắt `switch` thiếu `default`. Debt: `TD-25`. |
| 2026-11-08 | `SL-2.12` → `Done` (**trả `TD-3`**): **403 handler về `service-commons`** — thêm `web/AccessDeniedApiExceptionHandler` (bản duy nhất) + `web/AccessDeniedAutoConfiguration` (`@AutoConfiguration @ConditionalOnClass(AccessDeniedException) @ConditionalOnWebApplication(SERVLET)` + `@ConditionalOnMissingBean`) đăng ký trong `AutoConfiguration.imports`; `service-commons/pom.xml` thêm `spring-security-core` **`optional`** (cùng khuôn `webmvc`/`springdoc`); **xoá 6 bản copy** ở `catalog`/`inventory`/`cart`/`order`/`payment`/`promotion` (~296 dòng trùng). Test mới: `AccessDeniedApiExceptionHandlerTest` (403 + `code`/`path`/`traceId` + không lộ message nội bộ + **canary `@Order`**) và `AccessDeniedAutoConfigurationTest` (`WebApplicationContextRunner` + `FilteredClassLoader` chứng minh guard: không có security ⇒ im lặng bỏ qua). **Bug thật bắt được khi verify**: lần `verify` đầu đỏ 2 IT catalog (`expected 403 but was 500`) — `ExceptionHandlerExceptionResolver` lấy advice **đầu tiên khớp**, nên catch-all `@ExceptionHandler(Exception.class)` của `GlobalExceptionHandler` bắt `AuthorizationDeniedException` trước khi tới advice cụ thể (thứ tự bean đảo khi chuyển từ `@Component` sang auto-config) ⇒ fix bằng `@Order(Ordered.HIGHEST_PRECEDENCE)`. Verify: **655 test (537 unit + 118 IT) · 0 fail** · `mvn verify` BUILD SUCCESS 11 module · `grep -rl 'class AccessDeniedApiExceptionHandler'` = **1 file**, `services/**` = **0** · stack thật (rebuild `catalog`+`inventory`): buyer `POST /v1/categories` **403** `FORBIDDEN` + log `com.shoplab.commons.web.AccessDeniedApiExceptionHandler`, buyer `PUT /v1/stock` **403**, seller **201** / admin **200** (không chặn oan) · `e2e-smoke` 17/17 · `e2e-checkout` 27/27 · `e2e-checkout-fail` 41/41 (flaky 38/3 ở lần đầu sau rebuild → `TD-27`). |

| 2026-11-08 | **`SL-2.10` → `Done` · ĐÓNG PHASE 2** (15/15): **runbook** [`docs/runbook-phase-2.md`](docs/runbook-phase-2.md) — sơ đồ luồng saga (6 bước + ai dọn khi bỏ ngang) · bảng port/health/DB/log + **env knob** (`SAGA_READ_TIMEOUT` 800ms · TTL reservation/lock 15′ · `SAGA_PAYMENT_WINDOW` · 3 sweeper 60 s) · 20+ lệnh kiểm tra copy-paste (kể cả `psql` đọc `stock`/`reservations`/`stock_ledger`/`vouchers`/`checkout_sagas` + Redis `cart:u:*`) · **7 lỗi** thường gặp · **§5 output debug thật** (tắt `payment-service` ⇒ 503 + `reserved` về 0 + `RESERVE`/`RELEASE` khớp ledger + `saga FAILED_PAYMENT/DOWNSTREAM_UNAVAILABLE_PAYMENT`; khôi phục ⇒ 201) + bẫy **health check Docker trễ 1 nhịp**. **DoD phase**: archive **15 ticket** → [`Backlog-phase-2.md`](Backlog-phase-2.md) (viết bổ sung `SL-2.6b` vì ticket đó Done mà **không có đặc tả** — dựng lại từ commit `5da0cc6`, có ghi nguồn) · [`docs/phases/phase-2.md`](docs/phases/phase-2.md) (số liệu đo thật) · `docs/phases/README.md` + bảng archive (§3) + `architecture.md` §12 đã update. Verify: **`down -v` → `up -d --build` ⇒ 12/12 container healthy** trên volume rỗng (Flyway + seed 5/20/24 tự chạy) · `e2e-smoke` **17/17** · `e2e-checkout` **27/27** · `e2e-checkout-fail` **41/41** · `mvn -B -ntp verify` **BUILD SUCCESS · 655 test (537 unit + 118 IT) · 0 fail** · OpenAPI **41 path / 50 operation / 35 token / 46 schema**. Tag annotated **`phase-2`** + GitHub Release trên merge commit. |

| 2026-11-08 | **Breakdown Phase 3** → **10 ticket** chi tiết: `SL-3.0` (ADR-0007 contract event: envelope · naming · outbox relay polling · DLQ) · `SL-3.1` (RabbitMQ topology + `tools/rabbit-topology.sh`) · `SL-3.2` (commons: `EventEnvelope` + publisher confirm + consumer base retry→DLQ + dedupe `processed_events`) · `SL-3.3` (order **outbox** + relay ⇒ `order.confirmed.v1`/`order.cancelled.v1`) · `SL-3.4` (**`notification-service`** 8089: consumer + email mock + DLQ + `tools/dlq.sh`) · `SL-3.5` (`payment.succeeded.v1` ⇒ **auto-confirm** ở order, **trả `TD-17`**) · `SL-3.6` (catalog outbox + **`search-service`** 8090 **Postgres FTS** + reconcile + `GET /v1/search`) · `SL-3.7` (E2E async) · `SL-3.8` (báo cáo before/after async) · `SL-3.9` (runbook + DoD phase 3). **5 quyết định chốt khi breakdown**: broker = **RabbitMQ** (Kafka Phase 8) · `search-service` = **Postgres FTS** ở Phase 3, **OpenSearch Phase 8** (sửa mâu thuẫn §5 vs §7/§12 trong `architecture.md`) · outbox relay = **polling** (`FOR UPDATE SKIP LOCKED`, không CDC) · **at-least-once** + consumer idempotent theo `eventId` (không hứa exactly-once) · **DLQ không auto-replay**. |

| 2026-11-08 | `SL-2.8` → `Done`: **E2E checkout** — `tools/e2e-checkout.sh` (27 check: tiền · saga · idempotency · mock-pay · confirm · order · tồn kho) + workflow **`e2e.yml`** (compose 10 thành phần + 2 script; `workflow_dispatch` + `schedule` hằng tuần, **không** chạy mỗi PR vì ≈30 phút/run) ⇒ **trả `TD-4`**; verify OpenAPI **41 path/50 operation** + matrix `build-image` **9 service** (đã có, chỉ kiểm). Verify: `e2e-checkout` **27 PASS / 0 FAIL** (931 ms) · `e2e-smoke` 17/17 · `e2e.yml` parse OK · `compose config` OK. **Sự cố**: fail giả do Python đổi số tiền thành float ⇒ `parse_float=str`; CI `build-image` flaky do Maven Central hiccup (rerun xanh) ⇒ `TD-26`. |
| 2026-11-08 | `SL-3.0` → `Done`: **ADR-0007 — contract event Phase 3** (`docs/adr/0007-event-contract.md`, 185 dòng): chốt **5 quyết định** — (D1) envelope JSON 7 field (`eventId · eventType · version · occurredAt · producer · traceId · payload`, không field nghiệp vụ) · (D2) exchange topic `shoplab.events` + `shoplab.dlx`, queue **theo consumer** (`q.notification`/`q.order`/`q.search`) + `<queue>.dlq`, routing key `<aggregate>.<event>.v<N>` · (D3) **at-least-once** + consumer **bắt buộc** dedupe `eventId` (`processed_events` PK `(event_id, consumer)`) · (D4) outbox **polling relay** (`@Scheduled` 1 s, batch 100, `FOR UPDATE SKIP LOCKED`, `SENT` chỉ sau publisher confirm, lỗi giữ `PENDING`) · (D5) retry 3 lần trong consumer (200 ms/1 s/3 s) → DLQ, **không auto-replay**, replay tay qua `tools/dlq.sh` có 3 điều kiện kiểm trước. Kèm **bảng 5 event** (`order.confirmed.v1` · `order.cancelled.v1` · `payment.succeeded.v1` · `product.updated.v1` · `product.archived.v1`) + chính sách versioning (không sửa `.v1`, phát `.v2` song song). Mỗi hạng mục có bảng Options ≥ 3 phương án **có loại** + Revisit định lượng. Update `architecture.md`: **§9.4** (bỏ “schema registry từ Phase 3” → version trong envelope + link ADR), §11.2 (link ADR-0007), header `DRAFT v0.5`, Changelog. Verify: ADR 5 mục đúng template · 5/5 hạng mục có bảng trade-off · link check 0 broken. Không code. |
| 2026-11-08 | `SL-3.1` → `Done`: **RabbitMQ topology bằng code** — `service-commons` thêm `spring-boot-starter-amqp` (**optional**) + `events/EventTopology` (tên exchange/queue/routing key = nguồn sự thật duy nhất, lấy từ ADR-0007) + `EventTopologyAutoConfiguration` (`@ConditionalOnClass(Declarables)` · `@ConditionalOnProperty(shoplab.events.topology.enabled)`) khai `shoplab.events` (topic) + `shoplab.dlx` (direct) + 3 queue (`q.notification`/`q.order`/`q.search`, durable, DLX `shoplab.dlx` routing key = tên queue) + 3 `.dlq` + **5 binding**; thêm `EventTopologyDeclarer` gọi `amqpAdmin.initialize()` lúc startup (**bug thật**: `RabbitAdmin` khai lazy, không có consumer/publisher ⇒ không khai gì). `order-service` là service đầu tiên khai (`spring.rabbitmq.*` + env compose + `depends_on: rabbitmq healthy`). Tool mới **`tools/rabbit-topology.sh`** (`show` · `dlq` · `publish` · `drain` · `purge`, curl + python3). README thêm mục **Broker (RabbitMQ)** + bảng đọc `ready`/`unacked`/`consumers` + **4 bẫy**. Verify: topology đúng ADR (2 exchange · 6 queue · 8 binding) · publish → `ready=1` → `docker restart` broker → `messages=2` **không mất** → `drain` ack → `ready=0` · `routed=false` khi sai key · `mvn verify` **BUILD SUCCESS · 662 test (544 unit + 118 IT)** · `e2e-smoke` 17/17 · `e2e-checkout` 27/27 · `e2e-checkout-fail` 41/41. |
| 2026-11-08 | `SL-3.2` → `Done`: **hạ tầng event trong `service-commons`** — `EventEnvelope` (7 field, `create()` sinh `eventId` + `traceId` từ MDC, `versionOf()` parse hậu tố `.vN`) · `EventEnvelopeCodec` (encode/decode + validate: **version lạ ⇒ `EventFormatException`** để đẩy DLQ) · `EventPublisher` (**publisher confirm** qua `CorrelationData.getFuture()` + message `PERSISTENT`, nack/timeout ⇒ `EventPublishException`) · `ProcessedEventStore` + `JdbcProcessedEventStore` (`insert … on conflict do nothing` ⇒ `rowsAffected`, PK `(event_id, consumer)`, kèm `unmarkProcessed`) · `AbstractEventConsumer` (`ChannelAwareMessageListener`: decode → skip event lạ → **dedupe** → handler → ack; lỗi ⇒ retry 3 (200 ms/1 s/3 s) ⇒ **xoá marker** ⇒ `nack(requeue=false)` = DLQ; đặt lại `traceId` của producer vào MDC) · `EventProperties` + `EventAutoConfiguration` (container factory **MANUAL ack**, `prefetch=1`/`concurrency=1` để giữ thứ tự). **Bug thật**: `@ConditionalOnBean(JdbcTemplate)` chạy trước `JdbcTemplateAutoConfiguration` ⇒ store **không được tạo, không lỗi** ⇒ fix bằng `after`/`afterName` + 2 test khoá lại; verify trên stack: bảng `processed_events` xuất hiện trong DB `order` (PK `(event_id, consumer)`). Verify: commons **71 unit** (+15 mới) · toàn repo **BUILD SUCCESS · 679 test (561 unit + 118 IT)** · `order-service` rebuild healthy · `e2e-smoke` 17/17 · `e2e-checkout` 27/27 · `e2e-checkout-fail` 41/41. |
| 2026-11-08 | `SL-3.10` → `Done`: **tắt CI tạm thời** để tăng tốc merge Phase 3 — kill-switch có sẵn `gh variable set CI_ENABLED --body false` (3 job `skipping`, 0 phút) + **`tools/merge-pr.sh --skip-ci-check`** (mặc định vẫn chặn; có cờ ⇒ in CẢNH BÁO + ghi `CI: BỎ QUA` vào comment evidence hậu merge; vẫn bắt buộc PR mở/không draft + comment tóm tắt + diff không rỗng). Ghi rule: `AGENTS.md` §9 (ngoại lệ + lệnh bật lại) · `.clinerules/10-git-workflow.md` (điều kiện merge thay thế) · `README.md` mục CI · **`TD-28`** (mức `cao`, revisit = **bắt buộc** bật lại khi đóng Phase 3). Verify: `gh variable list` → `CI_ENABLED=false` · push nhánh mới ⇒ **0 run** · `gh pr checks` → 3 job `skipping` · không cờ ⇒ script **chặn** (`run … là 'skipped' (cần 'success')`) · có cờ ⇒ merge được + cảnh báo. |
| 2026-11-08 | `SL-3.3` → `Done`: **transactional outbox ở `order-service`** — migration `V4__create_outbox.sql` (`outbox` + `status PENDING/SENT` + CHECK `SENT ⇒ sent_at` + index riêng cho `PENDING` + `claimed_at` để 2 relay không gửi trùng) · `OutboxRepository`/`JdbcOutboxRepository` (`claimPending` = 1 câu `UPDATE … WHERE id IN (SELECT … FOR UPDATE SKIP LOCKED) RETURNING`, `markSent`, `markFailed` trả claim) · **`CheckoutOutbox`** (bean riêng `@Transactional`: `confirm`/`cancel` ghi order + saga + outbox **cùng transaction** — self-invocation sẽ bỏ qua proxy) · **`OutboxRelay`** (`@Scheduled` 1 s, batch 100, **`eventId = outbox.id`** để retry giữ nguyên id cho dedupe của consumer, lỗi 1 row không chặn row khác) · `OutboxProperties` + env compose. **2 bug thật**: (a) thiếu `spring.rabbitmq.publisher-confirm-type=correlated` ⇒ `getFuture()` không bao giờ complete ⇒ mỗi publish treo 5 s (outbox cứu: event không mất, chỉ trễ); (b) `JdbcOutboxRepository` đòi bean `ObjectMapper` ⇒ IT đỏ 28/28 ⇒ tự tạo mapper. Verify: **mvn verify BUILD SUCCESS · 687 test (566 unit + 121 IT)** · stack thật: confirm ⇒ `PENDING|0` → `SENT|1` + message `q.notification` · **broker chết ⇒ order vẫn `CONFIRMED`**, row `PENDING` `attempts=11`, bật lại ⇒ `SENT` · cancel ⇒ `order.cancelled.v1|SENT` · e2e 17/17 · 27/27 · 41/41. Nợ mới: `TD-29` (chưa có job dọn row `SENT`). |
| 2026-11-08 | `SL-3.4` → `Done`: **`notification-service` (8089)** — service mới (`notification` DB riêng, Flyway `V1`: `notification_logs` + `notification_templates` seed 2 mẫu + `processed_events`) · consumer `q.notification` (`order.confirmed.v1` / `order.cancelled.v1`) **không viết lại hạ tầng**: chỉ override `consumerName()`/`handledEventTypes()`/`handleEvent()` trên `AbstractEventConsumer` (~60 dòng, 0 dòng retry/DLQ/dedupe) · `TemplateRenderer` placeholder `{{var}}` (không engine) · `GET /v1/notifications/me` (userId từ claim `sub`, `?limit` bị chặn bởi trần cấu hình) · `SHOPLAB_NOTIFICATION_FAIL_MODE` **fail-closed** (chỉ `env=local`) để test retry/DLQ · **`tools/dlq.sh`** `list|peek|move|purge` — replay đọc **routing key gốc từ `x-death`**, `peek` trước rồi mới `ack`. Hạ tầng: commons thêm cờ `shoplab.events.consumer-auto-startup` (property `spring.rabbitmq.listener.simple.auto-startup` của Boot **không** áp cho container factory tự khai ⇒ IT đỏ 17 test), compose + gateway route + CI matrix + `.env.example`. **Bug thật**: `dlq.sh move` bản đầu publish bằng `routing_key` hiện tại = tên queue ⇒ `routed=false` ⇒ **mất message**; parse `python3 -c` nhiều dòng trong `$( )` cũng làm rơi 1 message (chuyển sang file python tạm). Verify: `mvn -B -ntp verify` **BUILD SUCCESS · 730 test (600 unit + 130 IT)** · async thật: publish → row sau **239 ms** · dedupe: cùng `eventId` 2 lần ⇒ **1 row** · fail-mode ⇒ `ready=0 unacked=0` + `q.notification.dlq ready=1`, `dlq.sh move` ⇒ DLQ `1→0` + `notification_logs` +1 · `docker stop` notification ⇒ confirm vẫn **200**, bật lại ⇒ message được xử lý · e2e 17/17 · 27/27 · 41/41. Nợ mới: `TD-30` (retention `notification_logs` + `processed_events`).
| 2026-11-08 | `SL-3.11` → `Done`: **service-to-service auth** (`client_credentials` + `aud`/`exp`, **ADR-0008**) — `identity-service`: `POST /v1/auth/service-token` (`clientId`+`clientSecret`+`audience`) ⇒ JWT RS256 `role=SERVICE`, `aud=[service đích]`, `exp=5m`, `sub=svc:<client>`, `jti`; registry client trong config (`shoplab.service-auth.clients[<id>].secret` từ env + `allowed-audiences`), secret so sánh **hằng thời gian**, client lạ/sai secret trả **cùng** 401; `service-commons`: `ServiceTokenClient` (cache tới `exp-30s`, một khoá cho lần nạp, **không cache lỗi**, `invalidate` khi 401) + `HttpServiceTokenSource` (JDK `HttpClient`) + `ServiceTokenRules.validator(issuer, serviceName)` (kiểm `aud` **chỉ khi** `role=SERVICE`); `inventory` + `promotion` dùng validator đó. Compose/`.env.example`: `ORDER_SERVICE_CLIENT_SECRET` (**không default** ⇒ thiếu env là identity không khởi động). Verify: `mvn -B -ntp verify` **BUILD SUCCESS · 746 test (614 unit + 132 IT)** · stack thật: token `expiresIn=300 role=SERVICE aud=inventory-service sub=svc:order-service` · sai secret **401**, `aud=payment-service` **400** (allowlist) · `inventory` + đúng `aud` **404** (qua auth) vs sai `aud` **401 `invalid_token`** · `promotion` + đúng `aud` **403** (thiếu role ADMIN) vs sai `aud` **401** · token user không hồi quy (404) · e2e 17/17 · 27/27 · 41/41. **2 bug thật**: record lồng record làm bind `clients.clients.*` ⇒ map rỗng ⇒ 401 "sai secret"; test JWT mốc cố định ⇒ token hết hạn ngay khi tạo. |
| 2026-11-09 | `SL-3.5` → `Done` (**trả `TD-17` + `TD-25`**): **`payment.succeeded.v1` ⇒ order tự confirm** — `payment-service`: migration `V2__create_outbox.sql` (DDL chép khuôn `SL-3.3` + **unique index** `ux_outbox_payment_succeeded` để 1 intent chỉ phát 1 event), outbox package (record/repository/relay/properties/`PaymentEvents`), `PaymentWebhookService` ghi outbox **trong cùng transaction** ở nhánh transition thật sang `SUCCEEDED` (nhánh idempotent-return không ghi), `spring-boot-starter-amqp` + `publisher-confirm-type: correlated`. `order-service`: `PaymentSucceededConsumer` (`q.order`, dùng lại `AbstractEventConsumer`), `CheckoutFinishService.confirmByEvent` (**ma trận 5 nhánh** trước khi chạm downstream · `paymentIntentId` phải khớp saga · **bỏ** `verifyPayment` nhưng ghi step `PAYMENT_VERIFY:SKIPPED` · step mới `PAYMENT_EVENT_CONFIRM`), commit/release gọi bằng **service token** (`SL-3.11`) nên **sweeper release ngay** thay vì chờ TTL (`TD-25`); `CheckoutStep` + `PAYMENT_EVENT_CONFIRM`. Hạ tầng: order khai `shoplab.service-auth.*` + compose env, `outputs`; commons `HttpServiceTokenSource` **tự tạo ObjectMapper** (bài học: inject bean Jackson 2 làm context fail IT). Verify: `mvn -B -ntp verify` **BUILD SUCCESS · 756 test (624 unit + 132 IT)** · mock-pay **không** gọi `/confirm` ⇒ CONFIRMED sau **1031 ms** (909 ms khi có voucher), saga `CONFIRMED`, steps `STOCK_COMMIT:OK`/`VOUCHER_COMMIT:OK`/`PAYMENT_VERIFY:SKIPPED`/`PAYMENT_EVENT_CONFIRM:OK`, voucher `used=1 locked=0` · `/confirm` sau đó **200**, stock **không** trừ 2 lần, history 2 dòng · webhook lặp ⇒ `duplicate:true`, outbox **1 row** · đơn `EXPIRED` ⇒ ack + WARN, state không đổi, **DLQ 0** · broker chết ⇒ `SUCCEEDED` + outbox `PENDING`, bật lại ⇒ tự confirm sau **7.8 s** · sweeper: `EXPIRED` ⇒ hàng về kho **~0 s** (`STOCK_RELEASE:OK`) · e2e 17/17 · 27/27 · 41/41. Nợ mới: `TD-31` (refund), `TD-32` (extract outbox). |
