# Runbook — Phase 2 (core commerce flow · checkout saga)

> Dùng khi: checkout fail, đơn treo `PENDING_PAYMENT`, **hàng/voucher không được trả lại**, hoặc
> `tools/e2e-checkout*.sh` đỏ.
> Nguyên tắc: **đọc state trong DB**, không chỉ tin HTTP status — 503 trả về client **không** chứng minh
> compensating đã chạy (bài học `SL-2.9`).
> Phase 1 (auth · JWT · JWKS · catalog CRUD) đã có runbook riêng: [`runbook-phase-1.md`](runbook-phase-1.md).
> Phase 5–6 sẽ mở rộng file này thành runbook sự cố đầy đủ (alert · dashboard · escalation).

## 1. Luồng checkout — ai gọi ai

**Saga orchestration** nằm ở `order-service` (ADR-0006): gọi **sync** 5 dependency theo một chiều, mỗi bước
ghi state xuống DB ngay (không giữ trong RAM).

```
client ──► api-gateway :8080 ──► order-service :8087   POST /v1/checkout   (header Idempotency-Key)
                                      │
                                      ├─1─► cart      :8083  (Redis)                đọc giỏ
                                      ├─2─► catalog   :8082  GET /v1/skus?ids=…     giá thật (batch)
                                      ├─3─► promotion :8086  POST /v1/vouchers/lock  giữ 1 lượt voucher
                                      ├─4─► inventory :8084  POST /v1/stock/reserve  giữ hàng  (TTL 15′)
                                      ├─5─► DB `order`  orders + order_items (snapshot giá) + checkout_sagas
                                      └─6─► payment   :8088  POST /v1/payment-intents  tạo intent + paymentUrl
                                                                  ⇒ order PENDING_PAYMENT, saga COMPLETED_START
client ──► gateway ──► POST /v1/checkout/{orderId}/confirm    → verify payment → commit stock + voucher ⇒ CONFIRMED
client ──► gateway ──► POST /v1/checkout/{orderId}/cancel     → compensating (release stock + voucher) ⇒ CANCELLED
```

**Fail ở bước 3–6** ⇒ compensating chạy **ngược** ngay trong request (release cái đã giữ) ⇒ client nhận
`503`/`409`, DB phải sạch (`reserved = 0`, `voucher.locked = 0`).

**Ai dọn khi client bỏ ngang** (không confirm, không cancel) — 3 sweeper độc lập, **đều idempotent**:

| Sweeper | Ở đâu | Nhịp | Dọn cái gì |
|---|---|---|---|
| saga hết hạn | `order-service` | 60 s | saga quá `SAGA_PAYMENT_WINDOW=15m` chưa confirm ⇒ `EXPIRED` (không tự gọi API — bài học `TD-25`) |
| reservation hết hạn | `inventory-service` | 60 s | reservation `PENDING` quá `expires_at` ⇒ `EXPIRED` + ledger `EXPIRE` (`FOR UPDATE SKIP LOCKED`) |
| voucher lock hết hạn | `promotion-service` | 60 s | lock quá TTL ⇒ `RELEASED`, trả lượt |

## 2. Bảng tra nhanh

| Thành phần | Port | Health | Data (source of truth) | Log |
|---|---|---|---|---|
| api-gateway | 8080 | `/health` `/ready` `/version` | — | `docker logs shoplab-api-gateway` |
| identity-service | 8081 | `/health` `/ready` | DB `identity` | `docker logs shoplab-identity-service` |
| catalog-service | 8082 | `/health` `/ready` | DB `catalog` | `docker logs shoplab-catalog-service` |
| **cart-service** | 8083 | `/health` `/ready` | **Redis** `cart:u:<userId>` / `cart:g:<guestToken>` (không có Postgres) | `docker logs shoplab-cart-service` |
| **inventory-service** | 8084 | `/health` `/ready` | DB `inventory` (`stock` · `reservations` · `stock_ledger`) | `docker logs shoplab-inventory-service` |
| **promotion-service** | 8086 | `/health` `/ready` | DB `promotion` (`vouchers` · `voucher_locks`) | `docker logs shoplab-promotion-service` |
| **order-service** | 8087 | `/health` `/ready` | DB `order` (`orders` · `order_items` · `checkout_sagas` · `idempotency_keys`) | `docker logs shoplab-order-service` |
| **payment-service** | 8088 | `/health` `/ready` | DB `payment` (`payments` · `payment_webhook_events`) | `docker logs shoplab-payment-service` |
| Postgres | 5432 | `pg_isready` | 6 DB (identity · catalog · inventory · promotion · order · payment) | `docker logs shoplab-postgres` |
| Redis | 6379 | `redis-cli -a $REDIS_PASSWORD ping` | cart | `docker logs shoplab-redis` |
| Adminer (UI DB) | 8085 | — | — | — |

