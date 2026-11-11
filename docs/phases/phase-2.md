# Phase 2 — Core commerce flow (snapshot `phase-2`)

| | |
|---|---|
| **Tag** | `phase-2` — gắn trên merge commit đóng Phase 2 |
| **Thời điểm** | 2026-11-08 |
| **Ticket** | `SL-2.0` … `SL-2.13` — **15/15 Done** (10 kế hoạch + 5 phát sinh: `SL-2.6a`/`2.6b` tách từ `SL-2.6`, `SL-2.11`, `SL-2.12`, `SL-2.13`) — chi tiết: [`Backlog-phase-2.md`](../../Backlog-phase-2.md) |
| **ADR** | [0001](../adr/0001-tech-stack.md) · [0002](../adr/0002-dev-environment.md) · [0003](../adr/0003-spring-boot-version-and-module-layout.md) · [0004](../adr/0004-service-commons-shared-library.md) · [0005](../adr/0005-phase-snapshot-and-release.md) · **[0006](../adr/0006-checkout-model.md) (mô hình checkout — ADR chính của phase)** |
| **Trạng thái code** | **Luồng mua hàng chạy end-to-end**: giỏ → checkout saga → reserve/lock → payment intent → confirm ⇒ đơn `CONFIRMED`; nhánh fail có compensating, **không oversell** dưới 100 request đồng thời |

## Có gì trong snapshot này

| Thành phần | Nội dung |
|---|---|
| **Service mới (5)** | `cart-service` (8083, **Redis** — không có Postgres) · `inventory-service` (8084, DB `inventory`) · `promotion-service` (8086, DB `promotion`) · `order-service` (8087, DB `order`) · `payment-service` (8088, DB `payment`) |
| **Checkout saga** | `order-service` là **orchestrator** sync 5 bước (cart → giá từ catalog → lock voucher → reserve stock → tạo order → payment intent), state ghi xuống `checkout_sagas` + `checkout_saga_steps` sau **mỗi** bước ⇒ compensating chỉ cần `select` lại là đủ (không giữ trong RAM) |
| **Chống oversell** | `inventory`: conditional `UPDATE … WHERE on_hand - reserved >= qty` + reservation **TTL 15′** + sweeper `FOR UPDATE SKIP LOCKED` + `stock_ledger` append-only (2 cột delta). Bằng chứng: **100 request đồng thời** ⇒ đúng **10 × 201** / **90 × 409**, `reserved = on_hand = 10`, `Σ delta_reserved = 10` |
| **Idempotency** | `Idempotency-Key` ở checkout/payment intent + webhook provider (khoá tự nhiên) + sweeper dọn row (`order`, `payment`); nhánh fail **trả lại key** để client retry được |
| **Snapshot giá** | `order_items` lưu giá tại thời điểm mua (không FK sang catalog) ⇒ catalog đổi giá/archive không làm sai đơn cũ |
| **Voucher** | `promotion`: PERCENT/FIXED + `lock`/`commit`/`release` 1 lượt dùng, lock TTL 15′ + sweeper |
| **Payment** | `payment-service`: intent + **mock provider** (`/mock-provider/pay/{id}`) + webhook **HMAC** idempotent; hook bơm lỗi `/debug/faults` (chỉ `shoplab.debug-endpoints=true`) |
| **API** | [`docs/api/openapi.yaml`](../api/openapi.yaml) — **41 path · 50 operation · 35 operation cần token · 46 schema** (sinh từ code + validate) |
| **Ops docs** | [`runbook-phase-1.md`](../runbook-phase-1.md) (auth · 401/403/404/503) · **[`runbook-phase-2.md`](../runbook-phase-2.md)** (checkout saga · **7 lỗi** · debug thật ở §5) |
| **Tooling** | `tools/e2e-checkout.sh` (**27** mục) · `tools/e2e-checkout-fail.sh` (**41** mục, F1…F6) · `tools/e2e-smoke.sh` (17) · `tools/export-openapi.sh` · `tools/merge-pr.sh` |

## Cách chạy ở snapshot này

```bash
git checkout phase-2
cp .env.example .env
bash tools/gen-dev-keys.sh                                    # khoá JWT dev (không commit)
docker compose -f infra/docker-compose.yml --env-file .env up -d --build
# 12 container healthy: postgres · redis · rabbitmq · adminer + 8 service

bash tools/e2e-smoke.sh          # 17 PASS / 0 FAIL   (Phase 1 không regress)
bash tools/e2e-checkout.sh       # 27 PASS / 0 FAIL   (happy path + tiền + idempotency)
bash tools/e2e-checkout-fail.sh  # 41 PASS / 0 FAIL   (F1…F6 + compensating)
bash tools/export-openapi.sh     # VALIDATE PASS · 41 path / 50 operation
mvn -B -ntp verify               # 655 test (537 unit + 118 IT) + lint + coverage gate
```

> ⚠️ **Ba bẫy đã gặp** (đừng mất thời gian tìm lại):
> 1. `docker compose` nạp `.env` theo thư mục chứa compose file (`infra/`) ⇒ **bắt buộc** `--env-file .env`.

## Số liệu đo được (baseline của phase)

