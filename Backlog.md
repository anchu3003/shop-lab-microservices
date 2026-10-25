# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | |
|---|---|
| **Ticket đang làm** | — |
| **Ticket tiếp theo** | `SL-0.1` |
| **Số ticket chi tiết hiện có** | 5 (Phase 0) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | 🔵 In Progress | 0 / 5 |
| 1 | Walking skeleton | ⚪ Todo (epic) | — |
| 2 | Core commerce flow | ⚪ Todo (epic) | — |
| 3 | Async & event-driven | ⚪ Todo (epic) | — |
| 4 | Scale & performance | ⚪ Todo (epic) | — |
| 5 | Resilience | ⚪ Todo (epic) | — |
| 6 | Observability & SRE | ⚪ Todo (epic) | — |
| 7 | Deployment & infra | ⚪ Todo (epic) | — |
| 8 | Advanced | ⚪ Todo (epic) | — |

---

## 2. Convention ticket

- **ID**: `SL-<phase>.<số>` — ví dụ `SL-0.1`, `SL-2.4`. Không đổi ID sau khi tạo.
- **Trạng thái**: `Todo` / `In Progress` / `Blocked` / `Review` / `Done`
- **Ước lượng**: `S` (< nửa ngày) · `M` (1–2 ngày) · `L` (3–5 ngày). Nếu `L` thì **phải tách nhỏ**.
- Viết như **BA đã define xong**: mô tả được, verify được, không còn chỗ mơ hồ.
- Ticket ở phase xa chỉ cần mức **epic** — chỉ breakdown khi tới lượt.

### Template

```markdown
### SL-X.Y — <tên ticket>

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | X — <tên phase> |
| **Phụ thuộc** | SL-... / — |
| **Ước lượng** | S / M / L |

**Bối cảnh**
Vì sao ticket này tồn tại. Vấn đề đang gặp là gì.

**Mục tiêu**
Kết quả cuối cùng cần đạt.

**In scope**
- ...

**Out of scope**
- ...

**Acceptance Criteria**
- [ ] ...

**Ghi chú kỹ thuật**
Pattern / thư viện / trade-off đã chấp nhận.

**Kết quả verify** *(điền khi Done)*
- Lệnh đã chạy + output tóm tắt

**Learning note** *(điền khi Done)*
- Học được gì, cái gì bất ngờ, lần sau nên làm khác thế nào.
```

---

## 3. Phase 0 — Foundation

> **Mục tiêu phase**: có bộ khung repo sạch, convention rõ, hạ tầng local chạy được bằng 1 lệnh,
> và 1 service template để các service sau chỉ việc copy. **Chưa** code business logic ở phase này.
>
> **Điều kiện hoàn thành phase**: `docker compose up -d` lên đủ infra healthy; service template
> trả `/health` 200; CI xanh; `architecture.md` §11 không còn chữ "cần chốt".

---

### SL-0.1 — Chốt tech stack và viết ADR-0001

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | — |
| **Ước lượng** | S |

**Bối cảnh**
`architecture.md` §11 mới chỉ là **đề xuất** cho ngôn ngữ/framework, message broker và API gateway.
Mọi ticket sau đều bị ảnh hưởng bởi quyết định này (folder structure, tooling, CI, cách viết test),
nên phải chốt trước khi làm gì khác.

**Mục tiêu**
Chốt 3 hạng mục — **ngôn ngữ + framework**, **message broker**, **API gateway** — và ghi lại lý do
dưới dạng ADR để sau này không phải tranh luận lại.

**In scope**
- So sánh phương án cho 3 hạng mục trên, dựa trên bảng ở `architecture.md` §11
- Tạo `docs/adr/0001-tech-stack.md` với cấu trúc: Context / Options / Decision / Consequences / Revisit
- Update `architecture.md` §11: bỏ chữ "đề xuất / cần chốt", ghi quyết định cuối + link ADR

**Out of scope**
- Chưa cài đặt công cụ, chưa tạo service, chưa viết code

**Acceptance Criteria**
- [ ] `docs/adr/0001-tech-stack.md` tồn tại, có đủ 5 mục, có bảng trade-off cho từng hạng mục
- [ ] ADR nêu rõ **lý do loại** phương án không chọn, không chỉ nêu lý do chọn
- [ ] ADR có mục "Revisit khi nào" với điều kiện cụ thể (không ghi chung chung "khi cần")
- [ ] `architecture.md` §11 khớp 100% với ADR, không còn từ "đề xuất" ở phần quyết định
- [ ] `architecture.md` §10.2 cập nhật lại danh sách công cụ cần cài theo stack đã chốt