**Env knob quyết định hành vi saga** (`.env` — sai một cái là test đỏ oan):

| Biến | Mặc định | Ý nghĩa | Sai thì sao |
|---|---|---|---|
| `SAGA_READ_TIMEOUT` | `800ms` | timeout đọc mỗi dependency | quá thấp ⇒ 503 oan; quá cao ⇒ checkout treo theo dependency |
| `SAGA_PAYMENT_WINDOW` | `15m` | cửa sổ chờ thanh toán | **phải bằng** TTL reservation/lock, không thì có lúc đơn `EXPIRED` mà tiền đã vào |
| `INVENTORY_RESERVATION_TTL` | `15m` | hàng giữ được bao lâu | ngắn quá ⇒ khách đang trả tiền thì hàng bị trả lại |
| `PROMOTION_LOCK_TTL` | `15m` | voucher giữ được bao lâu | như trên |
| `SAGA_SWEEP_INTERVAL` / `SHOPLAB_INVENTORY_SWEEP_INTERVAL_MS` / `SHOPLAB_PROMOTION_SWEEP_INTERVAL_MS` | `60s` / `60000` / `60000` | nhịp sweeper | sweeper chết ⇒ hàng treo tới khi có người `release` tay |
| `SHOPLAB_DEBUG_ENDPOINTS` | `true` (local) | bật `payment:/debug/faults` (bơm lỗi) | production **phải** `false` |

Tài khoản dev (chỉ `APP_ENV=local`): `seller@shoplab.local` / `admin@shoplab.local`, mật khẩu `matkhau123`.


## 3. Kiểm tra từng tầng (copy-paste)

```bash
# 0) 12 container còn sống?
docker compose -f infra/docker-compose.yml --env-file .env ps --format '{{.Name}}\t{{.Status}}'

# 1) gateway (cửa vào duy nhất) — 200 + route của phase 2
curl -s -o /dev/null -w 'gateway /health=%{http_code}\n' localhost:8080/health
curl -s localhost:8080/ready | python3 -m json.tool

# 2) 5 service của phase 2 (gọi THẲNG port — service tự verify JWT, defense in depth)
for p in 8083 8084 8086 8087 8088; do curl -s -o /dev/null -w "port $p health=%{http_code}\n" localhost:$p/health; done

# 3) DB — chỉ đọc, không sửa
docker exec shoplab-postgres psql -U shoplab -d inventory -c 'select sku_id, on_hand, reserved, version from stock order by updated_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d inventory -c 'select r.status, ri.qty, r.expires_at from reservations r join reservation_items ri on ri.reservation_id = r.id order by r.created_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d inventory -c 'select reason, delta_on_hand, delta_reserved from stock_ledger order by created_at desc limit 10'
docker exec shoplab-postgres psql -U shoplab -d promotion -c 'select code, used_count, quota from vouchers order by created_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d promotion -c 'select voucher_code, status, expires_at from voucher_locks order by created_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d order -c 'select id, status, total from orders order by created_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d order -c 'select order_id, state, failure_code from checkout_sagas order by created_at desc limit 5'
docker exec shoplab-postgres psql -U shoplab -d order -c 'select order_id, step, status, http_status, duration_ms from checkout_saga_steps order by created_at desc limit 10'
docker exec shoplab-postgres psql -U shoplab -d payment -c 'select id, status, amount from payments order by created_at desc limit 5'

# 4) Redis — giỏ hàng (cart KHÔNG có Postgres)
docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern 'cart:*' | head
docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning ttl 'cart:u:<userId>'

# 5) end-to-end — fail ở đâu in gợi ý kiểm tra ở đó
bash tools/e2e-smoke.sh            # 17 mục — Phase 1 (auth · catalog) không regress
bash tools/e2e-checkout.sh         # 27 mục — happy path: tiền · saga · idempotency · confirm
bash tools/e2e-checkout-fail.sh    # 41 mục — F1…F6 nhánh fail + compensating
```

**Cách đọc nhanh một ca "hàng không về"** — 3 con số đủ kết luận:

| Đọc gì | Câu hỏi | Kết luận |
|---|---|---|
| `stock.reserved` | có về 0 sau khi checkout fail? | > 0 ⇒ compensating **không** chạy, hoặc chưa tới lượt sweeper |
| `reservations.status` | `RELEASED` / `EXPIRED` / `PENDING`? | còn `PENDING` ⇒ chờ sweeper (≤ 60 s) hoặc TTL chưa hết |
| `stock_ledger` | có cặp `RESERVE` + `RELEASE`/`EXPIRE` khớp nhau? | lệch ⇒ bug thật, mở ticket (**đừng** sửa DB tay) |

