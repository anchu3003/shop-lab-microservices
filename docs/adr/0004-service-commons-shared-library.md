# ADR-0004 — Shared infrastructure library `service-commons`

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-02 |
| **Liên quan** | `Backlog-phase-0.md` → `SL-0.4`; ADR-0001 (tech stack); ADR-0003 (module layout); `architecture.md` §6, §11.1 |

---

## Context

Phase 1 tạo 3 service cùng lúc. Nếu mỗi service tự dựng config loader, logger, error handler, health
endpoint theo một kiểu thì đến Phase 5–6 (cần resilience + observability đồng bộ) sẽ phải sửa lại toàn bộ —
và lúc đó log/error format đã lệch nhau, không truy vết được xuyên service.

Ràng buộc:

- `AGENTS.md` §6: **không** import code của service này vào service khác — chỉ cho phép **shared library
  hạ tầng** ở `packages/`.
- **Không** share database, **không** share domain model. Shared lib phải chỉ chứa hạ tầng.
- Format log phải có `traceId`, `service`, `level`, `msg` (`AGENTS.md` §6).
- Phase 6 mới làm tracing/metrics — nhưng chỗ để cắm vào phải có sẵn từ bây giờ.
- Máy dev chỉ có 15 GiB RAM, đang chạy sẵn hạ tầng ~150 MiB → không được thêm thứ nặng vô ích.

## Options

### 1. Mức độ "tự động" của shared lib

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Spring Boot auto-configuration** (`AutoConfiguration.imports`) | Service chỉ cần khai dependency là có đủ bean; mọi bean `@ConditionalOnMissingBean` → service ghi đè được; danh sách bean nhìn thấy ở 1 chỗ | Thêm một lớp "magic" phải hiểu; debug khó hơn nếu không quen | Thư viện dùng cho nhiều service (trường hợp này) |
| **`@ComponentScan` package của thư viện** | Đơn giản, dễ hiểu | Service phải nhớ scan package nào; scan rộng dễ kéo theo bean ngoài ý muốn; khó override | Thư viện nội bộ, 1–2 consumer |
| **Copy-paste code vào từng service** | Không có coupling, service tự chủ hoàn toàn | 3 service = 3 bản logger/error handler/config trôi dần khỏi nhau → mất tính thống nhất đúng lúc cần nhất | Khi mỗi service thật sự cần hành vi khác nhau |

### 2. Endpoint health/ready

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Tự viết `/health` `/ready` `/version` + shared lib helper** | Học được cơ chế probe; format body do mình quyết; không thêm dependency; để sẵn chỗ cắm check sâu | Phải tự làm; không có sẵn health group, `management.endpoint.*`, security cho actuator | Muốn hiểu bản chất, hoặc cần format riêng |
| **Spring Boot Actuator** | Production-grade: health group, liveness/readiness state machine, metrics, `/actuator/info` | Endpoint mặc định là `/actuator/health` (khác thiết kế ticket); khó "học" vì làm hết sẵn; thêm bề mặt cần bảo vệ | Khi đã có K8s thật và cần tích hợp sẵn |

### 3. Độ sâu của dependency check

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **TCP connect (host:port)** | Rẻ; **không cần driver** (không nhét JDBC/Redis client vào shared lib); đủ để chứng minh 503 khi tắt container | Chỉ nói "cổng mở" — DB còn đang khởi động/replica lag vẫn báo UP | Phase 0–1, khi service chưa có DB |
| **Check sâu (query `SELECT 1`, `PING`)** | Phát hiện được DB "sống nhưng không dùng được" | Shared lib phải kéo JDBC/Redis client, và biết cấu hình của từng loại dependency | Khi service đã có DB thật (Phase 1+) |

### 4. Correlation id và traceId

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Một khái niệm duy nhất: `traceId` = `x-correlation-id`** | Đơn giản; chỉ 1 field trong log; đủ để truy vết xuyên service ở Phase 0–5 | Không phân biệt "id do client/gateway sinh" với "id do tracer sinh"; khi bật OTel sẽ thấy 2 id cùng tồn tại | Trước khi có distributed tracing thật |
| **Tách ngay `correlationId` (client) và `traceId` (tracer)** | Đúng chuẩn ngay từ đầu | Phase 0 chưa có tracer → `traceId` luôn rỗng, thêm phức tạp mà chưa có giá trị | Khi đã có OpenTelemetry từ đầu |

## Decision

**1. Auto-configuration.** `packages/service-commons` ship `ShoplabCommonsAutoConfiguration` đăng ký qua
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Mọi bean đều
`@ConditionalOnMissingBean` để service ghi đè được.

**Lý do loại phương án khác:**

- **`@ComponentScan`** — loại: buộc mỗi service nhớ scan package của thư viện; scan rộng dễ kéo bean ngoài
  ý muốn và khó override. Auto-config là cách chuẩn để thư viện Spring "tự cắm vào".
- **Copy-paste** — loại: chính là vấn đề mà ticket này tồn tại để giải quyết.

**2. Tự viết `/health`, `/ready`, `/version`; KHÔNG dùng Actuator ở Phase 0.** Actuator để dành cho Phase 6
khi cần metrics/tracing — lúc đó có thể chạy song song (Actuator lo metrics, endpoint của ta lo contract
`/health` `/ready` `/version` mà client đã biết).

