# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | |
|---|---|
| **Ticket đang làm** | — |
| **Ticket tiếp theo** | `SL-4.4` — Read replica cho `catalog` + `order` (streaming thật) + routing read/write + đo replica lag |
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
> Learning log + Changelog **đã tách** khỏi file này (`§6`) — gom ở [`Backlog-log-archive.md`](Backlog-log-archive.md).

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
| 1 | `SL-4.1` | **k6 harness + baseline**: 3 scenario · seed · `tools/load.sh` · tìm **điểm vỡ** đầu tiên | M | ✅ Done |
| 2 | `SL-4.2` | **Cache-aside `catalog`** (TTL + jitter + **single-flight**) — trả phần cache của `TD-22` | M | ✅ Done |
| 3 | `SL-4.3` | **Rate limit ở gateway** (token bucket Redis + Lua) — trả `TD-21` + phần limit của `TD-22` | M | ✅ Done |
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

**Debt sẽ trả trong phase**: `TD-6` (`4.9`) · ~~`TD-21` + `TD-22`~~ (**✅ đã trả ở `4.3`/`4.2`**) · `TD-23` + `TD-36` (`4.11`) · `TD-24` (`4.10`).
**Debt phát sinh trong phase**: `TD-37` (load.sh chưa sample CPU từng container) · `TD-38` (k6 image chưa pin) · `TD-39`
(invalidate trước commit) · `TD-40` (chi phí round-trip Redis của rate limit chưa tách được khỏi nhiễu).
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
| **Trạng thái** | ✅ Done (2026-11-12) |
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

- [x] `bash tools/load/seed.sh` chạy 2 lần: lần 2 **không** tạo thêm row, in cùng bộ ID.
- [x] `bash tools/load.sh --scenario read-catalog` chạy từ máy sạch (chỉ cần Docker + stack up), in bảng markdown theo bậc VU,
      raw lưu `/tmp/load-read-catalog-<ts>/`.
- [x] Có **điểm vỡ** cho `read-catalog` và `checkout`: bậc VU + RPS **ngay trước** điểm vỡ + lý do (p99 vượt ngưỡng hay
      error > 1%) + **hệ số quy đổi NFR**.
- [x] Script **dừng đúng** khi qua điểm vỡ và cảnh báo nếu **k6 chạm trần CPU** (điểm vỡ của client).
- [x] `docs/reports/scale-phase-4-baseline.md` có 3 bảng + kết luận "nghẽn đầu tiên ở đâu" kèm bằng chứng.

**Ghi chú kỹ thuật**

- **k6 qua Docker**: `docker run --network shoplab-net -v $PWD/tools/load:/scripts` ⇒ gọi `http://api-gateway:8080`.
- **Ramp theo bậc, không ramp mượt**: bậc rõ mới trả lời được "vỡ ở đâu"; mỗi bậc ≥ 60 s để p99 ổn định.
- **`ramping-vus`, không `constant-arrival-rate`**: mục tiêu là "bao nhiêu **client đồng thời**"; RPS là hệ quả.
- **Checkout cần state riêng**: pool 200 buyer, VU xoay vòng; `Idempotency-Key` = `<runId>-<vu>-<iter>` (bài học `SL-3.8`).
- **Đo trước, tối ưu sau**: baseline không được chứa thay đổi của `SL-4.2`+.

**Kết quả verify** *(2026-11-12, máy dev WSL2 12 thread · Docker 29.9.3 · stack 13 container)*

**1) Seed idempotent (AC #1)** — chạy `bash tools/load/seed.sh --buyers 400` **2 lần**:

```console
# lần 1
-- category: tạo mới 20 · đã có 0
-- product: tạo mới 1000 · đã có 0
-- sku: tạo mới 2000 · đã có 0
-- buyer: tạo mới 400 · đã có 0
   HTTP 200 {"scanned":1029,"upserted":1029,"archived":0}     ← reindex search
-- DB sau khi seed: catalog: 20 category / 2000 sku · identity: 400 buyer
-- fingerprint : d1412cc7427a84a8

# lần 2 — KHÔNG tạo row nào, cùng fingerprint
-- category: tạo mới 0 · đã có 20
-- product: tạo mới 0 · đã có 1000
-- sku: tạo mới 0 · đã có 2000
-- buyer: tạo mới 0 · đã có 400
-- DB sau khi seed: catalog: 20 category / 2000 sku · identity: 400 buyer
-- fingerprint : d1412cc7427a84a8
```

**2) Ba baseline** (mỗi lệnh tự dừng ở bậc vỡ):

