#!/usr/bin/env bash
# ==============================================================================
# bench-async.sh — đo số liệu thật cho báo cáo Phase 3 (SL-3.8)
#
# Vì sao có script này (không phải "chạy vài lệnh rồi chép số"):
#   Mọi con số trong `docs/reports/async-phase-3.md` phải **tái lập được** bằng một lệnh. Số đo rồi
#   chép tay từ trí nhớ là số không kiểm chứng được — đúng thứ ticket này muốn tránh.
#
# 5 phép đo (n và điều kiện theo ticket):
#   M1  latency `POST /v1/checkout`: 50 tuần tự + 20 đồng thời, **before** (không có consumer async)
#       và **after** (notification-service chạy)
#   M2  độ trễ event: confirm → email (`notification_logs.sent_at`) và webhook → order CONFIRMED
#   M3  throughput relay: bơm 500 `product.updated.v1`, đo msg/s từ outbox
#   M4  DLQ = 0 + không mất/trùng: 100 checkout + 100 update, cửa sổ 5 phút
#   M5  degradation: stop notification-service ⇒ p95 checkout + message tồn trong queue ⇒ bật lại
#
# Chạy (stack local đã up):
#   bash tools/bench-async.sh                 # đủ 5 phép, ~7 phút
#   N_SEQ=10 N_CONC=5 N_RELAY=50 bash tools/bench-async.sh   # bản nhanh để thử script
#
# Kết quả: in bảng markdown ra stdout + raw CSV/JSON ở /tmp/bench-async-<ts>/.
# Chỉ dùng curl + python3 + docker (máy dev không có jq/k6 — k6 là Phase 4).
# ==============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/infra/docker-compose.yml}"
if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"
PG_CONTAINER="${PG_CONTAINER:-shoplab-postgres}"
PG_USER="${POSTGRES_USER:-shoplab}"
NOTIF_CONTAINER="${NOTIF_CONTAINER:-shoplab-notification-service}"
MGMT="http://localhost:${RABBITMQ_MANAGEMENT_PORT:-15672}/api"

N_SEQ="${N_SEQ:-50}"        # số checkout tuần tự mỗi trạng thái
N_CONC="${N_CONC:-20}"      # số checkout đồng thời
N_RELAY="${N_RELAY:-500}"   # số event bơm cho phép đo throughput
N_LOAD="${N_LOAD:-100}"     # số checkout + số update cho phép đo DLQ
WINDOW_S="${WINDOW_S:-300}" # cửa sổ quan sát DLQ (giây) — ticket: 5 phút
STOCK="${STOCK:-2000}"      # tồn kho SKU bench (M1+M4+M5 ≈ 210 đơn)

TS="$(date +%s)"
RUN_KEY="bench-${TS}"
OUT="${OUT_DIR:-/tmp/bench-async-${TS}}"
mkdir -p "$OUT"
PASS=0
FAIL=0
TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

ok()  { PASS=$((PASS + 1)); echo "  [ok]   $1"; }
bad() { FAIL=$((FAIL + 1)); echo "  [FAIL] $1 — $2"; }

http_do() { # method url [extra curl args...]
  local method="$1" url="$2"
  shift 2
  : > "$TMP_BODY"
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"
  BODY="$(cat "$TMP_BODY")"
}

json_field() {
  printf '%s' "$BODY" | python3 -c \
    "import sys,json;print(json.load(sys.stdin, parse_float=str, parse_int=str).get('$1',''))" \
    2>/dev/null || true
}

psql_c() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$1" -tAc "$2" 2>/dev/null | tr -d '[:space:]'; }
# `-F ' '`: nhiều cột thì separator phải là dấu cách (diff.py split theo whitespace) — mặc định
# của `-tA` là `|`, và đó là lý do lần chạy smoke M2 ra n=0.
psql_raw() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$1" -tA -F ' ' -c "$2" 2>/dev/null; }

queue_ready() {
  curl -sS -u "${RABBITMQ_USER:-shoplab}:${RABBITMQ_PASSWORD:-}" "$MGMT/queues/%2F/$1" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin).get("messages_ready",0))' 2>/dev/null || echo -1
}

