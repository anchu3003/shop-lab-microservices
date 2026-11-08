# ADR-0008 — Service-to-service auth: service token (`client_credentials` + `aud`/`exp`)

- **Trạng thái**: Accepted
- **Ngày**: 2026-11-08
- **Ticket**: `SL-3.11` (tách từ `SL-3.5`) — trả một phần `TD-25`
- **Liên quan**: ADR-0006 (checkout model — nơi ghi nợ "khi Phase 3 có service-to-service auth"), ADR-0007 (event
  contract), `Backlog.md` §7 `TD-25`

---

## 1. Bối cảnh

Tới hết `SL-3.4`, **mọi** caller trong hệ đều là người dùng cuối: mang access token (JWT RS256, `role=BUYER|SELLER|ADMIN`,
`exp=15m`) và mỗi service verify độc lập bằng JWKS của `identity-service`.

Phase 3 tạo ra loại caller **thứ hai**: service gọi service, **không có người dùng phía sau**.

| Việc cần làm | Vì sao trước Phase 3 chưa cần | Vì sao giờ cần |
|---|---|---|
| `order-service` gọi `inventory`/`promotion` `commit` khi **tự confirm** đơn (`SL-3.5`) | Chỉ client gọi `/confirm` ⇒ dùng token của khách | Webhook tới sau khi khách đóng tab; token 15′ có thể đã hết hạn |
| `checkout` sweeper gọi `release` khi đơn hết hạn (`TD-25`) | Dựa vào TTL sweeper của inventory/promotion (trễ ≤ 1 nhịp) | Hàng/voucher phải về ngay, không phụ thuộc service khác |

Ràng buộc: `commit`/`release` yêu cầu `authenticated()` (defense in depth — compose publish port để debug, ai gọi thẳng
cũng phải có token hợp lệ).

## 2. Phương án đã cân nhắc

| # | Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|---|
| A | **Internal token dùng chung** (`X-Internal-Token` = shared secret, filter gán `ROLE_SERVICE`) | nhỏ nhất (~250 dòng), 1 chỗ dùng cho mọi service | secret **tĩnh, không hạn, không audience**; ai có secret gọi được mọi endpoint chỉ-cần-authenticated; log không biết ai gọi | ❌ Không dùng |
| B | **Endpoint internal riêng** (`/v1/internal/...`) + shared secret | không nới quyền endpoint người dùng; audit bằng `grep` | ~450 dòng; trùng nghiệp vụ với endpoint user (2 cửa vào 1 service method, dễ lệch) | ❌ Không dùng |
| C | **Service token thật** qua `identity-service` (`client_credentials`, JWT có `aud`/`exp`) | có hạn, có audience, rotate từng service, log biết **service nào** gọi; mở đường mTLS ở Phase 7 | lớn nhất; phải sửa `identity-service` (service mọi service khác phụ thuộc) | ✅ **Chọn** |

Chọn C vì repo là learning lab: mục tiêu là **hiểu đúng** pattern production, không phải bản rút gọn dễ nhất
(`AGENTS.md` §1, §3.4). A/B tiết kiệm ~2 ngày nhưng dạy sai bài: chúng không có khái niệm audience/thu hồi — đúng hai
thứ phân biệt "service-to-service auth" với "API key".


## 3. Quyết định

### D1 — `identity-service` phát service token qua `POST /v1/auth/service-token`

Request `{clientId, clientSecret, audience}` → JWT RS256:

```
iss  = https://identity.shoplab.local        (issuer của hệ)
sub  = svc:order-service                     (KHÔNG phải UUID user)
aud  = [inventory-service]                    (đúng 1 service đích)
role = SERVICE
exp  = iat + 5m                               (SHOPLAB_SERVICE_TOKEN_TTL)
jti  = <uuid>                                 (để log/đối soát)
```

- **Không** dùng Spring Authorization Server: hạ tầng đó kéo theo chuẩn đầy đủ (client registry, consent, UI rotate
  key) — ngoài scope Phase 3 (`AGENTS.md` §3.4). Endpoint này là **subset của `client_credentials`**: cùng claim, cùng
  ngữ nghĩa, thiếu phần chưa cần.
- **Không** phát refresh token: máy tự xin token mới; refresh cho máy chỉ thêm trạng thái có thể rò rỉ.
- Registry client **nằm trong config** (`shoplab.service-auth.clients.<id>.secret` + `.allowed-audiences`), secret đọc
  từ env. Thêm client = sửa config + restart (không build lại image). Bảng client + API quản lý là việc của Phase 6/7.
- **Không có default** cho secret trong `application.yml` ⇒ thiếu env thì identity **không khởi động**. Fail-closed:
  secret mặc định "ai cũng biết" là lỗ hổng tệ hơn downtime ở môi trường dev.
