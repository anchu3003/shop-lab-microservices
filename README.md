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
Phase 1 (Walking skeleton) 🔵 **In Progress**: `api-gateway` (8080) + `identity-service` (8081) chạy được và nối với
nhau — register/login qua gateway, verify JWT bằng JWKS. Xem [Backlog.md](Backlog.md).

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

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy khi **push lên `main`** và khi **mở/cập nhật PR vào `main`**
(PR ở dạng **draft bị bỏ qua** cho tới khi bấm *Ready for review*). Không có CI thì "code chạy máy tôi" sẽ âm thầm phá
build khi số service tăng lên ở Phase 1+.

| Job | Quan hệ | Lệnh | Chặn cái gì |
|---|---|---|---|
| `lint` | ∥ song song | `mvn -B -ntp checkstyle:check` rồi `mvn -B -ntp spotless:check` | vi phạm Checkstyle (log hiện `file:line`) + code chưa format |
| `test` | ∥ song song | `mvn -B -ntp verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` | test đỏ + coverage < 80% LINE (JaCoCo gate) |
| `build-image` | sau `lint` + `test` | **matrix**, 1 job con/service: `docker build -f services/<service>/Dockerfile --build-arg SKIP_TESTS=true …` | Dockerfile / image build hỏng **của từng service** |

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
| `8080` / `8081` / `8082` | — | dành cho `api-gateway` / `identity-service` / `catalog-service` (Phase 1) |

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

---


### catalog-service — `SL-1.5`

Service **read-heavy đầu tiên**: guest xem được sản phẩm, chỉ `seller`/`admin` ghi. DB riêng `catalog`
(Flyway `V1__create_catalog.sql`).

**API**

| Method + path | Quyền | Ghi chú |
|---|---|---|
| `GET /v1/categories` | **public** | danh sách category, sort theo tên |
| `POST /v1/categories` | `seller`/`admin` | 201 + `Location`; trùng slug → **409** |
| `GET /v1/products/{id}` | **public** | 200 · không tồn tại → **404** |
| `POST /v1/products` | `seller`/`admin` | 201; `categoryId` không tồn tại → **404** |
| `PUT /v1/products/{id}` | `seller`/`admin` | thay toàn bộ field sửa được; `version` +1 |
| `DELETE /v1/products/{id}` | `seller`/`admin` | **204** |

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

Xem `docs/` sau `SL-1.7` để có runbook debug từng tầng.

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
### E2E smoke — `SL-1.3`

Bằng chứng end-to-end **client → gateway → identity → DB** bằng **1 lệnh**: health → register → login → gọi endpoint
protected qua gateway → assert 401 khi thiếu token → đối chiếu `traceId` trong log identity. In `PASS/FAIL` từng bước +
exit code (`0` nếu tất cả pass).

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d    # gateway + identity phải healthy
bash tools/e2e-smoke.sh
```

Output mẫu:

```text
== E2E smoke — client → gateway → identity → DB
   email=smoke-1752340000-1234@shoplab.local  correlation=smoke-1752340000
  PASS [1] 1a health gateway
  PASS [2] 1b health identity
  PASS [3] 2 register
  PASS [4] 3 login
  PASS [5] 4 protected /v1/users/me (có token)
  PASS [6] 5 /v1/users/me KHÔNG token → 401
  PASS [7] 6 traceId smoke-1752340000 có trong log identity (gateway forward header)

== Kết quả: 7 PASS / 0 FAIL · 6 request · 1234 ms
E2E smoke: OK
```

- Script chỉ dùng `curl` + `python3` (không cần `jq`); **email ngẫu nhiên mỗi lần** nên chạy lại vẫn pass.
- Bước trái phép assert **đúng 401** (không phải "khác 200") — tránh bẫy 500 cũng bị tính là pass.
- Bước 6 chứng minh `traceId` xuyên service: script gửi `x-correlation-id`, identity log đúng id đó (gateway forward header).



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

