#!/usr/bin/env bash
# ==============================================================================
# e2e-smoke.sh — E2E smoke cho walking skeleton (SL-1.3 → SL-1.7 mở rộng)
#
# Chứng minh đường đi end-to-end của CẢ Phase 1:
#   client → api-gateway (8080) → identity (8081) / catalog (8082) → Postgres
#
# Luồng kiểm: register → login → /v1/users/me (401/200) → traceId xuyên service →
#   seller tạo category + product → **guest** list được product vừa tạo (public) →
#   **buyer** tạo product → 403.
#
# Chỉ dùng `curl` + `python3` (máy KHÔNG có `jq`).
#
# Chạy (cần stack local đã `docker compose ... up -d`, cả 3 service healthy):
#   bash tools/e2e-smoke.sh
#
# Khi fail: mỗi bước in **gợi ý lệnh kiểm tra** tương ứng — chi tiết ở
# `docs/runbook-phase-1.md` (triệu chứng → lệnh → nguyên nhân → cách xử lý).
#
# Vì sao không chỉ check "khác 200": bước trái phép phải ASSERT đúng 401/403 — nếu check `!= 200`
# thì 500 cũng "pass" (bẫy "test âm" đã học ở SL-0.5). Bước 403 là **case mong đợi lỗi**, không
# phải fail.
# ==============================================================================
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
IDENTITY="${IDENTITY:-http://localhost:8081}"
CATALOG="${CATALOG:-http://localhost:8082}"

# Tài khoản seller có sẵn ở local (LocalDevUserSeeder, chỉ APP_ENV=local — xem SL-1.4).
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
SELLER_PASSWORD="${SELLER_PASSWORD:-matkhau123}"

TS="$(date +%s)"
EMAIL="smoke-${TS}-${RANDOM}@shoplab.local"
PASSWORD="smoke-pass-123"   # 8–72 ký tự (RegisterRequest @Size)
DISPLAY_NAME="Smoke"
CORRELATION="smoke-${TS}"   # gửi qua header x-correlation-id để đối chiếu traceId trong log identity

PASS=0
FAIL=0
REQUESTS=0
T0="$(date +%s%N)"   # nanosec; millis = /1000000 (không dùng %3N — trên GNU date hệ này ra 9 chữ số)

TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

# http_do <method> <url> [curl args...]  → cập nhật STATUS (mã HTTP) + BODY (nội dung).
http_do() {
  local method="$1" url="$2"
  shift 2
  REQUESTS=$((REQUESTS + 1))
  # Xoá body cũ TRƯỚC khi curl: nếu connection-refused, curl không mở `-o` ⇒ file giữ nội dung lần
  # trước và ta sẽ in **body cũ** (đã gặp thật ở SL-1.7: catalog chết nhưng body vẫn `{"status":"UP"}`).
  : > "$TMP_BODY"
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"  # `|| true`: connection-refused không làm set -e kill script → in status 000 rõ ràng
  BODY="$(cat "$TMP_BODY")"
}

step_ok()  { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }
step_bad() { FAIL=$((FAIL + 1)); echo "  FAIL [x] $1 — mong đợi $2 nhưng nhận status=$3"; }

# expect_status <mô tả> <mong đợi> <thực tế> [gợi ý lệnh kiểm tra khi fail]
expect_status() {
  if [ "$3" = "$2" ]; then
    step_ok "$1"
  else
    step_bad "$1" "$2" "$3"
    echo "    body: $(printf '%s' "$BODY" | head -c 300)"
    if [ -n "${4:-}" ]; then echo "    gợi ý: $4"; fi
    echo "    → docs/runbook-phase-1.md"
  fi
}

# json_field <tên field>  — đọc field từ $BODY (python3, không cần jq).
json_field() {
  printf '%s' "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('$1',''))" 2>/dev/null || true
}

echo "== E2E smoke — client → gateway → identity/catalog → DB"
echo "   email=$EMAIL  correlation=$CORRELATION"

