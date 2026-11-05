# AGENTS.md — Working agreement cho repo ShopLab

> Đây là "hợp đồng làm việc" giữa **bạn (human)** và **AI agent (Cline)** trong repo này.
> Đọc file này trước khi làm bất cứ việc gì. Mọi thay đổi về cách làm việc đều update vào đây.

---

## 1. Repo này là gì

Repo này là một **learning lab** để học **microservices architecture** và **system design** ở quy mô
**high traffic**, lấy domain **e-commerce** làm bài toán.

- Mục tiêu là hiểu **tại sao** một pattern tồn tại, dùng khi nào, **trade-off** ra sao — không phải
  chỉ "chạy được là xong".
- Đây **không phải** sản phẩm thật. Ưu tiên **học đúng** hơn **ship nhanh**.
- Người thực hiện: **bạn + AI**. Không chia role kiểu Lead / DevOps / QA / BA riêng — cả hai cùng
  làm từ design → code → test → deploy.

Tài liệu nguồn (source of truth):

| File | Nội dung |
|---|---|
| `architecture.md` | Kiến trúc tổng quát, service map, pattern, roadmap, tech stack |
| `Backlog.md` | Danh sách ticket **đang làm / sắp làm** chia theo phase, thực hiện cuốn chiếu |
| `Backlog-phase-<N>.md` | **Archive** chi tiết các phase đã xong (giữ nguyên verify + learning note để tra cứu) |
| `AGENTS.md` | File này — cách làm việc |
| `docs/adr/` | Architecture Decision Record cho các quyết định lớn |

---

## 2. Ngôn ngữ & văn phong

| Quy tắc | Chi tiết |
|---|---|
| Ngôn ngữ tài liệu | **Tiếng Việt** |
| Thuật ngữ kỹ thuật | **Giữ nguyên tiếng Anh**, không dịch |
| Code, tên biến, tên service, commit message | **Tiếng Anh** |
| Văn phong | Như anh em dev Việt Nam nói chuyện: thẳng, ngắn, thực dụng. Ưu tiên câu ngắn, bullet, bảng. Tránh văn phong hành chính kiểu dịch máy. |
| Ví dụ | Luôn có ví dụ cụ thể (số liệu, request flow, đoạn code, lệnh chạy) thay vì nói chung chung |

**Keyword giữ tiếng Anh** (ví dụ, không giới hạn): `load balancing`, `trade-off`, `throughput`,
`latency`, `caching`, `cache stampede`, `idempotency`, `saga`, `outbox`, `backpressure`,
`circuit breaker`, `rate limit`, `sharding`, `read replica`, `eventual consistency`,
`dead letter queue`, `graceful degradation`, `observability`, `SLO / error budget`,
`bounded context`, `vertical slice`.

**Viết đúng:**

> Cache-aside ở đây giúp giảm read xuống DB, nhưng phải xử lý **cache stampede** khi key hot hết TTL
> cùng lúc — nếu không thì DB lại bị dồn request đúng lúc nó đang yếu nhất.

**Viết sai:**

> Chúng tôi áp dụng cơ chế lưu trữ đệm nhằm tối ưu hoá hiệu năng truy vấn dữ liệu.

---

## 3. Nguyên tắc làm việc (BẮT BUỘC)

### 3.1. Làm cuốn chiếu, tuyệt đối không làm một loạt

- Mỗi lượt chỉ làm **1 ticket** trong `Backlog.md` (hoặc 1 nhóm ticket rất nhỏ cùng phase).
- **Không** scaffold nhiều service / nhiều feature trong 1 lần rồi để đó "chạy sau".
- **Không code trước ticket.** Muốn làm gì → tạo/sửa ticket trước, chốt nội dung, rồi mới code.
- Sau mỗi ticket: chạy được + có test + update `Backlog.md` + ghi lại cái học được.

### 3.2. Đi theo vertical slice, không đi ngang

- Xong 1 tính năng phải **end-to-end chạy được**: client → gateway → service → DB → response.
- Không kiểu "hôm nay code xong tầng repository của 6 service".

### 3.3. Mọi quyết định kiến trúc phải có lý do

- Chọn giải pháp nào phải nói rõ: **tại sao**, **trade-off**, **khi nào nên revisit**.
- Thay đổi lớn (thêm service, đổi broker, đổi DB, đổi protocol) → viết ADR vào `docs/adr/`
  + update `architecture.md`.

### 3.4. Không over-engineer

- Không thêm hạ tầng (Kafka, K8s, service mesh, multi-region...) khi phase hiện tại chưa cần.
- Chỉ dựng đủ để chứng minh bài học của phase đó. Pattern phải xuất phát từ **vấn đề thật**,
  không phải "thấy hay nên thêm".

---

## 4. Quy trình cho mỗi ticket

