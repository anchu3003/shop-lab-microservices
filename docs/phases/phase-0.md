# Phase 0 — Foundation (snapshot `phase-0`)

| | |
|---|---|
| **Tag** | `phase-0` — gắn trên merge commit đóng Phase 0 |
| **Thời điểm** | 2026-11-03 |
| **Ticket** | `SL-0.1` … `SL-0.8` (**8/8 Done**) — chi tiết: [`Backlog-phase-0.md`](../../Backlog-phase-0.md) |
| **ADR** | [0001](../adr/0001-tech-stack.md) (tech stack) · [0002](../adr/0002-dev-environment.md) (WSL2) · [0003](../adr/0003-spring-boot-version-and-module-layout.md) (Spring Boot 4.1.2 + module layout) · [0004](../adr/0004-service-commons-shared-library.md) (`service-commons`) · [0005](../adr/0005-phase-snapshot-and-release.md) (snapshot) |
| **Trạng thái code** | **Không có business logic** — chỉ hạ tầng + 1 service mẫu |

## Có gì trong snapshot này

| Thành phần | Nội dung |
|---|---|
| **Hạ tầng local** | `infra/docker-compose.yml`: Postgres 16 · Redis 7 · RabbitMQ 4 (+ management UI) · Adminer · `service-template`. Init script tạo 7 database (database per service) |
| **Service** | `services/service-template` — `/health` (liveness, **không** check dependency) · `/ready` (readiness, check dependency → 503) · `/version` (git commit) · graceful shutdown · endpoint dev-only `/debug/*` |
| **Shared lib** | `packages/service-commons` — log JSON (`traceId`/`service`/`level`/`msg`) · error response thống nhất · correlation id · health check helper · env validator fail-fast. Auto-configuration, mọi bean `@ConditionalOnMissingBean` |
| **Build / lint** | Maven multi-module · Spotless (google-java-format) · Checkstyle **12.3.1** · JaCoCo **0.8.16** gate ≥ 80% LINE cho `config` + `health` · Surefire **3.6.1** |
| **CI** | `.github/workflows/ci.yml` — job `lint` ∥ `test` → `build-image` (GitHub Actions, cache Maven) |
| **Git hooks** | Lefthook **2.2.2**: Conventional Commits · secret-scan · chặn push trực tiếp `main` |
| **AI tooling** | `AGENTS.md` · `.clinerules/` (rules + 4 workflows) · `.cline/skills/` (3 skills) |

## Cách chạy ở snapshot này

```bash
git checkout phase-0
cp .env.example .env
docker compose -f infra/docker-compose.yml --env-file .env up -d      # 5/5 container healthy
curl -s localhost:8090/health ; curl -s localhost:8090/ready ; curl -s localhost:8090/version
mvn -B verify                                                         # 72 test + lint + coverage gate
```

> ⚠️ Bẫy `--env-file`: `docker compose` nạp `.env` theo **thư mục chứa compose file** (`infra/`), nên **bắt buộc**
> truyền `--env-file .env` — bỏ đi sẽ lỗi `required variable POSTGRES_PASSWORD is missing a value`.

## Số liệu đo được (baseline của phase)

| Chỉ số | Giá trị | Đo bằng |
|---|---|---|
| RAM hạ tầng idle | **~150 MiB** (Postgres ~25 · Redis ~4.5 · RabbitMQ ~106 · Adminer ~13) | `docker stats --no-stream` |
| RAM 1 service Java idle | ~150 MiB | `docker stats --no-stream` |
| Test | **72** (37 `service-commons` + 35 `service-template`), 0 failure | `mvn -B verify` |
| CI — cold cache | **2m35s** (`lint` 33s ∥ `test` 46s → `build-image`) | run `41657425300` |
| CI — warm cache (trên `main`) | ~2m13s | run `41661973256` |
| Image `service-template` | 252 MB | `docker image ls` |
| Port dùng | 5432 · 6379 · 5672 · 15672 · 8085 · 8090 (8080–8082 để dành Phase 1) | `README.md` |

## Chưa có (cố ý — sẽ đến ở phase sau)

- **Không** business logic, **không** service thật (Phase 1 mới có `api-gateway` / `identity-service` / `catalog-service`)
- **Không** MinIO (object storage), **không** OpenSearch (search)
- **Không** Actuator / metrics / tracing (Phase 6) · **không** K8s (Phase 7) · **không** Kafka (Phase 8)
- **Không** integration test với Testcontainers (Phase 1–2)
- CI **chưa** push image lên registry, **chưa** CD (Phase 7)
- Health check mức **TCP** (chưa query DB thật) — Phase 1 thay bằng check sâu

## Bài học đáng nhớ của phase

(đầy đủ ở [`Backlog-phase-0.md`](../../Backlog-phase-0.md) — mỗi ticket có learning note riêng)

- `BUILD SUCCESS` **không** có nghĩa là đã test: surefire mặc định 2.17 → `Tests run: 0` mà vẫn xanh → phải pin `3.6.1`.
- AC dạng *"đã thử làm theo hướng dẫn"* phải **thực thi thật** — copy template theo README mới lộ bug test hard-code tên service.
- CI phải **test âm** mới tin: run xanh chỉ chứng minh pipeline *chạy*, không chứng minh nó *chặn*.
- **`concurrency`** gom push + PR chung group → run PR huỷ run push → commit có check `cancelled` (tín hiệu sai trên PR).
- Repo **private**: badge/Release chỉ mình thấy; Actions tính theo **phút** (Free 2.000/tháng).