- Secret so sánh **hằng thời gian** (`MessageDigest.isEqual`); client lạ và sai secret trả **cùng** một lỗi 401 (không
  dò được client id nào tồn tại — cùng lý do với lỗi đăng nhập).

### D2 — Audience do client xin, server kiểm **allowlist**

Nếu server cấp token cho bất kỳ `audience` nào client yêu cầu thì `aud` chỉ là trang trí: một service bị lộ secret xin
được token đi mọi nơi. Allowlist biến "quyền đi tới đâu" thành dữ liệu cấu hình — đọc được, review được:

```yaml
order-service:
  secret: ${ORDER_SERVICE_CLIENT_SECRET}
  allowed-audiences: [inventory-service, promotion-service]
```

### D3 — Service đích kiểm `aud` **chỉ khi** `role=SERVICE`

`ServiceTokenRules.validator(issuer, serviceName)` = validator cũ (`iss`/`exp`/`nbf`) + luật: nếu `role=SERVICE` thì
`aud` **phải chứa** tên service này.

- **Vì sao không bắt token người cũng có `aud`**: token người hiện không có claim này (hợp đồng từ `SL-1.4`); thêm vào
  là thay đổi hợp đồng token của **8 service** trong một ticket về event ⇒ tách việc, ghi nợ.
- **Hệ quả phải nói rõ**: token người vẫn được chấp nhận ở mọi service như trước (chưa có audience binding giữa các
  service). Đây là giới hạn **đã biết**, không phải sơ suất.

### D4 — Sai `aud` ⇒ **401 `invalid_token`**, không phải 403

Đo trên stack thật: Spring resource server map lỗi validator thành `invalid_token` ⇒ 401. Đúng chuẩn OAuth2 (token
không hợp lệ cho resource này), và **403** để dành cho "đã xác thực nhưng thiếu quyền" (`@PreAuthorize` sai role).
Đo được ở `promotion-service`: token `aud=promotion-service` gọi endpoint `hasRole('ADMIN')` ⇒ **403**; token
`aud=inventory-service` gọi chính endpoint đó ⇒ **401**.

### D5 — Client cache token tới `exp - skew` (mặc định 30 s)

Xin token mỗi request biến `identity-service` thành **điểm chết của mọi call nội bộ**. `ServiceTokenClient` cache theo
audience, refresh sớm 30 s (token hết hạn giữa đường bay = 401 ngẫu nhiên, lỗi rất khó lần), một khoá cho lần nạp
(tránh "token stampede"), và **không cache lỗi** (giữ lỗi tới khi restart là tự bắn vào chân).

## 4. Hệ quả

**Được**

- `SL-3.5` và `TD-25` có đường gọi nội bộ hợp lệ, không mượn token người, không nhét token vào message.
- Token rò rỉ chỉ sống ≤ 5 phút và chỉ dùng được ở **đúng** service đích.
- Log/audit biết service nào gọi (`sub=svc:<clientId>`), không phải "một ai đó đã authenticated".
- Đổi sang mTLS (Phase 7) chỉ thay **lớp transport**; luật `aud` giữ nguyên.

**Mất / chấp nhận**

- `identity-service` trở thành **dependency của luồng nội bộ** (không chỉ login): identity chết ⇒ order không lấy được
  token mới ⇒ call nội bộ fail sau khi token trong cache hết hạn (≤ 5 phút). Giảm nhẹ bằng cache + timeout ngắn.
- Client registry trong config ⇒ thêm client phải restart identity (không hot-reload).
- Secret là shared secret **một chiều** (client → identity): ai đọc được `.env` của identity biết hết secret của mọi
  client (dev/local chấp nhận; production phải dùng secret store — Phase 7).
- Token người chưa có `aud` ⇒ chưa có audience binding giữa các service (xem D3).

## 5. Điều kiện revisit (định lượng được)

| Điều kiện | Chuyển sang | Vì sao |
|---|---|---|
| > **3 client** hoặc cần rotate secret **không restart** | Registry DB + endpoint quản lý client (Phase 6/7) | config + restart không còn phù hợp vận hành |
| Cần **thu hồi tức thời** token máy (sự cố bảo mật) | `exp` ≤ 60 s + token version/blacklist | hiện chỉ dựa vào TTL ngắn |
| Lên K8s / có secret store (Phase 7) | Secret từ Vault/ESO + **mTLS** giữa pod | bỏ được shared secret trong env |
| Cần audience binding cho cả token người | Ticket riêng: thêm `aud` ở identity + validator ở **tất cả** service | đổi hợp đồng token toàn hệ, không gộp vào ticket feature |
| ≥ 90 % call nội bộ dùng service token | Xem lại việc identity nằm trên đường tới hạn (cache ở sidecar?) | tránh single point of dependency |
