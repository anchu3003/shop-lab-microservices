# Baseline Phase 4 — điểm vỡ của hệ **trước khi** tối ưu (`SL-4.1`)

> Mọi số dưới đây lấy từ `bash tools/load.sh --scenario …` trên stack `docker compose` tại commit
> **`3a05d7f`** (chưa có cache / rate limit / read replica — đúng nghĩa baseline).
> Raw: `/tmp/load-<scenario>-<ts>/` (mỗi bậc 1 thư mục: `summary.json` · `k6.log` · `cpu.txt` ·
> `pg.txt` · `exit-code.txt`). Chạy lại: xem §8.

## TL;DR

| Câu hỏi | Trả lời bằng số |
|---|---|
| Điểm vỡ đầu tiên ở đâu? | **Đọc: 200 VU** (catalog p99 **190 ms** > 150 · search p99 **177 ms**) · **Ghi: 100 VU** (checkout p99 **762 ms** > 500) |
| RPS ngay **trước** điểm vỡ | catalog **3.796** · search **4.758** · checkout **225** req/s |
| Quy đổi NFR 50.000 RPS | **1/13** (catalog) · **1/11** (search) · **1/222** (checkout) |
| Nghẽn ở đâu? | Đọc: CPU `catalog-service` + `api-gateway` (hàng đợi chung — p50 của **3 endpoint giống hệt nhau**) · Ghi: chuỗi saga đồng bộ (`order-service` + Postgres + RabbitMQ) |
| DB có phải nghẽn? | **Không**: `lock-wait = 0`, buffer hit ratio **99,9–100%**, `conn` 18–34 (pool chưa cạn), `active` đỉnh 8 |
| k6 có phải điểm vỡ (client)? | **Không**: CPU k6 14–168% (0,14–1,7 core / 12 core), ngưỡng cảnh báo 800% |
| Có lỗi 5xx/409/422 không? | **Không**: error **0,00%** ở **mọi** bậc của cả 3 scenario ⇒ vỡ là do **latency**, không phải do lỗi |

**Điều đáng nhớ nhất**: checkout bão hoà ở **225 req/s** — chỉ **1/222** mục tiêu NFR. Đây là con số
định lượng cho câu "saga đồng bộ đắt": 3 scenario đọc đạt 1/11–1/13, ghi đạt 1/222 ⇒ **chênh ~18×**.
Mọi tối ưu Phase 4 (`SL-4.2` cache, `SL-4.3` rate limit, `SL-4.7/4.8` flash sale) phải so với bảng này.

## 1. Điều kiện đo

| Thông số | Giá trị |
|---|---|
| commit | `3a05d7f` (Phase 4 chưa sửa code service nào) |
| thời điểm | 2026-11-12, 14:36–15:10 UTC |
| máy | WSL2 · Intel i5-12600K · **12 thread** · 19,5 GB · Docker Engine 29.9.3 |
| stack | `docker compose` 13 container: Postgres 16 · Redis 7 · RabbitMQ 4 · 10 service Java (JVM mặc định) |
| client | `grafana/k6:latest` chạy **trong** network `shoplab-net` (gọi `http://api-gateway:8080`), không cài vào máy |
| dataset | seed `tools/load/seed.sh`: 20 category · **1.000 product** · **2.000 SKU** · **400 buyer** (200 theo chuẩn + 200 đệm) · tồn kho 1.000/SKU · index search 1.029 row |
| seed fingerprint | `d1412cc7427a84a8` (chạy seed lần 2 ra **đúng** số này ⇒ không tạo row mới) |
| bậc VU | `50 → 100 → 200 → 400`, mỗi bậc **hold 60 s** sau ramp 5 s |
| cửa sổ đo | **65 s** /bậc ⇒ RPS = `số request ÷ 65`. Ramp 5 s làm pha loãng ≤ **8%** (nêu rõ, không giấu) |
| ngưỡng vỡ | `error > 1%` **hoặc** p99 > NFR (read **150 ms**, checkout **500 ms**) |
| warm-up | 10 VU × 20 s trước bậc 50 (JVM/connection pool nóng) — **không** tính vào bảng |
| p99 | percentile do k6 tính (`summaryTrendStats` khai `p(95)`, `p(99)`) |

