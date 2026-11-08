#!/usr/bin/env bash
# ==============================================================================
# e2e-checkout.sh — E2E happy path của checkout Phase 2 (SL-2.8)
#
# Chứng minh **toàn bộ** luồng tiền chạy thật qua cửa vào duy nhất:
#   client → gateway → cart → order(checkout saga) → catalog · promotion · inventory → payment
#   → mock provider → confirm → order CONFIRMED
#
# Vì sao là script RIÊNG (không nhồi vào `e2e-smoke.sh`):
#   `e2e-smoke.sh` là walking skeleton Phase 1 (17 check, không có cart/checkout) và được dùng
#   như "health check nhanh". Nhồi Phase 2 vào đó làm nó chậm + khi đỏ thì khó biết hỏng tầng nào.
#
# Chạy (cần stack local đã up: `docker compose ... up -d`):
#   bash tools/e2e-checkout.sh
#
# Chỉ dùng `curl` + `python3` (máy KHÔNG có `jq`).
# ==============================================================================
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"

PRODUCT_PRICE="120000.00"
QTY=2
VOUCHER_VALUE="20000.00"
EXPECTED_SUBTOTAL="240000.00"                       # 2 × 120000
EXPECTED_TOTAL="220000.00"                          # subtotal − 20000

TS="$(date +%s)"
EMAIL="checkout-${TS}-${RANDOM}@shoplab.local"
IDEMPOTENCY_KEY="e2e-checkout-${TS}"

PASS=0
FAIL=0
REQUESTS=0
T0="$(date +%s%N)"

TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

http_do() {
  local method="$1" url="$2"
  shift 2
  REQUESTS=$((REQUESTS + 1))
  # Xoá body cũ TRƯỚC khi curl — connection-refused thì curl không mở `-o`
  # ⇒ không xoá trước là ta in body của lần trước (bẫy đã gặp ở SL-1.7).
  : > "$TMP_BODY"
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"
  BODY="$(cat "$TMP_BODY")"
}

json_field() {
  # `parse_float=str` để giữ ĐÚNG chuỗi số của API ("220000.00"), không bị Python đổi thành
  # float ("220000.0") — so sánh tiền mà lệch số 0 cuối là fail giả (bẫy đã gặp ở SL-2.13/ở đây).
  printf '%s' "$BODY" | python3 -c \
    "import sys,json;print(json.load(sys.stdin, parse_float=str, parse_int=str).get('$1',''))" \
    2>/dev/null || true
}

step_ok()  { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }
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

expect_value() {
  local label="$1" want="$2" got="$3" hint="${4:-}"
  if [ "$got" = "$want" ]; then step_ok "$label"; else step_bad "$label" "$want" "$got" "$hint"; fi
}

login() {
  http_do POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  json_field accessToken
}

echo "== E2E checkout happy path (Phase 2) — $GATEWAY"

# --- 1) Danh tính -------------------------------------------------------------
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
  -d "{\"email\":\"$EMAIL\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Checkout E2E\"}"
expect_status "2 register buyer → 201" 201 "$STATUS"
BUYER_TOKEN="$(login "$EMAIL")"
if [ -n "$BUYER_TOKEN" ]; then step_ok "3 login buyer"; else step_bad "3 login buyer" "có token" "rỗng"; fi

# --- 2) Dữ liệu bán -----------------------------------------------------------
http_do POST "$GATEWAY/v1/categories" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Checkout E2E $TS\"}"
expect_status "4 seller tạo category → 201" 201 "$STATUS"
CATEGORY_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Ao checkout E2E\",\"price\":$PRODUCT_PRICE}"
expect_status "5 seller tạo product → 201" 201 "$STATUS"
PRODUCT_ID="$(json_field id)"

http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"code\":\"E2E-$TS\"}"
expect_status "6 seller tạo SKU → 201" 201 "$STATUS"
SKU_ID="$(json_field id)"

http_do PUT "$GATEWAY/v1/stock/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"onHand\":10}"
expect_status "7 admin set tồn kho = 10 → 200" 200 "$STATUS"

NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
LATER="$(date -u -d '+1 day' +%Y-%m-%dT%H:%M:%SZ)"
VOUCHER="E2E$TS"
http_do POST "$GATEWAY/v1/vouchers" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"$VOUCHER\",\"type\":\"FIXED\",\"value\":$VOUCHER_VALUE,\"quota\":50,\"startAt\":\"$NOW\",\"endAt\":\"$LATER\"}"
expect_status "8 admin tạo voucher FIXED $VOUCHER_VALUE → 201" 201 "$STATUS"

