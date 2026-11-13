# ADR-0010 — Rate limit ở biên: token bucket Redis, **hai xô**, fail-open

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-13 |
| **Liên quan** | `SL-4.3` (trả `TD-21` + phần rate limit của `TD-22`) · `SL-4.1` (baseline) · `SL-4.2` (cache — cùng Redis) · `architecture.md` §6.3 (flash sale) |

## Context

Trước ticket này gateway **không giới hạn gì**. Hai chỗ public bị dội được chỉ ra từ Phase 2:

- `/v1/carts/**` — mỗi guest token do server cấp = **2 key Redis TTL 30 ngày** ⇒ spam token là spam Redis (`TD-21`).
- `GET /v1/skus?ids=` — nhận tới **100 id/request** (`TD-22`, phần rate limit).

Và `architecture.md` §6.3 đặt rate limit ở **biên** làm mảnh bắt buộc của flash sale: phần lớn request phải bị chặn
**trước khi** vào promotion/inventory, nếu không thì "atomic counter" chỉ là giảm nhẹ một cơn lũ đã vào tới DB.

Ràng buộc thật của repo này:

1. **Gateway tự viết** (servlet `@RequestMapping("/**")` + `RequestForwarder`), **không** phải Spring Cloud Gateway ⇒
   phải là servlet `Filter`, không có sẵn `RequestRateLimiter`.
2. **Danh tính chỉ đáng tin sau verify.** `JwtAuthFilter` hiện **không** verify token ở path public (nó chỉ xoá header
   danh tính). Đặt key rate limit bằng `sub` **chưa** verify = mỗi request một bucket mới (kẻ tấn công tự chọn `sub`) ⇒
   rate limit bằng 0.
3. **Không được biến gateway thành điểm chết.** Redis chết mà gateway chặn hết traffic là biến sự cố nhỏ thành sự cố
   toàn hệ — mà gateway là **cửa duy nhất**.
4. Số đo `SL-4.1`/`SL-4.2`: `api-gateway` đã ~250 % CPU khi đo nghẽn ⇒ mọi thứ thêm vào đường nóng phải **rẻ** và phải
   **đo được** cái giá của nó.

## Options

| # | Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|---|
| A | Fixed window (`INCR` + `PEXPIRE`) | đơn giản nhất, 1 lệnh | cho phép **gấp đôi** hạn mức ở ranh giới 2 cửa sổ — đúng lúc mở bán thì đó là lỗ hổng | ❌ |
| B | Token bucket **trong JVM** | không thêm hop mạng, nhanh nhất | nhiều instance = nhiều hạn mức riêng ⇒ hạn mức thật = `n × cấu hình`; không chia sẻ được | ❌ |
| C | Token bucket **Redis + Lua**, 1 xô (user **hoặc** IP) | atomic, chia sẻ giữa instance | user gọi path public vẫn tính theo IP (NAT: cả văn phòng chung 1 xô); và **xoay account là vượt trần** vì không có trần IP | ❌ |
| D | Token bucket Redis + Lua, **hai xô** (per-user **và** per-IP) trong **cùng một** `EVAL` | có cả công bằng (per-user) và trần chống lạm dụng (per-IP); cùng 1 round-trip | cấu hình 2 hạn mức/route; đo tải từ 1 IP phải tắt limiter | ✅ **Chọn** |
| E | Rate limit ở tầng **service** | không đụng gateway | phải giữ limiter ở 10 service; tải đã vào tới service rồi mới chặn ⇒ không giảm tải biên; trái §6.3 | ❌ |

Vì sao **D** chứ không **C**: per-user một mình thì xoay vài chục account là có vài chục hạn mức; per-IP một mình thì 50
người sau một NAT dùng chung 1 hạn mức. Hai xô cùng lúc cho **cả hai** tính chất, và vì kiểm trong cùng `EVAL` nên
**không** thêm round-trip.

## Decision

### D1 — Servlet `Filter`, chạy **sau** verify token