```console
$ bash tools/load.sh --scenario read-catalog
| 50 | 3842.9 | 11.9 | 25.5 | 35.2 | 0.00 | 125.28 |
| 100 | 3795.6 | 24.5 | 57.2 | 81.4 | 0.00 | 132.21 |
| 200 | 3505.0 | 53.7 | 132.2 | 189.9 | 0.00 | 149.16 |
   p99 theo endpoint: products_list=190 product_detail=188 skus_lookup=191
   ⇒ ĐIỂM VỠ ở 200 VU (p99 189.9 ms > NFR 150 ms) — dừng ramp
>> Bậc cuối ĐẠT: 100 VU · 3795.6 req/s · p99 81.4 ms ⇒ 1/13 mục tiêu NFR

$ bash tools/load.sh --scenario read-search
| 50 | 4917.3 | 8.4 | 21.7 | 30.7 | 0.00 | 144.67 |
| 100 | 4757.5 | 17.5 | 51.9 | 75.7 | 0.00 | 151.99 |
| 200 | 4598.3 | 31.0 | 118.9 | 177.3 | 0.00 | 167.52 |
   ⇒ ĐIỂM VỠ ở 200 VU (p99 177.3 ms > NFR 150 ms) — dừng ramp
>> Bậc cuối ĐẠT: 100 VU · 4757.5 req/s ⇒ 1/11 mục tiêu NFR

$ bash tools/load.sh --scenario checkout
| 50 | 225.2 | 209.6 | 314.7 | 370.9 | 0.00 | 14.36 |
| 100 | 230.6 | 411.6 | 649.6 | 761.5 | 0.00 | 14.82 |
   ⇒ ĐIỂM VỠ ở 100 VU (p99 761.5 ms > NFR 500 ms) — dừng ramp
>> Bậc cuối ĐẠT: 50 VU · 225.2 req/s ⇒ 1/222 mục tiêu NFR
```

**3) Nghẽn ở đâu** — probe 1 bậc đúng bằng bậc vỡ + `docker stats` mỗi 3 s cho **mọi** container
(bảng chi tiết ở báo cáo §5):

```console
catalog @200 VU (tái lập: RPS 3442 · p99 184.9):
  shoplab-catalog-service 286% · shoplab-api-gateway 248% · shoplab-postgres 207% · k6 195%
checkout @100 VU (tái lập: RPS 229.8 · p99 764):
  shoplab-order-service 273% · shoplab-postgres 208% · shoplab-rabbitmq 152% · inventory 47%
  (identity 372% chỉ ở 2 sample ĐẦU = setup login 400 buyer; sau đó 0.1–2.4%)
PG: lock-wait 0 · buffer hit ratio 99.9–100% · conn 18–34 (pool 10/service) · active đỉnh 8
```

→ **DB không nghẽn, client không nghẽn.** Đọc nghẽn ở CPU service/gateway (p99 của 3 endpoint chênh
< 2% ⇒ hàng đợi chung, không phải 1 query); ghi nghẽn ở chuỗi saga đồng bộ. Báo cáo đầy đủ:
`docs/reports/scale-phase-4-baseline.md`. Raw: `/tmp/load-{read-catalog-1794494252,read-search-1794494659,checkout-1794494968}`.

**Learning note**

1. **"Điểm vỡ" không có nghĩa là lỗi — nó là chỗ latency bắt đầu nứt.** Cả 3 scenario vỡ với error
   **0,00%**: hệ chỉ chậm, chưa sai. Vì vậy ngưỡng phải là **p99 vs NFR**, không phải "khi nào có 5xx" —
   chờ 5xx mới gọi là vỡ là bỏ qua đúng vùng cần tối ưu.
2. **Số RPS tuyệt đối vô nghĩa nếu thiếu cửa sổ đo + điều kiện dataset.** Cùng máy: search "đạt" 4,7k
   RPS vì index chỉ **1.029 row**; catalog 3,8k RPS vì nằm trọn trong buffer (`blks_read = 0`, hit 100%).
   Ghi con số mà bỏ 2 điều kiện này là tạo cảm giác an toàn giả.
3. **Ba endpoint chi phí query khác nhau nhưng p99 giống hệt (< 2%) ⇒ nghẽn không ở query.** Bằng chứng
   *loại trừ* này rất rẻ: chỉ cần **tag theo endpoint** rồi so p99 giữa chúng. Không tag thì ta sẽ đi
   `EXPLAIN`/thêm index và không hiểu vì sao p99 không đổi.
4. **Harness cũng tạo "điểm vỡ giả" được — và cách chặn là dựng trước tình huống đó:**
   - Checkout 3 request/iteration ⇒ p99 là p99 của *3 request*. Kiểm bằng probe (checkout **không** xoá
     giỏ) rồi dựng giỏ 1 lần trong `setup()` ⇒ iteration còn **1 request**.
   - 400 VU / 200 buyer ⇒ 2 VU tranh 1 giỏ ⇒ 422. Seed 400 buyer + `load.sh` **tự cảnh báo** khi
     `max(VU) > số buyer`.
   - `setup()` login 400 buyer (BCrypt) làm `identity-service` 372% CPU trong **15 s đầu** — nhìn `max`
     mà không nhìn **chuỗi thời gian** là kết luận sai "identity là nút cổ chai".