**Lý do loại Actuator ngay:** endpoint mặc định là `/actuator/health` (khác thiết kế ticket), và nó làm sẵn
mọi thứ nên mất phần "học cơ chế probe" — vốn là mục tiêu của repo.

**3. Dependency check mức TCP ở Phase 0.** Interface `DependencyHealthCheck` chỉ có `name()` + `check()`
→ Phase 1 service tự implement bản check sâu (query DB thật) mà **không phải sửa controller**.

**Lý do loại check sâu ngay:** shared lib sẽ phải kéo JDBC/Redis client và biết cấu hình từng loại
dependency — đúng thứ mà "chỉ chứa hạ tầng chung" muốn tránh.

**4. Một khái niệm duy nhất: `traceId` = `x-correlation-id`.** `CorrelationIdFilter` đọc header, validate
(độ dài ≤ 128, chỉ ký tự an toàn — vì input của client là **không tin được**), thiếu thì sinh UUID, đẩy vào
MDC, echo lại response header.

**Lý do loại tách 2 id ngay:** Phase 0 chưa có tracer → `traceId` luôn rỗng. Tách khi chưa có gì dùng là
over-engineer; Phase 6 sẽ tách khi cắm OpenTelemetry.

**Phạm vi shared lib — chỉ hạ tầng:**

| Có trong `service-commons` | KHÔNG có |
|---|---|
| `log` (MDC keys, `logback-shoplab.xml`) | domain model / entity |
| `web` (correlation id filter, `ApiError`, global exception handler) | repository / JPA / datasource dùng chung |
| `error` (`ErrorCode`, `AppException`, `NotFoundException`) | business rule, validation nghiệp vụ |
| `health` (`DependencyHealthCheck`, `TcpHealthCheck`, kết quả check) | client gọi service khác |
| `config` (`RequiredConfig`, `EnvValidator`, `MissingConfigException`) | cấu hình đặc thù của từng service |

## Consequences

- Service mới chỉ cần thêm dependency `shoplab-service-commons` + `spring-boot-starter-webmvc` là có: log
  JSON, error response thống nhất, correlation id, health check helper, fail-fast config.
- `spring-boot-starter-webmvc` trong commons để **`optional = true`** → commons compile được với web types
  nhưng **không** ép consumer kéo Tomcat. Service tự khai starter. Đổi lại: quên khai là lỗi compile, không
  phải lỗi runtime mơ hồ.
- **Log format là một chỗ duy nhất**: `logback-shoplab.xml`. Đổi format = đổi 1 file, mọi service theo.
  Service chỉ có `logback-spring.xml` 10 dòng để truyền `spring.application.name`.
- **Tên method ảnh hưởng API**: `HealthCheckResult.isUp()` bị Jackson coi là getter → lộ field thừa
  `"up": true` trong body `/ready`. Đã đổi thành `healthy()` (không khớp pattern getter). **Bài học chung:
  record trả ra API thì tên method phải để ý.**
- **Coupling đã chấp nhận**: mọi service phụ thuộc version của `service-commons`. Đổi shared lib = rebuild
  mọi service. Giảm thiểu bằng cách giữ nó **nhỏ và chỉ hạ tầng**.
- Health check TCP **không chặn được thời gian phân giải DNS**: khi tắt container, Docker gỡ DNS entry →
  check fail bằng `UnknownHostException` sau ~4s (timeout 500ms chỉ áp cho bước `connect`). Chấp nhận ở
  Phase 0; Phase 1 dùng check sâu + cache DNS của driver sẽ hết.
- Test của commons **không** khởi động Spring context (auto-config được kiểm chứng gián tiếp qua
  integration test của `service-template`: filter chạy thật, echo `x-correlation-id`, `/debug/error` có
  `traceId`). Hệ quả: `ShoplabCommonsAutoConfiguration` chưa có coverage riêng.

## Revisit

- Revisit **tự viết endpoint vs Actuator** ở **Phase 6** khi cắm OpenTelemetry/Prometheus: nếu phải viết lại
  health/metrics để tương thích chuẩn thì chuyển sang Actuator và giữ `/health` `/ready` `/version` làm
  alias mỏng.
- Revisit **độ sâu check** ngay ở **Phase 1** khi service đầu tiên có DB thật: điều kiện là có ≥ 2 service
  cần check sâu → lúc đó cân nhắc thêm `JdbcHealthCheck` vào commons, hoặc để từng service tự implement.
- Revisit **tách `correlationId` vs `traceId`** ở **Phase 6**: điều kiện là OpenTelemetry đã sinh `traceId`
  128-bit và có ít nhất 1 luồng đi qua ≥ 2 service cần nối trace.
- Revisit **phạm vi shared lib** khi commons vượt **1000 dòng code** hoặc có ≥ 2 thay đổi phá vỡ tương thích
  trong 1 phase → lúc đó coupling đã bắt đầu đắt hơn lợi ích, cân nhắc tách module nhỏ hơn.
- Revisit **`optional = true`** cho `spring-boot-starter-webmvc` khi có service không phải web (vd consumer
  chỉ đọc queue) → lúc đó cần tách phần `web` ra module riêng.