## 2. `read-catalog` — 60% list · 20% detail · 20% batch SKU

| VU | RPS | p50 (ms) | p95 (ms) | p99 (ms) | error % | k6 CPU max % |
|---|---|---|---|---|---|---|
| 50 | **3.842,9** | 11,9 | 25,5 | 35,2 | 0,00 | 125 |
| 100 | **3.795,6** | 24,5 | 57,2 | 81,4 | 0,00 | 132 |
| 200 | 3.505,0 | 53,7 | 132,2 | **189,9** ❌ | 0,00 | 149 |
| 400 | — | — | — | — | — | *không chạy: đã vỡ ở 200* |

- **Điểm vỡ: 200 VU** — p99 189,9 ms > 150 ms (error vẫn 0%).
- **RPS ngay trước điểm vỡ: 3.795 req/s ⇒ 1/13 mục tiêu NFR**; raw `/tmp/load-read-catalog-1794494252`.

**p99 theo endpoint** (đây là chỗ đáng đọc nhất):

| VU | `products_list` | `product_detail` | `skus_lookup` (10 id) |
|---|---|---|---|
| 50 | 35 | 35 | 35 |
| 100 | 81 | 80 | 82 |
| 200 | **190** | **188** | **191** |

Ba endpoint **khác nhau về chi phí query** (list = 1 query + count, detail = 2 query, batch = 1
`IN (10)`) nhưng p99 chênh nhau **< 2%** ⇒ độ trễ **không** đến từ query nào cả, nó đến từ **hàng đợi
phía trước** (CPU/thread pool của gateway + catalog). Nếu nghẽn là 1 câu query thì chỉ endpoint đó
chậm — số đo nói ngược lại. Đây là bằng chứng để **không** đi tối ưu SQL trong Phase 4.

## 3. `read-search` — `GET /v1/search?q=…`

| VU | RPS | p50 (ms) | p95 (ms) | p99 (ms) | error % | k6 CPU max % |
|---|---|---|---|---|---|---|
| 50 | **4.917,3** | 8,4 | 21,7 | 30,7 | 0,00 | 145 |
| 100 | **4.757,5** | 17,5 | 51,9 | 75,7 | 0,00 | 152 |
| 200 | 4.598,3 | 31,0 | 118,9 | **177,3** ❌ | 0,00 | 168 |

- **Điểm vỡ: 200 VU** — p99 177,3 ms > 150 ms.
- **RPS ngay trước điểm vỡ: 4.758 req/s ⇒ 1/11 mục tiêu NFR**. Đây là scenario **nhanh nhất** của hệ:
  `plainto_tsquery` trên index GIN **1.029 row** nằm trọn trong shared buffer ⇒ p50 **8,4 ms** ở 50 VU.
  ⚠️ Index 1.029 row là **nhỏ**: khi dữ liệu thật lớn hơn, con số này sẽ xấu đi — đừng trích
  "search chịu được 4,7k RPS" mà bỏ mất điều kiện dataset.
- So với catalog: search nhanh hơn ~30% ở cùng VU nhưng **vỡ ở cùng bậc** ⇒ hệ vỡ theo **tổng tải**,
  không theo endpoint riêng lẻ.

## 4. `checkout` — `POST /v1/checkout` (1 request/iteration)

| VU | RPS | p50 (ms) | p95 (ms) | p99 (ms) | error % | k6 CPU max % |
|---|---|---|---|---|---|---|
| 50 | **225,2** | 209,6 | 314,7 | 370,9 | 0,00 | 14 |
| 100 | 230,6 | 411,6 | 649,6 | **761,5** ❌ | 0,00 | 15 |
| 200/400 | — | — | — | — | — | *không chạy: đã vỡ ở 100* |