1. **Read** — đọc ticket trong `Backlog.md` + phần liên quan trong `architecture.md`.
2. **Plan** — tóm tắt lại cách hiểu, list các bước, nêu **giả định** và **câu hỏi mở**. Nếu có điểm
   mơ hồ ảnh hưởng tới design → **hỏi trước, không đoán**.
3. **Build** — code theo convention ở §6.
4. **Verify** — chạy test / `docker compose` / `curl` thật. **Dán kết quả thật**, không nói suông.
5. **Document** — update `Backlog.md` (status + learning note); update `architecture.md` nếu design đổi.
6. **Report** — summary ngắn: đã làm gì, file nào, lệnh nào để verify, phần chưa làm / technical debt.

---

## 5. Definition of Ready / Definition of Done

**Definition of Ready (DoR)** — ticket chỉ được bắt đầu khi có đủ:

- [ ] Bối cảnh & mục tiêu (why) — hiểu vì sao làm
- [ ] Scope rõ: in scope / out of scope
- [ ] Acceptance Criteria dạng checklist, verify được
- [ ] Phụ thuộc: ticket nào phải xong trước
- [ ] Ghi chú kỹ thuật: pattern sẽ dùng + trade-off chấp nhận

**Definition of Done (DoD):**

- [ ] Code chạy được, pass lint + format
- [ ] Có test tự động (unit bắt buộc; integration nếu có I/O)
- [ ] Đã verify bằng lệnh thật và ghi lại kết quả trong ticket
- [ ] Không hard-code secret / connection string; config qua env
- [ ] `Backlog.md` đã update status + learning note
- [ ] Doc/README liên quan đã update nếu cách chạy thay đổi

**Definition of Done của một PHASE** (khi ticket cuối của phase xong — ADR-0005):

- [ ] Tất cả ticket của phase `Done` · tách chi tiết sang `Backlog-phase-<N>.md`
- [ ] Viết `docs/phases/phase-N.md` (stack · có gì · cách chạy **ở tag đó** · số liệu · **cái gì chưa có** · bài học)
- [ ] Verify `docker compose up` **tại snapshot** chạy được (dán lệnh + output)
- [ ] Tag annotated `phase-N` trên **merge commit đóng phase** + **GitHub Release** (notes tái dùng `Backlog-phase-N.md`)
- [ ] Cập nhật `docs/phases/README.md` (index) + bảng "Archive các phase đã xong" trong `Backlog.md`
- [ ] `architecture.md` §12 đánh dấu phase đã xong (nếu roadmap thay đổi)

---

## 6. Convention code

Chi tiết ngôn ngữ/framework xem `architecture.md` §11. Quy ước chung:

- **Commit message**: Conventional Commits — `feat|fix|docs|refactor|test|chore(scope): mô tả`
  ví dụ `feat(order): add saga orchestration for checkout`.
- **Branch**: `<type>/<ticket-id>-slug` — ví dụ `feat/sl-2.4-order-saga`, `fix/sl-3.1-...`.
- **Git workflow**: **GitHub Flow** (base `main` + nhánh ngắn hạn + PR). **Commit sau khi hoàn thành mỗi ticket**
  (sau khi verify thật). Chi tiết: `docs/git-flow.md` + rule `.clinerules/10-git-workflow.md`.
  - **Commit chỉ sửa docs** (`Backlog.md`, `README.md`, `docs/**`, `.clinerules/**`) → thêm **`[skip ci]`**: không
    cần CI. PR chỉ-docs và push docs-only lên `main` đã được `paths-ignore` bỏ sẵn.
  - **Merge tự động**: ticket `Done` + **CI xanh** + **có comment tóm tắt** trên PR → `bash tools/merge-pr.sh <PR>
    --summary-file <file>` (squash + xoá nhánh feature). Sau merge **phải kiểm `main` có nội dung thật**.
- **Commit minor — KHÔNG tạo nhánh/PR riêng**: commit nhỏ (đóng ticket, update `Backlog.md`/learning note, sửa doc
  lặt vặt, tách `Backlog-phase-<N>.md`) **gộp vào nhánh của ticket kế tiếp** — cùng một PR.
  Chỉ tạo nhánh + PR riêng khi **không còn ticket kế tiếp** (hết phase / dừng dự án).
  ⚠️ Thay đổi **có code** thì **không** được gộp — phải là ticket riêng (§3.1).
- **Archive theo phase**: phase xong → tách phần chi tiết (ticket + verify + learning note) sang
  `Backlog-phase-<N>.md`; `Backlog.md` giữ việc đang làm + bảng archive + Learning log + Changelog.
