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
| **JDK 21** | Temurin/OpenJDK 21 | ❌ **chưa cài** | `sudo apt update && sudo apt install -y openjdk-21-jdk` |
| **Maven** | 3.9.x | ❌ **chưa cài** trong WSL (đang lẫn bản Windows ở `/mnt/d/...`) | `sudo apt install -y maven` |
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

Phase 0 chưa có service nào. Hạ tầng local (`docker compose up -d`) sẽ có ở **SL-0.3**;
service đầu tiên (`api-gateway`) ở **Phase 1**.
