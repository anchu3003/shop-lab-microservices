# Phase 1 — Walking skeleton (snapshot `phase-1`)

| | |
|---|---|
| **Tag** | `phase-1` — gắn trên merge commit đóng Phase 1 |
| **Thời điểm** | 2026-11-06 |
| **Ticket** | `SL-1.0` … `SL-1.18` — **19/19 Done** (15 kế hoạch + 4 phát sinh) — chi tiết: [`Backlog-phase-1.md`](../../Backlog-phase-1.md) |
| **ADR** | [0001](../adr/0001-tech-stack.md) · [0002](../adr/0002-dev-environment.md) · [0003](../adr/0003-spring-boot-version-and-module-layout.md) · [0004](../adr/0004-service-commons-shared-library.md) · [0005](../adr/0005-phase-snapshot-and-release.md) |
| **Trạng thái code** | Walking skeleton **chạy end-to-end**: auth + CRUD catalog qua gateway, có contract OpenAPI |

## Có gì trong snapshot này

| Thành phần | Nội dung |
|---|---|
| **Service** | `api-gateway` (8080) — routing + verify JWT + gắn `x-user-*`/`x-correlation-id` · `identity-service` (8081) — register/login/refresh/logout, JWT **RS256** + JWKS, RBAC (`buyer`/`seller`/`admin`) · `catalog-service` (8082) — category/product/SKU, **tự verify JWT qua JWKS** (defense in depth) |
| **Data** | Postgres 16, **database per service** (`identity`, `catalog`), Flyway `V1`/`V2` + seed dev `R__` (chỉ local, **fail-closed**) |
| **Auth** | Access token 15m · refresh token **opaque 256-bit** (DB lưu SHA-256), **rotate mỗi lần dùng** + reuse detection revoke cả chuỗi, **grace window 30s** cho client race/mất mạng |
| **API** | `POST /v1/auth/{register,login,refresh,logout}` · `GET /v1/users/me` · `GET /v1/admin/users` (ADMIN) · `GET/POST /v1/categories` · `GET/POST /v1/products` · `GET/PUT/DELETE /v1/products/{id}` · `POST /v1/products/{id}/skus` |
| **Đọc/ghi** | Guest **đọc** được product (public), chỉ `seller`/`admin` **ghi**; lỗi **400/401/403/404/409** đều là `ApiError` có `traceId` |
| **Contract** | [`docs/api/openapi.yaml`](../api/openapi.yaml) — sinh từ code bằng `bash tools/export-openapi.sh`; **14 path / 18 operation / 7 operation cần token / 18 schema** |
| **Shared lib** | `packages/service-commons`: JSON log (`traceId`) · `ApiError` + `GlobalExceptionHandler` · correlation id · health check · env validator fail-fast · auto-config OpenAPI (khung spec + bộ lỗi chuẩn) |
| **Tooling** | `tools/e2e-smoke.sh` (**17 mục**) · `tools/export-openapi.sh` · `tools/merge-pr.sh` · `tools/gen-dev-keys.sh` · `tools/secret-scan.sh` |
| **Ops docs** | [`docs/runbook-phase-1.md`](../runbook-phase-1.md) — luồng, kiểm tra từng tầng, **5 lỗi** (401/403/404/503/timeout) |

## Cách chạy ở snapshot này

```bash
git checkout phase-1
cp .env.example .env
bash tools/gen-dev-keys.sh                                    # sinh khoá JWT dev (không commit)
docker compose -f infra/docker-compose.yml --env-file .env up -d --build
# 7 container healthy: postgres · redis · rabbitmq · adminer · identity · catalog · gateway

bash tools/e2e-smoke.sh        # 17 PASS / 0 FAIL (register → login → seller tạo product → guest đọc → buyer 403)
bash tools/export-openapi.sh   # VALIDATE PASS · 14 path / 18 operation
mvn -B -ntp verify             # 299 test (unit + IT Testcontainers) + lint + coverage gate
```

> ⚠️ **Hai bẫy đã gặp** (đừng mất thời gian tìm lại):
> 1. `docker compose` nạp `.env` theo thư mục chứa compose file (`infra/`) ⇒ **bắt buộc** `--env-file .env`.
> 2. Khoá JWT mount read-only mode 600 ⇒ container phải chạy `user: "${SHOPLAB_UID}:${SHOPLAB_GID}"`
>    (`tools/gen-dev-keys.sh` in ra đúng 2 giá trị này).


## Số liệu đo được (baseline của phase)

