#!/usr/bin/env bash
# ==============================================================================
# tools/rabbit-topology.sh — xem & thao tác nhanh topology RabbitMQ (SL-3.1)
#
# Vì sao cần: routing key sai một ký tự thì RabbitMQ KHÔNG báo lỗi — message rơi vào hư không và chỉ
# lộ ra khi thiếu email/index. Lệnh này để nhìn đúng thứ broker đang có (không phải thứ mình nghĩ).
#
# Dùng:
#   bash tools/rabbit-topology.sh                    # bảng exchange + queue + binding
#   bash tools/rabbit-topology.sh publish <routingKey> ['{"json":1}']
#   bash tools/rabbit-topology.sh drain <queue> [n]  # lấy & ack message (kiểm tra "consume được")
#   bash tools/rabbit-topology.sh purge <queue>      # xoá sạch message trong 1 queue
#   bash tools/rabbit-topology.sh dlq                # chỉ xem các queue .dlq
#
# Lưu ý: số liệu của management API được tổng hợp ~5 s/lần ⇒ ngay sau restart broker, `ready` có thể
# còn là số CŨ. Muốn chắc thì chờ vài giây hoặc dùng `drain` để ack thật.
#
# Chỉ dùng curl + python3 (máy dev KHÔNG có `jq`) — cùng quy ước với tools/e2e-*.sh.
# ==============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Nạp .env (không `set -a` vì `.env` chứa giá trị có dấu cách/`#`).
if [ -f "$REPO_ROOT/.env" ]; then
  # shellcheck disable=SC1091
  set -a; . "$REPO_ROOT/.env"; set +a
fi

MGMT_HOST="${RABBITMQ_MANAGEMENT_HOST:-localhost}"
MGMT_PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"
USER="${RABBITMQ_USER:-shoplab}"
PASS="${RABBITMQ_PASSWORD:-}"
EXCHANGE="shoplab.events"
DLX="shoplab.dlx"
API="http://${MGMT_HOST}:${MGMT_PORT}/api"

if [ -z "$PASS" ]; then
  echo "LỖI: RABBITMQ_PASSWORD rỗng — kiểm .env (cp .env.example .env rồi set lại)." >&2
  exit 1
fi

api() { curl -sS -u "$USER:$PASS" -H 'content-type: application/json' "$@"; }

case "${1:-show}" in
  show)
    echo "== Exchanges (shoplab.*)"
    api "$API/exchanges" | python3 -c "
import json, sys
for e in json.load(sys.stdin):
    if e['name'].startswith('shoplab'):
        print('  {:20s} type={:8s} durable={}'.format(e['name'], e['type'], e['durable']))
"
    echo
    echo "== Queues (ready = chờ consumer · unacked = consumer đang giữ/đang retry)"
    api "$API/queues" | python3 -c "
import json, sys
rows = [q for q in json.load(sys.stdin) if q['name'].startswith('q.')]
if not rows:
    print('  (chưa có queue nào — service có AMQP đã chạy chưa? xem: docker logs shoplab-order-service)')
for q in sorted(rows, key=lambda q: q['name']):
    print('  {:22s} ready={:3d} unacked={:3d} consumers={}'.format(
        q['name'], q.get('messages_ready', 0), q.get('messages_unacknowledged', 0), q.get('consumers', 0)))
"
    echo
    echo "== Bindings"
    api "$API/bindings" | python3 -c "
import json, sys
for b in json.load(sys.stdin):
    if b['source'].startswith('shoplab'):
        key = b['routing_key'] or '(fanout)'
        print('  {:14s} --{:26s}--> {}'.format(b['source'], key, b['destination']))
"
    ;;

  dlq)
    api "$API/queues" | python3 -c "
import json, sys
rows = [q for q in json.load(sys.stdin) if q['name'].endswith('.dlq')]
if not rows:
    print('  (không có queue .dlq nào)')
for q in sorted(rows, key=lambda q: q['name']):
    print('  {:22s} ready={}'.format(q['name'], q.get('messages_ready', 0)))
"
    ;;

  publish)
    KEY="${2:?thiếu routingKey — vd: bash tools/rabbit-topology.sh publish order.confirmed.v1}"
    BODY="${3:-{\"ping\":true}}"
    PAYLOAD="$(python3 - "$KEY" "$BODY" <<'PY'
import json, sys
key, body = sys.argv[1], sys.argv[2]
try:
    parsed = json.loads(body)
except json.JSONDecodeError as exc:
    sys.exit("body không phải JSON hợp lệ: %s" % exc)
# delivery_mode=2 = persistent: message phải còn sau khi broker restart (AC của SL-3.1).
print(json.dumps({"properties": {"delivery_mode": 2}, "routing_key": key,
                  "payload": json.dumps(parsed), "payload_encoding": "string"}))
PY
)"
    RESULT="$(api -X POST "$API/exchanges/%2F/$EXCHANGE/publish" -d "$PAYLOAD")"
    echo "publish $KEY -> $RESULT"
    echo "  (routed=false nghĩa là KHÔNG queue nào bind routing key đó — sai tên key!)"
    ;;

  drain)
    QUEUE="${2:?thiếu queue — vd: bash tools/rabbit-topology.sh drain q.search 1}"
    COUNT="${3:-1}"
    api -X POST "$API/queues/%2F/$QUEUE/get" \
      -d "{\"count\":$COUNT,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}" \
      | python3 -c "
import json, sys
msgs = json.load(sys.stdin)
if not msgs:
    print('  (queue rỗng)')
for m in msgs:
    print('  ack  routing_key={}  payload={}'.format(m['routing_key'], m['payload'][:120]))
"
    ;;

  purge)
    QUEUE="${2:?thiếu queue}"
    api -X DELETE "$API/queues/%2F/$QUEUE/contents" >/dev/null
    echo "purge $QUEUE — xong"
    ;;

  *)
    echo "Lệnh không hợp lệ: $1" >&2
    sed -n '11,17p' "$0" >&2
    exit 1
    ;;
esac