5. **RPS = count ÷ cửa sổ, không dùng `rate` của k6** khi `setup()` nặng: `rate` chia cho cả thời gian
   setup ⇒ báo thấp hơn thực tế. Sai số "trông khoa học" (2 chữ số thập phân) nhưng lệch hệ thống.
6. **k6 v2 đổi chỗ số liệu** (`metrics[x].values.p(99)` thay vì `metrics[x].p(99)`) và image chạy bằng
   user `k6` nên thư mục mount phải `chmod 777` — cả hai đều làm harness "im lặng ra số 0" nếu không
   kiểm bằng một lần chạy thật trước khi tin nó.

### SL-4.2 — Cache-aside cho `catalog` (TTL + jitter + single-flight)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-12) |
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

- [x] Đo lại `read-catalog` ở **cùng** bậc VU: p99 giảm **≥ 30%** **hoặc** điểm vỡ lùi **≥ 1 bậc** (ghi cả hai nếu đạt); không
      đạt thì báo cáo phải giải thích **bằng số** (nghẽn chuyển sang đâu).
- [x] Hit ratio ≥ **80%** ở scenario read (log `cache=hit/miss` hoặc header debug).
- [x] **Stampede test**: xoá key hot rồi bắn **200 request đồng thời** ⇒ query DB cho key đó **≤ 2** (delta
      `pg_stat_database.xact_commit` hoặc log `db-load`).
- [x] **Invalidation**: update product ⇒ `GET /v1/products/{id}` + list trả **dữ liệu mới** trong ≤ 1 s (poll).
- [x] **Fail-open**: `docker stop shoplab-redis` ⇒ endpoint catalog vẫn `200` (chậm hơn, log WARN), không 500.
- [x] `mvn verify` xanh (unit test key/TTL/jitter/single-flight) + `e2e-smoke`/`e2e-checkout` không regress.

**Ghi chú kỹ thuật**

- **Cache-aside**, không read-through: quyết định đọc cache ở service ⇒ dễ debug hơn interceptor "tự động".
- **Jitter**: `TTL = base × (1 ± jitter%)` bằng `ThreadLocalRandom` — mục tiêu là **phá đồng pha**.
- **Single-flight bằng lock Redis, không `synchronized`**: nhiều instance thì lock JVM vô nghĩa. Đánh đổi: request thua lock
  chờ ~50–100 ms (đo được) — chấp nhận, vì "tất cả cùng đập DB" tệ hơn.
- **Invalidation theo version key**: xoá từng key dễ sai (list có filter/sort) ⇒ update 1 product xoá key cụ thể; hàng loạt
  thì bump `catalog:v1`.
- **Hit ratio phải có nguồn đo**: header `X-Cache` chỉ bật khi `SHOPLAB_DEBUG_ENDPOINTS=true` (khuôn có ở payment `/debug/faults`).

**Kết quả verify** *(2026-11-12 · máy dev WSL2 12 thread · cùng seed `d1412cc7427a84a8` · cùng image, chỉ đổi `SHOPLAB_CATALOG_CACHE_ENABLED`)*

**1) Before/after `read-catalog`** (AC #1 — đạt **cả hai** điều kiện):

```console
# OFF: /tmp/load-read-catalog-1794502919     # ON: /tmp/load-read-catalog-1794502428
VU 50 : 3288.9 RPS · p99 42.5 ms   →  4801.2 RPS · p99 32.3 ms    (+46% RPS · −24% p99)
VU 100: 3120.1 RPS · p99 101.2 ms  →  4799.0 RPS · p99 70.0 ms    (+54% · −31% p99 ← AC "p99 ≥30%")
VU 200: 3267.1 RPS · p99 189.8 ms ❌ → 4890.6 RPS · p99 145.7 ms   (+50% · −23%)
VU 400: (đã vỡ ở 200)              →  5181.6 RPS · p99 190.1 ms ❌  ← điểm vỡ lùi 1 bậc (200 → 400)
Điểm vỡ: 200 VU → 400 VU · NFR 1/16 → 1/10 · xact_commit 679.613 → 18.377 (−97%)
```

**2) Hit ratio (AC #2)** — delta `/debug/cache` trong đúng lần đo ON:

```console
hits=3763535 misses=14639 → hit ratio 99.61% · dbLoads=14506 · bypass=0 · errors=0
```

**3) + 4) + 5) Drill `bash tools/cache-drill.sh --with-fail-open`** (raw `/tmp/cache-drill-1794502415`):

