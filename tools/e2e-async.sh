#!/usr/bin/env bash
# ==============================================================================
# e2e-async.sh — E2E cho 4 luồng async của Phase 3 (SL-3.7)
#
# Vì sao cần script RIÊNG (không nhồi vào 3 script cũ):
#   Async **không** kiểm được bằng một `curl`: API trả `200` TRƯỚC khi việc thật xảy ra
#   (email gửi, index cập nhật, order tự chốt). Test kiểu "gọi xong rồi assert ngay" sẽ xanh
#   vì... chưa kịp chạy. Ở đây mọi assert đều **poll có timeout** tới khi state cuối đúng.
#
# 4 luồng được chứng minh:
#   1. order.confirmed.v1      ⇒ email (row trong `notification_logs`)
#   2. payment.succeeded.v1    ⇒ order tự CONFIRMED (KHÔNG gọi /confirm)
#   3. product.updated/archived ⇒ index search cập nhật / ẩn
#   4. message lỗi             ⇒ DLQ ⇒ `tools/dlq.sh move` ⇒ xử lý lại được
#
# Chạy (cần stack local đã up: `docker compose ... up -d --wait`):
#   bash tools/e2e-async.sh
#
# Script tự dọn state (fail-mode off + DLQ rỗng) kể cả khi chết giữa đường — xem `trap`.
# Chạy 2 lần liên tiếp phải xanh cả 2 (mọi dữ liệu sinh ra đều có timestamp + RANDOM).
#
# Chỉ dùng `curl` + `python3` (máy dev không có `jq`) + `docker exec` để đọc DB.
# ==============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/infra/docker-compose.yml}"

if [ -f "$ENV_FILE" ]; then
  set -a; . "$ENV_FILE"; set +a
fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"

PG_CONTAINER="${PG_CONTAINER:-shoplab-postgres}"
PG_USER="${POSTGRES_USER:-shoplab}"
DB_NOTIFICATION="${POSTGRES_DB_NOTIFICATION:-notification}"
DB_CATALOG="${POSTGRES_DB_CATALOG:-catalog}"
DB_SEARCH="${POSTGRES_DB_SEARCH:-search}"
NOTIF_PORT="${NOTIFICATION_SERVICE_PORT:-8089}"
MGMT="http://localhost:${RABBITMQ_MANAGEMENT_PORT:-15672}/api"

# Timeout poll: luồng event thường xong trong ~1-2 s (relay 1 s + consumer) ⇒ 5 s là rộng rãi.
# Luồng DLQ thì KHÔNG thể ≤ 5 s: consumer retry 200 ms + 1 s + 3 s ≈ 4,2 s rồi mới nack sang DLQ.
POLL_FAST="${POLL_FAST:-5}"
POLL_DLQ="${POLL_DLQ:-15}"

TS="$(date +%s)"
RND="$RANDOM"
EMAIL="async-${TS}-${RND}@shoplab.local"
# Khoá idempotency = prefix theo lần chạy + số thứ tự tăng dần. KHÔNG dùng $RANDOM: bash có thể lặp
# lại giá trị ⇒ hai checkout khác body mà cùng key ⇒ 409 "request hash mismatch".
CHECKOUT_KEY_PREFIX="e2e-async-${TS}-${RND}"
CHECKOUT_SEQ=1
PRODUCT_NAME="Async $TS-$RND"
PRODUCT_NAME_2="Async $TS-$RND updated"

PASS=0
FAIL=0
REQUESTS=0
T0="$(date +%s%N)"
OBSERVED=""

TMP_BODY="$(mktemp)"
trap 'cleanup; rm -f "$TMP_BODY"' EXIT