`RateLimitFilter` đăng ký order `HIGHEST_PRECEDENCE + 20`: sau `CorrelationIdFilter` và `JwtAuthFilter` (+10), trước
`GatewayRoutingController`. Nhờ vậy `x-user-id` (nếu có) **đã verify chữ ký**, và 429 rẻ hơn hẳn việc gọi service đích.

### D2 — Hai xô, khoá `rl:<routeKey>:u:<sub>` / `rl:<routeKey>:ip:<ip>`

- Có danh tính ⇒ kiểm **cả** xô user **và** xô IP; **từ chối nếu một trong hai cạn**.
- Không danh tính (khách, path public) ⇒ chỉ xô IP. Không bịa ra định danh.
- `routeKey` = prefix của rule khớp (dài nhất thắng, dùng chung `RouteMatcher.matchesPrefix`); path không khớp rule nào
  ⇒ `default` (đúng ngữ nghĩa "hạn mức mặc định", không phải mỗi path một xô).

### D3 — Toàn bộ thuật toán trong **một** Lua script, **kiểm hết rồi mới trừ**

Read-modify-write ở Java = hai request song song cùng thấy "còn 1 token" ⇒ hạn mức bị vượt. Và với 2 xô, thứ tự quan
trọng: nếu trừ user trước rồi mới phát hiện IP cạn thì user **mất token cho request không được phục vụ** (cạn dần dù
không gửi gì). Script tính token của mọi xô, chỉ trừ khi **tất cả** đều ≥ 1.

`now` (ms) do Java truyền vào (`Clock`), **không** dùng `TIME` của Redis: test và drill tất định được. Đánh đổi: nhiều
instance lệch đồng hồ ⇒ biên burst lệch vài ms — chấp nhận.

### D4 — 429 đúng chuẩn, dùng `ErrorCode` chung

`ApiError` (thêm `TOO_MANY_REQUESTS` vào `service-commons`) + `Retry-After` (giây, làm tròn **lên**) + `X-RateLimit-Limit`
(= burst) + `X-RateLimit-Remaining` + `X-RateLimit-Reset` (epoch giây khi xô đầy lại). Khi cả hai xô đều cho qua, header
báo **xô chặt hơn** (tỉ lệ còn lại thấp hơn) để client thấy sớm là sắp bị chặn.

### D5 — Fail-open, và Redis **không** nằm trên critical path

Mọi lỗi Redis (kể cả bug parse kết quả) ⇒ `failOpen` thì **cho qua + WARN 1 dòng/5 s** (đếm dồn, không log mỗi request);
`failOpen=false` ⇒ 503 `UNAVAILABLE`. Gateway **không** `depends_on` Redis, **không** thêm Redis vào `/ready`, timeout
Redis 100 ms. Rate limit là **lớp bảo vệ**, không phải chức năng: mất nó thì hệ chịu tải kém hơn, không phải ngừng phục vụ.

### D6 — Công tắc `RATE_LIMIT_ENABLED`

`false` ⇒ filter **không chạm Redis lần nào**. Dùng cho (a) đo điểm vỡ bằng `tools/load.sh` — k6 đi từ **một** IP nên xô
IP của gateway sẽ chặn chính phép đo, biến "hệ không chịu nổi tải" thành "bị chặn ở biên" (hai thứ khác hẳn nhau);
(b) ca sự cố cần nhận hết tải. `tools/load.sh` đếm `rl_429` và **cảnh báo** nếu bậc đo có 429 ⇒ không bao giờ ghi một
điểm vỡ sai bản chất.

### D7 — `X-Forwarded-For` chỉ tin khi peer là proxy tin cậy

`trusted-proxies` rỗng (mặc định) ⇒ dùng `getRemoteAddr()`. Khi peer ∈ danh sách ⇒ lấy hop **phải nhất không tin cậy**.
Tin header vô điều kiện = đổi header mỗi request là đổi bucket ⇒ rate limit bằng 0.

## Consequences