if [ -z "$SKU_ID" ] || [ -z "$BUYER_TOKEN" ]; then
  step_bad "tiền điều kiện (SKU + buyer token)" "có" "thiếu"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi

# __PART2__

# --- 3) Giỏ → checkout -------------------------------------------------------
http_do PUT "$GATEWAY/v1/carts/items/$SKU_ID" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"qty\":$QTY}"
expect_status "9 buyer thêm $QTY món vào giỏ → 200" 200 "$STATUS" \
  "401 ⇒ cart không nhận token · 404 ⇒ route /v1/carts chưa có"

http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"voucherCode\":\"$VOUCHER\"}"
expect_status "10 checkout → 201" 201 "$STATUS" \
  "409 ⇒ hết hàng/SKU chết · 422 ⇒ giỏ rỗng · 503 ⇒ service đích chết (xem docker compose logs order-service)"
ORDER_ID="$(json_field orderId)"
PAYMENT_URL="$(json_field paymentUrl)"
expect_value "10b checkout trả total = subtotal − voucher" "$EXPECTED_TOTAL" "$(json_field total)"
expect_value "10c checkout trả subtotal đúng" "$EXPECTED_SUBTOTAL" "$(json_field subtotal)"
if [ -n "$PAYMENT_URL" ]; then step_ok "10d có paymentUrl để redirect"; else step_bad "10d có paymentUrl" "không rỗng" "rỗng"; fi

if [ -z "$ORDER_ID" ]; then
  step_bad "tiền điều kiện (orderId)" "có" "thiếu"
  echo ""
  echo "== Kết quả: $PASS PASS / $FAIL FAIL · dừng sớm"
  exit 1
fi

# --- 4) Saga + idempotency ---------------------------------------------------
http_do GET "$GATEWAY/v1/checkout/$ORDER_ID" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "11 GET trạng thái saga → 200" 200 "$STATUS"
expect_value "11b saga đã xong bước start" "COMPLETED_START" "$(json_field state)"

http_do POST "$GATEWAY/v1/checkout" -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"voucherCode\":\"$VOUCHER\"}"
expect_status "12 retry cùng Idempotency-Key → 200 (replay)" 200 "$STATUS"
expect_value "12b replay trả cùng orderId" "$ORDER_ID" "$(json_field orderId)"
if printf '%s' "$BODY" | grep -q '"orderId"'; then
  step_ok "12c replay có body checkout hợp lệ"
else
  step_bad "12c replay có body checkout" "JSON có orderId" "$(printf '%s' "$BODY" | head -c 120)"
fi

# --- 5) Thanh toán + confirm -------------------------------------------------
MOCK_INTENT="$(basename "$PAYMENT_URL")"
http_do POST "$GATEWAY/mock-provider/pay/$MOCK_INTENT"
expect_status "13 mock provider báo đã thanh toán → 200" 200 "$STATUS" \
  "404 ⇒ PAYMENT_MOCK_ENABLED=false hoặc paymentUrl lạ"

http_do POST "$GATEWAY/v1/checkout/$ORDER_ID/confirm" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "14 confirm → 200" 200 "$STATUS" \
  "409 ⇒ payment chưa SUCCEEDED (kiểm tra bước 13) · 503 ⇒ payment-service chết"
expect_value "14b saga CONFIRMED" "CONFIRMED" "$(json_field state)"

# --- 6) Trạng thái thật của hệ ----------------------------------------------
http_do GET "$GATEWAY/v1/orders/$ORDER_ID" -H "Authorization: Bearer $BUYER_TOKEN"
expect_status "15 GET order (góc nhìn client) → 200" 200 "$STATUS"
expect_value "15b order CONFIRMED" "CONFIRMED" "$(json_field status)"
expect_value "15c order giữ total đã chốt" "$EXPECTED_TOTAL" "$(json_field total)"

http_do GET "$GATEWAY/v1/stock/$SKU_ID" -H "Authorization: Bearer $ADMIN_TOKEN"
expect_status "16 GET tồn kho → 200" 200 "$STATUS"
expect_value "16b onHand giảm đúng qty (hàng đã chốt)" "8" "$(json_field onHand)"
expect_value "16c reserved về 0 (không còn giữ chỗ)" "0" "$(json_field reserved)"

TOTAL_MS=$(( ($(date +%s%N) - T0) / 1000000 ))
echo ""
echo "== Kết quả: $PASS PASS / $FAIL FAIL · $REQUESTS request · ${TOTAL_MS} ms"
if [ "$FAIL" -gt 0 ]; then
  echo "E2E checkout: FAIL"
  echo "  gợi ý điều tra: docker compose -f infra/docker-compose.yml logs --tail=100 order-service payment-service"
  exit 1
fi
echo "E2E checkout: OK"
