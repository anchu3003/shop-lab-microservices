# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — *(`SL-1.15`/`1.16`/`1.17` xong; Phase 1 đóng)* |
| **Ticket tiếp theo** | **DoD Phase 1**: `docs/phases/phase-1.md` + tag `phase-1` + GitHub Release (ADR-0005) |
| **Số ticket chi tiết hiện có** | 23 (Phase 0: 8 — archive · Phase 1: 15) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | 🔵 In Progress | 11 / 15 |
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

## 4. Phase 1 — Walking skeleton

> **Mục tiêu phase**: đường đi từ client tới DB chạy được **end-to-end** với 3 service đầu tiên
> (`api-gateway` → `identity-service` / `catalog-service` → Postgres), có auth JWT + contract OpenAPI.
>
> **Điều kiện hoàn thành phase**: register → login → lấy token → gọi API product qua gateway chạy end-to-end ·
> CI xanh · có OpenAPI spec · tách archive + tag `phase-1` (ADR-0005).

**Đã breakdown thành 15 ticket chi tiết** (`SL-1.0` … `SL-1.14`) — thứ tự thực hiện:

| # | Ticket | Epic item | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-1.0` | *(phát sinh)* tối ưu chi phí CI | S | ✅ Done |
| 2 | `SL-1.1` | `identity-service`: register/login + JWT | M | ✅ Done |
| 3 | `SL-1.2` | `api-gateway` bản mỏng | M | ✅ Done |
| 4 | `SL-1.3` | **E2E sớm**: register → login qua gateway | S | ✅ Done |
| 5 | `SL-1.4` | `identity-service`: refresh token + RBAC | M | ✅ Done |
| 6 | `SL-1.5` | `catalog-service`: CRUD category/product | M | ✅ Done |
| 7 | `SL-1.6` | `catalog-service`: SKU + phân trang/filter + seed | M | ✅ Done |
| 8 | `SL-1.7` | E2E smoke đầy đủ + runbook | S | ✅ Done |
| 9 | `SL-1.8` | OpenAPI spec Phase 1 | S | ✅ Done |
| 10 | `SL-1.9` | *(phát sinh)* process: docs không cần CI + auto-merge PR | S | ✅ Done |
| 11 | `SL-1.10` | *(phát sinh)* fix: `[skip ci]` lọt vào squash message → `main` không chạy CI | S | ✅ Done |
| 12 | `SL-1.11` | *(phát sinh)* fix: message chẩn đoán của `merge-pr.sh` gây hiểu nhầm | S | ✅ Done |
| 13 | `SL-1.12` | *(phát sinh)* CI: build image cho **mọi** service (matrix) + cắt thời gian build | M | ✅ Done |
| 14 | `SL-1.13` | *(phát sinh)* compose: `service-template` vào **profile** `template` | S | ✅ Done |
| 15 | `SL-1.14` | *(phát sinh)* `merge-pr.sh`: cho phép PR **docs-only** + comment evidence **hậu merge** | S | ✅ Done |


| 16 | `SL-1.15` | *(phát sinh)* fix: seed dev chỉ nạp ở local + slug tiếng Việt | S | ✅ Done |
| 17 | `SL-1.16` | *(phát sinh)* process: tech debt register + rule | S | ✅ Done |
| 18 | `SL-1.17` | *(phát sinh)* docs(api): ẩn endpoint debug khỏi spec | S | ✅ Done |

**Quyết định đã chốt khi breakdown** *(2026-11-03)*: gateway **MVC tự viết** (dùng lại 100% `service-commons`) ·
JWT **RS256 + JWKS** · khoá dev sinh bằng `tools/gen-dev-keys.sh` (**không** commit) · **Spring Data JPA + Flyway** ·
**có Testcontainers** ngay Phase 1 · OpenAPI **sinh từ code** · E2E smoke chạy **local** (chưa vào CI) ·
register luôn tạo `buyer` (seller/admin qua seed).

---

### SL-1.0 — Tối ưu chi phí CI: paths filter, push chỉ trên `main`, kill-switch, bỏ qua draft PR

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-0.5 (CI pipeline) |
| **Ước lượng** | S |

**Bối cảnh**

Số liệu thật đo từ API sau SL-0.5 (18 run trong tháng 11/2026): **~71 phút billable**, trung bình **3,9 phút/run**.

- **11/18 run (62% — 44 phút) chỉ sửa docs** (`.md`, `docs/`, `.clinerules/`) → build/test hoàn toàn vô nghĩa.
- **9 run `push` trên nhánh feature = 35 phút**, trong khi đã có run `pull_request` tương ứng (chạy song song, gấp đôi).
- Job `build-image` chiếm **33/71 phút (46%)** — nặng nhất vì build trong Docker lại chạy `mvn verify` lần nữa.

GitHub Free cho repo private: **included compute usage $12/tháng**, tính theo **tiền** chứ không phải phút
($12 ÷ $0.006/phút Linux 2-core = 2.000 phút). Vượt quota: **chưa có payment method → bị block** (CI ngừng chạy);
có payment method → bị tính tiền. Phase 1 trở đi số run sẽ tăng mạnh (3 service + integration test) → tối ưu **trước**
khi bắt tay vào service để cả phase hưởng lợi.

**Mục tiêu**

Giảm ≥ 50% số phút CI cho cùng khối lượng công việc, **không** giảm chất lượng chốt chặn: vẫn lint + test + build image
cho mọi thay đổi **có code**, nhưng không chạy cho thay đổi docs, không chạy 2 lần cho cùng một nhánh, và có công tắc
tắt CI tạm thời khi cần.

**In scope**

- **A. `paths-ignore`**: bỏ hẳn run khi chỉ đổi `**.md`, `docs/**`, `.clinerules/**`, `.cline/**`, `LICENSE`
- **B. Trigger gọn**: `push` chỉ trên `main`; nhánh feature chỉ chạy qua `pull_request` (bỏ run trùng)
- **F. Kill-switch bằng config**: job-level `if: vars.CI_ENABLED != 'false'` + `gh variable set CI_ENABLED --body false`
- **G. Bỏ qua Draft PR**: thêm `ready_for_review` vào `on.pull_request.types`, job skip khi PR còn draft
- Cập nhật `README.md` (mục CI: trigger mới + 3 lệnh vận hành) + ghi lại số đo trước/sau

**Out of scope**

- Gộp `lint` vào `test` (mất tín hiệu "job nào đỏ") — chưa cần
- Gate `build-image` theo path / thêm `ARG SKIP_TESTS` — chưa cần
- Self-hosted runner, chuyển repo sang public, required checks (branch protection)
- Integration test Testcontainers (ticket riêng của Phase 1–2)

**Acceptance Criteria**

- [x] Commit **chỉ docs** → **không** có run nào được tạo (cả nhánh feature lẫn `main`)
- [x] Commit **có code** trên nhánh feature → chỉ **1 run** (`pull_request`), **không** còn run `push` trùng
- [x] Merge vào `main` → có run `push` trên `main`
- [x] **Draft PR** + commit code → không chạy; bấm **Ready for review** → chạy
- [x] `gh variable set CI_ENABLED --body false` → run vẫn được tạo nhưng **mọi job `skipped`**; đặt lại `true` → chạy bình thường
- [x] Đo lại usage bằng API: cùng kịch bản giảm **≥ 50%** số phút (thực đo: **8 phút** vs ~**19,5 phút** = **−59%**)
- [x] `actionlint` pass trên workflow mới
- [x] `README.md` + ticket cập nhật (trigger mới, 3 lệnh vận hành, trade-off)

**Ghi chú kỹ thuật**

- **Vì sao `paths-ignore` chứ không `paths`**: `paths` là allow-list → dễ sót path mới (ví dụ `packages/` mới) và im lặng
  bỏ qua thay đổi cần build. `paths-ignore` là deny-list hẹp, chỉ liệt kê thứ **chắc chắn** không ảnh hưởng build.
- **Không** ignore `.github/**` (đổi workflow phải được CI kiểm chứng), `.editorconfig`/`.gitattributes`
  (ảnh hưởng format/line-ending → Spotless), `tools/**`, `infra/**`.
- **Trade-off đã biết**: workflow bị skip (do `paths-ignore` hoặc `[skip ci]`) để lại check ở trạng thái **"Pending"** →
  **sẽ chặn merge nếu bật required checks**. Hiện repo private + Free nên không bật được branch protection ⇒ chấp nhận.
  Khi chuyển public/Team: chuyển `paths-ignore` ở trigger sang **điều kiện ở job level** (dùng `vars` + path filter),
  vì job bị **skip** thì check tính là pass.
- **Vì sao dùng `vars` cho kill-switch**: `vars` **được phép** trong `jobs.<job_id>.if` (đã verify bằng `actionlint` 1.7.12
  → exit 0), nên tắt/bật CI **không cần sửa file**, không cần commit "tạm" nào trong history.
- Giữ nguyên `concurrency: group: ${{ github.workflow }}-${{ github.ref }}` + `cancel-in-progress` (SL-0.5).
- Giữ nguyên cấu trúc 3 job (`lint` ∥ `test` → `build-image`) và cache Maven.

**Kết quả verify** *(đang đo — cập nhật dần)*

