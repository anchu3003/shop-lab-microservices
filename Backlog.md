# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (Phase 4 đã breakdown **13 ticket**; kế tiếp là `SL-4.1`) |
| **Ticket tiếp theo** | `SL-4.1` — k6 harness + baseline (tìm **điểm vỡ** đầu tiên, trước khi tối ưu gì) |
| **Số ticket chi tiết hiện có** | 66 (Phase 0: 8 · Phase 1: 19 · Phase 2: 15 · Phase 3: 12 — đã archive · Phase 4: **13** — đang mở) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | ✅ Done (tag `phase-1`) | 19 / 19 |
| 2 | Core commerce flow | ✅ Done (tag `phase-2`) | 15 / 15 |
| 3 | Async & event-driven | ✅ Done (tag `phase-3`, 12 ticket) | 12 / 12 |
| 4 | Scale & performance | 🟡 Đang làm (breakdown xong **13 ticket**) | 1 / 13 |
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
| 3 — Async & event-driven | [`Backlog-phase-3.md`](Backlog-phase-3.md) | [`docs/phases/phase-3.md`](docs/phases/phase-3.md) | `phase-3` | `SL-3.0` … `SL-3.11` | ✅ Done (12/12) |

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

### Phase 3 — Async & event-driven (✅ Done 2026-11-11 — chi tiết đã tách sang file archive)

> **Mục tiêu phase**: phần **không critical** chạy async, và **event không bị mất** khi có sự cố — trọng tâm là
> *transactional outbox + at-least-once + consumer idempotent + DLQ*, không phải "dùng được Kafka".
> **Điều kiện hoàn thành** (đã đạt): `order.confirmed` phát ra **từ outbox** → `notification-service` gửi email (mock)
> async · `payment.succeeded` tự confirm đơn (**trả `TD-17`**) · index search cập nhật sau khi product đổi · message lỗi
> vào **DLQ** (không chặn queue) · có báo cáo số liệu async · runbook + tag `phase-3`.

**12/12 ticket Done** (`SL-3.0` … `SL-3.11` — 10 kế hoạch + 2 phát sinh: `SL-3.10` tắt CI tạm thời, `SL-3.11` tách
service-to-service auth từ `SL-3.5`).

