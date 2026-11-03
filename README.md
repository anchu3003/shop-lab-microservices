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

**Trạng thái hiện tại**: Phase 0 — Foundation ✅ **Done (8/8)** — chi tiết ở [`Backlog-phase-0.md`](Backlog-phase-0.md).
Phase 1 (Walking skeleton) chưa bắt đầu. Chưa có business logic.

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
│   └── service-template/         # service mẫu (SL-0.4) — copy ra để tạo service mới
├── packages/                     # shared library hạ tầng (không chứa business logic của service)
│   └── service-commons/          # log JSON · error response · correlation id · health check · env validator
├── infra/                        # docker compose, init SQL, config hạ tầng local — SL-0.3
│   ├── docker-compose.yml        # Postgres · Redis · RabbitMQ · Adminer · service-template
│   └── init/                     # script tạo database lần đầu (chạy 1 lần khi volume rỗng)
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
mvn -q verify                 # spotless:check + checkstyle:check + test + jacoco:check
```

Convention: **Spotless + google-java-format** (format) · **Checkstyle** (lint) · **JUnit 5/6 + AssertJ** (test)
· **JaCoCo** (coverage gate ≥ 80% LINE cho package `config` + `health`).
Line-ending do `.gitattributes` ép **LF**; editor theo `.editorconfig`.


---

## CI (GitHub Actions) — `SL-0.5`

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy khi **push lên nhánh bất kỳ** và khi **mở PR vào `main`**.
Không có CI thì "code chạy máy tôi" sẽ âm thầm phá build khi số service tăng lên ở Phase 1+.

| Job | Quan hệ | Lệnh | Chặn cái gì |
|---|---|---|---|
| `lint` | ∥ song song | `mvn -B -ntp checkstyle:check` rồi `mvn -B -ntp spotless:check` | vi phạm Checkstyle (log hiện `file:line`) + code chưa format |
| `test` | ∥ song song | `mvn -B -ntp verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` | test đỏ + coverage < 80% LINE (JaCoCo gate) |
| `build-image` | sau `lint` + `test` | `docker build -f services/service-template/Dockerfile …` | Dockerfile / image build hỏng |

Vì sao tách 3 job: CI UI chỉ rõ job nào đỏ (đọc log nhanh), `lint` và `test` độc lập nên chạy song song, còn
`build-image` là job nặng nhất nên chỉ chạy khi 2 job kia đã xanh (repo private tính phút Actions theo gói).

**Push lên nhánh đang có PR sẽ thấy 2 run** (một cho event `push`, một cho `pull_request`) — đây là chủ ý:
gom chung group sẽ khiến run PR huỷ run push và để lại check `cancelled` trên commit. `cancel-in-progress`
chỉ huỷ run cũ **cùng ref** (push liên tiếp cùng nhánh / PR cập nhật liên tục).

Tái hiện đúng lệnh CI ở máy:

```bash
mvn -B -ntp checkstyle:check && mvn -B -ntp spotless:check             # job lint
mvn -B -ntp verify -Dcheckstyle.skip=true -Dspotless.check.skip=true  # job test
docker build -f services/service-template/Dockerfile \
  --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) -t shoplab-service-template:ci .   # job build-image
```

Vài điểm cần biết:

- **Badge ở đầu README** lấy trạng thái run **mới nhất trên `main`**. Repo đang **private** → badge chỉ render
  khi trình duyệt **đã đăng nhập GitHub**; khách chưa login sẽ thấy ảnh vỡ (không phải CI hỏng).
- CI **chưa** push image lên registry và **chưa** deploy — để Phase 7. Job `build-image` chỉ build rồi liệt kê image.
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
| `8090` | `service-template` | service mẫu (SL-0.4) |
| `8080` / `8081` / `8082` | — | dành cho `api-gateway` / `identity-service` / `catalog-service` (Phase 1) |

> ⚠️ `up -d` **build luôn image của `service-template`** (mất ~1–2 phút lần đầu). Muốn chỉ dựng hạ tầng:
> `docker compose -f infra/docker-compose.yml --env-file .env up -d postgres redis rabbitmq adminer`

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
- **Test không cần sửa** — template cố ý không hard-code tên service (đọc từ `spring.application.name`).
  Nếu bạn thêm test có assert tên service thì nhớ đọc từ config, đừng viết cứng.
- Nếu service cần check dependency sâu hơn TCP (query DB thật) → implement `DependencyHealthCheck`
  trong service, đừng nhét JDBC driver vào shared lib.
- Service cần một **database riêng** → thêm biến `POSTGRES_DB_<SVC>` vào `.env.example` và
  `infra/docker-compose.yml`, rồi thêm vào `infra/init/01-create-databases.sh` (nhớ `docker compose down -v`
  để init script chạy lại — nó chỉ chạy khi volume rỗng).

**Nguyên tắc cho shared lib** (`packages/service-commons`): chỉ để **hạ tầng** (log, config, error,
health, correlation id). **Tuyệt đối không** để domain logic hay entity/database dùng chung — làm vậy
là tự biến microservices thành distributed monolith (AGENTS.md §6).

### Service

Phase 1 sẽ tạo `api-gateway` / `identity-service` / `catalog-service` bằng cách copy `service-template`.