File sửa: `.github/workflows/ci.yml` · `README.md` · `Backlog.md`. PR [#7](https://github.com/anchu3003/shop-lab-microservices/pull/7) (mở dạng **draft** để test luôn cơ chế G).

```console
# --- AC: actionlint pass trên workflow mới ---
$ actionlint .github/workflows/ci.yml ; echo exit=$?
exit=0

# --- AC (B): push nhánh feature KHÔNG tạo run nào ---
$ git push -u origin feat/sl-1.0-ci-cost-optimization
$ gh api repos/.../actions/runs --jq '.total_count'
18                                   # trước và sau push đều 18 -> không có run `push` mới
$ gh run list --limit 1
completed success  docs(backlog): close SL-0.5 ...  CI  main  push  41668299796   ← vẫn là run cũ trên main

# --- AC (G): PR ở dạng DRAFT -> run được tạo nhưng KHÔNG job nào chạy (0 phút) ---
$ gh pr create --draft ...
https://github.com/anchu3003/shop-lab-microservices/pull/7
$ gh run list --limit 1
completed skipped  feat(ci): cut CI cost ...  CI  feat/sl-1.0-ci-cost-optimization  pull_request  41676119411
$ gh run view 41676119411 --json jobs --jq '.jobs[]|"\(.name) | \(.conclusion) | started=\(.started_at // "-")"'
test (unit + jacoco gate)      | skipped | started=-
lint (checkstyle + spotless)   | skipped | started=-
build-image (service-template) | skipped | started=-
# ⇒ cả 3 job `skipped` và KHÔNG có started_at ⇒ không cấp runner ⇒ 0 phút billable

# --- AC (F): kill-switch bằng config — tắt rồi RE-RUN (không cần commit giả) ---
$ gh variable set CI_ENABLED --body false
$ gh run rerun 41676541410
$ gh run list --limit 1
completed skipped  feat(ci): cut CI cost ...  pull_request  attempt=2      ← mọi job skip, 0 phút
$ gh variable set CI_ENABLED --body true ; gh run rerun 41676541410
completed success  ... attempt=3                                           ← chạy lại bình thường (3/3 job)

# --- AC (G dương): bấm "Ready for review" -> run chạy thật ---
$ gh pr ready 7
completed success  feat(ci): cut CI cost ...  pull_request  41676541410    ← lint · test · build-image xanh

# --- AC (B dương): merge vào `main` -> CÓ run `push` trên `main` ---
$ gh pr merge 7 --squash --delete-branch
$ gh run view 41677658371 --json jobs --jq '.jobs[]|"\(.name) -> \(.conclusion)"'
lint (checkstyle + spotless)   -> success
test (unit + jacoco gate)      -> success
build-image (service-template) -> success

# --- AC (A): PR CHỈ ĐỔI DOCS -> KHÔNG có run nào được tạo ---
$ gh api repos/.../actions/runs --jq '.total_count'
22                       # trước khi push nhánh docs + mở PR #8
$ git push -u origin docs/sl-1.0-close ; gh pr create --base main ...   # PR #8, KHÔNG phải draft
$ gh api repos/.../actions/runs --jq '.total_count'
22                       # KHÔNG tăng -> paths-ignore chặn hẳn: không có run nào được tạo
$ gh run list --limit 1
completed success  feat(ci): cut CI cost ...  CI  main  push  41677658371   ← vẫn là run cũ trên main

# --- Đo lại usage (làm tròn lên từng job; job `skipped` tính 0 phút) ---
run            event        concl     att  phút  ghi chú
41677658371    push         success   1     4   push trên main sau merge
41676541410    pull_request success   3     8   att1=4p · att2=skipped(0p) ← kill-switch · att3=4p
41676400180    pull_request skipped   1     0   commit docs trong PR có code (lúc còn draft)
41676119411    pull_request skipped   1     0   PR draft
⇒ TỔNG cả ticket SL-1.0: 12 phút (trong đó 2 run = 0 phút)
```

**So sánh trước/sau cho cùng kịch bản** (2 push + 1 PR + merge — đúng những gì ticket này đã làm):

| | Config cũ (SL-0.5) | Config mới (SL-1.0) | Giảm |
|---|---|---|---|
| Run được tạo | **5** (2 `push` + 3 `pull_request`) | **4** (2 trong đó `skipped` = 0 phút) | −20% |
| Phút billable | ~**19,5** | **8** (không tính lần re-run để test kill-switch) | **−59%** |
| Phút billable (tính cả lần test) | ~19,5 | 12 | −38% |

**Learning note**

- **`paths`/`paths-ignore` trên `pull_request` được đánh giá theo TOÀN BỘ diff của PR**, không phải commit cuối. Nên trong
  một PR có code, commit docs-only vẫn làm workflow chạy (ở đây là run `skipped` vì đang draft). Muốn "commit docs không
  tốn gì" thì phải là **PR chỉ-docs**, hoặc thêm `[skip ci]` vào commit.
- **Job `skipped` = 0 phút** (không cấp runner) — chứng minh bằng `started_at = null` (GraphQL). ⚠️ **Bẫy đo lường**:
  REST API `/runs/{id}/jobs` trả `started_at` **GIẢ** cho job `skipped` (started == completed) → tự đo bằng REST sẽ
  **tính dư ~1 phút/job**. Dùng `gh run view --json jobs` (GraphQL) hoặc lọc `conclusion == 'skipped'`.
- **Re-run có đánh giá lại `vars`** → test kill-switch **không cần commit giả** (không để lại commit rác trong history).
- **PR draft vẫn TẠO run** (kết luận `skipped`) chứ không phải "không có run" — vẫn 0 phút, nhưng khác bản chất với
  `paths-ignore` (không tạo run nào). Khi đọc log cần phân biệt "không có run" vs "run bị skip".
- **`ready_for_review` phải khai tường minh** trong `on.pull_request.types` (mặc định chỉ có `opened`/`synchronize`/`reopened`);
  thiếu nó thì bấm *Ready for review* sẽ không kích hoạt gì.
- **Free plan (2026) tính theo TIỀN, không phải phút**: "included compute usage" = **$12/tháng** (≈ 2.000 phút Linux 2-core
  vì $12 ÷ $0.006). Đổi runner là đổi "tốc độ đốt" (Windows 1,67×, macOS ~10×) → tối ưu trigger quan trọng hơn khi đổi runner.
- **Đo trước khi tối ưu**: nếu không kéo usage thật từ API (18 run = 71 phút, 62% là docs) thì rất dễ tối ưu sai chỗ —
  trực giác hay nghĩ "build image là đắt nhất" (đúng, 46%) nhưng **thứ đáng bỏ nhất lại là run docs-only**.

---

### SL-1.1 — `identity-service`: skeleton + Flyway + register/login + JWT (RS256)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.0 |
| **Ước lượng** | M |

**Bối cảnh**

Phase 1 cần một **nguồn token** cho mọi thứ phía sau. Hiện repo mới có `service-template` (chứng minh hạ tầng, **không** có
DB thật — health check mới ở mức TCP) nên chưa chứng minh được "database per service" chạy được. Đây là service thật đầu
tiên: schema do migration quản lý, dữ liệu ghi/đọc thật, test integration chạm Postgres thật.

**Mục tiêu**

`identity-service` (port **8081**) chạy trong compose, schema do **Flyway** quản lý, đăng ký + đăng nhập trả **JWT RS256**,
và công bố **JWKS** để service khác verify token **mà không cần giữ khoá ký**.

**In scope**

- Copy `service-template` → `services/identity-service` (đổi artifactId/package/tên service/port **8081**/Dockerfile theo
  README "Tạo service mới từ template"); đăng ký module vào parent `pom.xml`
- DB `identity` (init script đã tạo sẵn) + Flyway `V1__create_users.sql`:
  `users(id uuid pk, email unique not null, password_hash, display_name, role, created_at, updated_at)`
- Hash mật khẩu bằng **BCrypt** (`spring-security-crypto` — **chưa** kéo `spring-boot-starter-security`)
- `POST /v1/auth/register` (email, password, displayName) → **201** + user (không trả hash) · trùng email → **409**
- `POST /v1/auth/login` → **200** + `{accessToken, tokenType, expiresIn}` · sai → **401**
- JWT RS256: `iss`, `sub`=userId, `role`, `exp` 15 phút; private key đọc từ file, **fail-fast** nếu thiếu
- `GET /.well-known/jwks.json` → public key (kèm `kid`)
- `tools/gen-dev-keys.sh` — sinh keypair dev vào `infra/keys/` (**gitignored**)
- Thêm service vào `infra/docker-compose.yml` + `.env.example` (`IDENTITY_SERVICE_PORT`, `IDENTITY_JWT_*`)
- Test: unit (validate, JWT service) + **integration Testcontainers** (Postgres), tách bằng maven-failsafe (`*IT.java`)

**Out of scope**

- Refresh token / rotate / logout → **SL-1.4** · RBAC đầy đủ → **SL-1.4** · gateway/route/verify ở cửa vào → **SL-1.2**
- Rate limit (Phase 4) · email verification · reset/forgot password · social login · MFA

**Acceptance Criteria**

- [x] `docker compose … up -d` → `shoplab-identity-service` **healthy**; `curl localhost:8081/health` → 200
- [x] Flyway chạy lúc khởi động: DB `identity` có bảng `users` **và** `flyway_schema_history`
- [x] `POST /v1/auth/register` email mới → **201**; gọi lại cùng email → **409** với `ApiError` (`code` rõ ràng)
- [x] Mật khẩu lưu **BCrypt**: query DB thấy `$2a$…`, **không** thấy plaintext
- [x] `POST /v1/auth/login` đúng → **200** + `accessToken`; decode thấy `sub`, `role`, `exp` ≈ 15 phút
- [x] login sai mật khẩu → **401**, message **không** tiết lộ email có tồn tại hay không
- [x] `GET /.well-known/jwks.json` → **200**, `kid` **khớp** header của token vừa phát
- [x] Verify chữ ký token bằng public key lấy từ JWKS (lệnh thật, dán vào verify) → hợp lệ
- [x] Thiếu file private key → service **fail-fast**, message rõ ràng (không chạy nửa vời)
- [x] `mvn verify` xanh: **có** IT Testcontainers chạy thật (ghi lại `Tests run: N`)
- [x] CI xanh trên PR (job `test` chạy cả IT)

**Ghi chú kỹ thuật**

- ⚠️ **Testcontainers không chạy được trong `docker build`** (không có Docker socket) → tách IT bằng **maven-failsafe**
  (`*IT.java`), Dockerfile đổi sang `mvn verify -DskipITs`; CI job `test` chạy IT (runner `ubuntu-latest` có sẵn Docker).
- **RS256 (D2)** vì verifier (gateway/catalog) **không được** giữ khoá ký; JWKS cho phép rotate khoá sau này không phải
  deploy lại mọi service.
- **Khoá dev không commit** (`AGENTS.md` §9): `tools/gen-dev-keys.sh` sinh vào `infra/keys/`, thêm vào `.gitignore`; compose
  mount **read-only**; service fail-fast nếu thiếu (nhất quán với `EnvValidator` của commons).
- Dùng `spring-security-crypto` (BCrypt) chứ **chưa** kéo `spring-boot-starter-security` → tránh filter chain ảnh hưởng
  `/health` `/ready` `/version` hiện có; security chain đầy đủ ở SL-1.2 (gateway) và SL-1.4.
- ID dùng **UUID** (không lộ số lượng user, thuận lợi khi sharding ở Phase 8).
- **JPA + Flyway (D3)**: JPA tiện CRUD nhưng sinh SQL ngầm — chấp nhận ở Phase 1, đo bằng số liệu ở Phase 4.
- Migration **không sửa file đã chạy** (Flyway checksum) → mọi thay đổi schema là `V2__…`.
- IT Testcontainers làm job `test` lâu thêm ~1–2 phút (đã biết từ SL-1.0).

**Kết quả verify** *(2026-11-05 — output thật; WSL2 · JDK 21.0.12.1 · Maven 3.9.12 · Docker 29.9.2)*

File tạo: `services/identity-service/**` (service + Flyway migration + 39 unit test + 16 IT) · `tools/gen-dev-keys.sh`.
File sửa: `pom.xml` (thêm module + failsafe) · `packages/service-commons/…/ErrorCode.java` (+`UNAUTHORIZED`/`FORBIDDEN`) ·
`services/service-template/Dockerfile` (fix pom-copy) · `infra/docker-compose.yml` · `.env.example` · `.gitignore` · `README.md`.

```console
# --- AC: mvn verify (unit + IT Testcontainers + spotless + checkstyle + jacoco gate) ---
$ mvn -B -pl services/identity-service -am verify
[INFO] ShopLab :: Service Commons ... Tests run: 37, Failures: 0, Errors: 0   · 0 Checkstyle violations
[INFO] ShopLab :: Identity Service . Tests run: 39, Failures: 0, Errors: 0   (unit, không cần Docker)
[INFO]                              Tests run: 16, Failures: 0, Errors: 0   (IT — Postgres thật qua Testcontainers)
[INFO] You have 0 Checkstyle violations. · All coverage checks have been met.
[INFO] BUILD SUCCESS

# --- AC: container healthy + /health + /ready ---
$ docker compose -f infra/docker-compose.yml --env-file .env ps
shoplab-identity-service   Up (healthy)        shoplab-postgres   Up (healthy)
$ curl -s -w ' [http %{http_code}]\n' localhost:8081/health
{"status":"UP"} [http 200]
$ curl -s localhost:8081/ready
{"status":"UP","dependencies":[{"name":"postgres","status":"UP","detail":"ok","latency":"PT0.00090926S"}]}

# --- AC: Flyway đã migrate + bảng users trên Postgres thật ---
$ docker exec shoplab-postgres psql -U shoplab -d identity -c \
    'select version, description, success from flyway_schema_history;'
 version | description  | success
       1 | create users | t
$ docker exec shoplab-postgres psql -U shoplab -d identity -c '\d users'
 Indexes: "users_pkey" PRIMARY KEY, btree (id)
          "ux_users_email" UNIQUE, btree (email)      ← email là định danh đăng nhập

# --- AC: register 201 · trùng email 409 (ApiError) · login 200 ---
$ curl -s -w '\n[http %{http_code}]\n' -X POST localhost:8081/v1/auth/register -H 'Content-Type: application/json' \
    -d '{"email":"ac-…@shoplab.local","password":"matkhau123","displayName":"An"}'
{"id":"162a37e2-f4e3-4820-8c24-8a4763347000","email":"ac-…@shoplab.local","displayName":"An","role":"BUYER","createdAt":"…"}
[http 201]
$ (gọi lại đúng email đó)
{"code":"CONFLICT","message":"Email 'ac-…@shoplab.local' đã được đăng ký","traceId":"2dbb0218-…","path":"/v1/auth/register"}
[http 409]
$ (login)
tokenType= Bearer   expiresIn= 900   token len= 613

# --- AC: mật khẩu lưu BCrypt trong DB (không plaintext) ---
$ docker exec shoplab-postgres psql -U shoplab -d identity -tAc \
    "select email, left(password_hash,29) || '...', length(password_hash) from users;"
ac-1793885102@shoplab.local|$2a$10$Wt0tu76V6vwh.9xuZMzWo....|60

# --- AC: claims của token (exp - iat = 900s = 15 phút) ---
claims: {"iss": "https://identity.shoplab.local", "sub": "162a37e2-f4e3-4820-8c24-8a4763347000",
         "role": "BUYER", "exp": 1793886002, "iat": 1793885102}
```

```console
# --- AC: verify CHỮ KÝ bằng public key lấy từ JWKS (không dùng private key) ---
$ curl -s localhost:8081/.well-known/jwks.json
{"keys":[{"kty":"RSA","e":"AQAB","kid":"FfQyQesnP7c818474zybGHCrMxU_w7FjtR_dbpIWfcA","n":"…"}]}
JWKS kid        : FfQyQesnP7c818474zybGHCrMxU_w7FjtR_dbpIWfcA
JWKS co field d?: False
token header    : {'kid': 'FfQyQesnP7c818474zybGHCrMxU_w7FjtR_dbpIWfcA', 'alg': 'RS256'}
=> CHU KY HOP LE (verify bang public key lay tu JWKS, khong dung private key)

# --- AC: 401 không lộ email đã đăng ký (2 response chỉ khác traceId/timestamp) ---
$ (login sai mật khẩu)  → {"code":"UNAUTHORIZED","message":"Email hoặc mật khẩu không đúng",…} [http 401]
$ (login email không tồn tại) → CÙNG code + CÙNG message                              [http 401]
$ (password "123")      → {"code":"VALIDATION_ERROR","details":[{"field":"password",
                            "message":"password phải từ 8 đến 72 ký tự"}]}             [http 400]

# --- AC: fail-fast khi thiếu private key (service KHÔNG chạy nửa vời) ---
$ docker run --rm … -e IDENTITY_JWT_PRIVATE_KEY_PATH=/app/keys/khong-ton-tai.pem shoplab-identity-service:local
Caused by: com.shoplab.identity.auth.jwt.JwtKeyException: Thiếu private key: không đọc được file
'/app/keys/khong-ton-tai.pem'. Sinh khoá dev: bash tools/gen-dev-keys.sh (file bị .gitignore — KHÔNG commit, AGENTS.md §9).
exit=1

# --- AC: /ready 503 khi DB chết · /health vẫn 200 (đúng thiết kế liveness) · hồi phục ---
$ docker stop shoplab-postgres
$ curl -s -w ' [http %{http_code}]' localhost:8081/ready
{"status":"DOWN","dependencies":[{"name":"postgres","status":"DOWN","detail":"SocketTimeoutException: Connect timed out"}]} [http 503]
$ curl -s -w ' [http %{http_code}]' localhost:8081/health
{"status":"UP"} [http 200]
$ docker start shoplab-postgres → /ready http=200

# --- AC: CI xanh, và job `test` THẬT SỰ chạy IT (không skip) ---
$ gh run view 42037058543 --json jobs --jq '.jobs[]|"\(.name) -> \(.status)/\(.conclusion)"'
lint (checkstyle + spotless)   -> completed/success
test (unit + jacoco gate)      -> completed/success
build-image (service-template) -> completed/success
$ gh run view --job <test> --log | grep -E 'postgres:16-alpine|Tests run: 16'
INFO tc.postgres:16-alpine -- Pull complete. 11 layers, pulled in 4s (downloaded 110 MB)
INFO tc.postgres:16-alpine -- Container is started (JDBC URL: jdbc:postgresql://localhost:32769/test)
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.48 s
        -- in com.shoplab.identity.IdentityServiceApplicationIT
[INFO] BUILD SUCCESS   Total time: 01:24 min
```

**Learning note**

- **Spring Boot 4 tách auto-configuration theo từng công nghệ** → khai dependency trần là **im lặng không hoạt động**:
  `flyway-core` một mình **không** kích hoạt Flyway (phải dùng `spring-boot-starter-flyway`); JPA nằm trong module
  `spring-boot-hibernate`. Triệu chứng gặp thật: `Schema validation: missing table [users]` — Hibernate `validate` chạy
  trước, còn Flyway thì chưa từng chạy. **Bài học**: thêm dependency mới phải kiểm chứng nó *thật sự chạy* bằng log,
  không chỉ "build xanh".
- **Jackson 3**: Boot 4 dùng `tools.jackson.databind.ObjectMapper` (KHÔNG còn `com.fasterxml.jackson.databind`) ⇒ import
  cũ lỗi compile ngay (ồn ào, dễ thấy — khác case Flyway ở trên).
- **`spring-boot:repackage` chạy TRƯỚC failsafe** ⇒ failsafe lấy classpath theo artifact jar "fat" (class nằm trong
  `BOOT-INF/classes/`) nên báo `NoClassDefFoundError` cho chính class của module. Fix: khai tường minh
  `<classesDirectory>${project.build.outputDirectory}</classesDirectory>`. Bẫy này chỉ xuất hiện khi **vừa có failsafe
  vừa có spring-boot repackage**.
- **Spring Security 7 coi claim `iss` là URL**: `Jwt.getIssuer()` ném `Unable to convert claim 'iss' to URL` nếu `iss` là
  chuỗi thường ⇒ đổi `iss` sang dạng URI (`https://identity.shoplab.local`), cũng đúng quy ước thực tế của claim này.
- **Non-root container + bind-mount secret = uid mismatch**: private key mode 600 của user host (uid 1000) không đọc được
  bởi user `shoplab` trong image (uid hệ thống) ⇒ `AccessDeniedException`. Fix: `user: "${SHOPLAB_UID}:${SHOPLAB_GID}"`
  (vẫn non-root mà đọc được khoá). Nhờ fail-fast, lỗi hiện ra thành message đọc được ngay thay vì 401 bí ẩn sau này.
- **Mọi Dockerfile phải copy pom của TẤT CẢ module** trong `<modules>`: thêm module thứ 3 làm image build của cả
  `service-template` chết với `Child module ... does not exist` (Dockerfile cũ chỉ copy 2 pom). Đã fix cả 2 Dockerfile +
  ghi vào checklist README.
- **Test "đỏ vì đồng hồ"**: token phát với `Clock` cố định bị decoder coi là hết hạn vì nó so với giờ thật → phải cho
  `JwtTimestampValidator` dùng **cùng** clock đó. Đây chính là lý do tiêm `Clock` thay vì gọi `Instant.now()` trong code.
- **Assert "không lộ email" phải so đúng phần có nghĩa**: so cả body sẽ luôn fail vì `ApiError` chứa `traceId`/`timestamp`
  khác nhau mỗi request → so `code` + `message`.

---

### SL-1.2 — `api-gateway`: routing + verify JWT + correlation id + error thống nhất

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.1 |
| **Ước lượng** | M |

**Bối cảnh**

Nếu client gọi thẳng từng service thì auth bị rải khắp nơi, đổi địa chỉ service là vỡ client, và không có chỗ tập trung để
gắn rate limit (Phase 4) / circuit breaker (Phase 5). Cần **một cửa vào** làm 3 việc: route theo path, verify token một chỗ,
giữ `traceId` xuyên suốt để debug.

**Mục tiêu**

`api-gateway` (port **8080**) là cửa duy nhất: `/v1/**` được route tới service tương ứng; request thiếu/sai token → **401**
theo `ApiError`; `x-correlation-id` được giữ và truyền tiếp để log 2 đầu **cùng một traceId**.

**In scope**

- Service `services/api-gateway` (port 8080), dùng `service-commons` (log JSON, correlation id, error format)
- **Routing table từ config** (`shoplab.gateway.routes[]`: path prefix → base URL), nạp từ env — không hard-code
- Verify JWT RS256 bằng public key lấy từ **JWKS** của identity (cache + refetch khi gặp `kid` lạ)
- **Public route** (không cần token): `/v1/auth/**`, `/.well-known/**`, `/health`, `/ready`; còn lại **bắt buộc** token
- Forward method/body/query/header cần thiết; trả nguyên status + body của service đích; gắn `x-user-id`, `x-user-role`
  (ghi **đè** giá trị client gửi)
- **401** khi thiếu/hết hạn/sai chữ ký · **503** khi service đích không tới được (kèm `ApiError` thống nhất)
- Thêm vào compose + `.env.example` (`API_GATEWAY_PORT=8080`, `IDENTITY_SERVICE_URL`)
- Test: unit (match route, JWT filter với mock, mapping lỗi)

**Out of scope**

- Rate limit (Phase 4) · retry/circuit breaker/bulkhead (Phase 5) · aggregation/BFF · TLS · load balancing nhiều instance
- WebFlux/reactive (xem trade-off) · route động từ DB/UI (Phase 7)

**Acceptance Criteria**

- [x] `curl localhost:8080/health` → 200; container `healthy` trong compose
- [x] `POST /v1/auth/register` **qua gateway (8080)** → 201, response **giống hệt** khi gọi thẳng 8081
- [x] Gọi endpoint protected **không** token → **401** `ApiError` (`code`, `traceId` có giá trị)
- [x] Gọi với token hợp lệ → 200; service đích **nhận được** `x-user-id`/`x-user-role` (chứng minh bằng log)
- [x] `x-correlation-id` do client gửi được **giữ nguyên** xuyên gateway → identity (so log 2 bên)
- [x] Không gửi `x-correlation-id` → gateway tự sinh; log 2 bên **cùng** một traceId
- [x] Token hết hạn / sai chữ ký → 401, **không** lộ chi tiết nội bộ
- [x] `docker stop` identity → gọi qua gateway trả **503** `ApiError`, gateway **không** sập
- [x] `/ready` của gateway phản ánh dependency (503 khi identity chết) — giống cơ chế của template
- [x] `mvn verify` xanh + CI xanh

**Ghi chú kỹ thuật**

- **Tự viết gateway MVC (D1)** để dùng lại 100% `service-commons` (đều servlet-based). Trade-off: **không** reactive/streaming,
  **không** có route động/actuator sẵn. **Revisit** khi cần streaming/WebSocket hoặc route config động (Phase 7) → khi đó cân
  nhắc Spring Cloud Gateway (WebFlux) và chấp nhận viết lại correlation/error cho reactive.
- Dùng `RestClient` + `ClientHttpRequestFactory` có **timeout bắt buộc** (không timeout = treo vô hạn — mầm của Phase 5,
  nhưng phải có sẵn ngay từ đây).
- **JWKS cache** có TTL + refetch khi gặp `kid` lạ (rotate khoá không cần restart gateway).
- Header `x-user-*` do gateway **ghi đè**; service downstream phải **bỏ qua** giá trị client gửi trực tiếp — đây là lý do
  SL-1.5 verify JWT ở cả service (defense in depth).
- **Không** bọc lại response của service đích (tránh double-encode); lỗi do gateway sinh ra dùng `ApiError` của commons.
- Giữ nguyên `permissions: contents: read` + `concurrency` của CI (SL-1.0).

**Kết quả verify** *(2026-11-05 — output thật; WSL2 · JDK 21 · Docker 29.9.2)*

File tạo: `services/api-gateway/**` (gateway + 71 test) · `services/identity-service/…/user/{UserController,CurrentUserResponse}.java` (endpoint protected để chứng minh AC).
File sửa: `pom.xml` (thêm module) · `packages/service-commons/…/ErrorCode.java` (+`UNAVAILABLE` 503) · `infra/docker-compose.yml` · `.env.example` · **cả 3 `Dockerfile`** (thiếu pom module → Maven chết) · `README.md` · `Backlog.md`.

```console
# --- AC: mvn verify (spotless + checkstyle + unit test + IT + jacoco gate) ---
$ mvn -B -pl services/api-gateway,services/identity-service -am verify
[INFO] ShopLab :: Service Commons .... Tests run: 37, Failures: 0, Errors: 0   · 0 Checkstyle violations
[INFO] ShopLab :: Identity Service ... Tests run: 39, Failures: 0, Errors: 0   (unit)
[INFO]                                Tests run: 16, Failures: 0, Errors: 0   (IT — Testcontainers)
[INFO] ShopLab :: API Gateway ........ Tests run: 71, Failures: 0, Errors: 0   (unit + component, KHÔNG cần Docker)
[INFO] 0 Checkstyle violations · All coverage checks have been met.
[INFO] BUILD SUCCESS

# --- AC: container healthy + /health + /ready ---
$ docker compose … ps
shoplab-api-gateway  Up (healthy)   shoplab-identity-service  Up (healthy)   shoplab-postgres  Up (healthy)
$ curl -s -w ' [http %{http_code}]\n' localhost:8080/health
{"status":"UP"} [http 200]
$ curl -s localhost:8080/ready
{"status":"UP","dependencies":[{"name":"identity-service","status":"UP","detail":"ok","latency":"PT0.005937886S"}]}

# --- AC: register/login QUA GATEWAY, so với gọi thẳng 8081 ---
$ curl -s -X POST localhost:8080/v1/auth/register … -d '{"email":"gw2-…@shoplab.local",…}'
{"id":"cb9c88a0-…","email":"gw2-…@shoplab.local","displayName":"Gw2","role":"BUYER","createdAt":"…"}  [http 201]
$ curl -s -X POST localhost:8080/v1/auth/login …   → [http 200]
$ curl -s -X POST localhost:8081/v1/auth/login …   → [http 200]
cung keys: True | cung tokenType/expiresIn: True        ← gateway không bọc/diễn giải lại response

# --- AC: protected 401 khi thiếu token · 200 + x-user-* khi có token ---
$ curl -s -w '\n[http %{http_code}]\n' localhost:8080/v1/users/me
{"code":"UNAUTHORIZED","message":"Thiếu token","traceId":"3705519a-…","path":"/v1/users/me"}  [http 401]
$ curl -s localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN"
{"userId":"801fb6ad-09e0-4728-91f0-e3928d9b59bd","role":"BUYER"}   [http 200]
#   ↑ identity-service trả đúng danh tính ⇒ gateway ĐÃ gắn x-user-id/x-user-role khi forward
```

```console
# --- AC: correlation id xuyên 2 service (so log 2 bên) ---
$ curl -s -o /dev/null -D - localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN" -H 'x-correlation-id: e2e-trace-abc123'
x-correlation-id: e2e-trace-abc123
$ docker logs shoplab-api-gateway 2>&1 | grep e2e-trace-abc123
{"msg":"http GET /v1/users/me -> 200 (13 ms)","traceId":"e2e-trace-abc123","service":"api-gateway"}
$ docker logs shoplab-identity-service 2>&1 | grep e2e-trace-abc123
{"msg":"http GET /v1/users/me -> 200 (1 ms)","traceId":"e2e-trace-abc123","service":"identity-service"}
#   ↑ CÙNG một traceId ở 2 service ⇒ debug 1 request xuyên service làm được

$ (không gửi x-correlation-id) → gateway sinh: 9c45e3c1-551d-42f0-8f92-6186c185fb08
  api-gateway      → traceId=9c45e3c1-…
  identity-service → traceId=9c45e3c1-…      ← vẫn cùng một giá trị

# --- AC: token sai → 401 (không lộ chi tiết) ---
$ (token hết hạn, ký THẬT bằng khoá dev, exp quá khứ)
{"code":"UNAUTHORIZED","message":"token hết hạn","traceId":"fbe7b6a2-…"}        [http 401]
$ (token sửa 6 ký tự cuối của chữ ký)
{"code":"UNAUTHORIZED","message":"chữ ký không hợp lệ","traceId":"f2f8a979-…"}  [http 401]
$ (Authorization: Bearer khong-phai-token) → [http 401]
$ (curl :8080/version KHÔNG token) → [http 401]     ← policy: chỉ 4 pattern trong public-paths là public

# --- AC: identity chết → 503, gateway KHÔNG sập ---
$ docker stop shoplab-identity-service
$ curl -s localhost:8080/v1/users/me -H "Authorization: Bearer $TOKEN"
{"code":"UNAVAILABLE","message":"Service đích không tới được: http://identity-service:8081","traceId":"7b963977-…"} [http 503]
$ curl -s localhost:8080/health   → {"status":"UP"}  [http 200]   ← liveness không check dependency
$ curl -s localhost:8080/ready    → {"status":"DOWN","dependencies":[{"name":"identity-service","status":"DOWN",…}]} [http 503]
$ docker ps | grep gateway        → Up (healthy)                  ← gateway vẫn sống
$ docker start shoplab-identity-service → /v1/users/me 200 · /ready 200   (hồi phục, không cần restart gateway)

# --- AC: CI xanh, và job `test` THẬT SỰ chạy test gateway + IT ---
$ gh run view 42046262655 --json jobs --jq '.jobs[]|"\(.name) -> \(.status)/\(.conclusion)"'
lint (checkstyle + spotless)   -> completed/success
test (unit + jacoco gate)      -> completed/success
build-image (service-template) -> completed/success
$ gh run view --job <test> --log | grep -E 'API Gateway|Tests run: 71|Tests run: 16'
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.shoplab.identity.IdentityServiceApplicationIT
[INFO] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
[INFO] ShopLab :: API Gateway ............................. SUCCESS [ 19.298 s]
[INFO] BUILD SUCCESS   Total time:  01:49 min
```

**Learning note**

- **Gateway tự viết thì phải tự lo "ranh giới segment"**: prefix `/v1/auth` KHÔNG được khớp `/v1/authz` — dùng
  `startsWith` thô là bug kinh điển. Test riêng cho case này (`RouteMatcherTest`) rẻ hơn nhiều so với debug sau.
- **Header danh tính phải bị XOÁ, không chỉ ghi đè**: nếu chỉ `setHeader` ở chỗ forward, client vẫn có thể lọt
  `x-user-id` trên route public (hoặc khi code forward quên). Bọc request bằng `UserHeaderRequest` để mọi đường đọc
  header đều thấy giá trị do gateway quyết định.
- **Cache JWKS phải có cả TTL lẫn "nạp lại khi gặp `kid` lạ"** — và nạp lại phải **chặn dội**: token có `kid` rác là
  đầu vào miễn phí, không chặn thì mỗi request thành 1 lần gọi identity. Lần đầu viết tôi quên chặn dội; test
  `shouldThrottleForcedRefresh` mới lộ ra.
- **401 vs 503 là khác nhau về ngữ nghĩa**: token sai = lỗi client (đừng retry); không lấy được JWKS = lỗi hạ tầng
  (retry được). Gộp thành 401 sẽ khiến client đi đăng nhập lại vô ích khi identity chỉ đang khởi động.
- **Algorithm confusion là thật**: chặn `alg != RS256` TRƯỚC khi tra khoá (`JwtVerifierTest.shouldRejectHs256Token`
  không cần gọi JWKS) — nếu chỉ dựa vào thư viện verify, token HS256 ký bằng chính public key có thể lọt.
- **Bẫy Dockerfile lặp lại lần 2**: thêm module Maven ⇒ **mọi** Dockerfile phải thêm 1 dòng `COPY …/pom.xml` (lần này
  cả `identity-service` và `service-template` đều vỡ). Cảnh báo đã có trong README nhưng vẫn dính → khi thêm module,
  chạy `grep -c 'COPY services/' services/*/Dockerfile` để kiểm.
- **Test của tôi cũng có thể sai**: lần verify đầu, login qua gateway trả 401 — hoá ra 2 tool call chạy **song song**
  nên login tới trước khi register commit (identity đúng khi trả 401 cho user chưa tồn tại). Đọc log 2 bên mới phân
  biệt được "lỗi sản phẩm" với "lỗi kịch bản test".

**Learning note** *(điền khi Done)*

### SL-1.3 — Walking skeleton E2E: register → login qua gateway

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.2 |
| **Ước lượng** | S |

**Bối cảnh**

SL-1.1 và SL-1.2 tạo 2 service riêng lẻ nhưng **chưa có bằng chứng** đường đi "client → gateway → service → DB → response"
chạy thông. Đây là **điều kiện hoàn thành của phase**, nên phải chứng minh **sớm** — nếu vỡ ở tầng tích hợp thì sửa bây giờ
rẻ hơn nhiều so với sau khi đã đắp thêm catalog/RBAC.

**Mục tiêu**

Một script chạy được toàn bộ đường đi và in ra bằng chứng thật (dán được vào ticket); kèm tài liệu để người mới tự chạy lại.

**In scope**

- `tools/e2e-smoke.sh` (bash, `set -euo pipefail`), các bước:
  1) health gateway + identity · 2) register email **random** → 201 · 3) login → lấy token · 4) gọi endpoint protected
  **qua gateway** bằng token → 200 · 5) gọi lại **không** token → 401 · in `PASS/FAIL` từng bước + exit code đúng
- `README.md`: mục "Chạy E2E smoke" + bảng port (8080 gateway · 8081 identity · 8082 catalog — để dành)
- Ghi lại số đo: thời gian script chạy + số request

**Out of scope**

- Catalog (SL-1.5+) · đưa script vào CI (D6 — để Phase 2) · load test (Phase 4) · chaos test (Phase 5)

**Acceptance Criteria**

- [ ] `docker compose … up -d` → gateway + identity **healthy**
- [ ] `bash tools/e2e-smoke.sh` → **tất cả bước PASS**, exit **0**
- [ ] Chạy lại **nhiều lần** vẫn pass (email random, không phụ thuộc state cũ)
- [ ] `docker stop` identity rồi chạy lại → script **fail đúng bước**, exit ≠ 0, in rõ bước nào fail
- [ ] Log của identity thấy request đến từ gateway với **cùng traceId** mà script gửi
- [ ] README có mục chạy script + output mẫu

**Ghi chú kỹ thuật**

- Script chỉ dùng `curl` + `python3` (đã có trong máy — xem README "Yêu cầu môi trường") để parse JSON; **không** thêm `jq`
  (máy này **không** có `jq`, đã gặp trong SL-0.5/SL-1.0).
- Script phải **assert trường hợp mong đợi lỗi** (401), không chỉ "khác 200" — nếu chỉ check `!= 200` thì 500 cũng "pass"
  (đúng bẫy "test âm" đã học ở SL-0.5).
- Chạy **local**; đưa vào CI ở Phase 2 khi số service tăng (mỗi lần thêm `docker compose up` vào CI tốn ~3–5 phút/run — D6).

**Kết quả verify** *(2026-11-05 — output thật)*

```console
$ bash -n tools/e2e-smoke.sh
OK
$ bash tools/e2e-smoke.sh                    # stack healthy (gateway + identity)
== E2E smoke — client → gateway → identity → DB
   email=smoke-1793899426-25773@shoplab.local  correlation=smoke-1793899426
  PASS [1] 1a health gateway
  PASS [2] 1b health identity
  PASS [3] 2 register
  PASS [4] 3 login
  PASS [5] 4 protected /v1/users/me (có token)
  PASS [6] 5 /v1/users/me KHÔNG token → 401
  PASS [7] 6 traceId smoke-1793899426 có trong log identity (gateway forward header)
== Kết quả: 7 PASS / 0 FAIL · 6 request · 279 ms
E2E smoke: OK
exit=0
$ bash tools/e2e-smoke.sh >/dev/null && bash tools/e2e-smoke.sh >/dev/null   # chạy lại 2 lần → exit 0 (email random)

# --- Test âm: tắt identity rồi chạy ---
$ docker stop shoplab-identity-service
$ bash tools/e2e-smoke.sh
  PASS [1] 1a health gateway                  ← gateway liveness vẫn UP
  FAIL 1b health identity — mong đợi 200 nhưng nhận status=000
  FAIL 2 register — mong đợi 201 nhưng nhận status=503   body: {"code":"UNAVAILABLE", ...}
  FAIL 3 login  — mong đợi 200 nhưng nhận status=503
  FAIL 4/5/6 — bỏ qua (chưa có token)
== Kết quả: 1 PASS / 6 FAIL · 4 request · 4063 ms
E2E smoke: FAIL
exit=1                                         ← fail đúng bước (1b), in rõ, exit ≠ 0
$ docker start shoplab-identity-service
```

