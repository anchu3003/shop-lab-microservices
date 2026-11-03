# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | `SL-0.5` |
| **Ticket tiếp theo** | Phase 1 (epic — breakdown khi tới lượt) |
| **Số ticket chi tiết hiện có** | 8 (Phase 0) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | 🔵 In Progress | 7 / 8 |
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
| **Trạng thái** | Done |
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
- **Đóng ticket — verify lần cuối (2026-11-02, sau khi cài tooling trong WSL)**: điều kiện còn lại đã đạt.

  ```console
  $ java -version            → openjdk version "21.0.12.1" 2026-08-18      (which java: /usr/bin/java)
  $ mvn -v                   → Apache Maven 3.9.12                         (which mvn:  /usr/bin/mvn)
  $ lefthook version         → 2.2.2                                       (~/.local/bin/lefthook)
  $ mvn -B verify            → spotless:3.10.4:check + checkstyle:3.6.0:check
                               "You have 0 Checkstyle violations." → BUILD SUCCESS (1.085 s)
  $ pwd ; df -T .            → /home/anchu/shop-lab-microservices · /dev/sdd ext4
  $ git status --short       → rỗng (sạch)
  $ git check-ignore -v .env → .gitignore:10:.env
  ```

  ⇒ `which mvn` giờ là `/usr/bin/mvn`, **không** còn lẫn bản Windows `/mnt/d/Programs/Maven/...` → đạt ADR-0002.
- **Test âm lặp lại trên bản Lefthook đã cài thật** (chứng minh hook có tác dụng, không phải "xanh vì rỗng"):
  `git commit -m "update stuff"` → `🥊 lefthook v2.2.2 hook: commit-msg` →
  `❌ Commit message KHÔNG đúng Conventional Commits.` → `exit status 1`, số commit **không đổi** (10 → 10).
  File chứa `AKIA***` giả → `secret-scan` chặn: `❌ Nghi vấn secret trong: negtest-sl02b.txt`.
  `bash tools/protect-main.sh` trên nhánh feature → `exit 0`.
- **Hook chạy thật trên commit thật**: `eef58a3`, `ff54587` (ticket `SL-0.3`) đi qua đủ 3 hook —
  `pre-commit` (`secret-scan ✔️`, `java-format skip`) → `commit-msg` (`conventional-commit ✔️`) →
  `pre-push` (`protect-main ✔️`).

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
- **Verify bằng tooling cài tạm là chưa đủ** (bài học khi đóng ticket): lần verify đầu dùng Temurin 21.0.13 +
  Maven 3.9.16 cài tạm **không nằm trên PATH**, nên `which mvn` vẫn trỏ bản Windows ⇒ kết luận "đạt ADR-0002"
  lúc đó là **chưa** chứng minh được gì. Phải cài thật trong WSL rồi `which mvn` ra `/usr/bin/mvn` mới tính.
  → **Verify phải chạy bằng đúng công cụ mà dev sẽ dùng, không phải bản dựng tạm.**
- **Git hook nằm trong `.git/hooks/` — không được commit.** Người clone mới **phải** tự chạy `lefthook install`
  thì hook mới có; nếu không, mọi bảo vệ (Conventional Commits, secret-scan, chặn push `main`) đều **im lặng
  mất tác dụng**. Đây là lỗ hổng đã biết của phương án "dùng hook local thay branch protection" — cần nhắc
  trong README (đã có) và nên kiểm tra ở `SL-0.5` (CI) vì CI mới là chốt chặn thật.
- Test âm với file `.tmp` **không** chạy được vì `.gitignore` đã ignore `*.tmp` → `git add` báo
  "paths are ignored", test vô hiệu. Dùng đuôi file **không** bị ignore mới test đúng.

---