**Được** (drill `tools/rate-limit-drill.sh --with-overhead`, **10 PASS / 0 FAIL**):

| Drill | Kết quả |
|---|---|
| D1 `500 VU` không token, 30 s | **735.323 request · 728.871 × 429 (99,1 %)** — AC ≥ 90 % |
| D2 chặn ở biên, không ở DB | `xact_commit` Δ **597** trong khi **6.452** request lọt qua limiter ⇒ tải bị chặn **không** chạm DB |
| D3 hình dạng 429 | `429` + `Retry-After: 1` + `X-RateLimit-Limit/Remaining/Reset` + body `TOO_MANY_REQUESTS` có `traceId` |
| D4 hai user | A: 297 × 200 / 303 × 429 · B: **200** (không chặn oan) |
| D5 fail-open | Redis dừng ⇒ 200 + WARN `fail-open`; Redis trở lại ⇒ **429** (tự phục hồi, không kẹt fail-open) |
| D6 chi phí (100 VU `read-catalog`) | TẮT **4.804 RPS · p99 64,8 ms** → BẬT **4.498 RPS · p99 62,2 ms** (**−6,4 % RPS**, Δp99 −2,6 ms) |

**Về D6 — báo cáo cả hai lần đo, không chọn số đẹp:** lần chạy trước (máy ở trạng thái nặng hơn: chỉ 3.622 RPS khi limiter
tắt) cho **+16,4 ms p99 / −643 RPS**. Nghĩa là **Δp99 không phân biệt được với nhiễu** giữa các lần chạy ở quy mô này, còn
**throughput giảm 6–18 %** thì thấy ở **cả hai** lần. Kết luận trung thực: *chi phí của limiter chủ yếu là throughput (mỗi
request thêm 1 round-trip Redis); chưa đủ dữ liệu để nói nó làm p99 tệ đi* — muốn chốt phải lặp D6 ≥ 5 lần, ghi thành `TD-40`.

**Mất / phải chấp nhận:**

- **+1 round-trip Redis trên mỗi request** ở cửa vào: throughput −6…−18 % tuỳ trạng thái máy (D6).
- Trần **per-IP** khiến mọi phép đo tải từ **một** máy phải tắt limiter (`RATE_LIMIT_ENABLED=false`) ⇒ điểm vỡ đo được là
  của **hệ**, không phải của limiter. Đã ghi vào `tools/load.sh` + `.env.example`.
- NAT: nhiều user sau một IP dùng chung xô IP. Xô IP đặt rộng hơn xô user (200/400 vs 100/200) và có env riêng để nâng.
- Redis lưu **1 hash nhỏ/xô** ⇒ số key ~ số (route × user/IP) đang hoạt động; TTL = thời gian nạp đầy xô nên không phình.
- Không rate limit **bên trong** service (bulkhead/backpressure là Phase 5) — đúng scope.

## Revisit

- Khi **D6 (lặp ≥ 5 lần)** cho thấy round-trip Redis chiếm phần đáng kể p99 ⇒ đổi sang **local pre-check** (cho qua ngay nếu
  xô local còn nhiều token, chỉ gọi Redis khi gần cạn) — chấp nhận hạn mức xấp xỉ. Đây là `TD-40`.
- Khi có **nhiều instance** gateway: khoá đã sẵn sàng chia sẻ (Redis), nhưng `X-RateLimit-Reset` dùng đồng hồ từng instance ⇒
  đo lại trước khi tin tuyệt đối.
- Khi flash sale (`SL-4.7`/`4.8`) cần **xếp hàng** thay vì 429 ⇒ thêm "waiting room" ở biên, dùng lại xô này làm đầu vào.
- Khi cần giới hạn theo **API key/tenant** (Phase 5+): thêm scope mới vào key schema, không đổi thuật toán.
- Hạn mức hiện là **số tĩnh trong config**; nếu cần đổi theo giờ/chiến dịch ⇒ đọc từ Redis/config service (chưa làm).

