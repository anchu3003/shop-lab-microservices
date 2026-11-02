# ADR-0002 — Môi trường dev: WSL2

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-02 |
| **Liên quan** | `Backlog.md` → `SL-0.7`, `SL-0.2`; `architecture.md` §10; ADR-0001 |

---

## Context

- Stack đã chốt ở ADR-0001: **Java 21 + Spring Boot** (Phase 1), **Go** (Phase 2–3), nhiều container
  (PostgreSQL, Redis, RabbitMQ, OpenSearch, MinIO), Phase 7 có `kind`/K8s.
- Máy dev chạy **Windows**; Docker Desktop trên Windows dùng **WSL2 backend**.
- Vấn đề đã biết khi để source code trên Windows filesystem (`D:\` / `/mnt/d/...`) rồi bind mount vào
  Linux container:
  - I/O **chậm** (Docker docs: *"Performance is much higher when files are bind-mounted from the Linux
    filesystem"*), đặc biệt với workload nhiều file nhỏ (build Java/Go).
  - **inotify** (file watch / hot reload) **không** hoạt động qua ranh giới OS.
  - **Permission** (chmod, file mode) sai/lệch.
  - **Line endings** CRLF ↔ LF dễ lỗi.

## Options

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Windows-native** (repo ở `D:\`) | Quen sẵn, GUI tooling native | Bind mount chậm; inotify/permission/CRLF không đáng tin | Ít bind mount, chủ yếu dùng tooling Windows |
| **WSL2** (repo trong `~/`) | I/O nhanh; inotify OK; permission đúng; `kind`/K8s mượt; giống prod (Linux) | Phải làm quen WSL; cần giới hạn RAM cho `vmmem` | Dev container-heavy trên Windows |
| **Hybrid** (code ở Windows, container chạy trong WSL2) | Giữ GUI Windows | Vẫn bind mount qua ranh giới OS → giữ nguyên nhược điểm | Không khuyến nghị |

## Decision

Chuyển môi trường dev sang **WSL2**. Repo đặt trong **Linux filesystem** của WSL
(`~/MyGit/Microservices_2026`), **KHÔNG** ở `/mnt/d/...`.

- VS Code + extension **WSL** (Remote-WSL) để mở repo.
- Docker Desktop bật **WSL integration**.
- **Git chạy trong WSL** (tránh CRLF + perf).
- Giới hạn RAM/CPU cho WSL2 qua `%UserProfile%\.wslconfig` (Windows side).
- Yêu cầu WSL **≥ 2.1.5** (Docker Desktop).

**Lý do loại phương án khác:**

- **Windows-native** — loại: bind mount chậm + inotify/permission/CRLF không đáng tin → ảnh hưởng trực
  tiếp dev loop (build, hot reload, test).
- **Hybrid** — loại: vẫn giữ source trên Windows fs nên **không** giải quyết được bind mount chậm.

## Consequences

- `SL-0.2` phải cài **JDK 21 + Maven/Gradle + Lefthook** *trong WSL*; `README.md` ghi đường dẫn `~/`.
- `SL-0.3` (Docker Compose): bind mount trỏ Linux fs; dữ liệu dùng **named volume** (đã ghi trong ticket).
- Phase 7 (`kind`): chạy native trong WSL → mượt hơn so với Windows-native.
- Cần `%UserProfile%\.wslconfig` để `vmmem` không ngốn hết RAM (OpenSearch + kind khá nặng).
- GUI tools Windows (DBeaver, Postman…) vẫn kết nối qua `localhost` (WSL2 có localhost forwarding).
- Toàn bộ `.clinerules/`, `.cline/`, `AGENTS.md` đi kèm repo — không đổi nội dung.

## Revisit

- Revisit khi dev loop bị nghẽn vì WSL (networking/disk) **nhiều hơn** lợi ích, hoặc cần tooling
  Windows-specific/GPU.
- Revisit khi `%UserProfile%\.wslconfig` cấu hình không đủ RAM để chạy OpenSearch + `kind` cùng lúc →
  tăng RAM máy hoặc tách workload.
