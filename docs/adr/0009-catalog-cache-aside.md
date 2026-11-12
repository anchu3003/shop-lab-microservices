# ADR-0009 — Cache-aside cho `catalog` (key schema · generation invalidation · single-flight · fail-open)

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-12 |
| **Liên quan** | `SL-4.2` (trả phần cache của `TD-22`) · `SL-4.1` (baseline để so) · `architecture.md` §5 (pattern 11–12) |

## Context

`catalog-service` là service bị đọc nhiều nhất (read:write ≈ 20:1 — `architecture.md` §3) và **không có cache**.
Baseline `SL-4.1` đo được: **3.795 RPS** ở 100 VU, vỡ ở **200 VU** (p99 190 ms > NFR 150 ms), với
`catalog-service` 286% CPU + `api-gateway` 248% + `postgres` 207% — tức **CPU tầng service**, không phải query.

Số đo baseline còn cho một manh mối quan trọng: p99 của **3 endpoint khác nhau** (list / detail / batch SKU) chênh nhau
**< 2%** ⇒ request xếp hàng ở **một cửa chung**, không phải một câu SQL chậm. Vậy đòn bẩy là **bỏ bớt việc cho service**,
không phải tối ưu query.

Ràng buộc:

- **Không được làm sai dữ liệu**: giỏ/checkout đọc giá qua `GET /v1/skus?ids=` — cache trả giá cũ là **sai tiền**.
- **Redis không được thành điểm chết**: cart đã dùng Redis; nếu cache fail-closed thì Redis chết = catalog chết (tệ hơn
  không có cache).
- Phải **đo lại được** trên cùng bộ đo `SL-4.1` (cùng bậc VU, cùng seed) và **bật/tắt được bằng env** để so trên cùng một
  image.

## Options

| # | Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|---|
| A | **Không cache**, chỉ thêm index/tối ưu query | diff nhỏ nhất | Baseline đã chứng minh p99 3 endpoint bằng nhau ⇒ query **không** phải nút cổ chai; thêm index không đổi p99 | ❌ |
| B | Cache **theo tổ hợp request** (`list:<hash(query)>`, `skus:<hash(ids)>`) | code đơn giản nhất, 1 key/lần đọc | tổ hợp filter/sort và tổ hợp 100 id là **vô hạn** ⇒ hit ratio ~0 với traffic thật; invalidate cũng không biết xoá key nào | ❌ |
| C | **Cache-aside theo từng entity** (detail theo id, **từng SKU**, categories 1 key) + **generation** cho list | hit ratio cao, invalidation chính xác theo id, list vẫn tươi sau ghi | nhiều key hơn; list cần thêm 1 round-trip đọc generation | ✅ **Chọn** |
| D | Read-through / interceptor tự động ở tầng repository | service không phải sửa | "phép thuật" ở tầng thấp: khó biết cái gì được cache, TTL bao nhiêu, và rất dễ cache cả đường ghi | ❌ |
| E | Cache trong JVM (Caffeine) thay Redis | không thêm hop mạng, nhanh nhất | **nhiều instance = nhiều bản cache riêng** ⇒ invalidation không bao giờ đúng; mất khi restart | ❌ (ghi lại làm bài học cho Phase 7 khi có L1 cache) |

## Decision

### D1 — Cache-aside ở tầng **service**, không phải interceptor

Mỗi đường đọc gọi tường minh `ProductCache`/`CategoryCache`/`SkuLineCache` (bọc `CacheAside`). Đọc code là biết cái gì được
cache. Đường ghi **không** đi qua cache — response sau ghi luôn lấy từ entity vừa ghi.

### D2 — Key schema (`catalog:v1:*`) + TTL + jitter

| Key | Nội dung | TTL | Vô hiệu khi |
|---|---|---|---|
| `catalog:v1:product:<id>` | `ProductDetailResponse` | 60 s ± 10 % | update/archive product · tạo SKU của product đó |
| `catalog:v1:sku:<skuId>` | 1 dòng `SkuLine` (hoặc sentinel `null` = không mua được) | 60 s ± 10 % | update/archive product (mọi SKU của nó) |
| `catalog:v1:list:<gen>:<fingerprint>` | `PageResponse<ProductResponse>` | 60 s ± 10 % | bump `gen` |
| `catalog:v1:list:gen` | số nguyên | — | `INCR` mỗi lần ghi product |
| `catalog:v1:categories` | `List<CategoryResponse>` | 5 phút ± 10 % | tạo category |
| `catalog:v1:lock:<sha256(key)>` | token | 2 s | tự hết hạn |