```console
PASS [1] D1 hit ratio 99.2% ≥ 80%
-- D2 stampede: DEL key hot → 200 request đồng thời
   DB loads=1 · hits=199 · response khác 200: 0
PASS [2] D2 stampede: 200 request đồng thời → chỉ 1 lần đọc DB (≤ 2)
-- D3 invalidation: PUT product → poll detail/list
   write=200 · detail tươi sau 16 ms · list tươi sau 29 ms
PASS [3] D3 invalidation: detail 16 ms · list 29 ms (≤ 1000)
-- D4 fail-open: docker stop shoplab-redis
   detail=200 list=200 skus=200 categories=200 (Redis đang tắt)
PASS [4] D4 fail-open: 4 endpoint catalog vẫn 200 khi Redis chết
PASS [5] D4b Redis bật lại: cache tự hoạt động lại (hit=1)
== Kết quả: 5 PASS / 0 FAIL
```

**6) Không regress (AC #6)**:

```console
$ mvn -pl services/catalog-service -am verify
Tests run: 117, Failures: 0, Errors: 0   (unit — 33 test mới cho cache)
Tests run:  28, Failures: 0, Errors: 0   (IT: Postgres + Redis Testcontainer, có test single-flight 20 thread)
BUILD SUCCESS
$ bash tools/e2e-smoke.sh    → 17 PASS / 0 FAIL
$ bash tools/e2e-checkout.sh → 27 PASS / 0 FAIL
```

Quyết định + trade-off: `docs/adr/0009-catalog-cache-aside.md` · số liệu đầy đủ: `docs/reports/scale-phase-4-baseline.md` §9.

**Learning note**

1. **Baseline nói trước nên sửa ở đâu — và nó đúng.** `SL-4.1` cho thấy p99 của 3 endpoint đọc chênh nhau < 2% ⇒ nghẽn là
   hàng đợi chung, không phải query. Vì vậy ticket này **không** sờ vào SQL/index. Sau khi cache, p99 vẫn bằng nhau
   (145/146/146 ms ở 200 VU) ⇒ nghẽn **vẫn** ở tầng service/gateway, chỉ lùi xa hơn. Nếu hôm trước đi tối ưu query thì
   hôm nay đã "thành công" mà p99 không đổi.
2. **Hit ratio là hệ quả của KEY SCHEMA, không phải của Redis.** Cache `skus?ids=` theo *tổ hợp request* sẽ luôn ~0% vì tổ
   hợp 100 id là vô hạn; cache theo *từng SKU* cho 99,6%. Cùng endpoint, cùng Redis, khác mỗi cách đặt key.
3. **Thứ cache mua được là DB rảnh tay, không chỉ p99.** `xact_commit` giảm **97%** trong khi p99 giảm 31% — vì p99 lúc đó
   bị chi phối bởi CPU gateway/catalog. Phải báo cả hai; chỉ báo p99 thì dễ kết luận "cache không đáng".
4. **Fail-open chỉ có nghĩa nếu fail ĐÚNG NHANH.** Hai bẫy đã tránh: (a) **không** đưa Redis vào readiness (nếu không,
   orchestrator đánh unready khi Redis chết ⇒ fail-open vô nghĩa); (b) phân biệt `lock() == false` (có người đang đọc ⇒ chờ
   đúng ý đồ) với `CacheUnavailableException` (Redis hỏng ⇒ đọc DB **ngay**). Gộp hai thứ này thì lúc Redis chết mọi request
   ngủ thêm `lock-wait` ⇒ p99 tệ hơn không có cache.
5. **Invalidate phải đúng cả hai vế: xoá key cũ *và* bump generation.** Update 1 product phải xoá `product:<id>` + **mọi**
   `sku:<id>` của nó (giá SKU = `priceOverride ?? product.price`) và `INCR` generation của list. Quên vế SKU là bán giá cũ —
   sai tiền, không phải sai số đo.
6. **Hai bẫy của CHÍNH script drill** (không phải của hệ): `curl … | grep -q` với `set -o pipefail` ⇒ `grep -q` khớp sớm,
   `curl` bị SIGPIPE, pipeline trả **non-zero** ⇒ "tìm thấy" bị đọc thành "chưa thấy"; và `date +%s%3N` trên máy này in
   **epoch+nano** (không cắt 3 số) ⇒ deadline ms bị so với nano ⇒ vòng poll không chạy lần nào. Cả hai đều dễ bị "sửa" bằng
   cách nới ngưỡng — sai hoàn toàn.
7. **Nợ đã biết, ghi ngay**: invalidate chạy **trong transaction, trước commit** ⇒ request đọc chen giữa có thể cache lại giá
   trị cũ (sống tới hết TTL). Ghi `TD-39` với điều kiện revisit định lượng, không để "khi nào rảnh".

### SL-4.3 — Rate limit ở gateway (token bucket Redis + Lua)


| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-13) |
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

