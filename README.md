# ShopLab — microservices learning lab

[![CI](https://github.com/anchu3003/shop-lab-microservices/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/anchu3003/shop-lab-microservices/actions/workflows/ci.yml)

> Sàn thương mại điện tử multi-seller dựng lại từ đầu theo **microservices**, mục đích **học
> architecture & system design** ở quy mô high traffic. **Không phải sản phẩm thật.**

| | |
|---|---|
| Kiến trúc, tech stack, roadmap | [`architecture.md`](architecture.md) |
| Ticket & tiến độ theo phase | [`Backlog.md`](Backlog.md) |
| Archive Phase 0 (chi tiết ticket + verify) | [`Backlog-phase-0.md`](Backlog-phase-0.md) |
| Snapshot từng phase (tag + release + số liệu) | [`docs/phases/`](docs/phases/README.md) |
| Cách làm việc (working agreement) | [`AGENTS.md`](AGENTS.md) |
| Quyết định kiến trúc (ADR) | [`docs/adr/`](docs/adr/) |
| Git workflow | [`docs/git-flow.md`](docs/git-flow.md) |

**Trạng thái hiện tại**: Phase 0 ✅ **Done (8/8)** · Phase 1 ✅ **Done (tag `phase-1`)** —
chi tiết ở [`Backlog-phase-0.md`](Backlog-phase-0.md) và [`docs/phases/phase-1.md`](docs/phases/phase-1.md).
Phase 2 (Core commerce flow) 🔵 **In Progress**: đã có `inventory-service` (8084 — reserve/commit/release chống oversell)
`promotion-service` (8086 — voucher lock/commit/release), `order-service` (8087 — đơn hàng idempotent) và
`payment-service` (8088 — payment intent + webhook idempotent) và `cart-service` (8083 — giỏ hàng Redis); tiếp theo
là **saga checkout** (`SL-2.6`).
Xem [Backlog.md](Backlog.md).

---

## Yêu cầu môi trường

Tất cả công cụ cài **trong WSL2** (không cài trên Windows) — lý do: [`docs/adr/0002-dev-environment.md`](docs/adr/0002-dev-environment.md).
Repo phải nằm trong **Linux filesystem** (`~/...`), **không** ở `/mnt/d/...`.

| Công cụ | Version cần | Trạng thái trên máy này | Lệnh cài (trong WSL2) |
|---|---|---|---|
| WSL2 | ≥ 2.1.5 | cần kiểm tra (`wsl --version` trên PowerShell) | — |
| Docker | 29.x | ✅ `29.9.2` | Docker Desktop bật **WSL integration** |
| Python | 3.12.x | ✅ `3.12.9` (chỉ dùng cho script phụ trợ) | có sẵn trong WSL |
| **JDK 21** | Temurin/OpenJDK 21 | ✅ `21.0.12.1` | `sudo apt update && sudo apt install -y openjdk-21-jdk` |
| **Maven** | 3.9.x | ✅ `3.9.12` (`/usr/bin/mvn`) | `sudo apt install -y maven` |
| **Lefthook** | ≥ 2.0 | ✅ `2.2.2` (`~/.local/bin`) | xem mục *Git hooks* bên dưới |
| Go | 1.2x | ⏸️ để Phase 2–3 (ADR-0001) | — |
| Git | ≥ 2.4x | ✅ | có sẵn |

Kiểm tra nhanh:

```bash
java -version && mvn -v && lefthook version && docker version --format '{{.Server.Version}}'
```

> ⚠️ **Bẫy đã gặp**: `mvn` mặc định trên máy này trỏ vào bản **Windows**
> (`/mnt/d/Programs/Maven/...`) nên báo `JAVA_HOME is not defined correctly`. Sau khi cài Maven
> trong WSL, kiểm tra `which mvn` phải ra `/usr/bin/mvn`.

Sau khi cài, set JAVA_HOME (thêm vào `~/.bashrc`):

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

---

## Git hooks (Lefthook)

Cài **1 lần** trong WSL (không cần sudo, cài vào `~/.local/bin`):

```bash
mkdir -p ~/.local/bin
curl -sSL https://github.com/evilmartians/lefthook/releases/latest/download/lefthook_$(uname -s | tr '[:upper:]' '[:lower:]')_$(uname -m | sed 's/x86_64/amd64/').gz \
  | gunzip > ~/.local/bin/lefthook
chmod +x ~/.local/bin/lefthook
export PATH="$HOME/.local/bin:$PATH"     # thêm vào ~/.bashrc để giữ lâu dài
lefthook install
```

Hooks đang bật (xem `lefthook.yml`):

| Hook | Việc |
|---|---|
| `commit-msg` | Chặn commit message sai **Conventional Commits** |
| `pre-commit` | Format Java (Spotless) + quét secret sơ bộ |
| `pre-push` | **Chặn push trực tiếp lên `main`** — thay cho branch protection (repo private + gói Free không bật được) |

Bỏ qua hook khi thật cần (hạn chế tối đa): `LEFTHOOK=0 git commit -m "..."`.

---

## Cấu trúc thư mục

```
.
├── services/                     # mỗi service 1 thư mục kebab-case — Phase 1+
│   ├── service-template/         # service mẫu (SL-0.4) — copy ra để tạo service mới
│   ├── identity-service/         # định danh + JWT RS256 + JWKS (SL-1.1) — DB riêng, Flyway
│   └── api-gateway/              # cửa vào duy nhất: routing + verify JWT (SL-1.2) — KHÔNG có DB
├── packages/                     # shared library hạ tầng (không chứa business logic của service)
│   └── service-commons/          # log JSON · error response · correlation id · health check · env validator
├── infra/                        # docker compose, init SQL, config hạ tầng local — SL-0.3
│   ├── docker-compose.yml        # Postgres · Redis · RabbitMQ · Adminer · service-template · identity-service
│   ├── init/                     # script tạo database lần đầu (chạy 1 lần khi volume rỗng)
│   └── keys/                     # khoá JWT dev — SINH bằng tools/gen-dev-keys.sh, KHÔNG commit
├── tools/                        # script phụ trợ (git hooks, reset infra...)
├── config/checkstyle/            # Checkstyle config cho Java
├── docs/adr/                     # Architecture Decision Records
├── docs/phases/                  # Snapshot từng phase (tag + release) — ADR-0005
├── .github/workflows/            # CI (GitHub Actions) — SL-0.5
├── .cline/ .clinerules/          # AI tooling (skill / workflow / rule) — xem AGENTS.md §10
└── pom.xml                       # Maven parent POM (convention build/lint + version tập trung)
```

Maven là **multi-module**: parent `pom.xml` khai `<modules>`, version Spring Boot quản tập trung qua BOM.

---

## Lint, format & test

```bash
# Java — format (sửa file) rồi check (CI dùng check)
mvn -q spotless:apply
mvn -q verify                 # spotless:check + checkstyle:check + test + IT + jacoco:check
mvn -q verify -DskipITs       # bỏ qua integration test (máy không có Docker)
```

Convention: **Spotless + google-java-format** (format) · **Checkstyle** (lint) · **JUnit 5/6 + AssertJ** (test)
· **JaCoCo** (coverage gate ≥ 80% LINE cho package `config` + `health` + `auth/jwt`).
Line-ending do `.gitattributes` ép **LF**; editor theo `.editorconfig`.

**Unit test vs integration test** — tách bằng tên file, không bằng comment:

| Loại | Tên file | Chạy bởi | Cần gì |
|---|---|---|---|
| Unit | `*Test.java` | surefire (phase `test`) | chỉ JVM — chạy ở mọi máy |
| Integration | `*IT.java` | **failsafe** (phase `integration-test`) | **Docker** (Testcontainers) |

Service có DB dùng **Testcontainers** cho IT (Postgres thật, Flyway migrate thật). Cố ý **không** auto-skip khi
thiếu Docker — skip im lặng sẽ tạo cảm giác "đã verify" trong khi thực tế chưa chạy gì; máy không có Docker thì
dùng `-DskipITs` và biết rõ là đã bỏ qua.


---

## CI (GitHub Actions) — `SL-0.5` (dựng) + `SL-1.0` (tối ưu chi phí)

> ⚠️ **Đang TẮT tạm thời** (`SL-3.10`, 2026-11-08): `gh variable get CI_ENABLED` → `false`, để tăng tốc merge
> Phase 3 (~5–6 phút/PR). Hệ quả: PR **không có check nào**; merge bằng
> `bash tools/merge-pr.sh <PR> --summary-file f --skip-ci-check`; verify chuyển sang **local**
> (`mvn -B -ntp verify` + 3 script e2e) và **phải dán vào ticket**. Bật lại: `gh variable delete CI_ENABLED`.
> Theo dõi ở [`Backlog.md` §7](Backlog.md) → `TD-28`.

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy khi **push lên `main`** và khi **mở/cập nhật PR vào `main`**
(PR ở dạng **draft bị bỏ qua** cho tới khi bấm *Ready for review*). Không có CI thì "code chạy máy tôi" sẽ âm thầm phá
build khi số service tăng lên ở Phase 1+.

| Job | Quan hệ | Lệnh | Chặn cái gì |
|---|---|---|---|
| `lint` | ∥ song song | `mvn -B -ntp checkstyle:check` rồi `mvn -B -ntp spotless:check` | vi phạm Checkstyle (log hiện `file:line`) + code chưa format |
| `test` | ∥ song song | `mvn -B -ntp verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` | test đỏ + coverage < 80% LINE (JaCoCo gate) |
| `build-image` | sau `lint` + `test` | **matrix**, 1 job con/service: `docker build -f services/<service>/Dockerfile --build-arg SKIP_TESTS=true …` | Dockerfile / image build hỏng **của từng service** |

Ngoài `ci.yml` còn workflow [`e2e.yml`](.github/workflows/e2e.yml) (`SL-2.8`): dựng **cả stack** bằng compose rồi chạy
`e2e-smoke.sh` + `e2e-checkout.sh`. Chạy theo **`workflow_dispatch`** + **`schedule` hằng tuần**, **không** trên PR
(≈30 phút/run vì mỗi image build phải `mvn package` từ đầu) ⇒ `ci.yml` mới là thứ chặn merge, `e2e.yml` là lưới an toàn
bắt drift của compose/Dockerfile/script.

Vì sao tách 3 job: CI UI chỉ rõ job nào đỏ (đọc log nhanh), `lint` và `test` độc lập nên chạy song song, còn
`build-image` là job nặng nhất nên chỉ chạy khi 2 job kia đã xanh (repo private tính **tiền/phút** Actions theo gói).

**`SL-1.12` — build image cho MỌI service (matrix)**

- Trước `SL-1.12` job này hard-code `service-template` (viết ở `SL-0.5` khi repo chỉ có 1 service) ⇒ Dockerfile của
  `identity-service`/`api-gateway` **không** được CI kiểm: CI xanh nhưng image build có thể hỏng (2 lỗi loại này đã xảy
  ra thật khi chạy tay). Nay `matrix.service: [service-template, identity-service, api-gateway]` + `fail-fast: false`
  (1 service lỗi **không** chặn service khác). **Thêm service mới = thêm 1 dòng** trong `matrix.service`.
- `--build-arg SKIP_TESTS=true`: job `test` đã chạy test cho **cả reactor**, chạy lại test bên trong image chỉ tốn phút
  CI (`SL-1.0` đo: `build-image` chiếm 46% phút). `docker build` **tay** vẫn test như trước (mặc định `false`).

**Không chạy CI khi không cần** (`SL-1.0`) — đo bằng API trước khi tối ưu: 18 run = **~71 phút**, trong đó **62% số phút
chỉ là sửa docs** và **49% là run `push` trùng với run `pull_request`**:

| Cơ chế | Cách dùng | Tác dụng |
|---|---|---|
| `paths-ignore` | tự động — run chỉ đổi `**.md`, `docs/**`, `.clinerules/**`, `.cline/**`, `LICENSE` sẽ **không chạy** | bỏ hẳn nhóm run docs |
| `push` chỉ trên `main` | tự động — nhánh feature chỉ chạy qua `pull_request` | bỏ run trùng push/PR |
| **Kill-switch** | `gh variable set CI_ENABLED --body false` (tắt) · `--body true` (bật) | tạm dừng CI **không cần sửa file** — job bị `skipped`, không đốt phút |
| **Bỏ qua draft PR** | mở PR dạng **draft** → không chạy; bấm **Ready for review** → chạy | không đốt phút cho WIP |
| `[skip ci]` | thêm vào commit message để bỏ qua 1 commit cụ thể | cho commit docs lẻ |
| Tắt hẳn workflow | `gh workflow disable ci.yml` · `gh workflow enable ci.yml` | dừng hẳn CI khi làm loạt thay đổi lớn |

> ⚠️ Workflow bị **skip** (do `paths-ignore` hoặc `[skip ci]`) để lại check ở trạng thái **"Pending"** → sẽ **chặn merge**
> nếu sau này bật required checks. Repo hiện private + Free nên chưa bật được branch protection; khi chuyển public/Team
> thì chuyển `paths-ignore` ở trigger sang điều kiện ở **job level** (job bị skip được tính là pass).
>
> `cancel-in-progress` chỉ huỷ run cũ **cùng ref** (push liên tiếp cùng nhánh / PR cập nhật liên tục) — cố ý không gom
> `push` + `pull_request` chung group vì làm vậy sẽ để lại check `cancelled` sai trên commit.

Tái hiện đúng lệnh CI ở máy:

```bash
mvn -B -ntp checkstyle:check && mvn -B -ntp spotless:check             # job lint
mvn -B -ntp verify -Dcheckstyle.skip=true -Dspotless.check.skip=true  # job test
# job build-image (matrix) — lặp cho từng service trong matrix.service:
for s in service-template identity-service api-gateway; do
  docker build -f services/$s/Dockerfile \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg SKIP_TESTS=true -t shoplab-$s:ci .
done
```

Vài điểm cần biết:

- **Badge ở đầu README** lấy trạng thái run **mới nhất trên `main`**. Repo đang **private** → badge chỉ render
  khi trình duyệt **đã đăng nhập GitHub**; khách chưa login sẽ thấy ảnh vỡ (không phải CI hỏng).
- CI **chưa** push image lên registry và **chưa** deploy — để Phase 7. Job `build-image` (matrix) chỉ build rồi liệt kê
  image của **cả 3 service** (`service-template`, `identity-service`, `api-gateway`).
- Job `build-image` pull base image từ Docker Hub **anonymous** → có thể gặp rate limit; nếu tái diễn thì đổi
  sang mirror/registry nội bộ.


---

## Cách chạy

### Hạ tầng local (Docker Compose) — `SL-0.3`

Cần chạy trước khi làm bất cứ gì ở Phase 1+ (service cần Postgres/Redis/broker).

```bash
# 1) Tạo .env từ template (1 lần; .env đã nằm trong .gitignore)
cp .env.example .env

# 2) Dựng hạ tầng
docker compose -f infra/docker-compose.yml --env-file .env up -d

# 3) Kiểm tra
docker compose -f infra/docker-compose.yml --env-file .env ps     # tất cả (healthy)

# Dừng (giữ dữ liệu) / dừng + xoá sạch dữ liệu
docker compose -f infra/docker-compose.yml --env-file .env down
bash tools/reset-infra.sh
```

> ⚠️ **Bẫy `--env-file`**: `docker compose` nạp file `.env` theo **project directory** — mặc định là
> **thư mục chứa file compose** (`infra/`), **không** phải thư mục bạn đang đứng. Vì `.env` nằm ở gốc
> repo nên **bắt buộc** truyền `--env-file .env`. Bỏ đi sẽ lỗi
> `required variable POSTGRES_PASSWORD is missing a value`.

**Port dùng cho gì** (không trùng nhau):

| Port | Service | Dùng để |
|---|---|---|
| `5432` | PostgreSQL 16 | 1 instance, 7 database (database per service) |
| `6379` | Redis 7 | cache / lock / rate limit |
| `5672` | RabbitMQ 4 | AMQP — producer/consumer |
| `15672` | RabbitMQ management | UI: http://localhost:15672 (`shoplab` / password trong `.env`) |
| `8085` | Adminer | UI xem dữ liệu Postgres: http://localhost:8085 |
| `8090` | `service-template` | service mẫu (SL-0.4) — chỉ chạy khi bật profile `template` |
| `8080` / `8081` / `8082` | `api-gateway` / `identity-service` / `catalog-service` | Phase 1 |
| `8084` | `inventory-service` | Phase 2 (`SL-2.2`) — `reserve`/`commit`/`release`; DB `inventory` |
| `8086` | `promotion-service` | Phase 2 (`SL-2.4`) — voucher `lock`/`commit`/`release`; DB `promotion` |
| `8087` | `order-service` | Phase 2 (`SL-2.5`) — đơn hàng idempotent + state machine; DB `order` |
| `8088` | `payment-service` | Phase 2 (`SL-2.7`) — payment intent + webhook provider idempotent; DB `payment` |
| `8083` | `cart-service` | Phase 2 (`SL-2.1`) — giỏ hàng trên **Redis** (không có Postgres) |
| `8089` | `notification-service` | Phase 3 (`SL-3.4`) — consumer `q.notification` (email mock) + `GET /v1/notifications/me`; DB `notification` |
| `8091` | `search-service` | Phase 3 (`SL-3.6`) — index sản phẩm từ event + `GET /v1/search` (Postgres FTS) + `POST /v1/admin/search/reindex`; DB `search` |

> `up -d` mặc định **KHÔNG** dựng `service-template` — service mẫu nằm trong profile `template` (`SL-1.13`). Bật khi cần:
> `docker compose -f infra/docker-compose.yml --env-file .env --profile template up -d service-template`

**Database có sẵn sau lần init đầu tiên**: `identity`, `catalog`, `inventory`, `promotion`, `order`,
`payment`, `notification` (+ DB `shoplab` làm DB "main"). Danh sách này sinh từ biến `POSTGRES_DB_*`
trong `.env` — xem `infra/init/01-create-databases.sh`.

**Kết nối từ máy host** — host chưa cài `psql`/`redis-cli` thì chạy client qua container (vẫn đi qua
port đã publish):

```bash
# Postgres — thay <db> bằng tên database
docker run --rm -it --network host -e PGPASSWORD="$(grep ^POSTGRES_PASSWORD .env | cut -d= -f2)" \
  postgres:16-alpine psql -h 127.0.0.1 -U shoplab -d <db>

# Redis — LƯU Ý: Redis có requirepass nên `redis-cli ping` trần sẽ báo NOAUTH
docker run --rm -it --network host redis:7-alpine \
  redis-cli -h 127.0.0.1 -a "$(grep ^REDIS_PASSWORD .env | cut -d= -f2)" --no-auth-warning ping   # PONG
```

Muốn dùng client native: `sudo apt install -y postgresql-client redis-tools`.


### Broker (RabbitMQ) — `SL-3.1`

`rabbitmq` chạy từ Phase 0 nhưng **chỉ từ `SL-3.1`** mới thật sự có topology (exchange/queue/DLQ). Topology
được **khai bằng code** trong `service-commons` (`EventTopologyAutoConfiguration`, tên lấy từ
[ADR-0007](docs/adr/0007-event-contract.md)) — service nào có `spring-boot-starter-amqp` + bật
`shoplab.events.topology.enabled` (mặc định bật) thì khai; hiện `order-service` là service đầu tiên.

```bash
bash tools/rabbit-topology.sh                     # exchange + queue (ready/unacked/consumers) + binding
bash tools/rabbit-topology.sh dlq                 # chỉ xem các queue .dlq
bash tools/rabbit-topology.sh publish order.confirmed.v1       # gửi 1 message test
bash tools/rabbit-topology.sh drain q.notification 1           # lấy & ack 1 message (kiểm tra consume được)
bash tools/rabbit-topology.sh purge q.notification             # xoá sạch message trong queue
# UI (chỉ local): http://localhost:15672 — user `shoplab`, password trong .env
```

**Đọc 3 con số** (bảng `queues`):

| Số | Nghĩa | Khi nào đáng lo |
|---|---|---|
| `ready` | message đang chờ consumer | tăng liên tục ⇒ consumer chết/chậm |
| `unacked` | consumer **đang giữ** (đang xử lý hoặc đang retry) | > 0 lâu ⇒ consumer treo (giữ message không `ack`) |
| `consumers` | số consumer đang attach | `0` trên queue nghiệp vụ ⇒ service tương ứng chưa chạy |

**Bốn bẫy đã gặp** (đừng mất thời gian tìm lại):

1. **Topology không xuất hiện dù code đã khai**: `RabbitAdmin` khai **lazy** — chỉ khi có connection đầu tiên.
   Service chưa publish/consume ⇒ không ai mở connection ⇒ không có queue. Fix: `EventTopologyDeclarer` gọi
   `amqpAdmin.initialize()` lúc khởi động (fail mềm, broker chết không làm service chết).
2. **Số liệu `/api/queues` lag ~5 s** sau restart: vừa `docker restart` xong đọc thấy `ready=0` **không** có
   nghĩa là message mất — chờ vài giây hoặc kiểm bằng `drain`.
3. **Đổi `durable`/argument của queue đã tồn tại** ⇒ `PRECONDITION_FAILED`. Máy dev: xoá queue (UI hoặc
   `docker compose down -v`) rồi để service khai lại.
4. **`routed=false` khi publish** = không queue nào bind routing key đó (thường là gõ sai routing key) —
   message **mất im lặng**, không lỗi nào được ném ra.

### DLQ: xem & replay (`SL-3.4`)

`manager` UI đọc được nhưng replay bằng tay rất dễ sai. Dùng tool:

```bash
bash tools/dlq.sh list                     # DLQ nào đang có message (ready=…)
bash tools/dlq.sh peek q.notification      # xem 1 message — KHÔNG consume
bash tools/dlq.sh move q.notification      # replay 1 message về queue chính (routing key GỐC)
bash tools/dlq.sh purge q.notification     # xoá sạch .dlq (chỉ khi đã hiểu vì sao message vào đó)
```

> ⚠️ **Bẫy quan trọng**: trong DLQ, `routing_key` của message là **tên queue** (`q.notification`) — DLX đặt
> routing key = tên queue (`ADR-0007` D2). Routing key **gốc** nằm ở header `x-death[0].routing-keys`. Publish lại
> bằng tên queue ⇒ `routed=false` ⇒ **message mất**. `dlq.sh move` đọc `x-death`, `peek` trước rồi mới `ack`, nên
> khi parse lỗi nó **không consume gì**.

**RAM baseline** (đo lúc idle, `docker stats --no-stream`, WSL2/Docker 29.9.2):

| Container | RAM idle |
|---|---|
| `shoplab-postgres` | ~25 MiB |
| `shoplab-redis` | ~4.5 MiB |
| `shoplab-rabbitmq` | ~106 MiB |
| `shoplab-adminer` | ~13 MiB |
| `shoplab-service-template` | ~150 MiB (JVM, idle) |
| **Tổng** | **~150 MiB hạ tầng** + ~150 MiB cho mỗi service Java |

→ Dùng làm baseline cho Phase 7 (K8s resource request). Lưu ý: **chưa** gồm OpenSearch/MinIO
(chưa dựng ở SL-0.3) — hai cái này nặng hơn nhiều, cần chỉnh `%UserProfile%\.wslconfig`.

**Còn thiếu (cố ý, xem [`Backlog-phase-0.md`](Backlog-phase-0.md) → SL-0.3 out of scope)**: MinIO (object storage) và OpenSearch 2.x
(search) — dựng khi phase cần thật; observability stack (Phase 6); K8s (Phase 7).

### search-service — `SL-3.6`

Index sản phẩm (Postgres **FTS**), port **8091**, DB `search`. **Không** phải nguồn sự thật: nó học từ
event của `catalog` (`q.search`) và có thể dựng lại từ catalog bất cứ lúc nào.

```bash
# Tìm kiếm (PUBLIC — guest cũng gọi được qua gateway)
curl -s 'localhost:8080/v1/search?q=dien%20thoai&size=5'
curl -s 'localhost:8080/v1/search?categoryId=<uuid>&minPrice=1000000&maxPrice=5000000'
bash -c 'curl -s "localhost:8080/v1/search?q=dien%25"'      # ký tự đặc biệt: vô hại, không lỗi

# Dựng lại toàn bộ index từ catalog (ADMIN) — idempotent, chạy lại bao nhiêu lần cũng được
TOKEN="$(curl -s -X POST localhost:8080/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@shoplab.local","password":"matkhau123"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')"
curl -s -X POST localhost:8080/v1/admin/search/reindex -H "Authorization: Bearer $TOKEN"
# → {"scanned":65,"upserted":65,"archived":0}
```

**Cách nó giữ đồng bộ** (chi tiết `architecture.md` §6.4):

| Đường | Trễ | Khi nào dùng |
|---|---|---|
| event `product.updated.v1` / `product.archived.v1` qua outbox | ~1 s (relay polling) | đường thường — catalog không chờ search |
| `POST /v1/admin/search/reindex` | vài giây (đọc cả catalog theo lô 100) | index lệch, đổi máy, xoá dữ liệu tay |
| `@Scheduled` so số row (10′, lệch > 1 % ⇒ log WARN) | — | **phát hiện** lệch, không tự sửa |

```bash
# Kiểm index có khớp catalog không (đọc log job)
docker logs shoplab-search-service --since 15m | grep -E 'INDEX LỆCH|index khớp catalog'
# Xem thẳng 2 con số
docker exec shoplab-postgres psql -U shoplab -d search -tAc \
  "select status, count(*) from search_product group by status"
docker exec shoplab-postgres psql -U shoplab -d catalog -tAc \
  "select count(*) from product where deleted_at is null"
```

**Giới hạn đã biết (nói thật, không giấu)**: `simple` + `unaccent` **không** stem tiếng Việt và **không**
biết từ đồng nghĩa ⇒ `tai nghe` không khớp `headphone`; `minPrice` dùng **giá product**, chưa tính
`sku.price_override` (`TD-33`); phân trang **offset** (`TD-6`). Phase 8 đổi sang OpenSearch: contract
event + API giữ nguyên.

> ⚠️ Database `search` được tạo bởi `infra/init/01-create-databases.sh` **chỉ ở lần init volume đầu tiên**.
> Volume cũ có sẵn thì tạo tay một lần:
> `docker exec shoplab-postgres psql -U shoplab -d shoplab -c "CREATE DATABASE search OWNER shoplab"`

### Service template (`SL-0.4`)

`service-template` là service mẫu — **không có business logic**, chỉ chứng minh hạ tầng dùng chung chạy được.
Đây là thứ để copy ra thành service thật ở Phase 1.

| Endpoint | Trả về | Dùng cho |
|---|---|---|
| `GET /health` | `200` + `{"status":"UP"}` | **liveness** — cố ý KHÔNG check dependency |
| `GET /ready` | `200` khi mọi dependency UP, `503` khi có cái DOWN (kèm lý do + latency) | **readiness** |
| `GET /version` | service name · version · git commit · build time | biết đang chạy build nào |
| `GET /debug/slow?ms=N` | giữ request mở N ms | verify graceful shutdown |
| `GET /debug/error` · `GET /debug/boom` | lỗi 404 / 500 theo format chuẩn | verify error response |

`/debug/*` chỉ tồn tại khi `SHOPLAB_DEBUG_ENDPOINTS=true` (chỉ set trong compose local).

> ⚠️ `service-template` nằm trong profile `template` ⇒ trước khi chạy các lệnh dưới phải bật:
> `docker compose -f infra/docker-compose.yml --env-file .env --profile template up -d service-template`

```bash
curl -s localhost:8090/health  ; curl -s localhost:8090/ready ; curl -s localhost:8090/version

# Readiness thật sự đổi trạng thái khi dependency chết
docker stop shoplab-postgres && curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/ready   # 503
docker start shoplab-postgres
```

**Hai thứ dễ nhầm — đừng gộp lại:**

- **liveness** (`/health`) không được check dependency. Nếu check, DB chết sẽ khiến orchestrator
  **restart cả service đang khoẻ mạnh** → càng chết nặng.
- **readiness** (`/ready`) mới check dependency: trả `503` để load balancer **tạm ngừng đẩy traffic**
  vào instance này, nhưng **không** restart nó.

**Log**: mọi service dùng chung format JSON ở `packages/service-commons/src/main/resources/logback-shoplab.xml`
— field `traceId`, `service`, `level`, `msg`. Muốn đổi format thì sửa **một chỗ** đó.

---

### identity-service — `SL-1.1`

Service thật đầu tiên: đăng ký, đăng nhập, phát hành **JWT RS256** và công bố **JWKS**. DB riêng (`identity`),
schema do **Flyway** migrate lúc khởi động.

```bash
# 1) Sinh khoá JWT dev (1 lần). Private key KHÔNG commit; script in ra 2 dòng cần thêm vào .env
bash tools/gen-dev-keys.sh
#    SHOPLAB_UID=$(id -u)   # container chạy cùng uid để đọc được private key mode 600
#    SHOPLAB_GID=$(id -g)

# 2) Chạy service (kèm Postgres)
docker compose -f infra/docker-compose.yml --env-file .env up -d --build identity-service

# 3) Thử luồng đăng ký → đăng nhập
EMAIL="demo-$(date +%s)@shoplab.local"
curl -s -X POST localhost:8081/v1/auth/register -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"matkhau123\",\"displayName\":\"Demo\"}"
TOKEN=$(curl -s -X POST localhost:8081/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"matkhau123\"}" | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')
curl -s localhost:8081/.well-known/jwks.json          # public key để service khác verify
```

| Endpoint | Trả về | Ghi chú |
|---|---|---|
| `POST /v1/auth/register` | `201` + user (không có hash) · trùng email → `409` | luôn tạo role `BUYER` |
| `POST /v1/auth/login` | `200` + `{accessToken, tokenType, expiresIn}` · sai → `401` | message 401 giống nhau cho mọi trường hợp sai (không lộ email đã đăng ký) |
| `GET /.well-known/jwks.json` | JWK Set (chỉ `n`, `e`, `kid`) | service khác verify **không cần khoá ký** |

Khoá JWT: `iss` phải dạng URI (`https://identity.shoplab.local`) vì Spring Security coi `iss` là URL khi verify.
TTL mặc định 15 phút (`IDENTITY_JWT_ACCESS_TTL`).

---

### Token & phân quyền — `SL-1.4`

Vòng đời token đầy đủ + RBAC cơ bản. Access token là **JWT 15 phút** (stateless); refresh token là **chuỗi opaque
14 ngày**, DB chỉ lưu **hash SHA-256**.

| Endpoint | Trả về | Ghi chú |
|---|---|---|
| `POST /v1/auth/login` | `200` + `{accessToken, refreshToken, tokenType, expiresIn}` | sai → `401` (message giống nhau, không lộ email) |
| `POST /v1/auth/refresh` | cặp token **mới** | **rotate**: token cũ bị vô hiệu ngay; dùng lại token đã rotate ⇒ `401` **và thu hồi cả chuỗi** |
| `POST /v1/auth/logout` | `204` | thu hồi refresh token hiện tại (idempotent) |
| `GET /v1/users/me` | `200` + `{userId, role}` | danh tính lấy **từ token** (identity tự verify, không tin header gateway) |
| `GET /v1/admin/users` | `200` (chỉ **ADMIN**) | buyer → **`403`** (đã xác thực nhưng thiếu quyền) |

**401 vs 403**: 401 = chưa xác thực (thiếu/sai/hết hạn token), 403 = đã xác thực nhưng thiếu quyền. Cả hai trả
**`ApiError`** (`code`, `message`, `traceId`) — không có HTML mặc định của Spring.

**User dev seed sẵn** (chỉ khi `APP_ENV=local`, **không** seed ở môi trường khác — xem `LocalDevUserSeeder`):

| Email | Role | Mật khẩu dev |
|---|---|---|
| `admin@shoplab.local` | `ADMIN` | `matkhau123` |
| `seller@shoplab.local` | `SELLER` | `matkhau123` |

```bash
# login → cặp token
curl -s -X POST localhost:8080/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@shoplab.local","password":"matkhau123"}'
# refresh (rotate) — dùng refreshToken ở trên
curl -s -X POST localhost:8080/v1/auth/refresh -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
# endpoint chỉ ADMIN (buyer sẽ nhận 403)
curl -s localhost:8080/v1/admin/users -H "Authorization: Bearer <accessToken>"
# logout
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/v1/auth/logout \
  -H 'Content-Type: application/json' -d '{"refreshToken":"<refreshToken>"}'    # 204
```

> ⚠️ **Reuse detection là tính năng, không phải lỗi**: nếu client lưu refresh token sai chỗ (2 tab cùng refresh, retry
> sau timeout...) và dùng lại token **đã rotate** thì cả phiên bị thu hồi — buộc đăng nhập lại. Đây là hành vi đúng
> (dấu hiệu token bị đánh cắp); client phải lưu **token mới nhất**.
>
> 🕐 **Grace window 30s (`SL-1.18`)**: token đã rotate trong **≤ 30s** mà bị dùng lại được coi là *lặp/đua request*
> (2 tab, mất mạng, retry) ⇒ server cấp token mới **thay vì** revoke chuỗi, để client hợp lệ không bị logout oan.
> Quá 30s ⇒ revoke cả chuỗi như cũ. Tắt bằng `IDENTITY_REFRESH_REUSE_GRACE=0s`. Logout thì **không** có grace:
> dùng lại token cũ sau logout luôn 401.

---


### catalog-service — `SL-1.5`

Service **read-heavy đầu tiên**: guest xem được sản phẩm, chỉ `seller`/`admin` ghi. DB riêng `catalog`
(Flyway `V1__create_catalog.sql`).

**API**

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `GET /v1/categories` | **public** | danh sách category, sort theo tên |
| `POST /v1/categories` | `seller`/`admin` | 201 + `Location`; trùng slug → **409** |
| `GET /v1/products` | **public** | list phân trang: `page` · `size` (1..100) · `categoryId` · `q` (không phân biệt hoa/thường) · `sort` (`price,asc\|desc` · `createdAt,asc\|desc`) |
| `GET /v1/products/{id}` | **public** | 200 · không tồn tại **hoặc đã xoá mềm** → **404**; trả kèm danh sách `skus` |
| `GET /v1/skus?ids=` | **public** | **batch** giá/tên cho ≤ **100** SKU (1 request) — `SL-2.13`; SKU đã archive/không tồn tại → nằm trong `missingIds` (không 404 cả request); thiếu `ids` / sai UUID / > 100 → **400** |
| `POST /v1/products` | `seller`/`admin` | 201; `categoryId` không tồn tại → **404** |
| `POST /v1/products/{id}/skus` | `seller`/`admin` | 201; `code` trùng → **409**; product đã xoá mềm → **404** |
| `PUT /v1/products/{id}` | `seller`/`admin` | thay toàn bộ field sửa được; `version` +1; product đã xoá mềm → **404** |
| `DELETE /v1/products/{id}` | `seller`/`admin` | **204** — **xoá mềm** (`status=ARCHIVED` + `deleted_at`, SKU archive theo, row vẫn còn — ADR-0006 D5); xoá lần 2 → **404** |

**Hai điểm thiết kế quan trọng**

1. **Service TỰ verify JWT** (defense in depth) — tải public key qua **JWKS của identity**
   (`IDENTITY_JWKS_URI`), không giữ private key. Gọi thẳng `:8082` không token vẫn **401**, nên một
   gateway cấu hình sai không thể biến thành lỗ hổng ghi dữ liệu.
2. **Chỉ tin token, không tin header** `x-user-id`/`x-user-role`. Client tự gửi `x-user-role: admin`
   → không leo được quyền (gateway xoá header; service bỏ qua hoàn toàn).

**Thử nhanh (stack đang chạy)**

```bash
# Token seller có sẵn ở local (LocalDevUserSeeder) — mật khẩu dev: matkhau123
SELLER=$(curl -s -X POST localhost:8080/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"seller@shoplab.local","password":"matkhau123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

# Tạo category + product (ghi cần quyền)
CAT=$(curl -s -X POST localhost:8080/v1/categories -H "Authorization: Bearer $SELLER" \
  -H 'Content-Type: application/json' -d '{"name":"Điện thoại"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
curl -s -X POST localhost:8080/v1/products -H "Authorization: Bearer $SELLER" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CAT\",\"name\":\"iPhone 15\",\"price\":25990000.00}"

# Guest đọc không cần token
curl -s "localhost:8080/v1/categories"
```

`price` là **`BigDecimal` + `NUMERIC(12,2)`** (không dùng `double` cho tiền), `product` có `@Version`
(optimistic locking) nên response của `PUT` trả `version` đã tăng.

**Seed data + list cho guest**

`R__seed_dev_data.sql` (repeatable migration) tạo sẵn **5 category / 20 product / 24 SKU** để mở lên là
có dữ liệu demo; seed viết **idempotent** (`ON CONFLICT`) nên `up -d` nhiều lần **không** nhân đôi.

```bash
curl -s "localhost:8080/v1/products?page=0&size=5"                       # trang đầu (public)
curl -s "localhost:8080/v1/products?categoryId=<uuid>&sort=price,desc"   # filter + sort
curl -s "localhost:8080/v1/products?q=iphone"                            # tìm theo tên (không phân biệt hoa/thường)
# Tra giá nhiều SKU trong 1 request (SL-2.13) — saga checkout + giỏ hàng dùng đường này,
# KHÔNG tin giá do client gửi. Trả {items:[{skuId,productId,skuCode,title,unitPrice}],missingIds:[]}.
curl -s "localhost:8080/v1/skus?ids=<sku-uuid>,<sku-uuid>"
```

Tham số sai (`size` ngoài 1..100 · `page` âm · `sort` lạ) → **400** `VALIDATION_ERROR` — cố ý **không
clamp**, vì clamp làm client tưởng đã lấy đủ dữ liệu. `sort` là **whitelist** (`price`, `createdAt`)
để không lộ schema và không sort trên cột thiếu index.

Xem `docs/` sau `SL-1.7` để có runbook debug từng tầng.

### inventory-service — `SL-2.2`

Tồn kho: giữ chỗ (`reserve`) nguyên tử, chốt hàng (`commit`) khi tiền về, trả chỗ (`release`) khi huỷ/hết hạn.
Chống oversell bằng **conditional `UPDATE`** trên Postgres (ADR-0006 D1) — điều kiện `on_hand - reserved >= qty` nằm
**trong** câu UPDATE, không dùng Redis lock. DB riêng `inventory` (`V1__create_inventory.sql`).

**API** (đều cần token; `PUT` chỉ admin)

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `PUT /v1/stock/{skuId}` | `admin` | đặt `onHand` (upsert) + ghi ledger `ADJUST`; nhỏ hơn `reserved` → **409** |
| `GET /v1/stock/{skuId}` | mọi role | `{onHand, reserved, available}`; chưa có tồn kho → **404** |
| `POST /v1/stock/reserve` | mọi role | `{orderId, items[{skuId, qty}]}` → **201** (+`reservationId`, TTL 15′); retry cùng `orderId` → **200** cùng id; thiếu hàng → **409** + `details` |
| `POST /v1/stock/commit` | mọi role | `{reservationId}` → **200** (idempotent); quá TTL → **410**; đã release → **409** |
| `POST /v1/stock/release` | mọi role | `{reservationId}` → **200** (idempotent); đã commit → **409** |

**Sweeper TTL** (`SL-2.3`): job nền dọn reservation `PENDING` quá hạn (user bỏ ngang) ⇒ trả chỗ, tránh "cháy hàng
ảo". Mặc định 60 s/nhịp, batch 100, `FOR UPDATE SKIP LOCKED` (nhiều instance vẫn an toàn). Chỉnh bằng env:
`SHOPLAB_INVENTORY_SWEEP_INTERVAL_MS` · `SHOPLAB_INVENTORY_SWEEP_INITIAL_DELAY_MS` · `SHOPLAB_INVENTORY_SWEEP_BATCH_SIZE`.

Đối soát tồn kho (ledger append-only, 2 cột delta):

```sql
SELECT sum(delta_on_hand), sum(delta_reserved) FROM stock_ledger WHERE sku_id = '<sku>';
-- phải bằng (on_hand, reserved) trong bảng stock
```

### promotion-service — `SL-2.4`

Voucher: admin tạo mã (PERCENT có trần `maxDiscount`, hoặc FIXED), user **giữ 1 lượt** khi checkout rồi commit khi tiền về.
Bất biến chống bán quá quota là **`used + locked <= quota`**, do CHECK constraint ở DB canh + conditional `UPDATE` khi lock
(cùng pattern với inventory, ADR-0006). DB riêng `promotion` (`V1__create_promotion.sql`).

**API** (`lock/commit/release` cần token; `POST/GET` voucher chỉ admin)

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `POST /v1/vouchers` | `admin` | 201; mã chuẩn hoá UPPER; trùng mã → **409**; PERCENT > 100 / `maxDiscount` cho FIXED / `endAt <= startAt` → **400** |
| `GET /v1/vouchers/{code}` | `admin` | `{quota, used, locked, available, ...}` |
| `POST /v1/vouchers/lock` | mọi role | `{code, orderId, subtotal}` → **201** `{lockId, discount, finalTotal, expiresAt}` (TTL 15′); retry cùng `orderId` → **200** cùng id; không dùng được → **409** (chưa tới / hết hạn / hết lượt / dưới `minOrder`) |
| `POST /v1/vouchers/commit` | mọi role | `{lockId}` → **200** (`used+1, locked−1`, idempotent); quá TTL → **410**; đã release → **409** |
| `POST /v1/vouchers/release` | mọi role | `{lockId}` → **200** idempotent; đã commit → **409** |

Sweeper (`SL-2.4`) trả lượt cho lock bị bỏ quên: mặc định 60 s/nhịp, batch 100, `FOR UPDATE SKIP LOCKED`. Chỉnh bằng
`SHOPLAB_PROMOTION_SWEEP_INTERVAL_MS` · `_INITIAL_DELAY_MS` · `_BATCH_SIZE` · `PROMOTION_LOCK_TTL`.

### cart-service — `SL-2.1`

Giỏ hàng — service **duy nhất không có Postgres**: Redis là source of truth, TTL **30 ngày** là cơ chế dọn dữ liệu
(`architecture.md` §8.3/§8.5 — mất giỏ chỉ gây khó chịu, không sai sổ sách).

| Method + path | Auth | Ghi chú |
|---|---|---|
| `POST /v1/carts/guest` | **public** | cấp `{cartToken}` do **server** sinh (UUID, TTL 30d) — khách chưa đăng nhập vẫn có giỏ |
| `GET /v1/carts` | JWT **hoặc** `X-Cart-Token` | giỏ rỗng vẫn **200** (trạng thái hợp lệ, không phải 404); `ttlSeconds` cho biết giỏ sắp bị dọn |
| `PUT /v1/carts/items/{skuId}` | JWT **hoặc** `X-Cart-Token` | `{qty}` = số lượng **tuyệt đối** ⇒ idempotent; `qty` ngoài `1..999` → **422**; quá `maxLines` → **422** |
| `DELETE /v1/carts/items/{skuId}` | JWT **hoặc** `X-Cart-Token` | xoá một dòng |
| `DELETE /v1/carts` | JWT **hoặc** `X-Cart-Token` | xoá cả giỏ |
| `POST /v1/carts/merge` | **JWT + `X-Cart-Token`** | cộng dồn qty vào giỏ user, cap 999, xoá giỏ guest + thu hồi token ⇒ gọi lại **không** cộng lần 2 |

**Vì sao Lua**: một dòng giỏ là JSON nên `HINCRBY` không dùng được; đọc ở Java rồi ghi lại = **lost update** khi hai
request cùng SKU. Script cũng giữ bất biến `qty 1..999` + `maxLines` ngay trong Redis (kiểm ngoài = TOCTOU).

**Bảo mật**: gateway để `/v1/carts/**` public (guest không có JWT) ⇒ cart tự phân xử: có JWT → giỏ user; không có →
**bắt buộc** `X-Cart-Token` do server cấp (token bịa/bị thu hồi → 401). Giỏ **không** lưu giá — tiền thật do
`order-service` tính lại (`TD-20`).

### payment-service — `SL-2.7`

Thanh toán: tạo **payment intent** idempotent, mô phỏng cổng thanh toán bằng **mock provider** (Phase 2 chưa nối provider
thật), và nhận **webhook** của provider theo cách idempotent. DB riêng `payment` (`V1__create_payment.sql`).

**Vì sao có hai cơ chế idempotency** — hai nguồn gây trùng khác nhau:

| Nguồn trùng | Cơ chế | Khoá |
|---|---|---|
| Client bấm "thanh toán" 2 lần | `idempotency_keys` | `(scope, Idempotency-Key)`, scope = `"<userId>:POST /v1/payment-intents"` |
| Provider gọi webhook N lần | `payment_webhook_events` + `payments.provider_txn_id` | `provider_event_id` (PK) và `provider_txn_id` (UNIQUE) |

**API**

| Method + path | Auth | Ghi chú |
|---|---|---|
| `POST /v1/payment-intents` | JWT + **`Idempotency-Key`** | 201 · 200 replay (`Idempotent-Replayed`) · 422 key dùng lại · 409 request trước chưa xong · 400 thiếu header; mỗi đơn **tối đa 1 intent sống** (cùng tiền → trả intent cũ, khác tiền → 409) |
| `GET /v1/payment-intents/{id}` | chủ intent / admin | người khác → **404** |
| `GET /v1/payment-intents?orderId=` | JWT | buyer chỉ thấy intent của mình |
| `POST /v1/webhooks/payment` | **public + HMAC** | `X-Provider-Signature` = HMAC-SHA256(`timestamp + "\n" + raw body`), `X-Provider-Timestamp` lệch > 5 phút → 401; trùng `eventId` → 200 `duplicate: true`, **không ghi lại** |
| `POST /mock-provider/pay/{providerIntentId}[?fail=true]` | **public, chỉ local** | mô phỏng khách thanh toán; đi qua đúng đường webhook (ký + verify + dedupe). `PAYMENT_MOCK_ENABLED=true` ở `APP_ENV` khác local ⇒ **service không khởi động** |

**Bất biến tiền do DB canh**: `amount > 0`, `status = SUCCEEDED ⇔ paid_at IS NOT NULL`, `provider_txn_id UNIQUE`.
Env quan trọng: `PAYMENT_WEBHOOK_SECRET` (**bắt buộc**, không có default), `PAYMENT_INTENT_TTL` (15m), `PAYMENT_WEBHOOK_TOLERANCE` (5m), `PAYMENT_MOCK_ENABLED`, `PAYMENT_MOCK_LATENCY`.

### order-service — `SL-2.5`

Đơn hàng: tạo **idempotent** theo `Idempotency-Key`, `order_items` lưu **snapshot** giá/tên SKU (hoá đơn bất biến), và
state machine một chiều `PENDING_PAYMENT → CONFIRMED | CANCELLED | EXPIRED` (mỗi lần chuyển đều ghi
`order_status_history`). DB riêng `order` (`V1__create_order.sql`).

**API** (đều cần token; `userId` lấy từ JWT, không nhận từ body)

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `POST /v1/orders` | đã đăng nhập | **bắt buộc** header `Idempotency-Key`; 201 mới · **200 + `Idempotent-Replayed: true`** khi retry cùng key+payload · **422** khi cùng key payload khác · **409** khi request trước chưa xong · **400** thiếu header |
| `GET /v1/orders/{id}` | chủ đơn / admin | đơn của người khác → **404** (không lộ sự tồn tại) |
| `GET /v1/orders?page=&size=` | đã đăng nhập | buyer: đơn của mình; admin: tất cả |
| `POST /v1/orders/{id}/confirm` | chủ đơn / admin | PENDING → CONFIRMED (gọi lại → 200, không ghi history lần 2) |
| `POST /v1/orders/{id}/cancel` | chủ đơn / admin | PENDING → CANCELLED (`{reason}` ghi vào history); đơn đã CONFIRMED → **409** |
| `POST /v1/orders/{id}/expire` | chủ đơn / admin | PENDING → EXPIRED (saga/`SL-2.9` sẽ gọi kèm release stock+voucher) |

**Bất biến tiền do DB canh**: `total = subtotal − discount`, `line_total = unit_price × qty` (CHECK) — không phụ thuộc
code đúng. Job dọn `idempotency_keys` (COMPLETED > 24 h, IN_PROGRESS > 1 h) chạy mỗi giờ, chỉnh bằng
`ORDER_IDEMPOTENCY_RETENTION` · `ORDER_IDEMPOTENCY_STALE_AFTER` · `SHOPLAB_ORDER_SWEEP_INTERVAL_MS`.

### checkout saga — `SL-2.6a`

`POST /v1/checkout` biến `order-service` thành **orchestrator**: client gọi 1 phát, server chạy
`cart → giá (batch `GET /v1/skus?ids=`) → lock voucher → reserve stock → tạo order → payment intent`, và **tự bù** khi
fail (release stock/voucher, cancel order).

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `POST /v1/checkout` | đã đăng nhập | **bắt buộc** `Idempotency-Key`; body chỉ `{voucherCode?}` — dòng hàng lấy từ **giỏ**. 201 · 200 replay · **422** giỏ rỗng · **409** SKU không mua được / hết hàng · **503** không tới được service đích · **400** thiếu key |
| `GET /v1/checkout/{orderId}` | chủ đơn / admin | trạng thái saga: `state` · `lockId` · `reservationId` · `paymentIntentId` · `failureCode` |
| `POST /v1/checkout/{orderId}/confirm` | chủ đơn / admin | **verify payment thật** rồi commit stock + voucher ⇒ `CONFIRMED`; gọi lại → **200**; payment chưa `SUCCEEDED` → **409**; đơn 0 đồng bỏ qua bước verify |
| `POST /v1/checkout/{orderId}/cancel` | chủ đơn / admin | release stock + voucher ⇒ `CANCELLED` (body `{reason?}`); gọi lại → **200**; đơn đã `CONFIRMED` → **409** |

```bash
# Buyer: thêm hàng vào giỏ rồi checkout (voucher tuỳ chọn)
curl -s -X PUT localhost:8080/v1/carts/items/<skuId> -H "Authorization: Bearer $BUYER" \
  -H 'Content-Type: application/json' -d '{"qty":2}'
curl -s -X POST localhost:8080/v1/checkout -H "Authorization: Bearer $BUYER" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: ck-1" -d '{"voucherCode":"SALE20"}'
```

State lưu ở `checkout_sagas` + audit `checkout_saga_steps` (6 bước start + các bước finish, http status, ms) trong DB
`order`. **Đơn 0 đồng** (voucher 100%) không tạo payment intent: response `paymentRequired=false`, bước
`PAYMENT_INTENT` = `SKIPPED` ([ADR-0006](docs/adr/0006-checkout-model.md) D6). Timeout mỗi bước 2 s connect / 800 ms
read, URL 5 service qua `SAGA_*_URL`.

**Bước hoàn tất**: sau khi thanh toán (mock: `POST /mock-provider/pay/{id}`) client gọi
`POST /v1/checkout/{orderId}/confirm` → saga verify với payment-service rồi commit stock + voucher. Không thanh toán
trong `SAGA_PAYMENT_WINDOW` (mặc định **15′** = TTL reservation/voucher) ⇒ `CheckoutSagaSweeper` đánh dấu `EXPIRED`;
hàng/voucher về kho theo TTL sweeper của inventory/promotion ([ADR-0006](docs/adr/0006-checkout-model.md) D7).

```bash
curl -s -X POST localhost:8080/v1/checkout/<orderId>/confirm -H "Authorization: Bearer $BUYER"
curl -s -X POST localhost:8080/v1/checkout/<orderId>/cancel  -H "Authorization: Bearer $BUYER" \
  -H 'Content-Type: application/json' -d '{"reason":"doi y"}'
```

### api-gateway — `SL-1.2`

**Cửa vào duy nhất** của hệ thống (port **8080**): route `/v1/**` tới service tương ứng, verify JWT **một chỗ**,
giữ `traceId` xuyên suốt. Gateway **không** có DB (không sở hữu dữ liệu nào).

| Path | Ai xử lý | Cần token? |
|---|---|---|
| `/v1/auth/**`, `/.well-known/**`, `/health`, `/ready` | identity-service (riêng `/health`, `/ready` do gateway) | **không** |
| `/v1/users/**` | identity-service | **có** |
| còn lại | — | có (401 nếu thiếu token; 404 nếu không có route) |

```bash
# Chạy gateway (identity phải healthy trước)
docker compose -f infra/docker-compose.yml --env-file .env up -d --build api-gateway

EMAIL="gw-$(date +%s)@shoplab.local"
curl -s -X POST localhost:8080/v1/auth/register -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"matkhau123\",\"displayName\":\"Gw\"}"
TOKEN=$(curl -s -X POST localhost:8080/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"matkhau123\"}" | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')

curl -s localhost:8080/v1/users/me                        # 401 (thiếu token)
curl -s localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN"   # 200 {userId, role}

# traceId dùng chung 2 service — so log 2 bên
curl -s -o /dev/null -D - localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN" \
  -H 'x-correlation-id: e2e-trace-abc123'
docker logs shoplab-api-gateway    2>&1 | grep e2e-trace-abc123
docker logs shoplab-identity-service 2>&1 | grep e2e-trace-abc123
```

Cách gateway xử lý token: verify chữ ký **RS256** bằng public key lấy từ **JWKS** của identity (cache 5 phút, tự nạp lại
khi gặp `kid` lạ — rotate khoá không cần restart, có chặn dội). Token hợp lệ → gateway gắn `x-user-id`/`x-user-role`
(**ghi đè** giá trị client gửi, kể cả trên route public). Lỗi phân biệt rõ: token sai → **401**, service đích không tới
được → **503** (retry được).

---
### E2E checkout — `SL-2.8`

Bằng chứng end-to-end **luồng tiền Phase 2** bằng **1 lệnh**: `client → gateway → cart → order(checkout saga) →
catalog/promotion/inventory → payment → confirm`, assert cả **số tiền** lẫn **trạng thái cuối**.

```bash
bash tools/e2e-checkout.sh      # cần stack đang chạy (compose up -d)
```

```text
== Kết quả: 27 PASS / 0 FAIL · 14 request · 931 ms
E2E checkout: OK
```

Luồng kiểm: login admin/seller → register buyer → seller tạo category/product/SKU → admin set tồn kho **10** + voucher
FIXED **20 000** → buyer thêm **2** món vào giỏ → `POST /v1/checkout` (**201**, `total = 240 000 − 20 000 = 220 000`,
có `paymentUrl`) → saga `COMPLETED_START` → retry cùng `Idempotency-Key` (**200**, cùng `orderId`) → mock-pay →
`confirm` (**200**, `CONFIRMED`) → `GET /v1/orders/{id}` = **CONFIRMED** → tồn kho `onHand 10 → 8`, `reserved 0`.

> Vì sao script **riêng** (không nhồi vào `e2e-smoke.sh`): smoke là "health check nhanh" Phase 1 (17 check, ~1 s);
> tách ra thì khi đỏ biết ngay hỏng tầng nào.

**Chạy trong CI**: workflow [`e2e.yml`](.github/workflows/e2e.yml) dựng **cả 10 thành phần** bằng
`docker compose up -d --build --wait` rồi chạy 2 script trên. Nó chạy theo **`workflow_dispatch`** (bấm tay) và
**`schedule` hằng tuần** — **không** chạy mỗi PR vì 9 image × `mvn package` từ đầu ≈ 30 phút/run (gói Free 2 000
phút/tháng). `ci.yml` vẫn là hàng rào chạy mỗi PR.

### E2E nhánh fail + compensating — `SL-2.9`

```bash
bash tools/e2e-checkout-fail.sh     # cần stack đang chạy + SHOPLAB_DEBUG_ENDPOINTS=true cho payment
# == Kết quả: 41 PASS / 0 FAIL / 0 SKIP · 37 request · 12118 ms
```

| Kịch bản | Bơm lỗi bằng | Assert state thật |
|---|---|---|
| F1 hết hàng | SKU `onHand = 0` | **409** · `voucher.locked = 0` · `stock.reserved = 0` |
| F2 provider từ chối | `POST /mock-provider/pay/{id}?fail=true` | `confirm` **409** · `cancel` **200** · saga `CANCELLED` · `reserved 0` · `locked 0` |
| F3 payment 5xx | hook `failStatus=500` | checkout **503** · `reserved 0` · `locked 0` · order `CANCELLED` |
| F4 **timeout** | hook `sleepMs=2000` (> `SAGA_READ_TIMEOUT` 800 ms) | checkout **503** sau **921 ms** · bù sạch |
| F5 dependency chết | `docker stop shoplab-payment-service` | checkout **503** · service tự sống lại · bù sạch |
| F6 retry sau fail | cùng `Idempotency-Key` của F3 | **201** (khoá đã được trả, không kẹt `IN_PROGRESS`) |

**Hook bơm lỗi** (`payment-service: debug/FaultInjection*`): chỉ tồn tại khi `shoplab.debug-endpoints=true` (compose local/E2E),
`@Hidden` khỏi OpenAPI, chỉ tác động `/v1/payment-intents`.

```bash
# Gọi THẲNG port payment — `/debug/**` cố ý KHÔNG có route ở gateway (debug là nội bộ).
curl -s -X POST 'localhost:8088/debug/faults?sleepMs=2000'   # làm chậm (test read timeout)
curl -s -X POST 'localhost:8088/debug/faults?failStatus=500' # trả 5xx ngay
curl -s -X POST 'localhost:8088/debug/faults/reset'          # tắt
```

> Script tự `reset` + `docker start` trong `trap EXIT`: để hook bật dở mà script chết thì **mọi** checkout sau đó sẽ timeout oan.



### E2E async — `SL-3.7`

Async không nhìn thấy được bằng một `curl`: API trả `200` **trước khi** việc thật xảy ra. Script này
**poll state cuối** (có timeout) thay vì `sleep` mù, và chứng minh cả 4 luồng event của Phase 3:

```bash
bash tools/e2e-async.sh     # cần stack đang chạy (compose up -d --wait)
```

```text
== Kết quả: 48 PASS / 0 FAIL · 40 request · 27774 ms
E2E async: OK
```

| Luồng | Bằng chứng (nguồn: DB/broker, không tin lời service) |
|---|---|
| `order.confirmed.v1` ⇒ email | row trong `notification_logs` (DB `notification`) |
| `payment.succeeded.v1` ⇒ order tự chốt | mock-pay rồi **KHÔNG** gọi `/confirm` ⇒ `order.status = CONFIRMED` ≤ 5 s |
| `product.updated/archived` ⇒ index | sửa product ⇒ `/v1/search` thấy **giá mới**; xoá ⇒ search mất, row index `ARCHIVED` |
| lỗi ⇒ DLQ ⇒ replay | bật `SHOPLAB_NOTIFICATION_FAIL_MODE=true` ⇒ `q.notification.dlq ≥ 1` ⇒ tắt lỗi ⇒ `tools/dlq.sh move` ⇒ có email + DLQ rỗng |

**Vì sao 2 timeout khác nhau** (`POLL_FAST=5s`, `POLL_DLQ=15s`):

- luồng event thường xong trong ~1 s (relay outbox 1 s + consumer) ⇒ 5 s là rộng;
- riêng DLQ **không thể** ≤ 5 s: consumer retry `200ms,1s,3s` ≈ **4,2 s** rồi mới nack sang DLQ;
- và `/api/queues` của RabbitMQ là **số liệu thống kê, làm mới mỗi ~5 s** ⇒ `messages_ready` có thể
  còn `1` sau khi message đã đi. Poll dài hơn là cách đúng — không phải thêm `sleep`.

**Script tự dọn state** trong `trap EXIT` (kể cả khi chết giữa đường): tắt fail-mode + `dlq.sh purge
q.notification`. Không có trap thì lần chạy sau thấy DLQ đầy message cũ và fail-mode đang bật ⇒ FAIL giả.

> Bật fail-mode = **recreate container** (`SHOPLAB_NOTIFICATION_FAIL_MODE=true docker compose up -d
> notification-service`): property bind lúc khởi động, không có endpoint runtime để đổi. Vì vậy trong
> `e2e.yml` bước này chạy **cuối cùng**, sau 3 script kia.

### E2E smoke — `SL-1.3` → `SL-1.7`

Bằng chứng end-to-end **cả Phase 1** bằng **1 lệnh**: `client → gateway → identity/catalog → DB`.

Luồng kiểm: health 3 service → register → login → `/v1/users/me` (200 khi có token, **401** khi không) →
`traceId` xuyên service → **seller** tạo category + product (201) → **guest** list/đọc sản phẩm vừa tạo
(**200, không token**) → **buyer** tạo product (**403**, case mong đợi lỗi). In `PASS/FAIL` từng bước +
exit code (`0` nếu tất cả pass).

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d    # cả 3 service phải healthy
bash tools/e2e-smoke.sh
```

Output mẫu:

```text
== E2E smoke — client → gateway → identity/catalog → DB
   email=smoke-1793975246-8165@shoplab.local  correlation=smoke-1793975246
  PASS [1] 1a health gateway
  PASS [2] 1b health identity
  PASS [3] 1c health catalog
  PASS [4] 2 register
  PASS [5] 3 login
  PASS [6] 4 protected /v1/users/me (có token)
  PASS [7] 5 /v1/users/me KHÔNG token → 401
  PASS [8] 6 traceId smoke-1793975246 có trong log identity (gateway forward header)
  PASS [9] 7 login seller (seller@shoplab.local)
  PASS [10] 8 seller tạo category → 201
  PASS [11] 9 seller tạo product → 201
    product id = f36347f0-d1b3-4e58-9ecd-682d1aa47505
  PASS [12] 10 guest list product (KHÔNG token) → 200
  PASS [13] 10b product f36347f0-… xuất hiện trong list của guest
  PASS [14] 11 guest xem chi tiết product → 200
  PASS [15] 11b chi tiết product có field skus
  PASS [16] 12 buyer tạo product → 403 (đúng như mong đợi)
  PASS [17] 12b body 403 là ApiError FORBIDDEN (không phải HTML)

== Kết quả: 17 PASS / 0 FAIL · 13 request · 927 ms
E2E smoke: OK
```

- Chỉ dùng `curl` + `python3` (không cần `jq`); **email/category ngẫu nhiên mỗi lần** nên chạy lại vẫn pass
  (đã chạy 6 lần liên tiếp: 6/6 pass, ~430 ms/lần).
- Case trái phép assert **đúng 401/403**, không phải "khác 200" — nếu không, 500 cũng bị tính là pass.
- **Fail ở đâu in gợi ý lệnh kiểm tra ở đó** → làm theo [`docs/runbook-phase-1.md`](docs/runbook-phase-1.md)
  (sơ đồ luồng · bảng port · kiểm tra từng tầng · **5 lỗi thường gặp**: 401 · 403 · 404 · 503 · timeout).



## API spec (OpenAPI) — `SL-1.8`

[`docs/api/openapi.yaml`](docs/api/openapi.yaml) là **contract** của API Phase 1 — sinh từ code nên không
lệch thực tế, và nằm trong git nên **diff được theo phase**.

```bash
bash tools/export-openapi.sh    # gom /v3/api-docs của identity + catalog → docs/api/openapi.yaml + tự validate
```

- `servers[0].url = http://localhost:8080` — **gateway**, cửa vào duy nhất (client không gọi port service).
- Mọi lỗi dùng schema **`ApiError`** cho 400/401/403/404/409 — gắn **tự động cho mọi operation** ở
  `service-commons` (nên endpoint Phase 2 cũng tự có).
- `/v3/api-docs` **chỉ bật ở local**: `APP_ENV != local` ⇒ tự tắt (override có chủ ý bằng
  `SHOPLAB_OPENAPI_ENABLED=true`).
- Hiện tại: **14 path / 18 operation**, 7 operation cần token (endpoint `/debug/*` bị ẩn khỏi spec).
  Chi tiết cách đọc + cập nhật: [`docs/api/README.md`](docs/api/README.md).

## Debug khi có sự cố

Xem [`docs/runbook-phase-1.md`](docs/runbook-phase-1.md) — sơ đồ luồng, kiểm tra từng tầng, 5 lỗi thường
gặp (401 · 403 · 404 · 503 · timeout) và output debug thật.

## Tạo service mới từ template

Ví dụ tạo `order-service`:

```bash
# 1) Copy template (giữ nguyên cấu trúc, chỉ đổi tên)
cp -r services/service-template services/order-service
rm -rf services/order-service/target services/order-service/Dockerfile.bak

# 2) Đổi artifactId + name trong pom.xml của service mới
#    shoplab-service-template  →  shoplab-order-service
#    ShopLab :: Service Template → ShopLab :: Order Service

# 3) Đổi tên class main + package (tuỳ chọn nhưng nên làm cho rõ)
#    com.shoplab.template.ServiceTemplateApplication → com.shoplab.order.OrderServiceApplication
#    (nhớ sửa luôn package trong mọi file java của service)

# 4) Đăng ký module vào parent pom.xml
#    <module>services/order-service</module>

# 5) Đổi tên service trong application.yml (spring.application.name) — tên này vào log + /version
#    spring.application.name: order-service

# 6) Thêm port + biến môi trường vào .env.example và một service trong infra/docker-compose.yml
#    (copy block `service-template`, đổi tên + port)

# 7) Chạy
mvn -q verify
docker compose -f infra/docker-compose.yml --env-file .env up -d --build order-service
curl -s localhost:<port>/health
```

**Đừng quên** (những thứ copy không tự mang theo):

- Thêm biến bắt buộc mới vào `com.shoplab.<svc>.config.RequiredEnv` — validate chạy ở `main()`.
- **`Dockerfile`: 4 chỗ phải đổi tên** — `COPY services/<svc>/pom.xml services/<svc>/`,
  `mvn -pl services/<svc> -am dependency:go-offline`, `mvn -pl services/<svc> -am verify`, và
  `COPY --from=build /workspace/services/<svc>/target/shoplab-<svc>-*.jar app.jar`.
  Build context vẫn phải là **gốc repo** (cần parent pom + `packages/service-commons`).
- ⚠️ **Mọi `Dockerfile` phải copy pom của TẤT CẢ module** trong `<modules>` của parent pom — thiếu một cái là
  Maven chết ngay với `Child module ... does not exist` (gặp thật ở SL-1.1 khi thêm module thứ 3).
  Thêm service mới ⇒ thêm 1 dòng `COPY services/<svc>/pom.xml ...` vào **từng** Dockerfile.
- **Test không cần sửa** — template cố ý không hard-code tên service (đọc từ `spring.application.name`).
  Nếu bạn thêm test có assert tên service thì nhớ đọc từ config, đừng viết cứng.
- Service **có DB** → dùng **Testcontainers** cho IT và tách đúng chỗ:
  `*IT.java` (failsafe) cho test cần DB; `*Test.java` (surefire) phải chạy được **không cần Docker**.
  Dockerfile phải thêm `-DskipITs` vì trong `docker build` **không có Docker socket**.
  Thêm `<classesDirectory>${project.build.outputDirectory}</classesDirectory>` cho failsafe — nếu không,
  `spring-boot:repackage` (chạy trước failsafe) làm failsafe không thấy class của chính module.
- Nếu service cần check dependency sâu hơn TCP (query DB thật) → implement `DependencyHealthCheck`
  trong service, đừng nhét JDBC driver vào shared lib.
- Service cần một **database riêng** → thêm biến `POSTGRES_DB_<SVC>` vào `.env.example` và
  `infra/docker-compose.yml`, rồi thêm vào `infra/init/01-create-databases.sh` (nhớ `docker compose down -v`
  để init script chạy lại — nó chỉ chạy khi volume rỗng).
- Service **đọc file bí mật** (khoá, cert) → đừng nhét vào image: mount read-only từ host và chạy container
  bằng **cùng uid/gid với user trên host** (`user: "${SHOPLAB_UID}:${SHOPLAB_GID}"`) — nếu không, file mode 600
  sẽ cho `AccessDeniedException` vì user trong image có uid khác.

**Nguyên tắc cho shared lib** (`packages/service-commons`): chỉ để **hạ tầng** (log, config, error,
health, correlation id). **Tuyệt đối không** để domain logic hay entity/database dùng chung — làm vậy
là tự biến microservices thành distributed monolith (AGENTS.md §6).

### Service

Phase 1 sẽ tạo `api-gateway` / `identity-service` / `catalog-service` bằng cách copy `service-template`.