now_ms() { echo $(( $(date +%s%N) / 1000000 )); }

# nearest-rank percentile: index = ceil(p/100 * n) - 1 trên mẫu đã sort (ghi rõ trong báo cáo).
perc() { # file p
  python3 - "$1" "$2" <<'PY'
import math, sys
vals = sorted(float(x) for x in open(sys.argv[1]).read().split() if x.strip())
p = float(sys.argv[2])
if not vals:
    print("n/a"); raise SystemExit
idx = max(0, min(len(vals) - 1, math.ceil(p / 100 * len(vals)) - 1))
print(f"{vals[idx]:.1f}")
PY
}

stats() { # file → "n=… p50=… p95=… max=…"
  local f="$1"
  local n; n=$(wc -w < "$f")
  echo "n=$n p50=$(perc "$f" 50) p95=$(perc "$f" 95) max=$(python3 -c "
import sys
v=[float(x) for x in open('$f').read().split() if x.strip()]
print(f'{max(v):.1f}' if v else 'n/a')")"
}

login() {
  http_do POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  json_field accessToken
}

wait_ready() { # port timeout_s
  local port="$1"
  local timeout="$2"
  local deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/ready" || true)" = "200" ] && return 0
    sleep 1
  done
  return 1
}

wait_queue_drain() { # queue timeout_s
  local q="$1"
  local timeout="$2"
  local deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    [ "$(queue_ready "$q")" = "0" ] && return 0
    sleep 1
  done
  return 1
}

# ------------------------------------------------------------------------------
# Một đơn checkout hoàn chỉnh, chỉ đo thời gian `POST /v1/checkout`.
# Chuỗi: dọn giỏ → thêm món → checkout (ĐO) → mock-pay → confirm (để sinh event).
# ------------------------------------------------------------------------------
CHECKOUT_SEQ=0
checkout_once() { # token ms_file orders_file
  local token="$1" ms_file="$2" ids_file="$3"
  CHECKOUT_SEQ=$((CHECKOUT_SEQ + 1))
  local key="${RUN_KEY}-${CHECKOUT_SEQ}"

  http_do DELETE "$GATEWAY/v1/carts" -H "Authorization: Bearer $token"
  http_do PUT "$GATEWAY/v1/carts/items/$SKU_ID" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' -d '{"qty":1}'
  if [ "$STATUS" != "200" ]; then echo "cart=$STATUS"; return 1; fi

  local sec
  sec="$(curl -s -o "$TMP_BODY" -w '%{time_total}' -X POST "$GATEWAY/v1/checkout" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: $key" -d '{}' || echo 0)"
  BODY="$(cat "$TMP_BODY")"
  awk -v s="$sec" 'BEGIN{printf "%.1f\n", s*1000}' >> "$ms_file"

  local order intent
  order="$(json_field orderId)"
  intent="$(basename "$(json_field paymentUrl)")"
  if [ -z "$order" ]; then echo "checkout_body=$(printf '%s' "$BODY" | head -c 120)"; return 1; fi
  echo "$order" >> "$ids_file"

  http_do POST "$GATEWAY/mock-provider/pay/$intent"
  http_do POST "$GATEWAY/v1/checkout/$order/confirm" -H "Authorization: Bearer $token"
  return 0
}

# ------------------------------------------------------------------------------
# 0) Setup dữ liệu bán
# ------------------------------------------------------------------------------
echo "== bench-async (SL-3.8) · out=$OUT"
ADMIN_TOKEN="$(login "$ADMIN_EMAIL")"
SELLER_TOKEN="$(login "$SELLER_EMAIL")"
BUYER_EMAIL="${RUN_KEY}@shoplab.local"
http_do POST "$GATEWAY/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$BUYER_EMAIL\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Bench\"}"
BUYER_TOKEN="$(login "$BUYER_EMAIL")"
if [ -z "$ADMIN_TOKEN" ] || [ -z "$SELLER_TOKEN" ] || [ -z "$BUYER_TOKEN" ]; then
  echo "LỖI: không login được (stack đã up? LocalDevUserSeeder chỉ chạy khi APP_ENV=local)"; exit 1