- **Điểm vỡ: 100 VU** — p99 761,5 ms > 500 ms.
- **RPS ngay trước điểm vỡ: 225 req/s ⇒ 1/222 mục tiêu NFR**; raw `/tmp/load-checkout-1794494968`.
- **Dấu hiệu bão hoà kinh điển**: VU ×2 (50→100) mà RPS **+2,4%** (225,2 → 230,6) trong khi **p50 ×1,96**
  (209,6 → 411,6 ms). Throughput không tăng, chỉ thời gian chờ tăng ⇒ hệ đã ở trần, request xếp hàng.

### Vì sao 225 req/s là con số quan trọng nhất của báo cáo

Mỗi checkout là **1 saga đồng bộ**: `GET cart` → `POST /v1/skus` (giá) → `POST /v1/stock/reserve` →
`POST /v1/payment-intents` → insert order + saga + outbox. Ở 225 req/s ⇒ **~1.100 lời gọi HTTP nội bộ/s**
+ ~225 transaction Postgres/s (mỗi transaction nhiều statement). Cùng một máy: đọc **3,8k RPS**, ghi
**0,23k RPS** — chênh **16×**. Đó chính là lý do Phase 4 tồn tại.

## 5. Nghẽn đầu tiên ở đâu — bằng chứng

Cách đo: probe riêng 1 bậc đúng bằng bậc vỡ (200 VU catalog / 100 VU checkout) và sample
`docker stats --no-stream` **mỗi 3 s** cho **mọi** container (không chỉ k6). Số dưới là **đỉnh**.
`CPUPerc` của Docker tính theo **1 core** ⇒ 286% = **2,86 core**.

**`read-catalog` @200 VU** (tái lập: RPS 3.442 · p99 184,9 ms — khớp lần ramp 3.505 / 189,9 ms):

| Container | CPU đỉnh | Đọc |
|---|---|---|
| `catalog-service` | **286%** | đắt nhất — JPA + serialize JSON cho list/detail |
| `api-gateway` | **248%** | mỗi request đi qua nó: JWT verify + proxy |
| `postgres` | **207%** | 2 core cho ~3,4k RPS × ~1,5 query |
| k6 (client) | 195% | **1/6** ngưỡng cảnh báo ⇒ không phải client |
| redis | 4% | chưa dùng ở đường đọc (Phase 4 mới thêm cache) |

**`checkout` @100 VU** (tái lập: RPS 229,8 · p99 764 ms — khớp 230,6 / 761,5):

| Container | CPU đỉnh | Đọc |
|---|---|---|
| `order-service` | **273%** | orchestrate 5 lời gọi + ghi order/saga/outbox |
| `postgres` | **208%** | transaction của saga + `SELECT … FOR UPDATE` của inventory |
| `rabbitmq` | **152%** | outbox relay publish event `order.*` |
| `inventory-service` | 47% | reserve/commit — khoá theo SKU nên không nghẽn toàn cục |
| `api-gateway` | 46% | không phải nút cổ chai ở đường ghi |
| `payment-service` | 36% | mock provider, rất nhẹ |
| `identity-service` | 372% **chỉ 2 sample đầu** | **Bẫy đọc số**: đây là `setup()` login 400 buyer bằng BCrypt trong ~15 s đầu, **không phải** tải checkout. Từ sample 3 trở đi: **0,1–2,4%** |

**Kết luận nghẽn (bằng chứng, không phải phỏng đoán):**

1. **DB không phải nghẽn**: `lock-wait = 0` ở mọi bậc, buffer hit **99,9–100%**, `conn` 18–34 trong khi
   pool tối đa 10/service ⇒ chưa cạn, `pg_stat_activity` `active` đỉnh **8**. Đi tối ưu Postgres lúc này
   là tối ưu sai chỗ.
2. **Đường đọc nghẽn ở CPU tầng service/gateway**, không phải ở query: p99 của 3 endpoint khác nhau
   chênh < 2% (§2) ⇒ request xếp hàng ở cùng một cửa.
