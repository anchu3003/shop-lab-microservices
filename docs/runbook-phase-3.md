# Runbook — Phase 3 (async & event-driven · RabbitMQ · outbox · DLQ)

> Dùng khi: **email không tới**, **DLQ tăng**, **order không tự `CONFIRMED`**, **search trả rỗng/lệch**,
> hoặc `tools/e2e-async.sh` đỏ.
> Nguyên tắc: async thì **lỗi không nằm trong response HTTP** — `201` chỉ nghĩa là "đã ghi DB + bỏ vào outbox".
> Muốn biết việc thật đã xảy ra chưa thì phải đọc **4 tầng**: outbox → queue → `processed_events` → bảng kết quả.
> Phase 1 (auth/JWT) và Phase 2 (checkout saga) đã có runbook riêng: [`runbook-phase-1.md`](runbook-phase-1.md) ·
> [`runbook-phase-2.md`](runbook-phase-2.md). File này **không** lặp lại 401/403 hay compensating của checkout.

## 1. Luồng event — ai phát, ai nhận

```text
                       ┌──────────────── exchange shoplab.events (topic, durable) ─────────────────┐
order-service  ──┐     │                                                                          │
  (outbox relay) ├─►───┤ rk=order.confirmed.v1  ──► q.notification ──► notification-service :8089 │
                 │     │ rk=order.cancelled.v1  ──► q.notification                                │
payment-service ─┤     │ rk=payment.succeeded.v1 ──► q.order        ──► order-service   :8087    │
  (outbox relay) │     │ rk=product.updated.v1   ──► q.search       ──► search-service    :8091   │
catalog-service ─┘     │ rk=product.archived.v1  ──► q.search                                    │
  (outbox relay)        └──────────────────────────────────────────────────────────────────────────┘
                             │ retry cạn (200 ms · 1 s · 3 s)
                             ▼
                       exchange shoplab.dlx (direct) ──► q.<consumer>.dlq   (KHÔNG auto-replay)
```

3 điểm bắt buộc phải nhớ (đều là nguồn của lỗi "im lặng"):

| Điều | Hệ quả khi quên |
|---|---|
| Producer **không** publish trực tiếp — chỉ ghi bảng `outbox` **cùng transaction** với dữ liệu | Ghi DB xong mà event mất; hoặc event phát cho dữ liệu chưa commit |
| `eventId = outbox.id` (relay dùng id của row) | Trace event: lấy `outbox.id`, **không** có `eventId` trong `payload` |
| Consumer dedupe theo `(event_id, consumer)` trong `processed_events` | Retry/replay ⇒ email gửi 2 lần |

## 2. Bảng tra nhanh

| Thành phần | Producer? | Queue | DB (bảng cần xem) | Log |
|---|---|---|---|---|
| `order-service` :8087 | ✅ `order.confirmed.v1` · `order.cancelled.v1` | — | `order`: `outbox` · `orders` · `order_status_history` | `docker logs shoplab-order-service` |
| `payment-service` :8088 | ✅ `payment.succeeded.v1` | — | `payment`: `outbox` · `payments` | `docker logs shoplab-payment-service` |
| `catalog-service` :8082 | ✅ `product.updated.v1` · `product.archived.v1` | — | `catalog`: `outbox` · `products` | `docker logs shoplab-catalog-service` |
| **`notification-service`** :8089 | — | `q.notification` (+ `.dlq`) | `notification`: `notification_logs` · `processed_events` · `notification_templates` | `docker logs shoplab-notification-service` |
| **`order-service`** (consumer) | — | `q.order` (+ `.dlq`) | `order`: `processed_events` · `orders` · `order_status_history` | như trên |
| **`search-service`** :8091 | — | `q.search` (+ `.dlq`) | `search`: `search_product` · `processed_events` | `docker logs shoplab-search-service` |
| RabbitMQ | — | 6 queue | management UI `http://localhost:15672` | `docker logs shoplab-rabbitmq` |

**Knob** (env, đổi phải **recreate** container — property bind lúc khởi động, không có endpoint runtime):

