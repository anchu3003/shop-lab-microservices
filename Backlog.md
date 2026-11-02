# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | |
|---|---|
| **Ticket đang làm** | `SL-0.2` |
| **Ticket tiếp theo** | `SL-0.3` |
| **Số ticket chi tiết hiện có** | 8 (Phase 0) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | 🔵 In Progress | 4 / 8 |
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

### SL-0.6 — Bootstrap AI tooling (rules / skills / workflows)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | — (làm **trước** `SL-0.1`) |
| **Ước lượng** | S |

**Bối cảnh**
Repo đã có `AGENTS.md` (được Cline tự nạp như một Rule) + `architecture.md` + `Backlog.md`, nhưng
**chưa có AI tooling**: không skill, không workflow, không rule phụ. Hệ quả: mỗi lượt phải nhắc lại
cách làm, dễ trôi khỏi convention, và các quy trình lặp (viết ADR, chạy ticket, verify, đóng ticket)
phải làm bằng tay. Chuẩn hoá trước khi bắt đầu `SL-0.1` để mọi ticket sau chạy nhất quán và
tiết kiệm token.

**Mục tiêu**
Có bộ AI tooling tối thiểu, token-efficient: **rule** nạp context, **skill** chứa playbook tái dùng,
**workflow** chạy quy trình nhiều bước bằng slash command — đủ để vòng
Read → Plan → Build → Verify → Document (AGENTS.md §4) chạy trơn.

**In scope**
- `.clinerules/00-context.md` — rule nhỏ: nhắc đọc `architecture.md` + `Backlog.md` khi bắt đầu task
  ("Memory Bank lite"), liệt kê skill/workflow đang có
- 3 skills trong `.cline/skills/`:
  - `shoplab-adr` — cách viết ADR (template 5 mục + quality bar), dùng cho `docs/adr/`
  - `shoplab-ticket` — cấu trúc ticket BA-style + checklist DoR/DoD
  - `shoplab-verify` — recipe verify (docker compose health, `curl /health`, `mvn`/`go test`) + quy tắc
    "dán output thật, không nói suông"
- 3 workflows trong `.clinerules/workflows/`:
  - `run-ticket.md` — thực thi 1 ticket theo vòng §4
  - `new-ticket.md` — breakdown epic → ticket chi tiết
  - `close-ticket.md` — update `Backlog.md` (status + verify + learning note)
- `AGENTS.md`: thêm mục "AI assets" (liệt kê + link) + ghi rõ hooks/plugins không áp dụng VS Code
- `Backlog.md`: thêm ticket này + note thứ tự làm trước `SL-0.1`

**Out of scope**
- Chưa tạo skill/workflow cho phase sau (`compose`, `service-scaffold`, `loadtest`, `otel`, `k8s`) —
  tạo khi tới lượt
- Không dùng Cline Hooks/Plugins (không hỗ trợ VS Code extension)
- Không dựng `memory-bank/` chuẩn (đã có `architecture.md` + `Backlog.md`)
- Không cấu hình git hooks (thuộc `SL-0.2`)
- Không viết code service

**Acceptance Criteria**
- [x] `.clinerules/00-context.md` tồn tại, trỏ `architecture.md` + `Backlog.md`, liệt kê skill/workflow hiện có
- [x] 3 skills đúng format: `SKILL.md` có frontmatter `name` khớp tên thư mục + `description` cụ thể (≤ 1024 ký tự)
- [x] 3 workflow gọi được bằng `/run-ticket.md`, `/new-ticket.md`, `/close-ticket.md` — *user xác nhận khi yêu cầu đóng ticket*
- [x] `AGENTS.md` có mục "AI assets", nêu rõ hooks/plugins không áp dụng VS Code
- [x] Verify: gõ `/run-ticket.md` → Cline nạp đúng nội dung workflow — *user xác nhận*
- [x] Verify: yêu cầu "viết ADR cho tech stack" → skill `shoplab-adr` trigger — *đã trigger trong session này*
- [x] Không có secret/API key trong `.cline/` (kiểm tra bằng grep)
- [x] `Backlog.md` cập nhật trạng thái ticket

