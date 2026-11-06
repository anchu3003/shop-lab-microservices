# Snapshot theo phase — ShopLab

> Mỗi phase là một **trạng thái học** khác nhau (tech stack, quy mô, pattern). File này là **index** để tra lại
> về sau. Quyết định + lý do: [ADR-0005](../adr/0005-phase-snapshot-and-release.md).
> Ticket chi tiết của từng phase: [`Backlog-phase-<N>.md`](../../Backlog.md).

## Cách dùng (3 lệnh)

```bash
gh release view phase-1            # 1) đọc release notes: stack · số liệu · cách chạy
git checkout phase-1               # 2) xem code + docs tại snapshot đó
git diff phase-1 phase-2 --stat    # 3) thấy ĐÚNG những gì phase sau thêm
```

Muốn **sửa** snapshot (không đụng `main`) → mở nhánh từ tag (chỉ khi thực sự cần — xem ADR-0005 → Revisit):

```bash
git fetch --tags
git switch -c maint/phase-0 phase-0
```

## Danh sách snapshot

| Phase | Tag / Release | Doc snapshot | Ticket archive | Stack chính | Trạng thái |
|---|---|---|---|---|---|
| **0** — Foundation | `phase-0` | [`phase-0.md`](phase-0.md) | [`Backlog-phase-0.md`](../../Backlog-phase-0.md) | Java 21 · Spring Boot 4.1.2 · Docker Compose · GitHub Actions | ✅ Done 2026-11-03 |
| **1** — Walking skeleton | `phase-1` | [`phase-1.md`](phase-1.md) | [`Backlog-phase-1.md`](../../Backlog-phase-1.md) | + `api-gateway` · `identity-service` · `catalog-service` · JWT RS256 + refresh rotate (grace 30s) · OpenAPI | ✅ Done 2026-11-06 |
| **2** — Core commerce flow | `phase-2` | `phase-2.md` | `Backlog-phase-2.md` | + cart (Redis) · inventory (reserve/commit) · promotion · order (Saga) · payment | 🟡 Breakdown xong (11 ticket) |
| **3** — Async & event-driven | `phase-3` | `phase-3.md` | `Backlog-phase-3.md` | + RabbitMQ/Kafka · outbox · DLQ · search index | ⚪ Todo |
| **4** — Scale & performance | `phase-4` | `phase-4.md` | `Backlog-phase-4.md` | + cache-aside · rate limit · read replica · partition · k6 | ⚪ Todo |
| **5** — Resilience | `phase-5` | `phase-5.md` | `Backlog-phase-5.md` | + timeout/retry · circuit breaker · bulkhead · backpressure | ⚪ Todo |
| **6** — Observability & SRE | `phase-6` | `phase-6.md` | `Backlog-phase-6.md` | + OpenTelemetry · Prometheus/Grafana · SLO/SLI · alert | ⚪ Todo |
| **7** — Deployment & infra | `phase-7` | `phase-7.md` | `Backlog-phase-7.md` | + K8s (kind) · Helm · HPA · blue-green/canary · registry | ⚪ Todo |
| **8** — Advanced | `phase-8` | `phase-8.md` | `Backlog-phase-8.md` | + CQRS read model · OpenSearch · sharding · CDC · Kafka | ⚪ Todo |

## Quy ước

- **Tag**: annotated, tên `phase-N`, gắn trên **merge commit cuối của phase** (commit đóng ticket cuối).
  Message ghi: ticket ID trong phase · số liệu chính · lệnh chạy ở snapshot.
- **Release**: tạo cùng tag, notes tái dùng `Backlog-phase-N.md` + link `docs/phases/phase-N.md`.
- **`phase-N.md`**: viết **sau khi** phase đóng (tránh doc lệch với thực tế đang thay đổi).
- **Nhánh**: **không** giữ nhánh thường trực cho snapshot — chỉ mở `maint/phase-N` từ tag khi cần patch.
- **Tag không trigger CI** (workflow chỉ khai `on.push.branches`) → tạo tag/release không tốn phút Actions.