**Ghi chú kỹ thuật**
- Broker: cân nhắc RabbitMQ (dễ học, routing linh hoạt, có DLQ sẵn) vs Kafka (throughput cao, replay, hợp CDC)
- Gateway: tự build (học được cơ chế) vs Kong/APISIX/Envoy (production-like, ít học được)
- Đây là **ADR đầu tiên** → đồng thời chốt luôn format ADR cho các ADR sau

---

### SL-0.2 — Bootstrap repo skeleton, convention và dev environment

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.1 |
| **Ước lượng** | M |

**Bối cảnh**
Repo hiện chỉ có 3 file markdown, chưa `git init`, chưa có cấu trúc thư mục, chưa có tooling.
Máy dev cũng chưa cài runtime theo stack sẽ chốt ở SL-0.1. Cần bộ khung chuẩn trước khi viết
dòng code đầu tiên để tránh mỗi service một kiểu.

**Mục tiêu**
Có repo skeleton chạy được: cấu trúc thư mục rõ, lint/format/commit convention được enforce tự động,
và máy dev sẵn sàng build service đầu tiên.

**In scope**
- `git init` + `.gitignore` (bỏ `node_modules`, `.env`, `dist`, `coverage`, log, volume data)
- Cấu trúc thư mục: `services/`, `packages/` (shared lib), `infra/`, `docs/adr/`, `tools/` (script)
- `.editorconfig` + lint/format (ESLint + Prettier nếu chốt Node)
- Git hooks: Husky + lint-staged + commitlint (Conventional Commits)
- `README.md`: giới thiệu, yêu cầu môi trường, cách chạy, link tới `AGENTS.md` / `architecture.md` / `Backlog.md`
- `.env.example` với các biến chung (không có secret thật)
- Cài runtime/tooling còn thiếu trên máy dev (theo stack ở SL-0.1) + ghi lại version vào README

**Out of scope**
- Chưa viết service nào
- Chưa dựng infra (đó là SL-0.3)
- Chưa cấu hình CI (đó là SL-0.5)

**Acceptance Criteria**
- [ ] `git status` sạch, commit đầu tiên theo Conventional Commits
- [ ] Cấu trúc thư mục khớp mô tả, có `.gitkeep` cho thư mục rỗng
- [ ] Lệnh lint + format chạy được và pass trên skeleton rỗng
- [ ] Commit sai convention bị commitlint chặn (đã thử và bị chặn thật)
- [ ] `README.md` có mục "Yêu cầu môi trường" ghi rõ version runtime + lệnh cài
- [ ] `.env.example` đủ biến cho Phase 1, `.env` đã nằm trong `.gitignore`
- [ ] Không có secret thật nào trong repo (đã kiểm tra bằng grep)

**Ghi chú kỹ thuật**
- Nếu chốt Node: dùng npm workspaces hoặc pnpm workspace để share `packages/` giữa các service
- Không dùng Nx/Turborepo ở phase này — thêm công cụ khi chưa cần là over-engineer
- Convention đặt tên service: `kebab-case` + hậu tố `-service`, riêng gateway là `api-gateway`

---

### SL-0.3 — Dựng local infrastructure bằng Docker Compose

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.2 |
| **Ước lượng** | M |

**Bối cảnh**
Mọi phase sau đều cần hạ tầng local chạy được: database, cache, message broker. Nếu tự cài bằng tay
thì môi trường sẽ lệch nhau, bug "chạy máy tôi được" sẽ xuất hiện ngay từ Phase 1.

**Mục tiêu**
1 lệnh `docker compose up -d` dựng đủ hạ tầng nền, có healthcheck, có UI để quan sát, và có
script reset sạch dữ liệu.

**In scope**
- `infra/docker-compose.yml` gồm:
  - **PostgreSQL** — 1 instance, tạo sẵn nhiều database (`identity`, `catalog`, `inventory`,
    `promotion`, `order`, `payment`, `notification`) để giữ nguyên tắc database per service
  - **Redis** — cache / lock / rate limit
  - **Message broker** — theo stack chốt ở SL-0.1 (RabbitMQ có management UI, hoặc Kafka + UI)
  - **Adminer / pgAdmin** — xem dữ liệu Postgres
  - *(tuỳ chọn)* MinIO cho object storage — chỉ bật nếu thấy cần sớm