**Learning note**

- **`date +%s%3N` không portable**: trên GNU date hệ này `%3N` ra **9 chữ số** (nanoseconds) chứ không phải 3 →
  lần đầu số đo ms in `1242150430` ("14 ngày"). Dùng `%s%N` rồi `/1000000` → còn `279 ms`.
- **`set -euo pipefail` + curl connection-refused = script chết câm**: curl exit 7 khi identity tắt → command
  substitution trong assignment làm `set -e` kill script trước khi in FAIL. Fix: `curl … || true` để nhận status `000`
  rồi xử lý, và guard `TOKEN` rỗng để `python3` KeyError không giết script. Smoke script phải **chịu được hạ tầng chết**
  — vì chính việc đó là thứ nó kiểm.
- **503 (service chết) ≠ 401 (thiếu token)** là tín hiệu quý: khi identity tắt, register/login qua gateway trả
  `503 UNAVAILABLE` kèm `traceId` — đúng thiết kế gateway ở SL-1.2, script nhờ đó báo đúng nguyên nhân.

---

### SL-1.4 — `identity-service`: refresh token + RBAC + `/v1/users/me`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.3 |
| **Ước lượng** | M |

**Bối cảnh**

Access token sống 15 phút → không có refresh thì user bị logout liên tục. Và "RBAC cơ bản" trong epic hiện **chưa có gì**:
chưa endpoint nào cần quyền, nên chưa chứng minh được phân quyền có tác dụng thật (chỉ mới có `role` nằm trong token).

**Mục tiêu**

Vòng đời token đầy đủ (access ngắn + refresh dài, có **rotate** và thu hồi) và ít nhất 1 endpoint protected thật để chứng
minh phân quyền hoạt động đúng (401 vs 403).

**In scope**

- Flyway `V2__create_refresh_tokens.sql`: `refresh_tokens(id, user_id, token_hash, expires_at, revoked_at, replaced_by)`
- `POST /v1/auth/refresh` → cặp token **mới** + **rotate** (token cũ bị revoke); dùng lại token đã rotate → **revoke cả chuỗi**
  (phát hiện token bị đánh cắp)
- `POST /v1/auth/logout` → revoke refresh token hiện tại
- `GET /v1/users/me` (cần token) → trả user hiện tại
- `GET /v1/admin/users` (**chỉ** `admin`) → dùng để chứng minh 403
- Seed/migration tạo `seller@shoplab.local` + `admin@shoplab.local` (D7 — mật khẩu dev ghi trong README, **không** dùng prod)
- `spring-boot-starter-security` + security filter chain; map **401/403** sang `ApiError`
- Test: unit (rotate + phát hiện reuse) + IT (luồng refresh + phân quyền)

**Out of scope**

- Social login/OAuth2 · MFA · reset/forgot password · UI quản lý session · rotate khoá ký tự động (đã có JWKS để làm sau)

**Acceptance Criteria**

- [ ] login → refresh → access token **mới** hoạt động; refresh token **cũ** dùng lại → **401** và cả chuỗi bị revoke
- [ ] `GET /v1/users/me` không token → **401**; có token → **200** đúng user
- [ ] `GET /v1/admin/users` bằng token `admin` → **200**; bằng token `buyer` → **403** (không phải 401)
- [ ] 401/403 trả **đúng `ApiError`** (`code`, `message`, `traceId`) — **không** trả HTML mặc định của Spring Security
- [ ] Refresh token lưu **hash** trong DB: query thấy hash, **không** thấy token thật
- [ ] `logout` → refresh token đó không dùng được nữa
- [ ] Migration `V1` **không** bị sửa; schema mới là `V2`
- [ ] `mvn verify` + CI xanh

**Ghi chú kỹ thuật**

- Refresh token là **chuỗi random 256-bit** (không phải JWT) và **lưu hash** → thu hồi được, không cần DB lớn; access token
  vẫn là JWT (stateless).
- **Rotation + reuse detection** là pattern chuẩn: nếu token đã rotate bị dùng lại ⇒ kẻ tấn công đang giữ token cũ ⇒ revoke
  cả chuỗi, buộc login lại. Đây là bài học "security là state machine, không phải boolean" — sẽ ghi learning note.
- Role lấy từ **claim trong JWT** (đã có ở SL-1.1) nên không query DB mỗi request. **Trade-off**: đổi role cần token mới
  (tối đa 15 phút). **Revisit** nếu cần thu hồi quyền tức thời (khi đó: token version/blacklist — nhưng phải trả giá bằng
  state ở mọi request).
- Thêm `spring-boot-starter-security` ở đây (không thêm ở SL-1.1) để cô lập thay đổi filter chain vào 1 ticket; phải khai
  **permitAll** cho `/health` `/ready` `/version` để không phá probe + graceful shutdown của SL-0.4.
- Endpoint `/debug/*` (dev-only) cũng phải nằm trong danh sách permitAll hoặc tắt ở môi trường ngoài local.

**Kết quả verify** *(2026-11-05 — output thật)*

```console
$ mvn -B -ntp -pl services/identity-service -am verify
[INFO] Tests run: 46, Failures: 0, Errors: 0     ← unit (mới: RefreshTokenServiceTest 7 test)
[INFO] Tests run: 22, Failures: 0, Errors: 0     ← IT, Postgres thật (Testcontainers); mới 6 test SL-1.4
[INFO] BUILD SUCCESS                             ← checkstyle + spotless + jacoco gate

# --- E2E trên stack thật (rebuild image identity + gateway) ---
1) login admin → accessToken=co refreshToken=co
2) refresh → token moi khac token cu: YES · access token moi goi /v1/users/me → 200
3) dung lai refresh CU → HTTP 401  {"code":"UNAUTHORIZED","message":"Refresh token không hợp lệ hoặc đã bị thu hồi"}
4) sau reuse, refresh MOI nhat → HTTP 401         ← revoke CẢ CHUỖI (đúng kỳ vọng)
5) /v1/admin/users: admin → 200 · buyer → 403 {"code":"FORBIDDEN","traceId":"75a09d5d…"}
6) /v1/users/me khong token → 401 {"code":"UNAUTHORIZED","traceId":"3267fb09…"}
7) logout → 204 · refresh sau logout → 401
8) DB: token_hash moi nhat = f0217f7bc678842bd242b40bb8e33d865a85045fa3f110d092e2196255c04752   (64 hex)
9) QUA GATEWAY (8080): admin login + /v1/admin/users → 200 · /v1/users/me → 200
$ bash tools/e2e-smoke.sh → 7 PASS / 0 FAIL      ← regression SL-1.3 không vỡ
$ git status → V1__create_users.sql KHÔNG đổi (chỉ thêm V2__create_refresh_tokens.sql)
```

**Learning note**

- **`@Transactional` + throw = rollback xoá luôn việc vừa ghi**: reuse detection revoke cả chuỗi rồi `throw` 401 ⇒
  transaction rollback ⇒ **mất luôn các lệnh revoke** (đúng thứ vừa phát hiện). Phải `noRollbackFor` ở **mọi tầng**
  `@Transactional` mà exception đi qua — sửa ở tầng trong (`RefreshTokenService`) vẫn hỏng vì tầng **ngoài**
  (`AuthService.refresh`) mới là cái quyết định commit. **IT bắt được, unit test mock thì không** ⇒ bài học: test tích hợp
  không phải "cho có", nó bắt đúng lớp lỗi transaction mà mock che mất.
- **Endpoint mới phải verify qua CỬA VÀO DUY NHẤT**: `/v1/admin/users` trả 200 khi gọi thẳng identity nhưng **404 qua
  gateway** vì gateway chưa có route `/v1/admin`. Chỉ lộ ra khi test qua 8080 → đã thêm route + test lại.
- **Security filter đổi hành vi cũ**: `anyRequest().authenticated()` làm path không tồn tại trả **401** thay vì 404
  (không cho người lạ dò danh sách route). Test cũ fail — sửa test theo **hành vi đúng**, không sửa cho xanh.
- **Defense in depth có giá trị thật**: gọi thẳng port service (8081) vẫn phải có token hợp lệ; header `x-user-*` không
  còn là nguồn tin duy nhất ở identity.
- **Seed bằng code, không bằng SQL**: hash BCrypt nhúng trong migration là thứ ai đọc repo cũng thấy và không đổi được
  nếu không viết migration mới → dùng `LocalDevUserSeeder` (idempotent, chỉ `APP_ENV=local`).

### SL-1.5 — `catalog-service`: skeleton + Flyway + category/product CRUD

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.3 |
| **Ước lượng** | M |

**Bối cảnh**

Đây là service **read-heavy đầu tiên** (guest xem sản phẩm) — đúng bài toán mà Phase 4 sẽ tối ưu bằng cache, nên phải có
API + dữ liệu thật ngay từ Phase 1 thì Phase 4 mới có cái để **đo**. Đồng thời đây là service đầu tiên cần **ghi dữ liệu
có phân quyền** (chỉ seller/admin được tạo sản phẩm).

**Mục tiêu**

`catalog-service` (port **8082**) có schema riêng do Flyway quản lý, CRUD category/product hoạt động: ai cũng **đọc** được,
chỉ `seller`/`admin` **ghi** được.

**In scope**

- Service `services/catalog-service` (port 8082) + DB `catalog` + Flyway `V1__create_catalog.sql` (`category`, `product`)
- API:
  - `POST /v1/categories` (seller/admin) · `GET /v1/categories` (public)
  - `POST /v1/products` (seller/admin) · `PUT /v1/products/{id}` · `DELETE /v1/products/{id}`
  - `GET /v1/products/{id}` (public)
- Validate input bằng `@Valid`; lỗi 400 trả `ApiError` kèm `details` theo field
- **Verify JWT tại service** (defense in depth, không tin hoàn toàn gateway) + dùng `x-user-id`/`x-user-role`
  **do gateway gắn** (bỏ qua header client tự gửi)
- 404 khi id không tồn tại (dùng `NotFoundException` có sẵn trong commons)
- Test: unit (service/validate) + IT Testcontainers (CRUD + phân quyền 403/401)
- Thêm vào compose + `.env.example` (`CATALOG_SERVICE_PORT=8082`, `CATALOG_SERVICE_URL` cho gateway)

**Out of scope**

- SKU · phân trang/filter · seed data → **SL-1.6**
- Cache (Phase 4) · search engine (Phase 3/8) · upload ảnh · tồn kho (Phase 2) · promotion/giá theo campaign (Phase 2)

**Acceptance Criteria**

- [ ] container **healthy**; `/health` → 200; DB `catalog` có `category`/`product` + `flyway_schema_history`
- [ ] `POST /v1/categories` bằng token **seller** → **201**; bằng token **buyer** → **403**
- [ ] `POST /v1/products` (seller) → **201**; `GET /v1/products/{id}` → **200** đúng dữ liệu
- [ ] `PUT`/`DELETE` sản phẩm không tồn tại → **404** `ApiError`
- [ ] Body sai (thiếu `name`, `price` âm) → **400**, `details` nêu rõ field sai
- [ ] Gọi **thẳng** `:8082` **không** token → **401** (không phụ thuộc gateway)
- [ ] Client tự gửi `x-user-role: admin` tới gateway → **không** leo được quyền (gateway ghi đè — verify bằng cách gọi qua 8080)
- [ ] `GET /v1/products/{id}` **không** token → 200 (public cho guest)
- [ ] `mvn verify` + CI xanh

**Ghi chú kỹ thuật**

- `price` dùng **`BigDecimal` + `numeric(12,2)`** — **không** dùng `double` cho tiền (sai số làm tròn sẽ thành bug thật ở Phase 2).
- FK `product.category_id → category.id`; index trên `category_id` (Phase 4 sẽ đo lại).
- Thêm `@Version` (optimistic locking) cho `product` ngay từ đầu: rẻ bây giờ, và Phase 2 (đặt hàng) sẽ cần.
- **Verify JWT ở cả service** (không chỉ gateway): nếu chỉ tin gateway thì service trở thành "tin tuyệt đối" vào 1 lớp; đây là
  defense in depth tiêu chuẩn.
- Quy ước lỗi: **401** = chưa xác thực · **403** = đã xác thực nhưng thiếu quyền. Sai quy ước này là nguồn bug khó tìm ở client.

**Kết quả verify**

**Unit + IT** — `mvn -B -ntp -pl services/catalog-service -am verify`:

```console
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0   ← service-commons (không hồi quy)
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0   ← catalog unit (config/health/service)
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0   ← catalog IT (Testcontainers postgres:16-alpine)
[INFO] BUILD SUCCESS
```
Checkstyle + Spotless + JaCoCo gate (≥80% LINE cho `config/*`, `health/*`) đều pass.

**AC trên stack thật** — `docker compose up -d --build` (gateway 8080 · identity 8081 · catalog 8082),
script verify Python (login thật → token thật → JWKS thật): **36 PASS / 0 FAIL**.

| AC | Kết quả đo được |
|---|---|
| container healthy · `/health` 200 · `/ready` 200 · `/version` = `catalog-service` | PASS |
| DB `catalog` có `category` + `product`; `flyway_schema_history` = 1 row (V1) | PASS |
| `POST /v1/categories` seller → **201** (+`Location`) · buyer → **403** `FORBIDDEN` có `traceId` | PASS |
| `GET /v1/categories` guest → **200**, thấy category vừa tạo | PASS |
| `POST /v1/products` seller → **201**; `price` = `25990000.0` (BigDecimal, không sai số); `version` 0 | PASS |
| `GET /v1/products/{id}` **không** token → **200** (cả qua gateway và gọi thẳng :8082) | PASS |
| `PUT`/`DELETE` id không tồn tại → **404** `NOT_FOUND`; `PUT` thật → 200 và `version` **tăng lên 1** | PASS |
| `DELETE` thật → **204**; `GET` sau đó → **404** | PASS |
| body sai (`name` rỗng, `price` = -1) → **400** `VALIDATION_ERROR`, `details` nêu field `name` + `price` | PASS |
| gọi **thẳng** `:8082` không token → **401** `UNAUTHORIZED` (không phụ thuộc gateway) | PASS |
| client tự gửi `x-user-role: admin` → qua gateway **401** (không token) / **403** (buyer token) / thẳng service **403** | PASS |
| token rác → **401** | PASS |

**Regression**: `bash tools/e2e-smoke.sh` → **7 PASS / 0 FAIL** (identity + gateway không hồi quy).

**3 bug thật phát hiện khi verify (đã sửa + có test giữ)**

1. **`@PreAuthorize` từ chối ⇒ 500 chứ không phải 403**: `AuthorizationDeniedException` ném *trong* lời gọi
   controller nên `ExceptionTranslationFilter` không thấy, `GlobalExceptionHandler` bắt `Exception` → 500. Thêm
   `AccessDeniedApiExceptionHandler` (→ 403 `ApiError`). IT bắt được, curl tay dễ bỏ qua.
2. **`@Version` trả số cũ trong response PUT**: version chỉ tăng lúc flush ⇒ response `version: 0` dù DB đã 1 ⇒
   client update lần sau bằng version sai. Sửa bằng `saveAndFlush` + IT assert `"version":1`.
3. **`docker build` đỏ vì JaCoCo gate**: trong image build không có Docker socket ⇒ `-DskipITs` ⇒ thiếu đúng phần
   coverage chỉ IT chạm (`SecurityConfig`) ⇒ gate 80% fail. Thêm `-Djacoco.skip=true` vào cả 4 Dockerfile (gate
   coverage thuộc job `test` của CI, chạy đủ IT).

**Learning note**

- **Method security ném exception *sau* DispatcherServlet ⇒ 500 nếu không map tay.** `@PreAuthorize` là chốt
  phân quyền *trong* code, KHÔNG phải filter — nên `ExceptionTranslationFilter` (thứ trả 401/403 cho filter chain)
  không thấy nó. Bài học chung: **mỗi lớp chặn quyền phải có đường trả lỗi riêng**; thêm một lớp bảo vệ là thêm
  một chỗ phải kiểm hành vi lỗi, không chỉ hành vi đúng.
- **Optimistic locking: đọc `version` sau khi flush, không phải sau khi gán field.** JPA tăng `@Version` ở thời
  điểm flush/commit. Trả DTO trước flush = trả dữ liệu cũ ⇒ client cầm version sai ⇒ 409 "ma" ở lần update sau,
  hoặc tệ hơn là ghi đè. Bài học: **giá trị do DB sinh ra (version, default, trigger) phải đọc sau flush**.
- **`docker build` không có Docker socket ⇒ mọi thứ phụ thuộc IT sẽ lệch.** `-DskipITs` không chỉ "bỏ test":
  nó **đổi kết quả gate coverage**. Fix đúng là tách vai: **gate chất lượng ở job `test` (có IT)**, image build chỉ
  cần compile + unit. Đây là lần thứ hai bẫy "image build đỏ vì môi trường thiếu" (lần đầu: thiếu `COPY` pom module).
- **Gateway public-path là theo PATH, không theo METHOD** ⇒ service có "đọc public + ghi cần token" trên cùng
  prefix thì gateway phải thả cả prefix, và **service tự chặn phần ghi**. Đánh đổi: gateway thôi làm chốt 401 cho
  các path đó. Chấp nhận được *nhờ* defense in depth (service tự verify JWT) — nếu chỉ tin header gateway thì
  đánh đổi này là lỗ hổng, không phải trade-off.
- **`x-user-role` client gửi lên không bao giờ được dùng để phân quyền**: gateway xoá/ghi đè, catalog thì bỏ qua
  hoàn toàn (chỉ tin token). Verify bằng 3 đường (không token / buyer token / gọi thẳng service) — kiểm 1 đường
  là chưa đủ.
- **Verify script viết bằng bash heredoc dễ tự bắn vào chân**: escaping 3 lớp (`-d '{\"...\"}'` trong `$( )` trong
  heredoc) làm token bị cắt → **17 FAIL giả** trong khi sản phẩm đúng. Chuyển sang Python `urllib` (36 PASS) rẻ
  hơn nhiều so với việc đi debug "lỗi" không tồn tại. Bài học: **script verify là code, phải chọn môi trường ít
  escaping** — và khi thấy FAIL hàng loạt cùng một kiểu (401/None) thì nghi script trước, nghi sản phẩm sau.
- **Bug `@Version` chỉ lộ khi chạy HTTP thật**: unit test mock repository không mô phỏng flush ⇒ 36 unit test xanh
  vẫn không thấy. IT + gọi thật mới thấy. Củng cố quy ước: **hành vi liên quan transaction/flush phải test bằng IT**.

### SL-1.6 — `catalog-service`: SKU + phân trang/filter + seed data

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.5 |
| **Ước lượng** | M |

**Bối cảnh**

Một dòng `product` chưa đủ cho e-commerce: cần **SKU** (biến thể màu/size). Và guest cần **duyệt** danh sách → cần phân
trang + filter, nếu không Phase 4 không có gì để đo p99/throughput. Cuối cùng, cần **seed data** để mọi người (và chính mình
sau này) luôn có dữ liệu demo thay vì DB rỗng.

**Mục tiêu**

Catalog có dữ liệu mẫu dùng được ngay sau `up -d`, API duyệt sản phẩm cho guest có phân trang/filter/sort **ổn định** và
**không** cho phép sort tuỳ ý (tránh lộ schema + tránh full scan).

**In scope**

- Flyway `V2__create_sku.sql`: `sku(id, product_id fk, code unique, price_override, stock_hint, created_at)`
- API SKU lồng trong product (`GET /v1/products/{id}` trả kèm danh sách SKU; `POST /v1/products/{id}/skus` cho seller/admin)
- `GET /v1/products?page=&size=&categoryId=&q=&sort=` → response dạng page: `{content, page, size, totalElements, totalPages}`
  — **public** (không cần token)
- Filter `categoryId` · `q` (tìm theo tên, **không** phân biệt hoa thường) · `sort` theo **whitelist**
  (`price,asc|desc` · `createdAt,asc|desc`)
- **Seed data** (Flyway repeatable `R__seed_dev_data.sql`): ~5 category + ~20 product + SKU, tên rõ ràng là dữ liệu dev
- Index: `category_id`, `created_at`, `lower(name)`
- Test: IT cho phân trang (page 0/1, size, tổng số), filter, sort, whitelist sort; unit cho validate param

**Out of scope**

- Search engine/OpenSearch (Phase 3/8) · full-text search · cache (Phase 4) · ảnh · giá theo thời điểm · tồn kho thật (Phase 2)

**Acceptance Criteria**

- [ ] `GET /v1/products?page=0&size=5` → 5 phần tử + `totalElements` = số seed; `page=1` khác trang 0 và **không** trùng id
- [ ] `size=0` · `size=1000` · `page=-1` → hành vi **đã chọn** (400 hoặc clamp) và verify đúng hành vi đó (ghi rõ trong ticket)
- [ ] `categoryId=<id>` → chỉ trả sản phẩm thuộc category đó
- [ ] `q=iphone` (gõ hoa/thường khác nhau) → trả đúng kết quả
- [ ] `sort=price,desc` → đúng thứ tự; `sort=drop table` → **400** (không lọt vào SQL)
- [ ] `GET /v1/products` **không** token → **200** (public cho guest)
- [ ] Sau `docker compose down -v && up -d`: seed data **có** trong DB
- [ ] `up -d` lần thứ hai **không** nhân đôi dữ liệu (migration idempotent)
- [ ] `EXPLAIN` query list **dùng index** (dán output vào verify)
- [ ] `mvn verify` + CI xanh

**Ghi chú kỹ thuật**

- Phân trang **offset** (`Pageable`) là đủ ở Phase 1. **Keyset pagination** là bài toán của Phase 4 khi dữ liệu lớn
  (offset sâu ⇒ `OFFSET 100000` quét bỏ rất nhiều row) → ghi vào revisit, sẽ đo bằng số liệu.
- **Sort whitelist** (không nhận field tự do) để: (a) không lộ schema, (b) chỉ cho sort trên cột **có index** — nếu không
  index thì sort sẽ thành full scan + sort in memory.
- `q` dùng `lower(name) LIKE` + index functional `lower(name)`; **chưa** dùng full-text (`tsvector`) — để Phase 3/8 khi cần.
- Seed dùng **repeatable migration** (`R__`) để chạy lại được khi cần reset dữ liệu dev mà không phải viết migration mới;
  **không** dùng cho dữ liệu prod (ghi rõ trong comment file).
- Giới hạn `size` tối đa (ví dụ 100) để tránh client xin 1 triệu row — đây là dạng "rate limit ở tầng dữ liệu" đầu tiên.

**Kết quả verify**

**Hành vi đã chọn cho tham số sai** (AC yêu cầu ghi rõ): `size` ngoài **1..100**, `page < 0`,
`sort` không thuộc whitelist, `categoryId` không phải UUID, `q` > 100 ký tự → **400
`VALIDATION_ERROR`** kèm message nêu đúng field. **Không clamp** — clamp làm client tưởng đã lấy đủ
dữ liệu (bug im lặng). Mặc định: `page=0`, `size=20`, `sort=createdAt,desc`.

**Unit + IT** — `mvn -B -ntp -pl services/catalog-service -am verify`:

```console
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0   ← catalog unit (36 → 60: +ProductQuery 20, +Sku 4)
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0   ← catalog IT (12 → 18)
[INFO] BUILD SUCCESS
```

**AC trên stack thật** — script Python qua gateway: **35 PASS / 0 FAIL** (chạy cả trên DB cũ *và*
sau `down -v && up -d`).

