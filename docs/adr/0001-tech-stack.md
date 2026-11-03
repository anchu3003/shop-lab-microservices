# ADR-0001 — Chốt tech stack cho ShopLab

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-02 |
| **Liên quan** | `Backlog-phase-0.md` → `SL-0.1`; `architecture.md` §10, §11 |

---

## Context

ShopLab là learning lab học **microservices architecture** ở quy mô high traffic, không phải sản phẩm
thật. Mọi ticket sau `SL-0.1` (folder structure, tooling, CI, cách viết test, cách containerize) đều
phụ thuộc quyết định ở đây, nên phải chốt trước khi viết dòng code đầu tiên.

Ràng buộc:

- Ưu tiên **học pattern** (Saga, outbox, circuit breaker, caching, sharding...) hơn học framework/ngôn ngữ.
- Chạy **local trên máy dev Windows + Docker Desktop** (`architecture.md` §10).
- Người thực hiện: bạn + AI, không có chuyên gia riêng cho từng ngôn ngữ.
- Mong muốn **thử cả Java và Go** để hiểu trade-off của polyglot (yêu cầu từ bạn).
- Số liệu NFR ở `architecture.md` §3 là **giả định**, sẽ hiệu chỉnh sau Phase 4.

## Options

### 1. Ngôn ngữ & framework

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Node.js 22 + TypeScript + NestJS** | Module sẵn cho microservices (transport, client proxy), DI rõ, dev nhanh | Single-thread → chú ý CPU-bound; không phải stack "transactional enterprise" | Muốn đi nhanh, tập trung architecture |
| **Java 21 + Spring Boot** | Hệ sinh thái microservices trưởng thành nhất (Spring Cloud), transaction/JPA/JDBC rõ ràng, virtual thread | Nặng, startup chậm, boilerplate nhiều, dev chậm hơn | Học stack "enterprise" thật, nghiệp vụ transactional |
| **Go** | Nhẹ, concurrency tốt, binary nhỏ, latency thấp | Ít framework "pin sẵn" cho nghiệp vụ (ORM/transaction/saga), phải tự dựng nhiều | Service hạ tầng / high-throughput |
| **Polyglot (Java + Go)** | Học polyglot, contract giữa service khác ngôn ngữ, chọn ngôn ngữ hợp bài toán | **2 template + 2 shared lib + 2 toolchain CI**; nhân đôi plumbing; dễ lệch convention | Nền đã vững, muốn học thêm polyglot |

### 2. Message broker

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **RabbitMQ** | Dễ học, routing linh hoạt (exchange/queue/binding), UI tốt, retry + DLQ có sẵn | Throughput thấp hơn Kafka, không replay theo offset | Task queue + event ở quy mô vừa |
| **Kafka** | Throughput rất cao, event log bền, replay được, hợp CDC | Vận hành phức tạp, routing kém linh hoạt, tự làm retry/DLQ | Event streaming, event sourcing, CDC → DW |

### 3. API gateway

| Phương án | Ưu | Nhược |
|---|---|---|
| **Tự build (bản mỏng)** | Học được routing/authn/rate limit làm thế nào; dễ debug | Không production-grade, phải tự làm nhiều |
| **Kong / APISIX / Envoy** | Production-like, plugin phong phú | Ít "học" về cơ chế, thêm 1 hệ thống phải vận hành |

### 4. Thành phần còn lại

| Thành phần | Chọn | Alternative | Lý do |
|---|---|---|---|
| Database | PostgreSQL 16 | MySQL, MongoDB | Transaction, JSONB, `SKIP LOCKED` cho outbox relay |
| Cache / lock | Redis 7 | Memcached | Cache, counter, lock, TTL |
| Search | OpenSearch 2.x | Elasticsearch, Meilisearch | Miễn phí, API tương thích ES |
| Object store | MinIO | S3, local FS | S3-compatible, chạy local được |
| Observability | OpenTelemetry + Prometheus + Grafana + Loki | Datadog (tốn tiền) | Open-source, chuẩn industry |
| Local orchestration | Docker Compose → kind (K8s) | minikube | kind nhẹ, chạy tốt Windows + WSL2 |
| CI/CD | GitHub Actions | GitLab CI | Miễn phí cho repo public |
| Test | JUnit 5 + Testcontainers (Java) · `go test` + testcontainers-go (Go) · k6 (load) | — | Đủ cho unit → integration → load |
| Lint/format | Spotless + Checkstyle (Java) · gofmt + golangci-lint (Go) | — | Chuẩn theo ngôn ngữ |
| Git hooks | Lefthook (language-agnostic) + check Conventional Commits | pre-commit | Không phụ thuộc Node; chốt chi tiết ở `SL-0.2` |