**Ghi chú kỹ thuật**
- Cline trên VS Code hỗ trợ: Rule (`.clinerules/`, `.cline/rules/`, `AGENTS.md`),
  Skill (`.cline/skills/<name>/SKILL.md`), Workflow (`.clinerules/workflows/*.md`, gọi bằng `/<filename>.md`)
- Skill dùng **progressive loading**: metadata ~100 token nạp sẵn, body < 5k token khi trigger
  → dùng cho **kiến thức tái dùng**; workflow chỉ tốn token khi invoke → dùng cho **quy trình nhiều bước**
- `description` của skill phải cụ thể, nếu chung chung sẽ không auto-trigger
- Cline Hooks/Plugins chỉ chạy trên SDK/CLI/Kanban, **không** trên VS Code extension → thay bằng
  git hooks (`SL-0.2`)
- `.cline/` commit vào repo nhưng **không** chứa secret
- Trade-off: tách nhiều file nhỏ giúp token-efficient nhưng tăng số file phải bảo trì; giữ
  `AGENTS.md` làm single source of truth

**Kết quả verify**
- Đã tạo 7 file:
  - `.clinerules/00-context.md`
  - `.clinerules/workflows/{run-ticket,new-ticket,close-ticket}.md`
  - `.cline/skills/{shoplab-adr,shoplab-ticket,shoplab-verify}/SKILL.md`
- Frontmatter skill khớp tên thư mục:
  `dir=shoplab-adr name=shoplab-adr match=True` / `...ticket match=True` / `...verify match=True`
- Secret scan `.cline/` + `.clinerules/` → `no secret-like strings found`
- `git status --short` → `M AGENTS.md`, `M Backlog.md`, `?? .cline/`, `?? .clinerules/`
- 2026-11-02: user xác nhận 2 AC còn lại (slash command + skill trigger) và yêu cầu đóng ticket → `Done`.
  Skill `shoplab-adr` đã trigger thật trong session (yêu cầu "viết ADR cho tech stack").

**Learning note**
- Cline **Hooks/Plugins không hỗ trợ VS Code extension** → không dùng được cho auto-format/lint; thay
  bằng git hooks (**Lefthook**) ở `SL-0.2`.
- Skill dùng progressive loading nên đặt kiến thức tái dùng vào skill (token rẻ), còn quy trình nhiều
  bước để ở workflow (chỉ tốn token khi gọi).
- `description` của skill quyết định auto-trigger — phải cụ thể, không viết chung chung.

### SL-0.7 — Chốt môi trường dev (WSL2) và viết ADR-0002

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.1 |
| **Ước lượng** | S |

**Bối cảnh**
Stack đã chốt (ADR-0001) là **container-heavy** (Java + Go, nhiều container, Phase 7 có `kind`/K8s). Máy dev
chạy Windows; bind mount source code từ Windows fs (`/mnt/d/...`) vào container **rất chậm**, inotify /
permission / CRLF không đáng tin → ảnh hưởng trực tiếp dev loop. Cần chốt môi trường dev **trước khi**
`SL-0.2` cài tooling.

**Mục tiêu**
Chốt môi trường dev + ghi lý do dưới dạng ADR-0002; cập nhật `architecture.md` §10; điều chỉnh `SL-0.2`.

**In scope**
- Tạo `docs/adr/0002-dev-environment.md` (Context / Options / Decision / Consequences / Revisit)
- Cập nhật `architecture.md` §10 (môi trường + yêu cầu máy dev)
- Điều chỉnh `SL-0.2` cho khớp (tooling Java/Go, Lefthook, WSL2)

**Out of scope**
- Chưa cài đặt WSL2/tooling thật (thuộc `SL-0.2`)
- Chưa đổi code/service

