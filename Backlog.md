# Backlog.md — ShopLab

> **Cách dùng**: đọc `AGENTS.md` trước. Mỗi lượt chỉ làm **1 ticket**. Ticket xong thì update
> `Trạng thái` + điền `Kết quả verify` + `Learning note`, rồi mới sang ticket tiếp theo.
> **Không** breakdown các phase xa thành ticket chi tiết khi chưa tới lượt.

| | | |
|---|---|---|
| **Ticket đang làm** | — (chờ bắt đầu `SL-1.1`) |
| **Ticket tiếp theo** | `SL-1.1` — `identity-service`: register/login + JWT |
| **Số ticket chi tiết hiện có** | 17 (Phase 0: 8 — archive · Phase 1: 9) |

---

## 1. Trạng thái theo phase

| Phase | Tên | Trạng thái | Ticket Done |
|---|---|---|---|
| 0 | Foundation | ✅ Done | 8 / 8 |
| 1 | Walking skeleton | 🔵 In Progress | 1 / 9 |
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

**Đã breakdown thành 9 ticket chi tiết** (`SL-1.0` … `SL-1.8`) — thứ tự thực hiện:

| # | Ticket | Epic item | Ước lượng | Trạng thái |
|---|---|---|---|---|
| 1 | `SL-1.0` | *(phát sinh)* tối ưu chi phí CI | S | ✅ Done |
| 2 | `SL-1.1` | `identity-service`: register/login + JWT | M | Todo |
| 3 | `SL-1.2` | `api-gateway` bản mỏng | M | Todo |
| 4 | `SL-1.3` | **E2E sớm**: register → login qua gateway | S | Todo |
| 5 | `SL-1.4` | `identity-service`: refresh token + RBAC | M | Todo |
| 6 | `SL-1.5` | `catalog-service`: CRUD category/product | M | Todo |
| 7 | `SL-1.6` | `catalog-service`: SKU + phân trang/filter + seed | M | Todo |
| 8 | `SL-1.7` | E2E smoke đầy đủ + runbook | S | Todo |
| 9 | `SL-1.8` | OpenAPI spec Phase 1 | S | Todo |

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
| **Trạng thái** | Todo |
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

- [ ] `docker compose … up -d` → `shoplab-identity-service` **healthy**; `curl localhost:8081/health` → 200
- [ ] Flyway chạy lúc khởi động: DB `identity` có bảng `users` **và** `flyway_schema_history`
- [ ] `POST /v1/auth/register` email mới → **201**; gọi lại cùng email → **409** với `ApiError` (`code` rõ ràng)
- [ ] Mật khẩu lưu **BCrypt**: query DB thấy `$2a$…`, **không** thấy plaintext
- [ ] `POST /v1/auth/login` đúng → **200** + `accessToken`; decode thấy `sub`, `role`, `exp` ≈ 15 phút
- [ ] login sai mật khẩu → **401**, message **không** tiết lộ email có tồn tại hay không
- [ ] `GET /.well-known/jwks.json` → **200**, `kid` **khớp** header của token vừa phát
- [ ] Verify chữ ký token bằng public key lấy từ JWKS (lệnh thật, dán vào verify) → hợp lệ
- [ ] Thiếu file private key → service **fail-fast**, message rõ ràng (không chạy nửa vời)
- [ ] `mvn verify` xanh: **có** IT Testcontainers chạy thật (ghi lại `Tests run: N`)
- [ ] CI xanh trên PR (job `test` chạy cả IT)

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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

---

### SL-1.2 — `api-gateway`: routing + verify JWT + correlation id + error thống nhất

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

- [ ] `curl localhost:8080/health` → 200; container `healthy` trong compose
- [ ] `POST /v1/auth/register` **qua gateway (8080)** → 201, response **giống hệt** khi gọi thẳng 8081
- [ ] Gọi endpoint protected **không** token → **401** `ApiError` (`code`, `traceId` có giá trị)
- [ ] Gọi với token hợp lệ → 200; service đích **nhận được** `x-user-id`/`x-user-role` (chứng minh bằng log)
- [ ] `x-correlation-id` do client gửi được **giữ nguyên** xuyên gateway → identity (so log 2 bên)
- [ ] Không gửi `x-correlation-id` → gateway tự sinh; log 2 bên **cùng** một traceId
- [ ] Token hết hạn / sai chữ ký → 401, **không** lộ chi tiết nội bộ
- [ ] `docker stop` identity → gọi qua gateway trả **503** `ApiError`, gateway **không** sập
- [ ] `/ready` của gateway phản ánh dependency (503 khi identity chết) — giống cơ chế của template
- [ ] `mvn verify` xanh + CI xanh

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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-1.3 — Walking skeleton E2E: register → login qua gateway

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