- **Jitter ± 10 %**: nếu mọi key cùng loại được ghi trong cùng một khoảng (sau deploy, sau bump generation) mà TTL giống hệt
  nhau thì chúng **hết hạn cùng lúc** ⇒ một thời điểm mọi key hot cùng miss. Single-flight chỉ cứu được **một** key, không
  cứu được cả một **lớp** key.
- **404 KHÔNG được cache** (lỗi không phải dữ liệu). **Có** cache sentinel cho SKU không mua được (negative caching) để
  client hỏi id sai/archived không đập DB mỗi lần.
- Đổi `key-prefix` = "xoá" toàn bộ cache cũ (namespace mới) mà không cần `FLUSHDB`.

### D3 — `skus?ids=` cache **theo từng SKU**, không theo tổ hợp request

Endpoint nhận tới **100 id** ⇒ tổ hợp là vô hạn, còn giỏ hàng thật mỗi lần một khác. Cache theo tổ hợp sẽ **luôn miss**
(hit ratio ~0) dù cùng 100 SKU được hỏi đi hỏi lại. Theo từng SKU thì **1 `MGET`** phục vụ mọi tổ hợp, và miss chỉ rơi vào
id **chưa từng** được hỏi.

### D4 — List: không xoá từng key, **bump generation**

`page × size × category × q × sort` sinh ra vô hạn key ⇒ không thể liệt kê để xoá (`SCAN` ở 5.000 RPS là tự sát). Một lệnh
`INCR` làm toàn bộ list cũ **vô nghĩa**; key cũ chết dần theo TTL. Giá phải trả: 1 round-trip Redis thêm cho mỗi request
list (đọc generation) — rẻ hơn nhiều so với việc trả dữ liệu cũ hoặc SCAN.

### D5 — Single-flight bằng **lock Redis** (`SET NX PX`), không `synchronized`

Miss ⇒ thử `SET lock:<hash> <token> NX PX 2000`:

- **thắng** ⇒ đọc DB, ghi cache, nhả lock bằng Lua **compare-and-delete** (chỉ xoá nếu token còn của mình — nếu không, một
  request chậm có thể xoá lock của request đã giành được sau khi lock cũ hết hạn);
- **thua** ⇒ poll cache 20 ms, tối đa `lock-wait` (100 ms) ⇒ thấy giá trị thì trả; **hết hạn vẫn miss thì tự đọc DB**
  (**không ghi cache** — hai nguồn ghi cùng key có thể đảo thứ tự và để giá trị cũ đè giá trị mới).

Vì sao không `synchronized`: JVM lock chỉ đúng với **một** instance. Compose hôm nay 1 instance, nhưng code phải đúng khi
scale ngang — và Phase 4 chính là lúc học điều đó.

### D6 — Fail-open, và **Redis không nằm trên critical path**

Mọi lỗi Redis → `CacheUnavailableException` → đọc DB và trả 200; log WARN **rate-limit** (1 dòng/5 s, tránh 5.000 dòng/s lúc
sự cố) + bộ đếm `errors`. **Redis không được thêm vào `/ready` hay healthcheck**: nếu thêm thì "fail-open" thành vô nghĩa
vì orchestrator sẽ đánh service unready ngay khi Redis chết.

Phân biệt quan trọng: `lock()` trả `false` = **có người đang đọc DB** (⇒ chờ, đúng ý đồ) vs ném `CacheUnavailableException`
= **Redis không dùng được** (⇒ đọc DB **ngay**, không chờ). Gộp hai thứ này là bẫy: lúc Redis chết, mọi request ngủ
`lock-wait` rồi mới đọc DB ⇒ p99 tệ hơn cả khi không có cache.

### D7 — Đếm được, không chỉ log

