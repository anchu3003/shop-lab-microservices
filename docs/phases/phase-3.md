# Phase 3 — Async & event-driven (snapshot `phase-3`)

| | |
|---|---|
| **Tag** | `phase-3` — gắn trên merge commit đóng Phase 3 |
| **Thời điểm** | 2026-11-11 |
| **Ticket** | `SL-3.0` … `SL-3.11` — **12/12 Done** (10 kế hoạch + 2 phát sinh: `SL-3.10` tắt CI tạm thời, `SL-3.11` tách service-to-service auth từ `SL-3.5`) — chi tiết: [`Backlog-phase-3.md`](../../Backlog-phase-3.md) |
| **ADR** | [0001](../adr/0001-tech-stack.md) · [0002](../adr/0002-dev-environment.md) · [0003](../adr/0003-spring-boot-version-and-module-layout.md) · [0004](../adr/0004-service-commons-shared-library.md) · [0005](../adr/0005-phase-snapshot-and-release.md) · [0006](../adr/0006-checkout-model.md) · **[0007](../adr/0007-event-contract.md) (contract event — ADR chính của phase)** · [0008](../adr/0008-service-to-service-auth.md) |
| **Trạng thái code** | **4 luồng async chạy end-to-end**: `order.confirmed` ⇒ email · `payment.succeeded` ⇒ order tự `CONFIRMED` · `product.updated/archived` ⇒ search index · message lỗi ⇒ DLQ ⇒ replay tay. Không mất event khi consumer chết, không gửi trùng dưới tải |

## Có gì trong snapshot này

| Thành phần | Nội dung |
|---|---|
| **Service mới (2)** | **`notification-service`** (8089, DB `notification`) — consumer + email mock + retry 3 lần + DLQ · **`search-service`** (8091, DB `search`) — index từ event + **Postgres FTS** (`tsvector` + `unaccent` + GIN) + reconcile |
| **Broker** | **RabbitMQ 4** (lần đầu dùng thật): topic exchange `shoplab.events`, direct `shoplab.dlx`, **queue theo consumer** (`q.notification` · `q.order` · `q.search`) + `<queue>.dlq`; topology khai báo từ code (`EventTopologyDeclarer`) |
| **Transactional outbox** | 3 producer (`order` · `payment` · `catalog`) ghi bảng `outbox` **cùng transaction** với dữ liệu; relay polling `@Scheduled` 1 s / batch 100 / `FOR UPDATE SKIP LOCKED` + `claimed_at` (visibility timeout) |
| **Ngữ nghĩa** | **at-least-once** + consumer idempotent theo `(event_id, consumer)` trong `processed_events`; **`eventId = outbox.id`** ⇒ retry publish không sinh event mới (chống gửi trùng) |
| **Retry / DLQ** | consumer retry `200 ms · 1 s · 3 s` rồi `nack(requeue=false)` ⇒ `<queue>.dlq`; **không auto-replay** — `tools/dlq.sh list/peek/move/purge` (replay bằng routing key **gốc** từ `x-death`) |
| **Đường async đã thay sync** | Webhook `payment.succeeded` ⇒ event ⇒ order tự confirm (**trả `TD-17`**, trước đó tiền đã thu mà đơn treo) |
| **Service-to-service auth** | `POST /v1/auth/service-token` (client credentials, ADR-0008) để `order-service` gọi `inventory`/`promotion` `commit`/`release` không cần token user (**trả `TD-25`**) |
| **API** | [`docs/api/openapi.yaml`](../api/openapi.yaml) — **42 path · 51 operation · 35 operation cần token · 48 schema** (spec của `notification`/`search` **chưa** gộp — `TD-34`) |
| **Ops docs** | **[`runbook-phase-3.md`](../runbook-phase-3.md)** (4 tầng debug · **8 lỗi** · ca debug thật ở §5) · [`runbook-phase-2.md`](../runbook-phase-2.md) · [`runbook-phase-1.md`](../runbook-phase-1.md) |
| **Số liệu** | [`docs/reports/async-phase-3.md`](../reports/async-phase-3.md) — 5 bảng đo thật + **8 giới hạn đã biết** |
| **Tooling** | `tools/e2e-async.sh` (**48** mục) · `tools/bench-async.sh` (5 phép đo) · `tools/rabbit-topology.sh` · `tools/dlq.sh` |

## Cách chạy ở snapshot này

