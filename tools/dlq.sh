#!/usr/bin/env bash
# ==============================================================================
# tools/dlq.sh — xem & replay message trong dead letter queue (SL-3.4)
#
# Vì sao cần: DLQ là nơi message đi khi retry cạn. Không có lệnh này thì "replay" = mở RabbitMQ UI,
# copy payload, publish tay — dễ sai và không ai làm khi đang có sự cố. ADR-0007 D5: **không
# auto-replay** — replay là quyết định của con người nên phải là lệnh tường minh, có log.
#
# ⚠️ Bẫy quan trọng: trong DLQ, `routing_key` của message là **TÊN QUEUE** (DLX đặt routing key =
# tên queue, ADR-0007 D2), KHÔNG phải routing key gốc. Routing key gốc nằm ở header
# `x-death[0].routing-keys`. Replay bằng tên queue ⇒ `routed=false` ⇒ **message mất**. Script này đọc
# x-death, và chỉ consume message **sau khi** đã validate (peek trước, ack sau).
#
# Dùng:
#   bash tools/dlq.sh list                   # DLQ nào đang có message
#   bash tools/dlq.sh peek q.notification    # xem 1 message (không consume)
#   bash tools/dlq.sh move q.notification    # replay 1 message về queue chính (routing key GỐC)
#   bash tools/dlq.sh purge q.notification   # xoá sạch DLQ
#
# Chỉ dùng curl + python3 (máy dev không có `jq`).
# ==============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -f "$REPO_ROOT/.env" ]; then
  set -a; . "$REPO_ROOT/.env"; set +a
fi

HOST="${RABBITMQ_MANAGEMENT_HOST:-localhost}"
PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"
USER="${RABBITMQ_USER:-shoplab}"
PASS="${RABBITMQ_PASSWORD:-}"
API="http://${HOST}:${PORT}/api"

[ -n "$PASS" ] || { echo "LỖI: RABBITMQ_PASSWORD rỗng (kiểm .env)" >&2; exit 1; }

api() { curl -sS -u "$USER:$PASS" -H 'content-type: application/json' "$@"; }
need_queue() { [ -n "${1:-}" ] || { echo "thiếu tên queue (vd q.notification) — xem: bash tools/dlq.sh list" >&2; exit 2; }; }
dlq_name() { case "$1" in *.dlq) printf %s "$1" ;; *) printf %s "$1.dlq" ;; esac; }

# Dùng FILE python tạm cho phần parse: `python3 -c` nhiều dòng trong bash là bẫy quoting thật
# (đã làm mất 1 message ở chính ticket này). File tạm + heredoc quoted ⇒ không có expansion nào.
PY_LIST="$(mktemp)"
cat > "$PY_LIST" <<'PY'
import json, sys
rows = [q for q in json.load(sys.stdin) if q["name"].endswith(".dlq")]
if not rows:
    print("  (không có DLQ nào — tốt)")
for q in sorted(rows, key=lambda q: q["name"]):
    print("  {:24s} ready={:3d}".format(q["name"], q.get("messages_ready", 0)))
PY

PY_PEEK="$(mktemp)"
cat > "$PY_PEEK" <<'PY'
import json, sys
msgs = json.load(sys.stdin)
if not msgs:
    print("  (DLQ rỗng)")
for m in msgs:
    deaths = (m.get("properties", {}).get("headers") or {}).get("x-death") or []
    keys = (deaths[0].get("routing-keys") or []) if deaths else []
    print("  routing_key(hiện tại)={} · routing key GỐC={}".format(m["routing_key"], keys[0] if keys else "?"))
    print("  payload={}".format(m["payload"][:400]))
PY

PY_PLAN="$(mktemp)"
cat > "$PY_PLAN" <<'PY'
import json, sys
msgs = json.load(sys.stdin)
if not msgs:
    sys.exit(0)
m = msgs[0]
deaths = (m.get("properties", {}).get("headers") or {}).get("x-death") or []
keys = (deaths[0].get("routing-keys") or []) if deaths else []
if not keys:
    sys.stderr.write("  message trong DLQ không có x-death.routing-keys — không xác định được routing key gốc\n")
    sys.exit(0)
json.loads(m["payload"])  # validate TRƯỚC khi consume: payload hỏng thì đừng lấy ra khỏi DLQ
print(keys[0] + "\t" + m["payload"])
PY

cleanup() { rm -f "$PY_LIST" "$PY_PEEK" "$PY_PLAN"; }
trap cleanup EXIT

case "${1:-list}" in
  list)
    api "$API/queues" | python3 "$PY_LIST"
    ;;

  peek)
    need_queue "${2:-}"
    DLQ="$(dlq_name "$2")"
    api -X POST "$API/queues/%2F/$DLQ/get" \
      -d '{"count":1,"ackmode":"ack_requeue_true","encoding":"auto"}' | python3 "$PY_PEEK"
    ;;

  move)
    need_queue "${2:-}"
    # Bước 1: peek (requeue_true) để đọc routing key GỐC + validate payload — chưa consume gì.
    DLQ="$(dlq_name "$2")"
    PLAN="$(api -X POST "$API/queues/%2F/$DLQ/get" \
      -d '{"count":1,"ackmode":"ack_requeue_true","encoding":"auto"}' | python3 "$PY_PLAN")"
    if [ -z "$PLAN" ]; then
      echo "  (DLQ rỗng hoặc message không dùng được — KHÔNG consume gì)" >&2
      exit 0
    fi
    ORIG_KEY="$(printf '%s' "$PLAN" | cut -f1)"
    BODY="$(printf '%s' "$PLAN" | cut -f2-)"

    # Bước 2: đã chắc chắn ⇒ lấy message ra khỏi DLQ.
    api -X POST "$API/queues/%2F/$DLQ/get" \
      -d '{"count":1,"ackmode":"ack_requeue_false","encoding":"auto"}' >/dev/null

    # Bước 3: publish lại với routing key GỐC (không phải tên queue).
    PAYLOAD="$(python3 - "$ORIG_KEY" "$BODY" <<'PY'
import json, sys
key, body = sys.argv[1], sys.argv[2]
print(json.dumps({"properties": {"delivery_mode": 2}, "routing_key": key,
                  "payload": body, "payload_encoding": "string"}))
PY
)"
    RESULT="$(api -X POST "$API/exchanges/%2F/shoplab.events/publish" -d "$PAYLOAD")"
    echo "  replay 1 message từ $DLQ → shoplab.events (routing key GỐC = $ORIG_KEY) → $RESULT"
    case "$RESULT" in
      *'"routed":true'*) : ;;
      *) echo "  ⚠️ routed=false ⇒ routing key gốc không còn binding, message đã ra khỏi DLQ. Kiểm topology." >&2 ;;
    esac
    echo "  → kiểm: bash tools/rabbit-topology.sh | grep q."
    ;;

  purge)
    need_queue "${2:-}"
    DLQ="$(dlq_name "$2")"
    api -X DELETE "$API/queues/%2F/$DLQ/contents" >/dev/null
    echo "purge $2 — xong"
    ;;

  *)
    echo "Lệnh không hợp lệ: $1 (dùng: list | peek | move | purge)" >&2
    exit 2
    ;;
esac