- Healthcheck cho từng service trong compose
- `infra/init/` — script SQL tạo database/schema ban đầu
- Named volume để dữ liệu không mất khi restart
- `tools/reset-infra.ps1` — down + xoá volume + up lại
- `.env.example` bổ sung biến kết nối (host/port/user/password dạng placeholder)
- Document trong `README.md`: cách start/stop/reset, port nào dùng cho gì

**Out of scope**
- Chưa containerize service của mình (làm ở SL-0.4)
- Chưa dựng observability stack (Prometheus/Grafana/Loki) — để Phase 6
- Chưa dựng K8s — để Phase 7

**Acceptance Criteria**
- [ ] `docker compose up -d` → tất cả container `healthy` (chứng minh bằng `docker compose ps`)
- [ ] Kết nối được từ host: `psql` vào từng database, `redis-cli ping` trả `PONG`
- [ ] Broker UI mở được trên browser, thấy được exchange/queue
- [ ] Chạy `tools/reset-infra.ps1` → dữ liệu về trạng thái ban đầu, không lỗi
- [ ] `docker compose down` rồi `up` lại → dữ liệu cũ vẫn còn (chứng minh volume hoạt động)
- [ ] Không có port nào trùng nhau, danh sách port được ghi trong `README.md`
- [ ] Không hard-code password trong file compose — đọc từ `.env`

**Ghi chú kỹ thuật**
- Ưu tiên image **alpine/slim** để nhẹ, nhưng đảm bảo có healthcheck tool (`pg_isready`, `redis-cli`)
- Đặt tên container có prefix `shoplab-` để dễ filter khi debug
- Trên Windows + Docker Desktop: dùng **named volume** thay vì bind mount cho data
- Ghi lại dung lượng RAM mà cả stack chiếm — dùng làm baseline cho Phase 7 (K8s resource request)

---

### SL-0.4 — Service template và shared infrastructure library

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.2, SL-0.3 |
| **Ước lượng** | M |

**Bối cảnh**
Phase 1 sẽ tạo 3 service cùng lúc. Nếu mỗi service tự dựng config loader, logger, error handler,
health endpoint theo một kiểu khác nhau thì đến Phase 5–6 (cần observability và resilience đồng bộ)
sẽ phải sửa lại toàn bộ. Làm template 1 lần, dùng lại nhiều lần.

**Mục tiêu**
Có 1 service mẫu (`service-template`) chạy được trong Docker, kèm 1 shared library chứa các thành
phần hạ tầng dùng chung — đủ để Phase 1 copy ra thành service thật.

**In scope**
- `services/service-template/`:
  - Entry point + module structure theo framework đã chốt
  - Endpoint `GET /health` (liveness) và `GET /ready` (readiness, có check dependency)
  - Endpoint `GET /version` trả version + git commit sha
  - Config loader đọc từ env, validate lúc boot, fail fast nếu thiếu biến bắt buộc
  - Structured logger (JSON) có `traceId`, `service`, `level`, `msg`
  - Global error handler → response lỗi thống nhất (`code`, `message`, `traceId`)
  - Middleware sinh/propagate `correlationId` từ header `x-correlation-id`
  - Graceful shutdown: bắt `SIGTERM`, ngừng nhận request mới, đợi request đang chạy xong
  - `Dockerfile` multi-stage, image nhỏ, chạy non-root
- `packages/` (shared lib) — tách phần dùng chung: logger, config schema, error types,
  health check helper, correlation id
- Thêm service template vào `docker-compose` để chứng minh nó chạy được cùng infra
- Unit test cho config loader + health endpoint (làm mẫu test cho các service sau)
- `README.md` mục "Cách tạo service mới từ template" — quy trình copy & rename

**Out of scope**
- Chưa có business logic, chưa kết nối database thật (chỉ health check)
- Chưa có auth, chưa có gateway
- Chưa setup tracing/metrics (Phase 6) — nhưng **phải để sẵn chỗ** cho nó

**Acceptance Criteria**
- [ ] `docker compose up -d` → `service-template` healthy, gọi `GET /health` trả 200
- [ ] `GET /ready` trả 200 khi dependency OK, trả 503 khi tắt dependency (đã thử tắt thật)
- [ ] Thiếu env bắt buộc → service fail lúc boot với message rõ ràng, không chạy nửa vời
- [ ] Log ra JSON có `traceId`, và `traceId` xuất hiện trong response lỗi
- [ ] `docker stop` → service shutdown êm, log ghi rõ đang chờ request nào, không bị kill cứng
- [ ] `npm test` (hoặc lệnh tương đương) pass, coverage phần config/health ≥ 80%
- [ ] Image size ghi lại trong ticket (để so sánh khi optimize sau)
- [ ] `README.md` có hướng dẫn tạo service mới từ template, đã thử làm theo và chạy được