```bash
git checkout phase-3
cp .env.example .env
bash tools/gen-dev-keys.sh                                    # khoá JWT dev (không commit)
docker compose -f infra/docker-compose.yml --env-file .env up -d --build
# 14 container healthy: postgres · redis · rabbitmq · adminer + 10 service

bash tools/e2e-smoke.sh          # 17 PASS / 0 FAIL
bash tools/e2e-checkout.sh       # 27 PASS / 0 FAIL
bash tools/e2e-checkout-fail.sh  # 41 PASS / 0 FAIL
bash tools/e2e-async.sh          # 48 PASS / 0 FAIL   ← 4 luồng async + DLQ replay
bash tools/rabbit-topology.sh    # topology broker đang có (không phải thứ mình nghĩ)
bash tools/bench-async.sh        # ~7 phút: 5 phép đo, in bảng markdown
mvn -B -ntp verify               # 805 test (662 unit + 143 IT) + lint + coverage gate
```

> ⚠️ **Bốn bẫy đã gặp ở phase này** (đừng mất thời gian tìm lại):
> 1. `docker compose up` phải có `--env-file .env` (compose nạp `.env` theo thư mục của file compose — `infra/`).
> 2. **Đổi volume Postgres mà không đổi `POSTGRES_PASSWORD`** ⇒ mọi service `password authentication failed` rồi
>    `Flyway` fail ⇒ `identity` unhealthy ⇒ các service phụ thuộc không start. Gặp thật khi `.env` bị sửa sau khi
>    volume đã tạo: `docker compose down -v` rồi `up` lại (volume cũ giữ mật khẩu cũ).
> 3. **Index search rỗng sau `down -v`** là đúng thiết kế (index chỉ bơm bằng event) ⇒ chạy
>    `POST /v1/admin/search/reindex`, đừng đi tìm bug.
> 4. `eventId` **không** nằm trong `outbox.payload` — nó **là** `outbox.id`; đối chiếu nhầm cột thì kết luận sai
>    "consumer không dedupe".


## Số liệu đo được (baseline của phase)

| Chỉ số | Giá trị | Đo bằng |
|---|---|---|
| Test toàn repo | **805** = **662 unit + 143 IT** (commons 81 · template 35 · identity 54+24 · catalog 79+26 · inventory 44+15 · promotion 48+13 · order 78+28 · payment 66+12 · cart 34+6 · **notification 34+9** · **search 38+10** · gateway 71) | `mvn -B -ntp verify` (**BUILD SUCCESS**, 3 min 04 s khi stack compose đã tắt) |
| E2E | smoke **17** (~0,6 s) · checkout **27** (~1,2 s) · checkout-fail **41** (~13,5 s) · **async 48** (~37 s) | 4 script, stack compose từ volume rỗng |
| Async — latency checkout | p50 **42,7 → 43,1 ms** (+0,9%) · p95 **48,9 → 59,6 ms** (nhiễu tải, không phải phụ thuộc) · 20 request đồng thời p95 **~200 ms** ở **cả hai** trạng thái | `tools/bench-async.sh` |
| Async — độ trễ event | `confirm` → email p50 **552 ms** / p95 **1011 ms** · `webhook` → order `CONFIRMED` p50 **29 ms** / p95 **46 ms** | như trên (ghép 2 DB bằng timestamp) |
| Async — throughput relay | **63,7 msg/s** (500 event); trần lý thuyết **100** = batch 100 × nhịp 1 s ⇒ **nghẽn ở relay polling**, không phải broker | như trên |
| Async — độ bền | **DLQ = 0** trong 300 s (100 checkout + 100 update) · phát = xử lý **100/100** ở cả 3 luồng · **0** email trùng `event_id` | như trên |
| Degradation | consumer chết ⇒ 20 checkout p95 **44,6 ms**; `q.notification ready=20`; bật lại ⇒ drain **9,3 s**, email **20/20** | như trên (M5) |
| Nợ phát hiện từ chính số đo | `outbox` 715 / 380 / 359 row **không có job dọn** sau 1 buổi đo ⇒ `TD-35` | `psql` |
| OpenAPI | **42 path · 51 operation · 35 cần token · 48 schema** (spec `notification`/`search` **chưa** gộp — `TD-34`) | `tools/export-openapi.sh` |
| Container | **14** (4 hạ tầng + **10 service**) | `docker compose ps` |
| Image size | gateway **395 MB** · inventory/promotion **422 MB** · notification/search/order/payment **441–442 MB** · cart **458 MB** · identity **530 MB** · catalog **550 MB** | `docker image ls` |
| CI (`ci.yml`) | **4m16s – 5m40s** (lint ∥ test → build-image ×10) — lần xanh cuối `SL-3.2`, sau đó **tắt tạm** (`TD-28`, trả ở `SL-3.9`) | `gh run list --workflow ci.yml` |
| Port | 8080 gateway · 8081 identity · 8082 catalog · 8083 cart · 8084 inventory · 8086 promotion · 8087 order · 8088 payment · **8089 notification** · **8091 search** · 8085 adminer · 5432 · 6379 · 5672 · 15672 | `README.md` |


## Chưa có (cố ý — sẽ đến ở phase sau)

