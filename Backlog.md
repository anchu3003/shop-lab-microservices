# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (Phase 0 xong) |
| **Ticket tiếp theo** | Phase 1 (epic — breakdown khi tới lượt) |
| **Số ticket chi tiết hiện có** | 8 (Phase 0 — tất cả `Done`) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
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

## 3. Phase 0 — Foundation (✅ Done — chi tiết đã tách sang file archive)

> Phase 0 hoàn thành **8/8 ticket** (2026-11-03): repo skeleton + convention · hạ tầng local bằng 1 lệnh ·
> service template `/health` 200 · **CI xanh trên `main`**.
>
> Toàn bộ chi tiết 8 ticket (bối cảnh · scope · Acceptance Criteria · quyết định khi implement ·
> **kết quả verify bằng output thật** · learning note) đã tách sang **[`Backlog-phase-0.md`](Backlog-phase-0.md)**.
> Lý do: file này chỉ giữ việc **đang làm / sắp làm**; phần đã xong vẫn cần tra cứu nên **archive chứ không xoá**.

### Archive các phase đã xong

| Phase | File archive | Doc snapshot | Tag / Release | Ticket | Trạng thái |
|---|---|---|---|---|---|
| 0 — Foundation | [`Backlog-phase-0.md`](Backlog-phase-0.md) | [`docs/phases/phase-0.md`](docs/phases/phase-0.md) | `phase-0` | `SL-0.1` … `SL-0.8` | ✅ Done (8/8) |