# ------------------------------------------------------------------------------
# Cleanup: fail-mode PHẢI về false, DLQ PHẢI rỗng — kể cả khi script chết giữa đường.
# Thiếu trap thì lần chạy sau thấy DLQ đầy message cũ + fail-mode bật ⇒ FAIL giả (bài học SL-2.9).
# ------------------------------------------------------------------------------
set_fail_mode() { # true|false
  SHOPLAB_NOTIFICATION_FAIL_MODE="$1" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" \
    up -d notification-service >/dev/null 2>&1 || true
}

cleanup() {
  set_fail_mode false
  bash "$REPO_ROOT/tools/dlq.sh" purge q.notification >/dev/null 2>&1 || true
}

http_do() {
  local method="$1" url="$2"
  shift 2
  REQUESTS=$((REQUESTS + 1))
  # Xoá body cũ TRƯỚC khi curl (bẫy SL-1.7): connection-refused thì curl không mở `-o`.
  : > "$TMP_BODY"
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"
  BODY="$(cat "$TMP_BODY")"
}

json_field() {
  printf '%s' "$BODY" | python3 -c \
    "import sys,json;print(json.load(sys.stdin, parse_float=str, parse_int=str).get('$1',''))" \
    2>/dev/null || true
}

step_ok() { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }

step_bad() {
  FAIL=$((FAIL + 1))
  echo "  FAIL [x] $1 — mong đợi: $2 · nhận: $3"
  [ -n "${4:-}" ] && echo "    gợi ý: $4"
  return 0
}

expect_status() {
  local label="$1" want="$2" got="$3" hint="${4:-}"
  if [ "$got" = "$want" ]; then step_ok "$label"; else step_bad "$label" "$want" "$got" "$hint"; fi
}

# ------------------------------------------------------------------------------
# Poll có điều kiện — KHÔNG `sleep` mù. Predicate trả 0 = đạt, và ghi giá trị quan sát được vào
# $OBSERVED để khi fail in được "nhận: gì" thay vì "timeout".
# ------------------------------------------------------------------------------
poll_until() { # timeout_seconds predicate [args...]
  local timeout="$1"
  shift
  local deadline=$(( $(date +%s) + timeout ))
  while :; do
    if "$@"; then return 0; fi
    [ "$(date +%s)" -ge "$deadline" ] && return 1
    sleep 0.2
  done
}

poll_expect() { # label timeout predicate [args...]
  local label="$1" timeout="$2"
  shift 2
  if poll_until "$timeout" "$@"; then
    step_ok "$label"
  else
    step_bad "$label" "đạt sau ${timeout}s" "${OBSERVED:-không đạt}" \
      "bash tools/dlq.sh list · docker compose -f infra/docker-compose.yml logs --tail=50"
  fi
}

# --- Đọc state THẬT từ DB / broker / API (không tin lời khai của service) ---
psql_c() { # db sql
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$1" -tAc "$2" 2>/dev/null | tr -d '[:space:]'
}

queue_ready() { # queue
  curl -sS -u "${RABBITMQ_USER:-shoplab}:${RABBITMQ_PASSWORD:-}" "$MGMT/queues/%2F/$1" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin).get("messages_ready",0))' 2>/dev/null \
    || echo "-1"
}

notification_rows() { # orderId — luồng 1: event ⇒ email thật trong DB
  OBSERVED="$(psql_c "$DB_NOTIFICATION" "select count(*) from notification_logs where order_id='$1'")"
  [ "${OBSERVED:-0}" -ge 1 ]
}

index_status() { # productId status — luồng 3: row index (ARCHIVED, không xoá cứng)
  OBSERVED="$(psql_c "$DB_SEARCH" "select status from search_product where product_id='$1'")"
  [ "$OBSERVED" = "$2" ]
}

order_status() { # orderId status — luồng 2: order tự chốt
  http_do GET "$GATEWAY/v1/orders/$1" -H "Authorization: Bearer $BUYER_TOKEN"
  OBSERVED="$(json_field status)"
  [ "$OBSERVED" = "$2" ]
}