**Kết quả verify** *(2026-11-13)*

```
$ bash tools/rate-limit-drill.sh --with-overhead      → 10 PASS / 0 FAIL
== D0  enabled=True · default per-ip burst=400 · per-user burst=200
== D3  status=429 code=TOO_MANY_REQUESTS retry-after=1 limit=400 remaining=0 reset=1794538730
       body={"code":"TOO_MANY_REQUESTS",…,"traceId":"9332ea72-…","path":"/v1/products"}
  PASS [D3] 429 đủ 5 thông tin (status + Retry-After + 3 header X-RateLimit-*)
  PASS [D3] body là ApiError với code=TOO_MANY_REQUESTS + traceId
== D1  500 VU KHÔNG token dội /v1/products 30s
   tổng request 735323 · bị chặn 728871 · lọt 6452 · tỉ lệ 429 99.1% · p99 70.8 ms
  PASS [D1] tỉ lệ 429 = 99.1% (≥ 90%)                        ← AC1
== D2  xact_commit: 5021226 → 5021823 (Δ 597) · request LỌT limiter: 6452
  PASS [D2] Δ xact_commit 597 ≤ request lọt 6452 (+5)         ← AC1 (chặn Ở BIÊN, không ở DB)
== D4  A: 297 × 200 · 303 × 429 | B: 200
  PASS [D4] A bị chặn 303 lần, B vẫn 200                      ← AC5 (không chặn oan)
== D5  Redis dừng · GET /v1/products = 200 · WARN 'fail-open' = 1
  PASS [D5] fail-open đúng: 200 + WARN                       ← AC4
   Redis trở lại · đốt bucket · GET tiếp theo = 429
  PASS [D5] tự phục hồi: limiter chặn lại
== D6  limiter TẮT: 4804 RPS · p99 64.8 ms | BẬT (ngưỡng cao): 4498 RPS · p99 62.2 ms
  PASS [D6] Δp99 -2.6 ms · ΔRPS -306 (lần chạy trước: +16.4 ms / -643 RPS)
```

`mvn -B -ntp verify` (cả repo): **BUILD SUCCESS** — gateway **94 unit + 10 IT** (6 `RedisTokenBucketIT` trên Redis thật qua
Testcontainers + 4 `RateLimitIT` HTTP thật), JaCoCo gate `ratelimit/*` đạt; commons 87. `e2e-smoke` **17/17**,
`e2e-checkout` **27/27** (`RATE_LIMIT_ENABLED=true`, ngưỡng mặc định ⇒ **0 × 429**: không chặn oan luồng thật).

**Hai lần đo D6 khác nhau** (+16,4 ms và −2,6 ms) ⇒ Δp99 **nằm trong nhiễu**, chỉ throughput giảm nhất quán (−6…−18 %) ⇒ ghi
`TD-40` (lặp ≥ 5 lần) thay vì chọn số đẹp.

**Learning note**

1. **"Chặn ở biên" phải chứng minh bằng số của DB, không bằng status code.** 429 nhiều chưa nói gì: nếu chặn *sau* khi đã vào
   service thì DB vẫn quay. Bằng chứng đúng là **`xact_commit` Δ 597 cho 735.323 request**, và phép so phải là
   `Δcommit ≤ số request lọt limiter`, không phải "Δ nhỏ hơn tổng request".
2. **Đặt key bằng `sub` CHƯA verify = tự vô hiệu hoá rate limit.** `JwtAuthFilter` không verify token ở path public, nên
   filter rate limit **phải** chạy sau nó; nếu không, kẻ tấn công gửi `sub` ngẫu nhiên mỗi request là có vô hạn bucket. Thứ
   tự filter ở đây là **ranh giới an ninh**, không phải chi tiết triển khai.
3. **Một xô là không đủ, và hai xô rẻ hơn tưởng.** Per-user ⇒ xoay account vượt trần; per-IP ⇒ NAT chặn oan. Kiểm cả hai
   trong **cùng một `EVAL`** ⇒ có cả hai tính chất, **không** thêm round-trip. Hệ quả vận hành thật: **load test đi từ 1 IP
   nên bị chính limiter chặn** ⇒ phải có `RATE_LIMIT_ENABLED` và harness phải **đếm 429**, nếu không "điểm vỡ" ghi được là
   điểm vỡ của limiter — một con số sai mà không ai nghi ngờ.