**Ghi chú kỹ thuật**
- Trade-off: shared library giúp đồng bộ nhưng tạo coupling. Chỉ để **hạ tầng** trong shared lib
  (log, config, error, health), **tuyệt đối không** để domain logic hay database entity chung
- Giữ template tối giản — mỗi thứ thêm vào phải trả lời được "service thật có cần không"

---

### SL-0.5 — CI pipeline cơ bản (lint + test + build image)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.4 |
| **Ước lượng** | S |

**Bối cảnh**
Từ Phase 1 trở đi số lượng service tăng nhanh. Không có CI thì "code chạy máy tôi" sẽ âm thầm phá
build, và đến Phase 7 (deploy) sẽ phải làm lại từ đầu.

**Mục tiêu**
Mỗi lần push/PR, pipeline tự chạy lint → test → build Docker image, kết quả nhìn thấy ngay trên PR.

**In scope**
- `.github/workflows/ci.yml`: job `lint`, job `test`, job `build-image` (chạy trên `service-template`)
- Cache dependency để pipeline chạy nhanh
- Badge trạng thái CI trong `README.md`
- Ghi lại thời gian pipeline chạy lần đầu (baseline)

**Out of scope**
- Chưa deploy (CD) — để Phase 7
- Chưa publish image lên registry thật — chỉ build trong CI
- Chưa làm integration test với Testcontainers — để Phase 1–2 khi có DB thật

**Acceptance Criteria**
- [ ] Push lên nhánh bất kỳ → pipeline chạy tự động, xanh
- [ ] Cố tình tạo lỗi lint → pipeline đỏ, job lint báo đúng file/dòng
- [ ] Cố tình làm fail test → pipeline đỏ
- [ ] Build image thành công trong CI
- [ ] Thời gian pipeline lần đầu được ghi lại trong ticket
- [ ] Badge CI hiển thị đúng trạng thái trong `README.md`

**Ghi chú kỹ thuật**
- Chạy các job song song nếu độc lập để tiết kiệm thời gian
- Pin version của action (`@v4`) để tránh pipeline vỡ do upstream update

---

## 4. Phase 1 → 8 — Epic outline

> Các phase này **chỉ ở mức epic**. Khi Phase 0 xong mới breakdown Phase 1 thành ticket chi tiết.
> Đây là chủ ý: breakdown quá sớm sẽ phải viết lại vì hiểu biết thay đổi sau mỗi phase.

### Phase 1 — Walking skeleton
Mục tiêu: đường đi từ client tới DB chạy được end-to-end với 3 service đầu tiên.

- `SL-1.x` API Gateway bản mỏng: routing, verify JWT, correlation id, error response thống nhất
- `SL-1.x` `identity-service`: đăng ký, đăng nhập, refresh token, JWT, RBAC cơ bản
- `SL-1.x` `catalog-service`: CRUD product/SKU/category, migration + seed data, phân trang & filter
- `SL-1.x` E2E smoke test: register → login → create product (seller) → list product (guest) qua gateway
- `SL-1.x` OpenAPI spec cho các endpoint Phase 1

### Phase 2 — Core commerce flow
Mục tiêu: luồng mua hàng hoàn chỉnh, đúng dữ liệu kể cả khi fail.

- `SL-2.x` `cart-service` trên Redis (TTL, merge cart khi login)
- `SL-2.x` `inventory-service`: reserve / commit / release + chống oversell (atomic + lock)
- `SL-2.x` `promotion-service`: voucher + rule giảm giá + lock/commit voucher
- `SL-2.x` `order-service`: tạo order, state machine, **Saga orchestration** cho checkout
- `SL-2.x` `payment-service`: payment intent + **mock provider** + webhook idempotent
- `SL-2.x` Test đồng thời: 100 request mua cùng 1 SKU → không oversell
- `SL-2.x` Compensating action cho mọi nhánh fail của checkout

### Phase 3 — Async & event-driven
Mục tiêu: phần không critical chạy async, event không bị mất.

- `SL-3.x` Dựng message broker trong compose + convention đặt tên event
- `SL-3.x` **Transactional outbox** ở `order-service` và `payment-service` + relay
- `SL-3.x` Event envelope chuẩn + versioning + dedupe theo `eventId`
- `SL-3.x` `notification-service`: consumer gửi email/SMS (mock), retry + **DLQ**
- `SL-3.x` `search-service`: index product từ event, reconcile job khi index lệch
- `SL-3.x` Báo cáo: so sánh trước/sau khi chuyển sang async (latency checkout)