search_has() { # q productId
  http_do GET "$GATEWAY/v1/search?q=$1&size=50"
  if printf '%s' "$BODY" | grep -q "$2"; then OBSERVED="thấy"; return 0; fi
  OBSERVED="chưa thấy"
  return 1
}

search_gone() { # q productId
  http_do GET "$GATEWAY/v1/search?q=$1&size=50"
  if printf '%s' "$BODY" | grep -q "$2"; then OBSERVED="vẫn thấy"; return 1; fi
  OBSERVED="đã mất"
  return 0
}

search_price_is() { # q productId price
  http_do GET "$GATEWAY/v1/search?q=$1&size=50"
  # `parse_float=str` để giữ nguyên scale ("777000.00" ≠ float 777000.0) — so tiền mà lệch số 0
  # cuối là fail giả (bẫy đã gặp ở SL-2.13).
  OBSERVED="$(printf '%s' "$BODY" | python3 -c "
import sys, json
d = json.load(sys.stdin, parse_float=str, parse_int=str)
pid = '$2'
print(next((i.get('minPrice') for i in d.get('content', []) if i.get('productId') == pid),
           'không có'))
" 2>/dev/null || echo '?')"
  [ "$OBSERVED" = "$3" ]
}

dlq_at_least() { # queue n
  OBSERVED="$(queue_ready "$1")"
  [ "${OBSERVED:-0}" -ge "$2" ]
}

queue_empty() { # queue
  OBSERVED="$(queue_ready "$1")"
  [ "$OBSERVED" = "0" ]
}

ready_ok() { # port
  OBSERVED="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$1/ready" || true)"
  [ "$OBSERVED" = "200" ]
}

login() {
  http_do POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  json_field accessToken
}

# Checkout một đơn 1 món cho SKU đã có tồn kho; in orderId + payment intent ra biến global.
checkout_one() { # label [voucherCode]
  local label="$1"
  local voucher="${2:-}"
  local body="{}"
  [ -n "$voucher" ] && body="{\"voucherCode\":\"$voucher\"}"
  # DỌN GIỎ TRƯỚC: `PUT /carts/items/{sku}` là **thêm**, không phải thay giỏ. Không dọn thì giỏ còn
  # món của lần trước — và nếu SKU cũ vừa bị ARCHIVED (luồng 3 xoá product) thì checkout chết ở bước
  # `PRICE` với 409 "SKU không mua được", một lỗi rất khó lần vì nó nằm ở service khác.
  # Đây là bẫy thật gặp ở lần chạy đầu của chính script này.
  http_do DELETE "$GATEWAY/v1/carts" -H "Authorization: Bearer $BUYER_TOKEN"
  if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
    step_ok "$label.0 dọn giỏ (DELETE /v1/carts → $STATUS)"
  else
    step_bad "$label.0 dọn giỏ" "200/204" "$STATUS" "401 ⇒ cart không nhận token"
  fi
  http_do PUT "$GATEWAY/v1/carts/items/$SKU_ID" -H "Authorization: Bearer $BUYER_TOKEN" \
    -H 'Content-Type: application/json' -d '{"qty":1}'
  expect_status "$label.a thêm 1 món vào giỏ → 200" 200 "$STATUS" \
    "409 ⇒ SKU đã ARCHIVED (product bị xoá) hoặc hết hàng · 404 ⇒ SKU không tồn tại"
  http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: $CHECKOUT_KEY_PREFIX-$CHECKOUT_SEQ" \
    -d "$body"
  CHECKOUT_SEQ=$((CHECKOUT_SEQ + 1))
  expect_status "$label.b checkout → 201" 201 "$STATUS" \
    "409 ⇒ hết hàng/SKU chết · 422 ⇒ giỏ rỗng · 503 ⇒ service đích chết"
  ORDER_ID="$(json_field orderId)"
  PAYMENT_INTENT="$(basename "$(json_field paymentUrl)")"
}

# ==============================================================================
# Bắt đầu
# ==============================================================================
echo "== E2E async (Phase 3) — $GATEWAY"