**Acceptance Criteria**
- [x] `docs/adr/0002-dev-environment.md` tồn tại, đủ 5 mục, nêu rõ lý do loại Windows-native/hybrid
- [x] `architecture.md` §10 nêu WSL2 + link ADR-0002; §10.1/§10.2 cập nhật
- [x] `SL-0.2` điều chỉnh: bỏ tooling Node (ESLint/Prettier/Husky) → Spotless/gofmt + Lefthook; thêm WSL2
- [x] Mục Revisit có điều kiện cụ thể

**Ghi chú kỹ thuật**
- Docker Desktop trên Windows **đã** dùng WSL2 backend → chỉ cần đưa source code sang Linux fs
- Điều kiện sống còn: repo **trong** Linux fs (`~/`), không ở `/mnt/d/...`; WSL ≥ 2.1.5
- Tham chiếu: Docker Desktop "WSL 2 best practices"; VS Code "Improve disk performance"

**Kết quả verify**
- `docs/adr/0002-dev-environment.md` tồn tại, đủ 5 mục: `# ADR-0002` → `## Context` → `## Options` →
  `## Decision` → `## Consequences` → `## Revisit`; nêu rõ lý do loại Windows-native và hybrid
- `architecture.md` §10: thêm dòng "Máy dev chạy WSL2" + link ADR-0002; §10.1 đổi "trong WSL2";
  §10.2 thêm hàng WSL2 + ghi rõ "cài trong WSL2"
- `SL-0.2` đã bỏ tham chiếu Node: `npm test`→`mvn -q verify`; `ESLint+Prettier`→`Spotless+Checkstyle`/
  `gofmt+golangci-lint`; `Husky`→`Lefthook`; thêm bước chuyển dev env sang WSL2
- Grep `Husky|ESLint|npm|pnpm|commitlint` toàn repo: chỉ còn trong mục **Options/lịch sử**
  (ADR-0001 Options + SL-0.1 verify/learning) — không còn trong hướng dẫn thực thi
- Cũng đã sửa `.clinerules/00-context.md` + skill `shoplab-verify` (bỏ `npm test` → `mvn`/`go test`)

**Learning note**
- Docker Desktop trên Windows **vốn đã** chạy trên WSL2 backend → "chuyển sang WSL2" thực chất là **đưa
  source code sang Linux filesystem**, không phải đổi engine.
- Điểm chậm nhất là bind mount qua `/mnt/c` — để repo ở `~` là điều kiện sống còn, không phải "nice to have".
- Đổi ngôn ngữ (ADR-0001) kéo theo nhiều doc/skill phải sửa theo (tooling Node → Java/Go) — ADR ảnh
  hưởng rộng hơn tưởng.

### SL-0.8 — Git workflow: rule commit, workflow branch/PR, git flow doc

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.1 |
| **Ước lượng** | S |

**Bối cảnh**
`AGENTS.md` §6 mới chỉ nêu tên convention (Conventional Commits, `feat/<ticket-id>-slug`) nhưng chưa có
cơ chế enforce và chưa có quy trình tách nhánh/PR. Repo hiện commit trực tiếp lên nhánh chính, chưa có
remote, chưa có `gh` CLI. Cần chuẩn hoá git workflow trước khi code service để lịch sử sạch.

**Mục tiêu**
Có rule + workflow + tài liệu cho git flow: commit sau mỗi ticket, tách nhánh theo ticket, tạo PR vào `main`.

**In scope**
- Rule `.clinerules/10-git-workflow.md` (commit sau mỗi ticket, không commit thẳng `main`, không force-push `main`)
- Workflow `.clinerules/workflows/git-branch-pr.md` — tách nhánh + commit + push + tạo PR
- Tài liệu `docs/git-flow.md` — mô hình GitHub Flow, branch naming, commit, PR, lệnh mẫu, tiền đề
- Cập nhật `AGENTS.md` §6 + §10 và `.clinerules/00-context.md` để trỏ tới artifact mới

**Out of scope**
- Chưa set remote GitHub / cài `gh` CLI (việc của người dùng; tài liệu sẽ hướng dẫn)
- Chưa cấu hình branch protection trên GitHub
- Chưa làm git hooks (thuộc SL-0.2)