fi
ok "setup: login admin/seller/buyer"

http_do POST "$GATEWAY/v1/categories" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Bench cat $TS\"}"
CATEGORY_ID="$(json_field id)"
http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Bench product $TS\",\"price\":\"100000.00\"}"
PRODUCT_ID="$(json_field id)"
http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"BENCH-$TS\"}"
SKU_ID="$(json_field id)"
http_do PUT "$GATEWAY/v1/stock/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"onHand\":$STOCK}"
if [ -z "$SKU_ID" ] || [ "$STATUS" != "200" ]; then echo "LỖI setup SKU/stock"; exit 1; fi
ok "setup: product=$PRODUCT_ID sku=$SKU_ID stock=$STOCK"

# Buyer riêng cho phần đồng thời — dùng chung 1 buyer thì mọi request đánh vào CÙNG cái giỏ ⇒
# ta đo tranh chấp giỏ chứ không đo checkout.
CONC_TOKENS=()
for i in $(seq 1 "$N_CONC"); do
  e="bench-${TS}-c${i}@shoplab.local"
  http_do POST "$GATEWAY/v1/auth/register" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$e\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Bench c$i\"}"
  CONC_TOKENS+=("$(login "$e")")
done
ok "setup: $N_CONC buyer cho đo đồng thời"


# ------------------------------------------------------------------------------
# M1 — latency checkout: before (không có consumer async) / after (có consumer)
# ------------------------------------------------------------------------------
run_phase() { # label ms_file orders_file
  local label="$1" ms_file="$2" ids_file="$3"
  : > "$ms_file"; : > "$ids_file"

  echo "-- $label: $N_SEQ checkout tuần tự"
  local fail=0
  for i in $(seq 1 "$N_SEQ"); do
    checkout_once "$BUYER_TOKEN" "$ms_file" "$ids_file" || fail=$((fail + 1))
  done
  [ "$fail" -eq 0 ] && ok "$label · $N_SEQ checkout tuần tự" || bad "$label tuần tự" "fail=$fail"

  echo "-- $label: $N_CONC checkout đồng thời"
  local conc_ms="$OUT/${label}-conc.csv"; : > "$conc_ms"
  local pids=()
  for i in $(seq 0 $((N_CONC - 1))); do
    (
      token="${CONC_TOKENS[$i]}"
      key="${RUN_KEY}-conc-${label}-${i}"
      curl -s -o /dev/null -X DELETE "$GATEWAY/v1/carts" -H "Authorization: Bearer $token"
      curl -s -o /dev/null -X PUT "$GATEWAY/v1/carts/items/$SKU_ID" -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' -d '{"qty":1}'
      sec="$(curl -s -o "$OUT/${label}-conc-${i}.json" -w '%{time_total}' -X POST "$GATEWAY/v1/checkout" \
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
        -H "Idempotency-Key: $key" -d '{}' || echo 0)"
      awk -v s="$sec" 'BEGIN{printf "%.1f\n", s*1000}' >> "$conc_ms"
    ) &
    pids+=($!)
  done
  for p in "${pids[@]}"; do wait "$p"; done
  # Phần đồng thời chỉ ĐO checkout ⇒ còn PENDING_PAYMENT. Sau khi đã ghi số đo, thanh toán + chốt
  # luôn để (a) không để lại đơn treo, (b) có thêm mẫu cho M2 (đo độ trễ event).
  for i in $(seq 0 $((N_CONC - 1))); do
    f="$OUT/${label}-conc-${i}.json"
    [ -f "$f" ] || continue
    read -r c_order c_intent <<< "$(python3 -c "
import json, os
url = json.load(open('$f')).get('paymentUrl', '')
print(json.load(open('$f')).get('orderId', ''), os.path.basename(url) or '-')
")"
    [ -z "$c_order" ] && continue
    http_do POST "$GATEWAY/mock-provider/pay/$c_intent"
    http_do POST "$GATEWAY/v1/checkout/$c_order/confirm" -H "Authorization: Bearer ${CONC_TOKENS[$i]}"
    echo "$c_order" >> "$ids_file"
  done
  ok "$label · $N_CONC checkout đồng thời"
}