echo "-- 0) Dọn state trước khi chạy (fail-mode off, DLQ rỗng)"
set_fail_mode false
bash "$REPO_ROOT/tools/dlq.sh" purge q.notification >/dev/null 2>&1 || true
poll_expect "0 notification-service sẵn sàng, fail-mode OFF" 60 ready_ok "$NOTIF_PORT"

ADMIN_TOKEN="$(login "$ADMIN_EMAIL")"
SELLER_TOKEN="$(login "$SELLER_EMAIL")"
if [ -z "$ADMIN_TOKEN" ] || [ -z "$SELLER_TOKEN" ]; then
  step_bad "1 login admin/seller" "có token" "rỗng" "stack đã up? LocalDevUserSeeder chỉ chạy khi APP_ENV=local"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi
step_ok "1 login admin + seller"

http_do POST "$GATEWAY/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Async E2E\"}"
expect_status "2 register buyer → 201" 201 "$STATUS" "409 ⇒ email đã tồn tại (chạy lại script có TS mới)"
BUYER_TOKEN="$(login "$EMAIL")"
if [ -n "$BUYER_TOKEN" ]; then step_ok "3 login buyer"; else step_bad "3 login buyer" "có token" "rỗng"; fi

echo "-- Chuẩn bị dữ liệu bán"
http_do POST "$GATEWAY/v1/categories" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Async E2E $TS-$RND\"}"
expect_status "4 seller tạo category → 201" 201 "$STATUS"
CATEGORY_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"$PRODUCT_NAME\",\"price\":\"500000.00\"}"
expect_status "5 seller tạo product → 201" 201 "$STATUS"
PRODUCT_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"ASYNC-$TS-$RND\"}"
expect_status "6 seller tạo SKU → 201" 201 "$STATUS"
SKU_ID="$(json_field id)"

http_do PUT "$GATEWAY/v1/stock/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"onHand":10}'
expect_status "7 admin set tồn kho = 10 → 200" 200 "$STATUS"

if [ -z "$PRODUCT_ID" ] || [ -z "$SKU_ID" ] || [ -z "$BUYER_TOKEN" ]; then
  step_bad "tiền điều kiện (product/SKU/buyer)" "có" "thiếu"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi

# ==============================================================================
# LUỒNG 1: order.confirmed.v1 ⇒ email
# ==============================================================================
echo "-- Luồng 1: confirm ⇒ event ⇒ email (notification_logs)"
checkout_one "8"
if [ -z "$ORDER_ID" ]; then
  step_bad "8b có orderId" "không rỗng" "rỗng"
  echo ""; echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"; exit 1
fi

http_do POST "$GATEWAY/mock-provider/pay/$PAYMENT_INTENT"
expect_status "9 mock provider báo đã thanh toán → 200" 200 "$STATUS"
http_do POST "$GATEWAY/v1/checkout/$ORDER_ID/confirm" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "10 confirm → 200 (đường sync)" 200 "$STATUS" "409 ⇒ payment chưa SUCCEEDED"
expect_value_state="$(json_field state)"
if [ "$expect_value_state" = "CONFIRMED" ]; then step_ok "10b saga CONFIRMED"; else
  step_bad "10b saga CONFIRMED" "CONFIRMED" "$expect_value_state"; fi
# Trước khi chờ email: assert DB chưa có row là vô nghĩa (race) ⇒ chỉ poll TỚI KHI có.
poll_expect "11 email cho order 1 xuất hiện ($POLL_FAST s)" "$POLL_FAST" notification_rows "$ORDER_ID"

# ==============================================================================
# LUỒNG 2: payment.succeeded.v1 ⇒ order tự chốt (KHÔNG gọi /confirm)
# ==============================================================================
echo "-- Luồng 2: mock-pay xong, KHÔNG gọi /confirm ⇒ order tự CONFIRMED"
checkout_one "12 (đơn 2)"
ORDER2_ID="$ORDER_ID"