- **Không có metric/alert/dashboard**: sự cố async chỉ phát hiện được bằng cách **đọc 4 tầng bằng tay**
  (`outbox` → queue → `processed_events` → bảng kết quả) — đúng những gì `runbook-phase-3.md` hướng dẫn. Queue depth,
  DLQ size, relay lag **chưa** thành metric (Phase 6). Đây là khoảng trống lớn nhất của phase.
- **Không có job retention**: `outbox` · `processed_events` · `notification_logs` phình vô hạn (`TD-29` · `TD-30`).
- **Không có refund** cho ca `payment.succeeded` tới sau khi đơn đã `EXPIRED`/`CANCELLED` — chỉ log ERROR (`TD-31`).
- **Outbox bị copy 3 lần** (order · payment · catalog) — chấp nhận có ý thức, extract vào commons khi có producer thứ 4
  (`TD-32`).
- **Search chưa phải engine thật**: Postgres FTS `simple` + `unaccent` ⇒ không stemming tiếng Việt, không từ đồng nghĩa,
  không xếp hạng thông minh; `min_price` chưa tính `sku.price_override` (`TD-33`); OpenSearch để Phase 8.
- **DLQ không auto-replay** (chốt ở ADR-0007 D5) — replay là quyết định của con người, phải có lệnh tường minh.
- **Chưa có cache / rate limit / read replica / load test** (Phase 4) · **chưa retry–circuit breaker ở tầng HTTP**
  (Phase 5) · **chưa K8s/registry/CD** (Phase 7).
- Nợ đang mở đáng chú ý: `TD-29`/`TD-30` (retention) · `TD-31` (refund) · `TD-32` (outbox copy) · `TD-33` (`min_price`) ·
  `TD-34` (OpenAPI thiếu spec 2 service) · `TD-36` (IT `CheckoutSagaIT` flaky khi máy đang chạy stack compose).

## Bài học đáng nhớ của phase

(đầy đủ ở [`Backlog-phase-3.md`](../../Backlog-phase-3.md) + `Backlog.md` §6 Learning log)

- **"Chuyển sang async cho nhanh hơn" là câu sai, và số đo chứng minh**: checkout p50 42,7 → 43,1 ms. Async mua
  **decoupling** (consumer chết, checkout p95 vẫn 44,6 ms) và **độ trễ có trần** (~1 s/chặng) — không mua latency.
- **Async đổi "chặn" lấy "im lặng"**: `201` không còn nghĩa là "đã xử lý"; consumer chết thì không ai biết cho tới khi
  có người đi đọc DLQ. Không có metric thì decoupling trở thành **mất khả năng quan sát** (Phase 6 phải trả).
- **`eventId` phải ổn định trên mọi lần thử** (`eventId = outbox.id`): sinh mới mỗi lần publish ⇒ retry tạo event khác
  ⇒ dedupe vô nghĩa ⇒ email 2 lần. Chống trùng phải nằm ở **id**, không ở "chắc không retry".
- **Dedupe là điều kiện của retry**: marker chỉ được ghi khi xử lý **xong** — ghi trước rồi mới làm là tự chặn replay.
- **Routing key sai thì RabbitMQ không báo gì**: message rơi vào hư không, lỗi lộ ra ở tận "email không tới". Vì vậy có
  `tools/rabbit-topology.sh` và luật **peek trước, consume sau** cho DLQ (`dlq.sh move` ack khỏi DLQ — sai là mất).
- **Tool vận hành cũng phải có test**: `tools/dlq.sh` từng làm **mất 1 message** vì publish lại bằng `routing_key` của
  DLQ (là **tên queue**) thay vì key gốc trong `x-death[0]`; `python3 -c` nhiều dòng trong `$( … )` còn trả rỗng *sau khi*
  message đã bị `get` mất.
- **Script đo phải được thử với `n` nhỏ trước**: 4 lỗi im lặng (psql separator `|`, chờ queue rỗng thay vì chờ email,
  checkout đồng thời không thanh toán, `local` + `set -u`) chỉ lộ khi chạy smoke — một trong số đó cho ra `n=0` **không
  báo lỗi**, nguy hiểm hơn crash.
- **Số liệu luôn tự chỉ ra nợ**: 715 row outbox sau 1 buổi đo ⇒ `TD-35`; index lệch ⇒ `TD-33`. Đo không phải để khoe
  số, mà để tìm giới hạn thật.
- **Đóng phase là đọc lại toàn bộ tài liệu của phase**: lượt này lộ ra 1 nợ **trùng** (`TD-35` ≈ `TD-29` → đã gộp), 1 dòng
  trạng thái sai trong chính bảng nợ (`TD-29` ghi "đã trả ở SL-3.7 (chưa)"), và 1 IT **flaky theo tải**
  (`CheckoutSagaIT.sweeperExpiresOverdueOrder` đỏ khi 14 container đang chạy, xanh khi máy rảnh ⇒ `TD-36`).