- 📦 **Archive chi tiết** (verify + learning note từng ticket): [`Backlog-phase-3.md`](Backlog-phase-3.md)
- 🏷️ **Snapshot**: tag [`phase-3`](docs/phases/phase-3.md) · doc [`docs/phases/phase-3.md`](docs/phases/phase-3.md) ·
  [GitHub Release](https://github.com/anchu3003/shop-lab-microservices/releases/tag/phase-3)
- 📊 **Số liệu async**: [`docs/reports/async-phase-3.md`](docs/reports/async-phase-3.md) — checkout p50 **42,7 → 43,1 ms**
  (async **không** nhanh hơn), `confirm`→email p95 **1011 ms**, relay **63,7 msg/s**, **DLQ = 0** dưới tải
- 📄 **Contract**: [`docs/api/openapi.yaml`](docs/api/openapi.yaml) — **42 path / 51 operation / 48 schema** (`TD-34`)
- 🚑 **Ops**: [`docs/runbook-phase-3.md`](docs/runbook-phase-3.md) (4 tầng debug · **8 lỗi** · ca debug thật)
- 🧪 **Verify nhanh**: `bash tools/e2e-async.sh` (48) · `bash tools/bench-async.sh` · `bash tools/rabbit-topology.sh` ·
  `mvn -B -ntp verify` (**805 test**)

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-3.0` | **ADR-0007** — contract event: envelope · đặt tên · **outbox relay** (polling) · **DLQ policy** · dedupe | S | ✅ Done |
| 2 | `SL-3.1` | **RabbitMQ**: topology (exchange/queue/DLQ) + healthcheck + `tools/rabbit-topology.sh` | S | ✅ Done |
| 3 | `SL-3.2` | `service-commons`: `EventEnvelope` + publisher (confirm) + consumer base (retry → DLQ) + dedupe `processed_events` | M | ✅ Done |
| 4 | `SL-3.3` | `order-service`: **transactional outbox** + relay ⇒ `order.confirmed.v1` / `order.cancelled.v1` | M | ✅ Done |
| 5 | `SL-3.4` | **`notification-service`** (8089): consumer `order.confirmed` → email mock + retry + **DLQ** + `tools/dlq.sh` | M | ✅ Done |
| 6 | `SL-3.5` | `payment-service` outbox + `payment.succeeded.v1` ⇒ `order-service` **auto-confirm** (**trả `TD-17`** + `TD-25`) | M | ✅ Done |
| 7 | `SL-3.6` | `catalog-service` outbox product event + **`search-service`** (8091, Postgres **FTS**) index + reconcile + `GET /v1/search` | M | ✅ Done |
| 8 | `SL-3.7` | E2E async (`tools/e2e-async.sh`) + bước trong `e2e.yml` | S | ✅ Done |
| 9 | `SL-3.8` | **Báo cáo async**: latency checkout · độ trễ confirm→email · throughput relay · DLQ = 0 dưới tải | S | ✅ Done |
| 10 | `SL-3.9` | `docs/runbook-phase-3.md` + **DoD Phase 3** (archive · tag `phase-3` · release) | S | ✅ Done |
| 11 | `SL-3.10` | *(phát sinh)* **process**: tắt CI tạm thời (`CI_ENABLED=false`) để tăng tốc merge + `merge-pr.sh --skip-ci-check` (**đã bật lại ở `SL-3.9`**) | S | ✅ Done |
| 12 | `SL-3.11` | *(phát sinh, tách từ `SL-3.5`)* **Service-to-service auth**: `client_credentials` (identity) + `aud`/`exp` + validator ở commons | M | ✅ Done |

**Quyết định đã chốt khi breakdown** *(2026-11-08)*:

- **Broker = RabbitMQ** (đã chốt ở `architecture.md` §11.2): routing + DLQ có sẵn, học concept nhanh; **Kafka để Phase 8**
  (event streaming/CDC) — so sánh 2 broker là bài học trade-off, không làm sớm.
- **`search-service` dùng Postgres FTS ở Phase 3** (DB riêng `search`, `tsvector` + `unaccent`), **OpenSearch để Phase 8**.
  Lý do: docs cũ mâu thuẫn (§5 ghi OpenSearch Phase 3, §7/§12 ghi Phase 8); Phase 3 cần học **event-driven index +
  reconcile**, thứ đó không phụ thuộc engine. Đổi storage ở Phase 8 mà **không đổi event contract** ⇒ đã sửa `architecture.md`.
- **Outbox relay = polling** (`@Scheduled` 1 s, batch 100, `FOR UPDATE SKIP LOCKED`) chứ không dùng CDC/Debezium:
  thêm 1 hạ tầng nữa là over-engineer ở quy mô này. Revisit khi throughput > 500 msg/s hoặc relay trễ p99 > 5 s.
- **At-least-once, không hứa exactly-once**: mọi consumer **bắt buộc** idempotent theo `eventId` (`processed_events`),
  với `eventId = outbox.id` để retry publish không sinh event mới.
- **DLQ không auto-replay**: chỉ vào khi retry cạn; replay bằng `tools/dlq.sh` (có người quyết định, peek trước).

**Debt đã trả trong phase**: `TD-17` · `TD-25` · `TD-28` (CI bật lại) · `TD-32` · `TD-35` (gộp vào `TD-29`).
**Debt còn mở từ phase**: `TD-29`/`TD-30` (retention cho outbox/`processed_events`/`notification_logs` — **trung bình,
chặn Phase 4**) · `TD-31` (không có refund) · `TD-33` (`min_price` chưa tính SKU override) · `TD-34` (OpenAPI thiếu spec
`notification`/`search`) · `TD-36` (IT `CheckoutSagaIT` flaky theo tải) — xem §7.

### Phase 4 — Scale & performance (breakdown `SL-4.x` — **13 ticket**)

> **Mục tiêu phase**: mỗi pattern scale phải **chứng minh bằng số đo** — không thêm cache/limit/replica vì "nghe hay".
> Câu hỏi xuyên suốt: *pattern này cải thiện chỉ số nào, bao nhiêu, và trả giá gì?*
> **Điều kiện hoàn thành**: có harness đo lặp lại được · mỗi pattern có **before/after** trên cùng bộ đo · tìm được
> **điểm vỡ** của hệ · flash sale **không oversell, không sập** · báo cáo tổng hợp + runbook + tag `phase-4`.

**Chuẩn đo của phase (áp dụng cho MỌI ticket — không lặp lại trong từng AC)**

| Hạng mục | Quy ước |
|---|---|
| **Cách đo** | **Điểm vỡ (knee)**, không phải RPS tuyệt đối: ramp VU theo bậc (`50 → 100 → 200 → 400`, mỗi bậc ≥ 60 s). **Điểm vỡ** = bậc đầu tiên có `http_req_failed > 1%` **hoặc** p99 vượt ngưỡng NFR (read **150 ms**, checkout **500 ms**). Báo cáo ghi RPS đạt được **ngay trước** điểm vỡ |
| **Quy đổi về NFR** | Mọi báo cáo ghi thêm `hệ số = 50.000 RPS (NFR) ÷ RPS đo được` — vd "đạt 350 RPS ⇒ **1/143** mục tiêu NFR trên máy dev này". Không được viết "đạt 50k RPS" khi chưa đo |
| **Client** | `grafana/k6` qua Docker (không cài vào máy). Ghi lại cấu hình máy + **CPU của chính k6** — nếu k6 chạm trần CPU thì đó là điểm vỡ **của client**, phải nói rõ |
| **Dataset** | seed cố định, idempotent (`tools/load/seed.sh`): 1.000 product · 2.000 SKU · 20 category · 200 buyer · tồn kho 1.000/SKU |
| **Số liệu thu** | k6 summary (p50/p95/p99, RPS, error) · `docker stats` · `pg_stat_database` (xact_commit, blks_read) · `pg_stat_activity` · Redis `INFO` · log service |
| **Lưu raw** | `/tmp/load-<ticket>-<ts>/` + bảng markdown in ra từ `tools/load.sh` (cùng khuôn `tools/bench-async.sh` của `SL-3.8`) |
| **Báo cáo** | từng ticket dán số vào "Kết quả verify"; tổng hợp cuối phase ở `docs/reports/scale-phase-4.md` (`SL-4.12`) |

| # | Ticket | Nội dung | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 0 | `SL-4.0` | *(phát sinh)* **Retention job** `outbox` · `processed_events` · `notification_logs` (trả `TD-29`+`TD-30`) | M | ✅ Done |
| 1 | `SL-4.1` | **k6 harness + baseline**: 3 scenario · seed · `tools/load.sh` · tìm **điểm vỡ** đầu tiên | M | Todo |
| 2 | `SL-4.2` | **Cache-aside `catalog`** (TTL + jitter + **single-flight**) — trả phần cache của `TD-22` | M | Todo |
| 3 | `SL-4.3` | **Rate limit ở gateway** (token bucket Redis + Lua) — trả `TD-21` + phần limit của `TD-22` | M | Todo |
| 4 | `SL-4.4` | **Read replica** (streaming thật) + routing read/write + đo **replica lag** | M | Todo |
| 5 | `SL-4.5` | **Connection pool**: đo điểm gãy (Hikari × pgbouncer) | S | Todo |
| 6 | `SL-4.6` | **Partition `orders` theo tháng** + pruning + đo p99 list | M | Todo |
| 7 | `SL-4.7` | **Flash sale (1/2)**: atomic stock counter (Redis Lua) + pre-warm | M | Todo |
| 8 | `SL-4.8` | **Flash sale (2/2)**: degradation + backpressure + báo cáo spike | M | Todo |
| 9 | `SL-4.9` | **Keyset pagination** (thay offset) — trả `TD-6` | S | Todo |
| 10 | `SL-4.10` | **Retention `checkout_sagas` + `checkout_saga_steps`** — trả `TD-24` | S | Todo |
| 11 | `SL-4.11` | **Khử flaky theo tải** (`CheckoutSagaIT`, `inventory` IT) — trả `TD-36` + `TD-23` | S | Todo |
| 12 | `SL-4.12` | **Báo cáo scale tổng hợp + DoD Phase 4** (`docs/reports/scale-phase-4.md` · runbook · archive · tag) | S | Todo |

**Quyết định đã chốt khi breakdown** *(2026-11-11)*:

- **Đo bằng điểm vỡ + hệ số quy đổi**, không bám số tuyệt đối của NFR: máy dev (WSL2 12 thread) không tạo nổi 50k RPS; ghi
  "đạt X RPS ⇒ 1/F mục tiêu" trung thực hơn và vẫn so before/after được trên cùng bộ đo.
- **Read replica = streaming replication thật** (container replica + `primary_conninfo`), không giả lập lag: lag là thứ cần
  **đo** chứ không phải cần **giả**.
- **`orders` partition theo tháng (declarative)**, không sharding theo `user_id` — sharding đổi khoá phân giải toàn hệ, để Phase 8.
- **Flash sale tách 2 ticket** (`4.7` atomic counter, `4.8` degradation + báo cáo): "không oversell" và "không sập phần còn lại"
  là hai bài toán khác nhau, gộp lại thành ticket `L`.
- **Không** làm ở phase này: circuit breaker/retry/bulkhead (Phase 5) · metric/dashboard/alert (Phase 6) · `TD-31` (refund) ·
  `TD-32`/`TD-19` (extract shared lib) · `TD-33`/`TD-34` — ghi rõ để không phải quyết lại.

**Debt sẽ trả trong phase**: `TD-6` (`4.9`) · `TD-21` + `TD-22` (`4.3`/`4.2`) · `TD-23` + `TD-36` (`4.11`) · `TD-24` (`4.10`).
**Debt còn mở sau phase (dự kiến)**: `TD-1`, `TD-2`, `TD-5`, `TD-8`–`TD-11`, `TD-13`, `TD-16`–`TD-20`, `TD-26`, `TD-31`–`TD-34` — §7.

### SL-4.0 — Retention job cho `outbox` · `processed_events` · `notification_logs` (trả `TD-29` + `TD-30`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-11) |
| **Phase** | 4 — Scale & performance *(phát sinh: trả nợ Phase 3 trước khi load test)* |
| **Phụ thuộc** | Phase 3 `Done` (`SL-3.3` outbox · `SL-3.2` dedupe · `SL-3.4` notification) |
| **Ước lượng** | M |

**Bối cảnh**

Ba bảng phụ tăng **tuyến tính theo traffic** và hiện **không có job dọn**:

| Bảng | Ở đâu | Tăng khi nào | Nguồn nợ |
|---|---|---|---|
| `outbox` | DB `order` · `payment` · `catalog` | 1 row/event | `TD-29` (sau 1 buổi đo `SL-3.8`: 715 / 380 / 359 row) |
| `processed_events` | DB của **mỗi consumer** (`order` · `notification` · `search`) | 1 row/event/consumer, **không xoá được vì dedupe cần** | `TD-30` |
| `notification_logs` | DB `notification` | 1 row/đơn đã confirm | `TD-30` |

Điều kiện revisit của cả hai là **"trước load test Phase 4"** — load test sinh đúng loại traffic làm các bảng này phình, nên
dọn sau khi đo là dọn trong lúc bảng đã to. Không có job retention thì kết quả load test bị nhiễu bởi bảng lớn dần trong lúc
đo, và `VACUUM`/backup phải làm việc với dữ liệu chết.

**Mục tiêu**

1. Mỗi bảng phụ có **job dọn theo tuổi**, xoá **theo batch** (transaction ngắn), cấu hình bằng env.
2. Không bao giờ xoá dữ liệu còn cần: **`outbox` chỉ xoá `SENT`** (không chạm `PENDING`), `processed_events`/`notification_logs`
   chỉ xoá theo tuổi.
3. Tài liệu hoá **ràng buộc thứ tự retention** (dedupe phải sống ≥ outbox) — nếu sai, replay DLQ sau này gửi trùng.

**In scope**

- `packages/service-commons`
  - `ProcessedEventStore.deleteProcessedBefore(cutoff, batch)` + impl JDBC (batch `DELETE … LIMIT`).
  - `EventProperties`: **nested `dedup`** (`auto-create-schema` · `retention` · `cleanup-batch-size`) — kèm **fix bug**:
    3 service đang khai `shoplab.events.dedup.auto-create-schema: false` nhưng khoá này **không bind** (property đang flat)
    ⇒ DDL auto-create vẫn chạy. Sửa để `false` có tác dụng thật.
  - `ProcessedEventRetention` (bean, có `sweep()` + log) — **không** tự `@Scheduled` trong commons.
- 5 service: mỗi service **một** `RetentionSweeper` (`@Scheduled(fixedDelay)`, `try/catch`, log số row) gọi các cleaner
  áp dụng cho DB của nó:
  - `order`: outbox + `processed_events` · `catalog`/`payment`: outbox
  - `notification`: `notification_logs` + `processed_events` · `search`: `processed_events`
- Repository mới: `OutboxRepository.deleteSentOlderThan(cutoff, batch)` (×3 service),
  `NotificationLogRepository.deleteOlderThan(cutoff, batch)`.
- Default retention: **outbox 7 ngày** · **`processed_events` 7 ngày** · **`notification_logs` 90 ngày** (override bằng env).
- `.env.example` + `README` (bảng config) + `architecture.md` (§6.x + nơi mô tả retention).
- `runbook-phase-3.md` §3: thêm mục kiểm retention (SQL đếm row theo tuổi + kiểm job có chạy trong log).

**Out of scope**

- `checkout_sagas` / `checkout_saga_steps` (`TD-24`) — cùng họ nhưng DB/ngữ nghĩa khác, ticket riêng.
- Refund (`TD-31`) · gộp outbox vào commons (`TD-32`) · flaky test theo tải (`TD-36`).
- Job runner ngoài app (cron/K8s CronJob) — Phase 7. Ở phase này job in-process là đủ (giống sweeper đã có).
- Partition theo tháng cho các bảng này — `SL-4.x` partition (`orders` trước).

**Acceptance Criteria**

- [ ] `outbox`: row `SENT` cũ hơn `retention` bị xoá; row **`PENDING`** và row `SENT` **mới hơn** mặc định **còn nguyên**
      (assert ở IT với dữ liệu backdate).
- [ ] `processed_events` + `notification_logs`: chỉ xoá row cũ hơn ngưỡng, giữ row mới — IT có cả 2 loại row.
- [ ] Job chạy được trên stack compose: chèn row backdate → bật interval ngắn → log in số row đã xoá, số row đúng kỳ vọng.
- [ ] `shoplab.events.dedup.auto-create-schema: false` **thật sự có tác dụng** (test bind: `false` ⇒ không gọi `initSchema`).
- [ ] Default retention ghi ở **một** chỗ (properties) + override được bằng env; `.env.example`/`README` có tên biến.
- [ ] `mvn -B -ntp verify` xanh (unit + IT mới) · `Backlog.md` §7 đánh dấu `TD-29`/`TD-30` ✅ đã trả.

**Ghi chú kỹ thuật**

- **Khuôn**: `IdempotencySweeper` (đã trả `TD-14`) — `@Scheduled(fixedDelayString)`, tham số từ env, bắt `RuntimeException`
  để một nhịp lỗi không giết scheduler, log **số row đã xoá** (không có metric thì log là chỗ duy nhất biết job sống).
- **Batch delete** (`DELETE … WHERE <pk> IN (SELECT <pk> … LIMIT n)`): transaction ngắn, không khoá bảng lâu, không tạo WAL
  khổng lồ. Xoá **một batch mỗi nhịp** (không loop tới hết) để job không chiếm DB khi bảng lớn.
- **Ràng buộc thứ tự**: `processed_events` retention **≥** `outbox` retention. Nếu marker dedupe hết hạn trước khi row outbox
  bị xoá thì replay DLQ sau đó sẽ **xử lý lại** (gửi email 2 lần). Mặc định 7d/7d thoả; đổi phải giữ bất biến này.
- **Không xoá `PENDING`**: đó là event **chưa gửi**; xoá = mất event thật.
- **Timezone**: mọi cột là `timestamptz`, so sánh với `Instant` (UTC) — không dùng `now()` trong SQL để test backdate được.
- **Index**: `outbox` đã có `idx_outbox_pending (status, created_at) WHERE status='PENDING'`; job xoá theo `sent_at` nên cần
  cân nhắc index `(status, sent_at)` nếu bảng lớn — đo trước, chưa thêm ở ticket này.

**Kết quả verify** *(2026-11-11)*

**1) Unit test mới — 5 sweeper + retention của commons + fix bind `dedup`:**

```text
$ mvn -B -ntp -pl services/{order,catalog,payment,notification,search}-service -am \
      -Dtest='RetentionSweeperTest,ProcessedEventRetentionTest,EventAutoConfigurationTest' test
com.shoplab.commons.events.ProcessedEventRetentionTest    Tests run: 4, Failures: 0
com.shoplab.commons.events.EventAutoConfigurationTest     Tests run: 7, Failures: 0   (+2 mới: bind dedup, bean retention)
com.shoplab.order.retention.RetentionSweeperTest          Tests run: 5, Failures: 0
com.shoplab.catalog.retention.RetentionSweeperTest        Tests run: 3, Failures: 0
com.shoplab.payment.retention.RetentionSweeperTest        Tests run: 3, Failures: 0
com.shoplab.notification.retention.RetentionSweeperTest   Tests run: 3, Failures: 0
com.shoplab.search.retention.RetentionSweeperTest         Tests run: 2, Failures: 0
```

**2) IT Postgres thật — chỉ xoá row đúng tuổi, `PENDING`/row mới/`sent_at NULL` còn nguyên:**

```text
OrderServiceApplicationIT.retentionDeletesOnlyOldSentRows        PASS
  outbox:   SENT 10 ngày → xoá · SENT 1 giờ → giữ · PENDING 10 ngày → giữ
  processed_events: 10 ngày → xoá · 1 giờ → giữ
NotificationServiceApplicationIT.retentionDeletesOnlyOldSentLogs PASS
  notification_logs: SENT 120 ngày → xoá · SENT 1 ngày → giữ · FAILED(sent_at NULL) 120 ngày → giữ
  processed_events: 10 ngày → xoá · 2 giờ → giữ
```

**3) Chạy thật trên stack compose** (`up -d --build`, `SHOPLAB_RETENTION_SWEEP_INTERVAL=15s`,
`SHOPLAB_RETENTION_SWEEP_INITIAL_DELAY=20s` ⇒ chứng minh luôn **env → placeholder `@Scheduled` bind được**):

```console
# chèn row backdate rồi chờ 35 s
  order/catalog/payment outbox : oldSENT=0  newSENT=1  oldPENDING=1     ← PENDING KHÔNG bị xoá
  order/notification/search processed_events : old=0  new=1
  notification_logs            : oldSENT=0  newSENT=1  oldNoSentAt=1     ← gửi hỏng phải giữ

$ docker logs shoplab-order-service | grep 'retention: đã xoá'
  outbox retention: đã xoá 1 row SENT cũ hơn 2026-11-04T16:07:28Z (giữ PT168H · batch 1000)
  processed_events retention: đã xoá 1 row cũ hơn 2026-11-04T16:07:28Z (giữ PT168H · batch 1000)
$ docker logs shoplab-notification-service | grep 'retention: đã xoá'
  notification_logs retention: đã xoá 1 row cũ hơn 2026-08-13T16:07:27Z (giữ PT2160H · batch 1000)  ← 90d
  processed_events retention: đã xoá 1 row cũ hơn 2026-11-04T16:07:27Z (giữ PT168H)
$ docker logs shoplab-catalog-service|shoplab-payment-service|shoplab-search-service | grep retention
  … đã xoá 1 row … (PT168H)        ← mỗi service đúng phần bảng của nó
```

> **Bẫy gặp ngay trong drill**: row `PENDING` giả chèn vào `outbox` bị **relay publish** ngay (đó là việc của relay) rồi được đánh `SENT` với `sent_at = now()` ⇒ nhìn `group by status` sẽ **không còn `PENDING`**. Đừng đọc thành "retention xoá PENDING": assert đúng phải là *PENDING không bị xoá* **và** *PENDING vẫn là PENDING* — điều thứ hai chỉ kiểm được khi tắt nhịp nền của relay (`shoplab.outbox.interval=1h` trong IT; IT order giờ assert cả `status = 'PENDING'`).

**4) Không regress luồng async** (retention không chạm `PENDING`, không chạm marker mới):

```text
bash tools/e2e-async.sh   → 48 PASS / 0 FAIL · 38 request · 35 590 ms
bash tools/e2e-checkout.sh→ 27 PASS / 0 FAIL · 14 request ·  1 096 ms
bash tools/dlq.sh list    → q.notification.dlq 0 · q.order.dlq 0 · q.search.dlq 0
```

**5) Toàn repo:** `mvn -B -ntp verify` → **BUILD SUCCESS** · **829 test** (684 unit + 145 IT · trước ticket: 805).

**6) Bug phát hiện thêm (đã fix trong ticket):** `shoplab.events.dedup.auto-create-schema: false` ở **3 service**
(`catalog`, `notification`, `search`) là **no-op** — property là field flat (`shoplab.events.auto-create-schema`) nên khoá
YAML lồng nhau **không bind**, DDL auto-create vẫn chạy. Bằng chứng: `payment` (không khai gì) và `catalog` (khai `false`)
đều có bảng `processed_events`. Đã chuyển thành object lồng `dedup` + test bind.

**Learning note**

- **Nợ "không có job dọn" không chỉ là chuyện dung lượng — nó là chuyện `VACUUM`/backup/độ nhiễu của load test.** Job
  retention không làm hệ nhanh hơn hôm nay, nhưng nếu để tới Phase 4 thì mọi số đo load test đều rơi vào một DB đang lớn
  dần **trong lúc đo** — tự làm nhiễu chính bài đo.
- **Thứ tự retention là một bất biến hệ thống, không phải tham số tuỳ ý**: `processed_events` phải sống **≥** `outbox`.
  Nếu dedupe hết hạn trước, replay DLQ muộn sẽ gửi email lần 2 — mà triệu chứng lại xuất hiện ở **notification**, cách
  chỗ gây lỗi (cấu hình retention của outbox) 2 service. Đây là loại lỗi phải chặn bằng **tài liệu + mặc định an toàn**,
  không chặn được bằng code của một service.
- **Điều kiện "không xoá nhầm" mới là phần khó, không phải câu DELETE**: `outbox` chỉ được xoá `SENT` (PENDING = event
  **chưa gửi**), `notification_logs` chỉ xoá row **đã gửi** (`sent_at IS NOT NULL` — row `FAILED` là dấu vết lần gửi hỏng,
  phải giữ). Cả 3 điều kiện này đều được assert bằng IT với dữ liệu backdate, không tin vào "SQL nhìn đúng".
- **Sweeper trong lib vs trong service** (quyết định kiến trúc của ticket): SQL + ngữ nghĩa để ở `commons` (một chỗ),
  nhưng **nhịp chạy** để ở service. Cái giá là 5 class mỏng ~40 dòng; cái được là **không có scheduler ẩn** — đọc code
  service thấy ngay job nào chạy ở đâu, và tắt job ở một service không kéo theo service khác.
- **Một bug im lặng tìm thấy nhờ đụng đúng vùng cấu hình**: 3 service khai `shoplab.events.dedup.auto-create-schema: false`
  suốt từ `SL-3.2` mà **chưa bao giờ có tác dụng** (property flat ≠ khoá YAML lồng nhau). Không có gì đỏ, không log, chỉ
  là "DDL vẫn chạy dù mình bảo đừng". Bài học: **khoá YAML chỉ đúng khi có test bind**, và cách chắc nhất là dựng object
  lồng nhau đúng hình dạng khoá.
- **Compose phải khai env mới thì mới set được**: thêm property mà quên `infra/docker-compose.yml` ⇒ knob chỉ dùng được
  khi chạy jar ngoài Docker. Đã thêm cho cả 5 service, kèm drill chứng minh `SHOPLAB_RETENTION_SWEEP_INTERVAL=15s` có tác
  dụng thật (log `giữ PT168H` xuất hiện sau ~35 s thay vì 1 h).
- **Index phải đi cùng job**: `DELETE … WHERE status='SENT' AND sent_at < ?` không có index là **seq scan mỗi nhịp** ⇒ job
  dọn trở thành nguồn tải. Thêm partial index `idx_outbox_sent (sent_at) WHERE status='SENT'` ngay trong ticket này thay
  vì để "khi nào chậm thì tính" (đó chính là cách nợ `TD-29` sinh ra).



### SL-4.1 — k6 harness + baseline (tìm điểm vỡ đầu tiên)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | — (ticket đầu tiên; `SL-4.0` xong để bảng phụ không phình trong lúc đo) |
| **Ước lượng** | M |

**Bối cảnh**

Phase 3 dạy cách đo **một luồng** (`tools/bench-async.sh`), nhưng Phase 4 cần đo **tải tổng**: nhiều client đồng thời, nhiều
endpoint, kéo dài vài phút, và phải tìm được **điểm vỡ**. `bench-async.sh` đo latency tuần tự/20 song song, không ramp tải,
không có scenario đọc catalog (thứ chiếm 20:1 traffic theo `architecture.md` §3).

**Mục tiêu**

1. Harness k6 **lặp lại được** cho 3 scenario: đọc catalog (read-heavy), search, checkout.
2. Bộ **seed dữ liệu** cố định, idempotent (1.000 product · 2.000 SKU · 20 category · 200 buyer).
3. **Baseline**: p50/p95/p99 · RPS · error rate theo từng bậc tải + **điểm vỡ** khi **chưa** tối ưu gì.

**In scope**

- `tools/load/` — k6 scripts: `read-catalog.js` (list + detail + `GET /v1/skus?ids=`), `read-search.js`, `checkout.js`.
- `tools/load/seed.sh` — seed idempotent (prefix cố định, chạy lại không tạo trùng) + in ID cho scenario.
- `tools/load.sh` — chạy 1 scenario theo **bậc VU** (mặc định `50,100,200,400`, mỗi bậc 60 s), in **bảng markdown**
  (VU · RPS · p50 · p95 · p99 · error%), dừng khi qua điểm vỡ, lưu raw `/tmp/load-<scenario>-<ts>/`.
- `docs/reports/scale-phase-4-baseline.md` — baseline 3 scenario + điểm vỡ + hệ số quy đổi NFR + "nghẽn ở đâu".

**Out of scope**

- Không sửa code service (baseline phải là hệ **hiện tại**, chưa cache/limit/replica).
- Không dựng Grafana/Prometheus (Phase 6) · không test flash sale (đó là `SL-4.8`).

**Acceptance Criteria**

- [ ] `bash tools/load/seed.sh` chạy 2 lần: lần 2 **không** tạo thêm row, in cùng bộ ID.
- [ ] `bash tools/load.sh --scenario read-catalog` chạy từ máy sạch (chỉ cần Docker + stack up), in bảng markdown theo bậc VU,
      raw lưu `/tmp/load-read-catalog-<ts>/`.
- [ ] Có **điểm vỡ** cho `read-catalog` và `checkout`: bậc VU + RPS **ngay trước** điểm vỡ + lý do (p99 vượt ngưỡng hay
      error > 1%) + **hệ số quy đổi NFR**.
- [ ] Script **dừng đúng** khi qua điểm vỡ và cảnh báo nếu **k6 chạm trần CPU** (điểm vỡ của client).
- [ ] `docs/reports/scale-phase-4-baseline.md` có 3 bảng + kết luận "nghẽn đầu tiên ở đâu" kèm bằng chứng.

**Ghi chú kỹ thuật**

- **k6 qua Docker**: `docker run --network shoplab-net -v $PWD/tools/load:/scripts` ⇒ gọi `http://api-gateway:8080`.
- **Ramp theo bậc, không ramp mượt**: bậc rõ mới trả lời được "vỡ ở đâu"; mỗi bậc ≥ 60 s để p99 ổn định.
- **`ramping-vus`, không `constant-arrival-rate`**: mục tiêu là "bao nhiêu **client đồng thời**"; RPS là hệ quả.
- **Checkout cần state riêng**: pool 200 buyer, VU xoay vòng; `Idempotency-Key` = `<runId>-<vu>-<iter>` (bài học `SL-3.8`).
- **Đo trước, tối ưu sau**: baseline không được chứa thay đổi của `SL-4.2`+.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.2 — Cache-aside cho `catalog` (TTL + jitter + single-flight)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (phải có baseline để so) |
| **Ước lượng** | M |

**Bối cảnh**

`catalog-service` bị đọc nhiều nhất (read:write ≈ 20:1) và hiện **không có cache**: mỗi `GET /v1/products`,
`GET /v1/products/{id}`, `GET /v1/skus?ids=` là một (hoặc nhiều) query Postgres. Redis đã có trong hệ (cart dùng) nhưng chưa
dùng cho catalog.

**Mục tiêu**

1. Giảm tải đọc DB + **hạ p99 read** bằng cache-aside, có **jitter** để TTL không hết hạn đồng loạt.
2. Chống **cache stampede** bằng single-flight.
3. Cache **không bao giờ** làm sai dữ liệu: ghi product ⇒ key liên quan bị vô hiệu; Redis chết ⇒ vẫn phục vụ (fallback DB).

**In scope**

- `catalog-service`: cache-aside cho `GET /v1/products` (list), `GET /v1/products/{id}`, `GET /v1/skus?ids=` (trả phần cache
  của `TD-22`), `GET /v1/categories`.
- Key schema + TTL cấu hình được (`SHOPLAB_CATALOG_CACHE_TTL=60s`, `..._TTL_JITTER_PERCENT=10`) + **version key** `catalog:v1:*`.
- **Single-flight**: `SET <key>:lock <token> NX PX 2000`; request thua lock chờ ngắn rồi đọc lại cache/DB. Log
  `cache=hit|miss|stale`.
- **Invalidation** ngay sau ghi: create/update/archive product → xoá key detail/list + `skus?ids=`.
- Đo lại `read-catalog` cùng bậc VU như baseline + báo cáo before/after.

**Out of scope**

- Không cache `search-service` (index đã là bản sao), không cache `cart` (Redis vốn là store), không cache dữ liệu tiền.
- Không làm **cache warming** (đó là `SL-4.7`), không CDN/HTTP cache.

**Acceptance Criteria**

- [ ] Đo lại `read-catalog` ở **cùng** bậc VU: p99 giảm **≥ 30%** **hoặc** điểm vỡ lùi **≥ 1 bậc** (ghi cả hai nếu đạt); không
      đạt thì báo cáo phải giải thích **bằng số** (nghẽn chuyển sang đâu).
- [ ] Hit ratio ≥ **80%** ở scenario read (log `cache=hit/miss` hoặc header debug).
- [ ] **Stampede test**: xoá key hot rồi bắn **200 request đồng thời** ⇒ query DB cho key đó **≤ 2** (delta
      `pg_stat_database.xact_commit` hoặc log `db-load`).
- [ ] **Invalidation**: update product ⇒ `GET /v1/products/{id}` + list trả **dữ liệu mới** trong ≤ 1 s (poll).
- [ ] **Fail-open**: `docker stop shoplab-redis` ⇒ endpoint catalog vẫn `200` (chậm hơn, log WARN), không 500.
- [ ] `mvn verify` xanh (unit test key/TTL/jitter/single-flight) + `e2e-smoke`/`e2e-checkout` không regress.

**Ghi chú kỹ thuật**

- **Cache-aside**, không read-through: quyết định đọc cache ở service ⇒ dễ debug hơn interceptor "tự động".
- **Jitter**: `TTL = base × (1 ± jitter%)` bằng `ThreadLocalRandom` — mục tiêu là **phá đồng pha**.
- **Single-flight bằng lock Redis, không `synchronized`**: nhiều instance thì lock JVM vô nghĩa. Đánh đổi: request thua lock
  chờ ~50–100 ms (đo được) — chấp nhận, vì "tất cả cùng đập DB" tệ hơn.
- **Invalidation theo version key**: xoá từng key dễ sai (list có filter/sort) ⇒ update 1 product xoá key cụ thể; hàng loạt
  thì bump `catalog:v1`.
- **Hit ratio phải có nguồn đo**: header `X-Cache` chỉ bật khi `SHOPLAB_DEBUG_ENDPOINTS=true` (khuôn có ở payment `/debug/faults`).

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.3 — Rate limit ở gateway (token bucket Redis + Lua)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (đo được tải mới chứng minh được tác dụng) |
| **Ước lượng** | M |

**Bối cảnh**

Gateway hiện **không giới hạn gì**: `/v1/carts/**` (public) và `GET /v1/skus?ids=` (public, 100 id/request) có thể bị một
client dội tải — `TD-21`, `TD-22`. Đây cũng là mảnh bắt buộc của flash sale (`architecture.md` §6.3): phần lớn request phải bị
chặn **ở biên** để service bên trong chỉ nhận phần tải nó chịu được.

**Mục tiêu**

1. Chặn tải theo **user** (JWT `sub`) và theo **IP** (request chưa đăng nhập), cấu hình được theo route.
2. Trả `429` **đúng chuẩn**: JSON `ErrorCode`, `Retry-After`, `X-RateLimit-*`.
3. Không biến gateway thành **single point of failure**: Redis chết ⇒ **fail-open** + log WARN (thà nhận tải còn hơn sập).

**In scope**

- `api-gateway`: filter token bucket dùng **Redis + Lua** (atomic: đọc + trừ trong 1 `EVAL`).
- Cấu hình theo route (`application.yml`, override env): `default: 100 rps/burst 200` · `POST /v1/checkout: 10/20` ·
  `POST /v1/auth/login: 5/10`.
- Bypass `/health` `/ready` `/version` `/v3/api-docs` `/mock-provider/**`.
- 429 body theo `ApiError` hiện có + `Retry-After` + `X-RateLimit-Limit|Remaining|Reset`.
- Test: k6 500 VU không token đánh public read; k6 user hợp lệ dưới ngưỡng ⇒ **0** 429; stop Redis ⇒ fail-open.
- Trả `TD-21` + phần limit của `TD-22`.

**Out of scope**

- Không rate limit **bên trong** service (chỉ ở biên) — bulkhead/backpressure nội bộ thuộc Phase 5.
- Không quota theo ngày/tháng (billing), không allowlist theo API key thật.

**Acceptance Criteria**

- [ ] k6 500 VU (không token) vào `GET /v1/products` ⇒ **≥ 90%** là `429` và `pg_stat_database.xact_commit` **không** tăng
      tương ứng (chặn ở biên, không phải ở DB).
- [ ] User hợp lệ gửi **dưới ngưỡng** ⇒ **0** `429` (không chặn oan).
- [ ] 429 có đủ `Retry-After` + `X-RateLimit-*`; body parse được thành `ApiError` (có `code`, `traceId`).
- [ ] **Fail-open**: `docker stop shoplab-redis` ⇒ request vẫn `200` + log WARN `rate-limit: redis lỗi, fail-open`.
- [ ] Hai user khác nhau **không** dùng chung bucket (bắn user 1 tới ngưỡng ⇒ user 2 bình thường).
- [ ] `mvn verify` xanh (unit test thuật toán + filter) · `e2e-checkout` 27 không regress · `TD-21`/`TD-22` cập nhật §7.

**Ghi chú kỹ thuật**

- **Token bucket trong Lua** (`EVAL`): `INCR` + `PEXPIRE` kiểu "fixed window" cho phép burst gấp đôi ở ranh giới cửa sổ;
  token bucket cho **burst có kiểm soát** — đúng thứ cần cho flash sale.
- **Key**: `rl:<route>:u:<sub>` khi có JWT, `rl:<route>:ip:<ip>` khi không. Lấy IP **sau** `X-Forwarded-For` hợp lệ — không tin
  header client tự đặt.
- **Fail-open có chủ ý**: rate limit là **bảo vệ**, không phải chức năng; Redis chết mà chặn hết traffic là tự bắn vào chân.
  Đánh đổi này phải ghi trong runbook.
- **Không log mỗi request bị chặn** ở INFO (500 VU sẽ ngập log); log WARN theo cửa sổ.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.4 — Read replica cho `catalog` + `order` (streaming thật) + đo lag

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (baseline) · độc lập với `SL-4.2`/`4.3` |
| **Ước lượng** | M |

**Bối cảnh**

Toàn bộ read và write đang dùng **cùng một** Postgres. Với read:write ≈ 20:1, cách tách phổ biến nhất là **read replica**:
write vào primary, read vào replica. Cái giá là **replica lag** — và lag phải **đo được**, vì nó biến thành "khách vừa sửa
product mà mở lại vẫn thấy giá cũ" nếu routing sai chỗ.

**Mục tiêu**

1. Có **replica thật** (streaming replication) trong compose, `hot_standby=on`.
2. `catalog-service` + `order-service` **route** read → replica, write → primary, tắt được bằng env.
3. Đo **lag** dưới tải + chứng minh chỗ **buộc** đọc primary vẫn đọc primary.

**In scope**

- `infra/docker-compose.yml`: thêm `postgres-replica` (volume riêng, `pg_basebackup -R` khi khởi tạo + `primary_conninfo`),
  healthcheck `pg_isready` + kiểm `pg_is_in_recovery()`.
- 2 datasource: primary (write) + `shoplab.datasource.read.*` (replica, read-only); `SHOPLAB_READ_REPLICA_ENABLED=false` ⇒ đọc
  về primary (công tắc sự cố).
- Routing: `catalog` — `GET` list/detail/category → replica; `order` — `GET /v1/orders/**` → replica; **mọi** đường ghi +
  `GET /v1/skus?ids=` → **primary**.
- Đo lag: `pg_last_wal_replay_lsn()` vs `pg_current_wal_lsn()`, endpoint debug `GET /debug/replication` + log khi lag > ngưỡng.
- Test read-your-write: update product → `GET` ngay có thể **cũ** (ghi rõ ngưỡng), checkout **luôn** thấy giá mới.

**Out of scope**

- Không logical replication/CDC (Phase 8), không multi-primary, không failover tự động (Phase 7).
- Không route `inventory`/`promotion` sang replica (tồn kho/khuyến mãi phải tuyệt đối tươi).
- Không làm connection pooler ở ticket này (`SL-4.5`).

**Acceptance Criteria**

- [ ] `docker compose up` có **15 container**; replica `pg_is_in_recovery()=t`; insert vào primary ⇒ thấy ở replica ≤ 1 s khi idle.
- [ ] Chứng minh **routing thật** (`pg_stat_activity` hoặc `log_statement=all`): `GET /v1/products` ở **replica**;
      `POST /v1/products` + `GET /v1/skus?ids=` ở **primary**.
- [ ] **Đo lag dưới tải**: `checkout` + `read-catalog` đồng thời ≥ 100 RPS trong 60 s ⇒ p50/p95/**max** lag; nêu ngưỡng mà từ đó
      "đọc replica không còn an toàn cho màn hình X".
- [ ] **Read-your-write**: update giá rồi `GET` 20 lần ⇒ ghi rõ bao nhiêu lần thấy giá cũ, sau bao lâu luôn thấy mới; checkout
      dùng giá mới ngay request đầu (0 sai lệch).
- [ ] `SHOPLAB_READ_REPLICA_ENABLED=false` ⇒ chạy như cũ + e2e xanh (chứng minh **tắt được** khi replica sự cố).
- [ ] p99 `read-catalog` **không tệ hơn** baseline (nếu tệ: giải thích bằng số, vd thêm 1 hop mạng).

**Ghi chú kỹ thuật**

- **Streaming (physical)**: `pg_basebackup -R` sinh `standby.signal` + `primary_conninfo`; cần `wal_level=replica` (mặc định
  PG 16) + `max_wal_senders` đủ.
- **Read-only phải thật**: `default_transaction_read_only=on` hoặc user chỉ `SELECT` — lỡ ghi vào replica thì phải **fail**.
- **Lag là đánh đổi, không phải bug**: list product chấp nhận cũ 1–2 s; giá trong checkout thì không. Bảng ranh giới này ghi
  vào `runbook-phase-4.md` (`SL-4.12`).
- **Đừng route theo "GET = read"**: `GET /v1/skus?ids=` là GET nhưng dùng cho **quyết định giá** — đúng cái bẫy của pattern này.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.5 — Connection pool: đo điểm gãy (Hikari × pgbouncer)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (k6 + baseline) · `SL-4.4` (đo trên cả primary/replica) |
| **Ước lượng** | S |

**Bối cảnh**

Mỗi service có Hikari pool riêng + Postgres `max_connections=100`. Khi tải tăng, câu hỏi thực tế không phải "pool to là tốt"
mà là **pool bao nhiêu thì hàng đợi xin connection bắt đầu ăn vào p99** — và pgbouncer có cứu được không.

**Mục tiêu**

Có **bảng số** trả lời: (a) điểm gãy ở mức tải nào với pool 10/20/50; (b) pgbouncer (transaction pooling) cải thiện bao nhiêu;
(c) khuyến nghị cấu hình + lý do.

**In scope**

- Ma trận đo: `hikari.maximum-pool-size ∈ {10, 20, 50}` × bậc tải `SL-4.1` cho `read-catalog` + `checkout`.
- Thu: p99, throughput, connection thực tế trên Postgres, `pg_stat_activity` (`active`/`idle`/`waiting`).
- Thử **pgbouncer** (container, transaction mode); đo lại với pool/service nhỏ (vd 5) để thấy tác dụng.
- Kết luận ghi vào `docs/reports/scale-phase-4-baseline.md` mục "connection pool".

**Out of scope**

- Không đổi kiến trúc kết nối (vẫn JDBC/Hikari), không R2DBC.
- Không tuning Postgres (`shared_buffers`/`work_mem`) — bài học pool, không phải tuning DB.

**Acceptance Criteria**

- [ ] Bảng 3×2 (pool × scenario) có p99 + throughput; ghi bậc tải mà p99 bắt đầu phình.
- [ ] Xác định **pool size tối thiểu** mà p99 không tệ hơn baseline > 10% (ghi số).
- [ ] Có số của pgbouncer ở cùng tải với pool/service nhỏ (p99 + throughput so với không dùng).
- [ ] Khuyến nghị cấu hình cuối (số cụ thể + lý do) vào `.env.example`/`README` nếu đổi default.
- [ ] Không regress: `e2e-checkout` + `e2e-async` xanh với cấu hình mới.

**Ghi chú kỹ thuật**

- "Điểm gãy của pool" = p99 phình + connection `active` chạm trần **trong khi CPU Postgres chưa bão hoà** ⇒ nghẽn ở hàng đợi xin
  connection, không phải ở query.
- pgbouncer **transaction mode** + JDBC: phải để **`prepareThreshold=0`**, không thì prepared statement session-scoped bị dùng lại
  sai kết nối — **bẫy thật** phải ghi lại.
- Tổng connection = Σ pool các service + replica ⇒ so với `max_connections` trước khi tăng pool, kẻo đổi "nghẽn pool" thành
  "nghẽn `too many clients`".

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.6 — Partition `orders` theo tháng (declarative) + đo p99 list

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (đo list paging) · đi cùng `SL-4.9` rất hợp |
| **Ước lượng** | M |

**Bối cảnh**

`orders` + `order_items` tăng mãi theo thời gian, trong khi 99% truy vấn chỉ cần **đơn gần đây**. Partition theo tháng cắt vấn
đề theo **thời gian** — nhưng chỉ có lợi nếu query có **partition pruning**; nếu không, mọi query quét **tất cả** partition
(tệ hơn trước).

**Mục tiêu**

1. `orders` thành bảng **partition theo tháng** (`RANGE (created_at)`), dữ liệu cũ migrate xong, hành vi đọc/ghi không đổi.
2. Chứng minh **pruning** bằng `EXPLAIN` + đo p99 list ở độ sâu lớn trước/sau.
3. Có cơ chế **tạo partition kế tiếp** — không để "tháng sau hệ chết vì thiếu partition".

**In scope**

- Migration: `orders` partitioned (parent) + copy dữ liệu + đổi tên; giữ PK/FK/constraint (`order_items` FK → parent); partition
  `orders_YYYY_MM` cho 3 tháng gần nhất + tháng hiện tại + 1 tháng tới.
- Job tạo partition tháng kế tiếp (`@Scheduled` hoặc migration theo tháng — chọn + ghi lý do).
- Đảm bảo query list có `created_at` (hoặc `id`+`created_at`) trong WHERE để pruning hoạt động.
- Đo trước/sau: `EXPLAIN (ANALYZE, BUFFERS)` list sâu + p99 `GET /v1/orders` ở bậc tải `SL-4.1`.
- Cập nhật `architecture.md` §8 schema + ghi chú vận hành.

**Out of scope**

- Không sharding theo `user_id` (Phase 8) · không partition `order_items` (truy vấn theo `order_id`) — ghi rõ là **quyết định**.
- Không retention/xoá đơn cũ (bài toán nghiệp vụ, không phải scale).

**Acceptance Criteria**

- [ ] `orders` là partitioned table: `relkind='p'` + ≥ 6 partition (dán `\d+ orders`).
- [ ] Dữ liệu migrate đủ: số row trước = sau, `sum(total)` trước = sau (assert bằng SQL).
- [ ] `EXPLAIN` list sâu cho thấy **chỉ 1–2 partition** được đọc (dán output) — quét tất cả ⇒ ticket **chưa xong**.
- [ ] p99 `GET /v1/orders` (list sâu ≥ 10k đơn) **giảm ≥ 30%** hoặc ghi rõ "không giảm vì ..." kèm số.
- [ ] Tháng kế tiếp: chạy job/migration tạo partition ⇒ insert đơn mới thành công (không "no partition of relation").
- [ ] `e2e-checkout` 27 + `e2e-async` 48 xanh sau migrate.

**Ghi chú kỹ thuật**

- **FK tới bảng partitioned**: `order_items.order_id` → `orders(id)` chỉ hợp lệ nếu `orders` khai **PK chứa cả cột partition**
  (`PRIMARY KEY (id, created_at)`) — ràng buộc của Postgres, phải nói trước chứ không phát hiện giữa đường.
- **Không dùng trigger** route insert: declarative partitioning tự route theo `created_at`.
- **Query không có `created_at` sẽ quét mọi partition** ⇒ rà lại repository/report; đây là chỗ dễ regression hơn cả migration.
- Migration trên volume dev có thể lock bảng — chấp nhận downtime local; cách làm ở môi trường thật (create new + backfill +
  swap) là **ngoài scope**.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.7 — Flash sale (1/2): atomic stock counter (Redis Lua) + pre-warm

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.3` (rate limit chặn phần lớn tải ở biên) · `SL-4.1` (đo) |
| **Ước lượng** | M |

**Bối cảnh**

`architecture.md` §6.3: 1 SKU / 10.000 sản phẩm / hàng trăm nghìn người bấm mua. Hiện `inventory-service` reserve bằng
**conditional `UPDATE`** trên Postgres — đúng và chống oversell, nhưng **mọi** request đều chạm DB. Với 10k stock và 100k
request, DB nhận 100k update để bán được 10k: đúng nhưng phí, và p99 của **luồng khác** bị kéo theo.

**Mục tiêu**

1. Chặn phần lớn request **trước khi** chạm DB bằng **atomic counter trên Redis** (`DECRBY` trong Lua) ⇒ chỉ ~stock request đi
   tiếp vào DB.
2. **Không oversell**: DB vẫn là nguồn sự thật cuối; counter chỉ là "cửa hẹp", phải **reconcile** được khi lệch.
3. **Pre-warm**: nạp stock vào Redis trước giờ mở bán (không để request đầu tiên tự làm việc đó).

**In scope**

- `inventory-service`: `POST /v1/admin/flash-sale/{skuId}/warm` (admin) nạp `flash:stock:<skuId>` = `on_hand - reserved` + bật
  cờ `flash:<skuId>:active` (TTL).
- Reserve path: SKU đang flash-sale ⇒ Lua (`if stock >= qty then stock-qty; return 1 else return 0`) rồi mới gọi reserve DB;
  hết counter ⇒ `409` nhanh, **không chạm DB**.
- Compensate: bước sau fail (payment/saga) ⇒ **trả counter** (`INCRBY`) cùng chỗ compensating hiện có, idempotent theo `orderId`.
- Reconcile: job so `flash:stock + sold` với DB; lệch ⇒ log ERROR + sửa theo **DB thắng**.
- Đo: k6 flash-sale (1 SKU, N buyer, 60 s) ⇒ `pg_stat_database.xact_commit` delta **≈ số bán được**, không phải N.

**Out of scope**

- Không queue/"xếp hàng chờ mua" hay backpressure thật (đó là `SL-4.8` + Phase 5).
- Không làm counter cho voucher (cùng khuôn, làm sau nếu cần) · không đổi API public của checkout.

**Acceptance Criteria**

- [ ] Flash-sale scenario (N buyer theo chuẩn đo phase) ⇒ **sold == stock** chính xác (0 oversell, 0 thiếu) — dán số.
- [ ] Số transaction DB phát sinh **≤ stock × 2**, **không** ≈ N — đây là toàn bộ điểm của ticket.
- [ ] Hết hàng ⇒ `409` với p99 **< 20 ms** và **không** tăng `xact_commit` (chặn trước DB).
- [ ] Compensating: 1 đơn fail ở bước payment ⇒ counter được trả lại (assert giá trị Redis + bán lại được).
- [ ] Reconcile: cố tình sửa Redis lệch (+5) ⇒ job phát hiện, log ERROR, sửa về đúng DB.
- [ ] `e2e-checkout` 27 + `e2e-async` 48 xanh (luồng thường **không** bị ảnh hưởng khi SKU không flash-sale).

**Ghi chú kỹ thuật**

- **Redis là "cửa hẹp", không phải nguồn sự thật**: mất Redis = mất counter ⇒ phải có đường **tắt flash-sale** (cờ active TTL
  ngắn) để quay về reserve DB thường, thay vì bán sai.
- **Lua để atomic**: `GET` rồi `DECR` ở 2 lệnh là race (bán quá stock). Một `EVAL` là đủ.
- **Compensating idempotent**: retry mà cộng counter 2 lần là bán khống ⇒ key `flash:compensated:<orderId>` + `SETNX`.
- **Đo phải so baseline `SL-4.1`**: không có baseline thì "nhanh hơn" chỉ là cảm giác.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.8 — Flash sale (2/2): degradation + backpressure + báo cáo spike

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.3` (429 ở biên) · `SL-4.7` (counter) · `SL-4.2` (cache giảm tải đọc khi spike) |
| **Ước lượng** | M |

**Bối cảnh**

`SL-4.7` lo "bán đúng". Ticket này lo phần còn lại của câu "**không sập, không treo API khác**": khi hàng trăm nghìn người cùng
bấm mua, hệ phải **hy sinh có chủ ý** phần không critical để giữ luồng checkout sống — thay vì để mọi thứ chậm đều rồi cùng chết.

**Mục tiêu**

1. Có **công tắc degradation** (runtime, không build lại): tắt/giảm luồng phụ khi spike.
2. Có **bằng chứng**: khi bật degradation, checkout giữ error + p99 trong ngưỡng, phần bị hy sinh trả lỗi **có kiểm soát**
   (429/503 + `Retry-After`), không phải timeout treo.
3. Có **báo cáo spike** with/without degradation trên cùng scenario.

**In scope**

- Config degradation (endpoint admin hoặc env + refresh — **không cần restart**):
  - `search` tắt ⇒ gateway trả `503` + `Retry-After` ngay (không gọi service).
  - `notification` consumer tạm dừng — email chậm, **không mất** (event chờ trong queue).
  - rate limit checkout siết (vd 10 → 5 rps/burst 10) trong cửa sổ spike.
- Gateway: nhánh `503` "tạm tắt" cho route bị degrade (format `ApiError`, có `Retry-After`) + `GET /debug/degradation`.
- k6 `flash-sale.js`: đo **without** và **with** degradation trên cùng bậc tải.
- Báo cáo: checkout p99/error, search p99/error, số 429/503, CPU container, và **hệ có tự phục hồi** sau spike không.

**Out of scope**

- Không circuit breaker tự động theo lỗi (Phase 5) — ở đây là **công tắc có người bấm** + rate limit.
- Không autoscaling (Phase 7), không hàng đợi thật (Phase 5 backpressure) · không đổi schema/DB.

**Acceptance Criteria**

- [ ] Bật/tắt degradation **không cần restart**; trạng thái đọc được qua `GET /debug/degradation` (khi debug bật).
- [ ] Cùng scenario flash-sale: **without** degradation ⇒ checkout error > 1% **hoặc** p99 vượt 500 ms (ghi số thật); **with**
      degradation ⇒ checkout error ≤ 1% và p99 < 500 ms (hoặc ghi rõ không đạt + lý do).
- [ ] Khi `search` bị degrade: search trả `503` p99 **< 50 ms** (không phải timeout 2 s) + `Retry-After`; checkout không bị
      ảnh hưởng (error/p99 trong ngưỡng).
- [ ] Sau spike: tắt degradation ⇒ hệ về bình thường **không restart** (e2e sau spike xanh + p99 về mức baseline).
- [ ] Báo cáo `docs/reports/scale-phase-4-baseline.md` (mục flash sale) có bảng before/after + **hệ số quy đổi NFR**.

**Ghi chú kỹ thuật**

- **"Danh sách hy sinh" phải rõ**: search/notification là eventual-consistent nên hy sinh được; **không bao giờ** hy sinh
  `inventory`/`payment`/`order` (đúng/sai dữ liệu).
- **503 khác 429**: 429 = "bạn gửi nhiều quá" (client tự chịu); 503 + `Retry-After` = "hệ đang tự bảo vệ" (server chịu). Dùng đúng
  loại thì client/proxy xử lý đúng.
- **Công tắc phải quan sát được**: không ai biết degradation đang bật ⇒ nó sẽ bật mãi (bài học "im lặng" của Phase 3).
- Runbook (`SL-4.12`) phải ghi: **ai bật, bật cái gì, tắt khi nào, kiểm gì sau khi tắt**.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.9 — Keyset pagination cho list (trả `TD-6`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.1` (đo p99 ở độ sâu lớn) |
| **Ước lượng** | S |

**Bối cảnh**

`GET /v1/products` dùng **offset pagination**: `OFFSET 100000` buộc Postgres đọc rồi **bỏ** 100k row trước khi trả 20 row ⇒ p99
tăng tuyến tính theo độ sâu trang. Đây là `TD-6` với điều kiện revisit "Phase 4 khi đo được p99 của list" — giờ đã đo được.

**Mục tiêu**

1. Có **keyset pagination** (`after` + `created_at`/`id`) cho list, trả kết quả **ổn định** (không nhảy/lặp row khi có insert).
2. Đo p99 ở **trang sâu** trước/sau trên cùng dataset.
3. Giữ **tương thích**: client cũ dùng `page` vẫn chạy (deprecated) — hoặc cắt hẳn nếu contract cho phép (ghi rõ chọn gì).

**In scope**

- API: `GET /v1/products?limit=20&after=<cursor>` (cursor = base64 của `created_at|id`), trả `nextCursor` (null khi hết).
- Repository: keyset (`WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT ?`) + index khớp.
- OpenAPI cập nhật + `docs/api/openapi.yaml` regenerate (`tools/export-openapi.sh`).
- Đo: p99 `page=1` vs `page=5000` (offset) so với `after=<cursor sâu>` (keyset) trên 1.000 product + 200k row sinh thêm.
- Test: unit (cursor encode/decode, thứ tự), IT (không lặp/không mất row khi insert chen giữa), e2e cũ vẫn xanh.

**Out of scope**

- Không đổi **mọi** endpoint list trong 1 ticket: chỉ list được đo là chậm; endpoint khác để ticket sau (`TD-6` ghi phần còn lại).
- Không làm cursor cho search (`search-service` có paging riêng).

**Acceptance Criteria**

- [ ] p99 trang sâu: keyset **≥ 5× nhanh hơn** offset (hoặc ghi rõ số nếu khác) + `EXPLAIN` cho thấy **Index Scan** thay vì
      `Bitmap/Seq + Sort` (dán output).
- [ ] Trang sâu trả **đúng** số row + không trùng `id` giữa 2 trang liên tiếp.
- [ ] Insert 1 product **giữa lúc đang phân trang** ⇒ không lặp và không mất row (chứng minh bằng test đối chiếu 2 cách).
- [ ] `docs/api/openapi.yaml` có `after`/`nextCursor`, validate PASS; client cũ (`page`) vẫn qua e2e.
- [ ] `mvn verify` xanh + `e2e-smoke` 17 xanh.

**Ghi chú kỹ thuật**

- **Cursor phải chứa đủ khoá sắp xếp**: `(created_at, id)` chứ không chỉ `id` — sort theo thời gian, `id` chỉ là tie-breaker
  (nhiều row cùng giây).
- **Index phải khớp `ORDER BY`**: `(created_at DESC, id DESC)` — thiếu index thì keyset vẫn chậm, chỉ là chậm kiểu khác.
- **`OFFSET` không "sai" với dữ liệu tĩnh**: nó chỉ sai khi có insert/delete chen giữa ⇒ phải chứng minh bằng test có insert
  chen giữa, không chỉ đo tốc độ.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.10 — Retention cho `checkout_sagas` + `checkout_saga_steps` (trả `TD-24`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | `SL-4.0` (đã có khuôn retention + hạ tầng job) |
| **Ước lượng** | S |

**Bối cảnh**

`SL-4.0` dọn `outbox`/`processed_events`/`notification_logs`, nhưng **2 bảng saga** vẫn phình: `checkout_sagas` +
`checkout_saga_steps` ghi 1 dòng/bước/lần checkout (`TD-24`). Load test Phase 4 tạo **hàng chục nghìn** saga trong vài phút —
đúng lúc không nên để bảng audit này lớn vô hạn.

**Mục tiêu**

Dọn saga đã **kết thúc** (terminal state) theo tuổi, giữ đủ lâu để điều tra, và **không bao giờ** chạm saga đang chạy.

**In scope**

- `order-service`: mở rộng `RetentionSweeper` (đã có) — xoá `checkout_saga_steps` của saga cũ **trước**, rồi `checkout_sagas`
  (`state IN ('COMPLETED','EXPIRED','FAILED_*','CANCELLED')` và `created_at < cutoff`), batch delete.
- Ngưỡng `SHOPLAB_ORDER_SAGA_RETENTION` mặc định **30 ngày** (audit), cấu hình được + compose env.
- IT: saga terminal cũ ⇒ xoá cả steps; saga **đang chạy** (`COMPLETED_START`/`STARTED`) ⇒ giữ; saga mới ⇒ giữ.
- Runbook: thêm dòng kiểm (đếm saga cũ còn lại) — tổng hợp ở `runbook-phase-4.md` (`SL-4.12`).

**Out of scope**

- Không xoá `orders`/`order_items` (dữ liệu nghiệp vụ, yêu cầu lưu trữ riêng).
- Không archive sang bảng khác / cold storage (chỉ xoá theo tuổi).

**Acceptance Criteria**

- [ ] Saga terminal cũ hơn ngưỡng bị xoá **cùng steps của nó**; saga đang chạy + saga mới **còn nguyên** (assert ở IT).
- [ ] Không còn `checkout_saga_steps` mồ côi (steps trỏ tới saga đã xoá) — assert bằng query `left join`.
- [ ] Drill trên compose: chèn saga backdate → job xoá đúng số, log INFO có số row (dán output).
- [ ] `e2e-checkout` 27 + `e2e-checkout-fail` 41 xanh (saga vẫn chạy đúng).
- [ ] `TD-24` đánh dấu ✅ đã trả trong §7.

**Ghi chú kỹ thuật**

- **Thứ tự xoá**: steps trước, saga sau (FK). Xoá saga trước thì FK chặn hoặc cascade ngoài ý muốn.
- **Terminal state liệt kê tường minh**, không dùng `state <> 'STARTED'` — state mới thêm sau này sẽ **âm thầm** bị dọn.
- **30 ngày** chọn theo "điều tra sự cố" chứ không theo dung lượng; ghi lý do để lần sau không đổi bừa.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.11 — Khử flaky theo tải (`CheckoutSagaIT`, `inventory` IT) — trả `TD-36` + `TD-23`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | — (làm **trước** khi chạy load test hàng loạt) |
| **Ước lượng** | S |

**Bối cảnh**

Hai IT đã đỏ **vì tải**, không vì logic: `CheckoutSagaIT.sweeperExpiresOverdueOrder` (`TD-36`, payment-window 1 s) và
`inventory.commitAfterExpiryReturnsGone` (`TD-23`, sweeper 300 ms trong IT). Phase 4 sẽ chạy k6 + 15 container **đồng thời** với
`mvn verify` ⇒ hai test này đỏ thường xuyên và **che lỗi thật**.

**Mục tiêu**

1. Hai IT **deterministic**: không phụ thuộc thời gian thực/nhịp sweeper.
2. Chứng minh bằng cách chạy lặp **dưới tải** (compose up + k6 chạy nền) mà vẫn xanh.

**In scope**

- `CheckoutSagaIT`: mốc `created_at` **tường minh** cho **mọi** saga trong test; nâng payment-window IT lên ≥ 5 s (hoặc cả hai).
- `inventory` IT: tắt sweeper trong test (`sweep-interval-ms` rất lớn) hoặc `expires_at` tường minh + gọi sweeper **thủ công**
  thay vì chờ.
- Chạy lặp 5 lần mỗi class **khi** compose đang chạy + k6 read-catalog nền ⇒ 5/5 xanh (dán log).

**Out of scope**

- Không đổi logic nghiệp vụ (sweeper/expiry) — chỉ đổi **cách test điều khiển thời gian**.
- Không sửa `TD-23`/`TD-36` ngoài phạm vi 2 test này.

**Acceptance Criteria**

- [ ] `CheckoutSagaIT` + class chứa test expiry của `inventory` chạy **5 lần liên tiếp** dưới tải ⇒ **5/5 xanh** (dán log tóm tắt).
- [ ] Test **vẫn kiểm đúng hành vi**: cố tình sửa code (vd bỏ điều kiện EXPIRED) ⇒ test **đỏ** (chứng minh không làm yếu test để
      dễ xanh — dán kết quả lần đỏ đó).
- [ ] `mvn -B -ntp verify` xanh khi **không** có tải và khi **có** compose chạy.
- [ ] `TD-23` + `TD-36` đánh dấu ✅ đã trả trong §7.

**Ghi chú kỹ thuật**

- **Nguyên tắc**: test không được **chờ** thời gian thật; mọi mốc thời gian do test **đặt**. Chờ = flaky.
- **Chứng minh test còn giá trị** (mutation nhỏ: bỏ điều kiện ⇒ thấy đỏ) là bước hay bị bỏ — không có nó thì "test xanh" chỉ là
  test rỗng.
- Ticket **nhỏ nhưng chặn**: đỏ ngẫu nhiên trong Phase 4 sẽ làm mất niềm tin vào `mvn verify` (thứ đang là hàng rào chính).

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-4.12 — Báo cáo scale tổng hợp + DoD Phase 4 (runbook · archive · tag)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 4 — Scale & performance |
| **Phụ thuộc** | **mọi** ticket `SL-4.1` … `SL-4.11` `Done` |
| **Ước lượng** | S |

**Bối cảnh**

Mỗi ticket Phase 4 đo một pattern, nhưng **câu hỏi của phase** chỉ trả lời được khi gom lại: pattern nào giúp bao nhiêu, pattern
nào **không** giúp, và trần của hệ bây giờ là bao nhiêu. Không có ticket này thì Phase 4 kết thúc bằng 10 báo cáo rời rạc — đúng
thứ `AGENTS.md` §3.3 muốn tránh.

**Mục tiêu**

1. `docs/reports/scale-phase-4.md`: **một** bảng before/after cho mọi pattern + hệ số quy đổi NFR + "cái gì **không** giúp".
2. `docs/runbook-phase-4.md`: vận hành khi tải cao (429/503 · replica lag · degradation · flash sale · partition).
3. Đóng Phase 4 theo DoD (`AGENTS.md` §5, ADR-0005): archive · `docs/phases/phase-4.md` · tag `phase-4` + release.

**In scope**

- `docs/reports/scale-phase-4.md` — bảng: pattern · trước · sau · Δ% · điều kiện đo · **kết luận 1 dòng**; mục "**không** giúp /
  gây hại" + "trần hiện tại của hệ" (RPS + điểm vỡ mới) + giới hạn đã biết.
- `docs/runbook-phase-4.md` — đọc số liệu khi tải cao: **429 ở route nào** · **replica lag** (lệnh + ngưỡng) · **degradation**
  (bật/tắt/kiểm) · **flash sale** (pre-warm, counter, reconcile) · **partition** (tháng mới, kiểm pruning) · pool.
- `docs/phases/phase-4.md` + `docs/phases/README.md` + bảng archive + `architecture.md` §12 = ✅ Done.
- Archive `SL-4.x` → `Backlog-phase-4.md`; `Backlog.md` chỉ còn trạng thái + link.
- Tag annotated `phase-4` trên merge commit của PR này + GitHub Release (notes tái dùng `Backlog-phase-4.md`).

**Out of scope**

- Không đo thêm pattern mới (Phase 5) — thiếu số thì **mở ticket**, không nhét vào đây.
- Không sửa code service (bug phát hiện ⇒ ticket mới).

**Acceptance Criteria**

- [ ] `docs/reports/scale-phase-4.md` có **≥ 6 dòng** pattern với before/after **lấy từ ticket tương ứng** (không chép lại từ trí
      nhớ) + hệ số quy đổi NFR + mục "không giúp".
- [ ] `docs/runbook-phase-4.md` có ≥ **6 mục** và **mỗi lệnh chạy được thật** (dán output).
- [ ] `Backlog-phase-4.md` đủ ticket + header; `Backlog.md` **không còn** `### SL-4.x`; `docs/phases/phase-4.md` có số liệu phase.
- [ ] `docker compose down -v` → `up -d --build` ⇒ **15 container healthy** (thêm replica) + 4 script e2e xanh.
- [ ] `mvn -B -ntp verify` xanh; CI xanh trên PR (CI đang bật — `SL-3.9`).
- [ ] Tag `phase-4` + Release; `docs/phases/README.md` + bảng archive + `architecture.md` §12 = ✅ Done.

**Ghi chú kỹ thuật**

- **Số liệu phải có nguồn**: mỗi ô trong báo cáo tổng hợp trỏ về ticket/`/tmp/load-*` đã sinh ra nó — không có "khoảng 2×".
- **"Không giúp" là phần giá trị nhất**: pattern fail (vd cache hit thấp vì key phân tán) dạy nhiều hơn pattern thành công; phải
  ghi kèm số.
- DoD phase: tag gắn **sau merge** (tag trên merge commit đóng phase), release notes tái dùng `Backlog-phase-4.md`.
- **CI đang bật** ⇒ PR docs-only sẽ **không** có run (`paths-ignore`); nếu có commit code (vd regenerate `openapi.yaml`) thì phải
  chờ CI xanh.

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

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

| 2026-11-09 | SL-3.6 | **Index lệch là trạng thái bình thường — cái phải có là phát hiện + dựng lại**. Ba tính chất giữ cho index đáng tin: outbox ghi **cùng transaction**, consumer **idempotent** (UPSERT theo khoá tự nhiên), reconcile chạy lại bao nhiêu lần cũng ra một kết quả. `unaccent()` **không IMMUTABLE** ⇒ không dùng được generated column, phải ghi `search_vector` trong **chính câu UPSERT** (một chỗ tính, không hai); `plainto_tsquery` xoá cả lớp bug wildcard của `LIKE` nhưng **không** stem/đồng nghĩa tiếng Việt (nói thật, đừng hứa quá). UPSERT phải có **chốt thời gian** `updated_at <= EXCLUDED` (event lệch thứ tự không kéo dữ liệu cũ trở lại); reconcile cần **watermark** `updated_at < mốc bắt đầu quét` và phải **viết tay nhánh rỗng** (Spring đổi `NOT IN (:ids)` rỗng thành `NOT IN (NULL)` ⇒ "báo xong mà không làm gì"). `upserted` = số row **được ghi**, không phải số row đổi. Bẫy hạ tầng lặp lần 3: `RestClient.Builder` **không có** trong context service mới (Boot 4 tách auto-config) ⇒ JDK `HttpClient` + Jackson tường minh; và **đừng tin sort của service khác** (`sort=updatedAt` bị catalog trả 400 — chỉ e2e bắt được). |
| 2026-11-09 | SL-3.8 | **"Async cho nhanh" sai về bản chất — và số đo chứng minh**: checkout p50 42,7 → 43,1 ms (+0,9%). Email/index chưa bao giờ nằm trên đường checkout ⇒ thứ async mua được là **isolation** (consumer chết, checkout p95 vẫn 44,6 ms; 20 event chờ trong queue rồi xử lý đủ) và **độ trễ có trần** (p95 confirm→email 1011 ms ≈ relay 1 s). `p95` chênh +10,7 ms là **nhiễu tải trên cùng Postgres**, không phải phụ thuộc đồng bộ — chứng minh bằng phép đo ngược (consumer chết ⇒ p95 44,6 ms). Relay 63,7 msg/s so với trần 100 (batch 100 × 1 s) + đuôi 0,26 s ⇒ nghẽn là **relay polling**, không phải broker. Đo độ trễ event phải **ghép 2 database** (không JOIN xuyên DB được). Và **script đo cũng phải có test**: 4 lỗi tự gây (bash `local` + `set -u`, `psql -tA` separator `|`, chờ queue rỗng thay vì chờ email, checkout đồng thời không thanh toán) — trong đó lỗi separator cho ra `n=0` **không báo lỗi**, nguy hiểm hơn crash. Số đo tự chỉ ra nợ: `outbox` 715/380/359 row không có job dọn ⇒ `TD-35`. |
| 2026-11-11 | SL-3.9 | **Đóng phase là đọc lại toàn bộ tài liệu — và nó luôn tìm ra lỗi**: lượt này lộ 1 nợ trùng (`TD-35`≈`TD-29`), 1 dòng trạng thái sai trong bảng nợ (`TD-29` ghi "đã trả (chưa)"), 1 heading sai port (8090 vs **8091**), 1 contract snapshot **thiếu** endpoint (`/v1/auth/service-token` của `SL-3.11`) và 1 IT flaky **theo tải** (`CheckoutSagaIT`: đỏ khi 14 container chạy, xanh khi máy rảnh ⇒ đừng "chạy lại tới khi xanh", hãy chứng minh bằng 3 phép rồi ghi `TD` kèm hướng sửa). `down -v` bắt được lớp lỗi `up -d` che mất: **volume Postgres giữ credential cũ** ⇒ 14 container không lên. Runbook chỉ đáng tin khi dùng chính nó để debug: ca fail-mode thật dạy `SENT` ≠ "đã xử lý", `processed_events` **rỗng đúng thiết kế** khi handler fail, log "sau **4** lần thử" khi cấu hình 3 retry, và `dlq.sh move` **ack khỏi DLQ** (sai routing key là mất message). Cuối cùng: **tắt CI là quyết định, bật lại cũng là quyết định** — điều kiện revisit "khi đóng Phase 3" là thứ khiến `TD-28` được trả sau đúng 3 ngày. |
| 2026-11-11 | SL-4.0 | **Retention là bất biến hệ thống, không phải tham số tuỳ ý**: `processed_events` phải sống **>=** `outbox` — dedupe hết hạn trước thì replay DLQ muộn gửi email lần 2, mà triệu chứng nằm ở **service khác** chỗ gây lỗi (không service nào tự kiểm hộ được ⇒ chặn bằng mặc định an toàn + doc, không bằng code). Phần khó không phải câu DELETE mà là **điều kiện không xoá nhầm**: outbox chỉ `SENT` (PENDING = event chưa gửi), `notification_logs` chỉ row **đã gửi** (`sent_at IS NOT NULL`; row `FAILED` là dấu vết lần gửi hỏng, phải giữ) — cả 3 assert bằng IT với dữ liệu backdate. Quyết định: SQL + ngữ nghĩa ở **commons**, **nhịp chạy ở service** ⇒ không có scheduler ẩn, tắt job ở 1 service không kéo theo service khác. Đụng đúng vùng cấu hình thì lộ **bug im lặng**: `shoplab.events.dedup.auto-create-schema: false` ở 3 service **chưa bao giờ có tác dụng** (property flat ≠ khoá YAML lồng nhau) — khoá YAML chỉ đúng khi có **test bind**. Và: thêm property mà quên khai trong `infra/docker-compose.yml` thì knob chỉ dùng được ngoài Docker; index phải đi cùng job (không index ⇒ seq scan mỗi nhịp ⇒ job dọn thành nguồn tải). |
| 2026-11-09 | SL-3.7 | **Async test = poll state cuối, không assert sau khi gọi API** (xanh vì "chưa kịp chạy" là kiểu xanh nguy hiểm nhất). **Hai timeout**: 5 s cho luồng event thường, **15 s cho DLQ** vì retry `200ms,1s,3s` ≈ 4,2 s *trước khi* nack — và `/api/queues` của RabbitMQ là **metrics làm mới ~5 s** nên `messages_ready` còn `1` sau khi message đã đi (timeout phải lớn hơn chu kỳ làm mới của metrics, không chỉ thời gian xử lý). Bẫy thật: (1) xoá product ⇒ SKU `ARCHIVED` ⇒ checkout 409 hiện ra ở **bước DLQ** (tạo SKU riêng + assert cả `PUT /carts/items`); (2) `PUT /carts/items/{sku}` là **thêm** chứ không phải thay giỏ ⇒ giỏ còn SKU cũ ⇒ saga chết ở `PRICE` (`DELETE /v1/carts` đầu mỗi checkout); (3) `$RANDOM` **không** đủ làm khoá idempotency (bash lặp giá trị ⇒ 409 request-hash mismatch) ⇒ prefix + số thứ tự. Fail-mode bind lúc boot ⇒ chỉ bật bằng recreate container, **bắt buộc `trap` tắt lại**; replay **sau khi** tắt lỗi (sửa nguyên nhân rồi mới replay). Đo replay bằng **DB** (`notification_logs` có row), không bằng "DLQ rỗng" — DLQ rỗng cũng đúng khi message **mất**. |
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
| `TD-29` | Bảng `outbox` ở **3 service** (`order` · `payment` · `catalog`) phình 1 row/event (sau 1 buổi đo `SL-3.8`: 715 / 380 / 359 row) — index `PENDING` vẫn nhanh, nhưng backup/disk lớn lên | SL-3.3 | **✅ đã trả (`SL-4.0`)** — job retention xoá row `SENT` > 7d theo batch (`SHOPLAB_OUTBOX_RETENTION`), chỉ `SENT`; thêm index partial `idx_outbox_sent` | ✅ đã trả (SL-4.0) |
| `TD-28` | *(đã trả)* **CI từng TẮT tạm thời** (`vars.CI_ENABLED=false`, từ `SL-3.10`) để tăng tốc merge Phase 3 ⇒ mất 3 hàng rào tự động: `test` + JaCoCo gate độc lập, `lint` trong môi trường sạch, và **`build-image` matrix** (chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module/service). Verify chuyển sang **local** (`mvn verify` + e2e) do người/AI tự khai | SL-3.10 | ✅ **đã trả (`SL-3.9`)** — `gh variable delete CI_ENABLED` (2026-11-11), CI chạy lại bình thường; gỡ ngoại lệ ở `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + `README.md` | — |
| `TD-30` | `notification_logs` + `processed_events` phình: `processed_events` 1 row/event/consumer **vĩnh viễn** (không xoá được vì dedupe cần), `notification_logs` 1 row/đơn đã confirm | SL-3.4 | **✅ đã trả (`SL-4.0`)** — `processed_events` 7d (`SHOPLAB_EVENTS_DEDUP_RETENTION`), `notification_logs` 90d (`SHOPLAB_NOTIFICATION_LOG_RETENTION`); **bất biến**: dedup retention ≥ outbox retention | ✅ đã trả (SL-4.0) |
| `TD-31` | **Không có refund**: ca `payment.succeeded` cho đơn đã `EXPIRED`/`CANCELLED` (tiền đã thu, hàng đã trả) hiện chỉ log ERROR `TIỀN ĐÃ THU … cần hoàn tiền tay`. Chưa có bảng `refunds`/`transactions`, chưa có API hoàn tiền | SL-3.5 | Phase 6 (refund) hoặc ngay khi có ≥ 1 ca thật cần hoàn tiền trong môi trường chạy thật | cao |
| `TD-32` | **Outbox bị copy 3 lần** (`order` SL-3.3, `payment` SL-3.5, `catalog` SL-3.6): ~500 dòng gần giống nhau (record/repository/relay/properties/DDL) ở mỗi service | SL-3.5 | ✅ **đã trả (SL-3.6)** — chấp nhận copy lần 3 có ý thức: giữ `SL-3.6` đúng scope (diff nhỏ, review được) thay vì trộn refactor 2 service cũ vào ticket service mới. Extract `OutboxAutoConfiguration` vào commons khi có **producer thứ 4** hoặc khi lên OpenSearch (Phase 8 — lúc đó hình dạng outbox có thể phải đổi, extract sớm sẽ phải làm lại) | trung bình |
| `TD-33` | `search_product.min_price` = **giá niêm yết của product**, KHÔNG phải min qua `sku.price_override` (index chưa biết SKU) ⇒ `minPrice` filter sai lệch với SKU có override thấp hơn | SL-3.6 | Khi search cần lọc/sort theo giá SKU thật, hoặc khi có yêu cầu "từ giá X" trên UI — lúc đó thêm field SKU vào payload event + reindex theo SKU | trung bình |
| `TD-34` | `tools/export-openapi.sh` chưa gộp spec của `notification-service` + `search-service` ⇒ `docs/api/openapi.yaml` thiếu toàn bộ API Phase 3 | SL-3.6 | Khi cần spec đầy đủ (DoD Phase 3/4 hoặc có client sinh code từ spec) — sửa 1 lần: thêm 2 nguồn vào script merge | thấp |
| `TD-35` | *(đã gộp)* Bảng `outbox` 3 service không có job dọn — **trùng `TD-29`**, phát hiện lại khi viết báo cáo `SL-3.8`; gộp vào `TD-29` ở `SL-3.9` (một nguồn sự thật, không 2 dòng nợ cho cùng 1 việc) | SL-3.8 | ✅ đã gộp vào `TD-29` (`SL-3.9`) | — |
| `TD-36` | IT `order`: `CheckoutSagaIT.sweeperExpiresOverdueOrder` **flaky theo tải** — payment-window trong IT chỉ **1 s**, máy đang chạy 14 container compose thì một saga của test trước vừa quá hạn ⇒ lần `expireDue()` thứ 2 trả `1` thay vì `0` (gặp thật ở `SL-3.9`, `mvn verify` xanh lại ngay khi `docker compose down`; chạy riêng class cũng xanh) | SL-3.9 | Lần tới test này đỏ **hoặc** khi CI chạy song song với e2e: cô lập bằng `created_at` tường minh cho **mọi** saga trong test (không dựa vào `now()`) hoặc nâng payment-window IT lên ≥ 5 s | trung bình |


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
| 2026-11-09 | `SL-3.6` → `Done` (**đóng `TD-32`**): **`catalog` outbox product event + `search-service` 8091 (Postgres FTS) + reconcile**. `catalog-service`: `V4__create_outbox.sql` + package outbox (chép khuôn lần 3, có ý thức) + `CatalogOutbox` gọi trong **cùng transaction** của `ProductService.create/update/delete`; event `product.updated.v1` (tạo+sửa) / `product.archived.v1` (soft delete); +amqp, `publisher-confirm-type=correlated`, `shoplab.outbox.*`, **không** thêm rabbit vào readiness (producer chết broker vẫn phải 200). `search-service` **mới** (port **8091** — 8090 thuộc template): DB `search` (`unaccent` + `search_product` + GIN + `processed_events`), consumer `q.search` (consumer thứ 3 dùng lại `AbstractEventConsumer`), `GET /v1/search` (public, tsquery+unaccent, filter category/giá, phân trang offset), `POST /v1/admin/search/reindex` (ADMIN, watermark + archive orphan + idempotent), `@Scheduled` so số row (WARN lệch > 1%). Hạ tầng: Dockerfile + compose + CI matrix + **12 Dockerfile thêm 1 dòng COPY pom** + `POSTGRES_DB_SEARCH` + gateway route `/v1/search` (public) và `/v1/admin/search` (thắng `/v1/admin` nhờ prefix dài hơn) + README port. Verify: `mvn -B -ntp verify` **BUILD SUCCESS · 805 test (662 unit + 143 IT)** · AC1 thấy sau **228 ms**, giá đúng · AC2 row index `ARCHIVED` · AC3 `q=dien` khớp `Điện thoại`, `q=dien%` 200 · AC6 outbox `SENT` cho cả 2 event · AC4 reindex `{61,61,0}` sau khi xoá tay 7 row, lần 2 không nhân bản · AC5 broker chết: tạo product 201 + outbox `PENDING|attempts=3`, reindex vẫn dựng lại, bật broker ⇒ `SENT` · job lệch: `INDEX LỆCH: index=58 catalog=65 lệch=10.77%` · e2e 17/17 · 27/27 · 41/41. Nợ: `TD-33` (min_price chưa tính SKU override). |
| 2026-11-09 | `SL-3.7` → `Done`: thêm **`tools/e2e-async.sh`** — 48 assert, chứng minh 4 luồng async trên stack thật: `order.confirmed.v1` ⇒ email (`notification_logs`) · `payment.succeeded.v1` ⇒ order **tự** `CONFIRMED` (không gọi `/confirm`) · `product.updated/archived` ⇒ index search (giá mới + row `ARCHIVED`) · lỗi ⇒ **DLQ** ⇒ tắt fail-mode ⇒ `dlq.sh move` ⇒ xử lý lại. Poll có timeout (`POLL_FAST=5s`, `POLL_DLQ=15s` cho retry budget + độ trễ metrics của RabbitMQ), `trap` dọn state (fail-mode off + purge DLQ) kể cả khi bị kill, `DELETE /v1/carts` trước mỗi checkout, idempotency key = prefix + số thứ tự (không dùng `$RANDOM`). `e2e.yml`: thêm bước **E2E async (Phase 3)** chạy cuối (vì nó recreate `notification-service`) + job name `11 thành phần`; README mục **E2E async — SL-3.7**. Verify: script xanh **2 lần liên tiếp** (48/0 · 27,8 s và 48/0 · 30,9 s) · trap kiểm thật bằng `kill -TERM` (`FAIL_MODE true → false`, DLQ 0) · `e2e.yml` parse OK (10 step) · 3 script cũ 17/17 · 27/27 · 41/41 · `mvn -B -ntp verify` BUILD SUCCESS (805 test). |
| 2026-11-09 | `SL-3.8` → `Done`: thêm **`tools/bench-async.sh`** (5 phép đo, in bảng markdown + lưu raw) và **`docs/reports/async-phase-3.md`** — số đo thật: checkout p50 42,7→43,1 ms (async **không** làm nhanh hơn), confirm→email p50 552/p95 1011 ms, webhook→CONFIRMED p50 29/p95 46 ms, relay **63,7 msg/s** (trần 100 ⇒ nghẽn relay polling), DLQ **0** trong 300 s với 100 checkout + 100 update (phát = xử lý 100/100 ở cả 3 luồng, 0 email trùng), degradation: consumer chết ⇒ checkout p95 44,6 ms + 20 event chờ ⇒ bật lại xử lý đủ 20/20. Nợ mới `TD-35` (outbox không có job dọn). `README` + `architecture.md` trỏ tới báo cáo. |
| 2026-11-11 | `SL-3.9` → `Done`: **đóng Phase 3** — `docs/runbook-phase-3.md` (4 tầng debug · 8 lỗi · ca debug thật "email không tới" từ fail-mode) · `docs/phases/phase-3.md` · archive 12/12 ticket sang `Backlog-phase-3.md` (giờ "54 ticket chi tiết") · verify snapshot từ volume rỗng: **14/14 container healthy** + 4 script E2E (17/27/41/**48**) + `mvn verify` **805 test** + OpenAPI 42/51/48 · **CI bật lại** (`gh variable delete CI_ENABLED`, `TD-28` → đã trả) · gộp `TD-35` vào `TD-29`, thêm `TD-36` (IT order flaky theo tải) · tag `phase-3` + release. |
| 2026-11-11 | `SL-4.0` → `Done` (**trả `TD-29` + `TD-30`**): **retention job** cho `outbox` (chỉ `SENT`, 7d) · `processed_events` (7d) · `notification_logs` (90d); `ProcessedEventRetention` ở commons + 5 `RetentionSweeper` per service (`@Scheduled` `fixedDelay`, batch delete, log số row) · migration **partial index** `idx_outbox_sent` ở 3 DB · env + compose cho 5 service · **fix bug** `dedup.auto-create-schema` không bind (nested `dedup`) · runbook §3.7 (kiểm retention) + `architecture.md` §8.1 (nguyên tắc 4b + bất biến `dedup.retention ≥ outbox.retention`). Verify: **829 test** (684 unit + 145 IT) · IT backdate assert không xoá `PENDING`/row mới/`sent_at NULL` · drill compose `oldSENT=0 newSENT=1 oldPENDING=1` + log `giữ PT168H`/`PT2160H` · `e2e-async 48` + `e2e-checkout 27` + DLQ 0. |
| 2026-11-11 | **Breakdown Phase 4** → **13 ticket**: `SL-4.0` (retention — Done) · `4.1` k6 harness + baseline · `4.2` cache-aside catalog · `4.3` rate limit gateway · `4.4` read replica streaming · `4.5` connection pool · `4.6` partition `orders` · `4.7` flash sale (atomic counter) · `4.8` flash sale (degradation + báo cáo) · `4.9` keyset pagination (`TD-6`) · `4.10` saga retention (`TD-24`) · `4.11` khử flaky theo tải (`TD-36`+`TD-23`) · `4.12` báo cáo + DoD phase. **Chuẩn đo của phase**: **điểm vỡ** + **hệ số quy đổi NFR** (không bám 50k RPS tuyệt đối), k6 qua Docker, dataset 1.000 product / 200 buyer. **Quyết định**: replica = streaming thật · partition theo tháng (sharding → Phase 8) · flash sale tách 2 ticket · circuit breaker/metric → Phase 5/6 | bạn + AI |
