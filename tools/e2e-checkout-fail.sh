#!/usr/bin/env bash
# ==============================================================================
# e2e-checkout-fail.sh — E2E các NHÁNH FAIL của checkout + compensating (SL-2.9)
#
# Chứng minh saga **bù đúng** khi hỏng, và assert **state thật** (không phải status suông):
#   409/503 trả về client · `stock.reserved` về 0 · `voucher.locked` về 0 · order `CANCELLED`
#
# Kịch bản:
#   F1 hết hàng ở bước reserve        (dữ liệu thật: SKU onHand=0)
#   F2 provider từ chối thanh toán    (mock `?fail=true`) ⇒ confirm 409 ⇒ cancel trả hàng
#   F3 payment lỗi 5xx                (debug hook `failStatus=500`) ⇒ 503 + bù
#   F4 payment **chậm hơn timeout**   (debug hook `sleepMs=2000` > SAGA_READ_TIMEOUT 800 ms)
#   F5 payment **chết hẳn**           (docker stop — connection refused) ⇒ 503 + bù
#   F6 retry cùng Idempotency-Key sau fail ⇒ 201 (khoá đã được trả, không kẹt IN_PROGRESS)
#
# Chạy (stack đã up, `SHOPLAB_DEBUG_ENDPOINTS=true` cho payment — compose local đã set):
#   bash tools/e2e-checkout-fail.sh
#
# Chỉ dùng `curl` + `python3` (+ `docker` cho F5; thiếu docker thì F5 SKIP có đếm).
# ==============================================================================
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
# `/debug/**` CỐ Ý không có route ở gateway: debug là chuyện nội bộ, không expose ra biên.
# Script gọi thẳng port payment (compose publish 8088) — đúng như cách debug thật được dùng.
PAYMENT_DIRECT="${PAYMENT_DIRECT:-http://localhost:8088}"
PAYMENT_CONTAINER="${PAYMENT_CONTAINER:-shoplab-payment-service}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"

# Phải LỚN hơn `SAGA_READ_TIMEOUT` (mặc định 800 ms) để tạo read timeout thật ở F4.
FAULT_SLEEP_MS="${FAULT_SLEEP_MS:-2000}"

TS="$(date +%s)"
EMAIL="fail-${TS}-${RANDOM}@shoplab.local"

PASS=0
FAIL=0
SKIP=0
REQUESTS=0
T0="$(date +%s%N)"

TMP_BODY="$(mktemp)"
PAYMENT_STOPPED="false"

cleanup() {
  # Luôn tắt bơm lỗi + bật lại payment: script chết giữa đường mà để `sleepMs=2000` thì mọi
  # lần checkout sau (kể cả e2e-checkout.sh) sẽ timeout oan.
  curl -s -o /dev/null -X POST "$PAYMENT_DIRECT/debug/faults/reset" || true
  if [ "$PAYMENT_STOPPED" = "true" ]; then
    docker start "$PAYMENT_CONTAINER" >/dev/null 2>&1 || true
  fi
  rm -f "$TMP_BODY"
}
trap cleanup EXIT

http_do() {
  local method="$1" url="$2"
  shift 2
  REQUESTS=$((REQUESTS + 1))
  : > "$TMP_BODY"
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"
  BODY="$(cat "$TMP_BODY")"
}

# Giữ nguyên chuỗi số của API ("220000.00") — xem bài học SL-2.8 (`parse_float=str`).
json_field() {
  printf '%s' "$BODY" | python3 -c \
    "import sys,json;print(json.load(sys.stdin, parse_float=str, parse_int=str).get('$1',''))" \
    2>/dev/null || true
}

json_nested() {
  # json_nested <index> <field>  — phần tử thứ N của mảng `content` (response phân trang)
  printf '%s' "$BODY" | python3 -c \
    "import sys,json;d=json.load(sys.stdin,parse_float=str,parse_int=str);print(d['content'][$1].get('$2',''))" \
    2>/dev/null || true
}

step_ok()   { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }
step_bad()  {
  FAIL=$((FAIL + 1))
  echo "  FAIL [x] $1 — mong đợi: $2 · nhận: $3"
  [ -n "${4:-}" ] && echo "    gợi ý: $4"
  return 0
}
step_skip() { SKIP=$((SKIP + 1)); echo "  SKIP [-] $1 — $2"; }