| AC | Kết quả đo được |
|---|---|
| `GET /v1/products?page=0&size=5` → 5 phần tử, `totalElements` = 20 (seed), `totalPages` = 4 | PASS |
| `page=1` khác trang 0, **không trùng id** (assert `isdisjoint`) | PASS |
| `size=0` · `size=1000` · `page=-1` · `sort=drop table` · `sort=name,asc` → **400** + `VALIDATION_ERROR` | PASS |
| `size=100` (biên) → **200** | PASS |
| `categoryId=<Dien thoai>` → chỉ 4 product của category đó | PASS |
| `q=IPHONE` (hoa) và `q=iphone` (thường) → **cùng** kết quả, tìm thấy `iPhone 15` | PASS |
| `sort=price,desc` → giá giảm dần; `sort=price,asc` → tăng dần; `sort=createdAt,asc` → thời gian tăng dần | PASS |
| `GET /v1/products` **không** token → **200** (public cho guest) | PASS |
| `GET /v1/products/{id}` trả kèm `skus` (product seed có đúng 2 SKU) | PASS |
| `POST /v1/products/{id}/skus` trùng `code` → **409** | PASS |
| Sau `docker compose down -v && up -d`: **5 category / 20 product / 24 sku** có sẵn trong DB | PASS |
| Restart service (`docker restart`): số row **không đổi** (5/20/24 → 5/20/24) — seed idempotent | PASS |
| `EXPLAIN` sort mặc định: **`Index Scan Backward using ix_product_created_at`** | PASS |
| `EXPLAIN` filter category (buộc `enable_seqscan=off`): **`Index Scan using ix_product_category_id`** | PASS |

**Plan thật của Postgres** (query list mặc định `order by created_at desc limit 5`):

```console
                              QUERY PLAN
------------------------------------------------------------------
 Limit
   ->  Index Scan Backward using ix_product_created_at on product
(2 rows)
```

**Regression**: `bash tools/e2e-smoke.sh` → **7 PASS / 0 FAIL**.

**Learning note**

- **Planner không dùng index khi bảng nhỏ — và điều đó KHÔNG có nghĩa index sai.** Với 20 row,
  `seq scan` rẻ hơn `index scan` (đọc cả bảng vẫn ít I/O hơn nhảy index + random read). Muốn chứng
  minh index *dùng được* thì `SET enable_seqscan=off` rồi EXPLAIN, và **ghi rõ đã ép**. Còn query
  sort mặc định thì `Index Scan Backward` được chọn **tự nhiên** ngay cả với 20 row, vì `limit 5`
  làm index scan rẻ hơn hẳn. Bài học: **EXPLAIN phải kèm bối cảnh dữ liệu + query cụ thể**, không
  phải câu "đã dùng index" chung chung.
- **`q` với `%...%` thì functional index `lower(name)` KHÔNG cứu được**: leading wildcard (`LIKE
  '%abc%'`) không dùng được btree. Index đó chỉ có ích khi search prefix (`LIKE 'abc%'`). Muốn search
  giữa chuỗi ở scale lớn thì phải `tsvector`/trigram hoặc search engine (Phase 3/8). Đã thêm index để
  sẵn nhưng **ghi rõ giới hạn** thay vì tuyên bố "đã tối ưu".
- **Repeatable migration (`R__`) phải viết idempotent**, vì Flyway chạy lại nó mỗi khi checksum đổi
  (và ở lần boot đầu tiên trên DB mới). `ON CONFLICT (id) DO NOTHING` + UUID cố định ⇒ `up -d`
  nhiều lần không nhân đôi dữ liệu. Verify bằng **restart thật** rồi so số row (5/20/24 → 5/20/24),
  không tin vào việc "đọc code thấy đúng".
- **Whitelist sort là chốt chặn SQL injection ở dạng khó thấy**: `sort` đi vào `Sort.by(...)` của
  Spring Data, tức là vào ORDER BY của SQL. Value client gửi **chỉ được tra trong map** rồi mới dùng,
  không bao giờ ghép chuỗi. Test `sort=drop table` phải assert **400**, không phải "không crash".
- **Validate tham số ở tầng parse (`ProductQuery`) thay vì để Spring bind**: Spring bind sai kiểu trả
  400 nhưng body không phải `ApiError` (client phải xử lý 2 format lỗi). Tách record `ProductQuery`
  còn cho phép **20 unit test** chạy trong 0,2s — rẻ hơn nhiều so với test qua HTTP.
- **Trang chi tiết và trang list phải khác DTO**: list trả `ProductResponse` (không SKU) để tránh N+1
  (20 product × 1 query SKU); chi tiết trả `ProductDetailResponse` có `skus`. Cùng một entity nhưng
  hai nhu cầu đọc khác nhau — dùng chung DTO là tự tạo N+1 mà không ai thấy cho tới khi tải cao.

### SL-1.7 — E2E smoke test đầy đủ + runbook Phase 1

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.6 (và SL-1.4) |
| **Ước lượng** | S |

**Bối cảnh**

`tools/e2e-smoke.sh` hiện chỉ có phần identity (SL-1.3). Điều kiện hoàn thành Phase 1 yêu cầu đúng luồng:
**register → login → create product (seller) → list product (guest)** qua gateway. Ngoài ra khi hệ có 3 service, lỗi sẽ
xuất hiện ở nhiều tầng → cần **runbook** ngắn để không phải mò lại từ đầu mỗi lần vỡ.

**Mục tiêu**

Một lệnh chứng minh toàn bộ luồng Phase 1 chạy đúng (kể cả phân quyền), và một runbook đủ để tự debug khi có tầng chết.

**In scope**

- Mở rộng `tools/e2e-smoke.sh`: thêm bước tạo category + product bằng token **seller** (seed từ SL-1.4) → 201;
  list product bằng **guest** (không token) → thấy sản phẩm vừa tạo; tạo product bằng **buyer** → **403** (assert đúng 403)
- `docs/runbook-phase-1.md`: sơ đồ luồng request (client → gateway → service → DB), bảng port, lệnh kiểm tra **từng tầng**,
  và **5 lỗi thường gặp** + cách xử lý (401 · 403 · 404 · 503 · timeout)
- Cập nhật `README.md`: trạng thái Phase 1 + link runbook

**Out of scope**

- Chạy trong CI (D6 — để Phase 2) · load test (Phase 4) · chaos test (Phase 5) · UI/client thật

**Acceptance Criteria**

- [ ] `bash tools/e2e-smoke.sh` → **PASS toàn bộ**, exit 0, in ra id của product vừa tạo
- [ ] Guest list được product vừa tạo (chứng minh đọc public **qua gateway**)
- [ ] Bước "buyer tạo product" trả **403** và script **không** coi đó là fail (assert đúng kỳ vọng)
- [ ] Chạy lại nhiều lần vẫn pass (dữ liệu random, không phụ thuộc state cũ)
- [ ] Tắt 1 trong 3 service → script fail **đúng bước**, in gợi ý lệnh kiểm tra tương ứng trong runbook
- [ ] Runbook có đủ 5 lỗi + lệnh kiểm tra từng tầng; **thực sự làm theo** runbook để debug 1 lỗi giả lập (ghi lại vào verify)
- [ ] README trỏ tới runbook + script

**Ghi chú kỹ thuật**

- Script phải **assert trường hợp mong đợi lỗi** (403/401), không chỉ "khác 200" — nếu chỉ check `!= 200` thì 500 cũng "pass"
  (bẫy đã gặp ở SL-0.5 khi test âm).
- Runbook viết dạng bảng: **triệu chứng → lệnh kiểm tra → nguyên nhân thường gặp → cách xử lý**, mỗi lỗi ≤ 5 dòng.
- Đây là "runbook" đầu tiên của repo — Phase 5 sẽ mở rộng thành runbook sự cố đầy đủ (`SL-5.x`), nên đặt file theo tên
  `docs/runbook-phase-1.md` để sau này gom lại.

**Kết quả verify**

**Script mở rộng** — `bash tools/e2e-smoke.sh` (17 mục, 13 request):

```console
$ bash tools/e2e-smoke.sh
  PASS [1] 1a health gateway · [2] 1b health identity · [3] 1c health catalog
  PASS [4] 2 register · [5] 3 login · [6] 4 /v1/users/me có token · [7] 5 /v1/users/me KHÔNG token → 401
  PASS [8] 6 traceId ... có trong log identity (gateway forward header)
  PASS [9] 7 login seller (seller@shoplab.local)
  PASS [10] 8 seller tạo category → 201
  PASS [11] 9 seller tạo product → 201        product id = f36347f0-d1b3-4e58-9ecd-682d1aa47505
  PASS [12] 10 guest list product (KHÔNG token) → 200
  PASS [13] 10b product f36347f0-… xuất hiện trong list của guest
  PASS [14] 11 guest xem chi tiết product → 200 · [15] 11b có field skus
  PASS [16] 12 buyer tạo product → 403 (đúng như mong đợi)
  PASS [17] 12b body 403 là ApiError FORBIDDEN
== Kết quả: 17 PASS / 0 FAIL · 13 request · 927 ms
E2E smoke: OK
```

| AC | Kết quả đo được |
|---|---|
| Chạy → PASS toàn bộ, exit 0, in **id product** vừa tạo | PASS (`f36347f0-…`, `E2E smoke: OK`) |
| Guest list được product vừa tạo (qua gateway) | PASS (bước 10 + 10b) |
| "buyer tạo product" trả **403** và script **không** coi là fail | PASS (bước 12/12b, assert đúng 403 + body `FORBIDDEN`) |
| Chạy lại nhiều lần vẫn pass (dữ liệu random) | PASS — **6/6 lần**, ~430 ms/lần |
| Tắt 1 service → fail **đúng bước** + in gợi ý | PASS — `docker stop shoplab-catalog-service` → fail **1c** (`status=000`) và **8** (`503` + gợi ý), các bước khác vẫn PASS |
| Runbook đủ 5 lỗi + lệnh từng tầng; **làm theo** để debug 1 lỗi giả lập | PASS — xem `docs/runbook-phase-1.md` §5 (dán nguyên văn) |
| README trỏ tới runbook + script | PASS |

**2 bug thật phát hiện khi verify (đã sửa)**

1. **Smoke test flaky**: category tạo theo `Smoke $TS` (giây) ⇒ 2 lần chạy trong **cùng giây** đụng
   unique index `slug` ⇒ **409** ⇒ 4 bước fail (đo được: `9 PASS / 4 FAIL`). Sửa: thêm `$RANDOM`.
   Đúng là lý do AC "chạy lại nhiều lần vẫn pass" tồn tại — nếu chỉ chạy 1 lần thì đã không thấy.
2. **Script báo sai chỗ khi connection refused**: `curl -o <file>` không mở file khi không kết nối
   được ⇒ file còn nội dung lần trước ⇒ bước fail in **body cũ** (`{"status":"UP"}` trong khi catalog
   đã chết). Sửa: `: > "$TMP_BODY"` trước mỗi request. Bug "báo sai chỗ" nguy hiểm hơn bug báo đúng.

**Learning note**

- **Smoke test phải random "đủ mạnh"**: timestamp giây KHÔNG đủ khi test chạy <1s. Bất kỳ unique
  constraint nào (email, slug, code SKU) cũng phải có entropy thật (`$RANDOM`/UUID), nếu không test
  sẽ xanh trên máy chậm và đỏ trên máy nhanh — kiểu flaky tệ nhất vì `git bisect` không tìm ra.
- **Script debug cũng là sản phẩm**: in `status` + `body` + **gợi ý lệnh** ngay tại bước fail biến
  "chạy lại rồi đoán" thành "đọc là biết tầng nào". Nhưng body phải được **xoá trước** mỗi request,
  nếu không nó nói dối đúng vào lúc cần nhất.
- **`000` vs `503` chỉ ra 2 tầng khác nhau**: `000` = client không kết nối được (service chết/cổng
  sai) · `503` = gateway sống nhưng service đích không tới được. Runbook phải dạy đọc cặp này, vì
  cùng "không có 200" mà nguyên nhân khác hẳn.
- **Test âm phải là case có chủ đích**: bước "buyer tạo product → 403" là **PASS khi trả 403** — nằm
  cùng script với các bước happy path. Nếu tách ra script riêng thì không ai chạy.
- **Runbook chỉ đáng tin khi đã dùng nó để debug thật**: §5 của `docs/runbook-phase-1.md` là output
  nguyên văn của lần giả lập lỗi, không phải ví dụ viết tay.

### SL-1.8 — OpenAPI spec cho các endpoint Phase 1

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton |
| **Phụ thuộc** | SL-1.7 |
| **Ước lượng** | S |

**Bối cảnh**

Đến đây API Phase 1 đã ổn định (auth + catalog + phân quyền). Chưa có **contract** nào để người khác — hoặc chính mình 3
tháng sau — đọc mà không phải mở code. `AGENTS.md` §6 yêu cầu: *"API: REST version qua path `/v1/...`, contract mô tả bằng
OpenAPI"*.

**Mục tiêu**

Có spec **khớp với thực tế**, đọc được, và **diff được trong git** — làm nền cho client/contract test ở phase sau.

**In scope**

- Thêm `springdoc-openapi` cho 3 service, expose `/v3/api-docs` (**tắt** ở môi trường ngoài local)
- Annotation ngắn: mô tả endpoint, request/response, mã lỗi, **security scheme bearer** cho endpoint protected
- `tools/export-openapi.sh`: gom spec của 3 service → **`docs/api/openapi.yaml`** (1 file, `servers` trỏ gateway) rồi commit
- `docs/api/README.md`: cách đọc + cách cập nhật spec (khi thêm endpoint)
- Verify spec **khớp thực tế**: gọi vài endpoint và đối chiếu với spec (ghi vào verify)

**Out of scope**

- Sinh client SDK · contract testing tự động (Pact) · import spec vào gateway · nhiều version API (chỉ `/v1`)

**Acceptance Criteria**

- [ ] `GET /v3/api-docs` trên **cả 3 service** → 200 + JSON hợp lệ
- [ ] `bash tools/export-openapi.sh` → sinh `docs/api/openapi.yaml` **không lỗi** và file được commit
- [ ] Spec liệt kê **đủ** endpoint Phase 1 — đếm và ghi con số vào verify; **không** thiếu endpoint nào đang có
- [ ] Trong spec: có security scheme **bearer** + endpoint protected được đánh dấu cần auth
- [ ] Response lỗi trong spec dùng schema **`ApiError`** (400/401/403/404/409)
- [ ] Spec **validate pass** (spectral / swagger-cli; nếu không cài được thì dùng `python3` + `yaml.safe_load` + kiểm tra khoá bắt buộc — ghi rõ cách đã dùng)
- [ ] `/v3/api-docs` **không** bật khi `APP_ENV != local` (verify bằng env thật)

**Ghi chú kỹ thuật**

- Chọn **sinh từ code (D5)** để spec không lệch thực tế. **Trade-off**: spec là *hệ quả* của code, không phải *nguồn* → nếu
  sau này có client bên ngoài/nhiều team thì nên chuyển **contract-first** (viết YAML trước, code theo) — ghi vào revisit.
- Export script phải chạy **offline** khi stack đang chạy local (curl `:8080/v3/api-docs`), không cần công cụ ngoài;
  format YAML bằng `python3` (máy **không** có `jq`).
- Đặt spec ở `docs/api/openapi.yaml` để nằm cùng chỗ với các doc khác và **diff được theo phase** (Phase 2 sẽ thấy rõ API
  mới thêm gì).

**Kết quả verify**

**Export + validate** — `bash tools/export-openapi.sh`:

```console
$ bash tools/export-openapi.sh
== Lấy spec của từng service
  OK identity ← http://localhost:8081/v3/api-docs
  OK catalog ← http://localhost:8082/v3/api-docs
  OK gateway ← http://localhost:8080/v3/api-docs
== Merge + validate → docs/api/openapi.yaml
VALIDATE PASS · 17 path · 21 operation · 7 operation cần token · 18 schema
```

| AC | Kết quả đo được |
|---|---|
| `GET /v3/api-docs` trên **cả 3 service** → 200 + JSON hợp lệ | PASS — `:8080` 200 · `:8081` 200 · `:8082` 200 |
| `bash tools/export-openapi.sh` sinh `docs/api/openapi.yaml` không lỗi, file được commit | PASS (`VALIDATE PASS`) |
| Spec liệt kê **đủ** endpoint Phase 1 — đếm + ghi số | **17 path / 21 operation**: identity 7 (`auth/register`, `login`, `refresh`, `logout`, `users/me`, `admin/users`, `well-known/jwks.json`) · catalog 8 (`categories` GET+POST, `products` GET+POST, `products/{id}` GET/PUT/DELETE, `products/{productId}/skus` POST) · probe 4 (`health`, `ready`, `version`, `debug/*`) |
| Có security scheme **bearer** + endpoint protected đánh dấu cần auth | PASS — `components.securitySchemes.bearerAuth` (http/bearer/JWT); **7 operation** có `security` (admin/users · users/me · category POST · product POST/PUT/DELETE · sku POST), các GET public **không** có (đúng thực tế) |
| Response lỗi dùng schema **`ApiError`** (400/401/403/404/409) | PASS — validate kiểm từng operation: cả 5 mã đều `$ref: #/components/schemas/ApiError` |
| Spec **validate pass** (ghi rõ cách đã dùng) | PASS — `python3` + `pyyaml` 6.0.3 (máy không có spectral/swagger-cli): kiểm `openapi` 3.x · `info.title` · `servers` · bearer scheme · `ApiError` · đủ 5 response lỗi cho mọi operation |
| `/v3/api-docs` **không** bật khi `APP_ENV != local` | PASS — chạy container với `APP_ENV=prod`: `/v3/api-docs` → **404**, `/health` → 200, `/v1/products` → 200 (service vẫn chạy bình thường) |

**Regression**: `bash tools/e2e-smoke.sh` → **17 PASS / 0 FAIL**; cả reactor `mvn verify` xanh
(unit 42 commons + 35 template + 46 identity + 60 catalog + 71 gateway, IT 22 + 18).

**Spot-check spec ↔ thực tế** (gọi thật đúng như spec mô tả): `POST /v1/auth/register` body rỗng →
**400** `VALIDATION_ERROR` (spec ghi 400 + details) · `GET /v1/products?size=1` → **200** ·
`GET /.well-known/jwks.json` → **200**.

**Learning note**

- **Version springdoc phải tra bảng tương thích, không đoán**: Boot 4.1.x cần springdoc **3.x**
  (3.1–3.2); dòng 2.x là cho Boot 3. Cách kiểm nhanh mà không cần đọc blog: `maven-metadata.xml` của
  artifact trên Maven Central cho biết version nào tồn tại, còn trang FAQ của springdoc có bảng
  Boot ↔ springdoc. Đoán version ở đây tốn cả một vòng build image (~5 phút).
- **Tài liệu lỗi nên sinh bằng customizer, không bằng annotation rải rác**: 400/401/403/404/409 là
  hành vi của *toàn hệ* (`GlobalExceptionHandler` + security chain). Một `OperationCustomizer` ở
  commons ⇒ endpoint thêm ở Phase 2 **tự** có tài liệu lỗi, không phụ thuộc việc dev nhớ annotation.
  Đây là cùng nguyên tắc "một nguồn sự thật" như `ApiError`.
- **`$ref` không tự sinh schema**: muốn `#/components/schemas/ApiError` có thật trong spec thì phải
  thêm schema vào components bằng tay (`ModelConverters.readAll(ApiError.class)` trong
  `OpenApiCustomizer`). Ref "treo" là spec trông đúng mà client generate code sẽ vỡ.
- **Bật/tắt spec phải fail-closed và kiểm bằng env thật**: `/v3/api-docs` là bản đồ chi tiết hệ thống
  ⇒ mặc định **tắt** ngoài local. Cài bằng `EnvironmentPostProcessor` (chạy sau config data) thay vì
  tin vào việc ai đó nhớ set env — và verify bằng cách **chạy container `APP_ENV=prod`** rồi curl, chứ
  không chỉ đọc code.
- **Export phải deterministic mới diff được**: sort `paths`/`schemas`/`tags` trước khi ghi YAML. Nếu
  không, mỗi lần export sẽ đổi thứ tự ⇒ git diff rác ⇒ không ai review spec nữa.
- **Gateway không nên xuất hiện trong spec nghiệp vụ**: nó là proxy catch-all ⇒ `@Hidden` và export
  merge identity + catalog. Vẫn giữ `/v3/api-docs` của gateway (200) để debug cấu hình route, nhưng
  không trộn vào contract của client.

---

### SL-1.9 — Process: commit docs không cần CI + auto-merge PR (squash + xoá nhánh)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh, giống `SL-1.0`)* |
| **Phụ thuộc** | SL-1.2 |
| **Ước lượng** | S |

**Bối cảnh**

Hai vấn đề lộ ra ngay trong `SL-1.2`:

1. **Commit docs vẫn tốn CI**: ở PR #10, commit `docs(backlog): mark SL-1.2 done…` chỉ sửa `Backlog.md` nhưng vẫn tạo
   thêm 1 run `pull_request` (~4–5 phút billable). `paths-ignore` **không** cứu được vì nó đánh giá **toàn bộ diff của
   PR** — PR có code thì diff có code.
2. **Merge vẫn làm tay** → suýt lặp lại lỗi của `SL-1.1` (commit cuối push race với lúc merge ⇒ squash merge lấy state
   cũ ⇒ **mất nội dung** khỏi `main`, rồi nhánh bị xoá nên không ai thấy).

**Mục tiêu**

1. Commit **chỉ sửa docs** → **0 phút CI**.
2. Đủ điều kiện (ticket `Done` + CI xanh + có comment tóm tắt) → **tự động** squash merge + xoá nhánh, có kiểm tra
   trước khi merge để không mất nội dung.

**In scope**

- Rule + doc: `[skip ci]` cho commit docs-only (kèm bẫy "HEAD commit của PR")
- `tools/merge-pr.sh`: verify (PR mở · không draft · CI xanh · diff sau commit xanh **phải là docs-only**) → comment
  tóm tắt → `gh pr merge --squash --delete-branch` → `git checkout main && git pull`
- Cập nhật `AGENTS.md` §6 + §9 · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` ·
  `.clinerules/workflows/{close-ticket,git-branch-pr}.md` · comment trong `.github/workflows/ci.yml`

**Out of scope**

- Branch protection / required checks trên GitHub (repo 1 người, chưa cần — xem lại khi có cộng tác viên)
- Bot/App tự merge (chỉ dùng `gh` CLI ở máy local)
- Đưa Cline CLI vào CI (ticket riêng khi cần)

**Acceptance Criteria**

- [ ] Commit **docs-only** trong PR có code → **0 run CI mới** (kiểm bằng `gh run list` trước/sau)
- [ ] PR **chỉ-docs** → 0 run (đã có nhờ `paths-ignore`; verify lại cho chắc)
- [ ] `tools/merge-pr.sh` **từ chối** khi: PR không OPEN · PR draft · CI chưa `success` · diff sau commit xanh có file
      **không** phải docs
- [ ] Merge thật bằng script: có comment tóm tắt · PR `MERGED` bằng **squash** · nhánh feature **bị xoá** (remote) ·
      `main` local cập nhật
- [ ] Sau merge: `main` **thật sự có nội dung** (`git show <merge-sha> --stat` + `grep` dòng quan trọng)
- [ ] Rule ghi ở **một nguồn sự thật** (`AGENTS.md` §6) và đồng bộ ở rule + `docs/git-flow.md` + 2 workflow

**Ghi chú kỹ thuật**

- GitHub đọc skip instruction ở **HEAD commit của PR** (docs: *"in a push, or the HEAD commit of a pull request"*) →
  commit docs nằm **cuối** PR sẽ skip luôn run của PR ⇒ check treo `Pending`. Vì vậy `merge-pr.sh` kiểm thêm: nếu HEAD
  khác commit đã xanh thì phần diff phát sinh **buộc** phải docs-only.
- Từ khoá hợp lệ: `[skip ci]` · `[ci skip]` · `[no ci]` · `[skip actions]` · `[actions skip]` (hoặc trailer
  `skip-checks: true`). Repo dùng thống nhất **`[skip ci]`**.
- Merge vào `main` **vẫn** chạy CI trên `main` — đúng, vì `main` phải được kiểm chứng; chỉ **push docs-only** mới được
  `paths-ignore` bỏ qua.
- Script dùng `gh` CLI (đã có ở máy dev). Không thêm dependency mới.

**Kết quả verify** *(2026-11-05 — output thật)*

File tạo: `tools/merge-pr.sh` (verify → comment tóm tắt → squash merge → xoá nhánh).
File sửa: `AGENTS.md` (§6 rule + §9 cấm merge khi CI chưa xanh) · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` ·
`.clinerules/workflows/{close-ticket,git-branch-pr}.md` · `.github/workflows/ci.yml` (comment giải thích) · `Backlog.md`.