> Quy ước: mỗi phase xong → tách phần chi tiết sang `Backlog-phase-<N>.md`, viết `docs/phases/phase-N.md`,
> gắn **tag `phase-N` + GitHub Release**, rồi thêm một dòng vào bảng trên (chi tiết: `AGENTS.md` §5 — DoD của phase;
> lý do: [ADR-0005](docs/adr/0005-phase-snapshot-and-release.md)).
> Learning log + Changelog **vẫn ở file này** (chỉ mục xuyên phase).

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
| 2026-11-02 | SL-0.6 | Cline Hooks/Plugins không chạy trên VS Code extension → dùng Rule + Skill + Workflow; skill auto-trigger nhờ `description` cụ thể. |
| 2026-11-02 | SL-0.1 | Chốt polyglot staged (Java-first + Go); trade-off rõ: 2 template/shared lib/CI. Lint/test/git-hooks phải đổi theo ngôn ngữ, không dùng tooling Node. |
| 2026-11-02 | SL-0.7 | Dev env = WSL2: Docker Desktop vốn đã dùng WSL2 backend → cần đưa source sang Linux fs (`~`), không để `/mnt/d/...`. |
| 2026-11-02 | SL-0.8 | Chọn GitHub Flow (không GitFlow) cho lab 1 người; repo đang ở `master` cần rename → `main`; PR cần remote + `gh`. |
| 2026-11-02 | SL-0.8 | Diff nhiễu **CRLF** (1253/1253 dòng) do thiếu `.gitattributes` + `core.autocrlf` unset → chuẩn hoá LF. **Branch protection/rulesets không dùng được trên repo private + GitHub Free (403)** → phải thay bằng git hook local. |
| 2026-11-02 | SL-0.2 | Verify trên repo **rỗng là vô nghĩa** — phải làm **test âm** mới biết config có tác dụng. `maven-checkstyle-plugin` **không** nhận `-Dcheckstyle.version` (dependency của plugin resolve từ POM của plugin) → phải khai báo `<plugin><dependencies>`; mặc định là checkstyle 9.3 (2022). Lefthook `pre-push` chỉ bảo vệ **local**. |
| 2026-11-02 | SL-0.3 | `docker compose` nạp `.env` theo **project directory** (= thư mục chứa compose file), **không** theo CWD → compose ở `infra/` mà `.env` ở gốc thì **buộc** `--env-file .env`. |
| 2026-11-02 | SL-0.3 | **Test âm chỉ đáng tin khi môi trường sạch**: biến đã export vào shell (`set -a; . ./.env`) làm Compose resolve từ env → test "bỏ `--env-file`" cho kết quả sai. Phải chạy `env -i`. |
| 2026-11-02 | SL-0.3 | Postgres `docker-entrypoint-initdb.d` **chỉ chạy khi volume rỗng** → sửa init script phải `down -v` mới thấy tác dụng. Redis `requirepass` ⇒ `redis-cli ping` trần trả `NOAUTH` (AC gốc cần ghi rõ điều kiện auth). RAM baseline cả stack ≈ **150 MiB** (RabbitMQ chiếm ~106 MiB — nặng nhất). |
| 2026-11-02 | SL-0.2 | Verify bằng tooling **cài tạm** không chứng minh được gì: `which mvn` vẫn trỏ bản Windows → phải cài trong WSL rồi kiểm tra `which mvn`. Git hook nằm trong `.git/hooks` (không commit) → người clone mới phải tự `lefthook install`, nếu không mọi bảo vệ **im lặng** mất tác dụng. Test âm bằng file bị `.gitignore` ignore là test **vô hiệu**. |
| 2026-11-02 | SL-0.4 | **`BUILD SUCCESS` không có nghĩa là đã test**: surefire mặc định 2.17 không hỗ trợ JUnit Platform → `Tests run: 0` mà vẫn xanh. Pin `3.6.1`. Bỏ `spring-boot-starter-parent` (import BOM) thì phải tự bật `-parameters` — thiếu nó là **lỗi runtime**, không phải lỗi compile. |
| 2026-11-02 | SL-0.4 | Spotless (`{}`) và Checkstyle `WhitespaceAround` (`{ }`) **xung đột trực tiếp** ở block rỗng → phải bật `allowEmpty*`. Lint/format chỉ được kiểm chứng trên **code thật**. |
| 2026-11-02 | SL-0.4 | Graceful shutdown: nghe `ContextClosedEvent` là **quá muộn** (Boot drain xong mới đóng context) → phải dùng `SmartLifecycle` phase `Integer.MAX_VALUE`. Record trả ra API: tên method `isXxx()` bị Jackson coi là getter → lộ field thừa. |
| 2026-11-02 | SL-0.4 | Log JSON: logstash encoder mặc định nhét **mọi** property logger context + field `tags` không nhất quán → phải tắt `includeContext`/`includeTags` để schema log đồng nhất. Health check TCP **không chặn được DNS resolution** (timeout chỉ áp cho `connect`). |
| 2026-11-02 | SL-0.4 | **AC "đã thử làm theo và chạy được" phải thực thi thật**: copy template theo README → build đỏ vì test hard-code tên service. Sửa thành đọc từ `@Value("${spring.application.name}")`. Bug `-parameters` và bug này đều **không lộ** qua 72 test tự động — chỉ lộ khi curl tay / làm theo hướng dẫn. |
| 2026-11-03 | SL-0.5 | **CI cũng phải test âm mới tin**: run xanh chỉ chứng minh pipeline *chạy*, không chứng minh nó *chặn*. Phải cố tình vi phạm Checkstyle (→ `lint` đỏ, log hiện `file:line`) và cố tình fail test (→ `test` đỏ) mới thấy `needs:` hoạt động (`build-image` = `skipped`). |
| 2026-11-03 | SL-0.5 | **Chọn vi phạm cho test âm phải sống sót qua tooling phía trước**: `UnusedImports` bị Spotless (google-java-format) tự xoá ngay ở hook `pre-commit` → test âm tự chết trước khi tới CI. Dùng `NeedBraces` (formatter không sửa được) mới tới được CI. |
| 2026-11-03 | SL-0.5 | **Job `test` phải skip lint để không chạy 2 lần**: `mvn verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` — đo trước khi viết workflow (log hết "Starting audit..." nhưng `jacoco:check` vẫn chạy). Không suy đoán "chắc là skip được". |
| 2026-11-03 | SL-0.5 | **Hai token một máy, scope khác nhau**: token VS Code cấp qua `GIT_ASKPASS` có `workflow` (push được `.github/workflows/*`), token `gh` CLI **không**. Kiểm tra bằng `git credential fill` + `curl -D - -H "Authorization: token $TOKEN" https://api.github.com/user` → header `x-oauth-scopes` (không in token ra). |
| 2026-11-03 | SL-0.5 | Actions trên repo **private** tính theo phút (Free 2.000/tháng) → ràng buộc `needs:` giúp 2 run đỏ chỉ tốn 1m12s / 35s thay vì ~2m35s mỗi run. Badge repo private chỉ render khi đã login GitHub. |
| 2026-11-03 | SL-0.5 | **`concurrency` gom push + PR chung một group là bẫy**: run PR huỷ run push của **cùng commit** → commit có check `cancelled` (thấy bằng `gh api .../commits/<sha>/check-runs`), PR hiện như fail dù code sạch. Gom theo `github.ref` (push = `refs/heads/x`, PR = `refs/pull/N/merge`) thì 2 run độc lập, không huỷ nhau. **Tối ưu "ít run" có thể đánh đổi bằng tín hiệu sai trên PR.** |
| 2026-11-03 | SL-0.5 | **Badge workflow resolve theo default branch**: `actions/workflows/ci.yml/badge.svg` trả **404** khi `ci.yml` chưa nằm trên `main` (thử cả Bearer lẫn basic auth đều 404) → AC badge chỉ verify được **sau merge**. Đừng hứa "badge xanh" trước khi merge. |
| 2026-11-03 | SL-0.5 | **AC phụ thuộc trạng thái `main` (badge, branch protection…) thì phải merge mới verify được** → ticket phải để `Review` chứ không `Done` trước merge. Và repo **private** thì SVG badge không lấy được bằng curl (chỉ browser đã login) — trạng thái phải suy từ API run mới nhất của `main`. |
| 2026-11-03 | process | **Backlog dài → tách archive theo phase**: ticket đã xong vẫn có giá trị (verify output + learning note) nhưng làm loãng backlog sống → tách sang `Backlog-phase-<N>.md`, giữ **Learning log + Changelog** ở `Backlog.md` làm chỉ mục xuyên phase. Kết quả: `Backlog.md` 1267 → **260 dòng**, archive 1057 dòng, không mất nội dung (8/8 `### SL-`). |
| 2026-11-03 | process | **Commit minor không cần nhánh/PR riêng**: đóng ticket / update backlog / sửa doc → gộp vào nhánh ticket kế tiếp, tránh PR chỉ chứa doc. Rủi ro cần để ý: phần đóng ticket "trễ" một nhịp, nên **ticket chỉ được coi là `Done` khi PR chứa commit đóng ticket đã merge**. |
| 2026-11-03 | process | **Lưu trữ snapshot theo phase = tag + Release + doc snapshot, KHÔNG giữ nhánh thường trực**: tag cho phép `git switch -c maint/phase-N phase-N` bất cứ lúc nào ⇒ nhánh thường trực **không thêm khả năng nào**, chỉ thêm rot + phân kỳ doc. Gọi là "lazy D" (ADR-0005). Tag **không** trigger CI (workflow chỉ khai `on.push.branches`) ⇒ 0 phút Actions; `git diff phase-N phase-M --stat` là công cụ học rất tốt (thấy đúng phase sau thêm gì). |

