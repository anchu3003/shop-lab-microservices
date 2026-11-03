# 00 — Context loader (Memory Bank lite)

Rule phụ trợ. **Nguồn sự thật vẫn là `AGENTS.md`** — không lặp lại nội dung ở đây để tránh trùng/lệch.

## Khi bắt đầu một task

Đọc theo thứ tự:

1. `AGENTS.md` — working agreement (ngôn ngữ, nguyên tắc, convention, DoR/DoD).
2. `architecture.md` — kiến trúc, tech stack, roadmap, glossary.
3. `Backlog.md` — ticket đang làm / tiếp theo, learning log, bảng archive.
   Phase đã xong → chi tiết nằm ở `Backlog-phase-<N>.md` (chỉ đọc khi cần tra cứu).

Không tạo `memory-bank/` riêng. Hai file `architecture.md` + `Backlog.md` đóng vai **Memory Bank**.

## Nhắc nhanh (chi tiết xem `AGENTS.md`)

- Mỗi lượt chỉ làm **1 ticket** trong `Backlog.md` — không build một loạt.
- Trả lời **tiếng Việt**, giữ keyword tiếng Anh (`AGENTS.md` §2).
- Quyết định kiến trúc lớn → ADR trong `docs/adr/` + update `architecture.md`.
- Không claim "đã test" khi chưa chạy thật (`AGENTS.md` §8, §9).

## AI assets đang có

| Loại | Vị trí | Gọi bằng |
|---|---|---|
| Rule | `.clinerules/10-git-workflow.md` | luôn active (commit sau mỗi ticket, nhánh, PR) |
| Skill | `.cline/skills/shoplab-adr` | auto khi viết ADR, hoặc `/shoplab-adr` |
| Skill | `.cline/skills/shoplab-ticket` | auto khi tạo/sửa ticket, hoặc `/shoplab-ticket` |
| Skill | `.cline/skills/shoplab-verify` | auto khi verify, hoặc `/shoplab-verify` |
| Workflow | `.clinerules/workflows/run-ticket.md` | `/run-ticket.md` |
| Workflow | `.clinerules/workflows/new-ticket.md` | `/new-ticket.md` |
| Workflow | `.clinerules/workflows/close-ticket.md` | `/close-ticket.md` |
| Workflow | `.clinerules/workflows/git-branch-pr.md` | `/git-branch-pr.md` |

Lưu ý: Cline **Hooks / Plugins không chạy trên VS Code extension** (chỉ SDK/CLI/Kanban). Git hooks
(Lefthook + Conventional Commits) thuộc `SL-0.2`.