# 1) health 3 service (gateway + identity + catalog)
http_do GET "$GATEWAY/health"
expect_status "1a health gateway" 200 "$STATUS" \
  "docker ps | grep gateway · docker logs shoplab-api-gateway --tail 50"
http_do GET "$IDENTITY/health"
expect_status "1b health identity" 200 "$STATUS" \
  "docker ps | grep identity · docker logs shoplab-identity-service --tail 50"
http_do GET "$CATALOG/health"
expect_status "1c health catalog" 200 "$STATUS" \
  "docker ps | grep catalog · docker logs shoplab-catalog-service --tail 50"

# 2) register (qua gateway) → 201
http_do POST "$GATEWAY/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"$DISPLAY_NAME\"}"
expect_status "2 register" 201 "$STATUS" \
  "xem body: VALIDATION_ERROR (thiếu field) hay CONFLICT (email trùng)"

# 3) login (qua gateway) → 200 + lấy accessToken
http_do POST "$GATEWAY/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
expect_status "3 login" 200 "$STATUS" \
  "docker exec shoplab-postgres psql -U shoplab -d identity -c 'select email, role from users'"
TOKEN="$(json_field accessToken)"

# 4) protected qua gateway (có token + correlation id) → 200
if [ -n "$TOKEN" ]; then
  http_do GET "$GATEWAY/v1/users/me" \
    -H "Authorization: Bearer $TOKEN" \
    -H "x-correlation-id: $CORRELATION"
  expect_status "4 protected /v1/users/me (có token)" 200 "$STATUS" \
    "docker logs shoplab-api-gateway --tail 50 | grep -i jwks (sai issuer/JWKS ⇒ mọi token 401)"
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 4 /v1/users/me — bỏ qua (chưa có token, bước 3 fail)"
fi

# 5) cùng route, KHÔNG token → phải 401 (đúng, không phải "khác 200")
if [ -n "$TOKEN" ]; then
  http_do GET "$GATEWAY/v1/users/me"
  expect_status "5 /v1/users/me KHÔNG token → 401" 401 "$STATUS" \
    "401 ở đây là ĐÚNG; nếu 200 thì gateway đang bỏ qua xác thực (JwtAuthFilter + public-paths)"
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 5 /v1/users/me KHÔNG token → 401 — bỏ qua"
fi

# 6) traceId xuyên service: identity phải log request của bước 4 với traceId = CORRELATION
if [ -n "$TOKEN" ]; then
  if docker logs shoplab-identity-service 2>/dev/null | grep -qF "\"traceId\":\"$CORRELATION\""; then
    step_ok "6 traceId $CORRELATION có trong log identity (gateway forward header)"
  else
    step_bad "6 traceId $CORRELATION trong log identity" "có" "không"
    echo "    gợi ý: docker logs shoplab-identity-service --tail 100 | grep $CORRELATION"
  fi
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 6 traceId — bỏ qua (chưa có token)"
fi

# ---------------------------------------------------------------------------
# SL-1.7: luồng catalog đầy đủ (ghi cần quyền · đọc public · buyer bị chặn)
# ---------------------------------------------------------------------------

# 7) login bằng tài khoản SELLER có sẵn ở local (seed của SL-1.4)
http_do POST "$GATEWAY/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$SELLER_EMAIL\",\"password\":\"$SELLER_PASSWORD\"}"
expect_status "7 login seller ($SELLER_EMAIL)" 200 "$STATUS" \
  "seed chỉ chạy khi APP_ENV=local: psql -U shoplab -d identity -c 'select email,role from users'"
SELLER_TOKEN="$(json_field accessToken)"

# 8) seller tạo category → 201
CATEGORY_ID=""
if [ -n "$SELLER_TOKEN" ]; then
  http_do POST "$GATEWAY/v1/categories" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"Smoke $TS $RANDOM\"}"
  expect_status "8 seller tạo category → 201" 201 "$STATUS" \
    "403 ⇒ token không phải SELLER · 404 ⇒ gateway thiếu route /v1/categories · 503 ⇒ catalog chết"
  CATEGORY_ID="$(json_field id)"
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 8 seller tạo category — bỏ qua (không có token seller)"
fi