---

## 6. Changelog

| Ngày | Thay đổi |
|---|---|
| 2026-10-25 | Tạo backlog. Phase 0 có 5 ticket chi tiết, Phase 1–8 ở mức epic. |
| 2026-11-02 | Thêm `SL-0.6` — Bootstrap AI tooling (rules/skills/workflows), đặt làm trước `SL-0.1`. |
| 2026-11-02 | Implement `SL-0.6`: tạo `.clinerules/00-context.md`, 3 skills `.cline/skills/`, 3 workflows `.clinerules/workflows/`; update `AGENTS.md` §10. Trạng thái `Review` (chờ user test slash command). |
| 2026-11-02 | Đóng `SL-0.6` → `Done` (user xác nhận slash command + skill trigger). |
| 2026-11-02 | Đóng `SL-0.1` → `Done`: tạo `docs/adr/0001-tech-stack.md`, cập nhật `architecture.md` §10.2/§11. |
| 2026-11-02 | Thêm `SL-0.7` → `Done`: ADR-0002 (dev env = WSL2); cập nhật `architecture.md` §10; điều chỉnh `SL-0.2`. |
| 2026-11-02 | `SL-0.2`: bổ sung `.gitattributes` + git config (`core.autocrlf`/`core.fileMode`) để tránh nhiễu khi chuyển sang WSL. |
| 2026-11-02 | Thêm `SL-0.8` (Review): git workflow — rule `.clinerules/10-git-workflow.md`, workflow `/git-branch-pr.md`, `docs/git-flow.md`. |
| 2026-11-02 | Đóng `SL-0.8` → `Done` (user gọi `/git-branch-pr.md` xác nhận workflow nạp đúng). |
| 2026-11-02 | Tạo PR [#1](https://github.com/anchu3003/shop-lab-microservices/pull/1) cho `SL-0.1/0.6/0.7/0.8` (đã merge vào `main` bằng **merge commit** `368009e` — lệch quy ước **squash** của `docs/git-flow.md`, ghi nhận để lần sau merge đúng). Commit `a881a01` chuẩn hoá line-ending bằng `.gitattributes` (`* text=auto eol=lf`) — sớm hơn `SL-0.2`. |
| 2026-11-02 | Ghi learning note `SL-0.8`: diff nhiễu CRLF + kết quả thử branch protection (**403 — private + GitHub Free**, không bật được). |
| 2026-11-02 | `SL-0.2` → `In Progress`: tạo nhánh `chore/sl-0.2-repo-skeleton`; thêm `.editorconfig`, `.gitignore` (mở rộng), `.env.example`, `README.md`, `lefthook.yml`, `tools/*.sh`, `pom.xml` + `config/checkstyle/checkstyle.xml`, thư mục `services/ packages/ infra/`. |
| 2026-11-02 | `SL-0.2` → `Review`: pin checkstyle **12.3.1** (plugin mặc định kéo 9.3); verify thật `mvn verify` BUILD SUCCESS + test âm cho cả Spotless và Checkstyle; PR [#2](https://github.com/anchu3003/shop-lab-microservices/pull/2). Còn chờ user cài JDK 21 + Maven trong WSL (sudo). |
| 2026-11-02 | `SL-0.3` → `In Progress`: tạo nhánh `feat/sl-0.3-local-infra-compose`. |
| 2026-11-02 | `SL-0.3` → `Done`: `infra/docker-compose.yml` (Postgres 16 · Redis 7 · RabbitMQ 4 + management UI · Adminer), `infra/init/01-create-databases.sh` (7 DB per service, idempotent), `tools/reset-infra.sh` (bash, thay `reset-infra.ps1` theo ADR-0002), `.env.example` (+`POSTGRES_DB`, `ADMINER_PORT`), README (port table + RAM baseline + bẫy `--env-file`). Verify thật: 4/4 container `healthy`, psql 7/7 DB OK, `redis-cli -a … ping` → `PONG`, RabbitMQ UI + Adminer HTTP 200, reset script OK, volume bền qua `down`/`up`, test âm `${VAR:?}` fail-fast. RAM baseline ≈ **150 MiB**. |
| 2026-11-02 | Ghi nhận (ngoài scope `SL-0.3`): JDK **21.0.12.1** + Maven **3.9.12** đã có trong WSL → `mvn -B verify` **BUILD SUCCESS** (0 Checkstyle violations) ⇒ điều kiện cuối của `SL-0.2` đã đạt, `SL-0.2` có thể chuyển `Done`. `lefthook` **2.2.2** cũng đã cài (`~/.local/bin`, hooks active — thấy chạy thật khi commit `eef58a3`). Sửa lại bảng "Yêu cầu môi trường" trong `README.md` cho khớp. |
| 2026-11-02 | Commit `eef58a3` trên nhánh `feat/sl-0.3-local-infra-compose` (lefthook `pre-commit` + `commit-msg` chạy và pass). |
| 2026-11-02 | Đóng `SL-0.2` → `Done`: điều kiện cuối đã đạt — JDK **21.0.12.1** + Maven **3.9.12** (`/usr/bin/mvn`, không còn bản Windows) + Lefthook **2.2.2** cài trong WSL; `mvn -B verify` **BUILD SUCCESS**; test âm lặp lại cho `commit-msg` + `secret-scan` đều **bị chặn thật**. Phase 0 → **6 / 8**. |
| 2026-11-02 | Tạo PR [#3](https://github.com/anchu3003/shop-lab-microservices/pull/3) cho `SL-0.3` + đóng `SL-0.2` (nhánh `feat/sl-0.3-local-infra-compose`, 3 commit). **Nhắc lại quy ước**: merge bằng **squash** (PR #1 đã lệch — dùng merge commit). |
| 2026-11-02 | `SL-0.4` → `In Progress`: nhánh `feat/sl-0.4-service-template`. Chốt với user: **Spring Boot 4.1.2**. |
| 2026-11-02 | `SL-0.4` → `Done`: Maven multi-module (`packages/service-commons` + `services/service-template`), shared lib auto-configuration (log JSON · error response thống nhất · correlation id · health check · env validator fail-fast), service mẫu `/health` `/ready` `/version` + graceful shutdown + Dockerfile multi-stage non-root, thêm vào compose, JaCoCo gate ≥80% cho config/health, README "tạo service mới từ template". ADR-[0003](docs/adr/0003-spring-boot-version-and-module-layout.md) + ADR-[0004](docs/adr/0004-service-commons-shared-library.md). Verify thật: 72 test pass, 5/5 container healthy, `/ready` 200→503 khi tắt Postgres, fail-fast khi thiếu `APP_ENV`, log JSON đủ 4 field, graceful shutdown hoàn tất request 8s. Phase 0 → **7 / 8**. |
| 2026-11-02 | Sửa 3 bug tiềm ẩn lộ ra khi có code Java thật: **surefire 2.17** (Tests run: 0 mà vẫn xanh) → pin `3.6.1`; thiếu **`-parameters`** → lỗi runtime ở `@RequestParam`; **Spotless ↔ Checkstyle xung đột** ở block rỗng → bật `allowEmpty*` trong `checkstyle.xml`. |
| 2026-11-02 | Verify AC "tạo service mới từ template" bằng cách **thực sự** copy ra `smoke-service` theo README → phát hiện test hard-code tên service (template không copy-safe) → sửa test đọc tên từ config; chạy lại `BUILD SUCCESS` rồi xoá `smoke-service`. |
| 2026-11-02 | Tạo PR [#4](https://github.com/anchu3003/shop-lab-microservices/pull/4) cho `SL-0.4` (nhánh `feat/sl-0.4-service-template`, 5 commit). Merge bằng **squash** theo `docs/git-flow.md`. |
| 2026-11-03 | `SL-0.5` → `In Progress`: nhánh `feat/sl-0.5-ci-pipeline`. Chốt với user: **giữ remote HTTPS** (token VS Code có scope `workflow`, không cần đổi SSH). |
| 2026-11-03 | `SL-0.5` → `Review`: `.github/workflows/ci.yml` (job `lint` ∥ `test` → `build-image`; cache Maven; `concurrency` theo tên nhánh; `timeout-minutes`; `permissions: contents: read`), badge CI + mục **CI (GitHub Actions)** trong `README.md`, `architecture.md` §10.3 + Changelog. **Baseline run đầu: 2m35s** (3/3 job xanh). Test âm thật: `lint` đỏ (`HealthController.java:54:5 ... [NeedBraces]`), `test` đỏ (`AppEnvTest.shouldReportLocalOnly:48`) — cả hai lần `build-image` **skipped**. PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5). |
| 2026-11-03 | `SL-0.5`: **sửa lỗi thiết kế `concurrency`** — gom push+PR chung group làm run PR huỷ run push, để lại check `cancelled` trên commit; đổi sang `group: ${{ github.workflow }}-${{ github.ref }}`. Cập nhật `README.md` + ticket + learning log. |
| 2026-11-03 | Merge PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5) bằng **squash** (`60e8afe`). Run trên `main` xanh: 3/3 job, 2m13s → badge CI `passing`. |
| 2026-11-03 | Đóng `SL-0.5` → `Done`: **6/6 AC** đạt. **Phase 0 → 8 / 8 ✅ Done** — điều kiện hoàn thành phase đã đủ: `docker compose up` infra `healthy` · service template `/health` 200 · CI xanh · `architecture.md` §11 không còn chữ "cần chốt". Việc tiếp theo: breakdown **Phase 1** (epic → ticket chi tiết). |
| 2026-11-03 | **Tách archive**: chuyển toàn bộ Phase 0 (8 ticket + bối cảnh/AC/quyết định/verify/learning note) sang [`Backlog-phase-0.md`](Backlog-phase-0.md); `Backlog.md` còn **260 dòng** (bảng phase · convention · pointer + bảng archive · epic Phase 1–8 · Learning log · Changelog). Cập nhật tham chiếu ở `README.md`, `architecture.md`, ADR-0001…0004, `.clinerules/00-context.md`, `AGENTS.md` §1/§6. |
| 2026-11-03 | **Sửa rule git**: commit minor (đóng ticket / update backlog / sửa doc / tách archive) **không** tạo nhánh + PR riêng — gộp vào nhánh của **ticket kế tiếp**; chỉ tách PR riêng khi hết ticket kế tiếp. Ghi vào `AGENTS.md` §6 · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` · workflow `/close-ticket.md` (bước 8–9). |
| 2026-11-03 | Chốt **ADR-[0005](docs/adr/0005-phase-snapshot-and-release.md)** + dựng `docs/phases/` ([index](docs/phases/README.md) + [`phase-0.md`](docs/phases/phase-0.md)): tag `phase-N` + **GitHub Release** + doc snapshot, **không** dùng release branch ("lazy D"). `AGENTS.md` §5 thêm **DoD của một phase**; `/close-ticket.md` thêm bước 8 & 10 (tách archive, tag/release sau merge). |