expect_status() {
  local label="$1" want="$2" got="$3" hint="${4:-}"
  if [ "$got" = "$want" ]; then step_ok "$label"; else step_bad "$label" "$want" "$got" "$hint"; fi
}

expect_value() {
  local label="$1" want="$2" got="$3" hint="${4:-}"
  if [ "$got" = "$want" ]; then step_ok "$label"; else step_bad "$label" "$want" "$got" "$hint"; fi
}

login() {
  http_do POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  json_field accessToken
}

faults() {
  # faults <sleepMs> <failStatus>
  http_do POST "$PAYMENT_DIRECT/debug/faults?sleepMs=$1&failStatus=$2"
}

stock_reserved() {
  http_do GET "$GATEWAY/v1/stock/$1" -H "Authorization: Bearer $ADMIN_TOKEN"
  json_field reserved
}

voucher_locked() {
  http_do GET "$GATEWAY/v1/vouchers/$1" -H "Authorization: Bearer $ADMIN_TOKEN"
  json_field locked
}

latest_order_status() {
  http_do GET "$GATEWAY/v1/orders?size=5" -H "Authorization: Bearer $BUYER_TOKEN"
  json_nested 0 status
}

echo "== E2E checkout nhánh FAIL (Phase 2) — $GATEWAY"

# __FAIL_SETUP__

# --- 0) Danh tính + dữ liệu ---------------------------------------------------
ADMIN_TOKEN="$(login "$ADMIN_EMAIL")"
SELLER_TOKEN="$(login "$SELLER_EMAIL")"
if [ -z "$ADMIN_TOKEN" ] || [ -z "$SELLER_TOKEN" ]; then
  step_bad "0 login admin/seller" "có token" "rỗng" "stack đã up? LocalDevUserSeeder chỉ chạy khi APP_ENV=local"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi
step_ok "0 login admin + seller"

http_do POST "$GATEWAY/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Fail E2E\"}"
expect_status "0b register buyer → 201" 201 "$STATUS"
BUYER_TOKEN="$(login "$EMAIL")"
[ -n "$BUYER_TOKEN" ] && step_ok "0c login buyer" || step_bad "0c login buyer" "token" "rỗng"

http_do POST "$GATEWAY/v1/categories" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Fail E2E $TS\"}"
CATEGORY_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Ao fail E2E\",\"price\":100000.00}"
PRODUCT_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"FAIL-OK-$TS\"}"
SKU_OK="$(json_field id)"

http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"FAIL-OOS-$TS\"}"
SKU_OOS="$(json_field id)"

# SKU có hàng để test các nhánh sau reserve; SKU hết hàng cho F1.
http_do PUT "$GATEWAY/v1/stock/$SKU_OK" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"onHand":10}'
expect_status "0d set tồn kho SKU có hàng = 10 → 200" 200 "$STATUS"
http_do PUT "$GATEWAY/v1/stock/$SKU_OOS" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"onHand":0}'
expect_status "0e set tồn kho SKU hết hàng = 0 → 200" 200 "$STATUS"

NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
LATER="$(date -u -d '+1 day' +%Y-%m-%dT%H:%M:%SZ)"
mkvoucher() {
  http_do POST "$GATEWAY/v1/vouchers" -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"code\":\"$1\",\"type\":\"FIXED\",\"value\":10000.00,\"quota\":50,\"startAt\":\"$NOW\",\"endAt\":\"$LATER\"}"
  expect_status "0f tạo voucher $1 → 201" 201 "$STATUS"
}
V1="FAILOOS$TS"; V2="FAILPAY$TS"; V3="FAILLAT$TS"; V4="FAIL500$TS"; V5="FAILSLOW$TS"
for v in "$V1" "$V2" "$V3" "$V4" "$V5"; do mkvoucher "$v"; done

faults 0 0
expect_status "0g tắt bơm lỗi (trạng thái sạch) → 200" 200 "$STATUS"

if [ -z "$SKU_OK" ] || [ -z "$BUYER_TOKEN" ]; then
  step_bad "tiền điều kiện (SKU + buyer)" "có" "thiếu"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi

cart_add() {
  http_do PUT "$GATEWAY/v1/carts/items/$1" -H "Authorization: Bearer $BUYER_TOKEN" \
    -H 'Content-Type: application/json' -d "{\"qty\":1}"
}
cart_clear() {
  http_do DELETE "$GATEWAY/v1/carts" -H "Authorization: Bearer $BUYER_TOKEN"
}