### SL-0.3 — Dựng local infrastructure bằng Docker Compose

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 0 — Foundation |
| **Phụ thuộc** | SL-0.2 (skeleton đã merge ở PR #2 — repo skeleton dùng được dù SL-0.2 còn `Review` vì lý do cài tooling) |
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
- [x] `docker compose up -d` → tất cả container `healthy` (chứng minh bằng `docker compose ps`)
- [x] Kết nối được từ host: `psql` vào từng database, `redis-cli ping` trả `PONG`
      *(điều chỉnh: Redis bật `requirepass` nên phải `redis-cli -a "$REDIS_PASSWORD" --no-auth-warning ping`;
      `redis-cli ping` trần trả `NOAUTH Authentication required.` — xem Learning note)*
- [x] Broker UI mở được trên browser, thấy được exchange/queue
- [x] Chạy `tools/reset-infra.sh` → dữ liệu về trạng thái ban đầu, không lỗi
      *(đổi tên từ `tools/reset-infra.ps1` → `.sh`: ADR-0002 chốt dev env = WSL2, mọi script trong `tools/` là bash)*
- [x] `docker compose down` rồi `up` lại → dữ liệu cũ vẫn còn (chứng minh volume hoạt động)
- [x] Không có port nào trùng nhau, danh sách port được ghi trong `README.md`
- [x] Không hard-code password trong file compose — đọc từ `.env`

**Ghi chú kỹ thuật**
- Ưu tiên image **alpine/slim** để nhẹ, nhưng đảm bảo có healthcheck tool (`pg_isready`, `redis-cli`)
- Đặt tên container có prefix `shoplab-` để dễ filter khi debug
- Trên Windows + Docker Desktop: dùng **named volume** thay vì bind mount cho data
- Ghi lại dung lượng RAM mà cả stack chiếm — dùng làm baseline cho Phase 7 (K8s resource request)

**Quyết định khi implement** *(chốt với user 2026-11-02)*
- `tools/reset-infra.sh` (bash) thay `tools/reset-infra.ps1` — khớp ADR-0002 (WSL2).
- Broker = **RabbitMQ 4** (`rabbitmq:4-management-alpine`), theo ADR-0001 §11.2 ("Phase 3 dùng RabbitMQ").
- DB UI = **Adminer** (`adminer:4-standalone`) thay pgAdmin — nhẹ, 1 container.
- **Bỏ MinIO + OpenSearch** ở ticket này (MinIO là "tuỳ chọn", OpenSearch không nằm trong In scope;
  cả hai nặng RAM) → dựng khi phase cần thật. Đã ghi baseline RAM để Phase 7 tính.
- Biến nhạy cảm dùng `${VAR:?}` (**required**) thay vì `${VAR:-default}` → thiếu `.env` là compose
  **fail-fast**, không âm thầm chạy bằng password mặc định yếu. Biến không nhạy cảm (port, tên DB, user)
  mới có `:-default`.
- Init script là `.sh` (đọc env `POSTGRES_DB_*`) thay vì `.sql` cứng → không lặp tên database ở 2 nơi.

**Kết quả verify** *(2026-11-02 — output thật; WSL2 · Docker 29.9.2 · Compose v5.6.1 · RAM host 15 GiB)*

File tạo/sửa: `infra/docker-compose.yml` · `infra/init/01-create-databases.sh` · `tools/reset-infra.sh`
· `.env.example` (thêm `POSTGRES_DB`, `ADMINER_PORT`) · `README.md` · `Backlog.md` · `architecture.md`.

```console
# --- AC1: up + ps → tất cả healthy ---
$ docker compose -f infra/docker-compose.yml --env-file .env up -d
 Container shoplab-postgres Healthy
 Container shoplab-adminer Started

$ docker compose -f infra/docker-compose.yml --env-file .env ps
NAME               SERVICE    STATUS                    PORTS
shoplab-adminer    adminer    Up 43 seconds (healthy)   0.0.0.0:8085->8080/tcp, [::]:8085->8080/tcp
shoplab-postgres   postgres   Up 49 seconds (healthy)   0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp
shoplab-rabbitmq   rabbitmq   Up 49 seconds (healthy)   0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
shoplab-redis      redis      Up 49 seconds (healthy)   0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp

# --- AC2a: psql vào TỪNG database (client chạy --network host ⇒ đi đúng port đã publish) ---
$ for db in identity catalog inventory promotion order payment notification; do
    docker run --rm --network host -e PGPASSWORD="$POSTGRES_PASSWORD" postgres:16-alpine \
      psql -h 127.0.0.1 -U shoplab -d "$db" -tAc 'select current_database()'; done
identity      → identity
catalog       → catalog
inventory     → inventory
promotion     → promotion
order         → order        # tên DB là reserved word → init script dùng format('%I') để quote
payment       → payment
notification  → notification

# --- AC2b: redis ---
$ docker run --rm --network host redis:7-alpine redis-cli -h 127.0.0.1 ping
NOAUTH Authentication required.            # đúng như dự kiến (có requirepass)
$ docker run --rm --network host redis:7-alpine \
    redis-cli -h 127.0.0.1 -a "$REDIS_PASSWORD" --no-auth-warning ping
PONG

# --- AC3: UI ---
$ curl -s -o /dev/null -w '%{http_code}\n' http://localhost:15672/                     → 200 (RabbitMQ UI)
$ curl -s -o /dev/null -w '%{http_code}\n' -u shoplab:****** http://localhost:15672/api/overview → 200
$ curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8085/                      → 200 (Adminer)
```
```console
# --- AC4: reset script ---
$ bash tools/reset-infra.sh
→ Dừng container + xoá volume (mất sạch dữ liệu local)...
→ Dựng lại hạ tầng...
→ Chờ container healthy (tối đa 180s)...
✅ Tất cả container đã healthy.
$ # kiểm chứng: bảng test tạo trước đó đã biến mất
$ psql ... -tAc "select count(*) from information_schema.tables where table_name='smoke_persist'"
0

# --- AC5: volume bền qua down/up ---
$ # tạo bảng + 1 dòng trong DB catalog, rồi:
$ docker compose -f infra/docker-compose.yml --env-file .env down     # KHÔNG dùng -v
$ docker compose -f infra/docker-compose.yml --env-file .env up -d
$ psql ... -d catalog -tAc 'select count(*) from smoke_persist'
1                                    # dữ liệu còn nguyên ⇒ named volume hoạt động

# --- AC6: port listen, không trùng ---
$ ss -ltn | grep -E ':(5432|6379|5672|15672|8085)\b'
*:5432   *:6379   *:5672   *:15672   *:8085          # 5 port riêng biệt

# --- AC7: không hard-code password (test âm, môi trường SẠCH) ---
$ env -i PATH="$PATH" HOME="$HOME" docker compose -f infra/docker-compose.yml config   # KHÔNG --env-file
error while interpolating services.postgres.environment.POSTGRES_PASSWORD:
  required variable POSTGRES_PASSWORD is missing a value: POSTGRES_PASSWORD chưa set — cp .env.example .env rồi chạy lại
error while interpolating services.rabbitmq.environment.RABBITMQ_DEFAULT_PASS: ...
error while interpolating services.redis.environment.REDIS_PASSWORD: ...
$ env -i PATH="$PATH" HOME="$HOME" docker compose -f infra/docker-compose.yml --env-file .env config
compose config OK
```

**RAM baseline** (`docker stats --no-stream`, lúc idle — dùng cho Phase 7):

| Container | RAM idle |
|---|---|
| `shoplab-postgres` | ~25 MiB |
| `shoplab-redis` | ~4.4 MiB |
| `shoplab-rabbitmq` | ~106 MiB |
| `shoplab-adminer` | ~13 MiB |
| **Tổng** | **~149 MiB** (lần đo khác ~163 MiB → ghi nhận khoảng 145–165 MiB) |

*Chưa gồm OpenSearch + MinIO (chưa dựng) — hai cái này nặng hơn nhiều.*

**Learning note**

- 🔴 **`docker compose` KHÔNG đọc `.env` ở thư mục đang đứng** khi file compose nằm ở thư mục khác.
  Nó nạp `.env` theo **project directory** = thư mục chứa file compose (`infra/`). `.env` để ở gốc repo
  ⇒ **buộc** phải `--env-file .env`. Đã kiểm chứng 2 chiều: CWD = repo root → không resolve được;
  thêm `--env-file .env` → OK.
- 🔴 **Test âm phải chạy trong môi trường sạch.** Lần đầu tôi test "bỏ `--env-file` xem có lỗi không"
  và thấy **không lỗi** → suýt kết luận sai. Nguyên nhân: trước đó tôi đã `set -a && . ./.env && set +a`
  trong cùng shell ⇒ biến đã **export vào environment**, Compose resolve từ env chứ không từ file.
  Chạy lại bằng `env -i PATH=... HOME=...` mới thấy lỗi thật. **Đây là bẫy chung khi verify compose.**
- **Postgres init script chỉ chạy khi data dir rỗng.** Sửa `infra/init/*.sh` xong mà không `down -v`
  thì script **không** chạy lại → tưởng sửa không có tác dụng. `tools/reset-infra.sh` tồn tại vì lý do này.
- `order` là **reserved word** trong SQL ⇒ phải quote identifier. Dùng `format('CREATE DATABASE %I ...')`
  thay vì nối chuỗi tay → an toàn với mọi tên DB.
- `redis-cli ping` trần và `requirepass` **không thể cùng tồn tại**: AC gốc ("`redis-cli ping` trả `PONG`")
  mâu thuẫn với việc Redis có password. Đã ưu tiên **có password** (đúng 12-factor, khớp `.env.example`)
  và điều chỉnh lệnh verify. → **Bài học: AC cần ghi cả điều kiện auth, không chỉ "trả PONG".**
- `${VAR:?message}` là cách tốt để **fail-fast** khi thiếu secret: lỗi hiện ngay lúc parse compose kèm
  hướng dẫn, thay vì chạy với password mặc định yếu. Đánh đổi: không thể `up` "chay" mà không có `.env`.
- Chọn image theo **healthcheck tool có sẵn**, không chỉ theo kích thước: `adminer:4-standalone` là Alpine
  và có `wget`/`curl` ⇒ healthcheck được; `rabbitmq:4-management-alpine` (4.4.0) có `rabbitmq-diagnostics`
  ⇒ `ping` sạch hơn là `curl` vào UI.
- Nhiều lệnh trong **cùng 1 call thì chạy song song** → `docker compose ps` bắt được lúc container còn
  `health: starting`, dễ kết luận sai là "chưa healthy". Verify phải **tuần tự** hoặc chờ đủ.
- `docker compose down` giữ volume; `down -v` mới xoá → phân biệt rõ "restart" vs "reset".
- **Exec bit không đáng tin trong repo này**: `core.fileMode false` (SL-0.2) nên git ghi `100644` dù file
  trên máy là `755`. Với `infra/init/*.sh` điều này **không** sao: entrypoint của Postgres tự chọn
  `exec` nếu file có `+x`, ngược lại thì `source`. Đã verify cả 2 đường đi (mode 644 → log
  `sourcing /docker-entrypoint-initdb.d/...` → vẫn tạo đủ 7 DB). Các script khác trong `tools/` đều gọi
  bằng `bash <file>` nên cũng không cần `+x`.





---

### SL-0.4 — Service template và shared infrastructure library

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
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
- [x] `docker compose up -d` → `service-template` healthy, gọi `GET /health` trả 200
- [x] `GET /ready` trả 200 khi dependency OK, trả 503 khi tắt dependency (đã thử tắt thật)
- [x] Thiếu env bắt buộc → service fail lúc boot với message rõ ràng, không chạy nửa vời
- [x] Log ra JSON có `traceId`, và `traceId` xuất hiện trong response lỗi
- [x] `docker stop` → service shutdown êm, log ghi rõ đang chờ request nào, không bị kill cứng
- [x] `mvn -q verify` (Java) pass, coverage phần config/health ≥ 80%
- [x] Image size ghi lại trong ticket (để so sánh khi optimize sau)
- [x] `README.md` có hướng dẫn tạo service mới từ template, đã thử làm theo và chạy được

**Ghi chú kỹ thuật**
- Trade-off: shared library giúp đồng bộ nhưng tạo coupling. Chỉ để **hạ tầng** trong shared lib
  (log, config, error, health), **tuyệt đối không** để domain logic hay database entity chung
- Giữ template tối giản — mỗi thứ thêm vào phải trả lời được "service thật có cần không"

**Quyết định khi implement** *(chốt với user 2026-11-02)*
- **Spring Boot 4.1.2** (latest stable) — ADR-0003. Parent POM giữ convention riêng, **import BOM**
  `spring-boot-dependencies` thay vì dùng `spring-boot-starter-parent`.
- Maven multi-module: `packages/service-commons` (artifactId `shoplab-service-commons`) +
  `services/service-template` (`shoplab-service-template`); version nội bộ dùng `${project.version}`.
- Shared lib dùng **Spring Boot auto-configuration** (`AutoConfiguration.imports`), mọi bean
  `@ConditionalOnMissingBean` → service ghi đè được. Chi tiết: ADR-0004.
- **Tự viết** `/health` `/ready` `/version`, **không** dùng Actuator (để Phase 6) — đúng thiết kế ticket
  và giữ phần "học cơ chế probe".
- Dependency check mức **TCP** (`DependencyHealthCheck` + `TcpHealthCheck`) — không kéo JDBC/Redis client
  vào shared lib. Phase 1 thay bằng check sâu mà không phải sửa controller.
- Log JSON dùng **logstash-logback-encoder 9.0**, format đặt ở shared lib (`logback-shoplab.xml`) để mọi
  service log giống nhau.
- Thêm endpoint **dev-only** `/debug/slow`, `/debug/error`, `/debug/boom` (bật bằng
  `SHOPLAB_DEBUG_ENDPOINTS=true`, chỉ set trong compose local) để **thật sự verify** AC graceful shutdown
  và error response — không có request đang chạy thì không có gì để log.
- Port `SERVICE_TEMPLATE_PORT=8090` (8080–8082 dành cho Phase 1).

**Kết quả verify** *(2026-11-02 — output thật; WSL2 · JDK 21.0.12.1 · Maven 3.9.12 · Docker 29.9.2)*

File tạo: `packages/service-commons/**` · `services/service-template/**` (gồm `Dockerfile`) ·
`.dockerignore` · `docs/adr/0003-*.md` · `docs/adr/0004-*.md`.
File sửa: `pom.xml` · `config/checkstyle/checkstyle.xml` · `infra/docker-compose.yml` · `.env.example` ·
`README.md` · `architecture.md` · `Backlog.md`.

```console
# --- AC: mvn verify (spotless + checkstyle + test + jacoco gate) ---
$ mvn -B verify
[INFO] ShopLab :: Service Commons ...................... SUCCESS [ 2.177 s]   Tests run: 37
[INFO] You have 0 Checkstyle violations.
[INFO] All coverage checks have been met.
[INFO] ShopLab :: Service Template ..................... SUCCESS [ 5.120 s]   Tests run: 35
[INFO] You have 0 Checkstyle violations.
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS                       → tổng 72 test, 0 failure

# --- AC: docker compose up -d → service-template healthy ---
$ docker compose -f infra/docker-compose.yml --env-file .env up -d
$ docker compose -f infra/docker-compose.yml --env-file .env ps
NAME                       STATUS
shoplab-adminer            Up (healthy)
shoplab-postgres           Up (healthy)
shoplab-rabbitmq           Up (healthy)
shoplab-redis              Up (healthy)
shoplab-service-template   Up (healthy)        ← 5/5 healthy

# --- AC: GET /health → 200 ---
$ curl -s -w 'status=%{http_code}\n' localhost:8090/health
{"status":"UP"}
status=200

# --- AC: GET /ready → 200 khi dependency OK ---
$ curl -s localhost:8090/ready
{"status":"UP","dependencies":[
  {"name":"postgres","status":"UP","detail":"ok","latency":"PT0.006409952S"},
  {"name":"redis","status":"UP","detail":"ok","latency":"PT0.001030498S"}]}

# --- AC: GET /version → có git commit thật ---
$ GIT_COMMIT=$(git rev-parse --short HEAD) docker compose ... up -d --build service-template
$ curl -s localhost:8090/version
{"service":"service-template","version":"0.1.0-SNAPSHOT","gitCommit":"01ebfb6","buildTime":"2026-11-02T16:52:29.386Z"}
```

```console
# --- AC: tắt dependency → /ready trả 503 (đã thử tắt thật) ---
$ docker stop shoplab-postgres && curl -s -w 'status=%{http_code}\n' localhost:8090/ready
{"status":"DOWN","dependencies":[
  {"name":"postgres","status":"DOWN","detail":"UnknownHostException: postgres","latency":"PT3.992760968S"},
  {"name":"redis","status":"UP","detail":"ok","latency":"PT0.001370696S"}]}
status=503
$ docker start shoplab-postgres && curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/ready
200

# --- AC: thiếu env bắt buộc → fail-fast, message SẠCH (không stack trace rối) ---
$ docker run --rm --network shoplab-net shoplab-service-template:local     # không set APP_ENV

[shoplab] KHỞI ĐỘNG THẤT BẠI — cấu hình không hợp lệ
Thiếu biến môi trường bắt buộc — service dừng ngay để không chạy nửa vời:
  - APP_ENV: môi trường chạy: local | dev | staging | prod
Xem .env.example để lấy giá trị mẫu.
exit=1

# --- AC: response lỗi thống nhất + traceId (lấy từ header client) ---
$ curl -s -H 'x-correlation-id: trace-from-client-001' localhost:8090/debug/error
{"code":"NOT_FOUND","message":"Lỗi cố ý từ /debug/error để xem format ApiError",
 "traceId":"trace-from-client-001","timestamp":"2026-11-02T16:50:48.489871347Z",
 "path":"/debug/error","details":[]}

$ curl -s localhost:8090/khong-ton-tai                      # 404, KHÔNG bị biến thành 500
{"code":"NOT_FOUND",...,"path":"/khong-ton-tai","details":[]}

$ curl -s localhost:8090/debug/boom                         # 500, KHÔNG lộ chi tiết nội bộ
{"code":"INTERNAL_ERROR","message":"Lỗi hệ thống",...}      # chi tiết gốc chỉ nằm trong log

# --- AC: log JSON có traceId / service / level / msg ---
$ docker logs shoplab-service-template | tail -4   (rút gọn)
{"@timestamp":"...","msg":"http GET /health -> 200 (1 ms)","logger":"...",
 "thread":"http-nio-8090-exec-1","level":"INFO",
 "traceId":"02cc88e6-cf15-4f0d-afa8-32119bdfe29e","service":"service-template"}
→ đúng 4 field bắt buộc; `service` lấy từ spring.application.name qua shared lib.

# --- AC: graceful shutdown có request đang chạy (bằng chứng đầy đủ) ---
$ curl -s -H 'x-correlation-id: drain-demo-001' 'localhost:8090/debug/slow?ms=8000' &
$ sleep 2 && docker stop -t 35 shoplab-service-template
17:07:10.803 INFO  graceful shutdown: chờ 1 request đang chạy hoàn tất: [drain-demo-001 GET /debug/slow (2051 ms)]
17:07:10.804 INFO  Commencing graceful shutdown. Waiting for active requests to complete
17:07:16.760 INFO  http GET /debug/slow -> 200 (8014 ms)      ← request hoàn tất trọn 8s, KHÔNG bị kill
17:07:16.795 INFO  Graceful shutdown complete
$ cat /tmp/slow.out
{"sleptMs":8000}                                              ← client nhận response đầy đủ
```

**Coverage & image size** *(số thật từ `target/site/jacoco/jacoco.csv` + `docker image ls`)*

| Package (gate ≥ 80% LINE) | Kết quả |
|---|---|
| `com.shoplab.commons.config.*` | `EnvValidator` 85% · `RequiredConfig` 100% · `MissingConfigException` 100% |
| `com.shoplab.commons.health.*` | `TcpHealthCheck` 100% · `HealthCheckResult` 100% · `HealthStatus` 100% · `DependencyHealthCheck` 100% |
| `com.shoplab.template.config.*` | `ServiceProperties` 100% · `AppEnv` 100% · `Readiness`/`Dependency` 100% · `RequiredEnv` 80% |
| `com.shoplab.template.health.*` | `ReadinessService` 100% · `HealthController` 100% |

Ngoài gate (không ép coverage): `web` của commons (`GlobalExceptionHandler` 100%, `CorrelationIdFilter` 100%,
`ApiError` 100%, `CorrelationId` 85%), `web` của template (`VersionController` 100%,
`GracefulShutdownLogger` 87%, `RequestTracker` 81%, `DebugController` 100%).

**Image size**: `shoplab-service-template:local` = **370 MB** (base `eclipse-temurin:21-jre-alpine` = 287 MB
→ layer ứng dụng ≈ 83 MB, chủ yếu là fat jar). Ghi lại làm baseline — Phase 7 có thể tối ưu bằng layered jar
(`layertools`) hoặc jlink/native image.

**Learning note**

- 🔴 **`mvn verify` xanh nhưng `Tests run: 0` — bug nguy hiểm nhất gặp ở ticket này.** Maven super-POM để
  mặc định `maven-surefire-plugin:2.17`, bản này **không hỗ trợ JUnit Platform**; Spring Boot 4 kéo JUnit
  6.0.3 → surefire chạy 0 test và **vẫn `BUILD SUCCESS`**. Không có bước kiểm tra số test thì pipeline
  "xanh" mà thực chất không test gì. → Đã pin surefire `3.6.1`. **Bài học: khi thấy `Tests run: 0` phải
  coi là lỗi, không phải "repo chưa có test".**
- 🔴 **Thiếu `-parameters` của javac là lỗi runtime, không phải lỗi compile.** `@RequestParam long ms`
  không có tên tường minh → Spring MVC ném `Name for argument of type [long] not specified` **lúc gọi
  request đầu tiên**. `spring-boot-starter-parent` bật sẵn cờ này; vì ta import BOM thay vì dùng
  starter-parent nên phải tự bật. **Bài học: bỏ starter-parent thì phải tự kiểm kê những gì nó làm sẵn**
  (đã ghi vào ADR-0003).
- 🔴 **Test không chạm endpoint thì bug không lộ.** Bug `-parameters` chỉ lộ khi tôi curl `/debug/slow` bằng
  tay — 72 test pass mà không test nào chạm endpoint đó. Đã thêm test gọi thật `/debug/slow` để chốt lại.
- 🔴 **Spotless và Checkstyle xung đột trực tiếp ở block rỗng.** google-java-format luôn ép `{}`; checkstyle
  `WhitespaceAround` mặc định `allowEmpty*=false` đòi `{ }`. **Không thể thoả cả hai** → phải bật
  `allowEmptyTypes/Methods/Constructors/Lambdas/Catches/Loops=true`. Config này có từ `SL-0.2` nhưng chỉ lộ
  ra khi có code Java thật đầu tiên. **Bài học: lint + format phải được kiểm chứng trên code thật, không
  phải repo rỗng** (đúng như learning note của `SL-0.2`).
- **Tên method ảnh hưởng API**: `HealthCheckResult.isUp()` bị Jackson coi là getter → body `/ready` lộ thêm
  `"up": true`. Đổi thành `healthy()`. Với record trả ra API, tên method phải để ý.
- **`<includeContext>false</includeContext>` và `<includeTags>false</includeTags>` cho logstash encoder**:
  mặc định encoder nhét **mọi** property của logger context vào log (biến `SHOPLAB_SERVICE_NAME` của logback
  lọt ra thành field rác) và thêm field `tags` chỉ ở **một vài** dòng (log đi qua cầu commons-logging có
  marker `COMMONS-LOGGING`). Schema log không nhất quán sẽ làm Loki/ELK khó truy vấn.
- **`@EventListener(ContextClosedEvent)` KHÔNG dùng được cho graceful shutdown**: với
  `server.shutdown=graceful`, Spring Boot đợi request xong **trước khi** đóng context → nghe event đó thì
  đã quá muộn, lúc đó không còn request nào. Phải dùng `SmartLifecycle` với phase `Integer.MAX_VALUE`
  (cao hơn `WebServerStartStopLifecycle` = `MAX_VALUE - 1`) để chạy **trước** khi Tomcat drain.
- **Health check TCP không chặn được DNS resolution**: timeout 500ms chỉ áp cho bước `connect`. Khi tắt
  container, Docker gỡ DNS entry → fail bằng `UnknownHostException` sau ~4s. Muốn chặn cả DNS phải tự
  resolve trước hoặc dùng check sâu.
- **`${VAR:?}` fail-fast nhưng message bị chôn**: Spring Boot launcher bọc exception trong
  `InvocationTargetException` nên message đúng nhưng khó đọc. Đã bắt `MissingConfigException` ngay trong
  `main()` và in message sạch ra stderr rồi `System.exit(1)`.
- **`spring-boot-starter-web` đã deprecated ở Boot 4** → dùng `spring-boot-starter-webmvc`. Và
  `TestRestTemplate`/`@AutoConfigureMockMvc` **không còn** ở module test cũ → né bằng
  `@SpringBootTest(RANDOM_PORT)` + `@LocalServerPort` + `java.net.http.HttpClient` thuần (cũng tránh được
  việc phải theo dõi API test của Boot).
- **JaCoCo**: tham số đúng là `<value>COVEREDRATIO</value>` — viết `COVERAGE` sẽ lỗi
  `No enum constant ICounter.CounterValue.COVERAGE` lúc chạy `verify`, không phải lúc validate config.
- **Glitch môi trường (không tái lặp)**: một lần `docker start shoplab-postgres` fail với lỗi bind mount
  `./init:/docker-entrypoint-initdb.d` (Docker Desktop/WSL2 không tìm thấy đường dẫn mount nội bộ). Thử lại
  sau khi `docker rm -f` + `up -d` thì bình thường, và các lần `stop`/`start` sau đó đều OK → **không phải
  lỗi config**, chỉ là glitch của Docker Desktop. Nếu tái diễn thường xuyên thì nên bỏ bind mount, chuyển
  init script vào image (`COPY init/ /docker-entrypoint-initdb.d/`).
- 🔴 **Thực sự làm theo hướng dẫn trong README mới phát hiện bug "template không copy-safe".** Tôi copy
  `service-template` → `smoke-service` theo đúng các bước README rồi `mvn verify` → **BUILD FAILURE** vì test
  `versionShouldExposeServiceIdentity` **hard-code** `"service-template"`. Đã sửa: test đọc tên service từ
  `@Value("${spring.application.name}")` thay vì viết cứng. **Bài học: hướng dẫn "copy template" mà không
  chạy thử thì gần như chắc chắn sai** — cái sai không nằm ở bước copy mà ở giả định ngầm trong code
  (`application.yml` cũng hard-code tên). Sau khi sửa, quy trình chạy lại: `BUILD SUCCESS`, 35 test, jar
  `shoplab-smoke-service-0.1.0-SNAPSHOT.jar`. Đã xoá `smoke-service` sau khi verify.
- **Lần thứ 2 trong ticket này chứng minh điều tương tự**: bug `-parameters` lộ khi curl tay, bug
  "copy-safe" lộ khi làm theo README. Cả hai đều **không** lộ qua 72 test tự động. → Những AC dạng
  "đã thử làm theo và chạy được" phải được **thực thi thật**, không được suy luận.


---

### SL-0.5 — CI pipeline cơ bản (lint + test + build image)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Review |
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
- [x] Push lên nhánh bất kỳ → pipeline chạy tự động, xanh
- [x] Cố tình tạo lỗi lint → pipeline đỏ, job lint báo đúng file/dòng
- [x] Cố tình làm fail test → pipeline đỏ
- [x] Build image thành công trong CI
- [x] Thời gian pipeline lần đầu được ghi lại trong ticket
- [ ] Badge CI hiển thị đúng trạng thái trong `README.md` *(chờ merge vào `main` — badge lấy run mới nhất của `main`)*

**Ghi chú kỹ thuật**
- Chạy các job song song nếu độc lập để tiết kiệm thời gian
- Pin version của action (`@v4`) để tránh pipeline vỡ do upstream update

**Quyết định khi implement** *(chốt 2026-11-03)*
- Tách **3 job** (`lint` ∥ `test` → `build-image`) thay vì 1 job: CI UI chỉ rõ job nào đỏ; `lint`/`test` độc lập
  nên chạy song song; `build-image` là job nặng nhất nên chỉ chạy khi 2 job kia xanh (repo private tính **phút**
  Actions theo gói — Free chỉ 2.000 phút/tháng).
- Job `lint` tách **2 step** (`checkstyle:check` rồi `spotless:check`): Checkstyle chạy trước để lỗi hiện
  `file:line` (AC #2 đòi "báo đúng file/dòng") và CI UI chỉ rõ step nào đỏ.
- Job `test` chạy `mvn verify -Dcheckstyle.skip=true -Dspotless.check.skip=true` → giữ test + JaCoCo gate, không
  lặp lint. Đã **đo trước khi viết workflow**: 2 flag này thực sự skip spotless/checkstyle (log không còn
  "Starting audit...") mà `jacoco:check` vẫn chạy (`All coverage checks have been met.`).
- Trigger `push: branches: ['**']` (đúng AC "push nhánh bất kỳ") + `pull_request: branches: [main]` (để PR có
  check). `concurrency.group = ${{ github.workflow }}-${{ github.ref }}` + `cancel-in-progress: true` → chỉ huỷ
  run cũ **cùng ref** (push liên tiếp cùng nhánh, PR cập nhật liên tục).
- **Đã thử và bỏ cách gom push+PR chung một group** (`github.head_ref || github.ref_name`): khi đó run PR huỷ run
  push của cùng commit → `gh api .../commits/<sha>/check-runs` trả về **3 check `cancelled`** nằm cạnh 3 check
  `success`; PR hiện như có check fail dù code sạch, và sau này bật required-checks sẽ bị chặn oan. Đổi sang gom
  theo `github.ref` → mỗi push trên nhánh có PR sinh **2 run** (push + PR) chạy độc lập; đổi lại check sạch.
- Cache Maven qua `actions/setup-java@v4` (`cache: maven`) cho `lint`/`test`. `build-image` dùng `docker build`
  trần (không cache layer xuyên run) — chấp nhận chậm hơn để ít action/phụ thuộc. **Điều kiện revisit**: nếu job
  `build-image` > 3 phút thì chuyển sang `docker/build-push-action` + `cache-from: type=gha`.
- `build-image` **chỉ build** + `docker image ls`, **không** `docker run` smoke test → giữ đúng scope ticket;
  smoke-test container thật để Phase 1 (khi đã có integration test).
- Giữ remote **HTTPS** (không đổi sang SSH): token VS Code cấp qua `GIT_ASKPASS` có scope **`workflow`** nên push
  file `.github/workflows/*` qua HTTPS được (`git push --dry-run` → exit 0). Token của `gh` CLI thì **thiếu**
  `workflow` → push từ terminal **ngoài** VS Code sẽ fail, khi đó chạy
  `gh auth refresh -h github.com -s workflow && gh auth setup-git`, hoặc đổi sang SSH.
- Không cần ADR: CI đã chốt GitHub Actions ở `architecture.md` §10.3 (ADR-0001), ticket này chỉ hiện thực hoá.

**Kết quả verify** *(2026-11-03 — output thật từ GitHub Actions; runner `ubuntu-24.04`, JDK temurin 21)*

File tạo: `.github/workflows/ci.yml`. File sửa: `README.md` (badge + mục **CI** + cây thư mục),
`architecture.md` (§10.3 + Changelog), `Backlog.md`. PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5).

```console
# --- AC1: push nhánh bất kỳ → pipeline tự chạy, xanh (run 41657425300, event=push, sha 8260938) ---
$ gh run list --limit 1
completed success  feat(ci): add lint, ...  CI  feat/sl-0.5-ci-pipeline  push  41657425300  2m35s  2026-11-03T14:49:02Z
$ gh run view 41657425300 --json jobs --jq '.jobs[]|"\(.name) -> \(.status)/\(.conclusion)"'
lint (checkstyle + spotless)   -> completed/success     # 14:49:06 → 14:49:39  (33 s)
test (unit + jacoco gate)      -> completed/success     # 14:49:06 → 14:49:52  (46 s) ← SONG SONG với lint
build-image (service-template) -> completed/success     # 14:49:55 → 14:51:35
# run: created 14:49:02 → updated 14:51:37  ⇒ BASELINE lần đầu = 2m35s (cold cache)

# --- AC5: job lint thật sự check (không "xanh rỗng") ---
$ gh run view --job <lint> --log | grep -E 'Checkstyle violations|Spotless.Java|BUILD SUCCESS'
[INFO] You have 0 Checkstyle violations.      ×3 (parent · service-commons · service-template)
[INFO] Spotless.Java is keeping 23 files clean - 0 needs changes to be clean, 23 were already clean
[INFO] Spotless.Java is keeping 17 files clean - 0 needs changes to be clean, 17 were already clean
[INFO] BUILD SUCCESS   (Checkstyle step 12.312 s · Spotless step 6.545 s)

# --- AC: job test thật sự chạy test (chống bẫy "Tests run: 0" đã gặp ở SL-0.4) ---
$ gh run view --job <test> --log | grep -E 'Tests run:|coverage checks'
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0          ← service-commons
[INFO] All coverage checks have been met.
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 0          ← service-template
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS   Total time: 33.884 s
```

```console
# --- AC4: build image thành công trong CI ---
$ gh run view --job <build-image> --log | grep -E 'naming to|docker image ls|shoplab-service-template'
#23 naming to docker.io/library/shoplab-service-template:ci done
$ docker image ls shoplab-service-template:ci
shoplab-service-template   ci   138f1ff94268   1 second ago   252MB
# (trong image, `mvn verify` chạy luôn spotless + checkstyle + jacoco:check:
#  "Analyzed bundle 'shoplab-service-template' with 7 classes")

# --- AC2: cố tình vi phạm Checkstyle → pipeline ĐỎ, job lint báo đúng file/dòng (run 41658103835) ---
$ gh run view 41658103835 --json jobs --jq '.jobs[]|"\(.name) -> \(.status)/\(.conclusion)"'
lint (checkstyle + spotless)   -> completed/failure
test (unit + jacoco gate)      -> completed/success
build-image (service-template) -> completed/skipped     ← job nặng KHÔNG chạy khi lint đỏ
$ gh run view 41658103835 --log-failed | grep -E 'NeedBraces|exit code'
[ERROR] /home/runner/work/.../services/service-template/src/main/java/com/shoplab/template/health/HealthController.java:54:5: 'if' construct must use '{}'s. [NeedBraces]
[ERROR] src/main/java/com/shoplab/template/health/HealthController.java:[54,5] (blocks) NeedBraces: 'if' construct must use '{}'s.
##[error]Process completed with exit code 1.

# --- AC3: cố tình làm fail 1 test → pipeline ĐỎ (run 41658481217) ---
$ gh run view 41658481217 --json jobs --jq '.jobs[]|"\(.name) -> \(.status)/\(.conclusion)"'
lint (checkstyle + spotless)   -> completed/success
test (unit + jacoco gate)      -> completed/failure
build-image (service-template) -> completed/skipped
$ gh run view 41658481217 --log-failed | grep -E 'shouldReportLocalOnly|BUILD FAILURE'
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, ... -- in com.shoplab.template.config.AppEnvTest
[ERROR] com.shoplab.template.config.AppEnvTest.shouldReportLocalOnly -- Time elapsed: 0.011 s <<< FAILURE!
[ERROR]   AppEnvTest.shouldReportLocalOnly:48
[ERROR] BUILD FAILURE
```

Hai test âm làm trên nhánh tạm `test/sl-0.5-ci-negative` rồi **xoá** (không đưa vào PR). Code đã sạch trở lại:
`grep -c negativeTestNeedBraces` = `0`, `AppEnvTest` về `isFalse()`.

Run do `pull_request` trigger (mở PR #5): `41658741592` — `lint` success, `test` success, `build-image` success
⇒ check hiện trên PR. Workflow đã đăng ký: `gh api .../actions/workflows` → `CI | .github/workflows/ci.yml | state=active`.

```console
# --- Sau khi sửa `concurrency` (gom theo `github.ref`): push + PR chạy song song, không huỷ nhau ---
$ gh run list --limit 2                      # cùng lúc, 2 run khác group
in_progress  fix(ci): stop PR run ...  CI  feat/sl-0.5-ci-pipeline  push           41660147404  2026-11-03T15:05:02Z
in_progress  feat(ci): lint + test ...  CI  feat/sl-0.5-ci-pipeline  pull_request   41660155684  2026-11-03T15:05:05Z
# ~2.5 phút sau: push-run=completed/success   pr-run=completed/success
$ gh api repos/.../commits/72605c2/check-runs --jq '.check_runs[]|"\(.name) -> \(.status)/\(.conclusion)"'
build-image (service-template) -> completed/success
build-image (service-template) -> completed/success
lint (checkstyle + spotless)    -> completed/success
test (unit + jacoco gate)       -> completed/success
test (unit + jacoco gate)       -> completed/success
lint (checkstyle + spotless)    -> completed/success
# ⇒ 6/6 success, KHÔNG còn `cancelled` (trước khi sửa: 3 success + 3 cancelled trên cùng commit)
```

**AC #6 (badge)** chỉ verify được **sau khi merge vào `main`**: badge `actions/workflows/ci.yml/badge.svg` resolve
theo **default branch**, nên khi `ci.yml` chưa có trên `main` thì URL trả **404** (đã thử cả `Bearer` lẫn basic auth
→ `http=404 size=9 "Not Found"`). Ngoài ra repo **private** nên badge chỉ render trong trình duyệt đã đăng nhập.

**Learning note**
- **"Xanh" phải chứng minh bằng log, không suy luận**: job `test` mất 33.9 s cho 72 test — nhìn badge xanh thì
  không biết test có chạy thật hay không (SL-0.4 đã từng dính "Tests run: 0 mà BUILD SUCCESS" vì surefire cũ).
  Phải `grep 'Tests run:'` trong log CI mới dám kết luận.
- **CI cũng phải test âm mới tin được**: 2 run đỏ (vi phạm Checkstyle → `lint` đỏ; assert sai → `test` đỏ) mới
  chứng minh pipeline thật sự chặn, và chứng minh `needs:` hoạt động (`build-image` = `skipped`, không đốt phút).
  Một run xanh duy nhất **không** nói được gì về việc pipeline có chặn hay không.
- **Chọn loại vi phạm để test âm cũng phải nghĩ**: vi phạm `UnusedImports` sẽ bị Spotless (google-java-format) tự
  xoá ngay ở hook `pre-commit` → test âm "tự chết" trước khi tới CI. Dùng `NeedBraces` (formatter không sửa được)
  mới đi được tới CI. **Test âm phải sống sót qua các lớp tooling phía trước.**
- **Hai token trên cùng một máy có scope khác nhau**: token VS Code cấp qua `GIT_ASKPASS` có `workflow` (push được
  file `.github/workflows/*`), token `gh` CLI thì **không**. Cách kiểm tra không lộ secret:
  `printf 'protocol=https\nhost=github.com\n\n' | git credential fill` rồi
  `curl -sS -o /dev/null -D - -H "Authorization: token $TOKEN" https://api.github.com/user | grep x-oauth-scopes`.
- **Actions trên repo private tốn phút** (Free: 2.000 phút/tháng): 2 run đỏ chỉ tốn 1m12s và 35s vì `build-image`
  bị `skipped`; nếu 3 job chạy hết thì mỗi run ~2m35s. Ràng buộc `needs:` vừa tiết kiệm phút vừa đúng ý
  ("không build image cho code đã fail").
- **Đừng chạy 2 lệnh git song song** (lỗi tự gây trong ticket này): 2 lệnh `git commit` cùng lúc → tranh chấp
  `index.lock`, một commit bị mất (`fatal: Unable to create .git/index.lock`). Git luôn phải chạy tuần tự.
- **Badge trên repo private**: SVG chỉ render khi trình duyệt đã login GitHub → người xem ẩn danh thấy ảnh vỡ
  (không phải CI hỏng). Muốn badge công khai thì phải đổi repo sang public, hoặc chấp nhận badge chỉ để tự xem.

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




