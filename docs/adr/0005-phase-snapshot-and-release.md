# ADR-0005 — Lưu trữ snapshot theo phase (tag + GitHub Release + doc snapshot)

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-03 |
| **Liên quan** | `Backlog.md` (đóng Phase 0); ADR-0001 (tech stack); `architecture.md` §12; `docs/phases/`; `Backlog-phase-0.md` |

---

## Context

Repo này học theo **phase**, và mỗi phase là một "trạng thái học" khác nhau: Phase 0 chỉ có hạ tầng + 1 service
mẫu, Phase 1 thêm 3 service, Phase 3 đổi sang event-driven, Phase 4 thêm cache/rate limit, Phase 7 lên K8s.
Vấn đề: **code của phase cũ sẽ biến mất khỏi tầm nhìn** khi `main` đi tiếp — nhưng đúng những thứ đó lại là thứ
sẽ cần xem lại (cách chạy, số liệu baseline, quyết định tại thời điểm đó).

Hai nhu cầu **khác nhau** cần phân biệt:

1. **Đọc lại** một snapshot (xem code, đọc docs, so sánh giữa 2 phase) — chỉ cần con trỏ bất biến.
2. **Sửa lại** một snapshot (fix nhỏ, pin dependency để chạy lại benchmark) — cần một nhánh ghi được.

Ràng buộc:

- `main` là **đường sống duy nhất**, không nuôi nhánh dài hạn (`AGENTS.md` §6, `docs/git-flow.md`).
- Repo **private + GitHub Free** → Actions tính theo phút (2.000 phút/tháng); mọi thứ chạy thêm đều có giá.
- `Backlog-phase-<N>.md` đã lo phần **doc** (ticket + verify + learning note) nhưng **không** phải snapshot *code*.
- `AGENTS.md` §3.4: không thêm hạ tầng/registry ngoài scope phase hiện tại (registry để Phase 7).
- Máy dev 15 GiB RAM, đã dùng ~150 MiB cho hạ tầng → không được giữ thứ "nặng" chỉ để lưu trữ.

## Options

### 1. Cơ chế đánh dấu snapshot

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **A. Tag annotated `phase-N`** | Bất biến, **0 chi phí**; `git checkout phase-0` cho toàn bộ repo tại thời điểm đó (code + compose + docs); `git diff phase-3 phase-4 --stat` cho thấy **đúng những gì phase đó thêm** | Không nhận fix (bất biến); GitHub UI hơi ẩn (phải vào tab Tags); push tag **không** trigger CI (workflow chỉ khai `on.push.branches`) | **Luôn nên có** — nền tảng của mọi cách khác |
| **B. GitHub Release trên tag đó** | Có **trang release** + notes (stack · số liệu · cách chạy · link ADR/archive); tab *Releases* thành **timeline các phase**; tải source zip | Thêm 1 bước viết notes | **Nên có** — "mục lục" dễ đọc nhất |
| **C. `docs/phases/phase-N.md`** | Snapshot kỹ thuật cho người đọc: stack · service map · lệnh chạy **ở tag đó** · số liệu · **cái gì chưa có** · bài học | Viết tay; nếu viết khi phase còn mở thì dễ lệch | **Nên có** — trả lời "phase này khác phase kia ở đâu" |
| **D. Nhánh bảo trì `maint/phase-N`** | Sửa được snapshot mà không đụng `main`; backport/cherry-pick; CI chạy được trên snapshot; hiện trong branch list | **Rot** (dependency/CI lỗi thời → chạy lại dễ đỏ vì lý do không liên quan); **phân kỳ doc** (mỗi lần sửa doc chung là snapshot lệch thêm); branch sprawl (9 phase = 9 nhánh + 9 quyết định backport); tốn phút Actions | Chỉ khi **cần sửa** snapshot (không áp dụng cho `main`) |
| **E. Image tag theo phase** (`shoplab-*:phase-4`) | Chạy lại đúng artifact, không cần build | Cần registry (roadmap để Phase 7); 252 MB/phase; phải rebuild khi Dockerfile đổi | Từ **Phase 4** khi cần so sánh runtime/load test giữa ≥ 2 snapshot |
| **F. Copy code vào `archive/phase-N/`** | Xem trực tiếp trong repo | Duplicate code, repo phình, drift, CI/build phải ignore | ❌ **Không dùng** |
| **G. Repo/orphan branch riêng cho archive** | Repo sống gọn tuyệt đối | Nặng, phá lịch sử liên tục, khó `diff` giữa phase, phải clone 2 nơi | ❌ Over-engineering |

**Điểm quyết định giữa A và D**: *tag cho phép tạo nhánh bất cứ lúc nào* —
`git switch -c maint/phase-4 phase-4`. Nghĩa là **giữ nhánh thường trực không cho thêm khả năng nào**, chỉ thêm
chi phí. Đây gọi là **"lazy D"**: mở nhánh khi thực sự có nhu cầu sửa snapshot.