## 4. Bảy lỗi thường gặp

| # | Triệu chứng | Lệnh kiểm tra | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|---|---|
| 1 | **409** khi checkout (không đủ hàng) | `psql -d inventory -c "select on_hand, reserved from stock where sku_id='…'"` | `reserved` đã chạm `on_hand` — **đúng thiết kế** chống oversell · hoặc SKU đã `ARCHIVED` | Không phải bug. Nếu `on_hand` còn dư mà vẫn 409 ⇒ kiểm row `reservations` treo (`PENDING` quá hạn) |
| 2 | **410** khi `commit` stock | `psql -d inventory -c "select status, expires_at from reservations …"` | Reservation quá TTL 15′ đã bị sweeper chuyển `EXPIRED` | Thiết kế đúng (lazy expire) — khách checkout lại. Hay gặp ⇒ tăng `INVENTORY_RESERVATION_TTL` cho khớp luồng thanh toán |
| 3 | Đơn treo **`PENDING_PAYMENT`** mãi | `psql -d order -c "select state, failure_code from checkout_sagas"` + `psql -d payment -c "select status from payments"` | Khách chưa trả tiền (bình thường — chờ sweeper) · webhook không tới · `PAYMENT_INTENT_TTL` ≠ `SAGA_PAYMENT_WINDOW` | Chờ sweeper (≤ 15′ + 60 s). Quá hạn vẫn `PENDING` ⇒ `docker logs shoplab-order-service \| grep -i sweep` xem sweeper có chạy |
| 4 | **`reserved` không về 0** sau khi fail | `psql -d inventory -c "select reserved from stock …"` + `stock_ledger` | Compensating lỗi (service chết đúng lúc release) · sweeper chết · reservation còn `PENDING` trong TTL | Chờ 60 s rồi đọc lại. Vẫn treo ⇒ `POST /v1/stock/release` bằng admin (có ledger) + mở ticket — **đừng** `update stock` tay (phá ledger, mất dấu vết) |
| 5 | **503** ở checkout | `docker ps \| grep -E 'cart\|catalog\|inventory\|promotion\|payment'` + đọc `message` (ghi rõ tên dependency) | Service đích chết · chậm quá `SAGA_READ_TIMEOUT=800ms` · sai `SAGA_*_URL` (phải là **tên service** trong compose, không phải `localhost`) | `docker compose up -d <service>` rồi chờ **`/ready` 200** — đừng tin `docker inspect` ngay sau `docker start` (còn báo `healthy` cũ tới hết 1 nhịp probe, xem §5) |
| 6 | **409** ở confirm dù khách đã "thanh toán" | `curl localhost:8088/v1/payment-intents/{id}` · `docker logs shoplab-payment-service \| grep -i webhook` | Payment chưa `SUCCEEDED`: webhook sai HMAC hoặc trễ quá `PAYMENT_WEBHOOK_TOLERANCE=5m` | Gọi lại `POST /mock-provider/pay/{intentId}` (local) rồi confirm; kiểm `PAYMENT_WEBHOOK_SECRET` khớp 2 phía |
| 7 | **409 / kẹt `IN_PROGRESS`** khi retry cùng `Idempotency-Key` | `psql -d order -c "select idempotency_key, status, created_at from idempotency_keys order by created_at desc limit 5"` | Lần trước chết giữa đường nên key còn `IN_PROGRESS` (job dọn chỉ xoá sau 1 h) | Tạm dùng key mới. Nếu lặp lại ⇒ nhánh fail **không** trả key (bug thật, đã sửa ở `SL-2.6a` — kiểm lại regression) |

Quy ước mã lỗi phase 2: **409** = xung đột dữ liệu (hết hàng · voucher hết lượt · state machine từ chối ·
idempotency kẹt) · **410** = reservation/intent đã hết hạn · **422** = giỏ rỗng · **503** = dependency chết
hoặc chậm quá timeout (retry được).



## 5. Ví dụ debug thật (làm theo runbook, dán nguyên văn)

**Giả lập**: tắt `payment-service` giữa saga ⇒ checkout phải `503` **và** hàng phải được trả lại.
Đây là ca đắt nhất của phase 2: chỉ nhìn HTTP status thì **không** biết compensating có chạy hay không.