| Env | Default | Ý nghĩa |
|---|---|---|
| `SHOPLAB_OUTBOX_INTERVAL` | `1s` | nhịp relay quét outbox (**trần throughput** = `batch` × `1/interval`) |
| `SHOPLAB_OUTBOX_BATCH_SIZE` | `100` | số row mỗi nhịp |
| `SHOPLAB_OUTBOX_CLAIM_TIMEOUT` | `30s` | quá hạn ⇒ relay khác được claim lại (visibility timeout) |
| `SHOPLAB_NOTIFICATION_RETRY_DELAYS` | `200ms,1s,3s` | backoff trước khi nack ⇒ DLQ |
| `SHOPLAB_NOTIFICATION_FAIL_MODE` | `false` | **bơm lỗi có chủ đích** để thử DLQ (chỉ có tác dụng khi `APP_ENV=local`) |
| `SHOPLAB_SEARCH_RECONCILE_BATCH` | `100` | batch của reindex (gọi tay `/v1/admin/search/reindex`) |
| `SHOPLAB_SEARCH_DRIFT_INTERVAL` | `10m` | nhịp job so số row index vs catalog (chỉ **WARN**, không tự sửa) |
| `SHOPLAB_RETENTION_SWEEP_INTERVAL` | `1h` | nhịp job dọn bảng phụ (mọi service) — `fixedDelay` |
| `SHOPLAB_OUTBOX_RETENTION` | `7d` | giữ row `outbox` `SENT` bao lâu (`PENDING` **không bao giờ** bị xoá) |
| `SHOPLAB_EVENTS_DEDUP_RETENTION` | `7d` | giữ `processed_events` bao lâu — **phải ≥** `SHOPLAB_OUTBOX_RETENTION` |
| `SHOPLAB_NOTIFICATION_LOG_RETENTION` | `90d` | giữ `notification_logs` (dữ liệu nghiệp vụ) |



## 3. Kiểm tra từng tầng (copy-paste được)

Đi **từ ngoài vào trong**: broker → outbox → consumer → bảng kết quả. Gặp tầng nào sai thì dừng ở đó.

### 3.1. Tầng broker — topology có đúng không?

```console
$ bash tools/rabbit-topology.sh
== Exchanges (shoplab.*)
  shoplab.dlx          type=direct   durable=True
  shoplab.events       type=topic    durable=True

== Queues (ready = chờ consumer · unacked = consumer đang giữ/đang retry)
  q.notification         ready=  0 unacked=  0 consumers=1
  q.notification.dlq     ready=  0 unacked=  0 consumers=0
  q.order                ready=  0 unacked=  0 consumers=1
  q.order.dlq            ready=  0 unacked=  0 consumers=0
  q.search               ready=  0 unacked=  0 consumers=1
  q.search.dlq           ready=  0 unacked=  0 consumers=0

== Bindings
  shoplab.dlx    --q.notification            --> q.notification.dlq
  shoplab.dlx    --q.order                   --> q.order.dlq
  shoplab.dlx    --q.search                  --> q.search.dlq
  shoplab.events --order.cancelled.v1        --> q.notification
  shoplab.events --order.confirmed.v1        --> q.notification
  shoplab.events --payment.succeeded.v1      --> q.order
  shoplab.events --product.archived.v1       --> q.search
  shoplab.events --product.updated.v1        --> q.search
```

Đọc 3 cột của queue: `consumers=0` ⇒ **không ai tiêu thụ** (service chết / đang restart / `auto-startup=false`);
`ready` tăng liên tục ⇒ consumer chết hoặc chậm; `unacked > 0` lâu ⇒ consumer đang **retry** hoặc thread kẹt.

> ⚠️ **Ngay sau `docker compose up` topology có thể RỖNG** (`chưa có queue nào — service có AMQP đã chạy chưa?`) vì
> topology do **service khai báo lúc khởi động** (`EventTopologyDeclarer`), không phải do broker tự có. Trên máy này
> phải chờ **~45 s**. Đừng kết luận "mất topology" khi mới `up` 5 s.