# --- F1) Hết hàng ở bước reserve → 409 + voucher được trả lại -------------------
echo "-- F1 hết hàng giữa chừng"
cart_clear
cart_add "$SKU_OOS"
http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: f1-$TS" \
  -d "{\"voucherCode\":\"$V1\"}"
expect_status "F1.1 checkout hết hàng → 409" 409 "$STATUS" \
  "201 ⇒ inventory cho phép oversell (kiểm tra conditional UPDATE) · 503 ⇒ service đích chết"
expect_value "F1.2 voucher đã release (locked = 0)" "0" "$(voucher_locked "$V1")"
expect_value "F1.3 không giữ chỗ hàng (reserved = 0)" "0" "$(stock_reserved "$SKU_OOS")"

# --- F2) Provider từ chối thanh toán → confirm 409, cancel trả hàng -------------
echo "-- F2 payment fail (provider từ chối)"
cart_clear
cart_add "$SKU_OK"
http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: f2-$TS" \
  -d "{\"voucherCode\":\"$V2\"}"
expect_status "F2.1 checkout → 201" 201 "$STATUS"
ORDER_F2="$(json_field orderId)"
PAY_F2="$(json_field paymentUrl)"
expect_value "F2.2 hàng đã được giữ (reserved = 1)" "1" "$(stock_reserved "$SKU_OK")"

http_do POST "$GATEWAY/mock-provider/pay/$(basename "$PAY_F2")?fail=true"
expect_status "F2.3 provider báo FAILED → 200" 200 "$STATUS"

http_do POST "$GATEWAY/v1/checkout/$ORDER_F2/confirm" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "F2.4 confirm khi payment FAILED → 409" 409 "$STATUS" \
  "200 ⇒ saga tin client thay vì verify payment (lỗ hổng!)"

http_do POST "$GATEWAY/v1/checkout/$ORDER_F2/cancel" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "F2.5 cancel → 200" 200 "$STATUS"
expect_value "F2.6 saga CANCELLED" "CANCELLED" "$(json_field state)"
expect_value "F2.7 hàng đã trả (reserved = 0)" "0" "$(stock_reserved "$SKU_OK")"
expect_value "F2.8 voucher đã trả lượt (locked = 0)" "0" "$(voucher_locked "$V2")"

# __FAIL_REST__

# --- F3) Payment trả 5xx → 503 + bù (hàng + voucher trả lại, order CANCELLED) ----
echo "-- F3 payment lỗi 5xx (debug hook)"
cart_clear
cart_add "$SKU_OK"
faults 0 500
expect_status "F3.1 bật bơm lỗi failStatus=500 → 200" 200 "$STATUS"
http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: f3-$TS" \
  -d "{\"voucherCode\":\"$V3\"}"
expect_status "F3.2 checkout khi payment lỗi → 503" 503 "$STATUS" \
  "500 ⇒ map sai: 5xx của dependency là 503 (thử lại được) · 409 ⇒ lỗi bị hiểu thành xung đột"
faults 0 0
expect_status "F3.3 tắt bơm lỗi → 200" 200 "$STATUS"
expect_value "F3.4 hàng đã trả (reserved = 0)" "0" "$(stock_reserved "$SKU_OK")"
expect_value "F3.5 voucher đã trả lượt (locked = 0)" "0" "$(voucher_locked "$V3")"
expect_value "F3.6 order mới nhất CANCELLED" "CANCELLED" "$(latest_order_status)" \
  "PENDING_PAYMENT ⇒ compensating không chạy (đơn treo, sẽ bị sweeper dọn)"

# --- F4) Payment CHẬM hơn SAGA_READ_TIMEOUT → read timeout thật → 503 + bù ------
echo "-- F4 payment chậm hơn read-timeout ($FAULT_SLEEP_MS ms)"
cart_clear
cart_add "$SKU_OK"
faults "$FAULT_SLEEP_MS" 0
expect_status "F4.1 bật bơm chậm sleepMs=$FAULT_SLEEP_MS → 200" 200 "$STATUS"
T4_START="$(date +%s%N)"
http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: f4-$TS" \
  -d "{\"voucherCode\":\"$V4\"}"
T4_MS=$(( ($(date +%s%N) - T4_START) / 1000000 ))
# Giữ status/time của chính lần gọi checkout TRƯỚC khi gọi reset (reset cũng dùng `http_do` và sẽ
# ghi đè `$STATUS` — bẫy đã gặp thật: assert đọc status của lệnh reset ⇒ 200).
F4_STATUS="$STATUS"
faults 0 0
expect_status "F4.2 checkout khi payment chậm → 503 (timeout)" 503 "$F4_STATUS" \
  "201 ⇒ SAGA_READ_TIMEOUT không có tác dụng (dependency chậm vẫn kéo được checkout)"
if [ "$T4_MS" -ge 700 ]; then
  step_ok "F4.3 đã chờ tới timeout rồi mới bỏ (${T4_MS} ms ≥ 700 ms)"
else
  step_bad "F4.3 thời gian chờ trước khi bỏ" "≥ 700 ms" "${T4_MS} ms"
fi
expect_value "F4.4 hàng đã trả (reserved = 0)" "0" "$(stock_reserved "$SKU_OK")"
expect_value "F4.5 voucher đã trả lượt (locked = 0)" "0" "$(voucher_locked "$V4")"

# --- F5) Payment chết hẳn (connection refused) → 503 + bù ----------------------
echo "-- F5 payment chết hẳn (docker stop)"
if ! command -v docker >/dev/null 2>&1 || ! docker inspect "$PAYMENT_CONTAINER" >/dev/null 2>&1; then
  step_skip "F5 payment chết hẳn" "không có docker/container '$PAYMENT_CONTAINER' — bỏ qua (đã đếm SKIP)"
else
  cart_clear
  cart_add "$SKU_OK"
  docker stop "$PAYMENT_CONTAINER" >/dev/null
  PAYMENT_STOPPED="true"
  sleep 2
  http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: f5-$TS" \
    -d "{\"voucherCode\":\"$V5\"}"
  expect_status "F5.1 checkout khi payment chết → 503" 503 "$STATUS" \
    "500 ⇒ lỗi hạ tầng bị map thành lỗi của mình · 201 ⇒ không thể (payment đã stop)"
  docker start "$PAYMENT_CONTAINER" >/dev/null
  PAYMENT_STOPPED="false"
  for _ in $(seq 1 45); do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/v1/payment-intents?orderId=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $BUYER_TOKEN" || true)"
    [ "$code" = "200" ] && break
    sleep 2
  done
  step_ok "F5.2 payment-service sống lại (không cần can thiệp tay)"
  expect_value "F5.3 hàng đã trả (reserved = 0)" "0" "$(stock_reserved "$SKU_OK")"
  expect_value "F5.4 voucher đã trả lượt (locked = 0)" "0" "$(voucher_locked "$V5")"
fi

# --- F6) Retry cùng Idempotency-Key sau fail ⇒ 201 (khoá đã được trả) ----------
echo "-- F6 retry cùng key sau fail"
http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: f3-$TS" \
  -d "{\"voucherCode\":\"$V3\"}"
expect_status "F6.1 retry key của F3 (sau khi fail) → 201" 201 "$STATUS" \
  "409 ⇒ khoá bị kẹt IN_PROGRESS (thiếu abort khi fail — xem SL-2.6a)"
expect_value "F6.2 hàng được giữ lại cho đơn mới (reserved = 1)" "1" "$(stock_reserved "$SKU_OK")"

# Dọn: huỷ đơn F6 để không để rác lại cho lần chạy sau trên cùng stack.
ORDER_F6="$(json_field orderId)"
http_do POST "$GATEWAY/v1/checkout/$ORDER_F6/cancel" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "F6.3 huỷ đơn vừa tạo (dọn state) → 200" 200 "$STATUS"
expect_value "F6.4 hàng trả lại sau huỷ (reserved = 0)" "0" "$(stock_reserved "$SKU_OK")"

TOTAL_MS=$(( ($(date +%s%N) - T0) / 1000000 ))
echo ""
echo "== Kết quả: $PASS PASS / $FAIL FAIL / $SKIP SKIP · $REQUESTS request · ${TOTAL_MS} ms"
if [ "$FAIL" -gt 0 ]; then
  echo "E2E checkout fail-branches: FAIL"
  echo "  gợi ý điều tra: docker compose -f infra/docker-compose.yml logs --tail=200 order-service payment-service inventory-service promotion-service"
  exit 1
fi
echo "E2E checkout fail-branches: OK"