### 2. Tên gọi & vị trí

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **`phase-N`** (khớp `Backlog-phase-N.md`) | Đọc là hiểu; sắp xếp tự nhiên `phase-0…phase-8`; không lẫn với version phần mềm | Không phải semver | Lab học theo phase (trường hợp này) |
| **Semver `v0.1.0`, `v0.2.0`** | Chuẩn phổ biến, hợp khi publish | Phải nhớ map version ↔ phase; phase ≠ release | Khi repo public / publish image cho người ngoài dùng |
| **Ngày `2026-11-03`** | Không trùng | Không nói được đang ở phase nào | ❌ Không dùng làm tag chính |

## Decision

Chọn **A + B + C**, và coi **D là "lazy D"**:

1. **Tag annotated `phase-N`** gắn trên **merge commit cuối cùng của phase** (commit đóng ticket cuối của phase).
   Message của tag ghi: ticket ID trong phase · số liệu chính · lệnh chạy ở snapshot.
2. **GitHub Release** cho tag đó; notes **tái dùng** `Backlog-phase-N.md` (không viết lại) + link `docs/phases/phase-N.md`.
3. **`docs/phases/phase-N.md`** — snapshot doc (stack · có gì · cách chạy **ở tag đó** · số liệu · cái gì **chưa** có ·
   bài học), viết **sau khi** phase đóng. `docs/phases/README.md` là index.
4. **Không** giữ nhánh thường trực cho snapshot. Khi cần sửa:
   `git switch -c maint/phase-N phase-N` → lúc đó nhánh mới tồn tại; nếu phải bảo trì dài thì giữ nó lại.

**Lý do loại các phương án khác:**

- **D thường trực**: trả giá rot + phân kỳ doc **ngay từ đầu** cho một nhu cầu (patch snapshot) chưa xuất hiện —
  trong khi "lazy D" đã phủ 100% khả năng mà không cần giữ nhánh.
- **E (image tag)**: chưa có registry (roadmap để Phase 7) và Phase 0–3 chưa có nhu cầu so sánh runtime.
- **F (copy code)**: duplicate code, phá "single source of truth", CI/build phải ignore — đổi lấy lợi ích không có.
- **G (repo riêng)**: nặng, phá lịch sử liên tục, và `diff` giữa 2 phase gần như không làm được — trong khi `diff`
  giữa các phase lại chính là thứ cần.
- **Semver**: repo private, chưa publish → semver chỉ thêm một lớp map phải nhớ.

## Consequences

**Được:**

- Tra cứu 1 snapshot = 3 lệnh, không cần dựng lại gì:
  `gh release view phase-0` (đọc) → `git checkout phase-0` (xem code) → `git diff phase-0 phase-1 --stat`
  (thấy đúng phase sau thêm gì).
- **0 phút Actions thêm**: push tag **không** trigger workflow (workflow chỉ khai `on.push.branches`).
- Tab *Releases* trở thành **timeline** của dự án — đúng nhu cầu "xem lại sau này".
- `main` vẫn là đường sống duy nhất; **không** thêm nhánh nào phải bảo trì.

**Mất / chấp nhận:**

- Tag **bất biến** → muốn sửa snapshot phải mở nhánh (lazy D) rồi tạo tag patch `phase-4.1`. Hành vi đúng
  (giống patch release) nhưng cần nhớ.
- `docs/phases/phase-N.md` viết tay → dễ lệch nếu phase còn đang thay đổi ⇒ **quy ước: viết sau khi phase đóng**.
- Repo **private** → Release/tag chỉ mình thấy (đủ cho mục đích học; chuyển public thì không phải sửa gì).
- Thêm 1 bước vào quy trình đóng phase (viết snapshot doc + tạo tag/release) — ước tính **~10 phút/phase**.

**Ảnh hưởng tới file khác:**

- `AGENTS.md` §5: thêm **Definition of Done của một phase** (checklist).
- `Backlog.md`: bảng "Archive các phase đã xong" thêm cột **Doc snapshot** + **Tag / Release**.
- `.clinerules/workflows/close-ticket.md`: bổ sung bước tạo tag/release khi phase đóng.
- `architecture.md` §12: khi phase xong thì đánh dấu trong roadmap.

## Revisit

- **Mở `maint/phase-N`** khi có commit **chỉ có nghĩa với snapshot cũ** và không áp dụng cho `main`
  (ví dụ: sửa script benchmark, pin lại dependency để chạy lại số liệu Phase 4, hoặc fix bug chỉ tồn tại ở code cũ).
  Đây chính là lúc "lazy D" được kích hoạt.
- **Thêm image tag theo phase (E)** từ **Phase 4** khi cần so sánh runtime/load test giữa **≥ 2 snapshot**
  (điều kiện: đã có registry + có ≥ 2 bộ số liệu cần đối chiếu).
- **Đổi sang semver** khi repo chuyển **public** hoặc publish image cho người ngoài dùng.
- **Bỏ `docs/phases/`** nếu sau **3 phase liên tiếp** không có commit nào chạm `docs/phases/` ngoài lúc đóng phase
  → nghĩa là không ai đọc lại; giữ chỉ tốn công viết.
- **Revisit cách gắn tag** nếu phase bị tách thành nhiều nhánh song song — khi đó tag theo phase không còn là
  một điểm duy nhất trên `main`.