```console
$ curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASSWORD" \
    'http://localhost:15672/api/queues/%2F/q.notification' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print({k:d[k] for k in ("name","messages_ready","messages_unacknowledged","consumers")})'
{'name': 'q.notification', 'messages_ready': 0, 'messages_unacknowledged': 0, 'consumers': 1}
```

> ⚠️ Management API tổng hợp số liệu **~5 s/lần** ⇒ ngay sau restart broker/consumer, `ready`/`consumers` có thể
> là số **cũ**. Cần chắc thì dùng `tools/rabbit-topology.sh drain <queue>` (ack thật) hoặc chờ 5 s đọc lại.

### 3.2. Tầng DLQ — message nào đã cạn kiên nhẫn?

```console
$ bash tools/dlq.sh list
  q.notification.dlq       ready=  1
  q.order.dlq              ready=  0
  q.search.dlq             ready=  0
$ bash tools/dlq.sh peek q.notification          # KHÔNG consume
  routing_key(hiện tại)=q.notification · routing key GỐC=order.confirmed.v1
  payload={"eventId":"a43d3d11-…","eventType":"order.confirmed.v1","version":1,…}
$ bash tools/dlq.sh move q.notification          # replay 1 message về queue chính
  replay 1 message từ q.notification.dlq → shoplab.events (routing key GỐC = order.confirmed.v1) → {"routed":true}
```