| Chỉ số | Giá trị | Đo bằng |
|---|---|---|
| Test toàn repo | **655** = **537 unit + 118 IT** (commons 47 · template 35 · identity 50+22 · catalog 79+25 · inventory 44+15 · promotion 48+13 · order 65+25 · payment 64+12 · cart 34+6 · gateway 71) | `mvn -B -ntp verify` |
| E2E | smoke **17** (~0,5 s) · checkout **27** (~1,1 s) · checkout-fail **41** (~11,5 s) | 3 script, stack compose |
| OpenAPI | **41 path · 50 operation · 35 cần token · 46 schema** | `tools/export-openapi.sh` |
| CI (`ci.yml`) | **~5 phút** (lint ∥ test → build-image ×8) | run trên `main` |
| `e2e.yml` | workflow có (compose + 2 script) nhưng **chưa dispatch** trên Actions — verify của phase là local | `gh run list --workflow e2e.yml` (rỗng) |
| Container | **12** (4 hạ tầng + 8 service) | `docker compose ps` |
| Image size | gateway **395 MB** · inventory/promotion/payment **421 MB** · order **422 MB** · cart **458 MB** · catalog/identity **530 MB** | `docker image ls` |
| Seed dev | 5 category · 20 product · 24 SKU (**chỉ local**) | `psql -d catalog` |
| Port | 8080 gateway · 8081 identity · 8082 catalog · **8083 cart** · **8084 inventory** · **8086 promotion** · **8087 order** · **8088 payment** · 8085 adminer · 5432 · 6379 · 5672 | `README.md` |

## Chưa có (cố ý — sẽ đến ở phase sau)

- **Chưa có message broker thật**: RabbitMQ có trong compose nhưng **không** publish/consume; chưa outbox/DLQ ⇒
  webhook `payment.succeeded` **không** tự cập nhật order (`TD-17`, mức `cao`) — Phase 3.
- **Chưa có refund/transactions**: tiền đã thu mà đơn `EXPIRED` thì chưa hoàn được (Phase 6).
- **Chưa có cache / rate limit / read replica / load test** (Phase 4) — `GET /v1/skus?ids=` public chưa rate limit
  (`TD-22`), `/v1/carts/**` cũng chưa (`TD-21`).
- **Chưa có resilience pattern**: chỉ timeout (`SAGA_CONNECT_TIMEOUT` 2 s / `SAGA_READ_TIMEOUT` 800 ms) + fail-fast;
  chưa retry/circuit breaker/bulkhead (Phase 5).
- **Chưa có metrics/tracing/alert/SLO** (Phase 6) — hiện chỉ log JSON + `/health` `/ready` `/version`.
- **Sweeper chạy in-process**: an toàn khi > 1 instance nhờ `SKIP LOCKED` (`TD-15` đã trả) nhưng vẫn là `@Scheduled`
  trong app, chưa tách job runner.
- **Chưa có K8s/registry/CD** (Phase 7) · **chưa CQRS/sharding/search engine** (Phase 8).
- Nợ đang mở đáng chú ý: `TD-17` (webhook không cập nhật order) · `TD-23` (IT inventory flaky) · `TD-25` (sweeper saga
  không có token user) · `TD-27` (`e2e-checkout-fail.sh` flaky ngay sau rebuild) — xem [`Backlog.md` §7](../../Backlog.md).

## Bài học đáng nhớ của phase

(đầy đủ ở [`Backlog-phase-2.md`](../../Backlog-phase-2.md) + [`Backlog-log-archive.md`](../../Backlog-log-archive.md) §Learning log)

- **Compensating chỉ đáng tin khi assert *state*, không phải status**: `503` trả về client **không** chứng minh hàng đã
  được trả — phải đọc `stock.reserved` và `voucher.locked`.
- **State bền > biến RAM**: ghi `reservation_id`/`lock_id` xuống DB ngay sau mỗi bước saga ⇒ crash giữa chừng vẫn có
  dữ liệu để bù; giữ trong RAM là mất dấu và hàng treo tới hết TTL.
- **Idempotency key phải được *trả lại* khi fail**: để `IN_PROGRESS` thì client sửa giỏ xong vẫn `409` suốt 1 giờ.
- **Timeout chỉ được chứng minh khi dependency *chậm hơn* ngưỡng**: `docker stop` (refused ~0 ms) và `failStatus=500`
  đi cùng một nhánh code ⇒ phải bơm `sleepMs=2000` mới thấy `SAGA_READ_TIMEOUT` có tác dụng (đo được 921 ms).
- **Refactor "chỉ move file" vẫn đổi hành vi**: advice 403 chuyển từ `@Component` sang auto-config làm **thứ tự bean
  đảo** ⇒ catch-all `Exception` bắt trước ⇒ 403 thành 500 (`SL-2.12`).
- **Số liệu trong ticket cũ sẽ mục**: ticket ghi "2 bản copy" nhưng thực tế 6 — đếm lại (`grep -rl`) trước khi làm.
- **E2E phải assert giá trị, không chỉ status**: 3 check tiền đỏ dù mọi `status` đều 201; và mọi ngôn ngữ trung gian
  "chuẩn hoá" số (`220000.00` → `220000.0`) ⇒ dùng `parse_float=str`.

> 2. `SAGA_*_URL` phải là **tên service** trong compose (`http://payment-service:8088`), không phải `localhost`.
> 3. Sau `docker start`, `docker inspect …Health.Status` còn báo `healthy` **cũ** tới hết 1 nhịp probe ⇒
>    chờ `/ready` 200 của chính service (ghi ở `runbook-phase-2.md` §5).