http_do POST "$GATEWAY/mock-provider/pay/$PAYMENT_INTENT"
expect_status "13 mock provider báo đã thanh toán → 200" 200 "$STATUS"

# Điểm mấu chốt: KHÔNG gọi /confirm. Order phải tự chuyển trạng thái nhờ event
# `payment.succeeded.v1` (SL-3.5) — nếu chỉ poll `saga` thì chưa đủ, phải kiểm chính `order.status`.
poll_expect "14 order 2 tự CONFIRMED KHÔNG cần /confirm ($POLL_FAST s)" "$POLL_FAST" \
  order_status "$ORDER2_ID" "CONFIRMED"
poll_expect "15 email cho order 2 (chuỗi 2 event: payment→order→notification) ($POLL_FAST s)" \
  "$POLL_FAST" notification_rows "$ORDER2_ID"

# ==============================================================================
# LUỒNG 3: product.updated.v1 / product.archived.v1 ⇒ index search
# ==============================================================================
echo "-- Luồng 3: sửa product ⇒ search cập nhật; xoá ⇒ search ẩn"
http_do PUT "$GATEWAY/v1/products/$PRODUCT_ID" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"$PRODUCT_NAME_2\",\"price\":\"777000.00\"}"
expect_status "16 seller sửa product (tên + giá) → 200" 200 "$STATUS"

poll_expect "17 search thấy product sau khi sửa ($POLL_FAST s)" "$POLL_FAST" \
  search_has "$TS-$RND" "$PRODUCT_ID"
poll_expect "18 search trả GIÁ MỚI 777000.00 ($POLL_FAST s)" "$POLL_FAST" \
  search_price_is "$TS-$RND" "$PRODUCT_ID" "777000.00"

http_do DELETE "$GATEWAY/v1/products/$PRODUCT_ID" -H "Authorization: Bearer $SELLER_TOKEN"
expect_status "19 seller xoá mềm product → 204" 204 "$STATUS"
poll_expect "20 search KHÔNG còn product đã xoá ($POLL_FAST s)" "$POLL_FAST" \
  search_gone "$TS-$RND" "$PRODUCT_ID"
poll_expect "21 row index chuyển ARCHIVED (không xoá cứng) ($POLL_FAST s)" "$POLL_FAST" \
  index_status "$PRODUCT_ID" "ARCHIVED"

# ==============================================================================
# Chuẩn bị dữ liệu cho luồng 4 — SKU mới
#
# Vì sao phải tạo SKU mới: luồng 3 vừa **xoá mềm** product ⇒ SKU của nó bị ARCHIVED theo
# (`SL-2.11`) ⇒ checkout với SKU đó sẽ 409. Lần chạy đầu của script này đã mắc đúng bẫy đó —
# và nếu chỉ assert "checkout 201" thì lỗi hiện ra ở bước DLQ (rất khó lần).
# ==============================================================================
echo "-- Chuẩn bị SKU mới cho luồng 4 (SKU cũ đã ARCHIVED theo product ở luồng 3)"
http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"DLQ $TS-$RND\",\"price\":\"400000.00\"}"
expect_status "22a seller tạo product cho luồng DLQ → 201" 201 "$STATUS"
DLQ_PRODUCT_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products/$DLQ_PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"ASYNC-DLQ-$TS-$RND\"}"
expect_status "22b seller tạo SKU cho luồng DLQ → 201" 201 "$STATUS"
SKU_ID="$(json_field id)"

http_do PUT "$GATEWAY/v1/stock/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"onHand":5}'
expect_status "22c admin set tồn kho SKU mới = 5 → 200" 200 "$STATUS"