4. **Kiểm hết rồi mới trừ** (trong Lua): trừ xô user trước rồi mới phát hiện xô IP cạn ⇒ user mất token cho request **không
   được phục vụ**. Có test riêng (`RedisTokenBucketIT.rejectedRequestShouldNotConsumeOtherBucket`).
5. **Fail-open phải fail ĐÚNG NHANH**: bắt `RuntimeException` (không chỉ lỗi Redis) + timeout 100 ms + Redis **không** trong
   readiness + gateway **không** `depends_on` Redis. Redis mà vào readiness thì orchestrator đánh gateway unready ⇒ fail-open
   thành vô nghĩa (bài học `SL-4.2`, lặp lại ở tầng khác).
6. **Rate limit làm hỏng công cụ của chính mình — đó là thông tin, không phải phiền.** `tools/load/seed.sh` (400 buyer qua
   `POST /v1/auth/register`) và e2e chạy từ 1 IP bị 429 giữa đường. Sửa đúng là **chờ `Retry-After`** (seed) và **tắt
   limiter khi đo tải**, **không** nới ngưỡng toàn cục — nới rồi thì không ai nhớ trả lại.
7. **Script đo cũng có bug, và bug của script trông y như bug của hệ.** Ba lần drill đỏ liên tiếp đều do script: đốt bucket
   đúng bằng `burst` (400) rồi kiểm ngay ⇒ xô đã nạp lại 50 token (`remaining=50`) ⇒ đọc thành "limiter không chặn";
   `curl -o /dev/null` chỉ áp cho URL **đầu** khi dùng `--parallel` ⇒ log ngập JSON; và cửa sổ lấy mẫu phải nằm **trong lúc**
   đang dội (nạp 200 token/giây ⇒ 5 ms sau khi dừng là đã có token). Sửa bằng `burn_forever` (dội liên tục tới khi kill),
   không nới ngưỡng, không tăng sleep.
8. **Test của chính rate limit cũng flaky vì thời gian — CI bắt được, local không.** `RateLimitIT` bản đầu gửi 3 request
   *tuần tự* với hạn mức `2 rps / burst 2` rồi kỳ vọng request thứ 3 là 429; trên runner CI mỗi request ~350 ms ⇒ xô nạp lại
   0,7 token ⇒ **200** thay vì 429 (đỏ ở run `43357368722`, xanh ở máy dev). Sửa đúng: hạn mức **1/1** + bắn **6 request
   đồng thời** ⇒ đúng 1 lọt, 5 bị chặn, **không phụ thuộc tốc độ máy** — và tiện thể kiểm luôn tính **nguyên tử** của `EVAL`
   (nếu không nguyên tử thì nhiều request cùng lọt). Chạy lặp 3 lần: 3/3 xanh. Bài học: với limiter, **thời gian là biến số
   ẩn**; test phải hoặc cố định đồng hồ, hoặc bắn song song — đừng "gửi N request rồi tin thứ tự".


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

## 6. Bài học & nhật ký (đã tách khỏi file)

