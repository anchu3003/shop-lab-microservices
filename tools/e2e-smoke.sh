#!/usr/bin/env bash
# ==============================================================================
# e2e-smoke.sh — E2E smoke cho walking skeleton (SL-1.3)
#
# Chứng minh đường đi end-to-end: client → api-gateway (8080) → identity-service (8081) → Postgres.
# Chỉ dùng `curl` + `python3` (máy KHÔNG có `jq`).
#
# Chạy (cần stack local đã `docker compose ... up -d`, gateway + identity healthy):
#   bash tools/e2e-smoke.sh
#
# Vì sao không chỉ check "khác 200": bước trái phép phải ASSERT đúng 401 — nếu check `!= 200` thì
# 500 cũng "pass" (bẫy "test âm" đã học ở SL-0.5).
# ==============================================================================
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
IDENTITY="${IDENTITY:-http://localhost:8081}"

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
  STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$url" "$@" || true)"  # `|| true`: connection-refused không làm set -e kill script → in status 000 rõ ràng
  BODY="$(cat "$TMP_BODY")"
}

step_ok()  { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }
step_bad() { FAIL=$((FAIL + 1)); echo "  FAIL $1 — mong đợi $2 nhưng nhận status=$3"; }

# expect_status <mô tả> <mong đợi> <thực tế>
expect_status() {
  if [ "$3" = "$2" ]; then step_ok "$1"; else step_bad "$1" "$2" "$3"; echo "    body: $(printf '%s' "$BODY" | head -c 300)"; fi
}

echo "== E2E smoke — client → gateway → identity → DB"
echo "   email=$EMAIL  correlation=$CORRELATION"

# 1) health gateway + identity
http_do GET "$GATEWAY/health"
expect_status "1a health gateway" 200 "$STATUS"
http_do GET "$IDENTITY/health"
expect_status "1b health identity" 200 "$STATUS"

# 2) register (qua gateway) → 201
http_do POST "$GATEWAY/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"$DISPLAY_NAME\"}"
expect_status "2 register" 201 "$STATUS"

# 3) login (qua gateway) → 200 + lấy accessToken
http_do POST "$GATEWAY/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
expect_status "3 login" 200 "$STATUS"
if [ "$STATUS" = "200" ]; then
  TOKEN="$(printf '%s' "$BODY" | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')"
else
  TOKEN=""
fi

# 4) protected qua gateway (có token + correlation id) → 200
if [ -n "$TOKEN" ]; then
  http_do GET "$GATEWAY/v1/users/me" \
    -H "Authorization: Bearer $TOKEN" \
    -H "x-correlation-id: $CORRELATION"
  expect_status "4 protected /v1/users/me (có token)" 200 "$STATUS"
else
  FAIL=$((FAIL + 1)); echo "  FAIL 4 protected /v1/users/me — bỏ qua (chưa có token, bước 3 không thành công)"
fi

# 5) cùng route, KHÔNG token → phải 401 (đúng, không phải "khác 200")
if [ -n "$TOKEN" ]; then
  http_do GET "$GATEWAY/v1/users/me"
  expect_status "5 /v1/users/me KHÔNG token → 401" 401 "$STATUS"
else
  FAIL=$((FAIL + 1)); echo "  FAIL 5 /v1/users/me KHÔNG token → 401 — bỏ qua (chưa có token)"
fi

# 6) traceId xuyên service: identity phải log request của bước 4 với traceId = CORRELATION
if [ -n "$TOKEN" ]; then
  if docker logs shoplab-identity-service 2>/dev/null | grep -qF "\"traceId\":\"$CORRELATION\""; then
    step_ok "6 traceId $CORRELATION có trong log identity (gateway forward header)"
  else
    step_bad "6 traceId $CORRELATION trong log identity" "có" "không"
  fi
else
  FAIL=$((FAIL + 1)); echo "  FAIL 6 traceId — bỏ qua (chưa có token)"
fi

TOTAL_MS=$(( ($(date +%s%N) - T0) / 1000000 ))
echo ""
echo "== Kết quả: $PASS PASS / $FAIL FAIL · $REQUESTS request · ${TOTAL_MS} ms"
if [ "$FAIL" -gt 0 ]; then
  echo "E2E smoke: FAIL"
  exit 1
fi
echo "E2E smoke: OK"