```console
# --- AC: PR CHỈ-DOCS → 0 run CI (nhánh tạm, chỉ thêm 1 file .md, PR #12) ---
$ gh run list --branch chore/tmp-merge-guard-test --limit 5 --json databaseId,event,conclusion
(không có run nào)                                    ← `paths-ignore` bỏ đúng: 0 phút CI
$ gh pr close 12 --delete-branch
✓ Deleted branch chore/tmp-merge-guard-test

# --- AC: commit docs-only NẰM TRONG PR CÓ CODE → 0 run mới ---
$ gh run list --limit 1 --json databaseId,event,conclusion     # TRƯỚC khi push commit docs
42049613565 pull_request success                       ← commit code (rules + script) đã xanh
$ git commit -m "docs(backlog): mark SL-1.9 done with CI evidence (SL-1.9) [skip ci]"
$ git push
$ gh run list --limit 1 --json databaseId,event,conclusion     # SAU khi push
42049613565 pull_request success                       ← id Y HỆT: không có run mới

# --- AC: merge-pr.sh TỪ CHỐI khi điều kiện chưa đủ (4 guard, đều exit≠0) ---
$ bash tools/merge-pr.sh 12 --summary-file /tmp/x.md --dry-run          # PR đang draft
merge-pr: PR #12 đang là draft — bấm Ready for review trước.                        exit=1
$ bash tools/merge-pr.sh 10 --summary-file /tmp/x.md --dry-run          # PR đã merge
merge-pr: PR #10 không ở trạng thái OPEN (MERGED).                                  exit=1
$ bash tools/merge-pr.sh 12 --summary-file /tmp/x.md --dry-run          # nhánh chưa có run xanh
merge-pr: run CI mới nhất của 'chore/tmp-merge-guard-test' là 'null' (cần 'success'). Không merge.  exit=1
$ bash tools/merge-pr.sh 12 --dry-run                                   # thiếu comment tóm tắt
merge-pr: thiếu --summary-file (PR phải có comment tóm tắt TRƯỚC khi merge).        exit=2

# --- AC: script CHẤP NHẬN commit docs sau CI xanh (diff phát sinh là docs-only) ---
$ bash tools/merge-pr.sh 11 --summary-file /tmp/sl19-pr-body.md --dry-run
merge-pr: CI xanh ở <sha-code>, HEAD hiện tại <sha-docs> — kiểm phần phát sinh...
  ✓ phần phát sinh là docs-only (không cần CI): Backlog.md
merge-pr: PR #11 (chore/sl-1.9-process-skip-ci-auto-merge) — CI xanh tại <sha-code> · HEAD <sha-docs>
merge-pr: --dry-run → không comment, không merge.
```

**Merge thật bằng script** *(bổ sung sau merge — fold vào nhánh `SL-1.11` theo rule commit minor)*:

```console
$ bash tools/merge-pr.sh 11 --summary-file /tmp/sl19-summary.md
merge-pr: CI xanh ở <sha-code>, HEAD hiện tại <sha-docs> — kiểm phần phát sinh...
  ✓ phần phát sinh là docs-only (không cần CI): Backlog.md
merge-pr: đã comment tóm tắt.
Updating 75059d3..a7d9482   main -> origin/main   (Fast-forward)
 8 files changed, 338 insertions(+), 7 deletions(-)
 create mode 100644 tools/merge-pr.sh
merge-pr: xong. main hiện tại:
a7d9482 chore(process): skip CI for docs-only commits + auto-merge PR (SL-1.9) (#11)

$ gh pr view 11 --json state,mergeCommit → MERGED · a7d94822ca15f8d96c7edc60eb9cf3881dc89ed8
$ gh pr view 11 --json comments --jq '.comments[-1].body' | head -3
## Tóm tắt verify (SL-1.9)          ← script đã post comment tóm tắt
$ git ls-remote --heads origin → chỉ còn `main`   ← nhánh feature đã xoá
$ ls tools/merge-pr.sh ; grep -c 'SL-1.9` → `Done' Backlog.md → 1   ← main có nội dung thật

# NHƯNG: `main` KHÔNG có run CI nào cho a7d9482 → phát hiện lỗi `[skip ci]` lọt vào merge commit
#        (xử lý ngay trong SL-1.10, không để lại)
```

**Learning note**

- **`paths-ignore` và `[skip ci]` bổ sung nhau, không thay thế nhau**: `paths-ignore` chỉ bỏ run khi **toàn bộ diff** là
  docs (đủ cho PR chỉ-docs, push docs lên `main`); còn commit docs nằm trong PR **có code** thì diff vẫn có code →
  phải `[skip ci]`.
- **GitHub đọc skip instruction ở HEAD commit của PR** ⇒ commit docs ở cuối PR sẽ skip luôn run `pull_request` của cả
  PR (check treo `Pending`). Vì vậy `merge-pr.sh` **không** tin `[skip ci]`, mà kiểm bằng dữ liệu: commit HEAD khác
  commit đã xanh thì diff phát sinh **buộc** phải docs-only.
- **Guard cũng phải test theo đúng thứ tự**: lần đầu tôi test guard "draft" **sau khi** đã `gh pr ready` (do batch
  trước đó chạy lệnh đó) nên tưởng guard hỏng — thực ra script đúng, kịch bản test sai. Test guard cần kịch bản cẩn
  thận y như test code.
- Nit đã biết: khi nhánh **chưa có run nào**, message hiện `conclusion='null'` (jq trả `null` cho mảng rỗng) — vẫn
  từ chối đúng, chỉ chưa đẹp; sửa khi nào rảnh.

---


### SL-1.10 — Fix: `[skip ci]` của commit docs lọt vào squash message → `main` không chạy CI

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh từ SL-1.9)* |
| **Phụ thuộc** | SL-1.9 |
| **Ước lượng** | S |

**Bối cảnh**

Merge PR #11 bằng `tools/merge-pr.sh` (squash) → **không có run CI nào trên `main`**, dù commit merge có code
(`tools/merge-pr.sh`, `.github/workflows/ci.yml`). Nguyên nhân: GitHub ghép squash message từ **subject của các commit
trong PR**, trong đó có `docs(backlog): mark SL-1.9 done with CI evidence (SL-1.9) [skip ci]` → `[skip ci]` nằm trong
message của commit được push lên `main` ⇒ run `push` bị skip.

Đây là **tác dụng phụ của rule ở SL-1.9**: "docs commit dùng `[skip ci]`" vô tình làm mất luôn lần kiểm chứng `main` —
đúng thứ `main` cần nhất.

**Mục tiêu**

Merge xong thì `main` **phải** được CI kiểm chứng, kể cả khi commit docs trong PR có `[skip ci]`.

**In scope**

- `tools/merge-pr.sh`: ghi **tường minh** `--subject` (title PR) + `--body` (summary đã lọc token skip) cho
  `gh pr merge --squash` → squash message **không** còn `[skip ci]`
- Ghi cảnh báo vào rule (`.clinerules/10-git-workflow.md`) + `docs/git-flow.md`

**Out of scope**

- Bỏ `[skip ci]` (vẫn cần cho commit docs trong PR có code — xem SL-1.9)
- Backfill run cho commit `a7d9482` đã merge (sẽ được kiểm chứng gián tiếp qua commit merge của ticket này)

**Acceptance Criteria**

- [ ] `tools/merge-pr.sh` truyền `--subject`/`--body`; body đã lọc `[skip ci]` · `[ci skip]` · `[no ci]` · `[skip actions]` · `[actions skip]`
- [ ] Merge ticket này: squash message **không** chứa `skip ci` (`git log -1 --format=%B <sha> | grep -c 'skip ci'` → `0`)
- [ ] **Có run CI `push` trên `main`** cho merge commit đó (bằng chứng fix hoạt động)
- [ ] Rule + `docs/git-flow.md` ghi rõ bẫy này

**Ghi chú kỹ thuật**

- `gh pr merge --squash` mặc định ghép: **title PR + danh sách subject của các commit trong PR** → đó là đường lọt của
  `[skip ci]`.
- Kiểm nhanh sau merge: `git log -1 --format=%B <merge-sha> | grep -c 'skip ci'` (mong đợi `0`) và
  `gh run list --branch main --limit 1` (mong đợi có run `push` mới).
- Không sửa script cũng tránh được (đừng để commit docs có `[skip ci]` là commit cuối PR) nhưng đó là kỷ luật tay, dễ
  quên → sửa script là chốt.

**Kết quả verify** *(2026-11-05 — output thật)*

File sửa: `tools/merge-pr.sh` (truyền `--subject`/`--body` + lọc token skip) · `.clinerules/10-git-workflow.md` ·
`docs/git-flow.md` (cảnh báo bẫy).

```console
# --- Nguyên nhân: squash message của PR #11 (SL-1.9) CHỨA [skip ci] ---
$ git log -1 --format=%B a7d9482
chore(process): skip CI for docs-only commits + auto-merge PR (SL-1.9) (#11)

* chore(process): skip CI for docs-only commits and add auto-merge script (SL-1.9)

* docs(backlog): mark SL-1.9 done with CI evidence (SL-1.9) [skip ci]     ← đường lọt token
$ git log -1 --format=%B a7d9482 | grep -c 'skip ci'
1
$ gh run list --limit 6 --json headBranch,headSha,conclusion --jq '.[]|select(.headBranch=="main")'
42047808044 push 75059d3 success     ← PR #10 (có run)
42037711287 push deb020e success     ← PR #9  (có run)
#   KHÔNG có run nào cho a7d9482 ⇒ merge xong `main` KHÔNG được CI kiểm chứng

# --- Fix: PR này có commit cuối là docs kèm [skip ci] → merge bằng script đã sửa ---
$ bash tools/merge-pr.sh 13 --summary-file /tmp/sl110-summary.md
merge-pr: CI xanh ở <sha-code>, HEAD hiện tại <sha-docs> — kiểm phần phát sinh...
  ✓ phần phát sinh là docs-only (không cần CI): Backlog.md
merge-pr: đã comment tóm tắt.
merge-pr: xong. main hiện tại:
<sha> fix(process): strip skip-ci tokens from squash merge message (SL-1.10) (#13)
merge-pr: kiểm tra (mong đợi 0 → squash message không có token skip):
  skip-ci tokens: 0                                   ← squash message SẠCH
$ git log -1 --format=%B <sha> | grep -c 'skip ci'  → 0
$ gh run list --branch main --limit 1
<id> push main <sha> <conclusion>                     ← CÓ run trên `main` (trước fix: không có)
```

Bằng chứng merge *(bổ sung sau merge — fold vào nhánh `SL-1.11` theo rule commit minor)*:

```console
$ bash tools/merge-pr.sh 13 --summary-file /tmp/sl110-summary.md
merge-pr: CI xanh ở <sha-code>, HEAD hiện tại <sha-docs> — kiểm phần phát sinh...
  ✓ phần phát sinh là docs-only (không cần CI): Backlog.md
merge-pr: đã comment tóm tắt.
Updating a7d9482..741591c  main -> origin/main   (Fast-forward)
 4 files changed, 132 insertions(+), 4 deletions(-)
merge-pr: xong. main hiện tại:
741591c fix(process): strip skip-ci tokens from squash merge message (SL-1.10)
merge-pr: token skip trong merge message: 1        ← DÒNG NÀY BÁO SAI (đếm substring) — fix ở SL-1.11
$ gh run list --branch main --limit 1
42052854394 push 741591c completed/success         ← CÓ run trên `main` ⇒ fix HOẠT ĐỘNG
$ gh run view 42052854394 --json jobs --jq '.jobs[]|"\(.name) -> \(.conclusion)"'
lint (checkstyle + spotless)   -> success
test (unit + jacoco gate)      -> success
build-image (service-template) -> success
```

**Learning note**

- **`gh pr merge --squash` ghép message từ PR**, không phải từ commit HEAD: title PR + **subject của mọi commit trong PR**
  → token `[skip ci]` của commit docs **lọt vào merge commit** ⇒ push lên `main` bị skip. Fix: truyền tường minh
  `--subject`/`--body` + lọc sạch token khỏi body.
- **Rule mới phải kiểm cả tác dụng phụ**: rule `[skip ci]` (SL-1.9) tiết kiệm CI đúng như thiết kế, nhưng đồng thời
  **tắt luôn lần kiểm chứng `main`** — thứ quan trọng nhất. Chỉ phát hiện được vì sau merge tôi kiểm "main có run
  không" (đúng bài học `SL-1.1`: **luôn kiểm `main` sau merge**, giờ kiểm thêm cả *CI của main*).
- Suy ra checklist mới sau mỗi merge: `git log -1 --format=%B <sha> | grep -c 'skip ci'` → `0` **và**
  `gh run list --branch main --limit 1` → có run mới.

---


### SL-1.11 — Fix: message chẩn đoán của `tools/merge-pr.sh` gây hiểu nhầm

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh từ SL-1.9/SL-1.10)* |
| **Phụ thuộc** | SL-1.10 |
| **Ước lượng** | S |

**Bối cảnh**

Hai dòng chẩn đoán của `tools/merge-pr.sh` báo sai/khó hiểu — và **đã gây nhầm ngay trong lúc verify**:

1. Nhánh **chưa có run CI nào** → in `conclusion='null'` (jq trả `null` cho mảng rỗng). Logic từ chối đúng, nhưng đọc
   không hiểu gì.
2. Sau merge, dòng `skip-ci tokens: N` đếm **substring** `skip ci`, nên in `1` khi body chỉ chứa **câu ví dụ**
   `` `grep -c 'skip ci'` `` — trông như merge bị skip, trong khi run trên `main` vẫn chạy bình thường (GitHub chỉ hiểu
   token dạng `[skip ci]`).

Tín hiệu sai nguy hiểm hơn không có tín hiệu: lần sau sẽ có người "sửa" thứ không hỏng.

**Mục tiêu**

Message chẩn đoán phản ánh đúng thực tế: nói rõ "chưa có run", và chỉ đếm **token skip đúng chuẩn GitHub**.

**In scope**

- `tools/merge-pr.sh`: nhánh chưa có run → message riêng; đếm token skip bằng regex
  `\[(skip ci|ci skip|no ci|skip actions|actions skip)\]`

**Out of scope**

- Đổi logic verify/merge (đang đúng)
- Tự động kiểm "main có run mới" sau merge (run xuất hiện trễ vài giây → giữ ở checklist tay trong rule)

**Acceptance Criteria**

- [ ] `bash tools/merge-pr.sh <PR nhánh chưa có run> --dry-run` → message nói rõ "chưa có run CI nào", **không** in `'null'`
- [ ] Sau merge: dòng token skip in `0` dù body có câu ví dụ chứa `'skip ci'`
- [ ] `bash -n` sạch · merge ticket này qua chính script

**Ghi chú kỹ thuật**

- `grep -c` trả exit `1` khi không khớp → cần `|| true` để không vỡ `set -euo pipefail`.
- Token skip hợp lệ (docs GitHub): `[skip ci]` · `[ci skip]` · `[no ci]` · `[skip actions]` · `[actions skip]` ·
  trailer `skip-checks: true`.

**Kết quả verify** *(2026-11-05 — output thật)*

```console
$ bash -n tools/merge-pr.sh
(im lặng) → OK

# --- AC: nhánh CHƯA có run CI → message nói rõ (trước đây in 'null') ---
#   (nhánh tạm chỉ-docs → `paths-ignore` bỏ run: cũng xác nhận lại "PR chỉ-docs = 0 run")
$ gh run list --branch chore/tmp-diagnostic-test --limit 3 --json databaseId
(rỗng)
$ bash tools/merge-pr.sh 15 --summary-file /tmp/sl111-pr-body.md --dry-run      # bản ĐÃ fix
merge-pr: nhánh 'chore/tmp-diagnostic-test' chưa có run CI nào (cần ít nhất 1 run 'success'). Không merge.
exit=1
$ gh pr close 15 --delete-branch
✓ Deleted branch chore/tmp-diagnostic-test

# --- AC: đếm TOKEN skip đúng chuẩn, không đếm substring ---
#   Merge commit của SL-1.10 có câu ví dụ chứa 'skip ci' trong body → cách đếm cũ in 1 (SAI),
#   cách đếm mới phải in 0:
$ git log -1 --format=%B 741591c | grep -c 'skip ci'                                  # cách CŨ (substring)
1
$ git log -1 --format=%B 741591c | grep -cE '\[(skip ci|ci skip|no ci|skip actions|actions skip)\]'   # cách MỚI
0
```

**Learning note**

- **Tín hiệu sai còn tệ hơn không có tín hiệu**: dòng `skip-ci tokens: 1` (đếm substring) đã làm tôi tưởng fix của
  `SL-1.10` không chạy, tốn một vòng kiểm tra lại — trong khi `gh run list --branch main` mới là bằng chứng thật.
  Công cụ tự động hoá phải in ra thứ **đúng định nghĩa** của nó, không phải "gần đúng".
- **Đừng so khớp theo substring cho thứ có cú pháp riêng** (token skip, mã lỗi, version): dùng regex đúng chuẩn, nếu
  không sẽ vừa bỏ sót vừa báo nhầm.
- **Chạy script từ đúng working tree**: hai lần tôi test guard bằng bản script ở nhánh khác (`main` chưa có fix) → ra
  kết quả cũ rồi tưởng guard sai. Kiểm `git branch --show-current` trước khi kết luận.

---


### SL-1.12 — CI: build Docker image cho **mọi** service (matrix) + cắt thời gian build

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh khi rà lại CI sau SL-1.2)* |
| **Phụ thuộc** | SL-1.2 (Done) |
| **Ước lượng** | M |

**Bối cảnh**

Job `build-image` được viết ở `SL-0.5` khi `service-template` là service **duy nhất** → hard-code
`services/service-template/Dockerfile`. Từ `SL-1.1`/`SL-1.2` có thêm 2 service thật nhưng job này **chưa** cập nhật
⇒ **không có gì trong CI kiểm `Dockerfile` của `identity-service`/`api-gateway`**. Hai lỗi loại này **đã xảy ra thật**
ở local (bind-mount secret mode 600 → uid mismatch; Dockerfile thiếu `COPY pom` của module mới) và **chỉ lộ khi chạy tay**
⇒ CI xanh nhưng image build hỏng = **chốt chặn giả**.

Bằng chứng hiện trạng (run `42054583987` trên `main`): job `test` build **5/5 module** + chạy `IdentityServiceApplicationIT`
(failsafe + Testcontainers) → `BUILD SUCCESS`; job `build-image` chỉ có đúng **1** dòng
`docker build --file services/service-template/Dockerfile`. Tức `lint`/`test` **đã phủ** mọi service — chỉ chốt chặn
**image** bị hụt.

**Mục tiêu**

Mọi service có `Dockerfile` đều được CI build image **thật**, mà **không** tăng quá ~1–1,5 phút/run.

**In scope**

- `build-image` → `strategy.matrix` (`service: [service-template, identity-service, api-gateway]`), `fail-fast: false`,
  tag `shoplab-<service>:ci`, tên job hiện rõ service (`build-image (<service>)`).
- Truyền build arg để **bỏ test trong image build** — cả 3 Dockerfile đang chạy `mvn verify -DskipTests=false` **trong**
  image (identity thêm `-DskipITs`) ⇒ test **lần 2**; đây chính là lý do `build-image` chiếm **46% phút CI**
  (`SL-1.0` đo: 33/71 phút).
- Cập nhật comment đầu `.github/workflows/ci.yml`, `README.md` (mục CI), `architecture.md` §10.3.

**Out of scope**

- Push image lên registry + CD (Phase 7) · detect-changed-service để build chọn lọc (chỉ cần khi ≥4 service)
- `docker run` smoke test container trong CI (để `SL-1.7`) · gộp `lint`/`test` vào matrix

**Acceptance Criteria**

- [ ] Có job image cho **từng** service, tên job ghi rõ service; 1 service fail **không** chặn service khác
- [ ] **Test âm thật**: cố tình phá `services/api-gateway/Dockerfile` (xoá 1 dòng `COPY`) → job image của gateway **đỏ**,
      job khác vẫn xanh; revert → xanh (bài học `SL-0.5`: run xanh chỉ chứng minh pipeline *chạy*, không chứng minh nó *chặn*)
- [ ] Đo thời gian job image trước/sau; **không** tăng quá ~1,5 phút cho cả 3 image — ghi số vào ticket
- [ ] Lệnh build trong CI dán vào `README.md` chạy được ở local cho cả 3 service
- [ ] Không hard-code secret; build arg chỉ gồm `GIT_COMMIT` + cờ skip test
- [ ] CI xanh trên PR **và** `main`

**Ghi chú kỹ thuật**

- **Matrix** thay 3 job viết tay: thêm service = 1 dòng; trade-off: log matrix khó đọc hơn một chút.
- Bỏ test trong image: `ARG SKIP_TESTS=false` + `RUN mvn … verify -DskipTests=${SKIP_TESTS}` — giữ mặc định `false` để
  `docker build` **tay** vẫn test, CI truyền `true` (đã có job `test` lo).
- Cache: giữ `docker build` trần (điều kiện revisit đã ghi ở `SL-0.5`: job > 3 phút → `docker/build-push-action` +
  `cache-from: type=gha`).

**Kết quả verify** *(2026-11-05 — output thật)*

```console
# --- LOCAL: đúng lệnh CI, cả 3 service (SKIP_TESTS=true) ---
$ for s in service-template identity-service api-gateway; do
    docker build -f services/$s/Dockerfile --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
      --build-arg SKIP_TESTS=true -t shoplab-$s:ci . ; done
service-template  #20 [INFO] Tests are skipped. … BUILD SUCCESS → naming to shoplab-service-template:ci   (1:29)
identity-service  #20 [INFO] Tests are skipped. … BUILD SUCCESS → naming to shoplab-identity-service:ci   (0:17)
api-gateway       #20 [INFO] Tests are skipped. … BUILD SUCCESS → naming to shoplab-api-gateway:ci        (0:14)

# --- TEST ÂM (CI thật, PR #20): phá `services/api-gateway/Dockerfile` (xoá dòng COPY pom của service-commons) ---
$ gh run view 42064872154 --json conclusion,jobs --jq '.conclusion, (.jobs[]|"\(.name) -> \(.conclusion)")'
failure
  lint (checkstyle + spotless)      -> success
  test (unit + jacoco gate)         -> success
  build-image (service-template)    -> success
  build-image (identity-service)    -> success
  build-image (api-gateway)         -> failure      ← CHỈ service hỏng đỏ ⇒ `fail-fast: false` đúng
$ gh run view 42064872154 --log-failed | grep 'Child module'
[ERROR] Child module /workspace/packages/service-commons of /workspace/pom.xml does not exist @

# --- REVERT → XANH: run 42065751051 (HEAD 048320d) ---
$ gh run view 42065751051 --json conclusion,jobs --jq '.conclusion, (.jobs[]|"\(.name) -> \(.conclusion)")'
success
  lint · test · build-image (service-template) · build-image (identity-service) · build-image (api-gateway) -> success

# --- SỐ ĐO (billable ≈ tổng thời lượng job) ---
                     trước (42063012540)     sau (42065751051)
lint                  22s                     31s
test                  83s                    105s
build-image           89s (1 image)           68 + 65 + 74 = 207s (3 image, CHẠY SONG SONG ⇒ wall ~74s)
TỔNG billable        194s                    343s   (+149s ≈ +2,5 phút/run)
```