## Decision

**1. Ngôn ngữ — Polyglot có kỷ luật, staged.**

- **Java 21 + Spring Boot** = ngôn ngữ chính (Phase 1–2). Phân vai: service nghiệp vụ/transactional —
  `order-service`, `payment-service`, `inventory-service`, `identity-service`, và `api-gateway` (Phase 1).
- **Go** = ngôn ngữ thứ hai, thêm từ **Phase 2–3** cho service hạ tầng/high-throughput, latency-sensitive
  (ví dụ `api-gateway` viết lại, `catalog-service`/`search-service`).
- **Staged**: Phase 1 dựng walking skeleton **toàn Java** để chốt template + shared lib + CI; Go thêm khi
  nền đã vững. Quy tắc phân vai là **bắt buộc**, không rải mỗi service một kiểu tùy hứng.

**Lý do loại phương án khác:**

- **Node.js + NestJS** — loại: không nằm trong mục tiêu học lần này (muốn học stack transactional
  Java/Go); NestJS tốt cho đi nhanh nhưng không đáp ứng mong muốn thử Java/Go.
- **Go-only** — loại: thiếu hệ sinh thái "pin sẵn" cho nghiệp vụ transactional, tốn thời gian vào
  plumbing thay vì pattern. (Vẫn dùng Go cho service hạ tầng.)
- **Java-only** — loại: bỏ mất cơ hội học polyglot mà bạn muốn thử.

**2. Message broker** — chọn **RabbitMQ ở Phase 3**, thêm **Kafka ở Phase 8**.
Lý do loại **Kafka-first**: vận hành phức tạp + phải tự làm retry/DLQ → quá nặng cho mục tiêu Phase 3
(học concept task queue + event). Kafka để dành cho event streaming/CDC.

**3. API gateway** — chọn **tự build bản mỏng ở Phase 1**.
Lý do loại **Kong/APISIX/Envoy ngay**: ít "học" về cơ chế, thêm 1 hệ thống phải vận hành → để Phase 7
cân nhắc thay thế.

**4. Thành phần còn lại** — chọn như bảng ở mục Options (PostgreSQL 16, Redis 7, OpenSearch 2.x, MinIO,
OTel stack, Compose → kind, GitHub Actions). Test/Lint/Git hooks dùng tooling **theo ngôn ngữ**
(JUnit/Spotless/… cho Java, gofmt/golangci-lint cho Go) — thay cho đề xuất Node (Jest/ESLint/Husky)
vì stack không còn Node.

## Consequences

- `architecture.md` §11 và §10.2 phải cập nhật khớp ADR này (bỏ chữ "đề xuất").
- `SL-0.2` phải cài **JDK 21 + Maven/Gradle**; Go toolchain cài trước Phase 2–3.
- `SL-0.4` cần **2 service template** + **2 shared lib** (logger/config/error/health/correlationId viết
  2 lần) → tăng maintenance; chấp nhận để học polyglot.
- `SL-0.5` CI phải chạy **2 toolchain** (Java + Go).
- Contract giữa service khác ngôn ngữ giữ qua **API/event schema** (OpenAPI/AsyncAPI), **không** share code.
- Rủi ro: polyglot dễ trôi convention và tăng độ phức tạp vận hành → giảm thiểu bằng quy tắc phân vai rõ
  và shared lib riêng cho từng ngôn ngữ.

## Revisit

- **Java-first**: revisit khi boilerplate/startup của Spring làm chậm tiến độ **2 phase liên tiếp**, hoặc
  Go tỏ ra phù hợp hơn cho cả service nghiệp vụ (đo bằng thời gian build + p99).
- **Polyglot**: revisit khi chi phí bảo trì 2 template/shared lib vượt lợi ích học (ví dụ > 30% thời gian
  mỗi ticket dành cho plumbing 2 ngôn ngữ) → cân nhắc quay về 1 ngôn ngữ.
- **RabbitMQ**: revisit khi cần replay event theo offset hoặc CDC (Phase 8) → thêm Kafka như đã định.
- **Gateway tự build**: revisit khi cần plugin thật / rate limit nâng cao (Phase 7) → cân nhắc Kong/APISIX.