**Quy tắc**: `peek` → sửa nguyên nhân → `move`. Replay trước khi sửa thì message quay lại DLQ sau ~4,2 s (retry

### 3.3. Tầng outbox — event đã ra khỏi DB chưa?

```console
$ docker exec shoplab-postgres psql -U shoplab -d order -tAc \
    "select id, status, event_type, attempts, last_error, to_char(sent_at,'HH24:MI:SS') from outbox
      where aggregate_id='<orderId>'"
 a43d3d11-f4a5-40dc-aecb-fa8c1442ae2a | SENT | order.confirmed.v1 | 1 |  | 13:46:32
```

`PENDING` + `sent_at NULL` ⇒ **relay chưa gửi**. `attempts` tăng + `last_error` có nội dung ⇒ broker/credential sai.
Đếm nhanh cả 3 service có outbox:

```console
$ for db in order catalog payment; do
    docker exec shoplab-postgres psql -U shoplab -d $db -tAc \
      "select '$db outbox: ' || status || ' ' || count(*) from outbox group by status"; done
order outbox: SENT 7
catalog outbox: SENT 8
payment outbox: SENT 5
```

> `eventId` của message **chính là** `outbox.id` (relay dùng id row — ADR-0007 D3). `payload` trong outbox chỉ chứa
> dữ liệu nghiệp vụ, **không** có `eventId` ⇒ muốn đối chiếu với `processed_events` thì lấy `outbox.id`.

### 3.4. Tầng consumer — đã xử lý chưa (và có xử lý 2 lần không)?

```console
$ docker exec shoplab-postgres psql -U shoplab -d notification -tAc \
    "select event_id, consumer, event_type, to_char(processed_at,'HH24:MI:SS')
       from processed_events where event_id='a43d3d11-…'"
a43d3d11-…|notification-service|order.confirmed.v1|13:46:50
$ docker exec shoplab-postgres psql -U shoplab -d notification -tAc \
    "select event_id, template, status, recipient, to_char(sent_at,'HH24:MI:SS')
       from notification_logs where order_id='<orderId>'"
a43d3d11-…|order.confirmed|SENT|user-61063b1c@shoplab.local|13:46:50
```

| Hiện tượng | Nghĩa |
|---|---|
| `processed_events` **có** row, `notification_logs` **không** | handler chạy nhưng ghi log thất bại, hoặc bị dedupe chặn từ lần trước (kiểm `notification_logs.event_id`) |
| `processed_events` **không** có row | consumer chưa nhận, hoặc **fail rồi marker bị xoá đúng** (marker chỉ giữ khi xử lý xong) |
| `notification_logs` có 2 row cùng `order_id` | dedupe hỏng — đối chiếu `event_id` với `outbox.id` |
| Log có lỗi UNIQUE của `notification_logs.event_id` | chốt chặn cuối ở DB đã bắt được 2 consumer cùng chạy |

API của chính người dùng (không có endpoint xem của người khác, kể cả admin):

```console
$ curl -s localhost:8080/v1/notifications/me -H "Authorization: Bearer $BUYER"
[]
status=200
```

### 3.5. Tầng index — search lệch/rỗng?

```console
$ docker exec shoplab-postgres psql -U shoplab -d search -tAc \
    "select status, count(*) from search_product group by 1 order by 1"
ACTIVE|25
ARCHIVED|1
$ curl -s 'localhost:8080/v1/search?q=ao%20thun&size=1'
{"content":[{"productId":"22222222-…-000000000011","name":"Ao thun oversize","minPrice":249000.00,…}],…}
```

Index **rỗng sau khi dựng lại volume** là bình thường (index chỉ được bơm bằng event hoặc reindex tay):

```console
$ curl -s -X POST localhost:8080/v1/admin/search/reindex -H "Authorization: Bearer $ADMIN"
{"scanned":25,"upserted":25,"archived":0}
```

### 3.6. Tầng nghiệp vụ — order có tự chốt không?

```console
$ docker exec shoplab-postgres psql -U shoplab -d order -tAc \
    "select to_char(created_at,'HH24:MI:SS'), coalesce(from_status,'(new)') || ' → ' || to_status, reason
       from order_status_history where order_id='<orderId>' order by created_at"
13:46:31|(new) → PENDING_PAYMENT|
13:46:31|PENDING_PAYMENT → CONFIRMED|thanh toán đã xác nhận
```

`PENDING_PAYMENT` **không** chuyển tiếp sau khi provider đã báo thanh toán ⇒ event `payment.succeeded.v1` chưa tới
`q.order` (kiểm 3.1) hoặc `q.order` không có consumer (kiểm `consumers=0`).

### 3.7. Tầng retention — bảng phụ có được dọn không?

Job dọn chạy **in-process** (`@Scheduled`, `fixedDelay` = `SHOPLAB_RETENTION_SWEEP_INTERVAL`, mặc định 1 h) ở
`order` · `catalog` · `payment` (outbox) · `notification` (`notification_logs` + dedupe) · `search` (dedupe).

```console
$ docker exec shoplab-postgres psql -U shoplab -d order -c \
    "select status, count(*), min(sent_at), max(sent_at) from outbox group by status"
 status | count |              min              |             max
--------+-------+-------------------------------+------------------------------
 SENT   |    16 | 2026-11-11 13:46:32.281395+00 | 2026-11-11 16:09:20.93792+00
```

Đọc: `SENT` mà `sent_at` cũ hơn **7 ngày** thì job **phải** đã xoá (hoặc job chưa chạy ⇒ xem log).

```console
$ for db in order notification search; do
    docker exec shoplab-postgres psql -U shoplab -d $db -tAc \
      "select '$db: ' || count(*) || ' row cũ hơn 7 ngày' from processed_events where processed_at < now() - interval '7 days'"; done
order: 0 row cũ hơn 7 ngày
notification: 0 row cũ hơn 7 ngày
search: 0 row cũ hơn 7 ngày
```

> ⚠️ **Đừng chèn row `PENDING` giả vào `outbox` để thử** khi relay đang chạy: relay sẽ **publish** nó ngay (đó là việc của
> nó), và nếu routing key không có binding thì message mất — trạng thái bạn muốn kiểm sẽ biến mất vì lý do **khác** với
> retention. Muốn kiểm "PENDING không bị xoá" thì tắt relay (`SHOPLAB_EVENTS_ENABLED=false`) hoặc assert ngay trong IT
> (đã làm ở `OrderServiceApplicationIT.retentionDeletesOnlyOldSentRows`).

**Job có chạy không** — grep log (chỉ log khi có row bị xoá, để không spam):

```console
$ docker logs --since 2h shoplab-order-service 2>&1 | grep 'outbox retention'
… "msg":"outbox retention: đã xoá 3 row SENT cũ hơn 2026-11-04T10:00:00Z (giữ PT168H · batch 1000)"
$ docker logs --since 2h shoplab-notification-service 2>&1 | grep 'notification_logs retention'
```

| Hiện tượng | Nghĩa / xử lý |
|---|---|
| Không có log retention **và** có row quá hạn | Job chưa tới nhịp (chờ `interval`), hoặc service không có `@EnableScheduling`, hoặc cấu hình sai (log ERROR khi lỗi) |
| Có log nhưng bảng vẫn to | Bảng lớn hơn `batch` ⇒ mỗi nhịp chỉ xoá `batch` row; nâng `SHOPLAB_RETENTION_SWEEP_INTERVAL`/batch, và kiểm index `idx_outbox_sent` có tồn tại |
| `PENDING` bị xoá | **Không thể xảy ra** theo thiết kế (điều kiện `status='SENT'`); nếu thấy ⇒ dừng job, kiểm ngay bản deploy |
| Replay DLQ sau nhiều ngày bị xử lý **2 lần** | `SHOPLAB_EVENTS_DEDUP_RETENTION` < `SHOPLAB_OUTBOX_RETENTION` ⇒ marker dedupe đã hết hạn. Đây là bất biến cấu hình, không phải bug code |



## 4. Bảng lỗi — triệu chứng người trực thấy trước

| # | Triệu chứng | Kiểm tra | Nguyên nhân thường gặp | Xử lý |
|---|---|---|---|---|
| **1** | Đơn `CONFIRMED` nhưng **không có email** | 3.2 → 3.4: `q.notification.dlq`? · `processed_events` có `outbox.id`? · `notification_logs`? | (a) DLQ có message ⇒ consumer lỗi (xem #2) · (b) `consumers=0` ⇒ notification-service chết/restart · (c) đang bật `SHOPLAB_NOTIFICATION_FAIL_MODE=true` | Replay: sửa nguyên nhân → `tools/dlq.sh move q.notification`; (c) thì `SHOPLAB_NOTIFICATION_FAIL_MODE=false docker compose … up -d notification-service` |
| **2** | `q.notification.dlq ready` **> 0** | `bash tools/dlq.sh peek q.notification` (đọc `x-death`) | payload/DB lỗi thật (user không tồn tại, template thiếu, DB notification chết) | `peek` → sửa → `move`. **Không** `purge` khi chưa đọc: purge = mất event, không có nguồn khác |
| **3** | Thanh toán xong nhưng order **vẫn `PENDING_PAYMENT`** | 3.1 `q.order` (consumers/ready) · 3.6 `order_status_history` · 3.3 outbox `payment` | (a) `payment.succeeded.v1` chưa phát (outbox `PENDING`) · (b) `q.order` không có consumer · (c) consumer lỗi ⇒ DLQ `q.order.dlq` | (a) xem #5 · (b) `docker start shoplab-order-service` + chờ `/ready` · (c) `tools/dlq.sh move q.order` sau khi sửa |
| **4** | Search **không thấy** product mới / trả rỗng | 3.5: `search_product` count · `processed_events` ở DB `search` · `docker logs shoplab-search-service` | (a) index rỗng sau `docker compose down -v` (bình thường) · (b) event `product.updated.v1` chưa tới · (c) product bị `ARCHIVED` (index ẩn, **không** xoá row) | (a) `POST /v1/admin/search/reindex` · (b) xem 3.1 `q.search` · (c) kiểm `status` trong `search_product` |
| **5** | Outbox **`PENDING` tăng dần**, `sent_at NULL` | 3.3 (`attempts`, `last_error`) · `docker logs shoplab-order-service \| grep -i relay` | (a) RabbitMQ chết/sai credential ⇒ relay không publish được · (b) relay không chạy (log không có `outbox relay: N / M message đã gửi`) · (c) `claim-timeout` quá ngắn ⇒ 2 relay giành nhau | (a) `docker start shoplab-rabbitmq`, kiểm `RABBITMQ_*` trong `.env` · (b) restart service · (c) nâng `SHOPLAB_OUTBOX_CLAIM_TIMEOUT` |
| **6** | **Email 2 lần** cho cùng 1 đơn | 3.4: `select event_id, count(*) from notification_logs where order_id=… group by 1` · so `event_id` với `outbox.id` | (a) relay sinh `eventId` **mới** mỗi lần thử (đã chống: `eventId = outbox.id`) · (b) consumer không dedupe (thiếu row trong `processed_events`) · (c) replay DLQ khi event **đã** xử lý xong | (a)/(b) là **bug code** → ticket mới · (c) trước khi `move`, kiểm event đã có trong `notification_logs` chưa |
| **7** | Queue **`ready` tăng**, `consumers=0` | 3.1 (management API + chờ 5 s đọc lại) · `curl -s -o /dev/null -w '%{http_code}' localhost:8089/ready` | service chết / crash-loop / đang restart (Flyway, thiếu env) | `docker logs shoplab-notification-service`, sửa rồi `up -d`; message **không mất** (durable queue) — xử lý tiếp khi consumer sống lại |
| **8** | `tools/dlq.sh move` báo `{"routed":false}` | `peek` xem routing key gốc | routing key gốc không còn binding (đổi tên event mà không cập nhật binding, hoặc `SHOPLAB_EVENTS_TOPOLOGY_ENABLED=false`) | `bash tools/rabbit-topology.sh` so binding; message đã bị `ack` khỏi DLQ ⇒ **mất** nếu `routed=false` (peek trước, move sau) |


## 5. Một ca debug thật — "email không tới" (dán nguyên văn)

Bối cảnh: bật `SHOPLAB_NOTIFICATION_FAIL_MODE=true` (bơm lỗi) để mô phỏng consumer hỏng, rồi checkout 1 đơn
bình thường. Client **không** thấy gì bất thường — đây chính là điểm khó của async.

```console
$ SHOPLAB_NOTIFICATION_FAIL_MODE=true docker compose -f infra/docker-compose.yml --env-file .env \
    up -d notification-service
$ docker exec shoplab-notification-service env | grep FAIL_MODE
SHOPLAB_NOTIFICATION_FAIL_MODE=true

$ curl -s -o /dev/null -w 'checkout=%{http_code}\n' -X POST localhost:8080/v1/checkout …
checkout=201
$ curl -s -o /dev/null -w 'confirm=%{http_code}\n' -X POST localhost:8080/v1/checkout/<orderId>/confirm …
confirm=200                     ← ĐƯỜNG CHÍNH VẪN 200: async không chặn checkout
```

**Bước 1 — log consumer: 4 lần thử (1 lần đầu + 3 retry) rồi nack:**

```console
$ docker logs --since 3m shoplab-notification-service | grep -E 'lỗi xử lý|DLQ'
… "msg":"[notification-service] lỗi xử lý a43d3d11-… (lần 1): bơm lỗi theo SHOPLAB_NOTIFICATION_FAIL_MODE (order.confirmed) — thử lại sau 200 ms"
… "msg":"[notification-service] lỗi xử lý a43d3d11-… (lần 2): … — thử lại sau 1000 ms"
… "msg":"[notification-service] lỗi xử lý a43d3d11-… (lần 3): … — thử lại sau 3000 ms"
… "msg":"[notification-service] xử lý event a43d3d11-… thất bại sau 4 lần thử → DLQ"
```

**Bước 2 — broker: queue chính rỗng, DLQ có 1 message:**

```console
$ bash tools/rabbit-topology.sh dlq
  q.notification.dlq     ready=1
  q.order.dlq            ready=0
  q.search.dlq           ready=0
```

**Bước 3 — DB: email không có, outbox vẫn `SENT`, order vẫn `CONFIRMED`:**

```console
$ psql -d notification -tAc "select count(*) from notification_logs where order_id='f5411c82-…'"
0
$ psql -d notification -tAc "select count(*) from processed_events where event_id='a43d3d11-…'"
0                                ← marker KHÔNG được ghi: dedupe chỉ chặn khi đã xử lý XONG
$ psql -d order -tAc "select id, status, event_type, attempts from outbox where aggregate_id='f5411c82-…'"
a43d3d11-f4a5-40dc-aecb-fa8c1442ae2a | SENT | order.confirmed.v1 | 1
$ psql -d order -tAc "select status from orders where id='f5411c82-…'"
CONFIRMED
```

**Bước 4 — đọc message trong DLQ (không consume) → sửa nguyên nhân → replay:**

```console
$ bash tools/dlq.sh peek q.notification
  routing_key(hiện tại)=q.notification · routing key GỐC=order.confirmed.v1
  payload={"eventId":"a43d3d11-…","eventType":"order.confirmed.v1","version":1,"occurredAt":…,
           "producer":"order-service","traceId":null,"payload":{"total":300000.0,…,"orderId":"f5411c82-…"}}

$ SHOPLAB_NOTIFICATION_FAIL_MODE=false docker compose -f infra/docker-compose.yml --env-file .env \
    up -d notification-service                      # sửa nguyên nhân TRƯỚC khi replay
$ bash tools/dlq.sh move q.notification
  replay 1 message từ q.notification.dlq → shoplab.events (routing key GỐC = order.confirmed.v1) → {"routed":true}
```

**Bước 5 — xác nhận đã xong (email có, dedupe có, DLQ rỗng):**

```console
$ psql -d notification -tAc "select template, recipient, status from notification_logs where order_id='f5411c82-…'"
order.confirmed|user-61063b1c@shoplab.local|SENT
$ psql -d notification -tAc "select consumer, event_type from processed_events where event_id='a43d3d11-…'"
notification-service|order.confirmed.v1
$ bash tools/rabbit-topology.sh dlq
  q.notification.dlq     ready=0
```

**Bốn điều ca này dạy (rất dễ đọc sai):**

1. `201`/`200` ở đường chính **không** nói gì về email — async chỉ hứa "event đã vào outbox", không hứa "đã xử lý".
2. `outbox.status = SENT` nghĩa là **đã publish**, không phải "đã xử lý xong" — `processed_events` mới trả lời câu đó,
   và nó **rỗng đúng thiết kế** khi handler fail (marker chỉ ghi khi xong, nếu không thì replay bị chính marker chặn).
3. Log ghi "thất bại sau **4** lần thử" dù cấu hình là **3** retry — lần đầu + 3 retry = 4. Không hiểu con số này là đi
   tìm "lần thứ 4 ở đâu ra".
4. `peek` trước `move`: `move` **ack khỏi DLQ** — nếu routing key gốc sai binding thì message mất luôn, không có lần hai.

## 6. Liên quan

- [`runbook-phase-2.md`](runbook-phase-2.md) — checkout saga · compensating · `stock.reserved`/`voucher.locked`.
- [`runbook-phase-1.md`](runbook-phase-1.md) — auth/JWT/gateway (401 · 403 · 404).
- [`docs/reports/async-phase-3.md`](reports/async-phase-3.md) — số đo của phase (latency · độ trễ event · throughput
  relay · DLQ dưới tải) + **8 giới hạn đã biết** của thiết kế hiện tại.
- [ADR-0007](adr/0007-event-contract.md) — envelope · naming · outbox relay polling · DLQ policy · dedupe.
- `tools/rabbit-topology.sh` · `tools/dlq.sh` · `tools/e2e-async.sh` · `tools/bench-async.sh`.
- `Backlog.md` § 7 — nợ đang mở liên quan: `TD-29` (outbox không có job dọn) · `TD-30` (`processed_events` /
  `notification_logs` không có job dọn) · `TD-33` (`min_price` chưa tính SKU override).
- Phase 6 sẽ bổ sung metric/alert thay cho việc "phải đi đọc 4 tầng bằng tay" như file này.

200 ms + 1 s + 3 s), và mỗi lần replay là một lần gửi email thật nếu nguyên nhân đã hết.