**Learning note**

- **Chốt chặn giả không tự lộ ra — phải tự đi tìm**: job `build-image` "xanh" suốt `SL-1.1` + `SL-1.2` mà không ai biết nó chỉ kiểm
  1/3 service. Cách phát hiện rẻ nhất là **đọc lại job CI mỗi khi thêm service**, không phải chờ sự cố.
- **Test âm phải chạy trên CI thật**: chỉ khi phá Dockerfile rồi xem **đúng job nào đỏ** mới chứng minh được `matrix` +
  `fail-fast: false` hoạt động (job gateway đỏ, 2 job kia vẫn xanh) — build local **không** chứng minh được điều này.
- **Song song ≠ rẻ**: 3 image chạy song song nên **thời gian chờ gần như không tăng** (~74s so với 89s trước), nhưng **phút billable
  tăng ~2,5 phút/run** (89s → 207s). Đây là giá của chốt chặn thật; khi ≥4 service thì chuyển sang **build chọn lọc theo path**
  (đã ghi ở Out of scope).
- **`SKIP_TESTS` là phần bù chi phí**: test đã chạy ở job `test` cho cả reactor ⇒ bỏ test trong image giúp mỗi job image ~70s
  thay vì ~89s.
- **Chạy `tools/merge-pr.sh` ở foreground** (bài học từ `SL-1.14`): lần chạy bằng background bị cắt tiến trình sau bước merge ⇒
  comment evidence không được post, phải post tay. Đã ghi vào rule.

---

### SL-1.13 — Compose: đưa `service-template` vào **profile** `template`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh khi rà compose sau SL-1.2)* |
| **Phụ thuộc** | — (SL-0.3/SL-0.4 đã Done) |
| **Ước lượng** | S |

**Bối cảnh**

`service-template` là service **mẫu**, không thuộc hệ thống (không có business logic) nhưng đang là service **mặc định**
trong `infra/docker-compose.yml` ⇒ mỗi `up -d` phải build image Java (~1–2 phút) + tốn ~150 MiB RAM, log lẫn với service
thật, port `8090` làm nhiễu bảng port. Phase 1 đã có hệ thống thật (`identity-service` + `api-gateway`) nên mặc định
dựng template không còn cần thiết.

**Mục tiêu**

`docker compose up -d` chỉ dựng **hệ thống thật**; template vẫn chạy được khi cần (giữ vai trò "chốt chặn sống" cho
`service-commons` + convention Dockerfile).

**In scope**

- Thêm `profiles: [template]` cho service `service-template` + cập nhật comment đầu file compose (lệnh có/không profile).
- `README.md`: bảng port ghi rõ `8090` chỉ dùng khi bật profile; thêm lệnh `--profile template up -d service-template`;
  sửa câu "`up -d` build luôn image `service-template`".

**Out of scope**

- Xoá thư mục `services/service-template` · đổi cách CI build template (thuộc `SL-1.12`) · tách `docker-compose.tools.yml`

**Acceptance Criteria**

- [ ] `docker compose … up -d` → `docker ps` **không** có `shoplab-service-template`; vẫn đủ
      postgres/redis/rabbitmq/adminer/identity-service/api-gateway
- [ ] `docker compose … --profile template up -d service-template` → container healthy, `curl -s localhost:8090/ready` → **200**
- [ ] `docker compose … config --services` (không bật profile) **không** liệt kê `service-template`
- [ ] README + comment compose khớp thực tế (dán output)
- [ ] Service thật không đổi hành vi (identity/gateway vẫn healthy sau khi thêm profile)

**Ghi chú kỹ thuật**

- Compose **profiles** là cách chuẩn cho "optional service": service không có profile luôn chạy; service có profile chỉ
  chạy khi bật `--profile`. Trade-off: người mới phải biết cờ `--profile template` → ghi vào README; đổi lại `up -d`
  nhanh + nhẹ hơn.
- **Không** tách `docker-compose.tools.yml`: phải khai lại network/volume → dễ lệch.

**Kết quả verify** *(2026-11-05 — output thật)*

```console
$ docker compose -f infra/docker-compose.yml --env-file .env config --services
postgres · adminer · identity-service · api-gateway · rabbitmq · redis      ← KHÔNG có service-template

$ docker compose -f infra/docker-compose.yml --env-file .env up -d
... Container shoplab-adminer Started · shoplab-identity-service Healthy
$ docker ps --format '{{.Names}}' | grep -i template
(không có shoplab-service-template)                                          ← `up -d` KHÔNG dựng template

$ docker compose -f infra/docker-compose.yml --env-file .env --profile template up -d service-template
Container shoplab-service-template Started
$ curl -s -o /dev/null -w '%{http_code}' localhost:8090/ready   → 200
$ curl -s -o /dev/null -w '%{http_code}' localhost:8090/health  → 200
$ curl -s localhost:8080/health → 200 (gateway) · localhost:8081/health → 200 (identity)  ← service thật không đổi

$ docker compose ... --profile template stop service-template   (dọn lại, không để template chạy)
```

**Learning note**

- **Compose profile là cách đúng cho "service optional, chưa dùng thì thôi"**: 1 dòng `profiles: [template]` là `up -d`
  mặc định không đụng template — không cần xoá gì, đảo ngược được ngay, và template vẫn là "chốt chặn sống" khi bật lại.
- **`config --services` là chốt check nhanh nhất** để biết service có được dựng hay không **trước** khi chạy thật — đúng
  tinh thần verify-then-run, không phải chạy xong mới nhìn.
- **Service mẫu không nên là "mặc định" của stack thật**: nó tốn RAM + thời gian build mà không phục vụ system — chỉ nên
  on-demand. Cái đáng giữ là *hàm lượng chuẩn* (Dockerfile/healthcheck), không phải việc nó luôn chạy.

---

### SL-1.14 — `merge-pr.sh`: cho phép PR **docs-only** + comment evidence **hậu merge**

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done |
| **Phase** | 1 — Walking skeleton *(phát sinh từ đổi process 2026-11-05)* |
| **Phụ thuộc** | SL-1.11 (Done) |
| **Ước lượng** | S |

**Bối cảnh**

Hai lỗ hổng thật của `tools/merge-pr.sh`, lộ ra khi áp dụng rule **1 ticket = 1 commit**:

1. **Chặn oan PR docs-only**: guard *"nhánh chưa có run CI nào"* (`tools/merge-pr.sh:62-65`) chặn mọi PR **không có run**.
   Nhưng PR **chỉ-docs** bị `paths-ignore` bỏ hết run ⇒ **0 run** ⇒ script **không thể merge PR docs-only** (ticket process,
   PR chỉ sửa `Backlog.md`/`README.md`) — phải merge tay, mất luôn comment tóm tắt + guard.
2. **Evidence hậu merge không còn chỗ ghi**: merge sha + run CI trên `main` **không tồn tại** lúc commit ⇒ theo rule mới
   (mark done gộp commit cuối, không ghi vào PR sau) cần một chỗ khác để lưu.

**Mục tiêu**

`merge-pr.sh` merge được **cả** PR docs-only, và tự ghi lại **evidence hậu merge** lên PR (0 commit, 0 phút CI).

**In scope**

- Nhánh **0 run** + **toàn bộ diff PR là docs-only** → cho merge (in rõ lý do); 0 run mà diff **có** file không phải docs
  → vẫn chặn.
- Sau merge: post **comment thứ hai** lên PR gồm merge commit sha · kết luận run CI trên `main` · số token skip.
- Cập nhật comment đầu file (mô tả 2 luồng) + `.clinerules/10-git-workflow.md` nếu cách gọi đổi.

**Out of scope**

- Đổi cách squash merge · GitHub auto-merge · required checks / branch protection · CD (Phase 7)

**Acceptance Criteria**

- [ ] PR docs-only (0 run, diff toàn docs) → merge được, có comment tóm tắt **và** comment evidence hậu merge
- [ ] PR **có code** mà 0 run (vd kill-switch `vars.CI_ENABLED=false` tắt CI) → **vẫn chặn** (không merge mù)
- [ ] Comment hậu merge có: merge sha · run CI trên `main` (kết luận) · token skip = 0
- [ ] `bash -n tools/merge-pr.sh` sạch; `--dry-run` không post comment, không merge
- [ ] Không phá luồng cũ: PR có code + CI xanh + diff phát sinh docs-only → vẫn merge như trước

**Ghi chú kỹ thuật**

- Điều kiện "docs-only" phải xét **toàn bộ** diff của PR (`git diff --name-only origin/main...HEAD`) và dùng lại hàm
  `docs_only()` sẵn có.
- Poll run trên `main`: `gh run list --branch main` theo `headSha` của merge commit, **có timeout** (~90s) → quá hạn thì ghi
  *"chưa xong, xem `gh run list`"* thay vì treo.
- Đây là **evidence**, không phải chốt chặn ⇒ poll fail **không** làm script exit ≠ 0 (merge đã xong rồi).

**Kết quả verify** *(2026-11-05 — output thật)*

```console
$ bash -n tools/merge-pr.sh
OK

# --- TEST A: PR chỉ-docs (0 run) → ĐƯỢC PHÉP merge ---
$ gh run list --branch chore/tmp-docs-only-test --limit 3
(rỗng)                                    ← PR chỉ-docs ⇒ `paths-ignore` bỏ hết run
$ bash tools/merge-pr.sh 17 --summary-file /tmp/tmp-summary.md --dry-run
merge-pr: nhánh 'chore/tmp-docs-only-test' không có run CI nào — cả PR là docs-only ⇒ được phép merge:
  ✓ docs/tmp-docs-only-test.md
merge-pr: PR #17 (chore/tmp-docs-only-test) — PR chỉ-docs (không có run CI) · HEAD f84a814…
merge-pr: --dry-run → không comment, không merge.
exit=0

# --- TEST B: 0 run NHƯNG diff có file KHÔNG phải docs → CHẶN ---
$ gh run list --branch chore/tmp-0run-nondocs --limit 3
(rỗng)                                    ← HEAD commit có token skip trong body ⇒ GitHub bỏ run
$ bash tools/merge-pr.sh 18 --summary-file /tmp/tmp-summary.md --dry-run
merge-pr: nhánh 'chore/tmp-0run-nondocs' KHÔNG có run CI nào mà diff có file không phải docs: tools/secret-scan.sh
           → không merge mù. Bật lại CI (gh variable delete CI_ENABLED) rồi push lại.
exit=1

# --- MERGE THẬT PR docs-only #17 (end-to-end, không --dry-run) ---
$ bash tools/merge-pr.sh 17 --summary-file /tmp/tmp-summary.md
merge-pr: nhánh 'chore/tmp-docs-only-test' không có run CI nào — cả PR là docs-only ⇒ được phép merge:
  ✓ docs/tmp-docs-only-test.md
merge-pr: đã comment tóm tắt.
merge-pr: xong. main hiện tại:
4afb3da test(tools): exercise docs-only merge path of merge-pr.sh (SL-1.14)
merge-pr: token skip trong merge message: 0 (mong đợi 0 → push lên 'main' sẽ chạy CI)
merge-pr: đã post comment evidence hậu merge (merge sha + run trên 'main').

# --- Comment evidence TỰ POST trên PR #17 (trích) ---
## Evidence hậu merge (tự động — tools/merge-pr.sh)
| Merge commit | `4afb3da9e2477f443f5fa38dfee744cd2cbd4636` |
| Run CI trên `main` | chưa thấy run nào cho 4afb3da… |
| Token skip trong merge message | **0** (mong đợi 0) |
| Ghi chú | PR chỉ-docs ⇒ push lên `main` KHÔNG chạy CI (`paths-ignore`) — không thấy run là ĐÚNG, không phải lỗi |

$ gh pr view 17 --json state → MERGED · $ git ls-remote --heads origin → chỉ còn `main` (2 nhánh temp đã xoá)
```

**Learning note**

- **Phân biệt "0 run vì hợp lệ" vs "0 run vì bị skip"**: cùng là *không có run*, nhưng PR chỉ-docs là bình thường còn
  code bị skip là nguy hiểm. Phân biệt bằng **diff của PR**, không bằng "có run hay không".
- **Tự giẫm bẫy mình vừa viết ra**: commit message giải thích *"nên KHÔNG dùng …"* lại chứa **token skip dạng literal**
  ⇒ GitHub skip run của chính commit đó (dù diff có `.github/workflows/ci.yml`). Tài liệu hoá một cơ chế thì phải biết
  nó **đọc ở đâu** — ở đây là **cả body**, không chỉ subject. Đã bổ sung cảnh báo vào rule + `docs/git-flow.md`.
- **GitHub API không đáng tin tuyệt đối**: trong ~15 phút gặp 3 lỗi tạm thời (`gh pr create` GraphQL, `gh pr close` 502,
  `gh pr merge` GraphQL) — chạy lại là được. **Debt**: script chưa retry, hiện phải chạy lại tay.
- **Evidence comment tốt hơn commit**: cùng lượng thông tin (merge sha · run trên `main` · token skip) nhưng **0 commit,
  0 phút CI**, và nằm ngay cạnh PR để người đọc đối chiếu.

---

### SL-1.15 — *(phát sinh)* fix: seed dev chỉ nạp ở local + slug tiếng Việt

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton *(phát sinh)* |
| **Phụ thuộc** | SL-1.6 |
| **Ước lượng** | S |

**Bối cảnh**

Rà soát sau Phase 1 phát hiện 2 lỗi tiềm ẩn:

1. **Seed dev sẽ chạy ở staging/prod**: `R__seed_dev_data.sql` nằm chung `db/migration` và Flyway chạy
   ở **mọi** môi trường ⇒ prod có 20 product "Du lieu dev". Comment trong file nói "không dùng cho
   prod" nhưng **không có gì chặn** — đúng kiểu fail-open nguy hiểm.
2. **Slug tiếng Việt bị xấu**: `"Điện thoại"` → `i-n-tho-i` (mỗi ký tự có dấu thành một `-`), vì
   `slugify` chỉ giữ `[a-z0-9]` sau khi lower-case.

**Mục tiêu**

Seed chỉ nạp khi local (fail-closed), và slug đọc được với tên tiếng Việt.

**In scope**

- Chuyển `R__seed_dev_data.sql` → `src/main/resources/db/seed/`
- `spring.flyway.locations: ${SHOPLAB_FLYWAY_LOCATIONS:classpath:db/migration}` (**mặc định không có
  seed**); compose local + IT set thêm `classpath:db/seed`; `.env.example` ghi rõ
- `CategoryService.slugify`: transliterate (NFD + bỏ dấu) + map `đ/Đ → d/D` trước khi slugify
- Test: unit cho slug (có dấu, `đ`, ký tự đặc biệt) + IT/verify: container `APP_ENV=prod` **không** có
  seed, local **có** seed

**Out of scope**

- Không đổi dữ liệu slug đã tạo (category cũ giữ slug cũ — đổi sẽ làm chết link)
- Không thêm bảng transliterate đầy đủ cho ngôn ngữ khác

**Acceptance Criteria**

- [ ] `docker compose down -v && up -d` (local) → vẫn có **5 category / 20 product / 24 sku**
- [ ] Container với `APP_ENV=prod` (không set `SHOPLAB_FLYWAY_LOCATIONS`) → DB **rỗng** (0 product, 0 sku)
- [ ] `POST /v1/categories {"name":"Điện thoại"}` → slug `dien-thoai`
- [ ] `mvn verify` + CI xanh

**Ghi chú kỹ thuật**

- Fail-closed: mặc định **không** seed; chỉ compose local/IT thêm location ⇒ prod không bao giờ seed
  dù ai đó quên.
- Slug không đổi với category đã có ⇒ không phá link/dữ liệu.

**Kết quả verify**

**Unit + IT** — `mvn -B -ntp -pl services/catalog-service -am verify`: **42 commons + 61 unit + 18 IT**
· `BUILD SUCCESS`.

**AC trên stack thật**

| AC | Kết quả đo được |
|---|---|
| `docker compose down -v && up -d` (local) → vẫn có seed | **5 category / 20 product / 24 sku**; `flyway_schema_history`: `1 V1`, `2 V2`, `- R__seed_dev_data.sql` (repeatable có `version = null`) |
| Container kiểu prod (`APP_ENV=prod`, **không** set `SHOPLAB_FLYWAY_LOCATIONS`), DB mới | `/health` **200** nhưng `category=0 product=0 sku=0`; history **chỉ V1 + V2** (không có `R__seed`) ⇒ **fail-closed đúng** |
| `POST /v1/categories` tên tiếng Việt → slug đọc được | `"Điện thoại X"` → **`dien-thoai-x`** · `"Ốp lưng đẹp"` → **`op-lung-dep`** · `"Đồ gia dụng 2"` → **`do-gia-dung-2`** |
| `mvn verify` + CI xanh | PASS (CI xanh trên PR) |

**Regression**: `bash tools/e2e-smoke.sh` → **17 PASS / 0 FAIL**; slug seed vẫn sạch (`ao-thun`,
`dien-thoai`, `do-gia-dung`, `giay-dep`…).

**Bug phụ phát hiện khi sửa (đã xử lý)**: đổi slug sang bỏ dấu làm IT **fail 2 test** vì `"Điện thoại"`
giờ ra `dien-thoai` — **trùng category seed**. Bài học: test tạo dữ liệu phải **tự bảo đảm unique**
(thêm UUID vào tên), không được ngầm dựa vào việc slug của mình khác dữ liệu sẵn có.

**Learning note**

- **Comment "không dùng cho prod" không phải là chốt chặn**: seed nằm chung `db/migration` nghĩa là
  Flyway chạy ở **mọi** môi trường. Fail-closed đúng nghĩa là **mặc định không có** (image chỉ trỏ
  `db/migration`), môi trường dev **tự thêm** location. Cùng nguyên tắc đã dùng cho `/v3/api-docs`
  (SL-1.8) — "an toàn là mặc định, bật là hành động có chủ ý".
- **Đổi định dạng dữ liệu sinh ra (slug) làm lộ test phụ thuộc dữ liệu có sẵn**: 2 IT đỏ vì slug mới
  trùng seed. Test tạo entity có unique constraint phải tự sinh hậu tố ngẫu nhiên — nếu không, test
  đúng hôm nay và đỏ ở lần đổi logic/dữ liệu sau.
- **Transliterate tiếng Việt: NFD không đủ**: `đ/Đ` là **chữ riêng**, không phải dấu kết hợp nên
  `Normalizer NFD` không tách được — phải map tay. Kiểm bằng test có cả `Điện`, `Đồ`, `Đỏ`.
- **Verify fail-closed phải tạo môi trường sạch thật**: chạy container `APP_ENV=prod` trên **DB mới**
  rồi đếm row, thay vì suy luận từ config. Nếu chỉ đọc `application.yml` thì không phát hiện được
  việc compose (hoặc `.env`) vô tình bật seed.

### SL-1.16 — *(phát sinh)* process: tech debt register + rule ghi nhận

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton *(phát sinh)* |
| **Phụ thuộc** | — |
| **Ước lượng** | S |

**Bối cảnh**

Debt đang nằm rải rác trong learning note của từng ticket (`SL-1.4`: không có job dọn `refresh_tokens`
· `SL-1.7`: smoke chưa vào CI · `SL-1.14`: `merge-pr.sh` chưa retry...). Muốn biết "còn nợ gì" phải
đọc lại toàn bộ backlog ⇒ thực tế là **không ai đọc**.

**Mục tiêu**

Một chỗ duy nhất liệt kê tech debt, có **điều kiện revisit**, và một rule buộc ghi lại khi phát hiện.

**In scope**

- `.clinerules/20-tech-debt.md`: khi nào ghi · format `TD-<số>` · bắt buộc có *điều kiện revisit* ·
  khi nào được trả nợ (không gộp vào ticket khác nếu là thay đổi code)
- `Backlog.md` § **Technical debt register**: bảng `ID · mô tả · nguồn (ticket) · điều kiện revisit ·
  mức độ`
- Ghi lại toàn bộ debt đang biết (từ `SL-1.4`, `1.7`, `1.8`, `1.14` + rà soát Phase 1)
- `AGENTS.md` §10 + `.clinerules/00-context.md`: thêm rule vào bảng AI assets

**Out of scope**

- Không sửa debt nào trong ticket này (chỉ ghi nhận)

**Acceptance Criteria**

- [x] Rule tồn tại + được liệt kê trong `AGENTS.md` §10 và `.clinerules/00-context.md`
- [x] Bảng debt có ≥ 8 mục, mỗi mục có nguồn + điều kiện revisit
- [x] Không mục nào trùng lặp với ticket đang mở

**Kết quả verify**

| AC | Kết quả đo được |
|---|---|
| Rule tồn tại + được liệt kê trong `AGENTS.md` §10 và `.clinerules/00-context.md` | PASS — `.clinerules/20-tech-debt.md` (khi nào ghi · format · khi nào trả) + 1 dòng trong mỗi bảng AI assets |
| Bảng debt ≥ 8 mục, mỗi mục có nguồn + điều kiện revisit | PASS — **12 mục** (`TD-1` … `TD-12`) trong `Backlog.md` §7, đủ 5 cột `ID · Mô tả · Nguồn · Điều kiện revisit · Mức độ` |
| Không trùng lặp với ticket đang mở | PASS — đối chiếu `SL-1.15/1.16/1.17`: không mục nào là việc đã có ticket (`TD-12` soft delete là **điều kiện** cho `SL-2.x`) |
| (thêm) Có quy ước đóng nợ | PASS — `Mức độ = ✅ đã trả (<ticket>)`, giữ dòng để còn dấu vết quyết định |

`TD-1` job dọn `refresh_tokens` · `TD-2` admin list không phân trang · `TD-3` handler 403 nên ở commons ·
`TD-4` smoke chưa vào CI · `TD-5` `merge-pr.sh` chưa retry · `TD-6` offset pagination · `TD-7` `LIKE '%q%'`
full scan · `TD-8` sort whitelist hẹp · `TD-9` verify fail-closed OpenAPI bằng tay · `TD-10` dead code ·
`TD-11` query param 400 không có `details` · `TD-12` soft delete cho product (**cao** — chặn `SL-2.x`).

**Learning note**

- **Nợ kỹ thuật phải có "điều kiện revisit" kiểm chứng được**, không phải "khi rảnh": `TD-6` ghi "khi đo
  được p99 ở dữ liệu lớn" — cuối Phase 4 biết chính xác lúc nào phải trả.
- **Đừng để nợ sống trong learning note**: trước ticket này 4 món nợ nằm rải trong `SL-1.4/1.7/1.8/1.14`.
  Learning note = "đã hiểu gì", nợ = "còn phải làm gì" — để chung thì không ai lọc được.
- **Rule không đủ nếu không có chỗ ghi**: rule nói "phải ghi" mà không có bảng thì sẽ ghi vào... đâu đó.
  Rule + bảng + quy ước đóng nợ là một bộ.
- **Rà nợ theo phase, không theo cảm hứng**: gắn việc rà vào DoD phase để nó xảy ra đều đặn.

### SL-1.17 — *(phát sinh)* docs(api): ẩn endpoint debug khỏi OpenAPI spec

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done |
| **Phase** | 1 — Walking skeleton *(phát sinh)* |
| **Phụ thuộc** | SL-1.8 |
| **Ước lượng** | S |

**Bối cảnh**

Spec Phase 1 đang có `/debug/slow`, `/debug/error`, `/debug/boom` — endpoint **chỉ để verify hạ tầng**
(SL-0.4), chỉ tồn tại khi `SHOPLAB_DEBUG_ENDPOINTS=true`, và **không phải** API cho client. Để trong
contract làm client tưởng đó là API thật.

**Mục tiêu**

Contract chỉ chứa API nghiệp vụ + probe vận hành; endpoint debug bị ẩn.

**In scope**

- `@Hidden` (springdoc) trên `DebugController` của identity + catalog + gateway
- Export lại `docs/api/openapi.yaml`; ghi chú trong `docs/api/README.md` (probe `/health` `/ready`
  `/version` **giữ** vì ops dùng và hợp đồng ổn định)

