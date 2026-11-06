# Runbook — Phase 1 (walking skeleton)

> Dùng khi: `tools/e2e-smoke.sh` fail, hoặc có request lỗi mà không biết vỡ ở tầng nào.
> Nguyên tắc: **đi từ ngoài vào trong** (client → gateway → service → DB), mỗi tầng có 1 lệnh kiểm tra.
> Phase 5 sẽ mở rộng file này thành runbook sự cố đầy đủ (`SL-5.x`).

## 1. Luồng request

```
client ──► api-gateway :8080 ──► identity-service :8081 ──► Postgres (DB identity)
                  │                    └─ /.well-known/jwks.json  (public key)
                  └────────► catalog-service  :8082 ──────► Postgres (DB catalog)
                                     └─ tải JWKS từ identity để tự verify token
```

- Gateway: xác thực token (trừ `public-paths`), xoá/ghi đè `x-user-*`, gắn `x-correlation-id`, forward.
- Service: **tự verify token** (defense in depth) → gọi thẳng port service cũng phải có token.

## 2. Bảng tra nhanh

| Thành phần | Port | Health | DB | Log |
|---|---|---|---|---|
| api-gateway | 8080 | `GET /health` `/ready` | — | `docker logs shoplab-api-gateway` |
| identity-service | 8081 | `GET /health` `/ready` `/version` | `identity` | `docker logs shoplab-identity-service` |
| catalog-service | 8082 | `GET /health` `/ready` `/version` | `catalog` | `docker logs shoplab-catalog-service` |
| Postgres | 5432 | `pg_isready` | 7 DB | `docker logs shoplab-postgres` |
| Redis / RabbitMQ | 6379 / 5672 | — | — | (Phase 4/3 mới dùng) |
| Adminer (UI DB) | 8085 | — | — | — |

Tài khoản dev có sẵn (**chỉ `APP_ENV=local`**, seed của SL-1.4): `seller@shoplab.local` /
`admin@shoplab.local`, mật khẩu `matkhau123`.

## 3. Kiểm tra từng tầng (copy-paste)

```bash
# 0) hạ tầng còn sống?
docker ps --format '{{.Names}}\t{{.Status}}' | grep shoplab

# 1) gateway
curl -s -o /dev/null -w 'gateway health=%{http_code}\n' localhost:8080/health
docker logs shoplab-api-gateway --tail 30

# 2) identity
curl -s -o /dev/null -w 'identity health=%{http_code}\n' localhost:8081/health
curl -s localhost:8081/ready | python3 -m json.tool
docker logs shoplab-identity-service --tail 30

# 3) catalog
curl -s -o /dev/null -w 'catalog health=%{http_code}\n' localhost:8082/health
curl -s localhost:8082/ready | python3 -m json.tool
docker logs shoplab-catalog-service --tail 30

# 4) DB
docker exec shoplab-postgres pg_isready -U shoplab
docker exec shoplab-postgres psql -U shoplab -d identity -c 'select count(*) from users'
docker exec shoplab-postgres psql -U shoplab -d catalog  -c 'select count(*) from product'

# 5) end-to-end (nếu 4 tầng trên OK thì lỗi nằm ở logic/quyền, không phải hạ tầng)
bash tools/e2e-smoke.sh
```

Đọc kết quả: mỗi bước PASS/FAIL kèm **status thật**. FAIL in thêm `body` (300 ký tự) + **gợi ý lệnh
kiểm tra** của đúng bước đó.

## 4. Năm lỗi thường gặp