echo "== M1 before: dừng notification-service (mô phỏng 'chưa có consumer async')"
docker stop "$NOTIF_CONTAINER" >/dev/null
sleep 2
docker inspect -f '{{.State.Running}}' "$NOTIF_CONTAINER" | grep -q false \
  && ok "notification đã dừng" || bad "dừng notification" "vẫn running"
run_phase "m1-before" "$OUT/m1-before-seq.csv" "$OUT/m1-before-orders.txt"
deadline=$(( $(date +%s) + 15 ))
while [ "$(queue_ready q.notification)" -lt "$N_SEQ" ] && [ "$(date +%s)" -lt "$deadline" ]; do sleep 1; done
BEFORE_QUEUE="$(queue_ready q.notification)"
echo "   q.notification ready=$BEFORE_QUEUE (event tồn lại vì không ai tiêu thụ)"

echo "== M1 after: bật lại notification-service"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d notification-service >/dev/null 2>&1
wait_ready "${NOTIFICATION_SERVICE_PORT:-8089}" 90 && ok "notification /ready 200" \
  || bad "notification ready" "timeout"
wait_queue_drain q.notification 240 && ok "queue q.notification drain xong" \
  || bad "drain queue" "ready=$(queue_ready q.notification)"
run_phase "m1-after" "$OUT/m1-after-seq.csv" "$OUT/m1-after-orders.txt"


# ------------------------------------------------------------------------------
# M2 — độ trễ event (server-side, ghép 2 database bằng python vì khác DB)
# ------------------------------------------------------------------------------
# DB `order`, `payment`, `notification` là 3 database riêng (database per service) ⇒ không JOIN
# xuyên DB được. Xuất 2 cột (order_id, epoch) từ mỗi bên rồi trừ trong python — trung thực và
# không cần FDW.
cat > "$OUT/diff.py" <<'PY'
import sys
left, right, out = sys.argv[1], sys.argv[2], sys.argv[3]
def load(path):
    m = {}
    for line in open(path):
        parts = line.split()
        if len(parts) == 2:
            m[parts[0]] = float(parts[1])
    return m
a, b = load(left), load(right)
deltas = sorted((b[k] - a[k]) * 1000 for k in a if k in b)
with open(out, "w") as f:
    f.write("\n".join(f"{d:.1f}" for d in deltas))
print(f"paired={len(deltas)}")
PY