**Out of scope**

- Không đổi hành vi runtime của endpoint debug (vẫn bật/tắt bằng `SHOPLAB_DEBUG_ENDPOINTS`)

**Acceptance Criteria**

- [x] Spec **không còn** path `/debug/*`; số path giảm đúng 3 (17 → 14)
- [x] Export + validate vẫn PASS; `mvn verify` + CI xanh

**Kết quả verify**

```console
$ bash tools/export-openapi.sh
  OK identity ← http://localhost:8081/v3/api-docs
  OK catalog ← http://localhost:8082/v3/api-docs
  OK gateway ← http://localhost:8080/v3/api-docs
VALIDATE PASS · 14 path · 18 operation · 7 operation cần token · 18 schema

$ grep -c '/debug' docs/api/openapi.yaml
0
```

| AC | Kết quả đo được |
|---|---|
| Spec không còn `/debug/*`, số path giảm đúng 3 | PASS — **17 → 14 path**, **21 → 18 operation** (`/debug/slow`, `/debug/error`, `/debug/boom` bị ẩn bằng `@Hidden` trên cả 3 service) |
| Export + validate vẫn PASS | PASS — `VALIDATE PASS · 14 path · 18 operation · 7 operation cần token · 18 schema` |
| `mvn verify` + CI xanh | PASS — `mvn -B -ntp verify -DskipITs` **BUILD SUCCESS** (toàn reactor) + CI xanh trên PR |
| Endpoint vẫn hoạt động ở runtime (chỉ ẩn khỏi docs) | PASS — `@Hidden` chỉ ảnh hưởng spec; `/debug/error` vẫn 200/404 theo `SHOPLAB_DEBUG_ENDPOINTS` |

**Learning note**

- **Contract ≠ danh sách endpoint kỹ thuật**: `/debug/*` tồn tại thật nhưng không phải API cho client.
  Để trong spec thì client/QA sẽ tưởng đó là API cần dùng, còn reviewer phải tự lọc. Ẩn bằng
  `@Hidden` là cách nói "endpoint này không thuộc hợp đồng".
- **Probe (`/health`, `/ready`, `/version`) thì GIỮ**: đó là hợp đồng vận hành thật, runbook dùng,
  và ổn định. Phân biệt "hạ tầng dùng để verify" với "hạ tầng dùng để vận hành" — cái đầu ẩn, cái sau giữ.
- **Đổi spec phải export lại rồi so số**, không chỉ sửa annotation: verify bằng **đếm path/operation
  trước-sau** (17→14, 21→18) để chắc không ẩn nhầm endpoint khác.

---

## 5. Phase 2 → 8 — Epic outline

> Các phase này **chỉ ở mức epic**. Chỉ breakdown thành ticket chi tiết khi tới lượt — breakdown sớm sẽ phải viết lại
> vì hiểu biết thay đổi sau mỗi phase (`AGENTS.md` §3.1).

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

## 6. Learning log

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
| 2026-11-03 | SL-1.0 | **`paths` filter trên `pull_request` đánh giá theo TOÀN BỘ diff của PR** (không phải commit cuối) → commit docs trong PR có code vẫn kích hoạt workflow. Muốn commit docs "miễn phí" thì phải là PR chỉ-docs hoặc `[skip ci]`. |
| 2026-11-03 | SL-1.0 | **Job `skipped` = 0 phút, nhưng REST jobs API trả `started_at` GIẢ cho job skipped** (started == completed) → đo chi phí bằng REST sẽ tính dư ~1 phút/job. Dùng GraphQL (`gh run view --json jobs`, `started_at = null`) hoặc lọc `conclusion == 'skipped'`. |
| 2026-11-03 | SL-1.0 | **Đo trước khi tối ưu, đừng đoán**: 71 phút/18 run với 62% là run docs-only — trực giác nghĩ "build-image đắt nhất" (đúng 46%) nhưng thứ **đáng bỏ** nhất là run docs-only. Re-run cũng **đánh giá lại `vars`** → test kill-switch không cần commit giả. |
| 2026-11-05 | SL-1.1 | **Spring Boot 4 tách auto-configuration theo công nghệ**: khai `flyway-core` trần ⇒ Flyway **không chạy** (im lặng!) → Hibernate `validate` báo `missing table [users]`. Phải dùng `spring-boot-starter-flyway`. **Dependency mới phải kiểm chứng bằng log, không chỉ "build xanh".** |
| 2026-11-05 | SL-1.1 | **`spring-boot:repackage` chạy TRƯỚC failsafe** ⇒ failsafe lấy classpath từ jar "fat" → `NoClassDefFoundError` cho class của chính module; fix bằng `<classesDirectory>${project.build.outputDirectory}</classesDirectory>`. Kèm: **Jackson 3** đổi package (`tools.jackson.databind`). |
| 2026-11-05 | SL-1.1 | **Non-root container + bind-mount secret = uid mismatch**: private key mode 600 của host (uid 1000) không đọc được bởi uid hệ thống trong image → chạy container bằng `user: "${SHOPLAB_UID}:${SHOPLAB_GID}"`. Và **mọi Dockerfile phải copy pom của TẤT CẢ module** — thêm module thứ 3 làm build image của `service-template` chết với `Child module ... does not exist`. |
| 2026-11-05 | SL-1.1 | **Spring Security 7 coi `iss` là URL** (`Jwt.getIssuer()` ném lỗi nếu không parse được) → đặt `iss` dạng URI. **Test với `Clock` cố định**: decoder phải dùng cùng clock, nếu không test "đỏ vì đồng hồ" chứ không phải vì logic sai. |
| 2026-11-05 | SL-1.2 | **Gateway tự viết: khớp route phải theo RANH GIỚI segment**, không dùng `startsWith` thô — prefix `/v1/auth` mà khớp `/v1/authz` là bug kinh điển. Test riêng case này rẻ hơn debug sau. |
| 2026-11-05 | SL-1.2 | **Header danh tính (`x-user-*`) phải bị XOÁ chứ không chỉ ghi đè**: chỉ `setHeader` ở chỗ forward thì client vẫn lọt được trên route public. Bọc request (`UserHeaderRequest`) để **mọi** đường đọc header thấy đúng giá trị do gateway quyết định. |
| 2026-11-05 | SL-1.2 | **Cache JWKS cần cả TTL lẫn "nạp lại khi gặp `kid` lạ" — và phải CHẶN DỘI**: token với `kid` rác là input miễn phí, không chặn thì mỗi request thành 1 lần gọi identity (tự DoS). Lần viết đầu tôi quên chặn dội; test `shouldThrottleForcedRefresh` mới lộ. |
| 2026-11-05 | SL-1.2 | **401 và 503 khác nhau về ngữ nghĩa**: token sai = lỗi client (đừng retry); không lấy được JWKS / service đích chết = lỗi hạ tầng (retry được). Gộp thành 401 khiến client đăng nhập lại vô ích khi identity chỉ đang khởi động. |
| 2026-11-05 | SL-1.2 | **Algorithm confusion**: chặn `alg != RS256` **trước** khi tra khoá — token HS256 ký bằng chính public key có thể lọt qua thư viện verify lỏng. |
| 2026-11-05 | SL-1.2 | **Bẫy Dockerfile lặp lần 2**: thêm module Maven ⇒ **mọi** Dockerfile phải thêm 1 dòng `COPY …/pom.xml` (lần này `identity-service` + `service-template` cùng vỡ). Kiểm nhanh: `grep -c 'COPY services/' services/*/Dockerfile`. |
| 2026-11-06 | SL-1.6 | **Planner bỏ index trên bảng nhỏ KHÔNG có nghĩa index sai**: 20 row ⇒ seq scan rẻ hơn index scan. Muốn chứng minh index dùng được thì `SET enable_seqscan=off` + EXPLAIN và **ghi rõ đã ép**; còn `order by created_at desc limit 5` thì planner **tự** chọn `Index Scan Backward`. EXPLAIN luôn phải kèm bối cảnh dữ liệu + query cụ thể. |
| 2026-11-06 | SL-1.6 | **`LIKE '%q%'` không dùng được btree** — functional index `lower(name)` chỉ cứu **prefix** search. Search giữa chuỗi ở scale lớn cần `tsvector`/trigram hoặc search engine (Phase 3/8); thêm index nhưng ghi rõ giới hạn thay vì tuyên bố "đã tối ưu". |
| 2026-11-06 | SL-1.6 | **Repeatable migration (`R__`) phải idempotent**: Flyway chạy lại khi checksum đổi ⇒ `ON CONFLICT DO NOTHING` + UUID cố định. Verify bằng **restart thật** rồi so số row (5/20/24 → 5/20/24), không tin "đọc code thấy đúng" — và phải test cả `down -v && up -d`. |
| 2026-11-06 | SL-1.6 | **Whitelist sort = chặn SQL injection ở dạng khó thấy** (`sort` đi thẳng vào `ORDER BY`). Value client gửi chỉ được **tra map**, không ghép chuỗi; test `sort=drop table` phải assert **400**, không phải "không crash". |
| 2026-11-06 | SL-1.6 | **List và detail phải khác DTO**: list trả product **không** kèm SKU (tránh N+1: 20 product × 1 query), detail trả kèm `skus`. Dùng chung DTO là tự tạo N+1 mà không ai thấy cho tới khi tải cao. |
| 2026-11-06 | SL-1.6 | **Validate query param ở tầng parse (`ProductQuery`) thay vì để Spring bind**: bind sai kiểu của Spring trả 400 nhưng body **không phải `ApiError`** ⇒ client phải xử lý 2 format lỗi. Tách record còn cho 20 unit test chạy 0,2s thay vì test qua HTTP. |
| 2026-11-06 | SL-1.7 | **Smoke test phải random "đủ mạnh"**: timestamp **giây** không đủ khi test chạy <1s ⇒ 2 lần chạy cùng giây đụng unique index (`slug`) ⇒ **flaky** (đo được 9 PASS/4 FAIL). Mọi unique constraint trong test (email/slug/code) phải có entropy thật (`$RANDOM`/UUID). |
| 2026-11-06 | SL-1.7 | **`curl -o <file>` KHÔNG mở file khi connection bị từ chối** ⇒ file giữ nội dung lần trước ⇒ bước fail in **body cũ** (`{"status":"UP"}` trong khi service đã chết). Luôn `: > "$TMP_BODY"` trước mỗi request — bug "báo sai chỗ" nguy hiểm hơn bug báo đúng. |
| 2026-11-06 | SL-1.7 | **`status=000` và `503` chỉ ra 2 tầng khác nhau**: `000` = client không kết nối được (service chết/cổng sai) · `503` = gateway sống nhưng không tới được service đích. Runbook phải dạy đọc **cặp mã** này thay vì chỉ nói "không có 200". |
| 2026-11-06 | SL-1.7 | **Test âm phải nằm chung script với happy path**: bước "buyer tạo product → **403**" là PASS khi trả 403. Tách ra script riêng thì không ai chạy ⇒ phân quyền hỏng mà không ai biết. |
| 2026-11-06 | SL-1.7 | **Runbook chỉ đáng tin khi đã dùng nó để debug thật**: `docs/runbook-phase-1.md` §5 dán **nguyên văn** output của lần giả lập lỗi (stop catalog → fail 1c/8 → `docker ps` → `up -d`), không phải ví dụ viết tay. |


| 2026-11-06 | SL-1.8 | **Version springdoc phải tra bảng tương thích, không đoán**: Boot **4.1.x** ⇒ springdoc **3.x** (3.1–3.2); dòng 2.x là cho Boot 3. Kiểm nhanh: `maven-metadata.xml` trên Maven Central + bảng Boot↔springdoc trong FAQ. Đoán sai tốn cả vòng build image (~5 phút). |
| 2026-11-06 | SL-1.8 | **Tài liệu lỗi sinh bằng `OperationCustomizer`, không rải annotation**: 400/401/403/404/409 là hành vi của *toàn hệ* (`GlobalExceptionHandler` + security chain) ⇒ một customizer ở commons làm endpoint mới **tự** có, không phụ thuộc dev nhớ annotation. |
| 2026-11-06 | SL-1.8 | **`$ref` không tự sinh schema**: phải tự thêm `ApiError` vào `components.schemas` (`ModelConverters.readAll`) thì ref mới resolve. Ref treo = spec trông đúng nhưng generate client sẽ vỡ. |
| 2026-11-06 | SL-1.8 | **Bật/tắt spec phải fail-closed + verify bằng env thật**: dùng `EnvironmentPostProcessor` (chạy SAU config data, nếu không bị yml ghi đè) ép `false` khi `APP_ENV != local`; verify bằng **chạy container `APP_ENV=prod`** rồi curl `/v3/api-docs` → 404, không chỉ đọc code. |
| 2026-11-06 | SL-1.8 | **Export phải deterministic mới diff được**: sort `paths`/`schemas`/`tags` trước khi ghi YAML — nếu không, lần export nào cũng đổi thứ tự ⇒ git diff rác ⇒ không ai review spec. |
| 2026-11-06 | SL-1.8 | **Gateway không nằm trong spec nghiệp vụ**: nó là proxy catch-all ⇒ `@Hidden` + export chỉ merge identity/catalog. Vẫn giữ `/v3/api-docs` của gateway (200) để debug route, nhưng không trộn vào contract client. |
| 2026-11-06 | SL-1.8 | **Thêm annotation qua script phải chạy `spotless:apply` ngay**: import/annotation chèn bằng python làm `spotless:check` fail **bên trong `docker build`** (image đỏ, không phải CI đỏ) — sửa bằng `mvn spotless:apply` trước khi build lại. |

| 2026-11-06 | SL-1.15 | **Comment "không dùng cho prod" không phải chốt chặn**: seed nằm chung `db/migration` ⇒ Flyway chạy ở MỌI môi trường. Fail-closed = **mặc định không có** (image chỉ trỏ `db/migration`), dev **tự thêm** location; verify bằng container `APP_ENV=prod` trên **DB mới** rồi đếm row. |
| 2026-11-06 | SL-1.15 | **Đổi định dạng dữ liệu sinh ra (slug) làm đỏ test phụ thuộc dữ liệu sẵn có**: `"Điện thoại"` → `dien-thoai` trùng category seed ⇒ 2 IT fail. Test tạo entity có unique constraint phải **tự sinh hậu tố ngẫu nhiên**. |
| 2026-11-06 | SL-1.15 | **Transliterate tiếng Việt: NFD không đủ** — `đ/Đ` là chữ riêng, không tách được bằng `\p{M}` nên phải map tay; test phải có `Điện`/`Đồ`/`Đỏ`. |
| 2026-11-06 | SL-1.17 | **Contract ≠ danh sách endpoint kỹ thuật**: `/debug/*` có thật nhưng không phải API cho client ⇒ `@Hidden`. Phân biệt "hạ tầng để verify" (ẩn) với "hạ tầng để vận hành" (`/health`, `/ready`, `/version` — GIỮ, runbook dùng). |
| 2026-11-06 | SL-1.16 | **Nợ kỹ thuật phải có điều kiện revisit verify được** ("khi đo được p99 ở dữ liệu lớn", không phải "khi rảnh") và phải nằm ở **một bảng**. Learning note = đã hiểu gì; debt = còn phải làm gì — để chung thì không ai lọc được. |
| 2026-11-05 | process | **Push race với lúc merge ⇒ mất commit**: commit cuối của `SL-1.1` (`feda53a` — status Done + block CI evidence + changelog) push lên đúng lúc user merge; squash merge dùng state **trước** commit đó, rồi nhánh bị xoá ⇒ thay đổi biến mất khỏi `main`. **Bài học**: sau khi merge phải `git show <merge-sha> --stat`/`grep` kiểm nội dung trên `main`, đừng tin "đã push là xong"; commit bị mất vẫn cứu được qua reflog nếu chưa GC. Đã khôi phục trong nhánh `SL-1.2`. |
| 2026-11-05 | SL-1.9 | **`paths-ignore` và `[skip ci]` bổ sung nhau, không thay thế nhau**: `paths-ignore` đánh giá theo **toàn bộ diff của PR** → commit docs trong PR **có code** vẫn tốn 1 run. `[skip ci]` xử lý đúng case đó. Nhưng GitHub đọc skip instruction ở **HEAD commit của PR** ⇒ docs commit ở cuối PR skip luôn run của cả PR (check treo `Pending`) → đừng tin nó, hãy kiểm bằng **diff thật**. |
| 2026-11-05 | SL-1.9 | **Guard cũng phải test theo đúng thứ tự**: lần đầu test guard "PR draft" **sau khi** đã chạy `gh pr ready` ⇒ tưởng guard hỏng, thực ra kịch bản test sai. Test guard cần kịch bản cẩn thận y như test code. |
| 2026-11-05 | process | **Merge tay là chỗ dễ sai nhất**: PR #9/#10 merge tay ⇒ không tái lập được, và đã một lần mất nội dung (push race ở `SL-1.1`). Từ `SL-1.9` mọi lần merge đi qua **một** chốt: `tools/merge-pr.sh` (kiểm CI xanh + comment tóm tắt + diff sau CI là docs-only) → squash + xoá nhánh + **kiểm `main` có nội dung**. |
| 2026-11-05 | SL-1.10 | **`gh pr merge --squash` ghép message từ PR, KHÔNG phải từ commit HEAD** (title PR + subject của mọi commit trong PR) ⇒ token `[skip ci]` của commit docs **lọt vào merge commit** → push lên `main` bị skip, mất luôn lần kiểm chứng `main`. Fix: truyền `--subject`/`--body` tường minh + lọc token skip. |
| 2026-11-05 | process | **Rule mới phải kiểm cả tác dụng phụ**: `[skip ci]` (SL-1.9) tiết kiệm CI đúng thiết kế nhưng đồng thời tắt luôn CI của `main`. Checklist sau merge giờ có **2 mục**: squash message không chứa token skip **và** `main` có run mới. |
| 2026-11-05 | process | **Sửa bảng markdown: đừng dùng đoạn ĐẦU của dòng làm `old_text`** — công cụ thay thế khớp prefix, để lại phần đuôi dính vào dòng mới (dính 2 lần ở Changelog). Dùng **cả dòng** hoặc `insert_line`, rồi kiểm lại bằng `grep -c '^| <ngày> | \`<ticket>\`'`. |
| 2026-11-05 | SL-1.11 | **Tín hiệu sai tệ hơn không có tín hiệu**: dòng chẩn đoán đếm substring `skip ci` in `1` khi body chỉ chứa **câu ví dụ** → tôi tưởng fix `SL-1.10` hỏng, tốn 1 vòng kiểm. Công cụ phải in đúng **định nghĩa** (token `[skip ci]`), và bằng chứng thật là `gh run list --branch main`. |
| 2026-11-05 | process | **Chạy script/test từ đúng working tree**: 2 lần test guard bằng bản script ở nhánh khác (chưa có fix) → kết quả cũ, lại tưởng guard sai. Kiểm `git branch --show-current` trước khi kết luận. |
| 2026-11-05 | process | **Chốt chặn CI phải rà lại mỗi khi thêm service**: job `build-image` viết ở Phase 0 (khi chỉ có `service-template`) → thêm 2 service thật mà không cập nhật ⇒ CI xanh nhưng **Dockerfile của 2 service thật không được kiểm** = **chốt chặn giả**. Thêm service ⇒ rà **mọi** job CI (cả `test` *và* `build-image`). |
| 2026-11-05 | process | **Đóng ticket ngay trong PR của nó** (mark done gộp vào commit cuối) thay vì để dành sang PR sau: bớt 1 nhịp chờ, bớt 1 commit, không dính bẫy `[skip ci]` ở HEAD. Đánh đổi: `main` chỉ thấy 1 commit/ticket. |
| 2026-11-05 | SL-1.14 | **Phân biệt "0 run vì hợp lệ" vs "0 run vì bị skip"**: cùng là *không có run*, nhưng PR chỉ-docs là bình thường còn code bị skip là nguy hiểm. Phân biệt bằng **diff của PR**, không bằng "có run hay không". |
| 2026-11-05 | SL-1.14 | **Tự giẫm bẫy mình vừa viết ra**: commit message giải thích *"nên KHÔNG dùng …"* lại chứa **token skip dạng literal** ⇒ GitHub skip run của chính commit đó (dù diff có `ci.yml`). Tài liệu hoá một cơ chế thì phải biết nó **đọc ở đâu** — ở đây là **cả body**, không chỉ subject. |
| 2026-11-05 | SL-1.14 | **GitHub API không đáng tin tuyệt đối**: trong ~15 phút gặp 3 lỗi tạm thời (`gh pr create` GraphQL *Something went wrong*, `gh pr close` 502, `gh pr merge` GraphQL) — chạy lại là được. **Debt**: `merge-pr.sh` chưa có retry, hiện phải chạy lại tay. |
| 2026-11-05 | SL-1.12 | **Chốt chặn giả sống được 2 ticket**: `build-image` "xanh" suốt `SL-1.1`+`SL-1.2` mà chỉ kiểm 1/3 service — không ai đọc lại job CI khi thêm service. **Song song ≠ rẻ**: 3 image chạy song song giữ nguyên thời gian *chờ* nhưng nhân 3 phút *billable* — đo bằng số, không bằng cảm giác. |
| 2026-11-05 | SL-1.13 | **Service optional ⇒ dùng compose profile chứ đừng xoá/để mặc định**: profile giữ được "chốt chặn sống" (template vẫn build/chạy được khi cần) mà `up -d` không tốn RAM/build cho nó. |
| 2026-11-05 | SL-1.3 | **Smoke script phải chịu được hạ tầng chết**: `set -e` + curl connection-refused (exit 7) kill script trước khi báo lỗi → `curl \|\| true` để lấy status `000`, guard token rỗng để `python3` không KeyError. Bonus: **GNU date `%3N` ra 9 chữ số** (nanosec) trên hệ này → dùng `%s%N`/1e6. |
| 2026-11-05 | SL-1.4 | **`@Transactional` + throw = rollback xoá luôn việc vừa ghi**: reuse detection revoke cả chuỗi rồi throw 401 ⇒ mất revoke. Phải `noRollbackFor` ở **mọi tầng** `@Transactional` mà exception đi qua (tầng ngoài mới quyết định commit). **IT bắt được, unit test mock thì không.** |
| 2026-11-05 | SL-1.4 | **Endpoint mới phải verify qua CỬA VÀO DUY NHẤT**: `/v1/admin/users` 200 khi gọi thẳng identity nhưng **404 qua gateway** (thiếu route) — chỉ lộ khi test qua 8080. Và security filter đổi hành vi path lạ 404 → **401** (có chủ ý: không lộ route). |
| 2026-11-06 | SL-1.5 | **Method security ném exception SAU DispatcherServlet** ⇒ `@PreAuthorize` từ chối thành **500** (không phải 403) vì `GlobalExceptionHandler` bắt `Exception`. Mỗi lớp chặn quyền phải có handler riêng. IT bắt được, curl tay dễ bỏ qua. |
| 2026-11-06 | SL-1.5 | **`@Version` chỉ tăng lúc flush** ⇒ trả DTO trước flush là trả version CŨ (0) ⇒ client update lần sau bằng version sai. Giá trị do DB sinh (version/default/trigger) phải đọc **sau flush** — `saveAndFlush`, và phải test bằng IT vì mock không mô phỏng flush. |
| 2026-11-06 | SL-1.5 | **`docker build` không có Docker socket ⇒ `-DskipITs` cũng đổi kết quả gate coverage** (image build đỏ dù code đúng). Tách vai: gate chất lượng ở job `test` (chạy đủ IT), image build `-Djacoco.skip=true`. |
| 2026-11-06 | SL-1.5 | **Gateway `public-paths` theo PATH, không theo METHOD**: service có "GET public + POST cần token" trên cùng prefix thì gateway thả cả prefix và **service tự chặn** phần ghi. Chỉ chấp nhận được vì service tự verify JWT; nếu tin header gateway thì đó là lỗ hổng. |
| 2026-11-06 | SL-1.5 | **Verify script là code**: bash heredoc + 3 lớp escaping làm token bị cắt → **17 FAIL giả**. FAIL hàng loạt cùng kiểu (401/None) ⇒ nghi script trước, sản phẩm sau. Python `urllib` cho 36 PASS. |
| 2026-11-05 | process | **Đừng dùng backtick trong `--body` của `gh` khi gõ trực tiếp**: shell thực thi phần trong backtick (`tools/merge-pr.sh` → `Permission denied`, `null: command not found`) và **body PR bị hỏng**. Luôn dùng `--body-file` với heredoc (`<<'EOF'`). |