3. **Đường ghi nghẽn ở chuỗi saga đồng bộ**: `order-service` 2,7 core + Postgres 2,1 core + RabbitMQ
   1,5 core cho chỉ 230 req/s. Muốn tăng RPS ghi thì phải **bớt lời gọi đồng bộ / bớt transaction**,
   không phải thêm replica đọc.
4. **Client không phải điểm vỡ**: k6 CPU 1,4–2,0 core ở scenario đọc (nặng nhất về RPS), 0,15 core ở
   checkout.

## 6. Điều đáng chú ý & giả định đã chốt

- **Checkout = 1 request/iteration** (không phải 3). Bằng chứng: `POST /v1/checkout` **không xoá giỏ**
  (probe: 3 lần checkout liên tiếp trên cùng 1 giỏ đều **201**), nên giỏ được dựng **1 lần** trong
  `setup()`. Nếu mỗi iteration phải `clear + set item`, p99 đo được là p99 của *3 request* — sai NFR.
  Hệ quả: RPS ở bảng §4 là **RPS của endpoint checkout**, không phải RPS tổng HTTP.
- **400 buyer, không phải 200**: chuẩn phase ghi 200 buyer (dataset), nhưng bậc cao nhất là 400 VU ⇒
  200 buyer sẽ khiến **2 VU dùng chung 1 giỏ** và trả 422 (lỗi harness, dễ đọc sai thành điểm vỡ).
  Seed 400 = 200 theo chuẩn + 200 đệm; mỗi buyer còn có **cặp SKU riêng**. Kết quả: **error 0,00%** ở
  mọi bậc checkout ⇒ giả thuyết "422 do tranh giỏ" đã bị loại bằng số.
- **Baseline không confirm đơn**: reservation hết hạn sau **15 phút** (TTL) và được sweeper nhả ⇒ stock
  tự hồi. `bash tools/load/seed.sh` chạy lại = **reset `on_hand` về 1.000** (PUT là set tuyệt đối). Nhờ
  vậy đo lại được nhiều lần mà không bị "hết hàng" giả.
- **RPS chia cho 65 s** (ramp 5 + hold 60) chứ không dùng `rate` của k6: k6 tính cả thời gian `setup()`
  (login 400 buyer) ⇒ với checkout, dùng `rate` sẽ **báo thấp hơn** thực tế. Ramp làm pha loãng ≤ 8%,
  đã ghi rõ chứ không làm tròn lên.
- **Điểm vỡ ở đây là "vỡ NFR trên máy dev 12 thread"**, không phải "hệ sập". Error vẫn 0,00% khi vỡ ⇒
  hệ chỉ **chậm**, chưa **sai**. Đừng trích con số này như "hệ chịu được X người dùng".

## 7. Chưa làm / ngoài scope

- **Cache (`SL-4.2`), rate limit (`SL-4.3`), read replica (`SL-4.4`), pool/pgbouncer (`SL-4.5`),
  partition (`SL-4.6`), keyset paging (`SL-4.9`)** — baseline phải là hệ **hiện tại**; thêm bất cứ thứ
  gì là làm mất giá trị so sánh.
- **Flash sale** (`SL-4.7`/`4.8`), **metric/dashboard/alert** (Phase 6), **circuit breaker** (Phase 5).
- Baseline **không** đo: GC log/JVM heap, network retransmit, hoặc p99 của từng **service** phía sau
  gateway (chỉ có p99 theo **endpoint**, đo ở client). Muốn chỉ đúng service nào chậm trong saga thì
  cần trace (Phase 6) — hiện chỉ suy được từ CPU + cấu trúc gọi.
- Nợ kỹ thuật phát sinh: xem `TD-37`, `TD-38` trong `Backlog.md` §7.

## 8. Chạy lại (2 lệnh + 3 lệnh)