**Acceptance Criteria**
- [x] `.clinerules/10-git-workflow.md` tồn tại, có quy tắc "commit sau khi hoàn thành mỗi ticket"
- [x] `.clinerules/workflows/git-branch-pr.md` gọi được bằng `/git-branch-pr.md`
- [x] `docs/git-flow.md` mô tả rõ mô hình + lệnh mẫu + tiền đề (remote, `gh`)
- [x] `AGENTS.md` §6/§10 và `.clinerules/00-context.md` trỏ tới artifact mới
- [x] Verify: gõ `/git-branch-pr.md` → Cline nạp đúng nội dung workflow (đã xác nhận)

**Ghi chú kỹ thuật**
- Mô hình: **GitHub Flow** (`main` + nhánh ngắn hạn + PR), **KHÔNG** dùng full GitFlow (develop/release/hotfix) — overkill cho lab 1 người
- Base branch: `main`. Repo hiện ở `master` → rename bằng `git branch -m master main`
- PR cần remote + `gh` CLI; workflow có nhánh dự phòng (push + tạo PR thủ công) khi thiếu
- Trade-off: PR vào `main` thêm 1 bước so với commit thẳng, nhưng giữ `main` luôn chạy được + có chỗ review

**Kết quả verify**
- `.clinerules/10-git-workflow.md` tồn tại — có mục "Commit sau khi hoàn thành mỗi ticket", mô hình GitHub Flow,
  quy tắc branch `<type>/<ticket-id>-<slug>`, an toàn (không force-push/commit thẳng `main`)
- `.clinerules/workflows/git-branch-pr.md` tồn tại — có `<task_objective>` + `<detailed_sequence_of_steps>` (9 bước)
- `docs/git-flow.md` tồn tại — bảng so sánh GitHub Flow vs GitFlow vs trunk-based + lệnh mẫu + tiền đề
- `AGENTS.md` §6 thêm bullet "Git workflow" + link; §10 thêm 2 hàng (rule + workflow)
- `.clinerules/00-context.md` thêm 2 hàng vào bảng AI assets
- 2026-11-02: user gọi `/git-branch-pr.md` → Cline nạp đúng nội dung workflow (đã xác nhận) → `Done`

**Learning note**
- Repo đang ở `master`, **chưa** có remote, **chưa** có `gh` → "PR vào `main`" cần 1 lần setup
  (rename + remote + `gh`); workflow phải có nhánh dự phòng khi thiếu `gh`.
- Chọn **GitHub Flow** thay vì GitFlow vì lab 1 người, không có release train → GitFlow chỉ thêm ma sát.
- **Diff nhiễu CRLF** (13 file, 1253/1253 dòng) khi commit `a881a01`: repo **thiếu `.gitattributes`** và
  `core.autocrlf` đang **unset** → editor trên WSL ghi ra CRLF. Cách kiểm chứng nhanh "chỉ đổi line-ending":
  `git diff --ignore-cr-at-eol --stat` trả **rỗng**. Đã xử lý bằng `git restore .` + `.gitattributes`
  (`* text=auto eol=lf`). Lưu ý: `.gitattributes` **vốn đã nằm trong scope `SL-0.2`** → phần này là việc của
  `SL-0.2` đi sớm, `SL-0.2` chỉ cần verify lại.
- **Branch protection cho `main` không khả dụng**: repo **private** + gói **GitHub Free** → API trả
  `403 Upgrade to GitHub Pro or make this repository public`. Cả branch protection **và** rulesets đều bị chặn.
  Muốn enforce "không push thẳng `main`" trên gói Free → phải dùng **git hook local** (`Lefthook` `pre-push`,
  thuộc `SL-0.2`), hoặc chuyển repo sang **public** / nâng **GitHub Pro**.

---

### SL-0.1 — Chốt tech stack và viết ADR-0001

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
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
- [x] `docs/adr/0001-tech-stack.md` tồn tại, có đủ 5 mục, có bảng trade-off cho từng hạng mục
- [x] ADR nêu rõ **lý do loại** phương án không chọn, không chỉ nêu lý do chọn
- [x] ADR có mục "Revisit khi nào" với điều kiện cụ thể (không ghi chung chung "khi cần")
- [x] `architecture.md` §11 khớp 100% với ADR, không còn từ "đề xuất" ở phần quyết định
- [x] `architecture.md` §10.2 cập nhật lại danh sách công cụ cần cài theo stack đã chốt