> Không ghi vào đây nữa. **Bài học** → mục `Learning note` trong từng ticket (`Backlog.md` / `Backlog-phase-<N>.md`).
> **Lịch sử thay đổi** → `git log` + `architecture.md` §Changelog. Toàn văn log cũ (Phase 0 → 4):
> [`Backlog-log-archive.md`](Backlog-log-archive.md).

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
| `TD-22` | `GET /v1/skus?ids=` là **public**, nhận tới 100 id, ~~chưa rate limit~~ và ~~chưa cache~~ ⇒ 1 client có thể dội tải catalog (100 id/request) hoặc dùng làm đòn bộc phát | SL-2.13 | ✅ **đã trả hết** — cache theo từng SKU + single-flight (`SL-4.2`, ADR-0009) và rate limit ở biên (`SL-4.3`, ADR-0010: mặc định `100/200` per-user + `200/400` per-IP, đổi bằng `RATE_LIMIT_DEFAULT_*`) | ✅ đã trả (SL-4.2 + SL-4.3) |
| `TD-23` | IT `inventory`: `commitAfterExpiryReturnsGone` **flaky** — test đẩy `expires_at` về quá khứ rồi `commit`, nhưng sweeper trong IT chạy mỗi **300 ms** nên có lúc nó expire trước ⇒ `commit` trả **409** thay vì **410** (đỏ trên CI chậm, xanh ở máy local) | SL-2.13 (CI run `42444551383`) | Lần tới khi sửa test của inventory: tắt sweeper (`sweep-interval-ms` rất lớn) cho test lazy-expire, hoặc assert `status ∈ {410, 409}` + kiểm `reserved = 0` (điều thực sự quan trọng) | trung bình |
| `TD-24` | `checkout_saga_steps` + `checkout_sagas` **không có job dọn** — 1 dòng/bước/lần checkout ⇒ phình mãi (audit nhưng không ai đọc sau 30 ngày) | SL-2.6a | Khi bảng > 10⁶ row hoặc khi `SL-2.6b` thêm sweeper (gộp 1 `@Scheduled` dọn saga `COMPLETED_START`/`FAILED_*` cũ > 30 ngày) | thấp |
| `TD-25` | Checkout sweeper **không gọi được `release`** (không có token user) ⇒ đơn `EXPIRED` nhưng hàng/voucher chỉ được trả khi **TTL sweeper** của inventory/promotion chạy (trễ tối đa ~1 nhịp, 60 s) | SL-2.6b | Khi Phase 3 có **service-to-service auth** (hoặc API internal riêng) ⇒ sweeper gọi `release` ngay trong cùng nhịp | ✅ đã trả (SL-3.5) |
| `TD-26` | `build-image` (CI) **flaky** khi Maven Central tạm thời không trả artifact: mỗi image `mvn package` từ đầu KHÔNG cache Maven ⇒ gặp hiccup là đỏ oan (gặp thật ở `SL-2.8`: `spring-boot-dependencies:4.1.2 … absent in central`, rerun thì xanh) | SL-2.8 (run `42451712993`) | Phase 7: registry + **cache layer Maven** (`--mount=type=cache` hoặc image builder có `.m2` ấm) ⇒ build nhanh và bớt phụ thuộc Central | thấp |
| `TD-21` | `/v1/carts/**` là public ở gateway và ~~chưa có rate limit~~ ⇒ có thể spam tạo guest token/giỏ (mỗi token = 2 key Redis, TTL 30 ngày) | SL-2.1 | ✅ **đã trả (`SL-4.3`)** — gateway rate limit theo **IP** cho `/v1/carts` (`50 rps / burst 100`, đổi bằng `RATE_LIMIT_CART_IP_*`); drill D1 chứng minh 99,1 % bị chặn ở biên, `xact_commit` không tăng theo | ✅ đã trả (SL-4.3) |
| `TD-29` | Bảng `outbox` ở **3 service** (`order` · `payment` · `catalog`) phình 1 row/event (sau 1 buổi đo `SL-3.8`: 715 / 380 / 359 row) — index `PENDING` vẫn nhanh, nhưng backup/disk lớn lên | SL-3.3 | **✅ đã trả (`SL-4.0`)** — job retention xoá row `SENT` > 7d theo batch (`SHOPLAB_OUTBOX_RETENTION`), chỉ `SENT`; thêm index partial `idx_outbox_sent` | ✅ đã trả (SL-4.0) |
| `TD-28` | *(đã trả)* **CI từng TẮT tạm thời** (`vars.CI_ENABLED=false`, từ `SL-3.10`) để tăng tốc merge Phase 3 ⇒ mất 3 hàng rào tự động: `test` + JaCoCo gate độc lập, `lint` trong môi trường sạch, và **`build-image` matrix** (chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module/service). Verify chuyển sang **local** (`mvn verify` + e2e) do người/AI tự khai | SL-3.10 | ✅ **đã trả (`SL-3.9`)** — `gh variable delete CI_ENABLED` (2026-11-11), CI chạy lại bình thường; gỡ ngoại lệ ở `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + `README.md` | — |
| `TD-30` | `notification_logs` + `processed_events` phình: `processed_events` 1 row/event/consumer **vĩnh viễn** (không xoá được vì dedupe cần), `notification_logs` 1 row/đơn đã confirm | SL-3.4 | **✅ đã trả (`SL-4.0`)** — `processed_events` 7d (`SHOPLAB_EVENTS_DEDUP_RETENTION`), `notification_logs` 90d (`SHOPLAB_NOTIFICATION_LOG_RETENTION`); **bất biến**: dedup retention ≥ outbox retention | ✅ đã trả (SL-4.0) |
| `TD-31` | **Không có refund**: ca `payment.succeeded` cho đơn đã `EXPIRED`/`CANCELLED` (tiền đã thu, hàng đã trả) hiện chỉ log ERROR `TIỀN ĐÃ THU … cần hoàn tiền tay`. Chưa có bảng `refunds`/`transactions`, chưa có API hoàn tiền | SL-3.5 | Phase 6 (refund) hoặc ngay khi có ≥ 1 ca thật cần hoàn tiền trong môi trường chạy thật | cao |
| `TD-32` | **Outbox bị copy 3 lần** (`order` SL-3.3, `payment` SL-3.5, `catalog` SL-3.6): ~500 dòng gần giống nhau (record/repository/relay/properties/DDL) ở mỗi service | SL-3.5 | ✅ **đã trả (SL-3.6)** — chấp nhận copy lần 3 có ý thức: giữ `SL-3.6` đúng scope (diff nhỏ, review được) thay vì trộn refactor 2 service cũ vào ticket service mới. Extract `OutboxAutoConfiguration` vào commons khi có **producer thứ 4** hoặc khi lên OpenSearch (Phase 8 — lúc đó hình dạng outbox có thể phải đổi, extract sớm sẽ phải làm lại) | trung bình |
| `TD-33` | `search_product.min_price` = **giá niêm yết của product**, KHÔNG phải min qua `sku.price_override` (index chưa biết SKU) ⇒ `minPrice` filter sai lệch với SKU có override thấp hơn | SL-3.6 | Khi search cần lọc/sort theo giá SKU thật, hoặc khi có yêu cầu "từ giá X" trên UI — lúc đó thêm field SKU vào payload event + reindex theo SKU | trung bình |
| `TD-34` | `tools/export-openapi.sh` chưa gộp spec của `notification-service` + `search-service` ⇒ `docs/api/openapi.yaml` thiếu toàn bộ API Phase 3 | SL-3.6 | Khi cần spec đầy đủ (DoD Phase 3/4 hoặc có client sinh code từ spec) — sửa 1 lần: thêm 2 nguồn vào script merge | thấp |
| `TD-35` | *(đã gộp)* Bảng `outbox` 3 service không có job dọn — **trùng `TD-29`**, phát hiện lại khi viết báo cáo `SL-3.8`; gộp vào `TD-29` ở `SL-3.9` (một nguồn sự thật, không 2 dòng nợ cho cùng 1 việc) | SL-3.8 | ✅ đã gộp vào `TD-29` (`SL-3.9`) | — |
| `TD-36` | IT `order`: `CheckoutSagaIT.sweeperExpiresOverdueOrder` **flaky theo tải** — payment-window trong IT chỉ **1 s**, máy đang chạy 14 container compose thì một saga của test trước vừa quá hạn ⇒ lần `expireDue()` thứ 2 trả `1` thay vì `0` (gặp thật ở `SL-3.9`, `mvn verify` xanh lại ngay khi `docker compose down`; chạy riêng class cũng xanh) | SL-3.9 | Lần tới test này đỏ **hoặc** khi CI chạy song song với e2e: cô lập bằng `created_at` tường minh cho **mọi** saga trong test (không dựa vào `now()`) hoặc nâng payment-window IT lên ≥ 5 s | trung bình |
| `TD-37` | `tools/load.sh` chỉ sample CPU của **k6**; muốn biết nghẽn ở service nào phải probe tay bằng `docker stats` trong một lần chạy riêng (đã làm ở `SL-4.1` §5 để tìm nghẽn đầu tiên) | SL-4.1 | Ngay ở `SL-4.2` (cache): cần so CPU **catalog-service/redis** trước–sau trên cùng bậc, làm tay 2 lần là đủ thấy phải tự động hoá | thấp |
| `TD-38` | Harness dùng `grafana/k6:latest` (không pin tag/digest) ⇒ image đổi hành vi giữa 2 lần đo thì số before/after không còn so sánh được (đã dính 1 lần: k6 v2 chuyển số liệu vào `metrics[x].values` làm parser ra toàn 0) | SL-4.1 | Khi pin version cho cả stack (Phase 7) hoặc khi 2 lần chạy cùng điều kiện lệch > 20% không giải thích được | thấp |
| `TD-39` | Cache catalog **invalidate trong transaction, trước commit**: một request đọc chen giữa có thể cache lại giá trị cũ (đọc bản đã commit trước đó) và nó sống tới hết TTL (≤ 66 s). Xảy ra khi có traffic đọc **đồng thời** với lúc update | SL-4.2 | Khi có ghi product đồng thời với tải đọc lớn (flash sale `SL-4.7`/`4.8`) hoặc khi phát hiện dữ liệu cũ > 1 s: chuyển invalidate sang **sau commit** (`TransactionSynchronization.afterCommit`) hoặc invalidate theo event outbox | trung bình |
| `TD-40` | Rate limit ở gateway thêm **1 round-trip Redis cho mỗi request** ở cửa vào: đo được **−6,4 % RPS** (4.804 → 4.498) nhưng Δp99 **không kết luận được** (2 lần chạy ra −2,6 ms và +16,4 ms) | SL-4.3 | Lặp `bash tools/rate-limit-drill.sh --with-overhead` **≥ 5 lần** ở trạng thái máy ổn định để tách nhiễu; nếu ΔRPS > 10 % đều đặn ⇒ làm **local pre-check** (xô token trong JVM, chỉ gọi Redis khi gần cạn — chấp nhận hạn mức xấp xỉ) | trung bình |