```console
$ docker exec shoplab-postgres psql -U shoplab -Atc 'select on_hand, reserved from stock' -d inventory
10|0
$ docker stop shoplab-payment-service
$ curl -s -w '\nstatus=%{http_code}\n' -X POST localhost:8080/v1/checkout \
    -H "Authorization: Bearer $BUYER" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: rb-1794127442000" -d '{}'
{"code":"UNAVAILABLE","message":"[payment] tạo payment intent thất bại: I/O error on POST request for
 \"http://payment-service:8088/v1/payment-intents\": null","traceId":"2214be2e-…","path":"/v1/checkout"}
status=503
```

**Đọc state — phần quyết định:**

```console
$ docker exec shoplab-postgres psql -U shoplab -Atc \
    "select on_hand, reserved from stock where sku_id='$SKU'" -d inventory
10|0                                            ← hàng đã trả lại: compensating CHẠY
$ docker exec shoplab-postgres psql -U shoplab -Atc \
    "select r.status, ri.qty from reservations r join reservation_items ri on ri.reservation_id=r.id
      where ri.sku_id='$SKU' order by r.created_at desc limit 1" -d inventory
RELEASED|2                                      ← reservation đóng, không treo
$ docker exec shoplab-postgres psql -U shoplab -Atc \
    "select reason, delta_on_hand, delta_reserved from stock_ledger
      where sku_id='$SKU' order by created_at, id" -d inventory
ADJUST|10|0                                     ← admin set tồn kho
RESERVE|0|2                                     ← giữ 2
RELEASE|0|-2                                    ← trả 2 NGAY trong request (không chờ sweeper)
$ docker exec shoplab-postgres psql -U shoplab -Atc \
    'select state, failure_code from checkout_sagas order by created_at desc limit 1' -d order
FAILED_PAYMENT|DOWNSTREAM_UNAVAILABLE_PAYMENT   ← lý do fail là một field, không phải đoán từ log
```

**Bẫy khi khôi phục** (đã gặp thật): `docker start` xong checkout **vẫn 503** thêm ~10 s, dù
`docker inspect --format '{{.State.Health.Status}}'` đã trả `healthy` — vì đó là kết quả của **lần chạy
trước**, còn nguyên tới nhịp probe kế tiếp. Chờ `/ready` 200 thì mới chắc:

```console
$ docker start shoplab-payment-service
$ for i in $(seq 1 40); do curl -sf -o /dev/null localhost:8088/ready && break; sleep 1; done
$ curl -s -o /dev/null -w 'payment /ready=%{http_code}\n' localhost:8088/ready
payment /ready=200
$ curl -s -w '\nstatus=%{http_code}\n' -X POST localhost:8080/v1/checkout \
    -H "Authorization: Bearer $BUYER" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: rb2-1794127468000" -d '{}'
{"orderId":"9186eafa-…","status":"PENDING_PAYMENT","subtotal":200000.00,"discount":0,"total":200000.00,
 "currency":"VND","paymentRequired":true,"paymentIntentId":"aded8e70-…",
 "paymentUrl":"http://localhost:8080/mock-provider/pay/pi_mock_aded8e70-…"}
status=201
$ docker exec shoplab-postgres psql -U shoplab -Atc \
    'select state, failure_code from checkout_sagas order by created_at desc limit 1' -d order
COMPLETED_START|                                ← paymentRequired=true ⇒ chờ webhook rồi confirm
```

**Bài học ghi lại**: (1) `503` **không** chứng minh compensating đã chạy — phải đọc `stock.reserved` +
`stock_ledger`; (2) `failure_code` trong `checkout_sagas` biến "đoán từ log" thành field đọc được;
(3) health check của Docker **trễ 1 nhịp** sau restart ⇒ luôn chờ `/ready` của chính service.

## 6. Liên quan

- `tools/e2e-checkout.sh` — happy path 27 mục (tiền · saga · idempotency · confirm · tồn kho).
- `tools/e2e-checkout-fail.sh` — 41 mục nhánh fail F1…F6 (hết hàng · provider từ chối · 5xx · timeout ·
  dependency chết · retry sau fail); fail ở đâu in gợi ý `docker compose logs …` ở đó.
- `docs/runbook-phase-1.md` — auth/JWT/gateway/catalog (401 · 403 · 404 · 503).
- [ADR-0006](adr/0006-checkout-model.md) — vì sao reservation TTL + idempotency key + snapshot giá.
- `Backlog.md` § 7 — nợ đang mở liên quan: `TD-23` (IT inventory flaky) · `TD-25` (sweeper saga không có
  token user) · `TD-27` (`e2e-checkout-fail.sh` flaky ngay sau rebuild).
- Phase 5–6 sẽ mở rộng file này: alert symptom-based, dashboard, escalation.