# ==============================================================================
# LUỒNG 4: message lỗi ⇒ DLQ ⇒ replay (dlq.sh move)
# ==============================================================================
echo "-- Luồng 4: bơm lỗi ⇒ retry cạn ⇒ DLQ ⇒ tắt lỗi ⇒ replay ⇒ xử lý lại"
set_fail_mode true
# Recreate container ⇒ /ready tạm 503; poll tới khi consumer sống lại rồi mới bắn event,
# nếu không message sẽ nằm chờ trong queue và ta "chờ DLQ" một cách vô nghĩa.
poll_expect "22 notification-service sống lại với fail-mode ON" 90 ready_ok "$NOTIF_PORT"

checkout_one "23 (đơn 3)"
ORDER3_ID="$ORDER_ID"
http_do POST "$GATEWAY/mock-provider/pay/$PAYMENT_INTENT"
expect_status "24 mock provider báo đã thanh toán → 200" 200 "$STATUS"
http_do POST "$GATEWAY/v1/checkout/$ORDER3_ID/confirm" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "25 confirm → 200 (order vẫn chốt được: async KHÔNG chặn đường chính)" 200 "$STATUS"

# Retry budget = 200 ms + 1 s + 3 s ≈ 4,2 s rồi mới nack ⇒ poll 15 s (AC ghi rõ ngoại lệ này).
poll_expect "26 message lỗi vào q.notification.dlq (retry cạn, $POLL_DLQ s)" "$POLL_DLQ" \
  dlq_at_least "q.notification.dlq" 1
poll_expect "27 q.notification rỗng lại (message đã sang DLQ, không kẹt unacked)" "$POLL_DLQ" \
  queue_empty "q.notification"

# "Sửa nguyên nhân rồi mới replay": fail-mode tắt ⇒ message không còn độc.
set_fail_mode false
poll_expect "28 notification-service sống lại với fail-mode OFF" 90 ready_ok "$NOTIF_PORT"

if bash "$REPO_ROOT/tools/dlq.sh" move q.notification >/dev/null 2>&1; then
  step_ok "29 dlq.sh move q.notification (replay 1 message)"
else
  step_bad "29 dlq.sh move q.notification" "exit 0" "exit != 0" "bash tools/dlq.sh peek q.notification"
fi
# Replay xong phải THẤY được kết quả trong DB — nếu chỉ kiểm DLQ rỗng thì "message mất" cũng pass.
poll_expect "30 replay xử lý lại được ⇒ có email cho order 3 ($POLL_FAST s)" "$POLL_FAST" \
  notification_rows "$ORDER3_ID"
# Timeout DLQ dùng POLL_DLQ (15 s) chứ không phải 5 s: `/api/queues` của RabbitMQ là **số liệu thống
# kê**, broker làm mới mỗi ~5 s ⇒ `messages_ready` có thể còn 1 sau khi message đã bị `get` khỏi DLQ.
# Đây là độ trễ của *cách đo*, không phải message còn sót — poll dài hơn là cách đúng, không phải
# thêm `sleep`.
poll_expect "31 DLQ rỗng sau replay ($POLL_DLQ s, số liệu broker trễ ~5 s)" "$POLL_DLQ" \
  queue_empty "q.notification.dlq"

# ==============================================================================
# Kết thúc: mọi queue chính phải sạch (không message kẹt, không DLQ mới)
# ==============================================================================
echo "-- Trạng thái cuối"
for q in q.notification q.order q.search q.notification.dlq q.order.dlq q.search.dlq; do
  poll_expect "32.$q ready = 0" "$POLL_DLQ" queue_empty "$q"
done

TOTAL_MS=$(( ($(date +%s%N) - T0) / 1000000 ))
echo ""
echo "== Kết quả: $PASS PASS / $FAIL FAIL · $REQUESTS request · ${TOTAL_MS} ms"
if [ "$FAIL" -gt 0 ]; then
  echo "E2E async: FAIL"
  echo "  gợi ý điều tra:"
  echo "    bash tools/dlq.sh list"
  echo "    docker compose -f infra/docker-compose.yml logs --tail=100 notification-service order-service search-service payment-service"
  exit 1
fi
echo "E2E async: OK"