order_id_list() { # file → 'id','id',...
  python3 -c "
ids=[l.strip() for l in open('$1') if l.strip()]
print(','.join(\"'%s'\" % i for i in ids))"
}

echo "== M2: độ trễ event (n = số đơn của M1-after)"
# Chờ EMAIL xuất hiện, KHÔNG chỉ chờ queue rỗng: `ready=0` cũng đúng khi event còn nằm trong outbox
# (relay 1 s) ⇒ nếu dừng ở đó, mẫu M2 bị thiếu (lần chạy smoke ra n=1/3 vì đúng lý do này).
IDS_M1_PRE="$(order_id_list "$OUT/m1-after-orders.txt")"
deadline=$(( $(date +%s) + 120 ))
while :; do
  have="$(psql_c notification "select count(distinct order_id) from notification_logs where order_id in ($IDS_M1_PRE)")"
  want="$(wc -l < "$OUT/m1-after-orders.txt")"
  [ "${have:-0}" -ge "$want" ] && break
  [ "$(date +%s)" -ge "$deadline" ] && { echo "   (chỉ có $have/$want email sau 120 s)"; break; }
  sleep 1
done
IDS_M1="$IDS_M1_PRE"
if [ -n "$IDS_M1" ]; then
  # (a) confirm (order_status_history CONFIRMED) → email (notification_logs.sent_at)
  psql_raw order "select order_id, extract(epoch from created_at) from order_status_history
                  where to_status='CONFIRMED' and order_id in ($IDS_M1)" > "$OUT/m2a-order.tsv"
  psql_raw notification "select order_id, extract(epoch from sent_at) from notification_logs
                         where sent_at is not null and order_id in ($IDS_M1)" > "$OUT/m2a-notif.tsv"
  echo "   confirm→email: $(python3 "$OUT/diff.py" "$OUT/m2a-order.tsv" "$OUT/m2a-notif.tsv" "$OUT/m2a-ms.csv")"

  # (b) webhook (payment_webhook_events.received_at) → order CONFIRMED
  psql_raw payment "select p.order_id, extract(epoch from w.received_at)
                    from payment_webhook_events w join payments p on p.id = w.payment_id
                    where p.order_id in ($IDS_M1)" > "$OUT/m2b-webhook.tsv"
  echo "   webhook→confirm: $(python3 "$OUT/diff.py" "$OUT/m2b-webhook.tsv" "$OUT/m2a-order.tsv" "$OUT/m2b-ms.csv")"
else
  bad "M2" "không có order id từ M1-after"
fi

# ------------------------------------------------------------------------------
# M3 — throughput relay: bơm N_RELAY event product.updated.v1, đo từ outbox
# ------------------------------------------------------------------------------
echo "== M3: throughput relay ($N_RELAY product.updated.v1)"
M3_START="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
T_PUMP0="$(now_ms)"
for i in $(seq 1 "$N_RELAY"); do
  price="$((100000 + i)).00"
  http_do PUT "$GATEWAY/v1/products/$PRODUCT_ID" -H "Authorization: Bearer $SELLER_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"Bench product $TS\",\"price\":\"$price\"}"
  [ "$STATUS" = "200" ] || { bad "M3 pump" "HTTP $STATUS ở lần $i"; break; }
done
PUMP_MS=$(( $(now_ms) - T_PUMP0 ))
echo "   bơm $N_RELAY update hết ${PUMP_MS} ms (~$(awk -v n="$N_RELAY" -v ms="$PUMP_MS" 'BEGIN{printf "%.1f", n*1000/ms}') request/s)"

# Chờ relay đẩy hết rồi mới tính throughput (nếu tính lúc còn PENDING là đếm event chưa gửi).
deadline=$(( $(date +%s) + 300 ))
while [ "$(psql_c catalog "select count(*) from outbox where aggregate_id='$PRODUCT_ID' and status='PENDING'")" != "0" ]; do
  [ "$(date +%s)" -ge "$deadline" ] && break
  sleep 1
done
M3_ROW="$(psql_raw catalog "select count(*) || ' ' || extract(epoch from min(created_at)) || ' ' ||
       coalesce(extract(epoch from max(sent_at)), 0) || ' ' || extract(epoch from max(created_at))
       from outbox where aggregate_id='$PRODUCT_ID' and status='SENT' and created_at >= '$M3_START'")"
read -r M3_N M3_FIRST_CREATED M3_LAST_SENT M3_LAST_CREATED <<< "$M3_ROW"
M3_SECONDS="$(awk -v a="$M3_FIRST_CREATED" -v b="$M3_LAST_SENT" 'BEGIN{print (b - a)}')"
M3_RATE="$(awk -v n="${M3_N:-0}" -v s="$M3_SECONDS" 'BEGIN{ if (s > 0) printf "%.1f", n/s; else print "n/a" }')"
M3_PENDING_LEFT="$(psql_c catalog "select count(*) from outbox where aggregate_id='$PRODUCT_ID' and status='PENDING'")"
TAIL_SECONDS="$(awk -v b="$M3_LAST_CREATED" -v c="$M3_LAST_SENT" 'BEGIN{print (c - b)}')"
echo "   outbox SENT=$M3_N · cửa sổ ${M3_SECONDS}s ⇒ ${M3_RATE} msg/s · PENDING còn $M3_PENDING_LEFT"
[ "${M3_PENDING_LEFT:-1}" = "0" ] && ok "M3 relay đẩy hết $M3_N event (${M3_RATE} msg/s)" \
  || bad "M3 relay" "còn $M3_PENDING_LEFT PENDING"


# ------------------------------------------------------------------------------
# M4 — DLQ = 0 và không mất/trùng message dưới tải (100 checkout + 100 update, 5 phút)
# ------------------------------------------------------------------------------
echo "== M4: $N_LOAD checkout + $N_LOAD update, quan sát DLQ trong ${WINDOW_S}s"
M4_START="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
: > "$OUT/m4-orders.txt"
M4_FAIL=0
T_M4="$(now_ms)"
for i in $(seq 1 "$N_LOAD"); do
  checkout_once "$BUYER_TOKEN" "$OUT/m4-checkout-ms.csv" "$OUT/m4-orders.txt" || M4_FAIL=$((M4_FAIL + 1))
done
for i in $(seq 1 "$N_LOAD"); do
  http_do PUT "$GATEWAY/v1/products/$PRODUCT_ID" -H "Authorization: Bearer $SELLER_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"Bench product $TS\",\"price\":\"$((200000 + i)).00\"}"
done
M4_LOAD_MS=$(( $(now_ms) - T_M4 ))
[ "$M4_FAIL" -eq 0 ] && ok "M4 chạy $N_LOAD checkout + $N_LOAD update (${M4_LOAD_MS} ms)" \
  || bad "M4 workload" "checkout fail=$M4_FAIL"

# Quan sát DLQ cho tới khi đủ cửa sổ 5 phút (tính từ lúc bắt đầu M4).
DLQ_MAX=0
while [ $(( $(date +%s) - $(date -d "$M4_START" +%s) )) -lt "$WINDOW_S" ]; do
  for q in q.notification.dlq q.order.dlq q.search.dlq; do
    r="$(queue_ready "$q")"
    [ "${r:-0}" -gt "$DLQ_MAX" ] && DLQ_MAX="$r"
  done
  sleep 5
done
echo "   DLQ max trong cửa sổ: $DLQ_MAX (kỳ vọng 0)"
[ "$DLQ_MAX" -eq 0 ] && ok "M4 DLQ = 0 suốt ${WINDOW_S}s" || bad "M4 DLQ" "max=$DLQ_MAX"

# Đối chiếu "phát" vs "xử lý" theo cửa sổ thời gian (không dùng JOIN xuyên DB).
sleep 10   # cho consumer xử lý nốt phần đuôi
M4_OUTBOX_ORDER="$(psql_c order "select count(*) from outbox where status='SENT' and event_type='order.confirmed.v1' and created_at >= '$M4_START'")"
M4_OUTBOX_PAY="$(psql_c payment "select count(*) from outbox where status='SENT' and event_type='payment.succeeded.v1' and created_at >= '$M4_START'")"
M4_OUTBOX_CAT="$(psql_c catalog "select count(*) from outbox where status='SENT' and event_type='product.updated.v1' and created_at >= '$M4_START'")"
M4_PROC_NOTIF="$(psql_c notification "select count(*) from processed_events where consumer='notification-service' and processed_at >= '$M4_START'")"
M4_PROC_ORDER="$(psql_c order "select count(*) from processed_events where consumer='order-service' and processed_at >= '$M4_START'")"
M4_PROC_SEARCH="$(psql_c search "select count(*) from processed_events where consumer='search-service' and processed_at >= '$M4_START'")"
M4_NOTIF_ROWS="$(psql_c notification "select count(*) from notification_logs where created_at >= '$M4_START'")"
M4_NOTIF_DISTINCT="$(psql_c notification "select count(distinct event_id) from notification_logs where created_at >= '$M4_START'")"
echo "   outbox SENT: order.confirmed=$M4_OUTBOX_ORDER payment.succeeded=$M4_OUTBOX_PAY product.updated=$M4_OUTBOX_CAT"
echo "   processed:   notification=$M4_PROC_NOTIF order=$M4_PROC_ORDER search=$M4_PROC_SEARCH"
echo "   notification_logs: rows=$M4_NOTIF_ROWS distinct event_id=$M4_NOTIF_DISTINCT"
[ "$M4_OUTBOX_ORDER" = "$M4_PROC_NOTIF" ] && ok "M4 order.confirmed phát/xử lý khớp ($M4_OUTBOX_ORDER)" \
  || bad "M4 notification phát/xử lý lệch" "$M4_OUTBOX_ORDER vs $M4_PROC_NOTIF"
[ "$M4_OUTBOX_PAY" = "$M4_PROC_ORDER" ] && ok "M4 payment.succeeded phát/xử lý khớp ($M4_OUTBOX_PAY)" \
  || bad "M4 order phát/xử lý lệch" "$M4_OUTBOX_PAY vs $M4_PROC_ORDER"
[ "$M4_OUTBOX_CAT" = "$M4_PROC_SEARCH" ] && ok "M4 product.updated phát/xử lý khớp ($M4_OUTBOX_CAT)" \
  || bad "M4 search phát/xử lý lệch" "$M4_OUTBOX_CAT vs $M4_PROC_SEARCH"
[ "$M4_NOTIF_ROWS" = "$M4_NOTIF_DISTINCT" ] && ok "M4 không có email trùng event_id" \
  || bad "M4 email trùng" "$M4_NOTIF_ROWS rows / $M4_NOTIF_DISTINCT event_id"

# ------------------------------------------------------------------------------
# M5 — degradation: consumer chết ⇒ checkout không đổi, event tồn rồi xử lý khi bật lại
# ------------------------------------------------------------------------------
echo "== M5: degradation (stop notification-service rồi chạy $N_CONC checkout)"
docker stop "$NOTIF_CONTAINER" >/dev/null
sleep 2
: > "$OUT/m5-seq.csv"; : > "$OUT/m5-orders.txt"
for i in $(seq 1 "$N_CONC"); do
  checkout_once "$BUYER_TOKEN" "$OUT/m5-seq.csv" "$OUT/m5-orders.txt" >/dev/null || true
done
deadline=$(( $(date +%s) + 20 ))
while [ "$(queue_ready q.notification)" -lt "$N_CONC" ] && [ "$(date +%s)" -lt "$deadline" ]; do sleep 1; done
M5_DOWN_QUEUE="$(queue_ready q.notification)"
M5_UNACKED="$(curl -sS -u "${RABBITMQ_USER:-shoplab}:${RABBITMQ_PASSWORD:-}" "$MGMT/queues/%2F/q.notification" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin).get("messages_unacknowledged",0))' 2>/dev/null || echo -1)"
echo "   service chết: q.notification ready=$M5_DOWN_QUEUE unacked=$M5_UNACKED"
[ "${M5_DOWN_QUEUE:-0}" -ge "$N_CONC" ] && ok "M5 event tồn trong queue ($M5_DOWN_QUEUE)" \
  || bad "M5 queue" "ready=$M5_DOWN_QUEUE (kỳ vọng ≥ $N_CONC)"

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d notification-service >/dev/null 2>&1
wait_ready "${NOTIFICATION_SERVICE_PORT:-8089}" 90 || bad "M5 ready" "timeout"
T_DRAIN0="$(now_ms)"
wait_queue_drain q.notification 240 && ok "M5 queue drain sau khi bật lại" \
  || bad "M5 drain" "ready=$(queue_ready q.notification)"
M5_DRAIN_MS=$(( $(now_ms) - T_DRAIN0 ))
sleep 3
IDS_M5="$(order_id_list "$OUT/m5-orders.txt")"
M5_LOGGED="$(psql_c notification "select count(distinct order_id) from notification_logs where order_id in ($IDS_M5)")"
M5_ORDERS="$(wc -l < "$OUT/m5-orders.txt")"
echo "   sau khi bật lại: drain ${M5_DRAIN_MS} ms · email cho $M5_LOGGED/$M5_ORDERS đơn"
[ "$M5_LOGGED" = "$M5_ORDERS" ] && ok "M5 event tồn được xử lý đủ sau khi bật lại" \
  || bad "M5 xử lý lại" "$M5_LOGGED/$M5_ORDERS"


# ------------------------------------------------------------------------------
# Tổng kết — in bảng markdown để dán thẳng vào báo cáo (số lấy từ file, không chép tay)
# ------------------------------------------------------------------------------
COMMIT="$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"
DOCKER_V="$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo unknown)"
M1B_SEQ="$(stats "$OUT/m1-before-seq.csv")"
M1B_CONC="$(stats "$OUT/m1-before-conc.csv")"
M1A_SEQ="$(stats "$OUT/m1-after-seq.csv")"
M1A_CONC="$(stats "$OUT/m1-after-conc.csv")"
M2A="$(stats "$OUT/m2a-ms.csv")"
M2B="$(stats "$OUT/m2b-ms.csv")"
M5="$(stats "$OUT/m5-seq.csv")"

SUMMARY="$OUT/summary.md"
{
  echo "### Điều kiện đo"
  echo ""
  echo "| Thông số | Giá trị |"
  echo "|---|---|"
  echo "| commit | \`$COMMIT\` |"
  echo "| thời điểm | $M4_START (UTC) |"
  echo "| Docker Engine | $DOCKER_V |"
  echo "| stack | compose 13 container (1 Postgres 16, Redis 7, RabbitMQ 4 + 10 service Java) |"
  echo "| n (tuần tự / đồng thời / relay / tải) | $N_SEQ / $N_CONC / $N_RELAY / $N_LOAD |"
  echo "| cửa sổ quan sát DLQ | ${WINDOW_S}s |"
  echo ""
  echo "### Kết quả"
  echo ""
  echo "| Chỉ số | Số đo | Cách đo |"
  echo "|---|---|---|"
  echo "| checkout tuần tự — before (không consumer) | $M1B_SEQ ms | \`curl -w '%{time_total}'\` trên \`POST /v1/checkout\` |"
  echo "| checkout tuần tự — after (có consumer) | $M1A_SEQ ms | như trên |"
  echo "| checkout đồng thời — before ($N_CONC song song) | $M1B_CONC ms | $N_CONC buyer riêng, mỗi người 1 giỏ |"
  echo "| checkout đồng thời — after ($N_CONC song song) | $M1A_CONC ms | như trên |"
  echo "| confirm → email | $M2A ms | \`order_status_history.created_at\` → \`notification_logs.sent_at\` |"
  echo "| webhook → order CONFIRMED | $M2B ms | \`payment_webhook_events.received_at\` → \`order_status_history.created_at\` |"
  echo "| throughput relay | $M3_RATE msg/s ($M3_N event) | \`min(outbox.created_at)\` → \`max(outbox.sent_at)\` |"
  echo "| độ trễ đuôi relay | ${TAIL_SECONDS}s (event cuối: tạo → gửi) | \`max(created_at)\` vs \`max(sent_at)\` |"
  echo "| bơm update (producer) | $(awk -v n="$N_RELAY" -v ms="$PUMP_MS" 'BEGIN{printf "%.1f", n*1000/ms}') request/s | $N_RELAY lần \`PUT /v1/products\` |"
  echo "| DLQ trong ${WINDOW_S}s | $DLQ_MAX | poll \`/api/queues\` mỗi 5 s |"
  echo "| event phát / xử lý (order.confirmed) | $M4_OUTBOX_ORDER / $M4_PROC_NOTIF | outbox SENT vs \`processed_events\` |"
  echo "| event phát / xử lý (payment.succeeded) | $M4_OUTBOX_PAY / $M4_PROC_ORDER | như trên |"
  echo "| event phát / xử lý (product.updated) | $M4_OUTBOX_CAT / $M4_PROC_SEARCH | như trên |"
  echo "| email / event_id duy nhất | $M4_NOTIF_ROWS / $M4_NOTIF_DISTINCT | \`notification_logs\` |"
  echo "| checkout khi consumer chết | $M5 ms | như M1, service notification stopped |"
  echo "| event tồn khi consumer chết | ready=$M5_DOWN_QUEUE · unacked=$M5_UNACKED | \`/api/queues\` |"
  echo "| drain sau khi bật lại | ${M5_DRAIN_MS} ms · email $M5_LOGGED/$M5_ORDERS | poll \`ready=0\` + đếm \`notification_logs\` |"
} > "$SUMMARY"

echo ""
echo "===================== BẢNG SỐ LIỆU (dán vào báo cáo) ====================="
cat "$SUMMARY"
echo "========================================================================="
echo "raw: $OUT"
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ] || exit 1