| # | Triệu chứng | Lệnh kiểm tra | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|---|---|
| 1 | **401** ở endpoint cần token | `curl -s -o /dev/null -w '%{http_code}' localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN"` · `docker logs shoplab-api-gateway --tail 20 \| grep -i jwks` | Token hết hạn (15m) · `IDENTITY_JWT_ISSUER` của gateway/service **khác** issuer trong token · JWKS không lấy được (identity chết) | Login lại · kiểm `grep ISSUER .env` khớp docker-compose · `curl localhost:8081/.well-known/jwks.json` phải 200 |
| 2 | **403** dù đã login | `curl -s localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN"` → xem `role` · `docker logs shoplab-catalog-service \| grep -i "từ chối quyền"` | Dùng token **buyer** để ghi catalog (đúng là bị chặn) · role trong token cũ (đổi role cần token mới) | Dùng token `seller@`/`admin@` · login lại để lấy token mới |
| 3 | **404** ở API vừa thêm | `curl -s -o /dev/null -w '%{http_code}' localhost:8082/v1/xxx` (gọi **thẳng** service) | Gọi thẳng 200 nhưng qua gateway 404 ⇒ **thiếu route** ở `services/api-gateway/src/main/resources/application.yml` | Thêm `path-prefix` vào `shoplab.gateway.routes` rồi `docker compose up -d api-gateway` |
| 4 | **503** (không phải 500) | `curl -s localhost:8080/ready \| python3 -m json.tool` · `docker ps \| grep -E 'identity\|catalog'` | Service đích chết/`unhealthy` · gateway không connect được (sai `*_SERVICE_URL`, sai network) | `docker compose up -d <service>` · kiểm `IDENTITY_SERVICE_URL`/`CATALOG_SERVICE_URL` là **tên service** trong compose (không phải `localhost`) |
| 5 | **Timeout** / request treo | `time curl -s localhost:8080/v1/products?size=100` · `docker stats --no-stream` · `docker logs <svc> \| grep -i "timeout\|pool"` | Pool DB cạn (`maximum-pool-size`) · query chậm (sort cột không index) · service đích chậm quá `GATEWAY_READ_TIMEOUT` (5s) | Tăng `DB_POOL_MAX_SIZE` · kiểm `EXPLAIN` query · tăng `GATEWAY_READ_TIMEOUT` **chỉ khi** đã hiểu vì sao chậm |

Quy ước mã lỗi (áp dụng mọi service): **401** = chưa xác thực · **403** = đã xác thực nhưng thiếu
quyền · **404** = không có tài nguyên/route · **400** = input sai (xem `details`) · **409** = xung đột
dữ liệu · **503** = dependency chết, retry được.

## 5. Ví dụ debug thật (làm theo runbook, ghi lại nguyên văn)

**Giả lập lỗi**: tắt catalog-service rồi chạy e2e — script phải fail **đúng bước**, không fail lan.

```console
$ docker stop shoplab-catalog-service
$ bash tools/e2e-smoke.sh
  PASS [1] 1a health gateway
  PASS [2] 1b health identity
  FAIL [x] 1c health catalog — mong đợi 200 nhưng nhận status=000
    body:
    gợi ý: docker ps | grep catalog · docker logs shoplab-catalog-service --tail 50
    → docs/runbook-phase-1.md
  PASS [3] 2 register
  PASS [4] 3 login
  PASS [5] 4 protected /v1/users/me (có token)
  PASS [6] 5 /v1/users/me KHÔNG token → 401
  PASS [7] 6 traceId smoke-... có trong log identity (gateway forward header)
  PASS [8] 7 login seller (seller@shoplab.local)
  FAIL [x] 8 seller tạo category → 201 — mong đợi 201 nhưng nhận status=503
    body: {"code":"UNAVAILABLE","message":"Service đích không tới được: http://catalog-service:8082",
           "traceId":"7b841dc0-...","path":"/v1/categories","details":[]}
    gợi ý: 403 ⇒ token không phải SELLER · 404 ⇒ gateway thiếu route /v1/categories · 503 ⇒ catalog chết
  FAIL [x] 9 seller tạo product — bỏ qua (chưa có category)
  FAIL [x] 10 guest list product — bỏ qua (chưa tạo được product)
```

**Đọc lỗi theo runbook**:

1. `status=000` ở 1c = **không kết nối được** (curl không tới được cổng) — khác `503` ở bước 8 =
   gateway **còn sống** nhưng không tới được service đích. Hai mã này chỉ ra hai tầng khác nhau.
2. Mục 3 → `docker ps | grep catalog` → **không có container** ⇒ nguyên nhân là service chết, không
   phải quyền hay logic.
3. Xử lý: `docker compose -f infra/docker-compose.yml --env-file .env up -d catalog-service` → chờ
   `healthy` → chạy lại `bash tools/e2e-smoke.sh` → **17 PASS / 0 FAIL**.

Bài học ghi lại: khi connection bị từ chối, `curl -o <file>` **không** mở file ⇒ nếu script không xoá
body cũ trước mỗi request thì bước fail sẽ in **body của bước trước** (`{"status":"UP"}` trong khi
catalog đã chết). Đã sửa bằng `: > "$TMP_BODY"` — đây là loại bug "báo sai chỗ" nguy hiểm hơn bug
báo đúng.

## 6. Liên quan

- `tools/e2e-smoke.sh` — E2E của cả Phase 1 (SL-1.3 → SL-1.7); fail ở đâu in gợi ý lệnh kiểm tra ở đó.
- `README.md` § "E2E smoke" — cách chạy + output mẫu.
- `Backlog.md` → `SL-1.7` — AC + kết quả verify của runbook này.
- Phase 5 sẽ mở rộng file này thành runbook sự cố đầy đủ (thêm dashboard, alert, quy trình escalation).