---

### SL-1.4 — `identity-service`: refresh token + RBAC + `/v1/users/me`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-1.5 — `catalog-service`: skeleton + Flyway + category/product CRUD

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-1.6 — `catalog-service`: SKU + phân trang/filter + seed data

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-1.7 — E2E smoke test đầy đủ + runbook Phase 1

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

### SL-1.8 — OpenAPI spec cho các endpoint Phase 1

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Todo |
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

**Kết quả verify** *(điền khi Done)*

**Learning note** *(điền khi Done)*

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

---

## 7. Changelog

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
| 2026-11-03 | Merge PR [#5](https://github.com/anchu3003/shop-lab-microservices/pull/5) bằng **squash** (`60e8afe`). Run trên `main` xanh: 3/3 job, 2m13s → badge CI `passing`. |
| 2026-11-03 | Đóng `SL-0.5` → `Done`: **6/6 AC** đạt. **Phase 0 → 8 / 8 ✅ Done** — điều kiện hoàn thành phase đã đủ: `docker compose up` infra `healthy` · service template `/health` 200 · CI xanh · `architecture.md` §11 không còn chữ "cần chốt". Việc tiếp theo: breakdown **Phase 1** (epic → ticket chi tiết). |
| 2026-11-03 | **Tách archive**: chuyển toàn bộ Phase 0 (8 ticket + bối cảnh/AC/quyết định/verify/learning note) sang [`Backlog-phase-0.md`](Backlog-phase-0.md); `Backlog.md` còn **260 dòng** (bảng phase · convention · pointer + bảng archive · epic Phase 1–8 · Learning log · Changelog). Cập nhật tham chiếu ở `README.md`, `architecture.md`, ADR-0001…0004, `.clinerules/00-context.md`, `AGENTS.md` §1/§6. |
| 2026-11-03 | **Sửa rule git**: commit minor (đóng ticket / update backlog / sửa doc / tách archive) **không** tạo nhánh + PR riêng — gộp vào nhánh của **ticket kế tiếp**; chỉ tách PR riêng khi hết ticket kế tiếp. Ghi vào `AGENTS.md` §6 · `.clinerules/10-git-workflow.md` · `docs/git-flow.md` · workflow `/close-ticket.md` (bước 8–9). |
| 2026-11-03 | Chốt **ADR-[0005](docs/adr/0005-phase-snapshot-and-release.md)** + dựng `docs/phases/` ([index](docs/phases/README.md) + [`phase-0.md`](docs/phases/phase-0.md)): tag `phase-N` + **GitHub Release** + doc snapshot, **không** dùng release branch ("lazy D"). `AGENTS.md` §5 thêm **DoD của một phase**; `/close-ticket.md` thêm bước 8 & 10 (tách archive, tag/release sau merge). |
| 2026-11-03 | Tạo **tag + GitHub Release `phase-0`** trên merge commit `259a7e9` (ADR-0005). Verify: push tag **không** trigger CI (workflow chỉ khai `on.push.branches`) → 0 phút Actions; `git show phase-0:<file>` đọc được snapshot. |
| 2026-11-03 | `SL-1.0` → `Done`: tối ưu chi phí CI — `paths-ignore` (bỏ run chỉ-docs), `push` chỉ trên `main` (hết run trùng), kill-switch `vars.CI_ENABLED`, bỏ qua draft PR (`ready_for_review`). Verify thật: push nhánh feature **0 run**, draft → run `skipped` (0 phút), `CI_ENABLED=false` + re-run → `skipped`, `true` → 3/3 xanh, merge → run `main` xanh, **PR chỉ-docs → 0 run**. Cùng kịch bản: **8 phút vs ~19,5 phút (−59%)**. Merge PR [#7](https://github.com/anchu3003/shop-lab-microservices/pull/7) (squash). **Phase 1 → 1 / 1**. |
| 2026-11-03 | **Breakdown Phase 1** → 9 ticket chi tiết: `SL-1.0` Done · `SL-1.1`…`SL-1.8` Todo (identity register/login + JWT RS256 + JWKS · gateway routing/verify JWT · **E2E sớm** · refresh token + RBAC · catalog CRUD · SKU/paging/seed · E2E đầy đủ + runbook · OpenAPI spec). Chốt 7 quyết định khi breakdown (gateway **MVC tự viết** · JWT **RS256 + JWKS** · **JPA + Flyway** · **có Testcontainers** · OpenAPI **sinh từ code** · smoke chạy **local** · register luôn là `buyer`). |