**Ghi chú kỹ thuật**
- Broker: cân nhắc RabbitMQ (dễ học, routing linh hoạt, có DLQ sẵn) vs Kafka (throughput cao, replay, hợp CDC)
- Gateway: tự build (học được cơ chế) vs Kong/APISIX/Envoy (production-like, ít học được)
- Đây là **ADR đầu tiên** → đồng thời chốt luôn format ADR cho các ADR sau

**Kết quả verify**
- `docs/adr/0001-tech-stack.md` tồn tại (115 dòng), đủ 5 mục:
  `# ADR-0001` → `## Context` → `## Options` → `## Decision` → `## Consequences` → `## Revisit`
- ADR có mục "Lý do loại phương án khác" (Node/NestJS, Go-only, Java-only) + lý do loại Kafka-first và
  Kong/APISIX-ngay
- Mục Revisit có 4 điều kiện định lượng (ví dụ "> 30% thời gian mỗi ticket dành cho plumbing 2 ngôn ngữ")
- `architecture.md` §11 đổi thành "Tech stack (đã chốt — ADR-0001)"; §11.2/§11.3 đổi "Đề xuất" → "Quyết định";
  §11.4 header "Đề xuất" → "Chọn"; hết chữ "CẦN CHỐT"
- `architecture.md` §10.2 cập nhật: JDK 21 + Maven/Gradle + Go toolchain (bỏ Node/.NET); Git đánh dấu đã `git init`
- Grep xác nhận `architecture.md` không còn "CẦN CHỐT"; "NestJS/Node" chỉ còn trong bảng Options (giữ để so sánh)
- 2026-11-02: user **confirm** điều chỉnh §11.4 — Test/Lint/Git hooks dùng tooling theo ngôn ngữ
  (JUnit 5 + Testcontainers / Spotless + Checkstyle / Lefthook) thay cho tooling Node (Jest/ESLint/Husky)

**Learning note**
- Polyglot là hợp lệ nhưng có giá: 2 service template, 2 shared lib, 2 toolchain CI → chọn **staged** để
  không gánh phức tạp ngay Phase 1.
- Đổi ngôn ngữ kéo theo đổi **tooling phụ trợ**: ESLint/Prettier/Husky (Node) không dùng được cho Java/Go →
  phải chuyển sang Spotless/gofmt + Lefthook. Bài học: tooling theo stack, không theo thói quen.
- ADR buộc phải ghi **lý do loại** phương án không chọn — phần này khó viết hơn tưởng nhưng mới là giá trị.

---

### SL-0.2 — Bootstrap repo skeleton, convention và dev environment

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Review |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.1, SL-0.7 |
| **Ước lượng** | M |

**Bối cảnh**
Repo đã có tài liệu (`AGENTS.md`, `architecture.md`, `Backlog.md`, `docs/adr/`, `.clinerules/`) và đã
`git init`, nhưng **chưa có cấu trúc thư mục, chưa có tooling**, và dev env **chưa** chuyển sang WSL2
(ADR-0002). Stack đã chốt ở SL-0.1 (ADR-0001). Cần bộ khung chuẩn trước khi viết dòng code đầu tiên để
tránh mỗi service một kiểu.

**Mục tiêu**
Có repo skeleton chạy được: cấu trúc thư mục rõ, lint/format/commit convention được enforce tự động,
và máy dev sẵn sàng build service đầu tiên.