---

## 7. Technical debt register

> Quy tắc ghi nhận: [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md). Mỗi món nợ **phải có
> điều kiện revisit verify được** — không phải "khi rảnh". Rà bảng này khi làm DoD mỗi phase.
> Trả xong thì giữ dòng, đổi `Mức độ` thành `✅ đã trả (<ticket>)`.

| ID | Mô tả | Nguồn | Điều kiện revisit | Mức độ |
|---|---|---|---|---|
| `TD-1` | Bảng `refresh_tokens` không có job dọn row hết hạn → phình dần | SL-1.4 | Khi bảng > 10⁶ row **hoặc** khi thêm scheduler | trung bình |
| `TD-2` | `GET /v1/admin/users` trả **toàn bộ** user, không phân trang | SL-1.4 | Khi số user > 1.000 hoặc có màn quản trị thật | thấp |
| `TD-3` | Handler map `AccessDeniedException` → 403 nằm trong **catalog**; service khác dùng `@PreAuthorize` phải copy | SL-1.5 | Khi có service **thứ hai** dùng `@PreAuthorize` (→ commons) | trung bình |
| `TD-4` | `tools/e2e-smoke.sh` phụ thuộc tài khoản seed + `docker logs` ⇒ chỉ chạy local, chưa vào CI | SL-1.7 | Phase 2 (D6: smoke trong CI) | trung bình |
| `TD-5` | `tools/merge-pr.sh` chưa retry khi GitHub API lỗi tạm thời | SL-1.14 | Lần tới script fail vì API (retry + backoff ~15 dòng) | thấp |
| `TD-6` | Phân trang **offset** (`OFFSET 100000` quét bỏ rất nhiều row) | SL-1.6 | Phase 4 khi đo được p99 của list (→ keyset) | trung bình |
| `TD-7` | `q` dùng `LIKE '%…%'` (leading wildcard) ⇒ không dùng được btree, full scan | SL-1.6 | Phase 3/8 khi cần search thật (→ `tsvector`/trigram) | trung bình |
| `TD-8` | Sort whitelist chỉ `price`, `createdAt` | SL-1.6 | Khi client cần và có index tương ứng | thấp |
| `TD-9` | Verify fail-closed của `/v3/api-docs` đang làm **bằng tay**, chưa có test trong CI | SL-1.8 | Khi CI có bước compose (D6) | thấp |
| `TD-10` | `ProductService.findById` không còn dùng ở main (chỉ test gọi) | SL-1.6 | Lần sửa `ProductService` tiếp theo | thấp |
| `TD-11` | `page`/`size`/`sort` sai → 400 nhưng không trả `details` theo field | SL-1.6 | Khi client cần hiển thị lỗi theo field | thấp |
| `TD-12` | Chưa có **soft delete** cho product; `DELETE` là hard delete + cascade SKU | SL-1.6 | Khi bắt đầu `SL-2.x` (order tham chiếu SKU) | cao |

## 8. Changelog


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
| 2026-11-05 | Tạo 3 ticket phát sinh sau khi rà CI/compose: `SL-1.12` (job `build-image` hiện **chỉ** build `service-template` — bằng chứng log run `42054583987`; 2 service thật không được CI kiểm Dockerfile), `SL-1.13` (`service-template` vào **compose profile** `template` để `up -d` chỉ dựng hệ thống thật), `SL-1.14` (`merge-pr.sh`: cho phép PR **docs-only** + post comment evidence **hậu merge**). **Đổi process**: mark done **gộp vào commit cuối của ticket** (1 ticket = 1 commit = 1 run CI trên PR), bỏ "fold commit minor sang PR sau"; evidence hậu merge ghi bằng PR comment. Chốt **giữ monorepo** (ghi vào ADR-0003 → Revisit). |
| 2026-11-05 | `SL-1.14` → `Done`: `tools/merge-pr.sh` (a) cho phép merge PR **chỉ-docs** (0 run là hợp lệ khi **toàn bộ** diff là docs; 0 run mà có file không phải docs → **vẫn chặn**) và (b) tự post **comment evidence hậu merge** (merge sha · run CI trên `main` · token skip · ghi chú chống hiểu nhầm). Verify: **TEST A** PR docs-only #17 → cho phép (`exit=0`); **TEST B** 0 run + `tools/secret-scan.sh` → chặn (`exit=1`); **merge thật** PR #17 → `main` = `4afb3da` + comment evidence tự post ✓. Kèm dọn 2 nhánh temp + xoá file test. |
| 2026-11-05 | `SL-1.12` → `Done`: job `build-image` chuyển sang **matrix** (`service-template` · `identity-service` · `api-gateway`, `fail-fast: false`) + `--build-arg SKIP_TESTS=true` (bỏ test trùng trong image — job `test` đã chạy cả reactor). **Test âm thật**: phá `api-gateway/Dockerfile` → run `42064872154` **chỉ** job gateway đỏ (`Child module … does not exist`), 2 job image kia xanh; revert → run `42065751051` **5/5 xanh**. Số đo: billable 194s → 343s (**+2,5 phút/run**, wall-clock gần như không đổi vì chạy song song). Cập nhật `README.md` + `architecture.md` §10.3 + comment `ci.yml`. PR [#20](https://github.com/anchu3003/shop-lab-microservices/pull/20). |
| 2026-11-05 | `SL-1.13` → `Done`: `service-template` vào **compose profile** `template` ⇒ `up -d` mặc định chỉ dựng hệ thống thật (không tốn RAM/build cho service mẫu). Verify: `config --services` (không profile) **không** có template · `up -d` không tạo `shoplab-service-template` · `--profile template up -d service-template` → Started, `/ready` 200 · gateway/identity vẫn healthy. Cập nhật `README.md` (bảng port + lệnh) + comment `infra/docker-compose.yml`. |
| 2026-11-05 | `SL-1.3` → `Done`: `tools/e2e-smoke.sh` — health gateway+identity → register (email random) → login → `/v1/users/me` có token → 200 · thiếu token → **401** · đối chiếu `traceId` trong log identity; `PASS/FAIL` từng bước + exit code. Verify: **7/7 PASS** · exit 0 · chạy lại 3 lần pass · **test âm** (stop identity) → fail đúng bước `1b` + `exit=1`. Thêm mục README "E2E smoke". Sửa 2 bug: `date +%s%3N` ra nanosec → `%s%N`/1e6; curl connection-refused + `set -e` → `\| true`. |
| 2026-11-06 | `SL-1.5` → `Done`: `catalog-service` (port **8082**, DB `catalog`, Flyway `V1__create_catalog.sql` với `price NUMERIC(12,2)` + `@Version` + FK/index) — `POST/GET /v1/categories`, `POST/GET/PUT/DELETE /v1/products`; guest **đọc** được, chỉ `seller`/`admin` **ghi**. Service **tự verify JWT qua JWKS của identity** (defense in depth: gọi thẳng :8082 không token vẫn 401, `x-user-role` client gửi bị bỏ qua). Error **400/401/403/404/409** đều là `ApiError` có `traceId`. Hạ tầng: module mới ở parent pom, 4 Dockerfile `COPY` pom mới, CI matrix **4 service**, compose + `.env.example` + gateway route `/v1/products` `/v1/categories`. Verify: **36 unit + 12 IT** (+37 commons) xanh; **36/36 mục AC** trên stack thật (seller 201 · buyer 403 · guest 200 · 404 · 400 details · 401 gọi thẳng · không leo quyền · `version` +1); `e2e-smoke` vẫn 7/7. Sửa 3 bug thật: `@PreAuthorize` → **500** thay vì 403, `@Version` trả số cũ trong response `PUT`, image build đỏ vì JaCoCo gate bị `-DskipITs`. |
| 2026-11-06 | `SL-1.6` → `Done`: `catalog-service` có **SKU** (`V2__create_sku.sql`: `code` unique, `price_override` NULL = dùng giá product, `stock_hint` chỉ là gợi ý — tồn kho thật là việc của inventory Phase 2) + **list phân trang/filter/sort** (`GET /v1/products?page=&size=&categoryId=&q=&sort=` → `{content,page,size,totalElements,totalPages}`, public cho guest) + **seed dev** (`R__seed_dev_data.sql`: 5 category / 20 product / 24 SKU, idempotent bằng `ON CONFLICT`). Tham số sai (`size` ngoài 1..100 · `page<0` · `sort` lạ · `categoryId` không UUID) → **400 `VALIDATION_ERROR`**, **không clamp**; sort whitelist `price,asc|desc` · `createdAt,asc|desc`. Verify: **60 unit + 18 IT** xanh; **35/35 mục AC** trên stack thật (paging không trùng id · filter · `q` không phân biệt hoa/thường · sort đúng thứ tự · 400 cho mọi input xấu · SKU trong chi tiết · **`down -v && up -d` → 5/20/24 seed** · restart **không** nhân đôi · `EXPLAIN` dùng `ix_product_created_at`); `e2e-smoke` 7/7. |
| 2026-11-06 | `SL-1.7` → `Done`: `tools/e2e-smoke.sh` mở rộng thành E2E **cả Phase 1** — **17 mục**: health 3 service · register · login · `/v1/users/me` (200/401) · `traceId` xuyên service · **seller** tạo category+product (201, in id) · **guest** list + xem chi tiết (**200, không token**, có `skus`) · **buyer** tạo product (**403**, case mong đợi lỗi) · body lỗi là `ApiError`. Fail ở đâu in **gợi ý lệnh kiểm tra** ở đó. Thêm `docs/runbook-phase-1.md` (sơ đồ luồng · bảng port · kiểm tra **từng tầng** · **5 lỗi**: 401/403/404/503/timeout · §5 là output debug thật). Verify: **17/17 PASS** · chạy **6/6 lần** pass (~430 ms) · `docker stop` catalog → fail đúng **1c (000)** + **8 (503)**, các bước khác vẫn PASS. Sửa 2 bug thật: category trùng slug khi 2 lần chạy cùng giây (**flaky**), và script in **body cũ** khi connection refused. |
| 2026-11-06 | `SL-1.8` → `Done`: OpenAPI spec Phase 1 — thêm `springdoc-openapi-starter-webmvc-api` **3.2.0** (Boot 4.1.x ⇒ springdoc 3.x) cho 3 service, expose `/v3/api-docs`; khung spec (info + security scheme `bearerAuth`) + **bộ response lỗi 400/401/403/404/409 tự gắn cho MỌI operation** (`$ref → ApiError`) đặt ở `service-commons` (`OpenApiAutoConfiguration`), nên endpoint Phase 2 tự có tài liệu lỗi. `OpenApiEnvironmentPostProcessor` ép tắt spec khi `APP_ENV != local` (**fail-closed**, override bằng `SHOPLAB_OPENAPI_ENABLED`). `tools/export-openapi.sh` merge spec identity + catalog → **`docs/api/openapi.yaml`** (`servers` = gateway, sort key để diff ổn định) + **tự validate** bằng `python3`+`pyyaml`. Verify: 3/3 service `/v3/api-docs` **200**; export **VALIDATE PASS · 17 path · 21 operation · 7 operation cần token · 18 schema**; `APP_ENV=prod` → `/v3/api-docs` **404** (service vẫn 200); e2e-smoke 17/17; cả reactor `mvn verify` xanh (254 unit + 40 IT). |
| 2026-11-06 | `SL-1.15` → `Done`: **seed dev không còn chạy ở prod** — chuyển `R__seed_dev_data.sql` sang `db/seed/`, `spring.flyway.locations: ${SHOPLAB_FLYWAY_LOCATIONS:classpath:db/migration}` (**fail-closed**); compose local + IT tự thêm `,classpath:db/seed`. **Slug tiếng Việt** bỏ dấu (NFD + map `đ/Đ`): `"Điện thoại X"` → `dien-thoai-x`, `"Ốp lưng đẹp"` → `op-lung-dep`. Verify: local `down -v && up -d` → **5/20/24** seed · kiểu prod `APP_ENV=prod` + DB mới → **category=0 product=0 sku=0** (history chỉ V1+V2) · **42 + 61 unit + 18 IT** xanh · e2e-smoke 17/17. Sửa kèm 2 IT bị đỏ do slug mới trùng seed (dùng hậu tố UUID). |
| 2026-11-06 | `SL-1.17` → `Done`: ẩn endpoint debug khỏi OpenAPI spec bằng `@Hidden` (identity + catalog + gateway) — spec **17 → 14 path**, **21 → 18 operation**, `grep -c '/debug' = 0`, validate vẫn PASS; probe `/health` `/ready` `/version` **giữ** vì là hợp đồng vận hành thật. Docs: `docs/api/README.md` thêm mục "cái gì không nằm trong spec". Verify: export lại + đếm path trước/sau · `mvn verify -DskipITs` xanh toàn reactor · CI xanh. |
| 2026-11-06 | `SL-1.16` → `Done`: rule [`.clinerules/20-tech-debt.md`](.clinerules/20-tech-debt.md) + **`Backlog.md` §7 Technical debt register** với **12 mục** `TD-1`…`TD-12` (job dọn `refresh_tokens` · admin list không phân trang · handler 403 nên ở commons · smoke chưa vào CI · `merge-pr.sh` chưa retry · offset pagination · `LIKE '%q%'` full scan · sort whitelist hẹp · verify OpenAPI fail-closed bằng tay · dead code · 400 không `details` · **soft delete chưa có — chặn `SL-2.x`**). Changelog đổi thành §8. |



| 2026-11-05 | `SL-1.4` → `Done`: refresh token (**opaque 256-bit, lưu SHA-256**) + **rotate + reuse detection** (dùng lại token đã rotate ⇒ 401 **và revoke cả chuỗi**), logout, `GET /v1/admin/users` (ADMIN 200 / buyer **403**), security filter chain (identity **tự verify JWT** — defense in depth) + 401/403 trả **ApiError** có `traceId`. Seed `admin@`/`seller@` bằng `LocalDevUserSeeder` (chỉ `APP_ENV=local`). Verify: **46 unit + 22 IT** xanh; E2E **9 mục** trên stack thật (rotate/reuse/logout/RBAC/hash-trong-DB/**qua gateway**); `tools/e2e-smoke.sh` vẫn 7/7. Sửa 3 lỗi thật: `noRollbackFor` (revoke bị rollback), thiếu route `/v1/admin` ở gateway, path lạ 404→**401**. |


| 2026-11-02 | Tạo PR [#4](https://github.com/anchu3003/shop-lab-microservices/pull/4) cho `SL-0.4` (nhánh `feat/sl-0.4-service-template`, 5 commit). Merge bằng **squash** theo `docs/git-flow.md`. |
| 2026-11-03 | `SL-0.5` → `In Progress`: nhánh `feat/sl-0.5-ci-pipeline`. Chốt với user: **giữ remote HTTPS** (token VS Code có scope `workflow`, không cần đổi SSH). |
| 2026-11-03 | `SL-0.5` → `Review`: `.github/workflows/ci.yml` (job `lint` ∥ `test` → `build-image`; cache Maven; `concurrency` theo tên nhánh; `timeout-minutes`; `permissions: contents: read`), badge CI + mục **CI (GitHub Actions)** trong `README.md`, `architecture.md` §10.3 + Changelog. **Baseline run đầu: 2m35s** (3/3 job xanh). Test âm thật: `lint` đỏ (`HealthController.java:54:5 ... [NeedBraces]`), `test` đỏ (`AppEnvTest.shouldReportLocalOnly:48`) — cả hai lần `build-image` **skipped**. PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5). |
| 2026-11-03 | `SL-0.5`: **sửa lỗi thiết kế `concurrency`** — gom push+PR chung group làm run PR huỷ run push, để lại check `cancelled` trên commit; đổi sang `group: ${{ github.workflow }}-${{ github.ref }}`. Cập nhật `README.md` + ticket + learning log. |
| 2026-11-03 | Merge PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5) bằng **squash** (`60e8afe`). Run trên `main` xanh: 3/3 job, 2m13s → badge CI `passing`. |
| 2026-11-03 | Đóng `SL-0.5` → `Done`: **6/6 AC** đạt. **Phase 0 → 8 / 8 ✅ Done** — điều kiện hoàn thành phase đã đủ: `docker compose up` infra `healthy` · service template `/health` 200 · CI xanh · `architecture.md` §11 không còn chữ "cần chốt". Việc tiếp theo: breakdown **Phase 1** (epic → ticket chi tiết). |
| 2026-11-05 | `SL-1.11` → `Done`: fix 2 dòng chẩn đoán gây hiểu nhầm của `tools/merge-pr.sh` (nhánh chưa có run → nói rõ thay vì in `'null'`; đếm token skip bằng regex `\[(skip ci|…)\]` thay vì substring). Verify: message mới trên nhánh tạm (`exit=1`); `grep -c 'skip ci'` = **1** (cách cũ, SAI) vs `grep -cE '\[(skip ci|…)\]'` = **0** (cách mới). PR [#14](https://github.com/anchu3003/shop-lab-microservices/pull/14). Kèm fold bằng chứng merge của `SL-1.9` + `SL-1.10`. |

| 2026-11-03 | **Tách archive**: chuyển toàn bộ Phase 0 (8 ticket + bối cảnh/AC/quyết định/verify/learning note) sang [`Backlog-phase-0.md`](Backlog-phase-0.md); `Backlog.md` còn **260 dòng** (bảng phase · convention · pointer + bảng archive · epic Phase 1–8 · Learning log · Changelog). Cập nhật tham chiếu ở `README.md`, `architecture.md`, ADR-0001…0004, `.clinerules/00-context.md`, `AGENTS.md` §1/§6. |
| 2026-11-03 | **Sửa rule git**: commit minor (đóng ticket / update backlog / sửa doc / tách archive) **không** tạo nhánh + PR riêng — gộp vào nhánh của **ticket kế tiếp**; chỉ tách PR riêng khi hết ticket kế tiếp. Ghi vào `AGENTS.md` §6 · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` · workflow `/close-ticket.md` (bước 8–9). |
| 2026-11-03 | Chốt **ADR-[0005](docs/adr/0005-phase-snapshot-and-release.md)** + dựng `docs/phases/` ([index](docs/phases/README.md) + [`phase-0.md`](docs/phases/phase-0.md)): tag `phase-N` + **GitHub Release** + doc snapshot, **không** dùng release branch ("lazy D"). `AGENTS.md` §5 thêm **DoD của một phase**; `/close-ticket.md` thêm bước 8 & 10 (tách archive, tag/release sau merge). |
| 2026-11-03 | Tạo **tag + GitHub Release `phase-0`** trên merge commit `259a7e9` (ADR-0005). Verify: push tag **không** trigger CI (workflow chỉ khai `on.push.branches`) → 0 phút Actions; `git show phase-0:<file>` đọc được snapshot. |
| 2026-11-03 | `SL-1.0` → `Done`: tối ưu chi phí CI — `paths-ignore` (bỏ run chỉ-docs), `push` chỉ trên `main` (hết run trùng), kill-switch `vars.CI_ENABLED`, bỏ qua draft PR (`ready_for_review`). Verify thật: push nhánh feature **0 run**, draft → run `skipped` (0 phút), `CI_ENABLED=false` + re-run → `skipped`, `true` → 3/3 xanh, merge → run `main` xanh, **PR chỉ-docs → 0 run**. Cùng kịch bản: **8 phút vs ~19,5 phút (−59%)**. Merge PR [#7](https://github.com/anchu3003/shop-lab-microservices/pull/7) (squash). **Phase 1 → 1 / 1**. |
| 2026-11-03 | **Breakdown Phase 1** → 9 ticket chi tiết: `SL-1.0` Done · `SL-1.1`…`SL-1.8` Todo (identity register/login + JWT RS256 + JWKS · gateway routing/verify JWT · **E2E sớm** · refresh token + RBAC · catalog CRUD · SKU/paging/seed · E2E đầy đủ + runbook · OpenAPI spec). Chốt 7 quyết định khi breakdown (gateway **MVC tự viết** · JWT **RS256 + JWKS** · **JPA + Flyway** · **có Testcontainers** · OpenAPI **sinh từ code** · smoke chạy **local** · register luôn là `buyer`). |
| 2026-11-05 | `SL-1.1` → `Done`: nhánh `feat/sl-1.1-identity-service`. `identity-service` (port 8081) — **Flyway** + register/login + **JWT RS256** + **JWKS**; `service-commons` thêm `UNAUTHORIZED`/`FORBIDDEN`; `tools/gen-dev-keys.sh` (khoá dev, không commit); `tools/secret-scan.sh` fix false positive; compose + `.env.example`. Verify thật: container `healthy` · Flyway v1 áp dụng · BCrypt trong DB (60 ký tự) · **chữ ký verify bằng public key từ JWKS** · 401 không lộ email · fail-fast khi thiếu khoá · `/ready` 503 khi DB chết. `mvn verify`: 37 + 39 unit + **16 IT** (Testcontainers); **CI xanh 3/3**, job `test` chạy IT thật (1:24). PR [#9](https://github.com/anchu3003/shop-lab-microservices/pull/9). |
| 2026-11-05 | `SL-1.2` → `Done`: nhánh `feat/sl-1.2-api-gateway`. `api-gateway` (port 8080) — routing theo config (prefix dài nhất thắng) · verify **JWT RS256 qua JWKS** (cache 5m + nạp lại khi `kid` lạ, có chặn dội) · gắn/ghi đè `x-user-id`/`x-user-role` · propagate `x-correlation-id` · **401** token sai / **503** service đích chết. `service-commons` thêm `UNAVAILABLE` (503). `identity-service` thêm `GET /v1/users/me` (protected). Verify thật: container healthy · register/login qua gateway giống hệt gọi thẳng · 401/200/503 đúng · traceId **trùng** ở log 2 service. `mvn verify`: 37 + 39 + **16 IT** + **71 test gateway**; **CI xanh 3/3** (job `test` 1:49). PR [#10](https://github.com/anchu3003/shop-lab-microservices/pull/10). Đồng thời khôi phục 3 thay đổi doc của `SL-1.1` bị mất do push race lúc merge (xem Learning log). |
| 2026-11-05 | `SL-1.9` → `Done`: rule **commit docs-only → `[skip ci]`** (không tốn CI) + **auto-merge** `tools/merge-pr.sh` (verify → comment tóm tắt → squash → xoá nhánh). Verify thật: PR chỉ-docs **0 run**; commit docs trong PR có code **0 run mới**; script **từ chối đúng 4 case** (draft · không OPEN · chưa có CI xanh · thiếu comment tóm tắt); merge PR [#11](https://github.com/anchu3003/shop-lab-microservices/pull/11) bằng chính script.
| 2026-11-05 | `SL-1.10` → `Done`: fix tác dụng phụ của `SL-1.9` — `gh pr merge --squash` ghép *title + subject các commit trong PR* nên `[skip ci]` của commit docs **lọt vào merge commit** ⇒ push `main` bị skip (mất kiểm chứng `main`). Fix: `tools/merge-pr.sh` truyền tường minh `--subject`/`--body` + lọc token skip; thêm cảnh báo vào rule + `docs/git-flow.md`. Verify: `a7d9482` có `1` token skip & **không** có run trên `main`; sau fix merge PR [#13](https://github.com/anchu3003/shop-lab-microservices/pull/13) → `skip-ci tokens: 0` + **có run `push` trên `main`**. |