```bash
# 1) dữ liệu (idempotent; lần 2 phải ra cùng fingerprint)
bash tools/load/seed.sh --buyers 400

# 2) đo — mỗi lệnh ~5 phút, tự dừng ở bậc vỡ, in bảng markdown + lưu raw /tmp/load-<scenario>-<ts>/
bash tools/load.sh --scenario read-catalog
bash tools/load.sh --scenario read-search
bash tools/load.sh --scenario checkout
```

Bản nhanh để thử harness (không dùng số này vào báo cáo): `--vus 50 --duration 10 --no-warmup`.

## 9. Sau `SL-4.2` — cache-aside catalog (before/after trên CÙNG image)

Cách đo: **cùng máy, cùng seed (`d1412cc7427a84a8`), cùng một image catalog**, chỉ đổi
`SHOPLAB_CATALOG_CACHE_ENABLED` ⇒ khác biệt đo được **chỉ** do cache, không do commit khác.

```bash
# OFF
SHOPLAB_CATALOG_CACHE_ENABLED=false docker compose -f infra/docker-compose.yml up -d catalog-service
bash tools/load.sh --scenario read-catalog      # raw: /tmp/load-read-catalog-1794502919
# ON
docker compose -f infra/docker-compose.yml up -d catalog-service
bash tools/load.sh --scenario read-catalog      # raw: /tmp/load-read-catalog-1794502428
```

| VU | OFF: RPS · p99 | ON: RPS · p99 | chênh |
|---|---|---|---|
| 50 | 3.288,9 · 42,5 ms | **4.801,2 · 32,3 ms** | +46 % RPS · −24 % p99 |
| 100 | 3.120,1 · 101,2 ms | **4.799,0 · 70,0 ms** | +54 % · **−31 % p99** |
| 200 | 3.267,1 · 189,8 ms ❌ | **4.890,6 · 145,7 ms** | +50 % · −23 % p99 |
| 400 | *(không chạy — đã vỡ ở 200)* | 5.181,6 · 190,1 ms ❌ | **điểm vỡ lùi 1 bậc** |

| Chỉ số | OFF | ON | Ghi chú |
|---|---|---|---|
| Điểm vỡ | **200 VU** | **400 VU** | lùi đúng **1 bậc** (AC: "lùi ≥ 1 bậc") |
| NFR (50.000 ÷ RPS bậc cuối đạt) | 1/16 | **1/10** | bậc cuối đạt: 100 VU (OFF) → 200 VU (ON) |
| p99 bậc cuối đạt | 101,2 ms | 70,0 ms | **−31 %** ⇒ AC "p99 giảm ≥ 30 %" cũng đạt |
| `pg_stat_database.xact_commit` (cả lần đo) | **679.613** | **18.377** | **−97 %** — đây là thứ cache thật sự mua được |
| `pg_stat_activity` active đỉnh | 3 | **0** | catalog gần như không chạm DB khi đọc |
| Cache | — | hit **99,61 %** (3,76 M hit / 14,6 k miss) · bypass 0 · error 0 | đọc từ `/debug/cache` (delta trước/sau) |

**Nghẽn chuyển đi đâu?** Không chuyển vào DB — nó vẫn ở **tầng service/gateway**: p99 của 3 endpoint vẫn bằng nhau
(145/146/146 ms ở 200 VU) ⇒ vẫn là hàng đợi chung, không phải query. Vì vậy bước tiếp theo đúng là chặn tải ở biên
(`SL-4.3` rate limit) và tách tải đọc (`SL-4.4` read replica), **không** phải tối ưu SQL.

**Drill 4 tính chất** (`bash tools/cache-drill.sh --with-fail-open`, raw `/tmp/cache-drill-1794502415`):

```console
PASS [1] D1 hit ratio 99.2% ≥ 80%
PASS [2] D2 stampede: 200 request đồng thời → chỉ 1 lần đọc DB (≤ 2)
PASS [3] D3 invalidation: detail 16 ms · list 29 ms (≤ 1000)
PASS [4] D4 fail-open: 4 endpoint catalog vẫn 200 khi Redis chết
PASS [5] D4b Redis bật lại: cache tự hoạt động lại (hit=1)
```