**In scope**
- `.gitignore` (bỏ `target/`, `build/`, `bin/`, `.env`, log, volume data) + `.editorconfig` + `.gitattributes` (`* text=auto eol=lf`)
- Cấu trúc thư mục: `services/`, `packages/` (shared lib), `infra/`, `docs/adr/`, `tools/` (script)
- Lint/format: **Spotless + Checkstyle** (Java) · **gofmt + golangci-lint** (Go)
- Git hooks: **Lefthook** + check Conventional Commits
- `README.md`: giới thiệu, yêu cầu môi trường, cách chạy, link tới `AGENTS.md` / `architecture.md` / `Backlog.md`
- `.env.example` với các biến chung (không có secret thật)
- **Chuyển dev env sang WSL2** (ADR-0002): repo nằm trong Linux fs (`~/`), cài tooling **trong WSL**; set `git config core.autocrlf false` + `git config core.fileMode false`
- Cài runtime/tooling còn thiếu **trong WSL** (theo ADR-0001): JDK 21 + Maven/Gradle + Lefthook; ghi version vào README

**Out of scope**
- Chưa viết service nào
- Chưa dựng infra (đó là SL-0.3)
- Chưa cấu hình CI (đó là SL-0.5)

**Acceptance Criteria**
- [x] `git status` sạch, commit đầu tiên theo Conventional Commits
- [x] Cấu trúc thư mục khớp mô tả, có `.gitkeep` cho thư mục rỗng
- [x] Lệnh lint + format chạy được và pass trên skeleton rỗng
- [x] Commit sai convention bị **Lefthook** chặn (đã thử và bị chặn thật)
- [x] `README.md` có mục "Yêu cầu môi trường" ghi rõ version runtime + lệnh cài (chạy trong WSL2)
- [x] `.env.example` đủ biến cho Phase 1, `.env` đã nằm trong `.gitignore`
- [x] Không có secret thật nào trong repo (đã kiểm tra bằng grep)
- [x] `.gitattributes` có `* text=auto eol=lf`; `git status` **sạch** sau khi chuyển sang WSL (không nhiễu line-ending/mode)
- [x] Repo nằm trong Linux fs của WSL2 (`~/...`), không ở `/mnt/d/...` (kiểm tra `pwd` + `df -T`)

**Ghi chú kỹ thuật**
- Java: **Maven multi-module** (hoặc Gradle) để share `packages/` (shared lib hạ tầng); Go dùng `go modules`
  khi tới Phase 2–3 (xem ADR-0001)
- Không dùng Nx/Turborepo ở phase này — thêm công cụ khi chưa cần là over-engineer
- Convention đặt tên service: `kebab-case` + hậu tố `-service`, riêng gateway là `api-gateway`
- Dev env trong **WSL2** (ADR-0002): repo ở `~/`, giới hạn RAM qua `%UserProfile%\.wslconfig`
- Chuyển Windows → WSL dễ làm `git status` **bẩn** vì line endings (CRLF↔LF) và file mode (executable bit).
  Xử lý bằng `.gitattributes` (`* text=auto eol=lf`) + `git config core.autocrlf false` + `git config core.fileMode false`

**Kết quả verify** *(2026-11-02 — output thật)*

- `lefthook install` → `sync hooks: ✔️(pre-commit, pre-push, commit-msg)`
- **Commit sai convention bị chặn thật** (AC): `git commit -m "update code"` →
  `🥊 lefthook hook: commit-msg` → `❌ Commit message KHÔNG đúng Conventional Commits.` → `exit status 1`
  (commit không được tạo). pre-commit cũng chạy: `secret-scan ✔️`, `java-format (skip) no files for inspection`.
- `bash tools/protect-main.sh` (test trong repo tạm đang ở nhánh `main`) →
  `❌ Không push trực tiếp lên 'main'.` → exit code `1`; ở nhánh feature → exit `0`.
- `bash tools/secret-scan.sh $(git ls-files --others --exclude-standard)` → exit `0` (không false-positive,
  kể cả trên chính `tools/secret-scan.sh`); với file chứa `ghp_***` giả → exit `1` và in ra dòng vi phạm.
- `mvn verify` (JDK **21.0.13 Temurin** + Maven **3.9.16**, cài tạm không cần sudo chỉ để verify) →
  `spotless:3.10.4:check` + `checkstyle:12.3.1:check` → `You have 0 Checkstyle violations.` → **BUILD SUCCESS**.