### Phase 4 — Scale & performance
Mục tiêu: có số liệu chứng minh mỗi pattern giúp gì.

- `SL-4.x` Load test harness bằng **k6** + baseline khi chưa tối ưu (p99, throughput, điểm vỡ)
- `SL-4.x` Cache-aside cho catalog + TTL + jitter + xử lý **cache stampede**
- `SL-4.x` **Rate limit** ở gateway (token bucket theo user/IP) + trả 429 có `Retry-After`
- `SL-4.x` **Read replica** + routing read/write, đo **replica lag**
- `SL-4.x` **Partition** bảng `orders` theo thời gian, đo cải thiện query
- `SL-4.x` Kịch bản **flash sale** 500k request/60s → báo cáo before/after
- `SL-4.x` Connection pooling: đo điểm gãy khi số connection vượt pool size

---

### Phase 5 — Resilience
Mục tiêu: hệ vẫn phục vụ luồng critical khi một phần chết.

- `SL-5.x` Timeout + **retry với exponential backoff + jitter** (chỉ cho operation idempotent)
- `SL-5.x` **Circuit breaker** cho `payment` và `inventory` client
- `SL-5.x` **Bulkhead**: pool riêng theo dependency
- `SL-5.x` **Backpressure** + queue depth limit + load shedding
- `SL-5.x` **Graceful degradation**: feature flag tắt phần non-critical khi quá tải
- `SL-5.x` Chaos test: kill dependency / inject latency → đo ảnh hưởng lên luồng checkout
- `SL-5.x` Runbook cho các sự cố phổ biến

### Phase 6 — Observability & SRE
Mục tiêu: nhìn thấy hệ thống đang làm gì mà không cần đọc log bằng mắt.

- `SL-6.x` **OpenTelemetry** tracing xuyên gateway → service → DB → broker
- `SL-6.x` Prometheus metrics (RED cho endpoint, USE cho hạ tầng) + Grafana dashboard
- `SL-6.x` Log tập trung (Loki) + query theo `traceId`
- `SL-6.x` Định nghĩa **SLI/SLO** + tính **error budget** tự động
- `SL-6.x` Alert symptom-based + link tới runbook
- `SL-6.x` Diễn tập: chạy load test rồi tìm nguyên nhân chậm chỉ bằng trace/metric

### Phase 7 — Deployment & infra
Mục tiêu: deploy lên K8s không downtime, rollback nhanh.

- `SL-7.x` Dựng K8s local bằng **kind** + Helm chart chung cho service
- `SL-7.x` Probe (liveness/readiness) + resource request/limit + **HPA**
- `SL-7.x` **Blue-green / canary** deploy + graceful shutdown hoàn chỉnh
- `SL-7.x` Config & secret management (ConfigMap/Secret), không nhúng secret vào image
- `SL-7.x` CD pipeline: build → push → deploy staging → approve → deploy prod
- `SL-7.x` Diễn tập rollback < 2 phút + báo cáo

### Phase 8 — Advanced
Mục tiêu: các pattern nâng cao, chỉ làm khi đã vững nền.

- `SL-8.x` **CQRS** + read model cho danh sách đơn của user
- `SL-8.x` `search-service` đầy đủ trên OpenSearch (filter, sort, facet, typo tolerance)
- `SL-8.x` **Sharding** dữ liệu theo `user_id` — thiết kế + demo
- `SL-8.x` **CDC** (Debezium) → `analytics-service` / warehouse
- `SL-8.x` Thêm **Kafka** cho event streaming, so sánh trade-off với RabbitMQ bằng số liệu
- `SL-8.x` Design doc **multi-region / DR** (không triển khai thật)
- `SL-8.x` `review-service`, `shipping-service`
- `SL-8.x` Security hardening: OAuth2/OIDC, mTLS nội bộ, secret rotation

---

## 5. Learning log

Ghi lại những thứ học được trong quá trình làm — đây là **output chính** của repo này.

| Ngày | Ticket | Học được |
|---|---|---|
| 2026-10-25 | — | Khởi tạo repo, viết `AGENTS.md`, `architecture.md`, `Backlog.md`. Chốt cách làm: cuốn chiếu theo ticket, không build một loạt. |

---

## 6. Changelog

| Ngày | Thay đổi |
|---|---|
| 2026-10-25 | Tạo backlog. Phase 0 có 5 ticket chi tiết, Phase 1–8 ở mức epic. |