- **Folder**: service dùng `kebab-case` — `order-service`, `api-gateway`.
- **Config**: 12-factor — đọc từ env, `.env.example` commit lên, `.env` **không** commit.
- **Log**: structured JSON, luôn có `traceId`, `service`, `level`, `msg`.
- **API**: REST version qua path `/v1/...`, contract mô tả bằng OpenAPI.
- **Không** share database giữa các service. **Không** import code của service này vào service khác
  (trừ shared library hạ tầng, đặt ở `packages/`).

---

## 7. Convention ticket trong `Backlog.md`

- **ID**: `SL-<phase>.<số>` — ví dụ `SL-0.1`, `SL-2.4`. Không đổi ID sau khi tạo.
- **Trạng thái**: `Todo` → `In Progress` → `Review` → `Done` (hoặc `Blocked` + lý do).
- **Ước lượng**: `S` (< nửa ngày) / `M` (1–2 ngày) / `L` (3–5 ngày). Nếu `L` → **phải tách nhỏ**.
- Viết như **BA đã define xong**: mô tả được, verify được, không còn chỗ mơ hồ.
- Mỗi ticket gồm: Bối cảnh → Mục tiêu → In scope → Out of scope → Acceptance Criteria →
  Ghi chú kỹ thuật → Kết quả verify (điền khi Done) → Learning note.
- Ticket ở phase xa chỉ cần mức **epic**; chỉ breakdown thành ticket chi tiết khi tới lượt.

---

## 8. AI agent — cách trả lời

- Trả lời **tiếng Việt**, giữ keyword tiếng Anh. Không lặp lại yêu cầu của user.
- Ngắn gọn, ưu tiên bullet / bảng. Không lan man.
- Luôn nêu **giả định** khi thiếu thông tin, thay vì im lặng chọn bừa.
- Có nhiều phương án → so sánh bằng bảng **Ưu / Nhược / Khi nào dùng**, rồi mới recommend.
- **Không bịa**: không bịa API, version, benchmark. Không claim "đã test" khi chưa chạy.
- Khi sửa nhiều file → list rõ file nào, sửa gì, vì sao.
- Cuối mỗi task → tóm tắt + lệnh để user tự verify.

---

## 9. Những việc KHÔNG được làm

- ❌ Build một loạt service/feature trong 1 lượt
- ❌ Thêm hạ tầng/công nghệ ngoài scope của phase hiện tại
- ❌ Đổi kiến trúc mà không update `architecture.md` + viết ADR
- ❌ Commit secret, token, connection string thật
- ❌ Đánh dấu ticket `Done` khi chưa chạy verify thật
- ❌ Merge PR khi CI chưa xanh hoặc chưa có comment tóm tắt (dùng `tools/merge-pr.sh` để không bỏ sót)
- ❌ Viết doc dài dòng, không có ví dụ / số liệu cụ thể
- ❌ Tự ý đổi scope của ticket đang làm — có gì mới thì tạo ticket mới

---

## 10. AI assets (rules / skills / workflows)

Bộ công cụ giúp Cline làm việc nhất quán, tiết kiệm token. **File này vẫn là nguồn sự thật.**

| Loại | Vị trí | Vai trò |
|---|---|---|
| Rule | `AGENTS.md` | Working agreement — luôn active |
| Rule | `.clinerules/00-context.md` | Nạp context (Memory Bank lite), liệt kê AI assets |
| Rule | `.clinerules/10-git-workflow.md` | Git workflow: commit sau mỗi ticket, nhánh, PR |
| Skill | `.cline/skills/shoplab-adr` | Cách viết ADR (template + quality bar) |
| Skill | `.cline/skills/shoplab-ticket` | Format ticket BA-style + DoR/DoD |
| Skill | `.cline/skills/shoplab-verify` | Verify bằng lệnh thật + dán output |
| Workflow | `.clinerules/workflows/run-ticket.md` | `/run-ticket.md` — thực thi 1 ticket |
| Workflow | `.clinerules/workflows/new-ticket.md` | `/new-ticket.md` — breakdown epic |
| Workflow | `.clinerules/workflows/close-ticket.md` | `/close-ticket.md` — đóng ticket |
| Workflow | `.clinerules/workflows/git-branch-pr.md` | `/git-branch-pr.md` — tách nhánh + tạo PR |

Quy ước:

- **Rule** = behavioral, luôn active. **Skill** = kiến thức load on-demand. **Workflow** = quy trình
  nhiều bước, gọi bằng `/filename.md` (chỉ tốn token khi invoke).
- **Memory Bank lite**: dùng `architecture.md` + `Backlog.md`, **không** tạo `memory-bank/` riêng.
- **Không** dùng Cline Hooks/Plugins trên VS Code (chỉ hỗ trợ SDK/CLI/Kanban). Git hooks → `SL-0.2`.
- `.cline/` được commit vào repo nhưng **không** chứa secret/API key.
- Thêm skill mới đặt tên `shoplab-*`; thêm workflow đặt tên `<verb>-<object>.md` — nhớ update bảng trên.