`CacheMetrics` (hit/miss/dbLoad/lockWin/lockTimeout/bypass/error theo loại) + `GET /debug/cache` (chỉ tồn tại khi
`SHOPLAB_DEBUG_ENDPOINTS=true`, `@Hidden` khỏi OpenAPI). Log từng quyết định ở mức **DEBUG**
(`cache=hit|miss-loaded|miss-timeout|bypass`) — 5.000 request/s × 1 dòng INFO là tự tạo sự cố.

## Consequences

**Được** (đo trên cùng máy, cùng seed, cùng image — chỉ đổi `SHOPLAB_CATALOG_CACHE_ENABLED`):

| Bậc VU | Cache OFF | Cache ON | Chênh |
|---|---|---|---|
| 50 | 3.289 RPS · p99 42,5 ms | 4.801 RPS · p99 32,3 ms | +46 % RPS · **−24 % p99** |
| 100 | 3.120 RPS · p99 101,2 ms | 4.799 RPS · p99 70,0 ms | +54 % · **−31 % p99** |
| 200 | 3.267 RPS · p99 189,8 ms (vỡ) | 4.891 RPS · p99 145,7 ms | +50 % · −23 % |
| 400 | — | 5.182 RPS · p99 190,1 ms (vỡ) | **điểm vỡ lùi 1 bậc** |

- Hit ratio trong lần đo: **99,61 %** (3,76 M hit / 14,6 k miss). `pg_stat_database.xact_commit` của cả lần đo:
  **679.613 → 18.377 (−97 %)** — đây mới là thứ cache mua được: **DB rảnh tay**.
- Nghẽn sau cache **không còn ở catalog**: p99 của 3 endpoint vẫn bằng nhau ⇒ vẫn là hàng đợi chung ở gateway/catalog, nên
  bước tiếp theo đúng là `SL-4.3` (rate limit ở biên) + `SL-4.4` (replica), không phải tối ưu SQL.

**Mất / phải chấp nhận:**

- **1 round-trip Redis thêm cho mỗi request list** (đọc generation) — đo được, và vẫn nhanh hơn trước vì bỏ được query.
- **Dữ liệu cũ tối đa 66 s** nếu một lệnh invalidate thất bại (Redis up nhưng lỗi tạm thời). Chấp nhận được: catalog không
  phải nguồn sự thật về tiền — giá trong **đơn** được chốt từ `GET /v1/skus` tại thời điểm checkout.
- **Thêm một dependency vận hành** (Redis) cho catalog, nhưng **không** thêm điểm chết (fail-open).
- **Negative caching** (sentinel `null`) có thể làm một SKU vừa mở bán bị "ẩn" tới 66 s.
- Invalidation chạy **trong transaction**, trước commit: một request đọc chen giữa có thể cache lại giá trị cũ (đọc thấy dữ
  liệu đã commit trước đó) và nó sống tới hết TTL. Đây là **nợ đã biết** (`TD-39`); cách sửa chuẩn là invalidate **sau
  commit** (`TransactionSynchronization` hoặc invalidate theo outbox event) — ngoài scope ticket này.

## Revisit

- Khi **p99 của 3 endpoint đọc không còn bằng nhau** (một endpoint chậm hẳn) ⇒ nghẽn đã chuyển vào trong service, lúc đó mới
  đáng tối ưu query/index.
- Khi **hit ratio < 90 %** ở tải thật (đọc `/debug/cache` hoặc metric Phase 6) ⇒ TTL quá ngắn hoặc key schema sai.
- Khi có **> 1 instance** catalog và cần giảm hop Redis: thêm **L1 (Caffeine) + pub/sub invalidate** — chỉ làm khi số đo cho
  thấy hop Redis là phần đáng kể của p99 (hiện Redis chỉ ~4 % CPU khi đo nghẽn).
- Khi `checkout` cần đọc giá **tuyệt đối tươi**: tách đường đọc thẳng DB (`?nocache=1` cho internal) thay vì hạ TTL toàn hệ.
- Khi `docker stop redis` xảy ra **thường xuyên** trong vận hành ⇒ phải có alert (Phase 6), không chỉ log WARN.