| Chỉ số | Giá trị | Đo bằng |
|---|---|---|
| Test toàn repo | **299** (42 `service-commons` · 35 template · 50 identity unit + 22 IT · 61 catalog unit + 18 IT · 71 gateway) | `mvn -B -ntp verify` |
| E2E smoke | **17 mục**, ~0,4–1,2 s/lần, chạy lại **6/6 lần** pass | `bash tools/e2e-smoke.sh` |
| OpenAPI | **14 path · 18 operation · 7 cần token · 18 schema** | `bash tools/export-openapi.sh` |
| CI (6 job: `lint` ∥ `test` → `build-image` ×4) | **~3m24s** (warm cache) | run `42250625469` trên `main` |
| Image | `identity` **530 MB** · `catalog` **530 MB** · `gateway` **395 MB** | `docker image ls` |
| Seed dev | 5 category · 20 product · 24 SKU (**chỉ local**) | `psql -d catalog` |
| Port dùng | 8080 · 8081 · 8082 (+ 5432 · 6379 · 5672 · 15672 · 8085 hạ tầng) | `README.md` |

## Chưa có (cố ý — sẽ đến ở phase sau)

- **Chưa có luồng mua hàng**: không cart · inventory · order · payment · promotion (Phase 2)
- **Chưa dùng message broker**: RabbitMQ có trong compose nhưng chưa publish/consume; chưa outbox/DLQ (Phase 3)
- **Chưa có cache / rate limit / read replica / load test** (Phase 4) · **chưa resilience pattern** (Phase 5)
- **Chưa có metrics/tracing/alert/SLO** (Phase 6 — hiện chỉ log JSON + `/health` `/ready` `/version`)
- **Chưa có K8s/registry/CD** (Phase 7) · **chưa CQRS/sharding/search engine** (Phase 8)
- **Technical debt đang mở**: xem [`Backlog.md` §7](../../Backlog.md) — đáng chú ý `TD-12` (**chưa có soft
  delete** cho product ⇒ chặn Phase 2) · `TD-1` (chưa dọn `refresh_tokens`) · `TD-4` (smoke chưa vào CI) ·
  `TD-6` (offset pagination) · `TD-7` (`LIKE '%q%'` full scan)
- Health check vẫn mức **TCP** (chưa `SELECT 1`) · access token **không** revoke được (hết hạn 15m) ·
  `GET /v1/admin/users` không phân trang

## Bài học đáng nhớ của phase

(đầy đủ ở [`Backlog-phase-1.md`](../../Backlog-phase-1.md) + [`Backlog-log-archive.md`](../../Backlog-log-archive.md) §Learning log)

- **Xanh không có nghĩa là đã kiểm**: `mvn verify` xanh mà 2 bug thật (**403 thành 500**, **`@Version` trả số
  cũ**) vẫn lọt — chỉ lộ khi **gọi HTTP thật**. Hành vi liên quan transaction/flush phải test bằng IT.
- **Thêm một lớp bảo vệ = thêm một chỗ phải kiểm hành vi lỗi**: `@PreAuthorize` ném exception *sau*
  `DispatcherServlet` nên không đi qua `ExceptionTranslationFilter` ⇒ phải có handler riêng.
- **Endpoint mới phải verify qua CỬA VÀO DUY NHẤT**: 200 khi gọi thẳng service nhưng **404 qua gateway**
  (thiếu route) — đã gặp **2 lần**.
- **Fail-closed mới là an toàn**: seed dev và `/v3/api-docs` phải **mặc định tắt** ở môi trường thật; comment
  "không dùng cho prod" không chặn được ai.
- **`@Transactional` + throw = rollback xoá luôn việc vừa ghi** ⇒ revoke chuỗi bị mất; cần `noRollbackFor` ở
  **mọi** tầng transaction mà exception đi qua.
- **Test phải có entropy thật**: timestamp giây làm smoke **flaky** (2 lần chạy cùng giây trùng `slug`) —
  xanh trên máy chậm, đỏ trên máy nhanh.
- **Verify script cũng là code**: bash + 3 lớp escaping làm token bị cắt ⇒ **17 FAIL giả**; chuyển sang Python.
- **Đo phải kèm bối cảnh**: với 20 row Postgres **bỏ index** là đúng (seq scan rẻ hơn); muốn chứng minh index
  dùng được phải `SET enable_seqscan=off` và **nói rõ đã ép**.
- **Bảo mật vs UX cần "cửa sổ" cấu hình được**: grace 30s cho refresh reuse — strict (`0s`) cho môi trường
  nhạy cảm, nới có biên cho client thật.