- **Test âm** (chứng minh config không vô nghĩa): tạo `src/main/java/com/shoplab/demo/Demo.java` viết xấu →
  `mvn spotless:check` → **BUILD FAILURE** `The following files had format violations`; sau `spotless:apply`
  file được format đúng chuẩn google-java-format; thêm `import java.util.List;` thừa → `checkstyle:check` →
  **BUILD FAILURE** `Unused import - java.util.List. [UnusedImports]`. Đã xoá `src/` sau khi test.
- `pwd` → `/home/anchu/shop-lab-microservices`; `df -T .` → `/dev/sdd  ext4` (Linux fs → đạt ADR-0002)
- `git status --short` → rỗng (không nhiễu line-ending/mode)
- **Còn lại (cần sudo của user)**: cài JDK 21 + Maven **vào WSL** để `java`/`mvn` có trên PATH:
  `sudo apt update && sudo apt install -y openjdk-21-jdk maven`. Trước đó `which mvn` trỏ vào bản **Windows**
  `/mnt/d/Programs/Maven/apache-maven-3.8.6/bin/mvn` (trái ADR-0002) và `java` MISSING.

**Learning note**

- `maven-checkstyle-plugin` resolve **dependency của plugin từ POM của plugin** → `-Dcheckstyle.version`
  trên CLI **không** có tác dụng; muốn pin version phải khai báo `<plugin><dependencies>`. Mặc định của
  plugin 3.6.0 là checkstyle **9.3 (2022)** → đã pin **12.3.1** (verify lại: 0 violations, BUILD SUCCESS).
- Verify "pass" trên repo **rỗng là vô nghĩa** — phải làm **test âm** (cố tình viết sai) mới biết config có
  thật sự tác dụng. Đây là cách phân biệt "xanh vì không có gì để check" và "xanh vì đúng".
- Lefthook `pre-push` chỉ là phương án **local** thay branch protection: người khác clone repo **không** được
  bảo vệ → khi repo chuyển public / nâng Pro thì nên bật lại branch protection thật.
- `lefthook` **không** có trong apt repo Ubuntu → cài bằng binary release vào `~/.local/bin` (không cần sudo).
- `mvn` bị lẫn bản Windows qua PATH kế thừa của WSL → cài tooling **trong WSL** rồi kiểm tra `which mvn`.

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
- [ ] `mvn -q verify` (Java) pass, coverage phần config/health ≥ 80%
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
| 2026-11-02 | SL-0.6 | Cline Hooks/Plugins không chạy trên VS Code extension → dùng Rule + Skill + Workflow; skill auto-trigger nhờ `description` cụ thể. |
| 2026-11-02 | SL-0.1 | Chốt polyglot staged (Java-first + Go); trade-off rõ: 2 template/shared lib/CI. Lint/test/git-hooks phải đổi theo ngôn ngữ, không dùng tooling Node. |
| 2026-11-02 | SL-0.7 | Dev env = WSL2: Docker Desktop vốn đã dùng WSL2 backend → cần đưa source sang Linux fs (`~`), không để `/mnt/d/...`. |
| 2026-11-02 | SL-0.8 | Chọn GitHub Flow (không GitFlow) cho lab 1 người; repo đang ở `master` cần rename → `main`; PR cần remote + `gh`. |
| 2026-11-02 | SL-0.8 | Diff nhiễu **CRLF** (1253/1253 dòng) do thiếu `.gitattributes` + `core.autocrlf` unset → chuẩn hoá LF. **Branch protection/rulesets không dùng được trên repo private + GitHub Free (403)** → phải thay bằng git hook local. |
| 2026-11-02 | SL-0.2 | Verify trên repo **rỗng là vô nghĩa** — phải làm **test âm** mới biết config có tác dụng. `maven-checkstyle-plugin` **không** nhận `-Dcheckstyle.version` (dependency của plugin resolve từ POM của plugin) → phải khai báo `<plugin><dependencies>`; mặc định là checkstyle 9.3 (2022). Lefthook `pre-push` chỉ bảo vệ **local**. |

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