# 9) seller tạo product → 201 (in id thật để thấy dữ liệu đã ghi)
PRODUCT_ID=""
if [ -n "$CATEGORY_ID" ]; then
  http_do POST "$GATEWAY/v1/products" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Smoke product $TS\",\"price\":123456.78}"
  expect_status "9 seller tạo product → 201" 201 "$STATUS" \
    "404 ⇒ categoryId không tồn tại · 400 ⇒ body sai (xem details) · 503 ⇒ catalog chết"
  PRODUCT_ID="$(json_field id)"
  if [ -n "$PRODUCT_ID" ]; then echo "    product id = $PRODUCT_ID"; fi
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 9 seller tạo product — bỏ qua (chưa có category)"
fi
# 10) GUEST (không token) list được product vừa tạo — đọc public qua gateway
if [ -n "$PRODUCT_ID" ]; then
  http_do GET "$GATEWAY/v1/products?page=0&size=100"
  expect_status "10 guest list product (KHÔNG token) → 200" 200 "$STATUS" \
    "401 ⇒ /v1/products chưa nằm trong public-paths của gateway · 503 ⇒ catalog chết"
  if printf '%s' "$BODY" | grep -qF "$PRODUCT_ID"; then
    step_ok "10b product $PRODUCT_ID xuất hiện trong list của guest"
  else
    step_bad "10b product trong list của guest" "có" "không"
    echo "    gợi ý: curl -s '$GATEWAY/v1/products?size=100' | head -c 300"
    echo "           psql -U shoplab -d catalog -c 'select id,name from product order by created_at desc limit 3'"
  fi
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 10 guest list product — bỏ qua (chưa tạo được product)"
fi

# 11) GUEST xem chi tiết product → 200 + có field skus
if [ -n "$PRODUCT_ID" ]; then
  http_do GET "$GATEWAY/v1/products/$PRODUCT_ID"
  expect_status "11 guest xem chi tiết product → 200" 200 "$STATUS" \
    "404 ⇒ product id sai/đã xoá · 401 ⇒ route chưa public"
  if printf '%s' "$BODY" | grep -q '"skus"'; then
    step_ok "11b chi tiết product có field skus"
  else
    step_bad "11b chi tiết product có skus" "có" "không"
    echo "    gợi ý: curl -s '$GATEWAY/v1/products/$PRODUCT_ID' | python3 -m json.tool"
  fi
fi

# 12) BUYER (tài khoản vừa register) tạo product → 403 (case MONG ĐỢI lỗi, không phải fail)
if [ -n "$TOKEN" ] && [ -n "$CATEGORY_ID" ]; then
  http_do POST "$GATEWAY/v1/products" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Buyer khong duoc tao\",\"price\":1.00}"
  expect_status "12 buyer tạo product → 403 (đúng như mong đợi)" 403 "$STATUS" \
    "401 ⇒ token buyer hỏng · 201 ⇒ PHÂN QUYỀN HỎNG (kiểm tra @PreAuthorize + claim role)"
  if printf '%s' "$BODY" | grep -q 'FORBIDDEN'; then
    step_ok "12b body 403 là ApiError FORBIDDEN (không phải HTML)"
  else
    step_bad "12b body 403 = ApiError" "FORBIDDEN" "$(printf '%s' "$BODY" | head -c 120)"
  fi
else
  FAIL=$((FAIL + 1)); echo "  FAIL [x] 12 buyer tạo product → 403 — bỏ qua (thiếu token/category)"
fi

TOTAL_MS=$(( ($(date +%s%N) - T0) / 1000000 ))
echo ""
echo "== Kết quả: $PASS PASS / $FAIL FAIL · $REQUESTS request · ${TOTAL_MS} ms"
if [ "$FAIL" -gt 0 ]; then
  echo "E2E smoke: FAIL"
  exit 1
fi
echo "E2E smoke: OK"
