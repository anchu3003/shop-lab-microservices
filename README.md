# ShopLab — microservices learning lab

> Sàn thương mại điện tử multi-seller dựng lại từ đầu theo **microservices**, mục đích **học
> architecture & system design** ở quy mô high traffic. **Không phải sản phẩm thật.**

| | |
|---|---|
| Kiến trúc, tech stack, roadmap | [`architecture.md`](architecture.md) |
| Ticket & tiến độ theo phase | [`Backlog.md`](Backlog.md) |
| Cách làm việc (working agreement) | [`AGENTS.md`](AGENTS.md) |
| Quyết định kiến trúc (ADR) | [`docs/adr/`](docs/adr/) |
| Git workflow | [`docs/git-flow.md`](docs/git-flow.md) |

**Trạng thái hiện tại**: Phase 0 — Foundation (xem `Backlog.md`). Chưa có business logic.

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
| **Lefthook** | ≥ 2.0 | ❌ chưa cài | xem mục *Git hooks* bên dưới |
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
├── services/            # mỗi service 1 thư mục kebab-case (vd: order-service) — Phase 1+
├── packages/            # shared library hạ tầng (không chứa business logic của service)
├── infra/               # docker compose, init SQL, config hạ tầng local — SL-0.3
│   ├── docker-compose.yml   # Postgres · Redis · RabbitMQ · Adminer
│   └── init/                # script tạo database lần đầu (chạy 1 lần khi volume rỗng)
├── tools/               # script phụ trợ (git hooks, reset infra...)
├── config/checkstyle/   # Checkstyle config cho Java
├── docs/adr/            # Architecture Decision Records
├── .cline/ .clinerules/ # AI tooling (skill / workflow / rule) — xem AGENTS.md §10
└── pom.xml              # Maven parent POM (convention lint/format)
```

---

## Lint & format

```bash
# Java — format (sửa file) rồi check (CI dùng check)
mvn -q spotless:apply
mvn -q verify                 # chạy cả spotless:check + checkstyle:check
```

Convention: **Spotless + google-java-format** (format) · **Checkstyle** (lint).
Line-ending do `.gitattributes` ép **LF**; editor theo `.editorconfig`.

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
| `8080` / `8081` / `8082` | — | dành cho `api-gateway` / `identity-service` / `catalog-service` (Phase 1) |

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
| **Tổng** | **~150 MiB** (dao động 145–165 MiB) |

→ Dùng làm baseline cho Phase 7 (K8s resource request). Lưu ý: **chưa** gồm OpenSearch/MinIO
(chưa dựng ở SL-0.3) — hai cái này nặng hơn nhiều, cần chỉnh `%UserProfile%\.wslconfig`.

**Còn thiếu (cố ý, xem `Backlog.md` → SL-0.3 out of scope)**: MinIO (object storage) và OpenSearch 2.x
(search) — dựng khi phase cần thật; observability stack (Phase 6); K8s (Phase 7).

### Service

Phase 0 chưa có service nào. Service đầu tiên (`api-gateway`) ở **Phase 1**.
