#!/usr/bin/env bash
# ==============================================================================
# rate-limit-drill.sh — chứng minh rate limit ở gateway (SL-4.3) bằng lệnh thật
#
# Vì sao có script này thay vì "curl vài lần rồi nói ổn": rate limit có 4 tính chất mà một lần curl
# KHÔNG chứng minh được, và cả 4 đều là chỗ hay hỏng:
#
#   D1. Chặn tải thật ở biên       — 500 VU không token ⇒ ≥ 90% là 429
#   D2. Chặn ở BIÊN, không ở DB    — `xact_commit` không tăng theo số request bị chặn
#   D3. 429 đúng chuẩn             — Retry-After + X-RateLimit-* + body ApiError (code + traceId)
#   D4. Không chặn oan             — user A cạn hạn mức KHÔNG làm user B bị 429
#   D5. Fail-open khi Redis chết   — request vẫn 200 + log WARN, và tự phục hồi khi Redis trở lại
#   D6. Chi phí của tầng chặn      — p99 tăng bao nhiêu vì thêm 1 round-trip Redis (--with-overhead)
#
# Chạy:
#   bash tools/rate-limit-drill.sh                     # D1–D5
#   bash tools/rate-limit-drill.sh --with-overhead     # thêm D6 (restart gateway 2 lần, ~4 phút)
#   bash tools/rate-limit-drill.sh --skip-flood        # bỏ D1/D2 (nhanh, khi chỉ cần xem header)
#
# Yêu cầu: stack đang chạy (docker compose up -d), limiter BẬT (RATE_LIMIT_ENABLED=true),
#          seed đã chạy (bash tools/load/seed.sh) cho D1, DEBUG endpoint bật cho preflight.
# Raw: /tmp/rate-limit-drill-<ts>/
# ==============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
GATEWAY_INTERNAL="${GATEWAY_INTERNAL:-http://api-gateway:8080}"
NETWORK="${NETWORK:-shoplab-net}"
K6_IMAGE="${K6_IMAGE:-grafana/k6:latest}"
SEED_DIR="${SEED_DIR:-/tmp/load-seed}"
REDIS_CONTAINER="${REDIS_CONTAINER:-shoplab-redis}"
PG_CONTAINER="${PG_CONTAINER:-shoplab-postgres}"
PG_USER="${POSTGRES_USER:-shoplab}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
FLOOD_VUS="${FLOOD_VUS:-500}"
FLOOD_SECONDS="${FLOOD_SECONDS:-30}"

DO_FLOOD=1
DO_OVERHEAD=0
while [ $# -gt 0 ]; do
  case "$1" in
    --skip-flood)     DO_FLOOD=0; shift ;;
    --with-overhead)  DO_OVERHEAD=1; shift ;;
    -h|--help)        sed -n '2,30p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "tham số lạ: $1 (xem --help)"; exit 2 ;;
  esac
done

TS="$(date +%s)"
OUT="${OUT:-/tmp/rate-limit-drill-$TS}"
mkdir -p "$OUT"
PASS=0; FAIL=0

ok()   { PASS=$((PASS+1)); echo "  PASS [$1] $2"; }
bad()  { FAIL=$((FAIL+1)); echo "  FAIL [$1] $2"; }
step() { echo; echo "== $* "; }

status_of() { curl -s -o /dev/null -w '%{http_code}' "$@" 2>/dev/null || echo 000; }

# Giá trị 1 field trong JSON bằng python3 (repo không phụ thuộc jq).
json_get() { python3 -c 'import json,sys;d=json.load(sys.stdin);
p=sys.argv[1].split(".");
for k in p: d=d[k]
print(d)' "$1" 2>/dev/null; }

login() {
  curl -s -X POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}" | json_get accessToken
}

# --- D0: preflight -----------------------------------------------------------
step "D0 preflight — stack sống và limiter đang BẬT (không thì mọi kết luận sau là rác)"
HEALTH="$(status_of "$GATEWAY/health")"
if [ "$HEALTH" = "200" ]; then ok D0 "/health = 200"; else bad D0 "/health = $HEALTH (stack chưa up?)"; fi

RL_STATE="$(curl -s "$GATEWAY/debug/rate-limit" 2>/dev/null || true)"
# /debug/** cần token (chỉ /health là public) ⇒ lấy token admin rồi hỏi lại.
if [ -z "$RL_STATE" ] || ! printf '%s' "$RL_STATE" | grep -q '"enabled"'; then
  ADMIN_TOKEN_PRE="$(login "$ADMIN_EMAIL")"
  RL_STATE="$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN_PRE}" "$GATEWAY/debug/rate-limit" 2>/dev/null || true)"
fi
printf '%s' "$RL_STATE" > "$OUT/rate-limit-state.json"
RL_ENABLED="$(printf '%s' "$RL_STATE" | json_get enabled 2>/dev/null || echo '')"
IP_BURST="$(printf '%s' "$RL_STATE" | json_get defaultPerIp.burst 2>/dev/null || echo '')"
USER_BURST="$(printf '%s' "$RL_STATE" | json_get defaultPerUser.burst 2>/dev/null || echo '')"
echo "   enabled=$RL_ENABLED · default per-ip burst=$IP_BURST · per-user burst=$USER_BURST"
if [ "$RL_ENABLED" = "True" ] || [ "$RL_ENABLED" = "true" ]; then
  ok D0 "limiter đang bật"
else
  bad D0 "limiter TẮT (RATE_LIMIT_ENABLED=false) ⇒ drill này vô nghĩa. Bật lại rồi chạy: docker compose up -d api-gateway"
  echo; echo "== Kết quả: $PASS PASS / $FAIL FAIL · raw=$OUT"; exit 1
fi

# --- helper: đốt cạn bucket ---------------------------------------------------
# Vì sao không "gửi N request rồi kiểm": bucket nạp lại 200 token/giây, nên 400 request song song
# chỉ vừa đúng burst — request kiểm tra ngay sau đó vẫn có token. Phải gửi THỪA XA burst, và kiểm
# bằng cách **giữ lại chính response 429** chứ không gửi thêm request mới (request mới có thể lọt).
burn_bucket() { # path [token] [số request]  — chạy ở BACKGROUND (gọi bằng `&`)
  local path="$1" token="${2:-}" n="${3:-3000}"
  local -a auth=()
  [ -n "$token" ] && auth=(-H "Authorization: Bearer $token")
  local -a urls=()
  local _i
  for _i in $(seq 1 "$n"); do urls+=("$GATEWAY$path"); done
  # `--parallel`: 1 tiến trình curl, nhiều kết nối ⇒ không tốn 3000 fork.
  # Redirect TOÀN BỘ stdout: `-o /dev/null` chỉ áp cho URL đầu, các URL sau sẽ đổ body ra stdout
  # (đã gặp thật — log drill ngập JSON product).
  curl --parallel --parallel-max 100 -s "${auth[@]}" "${urls[@]}" >/dev/null 2>&1
}

capture_429() { # path [token] → echo status; header/body ghi vào $OUT/rl-429-*
  local path="$1" token="${2:-}"
  local -a auth=()
  [ -n "$token" ] && auth=(-H "Authorization: Bearer $token")
  local st="" i
  for i in $(seq 1 5); do
    st="$(curl -s -D "$OUT/rl-429-headers.txt" -o "$OUT/rl-429-body.json" -w '%{http_code}' \
      "${auth[@]}" "$GATEWAY$path")"
    [ "$st" = "429" ] && { echo 429; return 0; }
    sleep 0.05
  done
  echo "$st"
}

# Dội LIÊN TỤC cho tới khi bị kill — để lấy mẫu 429 ở giữa cơn dội thay vì "đốt xong rồi mới kiểm".
# Vì sao cần: một lô 4.000 request song song xong trong ~0,2 s, trong khi xô nạp 200 token/giây ⇒ chỉ
# cần trễ 0,25 s là lại có ~50 token và ta nhận 200 (đã gặp thật: `remaining=50`). Vòng lặp giữ xô
# cạn suốt cửa sổ lấy mẫu nên kết quả không phụ thuộc timing.
burn_forever() { # path [token]
  local path="$1" token="${2:-}"
  while :; do
    burn_bucket "$path" "$token" 500
  done
}

# --- D3: hình dạng 429 (làm TRƯỚC D1 để bucket còn sạch) ---------------------
step "D3 hình dạng 429 — Retry-After + X-RateLimit-* + body ApiError"
# Phải bắt 429 TRONG LÚC đang dội: bucket nạp 200 token/giây, nên "đốt xong rồi mới kiểm" chỉ cần
# cách nhau ~5 ms là đã có token trở lại ⇒ nhận 200 và kết luận sai. Giữ flood chạy nền, lấy mẫu 429
# ở giữa — đúng cả tình huống thật (client đang bị chặn).
burn_forever "/v1/products?page=0&size=1" &
BURN_PID=$!
sleep 0.3
D3_STATUS="$(capture_429 "/v1/products?page=0&size=1")"
kill "$BURN_PID" 2>/dev/null; wait "$BURN_PID" 2>/dev/null || true
D3_BODY="$(cat "$OUT/rl-429-body.json" 2>/dev/null || true)"
D3_CODE="$(printf '%s' "$D3_BODY" | json_get code 2>/dev/null || echo '')"
D3_TRACE="$(printf '%s' "$D3_BODY" | json_get traceId 2>/dev/null || echo '')"
D3_RETRY="$(awk 'BEGIN{IGNORECASE=1} /^retry-after:/{print $2}' "$OUT/rl-429-headers.txt" | tr -d '\r' | head -1)"
D3_LIMIT="$(awk 'BEGIN{IGNORECASE=1} /^x-ratelimit-limit:/{print $2}' "$OUT/rl-429-headers.txt" | tr -d '\r' | head -1)"
D3_REMAIN="$(awk 'BEGIN{IGNORECASE=1} /^x-ratelimit-remaining:/{print $2}' "$OUT/rl-429-headers.txt" | tr -d '\r' | head -1)"
D3_RESET="$(awk 'BEGIN{IGNORECASE=1} /^x-ratelimit-reset:/{print $2}' "$OUT/rl-429-headers.txt" | tr -d '\r' | head -1)"
echo "   status=$D3_STATUS code=$D3_CODE retry-after=$D3_RETRY limit=$D3_LIMIT remaining=$D3_REMAIN reset=$D3_RESET"
printf '   body=%s\n' "$(printf '%s' "$D3_BODY" | head -c 300)"

if [ "$D3_STATUS" = "429" ] && [ -n "$D3_RETRY" ] && [ -n "$D3_LIMIT" ] && [ -n "$D3_REMAIN" ] && [ -n "$D3_RESET" ]; then
  ok D3 "429 đủ 5 thông tin (status + Retry-After + 3 header X-RateLimit-*)"
else
  bad D3 "thiếu header chuẩn (status=$D3_STATUS retry=$D3_RETRY limit=$D3_LIMIT remaining=$D3_REMAIN reset=$D3_RESET)"
fi
if [ "$D3_CODE" = "TOO_MANY_REQUESTS" ] && [ -n "$D3_TRACE" ]; then
  ok D3 "body là ApiError với code=TOO_MANY_REQUESTS + traceId"
else
  bad D3 "body không đúng ApiError (code=$D3_CODE traceId=$D3_TRACE)"
fi

# --- D1/D2: 500 VU không token ------------------------------------------------
flood() {
  local dir="$1" vus="$2" secs="$3"
  rm -rf "$dir"; mkdir -p "$dir"; chmod 777 "$dir"
  docker rm -f shoplab-k6-rl >/dev/null 2>&1
  docker run -d --name shoplab-k6-rl --network "$NETWORK" \
    -v "$REPO_ROOT/tools/load:/scripts:ro" \
    -v "$SEED_DIR:/seed:ro" \
    -v "$dir:/out" \
    -e BASE_URL="$GATEWAY_INTERNAL" -e VUS="$vus" -e DURATION="${secs}s" \
    "$K6_IMAGE" run /scripts/rate-limit.js >/dev/null 2>&1
  docker wait shoplab-k6-rl >/dev/null 2>&1
  docker logs shoplab-k6-rl > "$dir/k6.log" 2>&1
  docker rm -f shoplab-k6-rl >/dev/null 2>&1
}

pg_commits() {
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -tA -c \
    "select coalesce(sum(xact_commit),0) from pg_stat_database" 2>/dev/null || echo 0
}

read_flood_stats() { # summary.json → "total blocked not_throttled p99"
  python3 - "$1" <<'PY'
import json, sys
metrics = json.load(open(sys.argv[1]))["metrics"]
def vals(key):
    m = metrics.get(key, {})
    return m.get("values", m) if isinstance(m, dict) else {}
total = vals("http_reqs").get("count", 0) or 0
blocked = vals("rl_429").get("count", 0) or 0
not_throttled = vals("rl_not_throttled").get("count", 0) or 0
p99 = vals("http_req_duration").get("p(99)", 0.0)
print(f"{total}\t{blocked}\t{not_throttled}\t{p99:.1f}")
PY
}

if [ "$DO_FLOOD" = 1 ]; then
  if [ ! -f "$SEED_DIR/seed.json" ]; then
    echo "-- ⚠ thiếu $SEED_DIR/seed.json ⇒ chạy 'bash tools/load/seed.sh' trước; bỏ qua D1/D2"
  else
    step "D1 $FLOOD_VUS VU KHÔNG token dội /v1/products trong ${FLOOD_SECONDS}s (AC: ≥ 90% là 429)"
    COMMIT_BEFORE="$(pg_commits)"
    flood "$OUT/flood" "$FLOOD_VUS" "$FLOOD_SECONDS"
    COMMIT_AFTER="$(pg_commits)"
    if [ ! -f "$OUT/flood/summary.json" ]; then
      bad D1 "k6 không ghi summary.json — xem $OUT/flood/k6.log"
    else
      read -r TOTAL BLOCKED NOT_THROTTLED P99 <<< "$(read_flood_stats "$OUT/flood/summary.json")"
      RATIO="$(awk -v b="$BLOCKED" -v t="$TOTAL" 'BEGIN{printf "%.3f", (t>0)?b/t:0}')"
      RATIO_PCT="$(awk -v r="$RATIO" 'BEGIN{printf "%.1f", r*100}')"
      echo "   tổng request $TOTAL · bị chặn $BLOCKED · lọt $NOT_THROTTLED · tỉ lệ 429 ${RATIO_PCT}% · p99 ${P99} ms"
      if awk -v r="$RATIO" 'BEGIN{exit !(r>=0.90)}'; then
        ok D1 "tỉ lệ 429 = ${RATIO_PCT}% (≥ 90%)"
      else
        bad D1 "tỉ lệ 429 = ${RATIO_PCT}% (< 90%) — kiểm: limiter bật? ngưỡng per-ip quá cao?"
      fi

      step "D2 chặn ở BIÊN, không ở DB — xact_commit không tăng theo số request bị chặn"
      DELTA="$(awk -v a="$COMMIT_BEFORE" -v b="$COMMIT_AFTER" 'BEGIN{d=b-a; print (d<0)?0:d}')"
      echo "   xact_commit: $COMMIT_BEFORE → $COMMIT_AFTER (Δ $DELTA) · request LỌT limiter: $NOT_THROTTLED"
      # Mỗi request lọt qua nhiều nhất ~1 transaction ⇒ Δ phải ≤ số request lọt (thực tế còn thấp hơn
      # nhờ cache catalog của SL-4.2). Δ ≈ tổng request nghĩa là chặn đã xảy ra SAU DB — sai thiết kế.
      if awk -v d="$DELTA" -v n="$NOT_THROTTLED" 'BEGIN{exit !(d<=n+5)}'; then
        ok D2 "Δ xact_commit $DELTA ≤ request lọt $NOT_THROTTLED (+5 dung sai)"
      else
        bad D2 "Δ xact_commit $DELTA > request lọt $NOT_THROTTLED ⇒ tải vẫn chạm DB"
      fi
    fi
  fi
fi

# --- D4: hai user không dùng chung bucket ------------------------------------
step "D4 hai user KHÁC nhau không dùng chung bucket (không chặn oan)"
TOKEN_A="$(login "$ADMIN_EMAIL")"
TOKEN_B="$(login "$SELLER_EMAIL")"
if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
  bad D4 "không login được admin/seller (LocalDevUserSeeder chỉ chạy khi APP_ENV=local)"
else
  # Đốt hạn mức của A trên /v1/users/me (route mặc định: user 100/200). Song song để bù refill
  # 100 token/s — đốt tuần tự sẽ không bao giờ cạn.
  seq 1 600 | xargs -P 40 -I{} curl -s -o /dev/null -w '%{http_code}\n' \
    -H "Authorization: Bearer $TOKEN_A" "$GATEWAY/v1/users/me" > "$OUT/d4-a-status.txt" 2>/dev/null
  A_429="$(grep -c '^429$' "$OUT/d4-a-status.txt" 2>/dev/null || echo 0)"
  B_STATUS="$(status_of -H "Authorization: Bearer $TOKEN_B" "$GATEWAY/v1/users/me")"
  echo "   A: $(grep -c '^200$' "$OUT/d4-a-status.txt" 2>/dev/null || echo 0) × 200 · ${A_429} × 429 | B: $B_STATUS"
  if [ "${A_429:-0}" -gt 0 ] && [ "$B_STATUS" = "200" ]; then
    ok D4 "A bị chặn ${A_429} lần, B vẫn 200 (bucket tách theo user)"
  else
    bad D4 "A_429=$A_429 (cần > 0) · B=$B_STATUS (cần 200) — bucket có thể đang gộp chung theo IP"
  fi
fi

# --- D5: fail-open khi Redis chết --------------------------------------------
step "D5 fail-open: Redis chết ⇒ vẫn phục vụ (200) + WARN; Redis trở lại ⇒ chặn lại"
docker stop "$REDIS_CONTAINER" >/dev/null 2>&1
sleep 1
D5_STATUS="$(status_of "$GATEWAY/v1/products?page=0&size=1")"
D5_WARN="$(docker logs shoplab-api-gateway --since 90s 2>&1 | grep -c 'fail-open')"
echo "   Redis dừng · GET /v1/products = $D5_STATUS · số dòng WARN 'fail-open' = $D5_WARN"
if [ "$D5_STATUS" = "200" ] && [ "${D5_WARN:-0}" -ge 1 ]; then
  ok D5 "fail-open đúng: 200 + WARN"
else
  bad D5 "status=$D5_STATUS warn=$D5_WARN — Redis chết mà request không qua được (hoặc không log WARN)"
fi

docker start "$REDIS_CONTAINER" >/dev/null 2>&1
for _ in $(seq 1 30); do
  docker exec "$REDIS_CONTAINER" redis-cli -a "${REDIS_PASSWORD:-}" --no-auth-warning ping 2>/dev/null \
    | grep -q PONG && break
  sleep 1
done
# Đốt lại bucket: sau khi Redis hồi, limiter phải hoạt động lại (không kẹt ở fail-open).
# Lấy mẫu 429 ở giữa lúc dội — cùng lý do như D3.
burn_forever "/v1/products?page=0&size=1" &
BURN_PID=$!
sleep 0.3
D5_AFTER="$(capture_429 "/v1/products?page=0&size=1")"
kill "$BURN_PID" 2>/dev/null; wait "$BURN_PID" 2>/dev/null || true
echo "   Redis trở lại · đốt bucket · GET tiếp theo = $D5_AFTER"
if [ "$D5_AFTER" = "429" ]; then
  ok D5 "tự phục hồi: limiter chặn lại sau khi Redis trở lại"
else
  bad D5 "sau khi Redis trở lại vẫn $D5_AFTER (kỳ vọng 429) — limiter không hoạt động lại"
fi

# --- D6: chi phí của tầng chặn (tùy chọn) ------------------------------------
if [ "$DO_OVERHEAD" = 1 ]; then
  step "D6 chi phí: p99 read-catalog 100 VU khi limiter TẮT vs BẬT (ngưỡng cao, không chặn)"
  COMPOSE="docker compose -f $REPO_ROOT/infra/docker-compose.yml"

  wait_gateway() {
    for _ in $(seq 1 90); do
      [ "$(status_of "$GATEWAY/health")" = "200" ] && return 0
      sleep 2
    done
    return 1
  }

  run_catalog() { # dir vus secs
    local dir="$1"
    rm -rf "$dir"; mkdir -p "$dir"; chmod 777 "$dir"
    docker rm -f shoplab-k6-ovh >/dev/null 2>&1
    docker run -d --name shoplab-k6-ovh --network "$NETWORK" \
      -v "$REPO_ROOT/tools/load:/scripts:ro" -v "$SEED_DIR:/seed:ro" -v "$dir:/out" \
      -e BASE_URL="$GATEWAY_INTERNAL" -e VUS="$2" -e DURATION="${3}s" \
      "$K6_IMAGE" run /scripts/read-catalog.js >/dev/null 2>&1
    docker wait shoplab-k6-ovh >/dev/null 2>&1
    docker logs shoplab-k6-ovh > "$dir/k6.log" 2>&1
    docker rm -f shoplab-k6-ovh >/dev/null 2>&1
    python3 - "$dir/summary.json" <<'PY'
import json, sys
m = json.load(open(sys.argv[1]))["metrics"]
def vals(k):
    x = m.get(k, {})
    return x.get("values", x) if isinstance(x, dict) else {}
count = vals("http_reqs").get("count", 0) or 0
p99 = vals("http_req_duration").get("p(99)", 0.0)
print(f"{count/35:.0f}\t{p99:.1f}")
PY
  }

  RATE_LIMIT_ENABLED=false $COMPOSE up -d --no-deps api-gateway >/dev/null 2>&1
  wait_gateway || echo "   ⚠ gateway không lên sau khi tắt limiter"
  read -r OFF_RPS OFF_P99 <<< "$(run_catalog "$OUT/ovh-off" 100 30)"

  RATE_LIMIT_ENABLED=true RATE_LIMIT_DEFAULT_USER_RPS=200000 RATE_LIMIT_DEFAULT_USER_BURST=200000 \
    RATE_LIMIT_DEFAULT_IP_RPS=200000 RATE_LIMIT_DEFAULT_IP_BURST=200000 \
    $COMPOSE up -d --no-deps api-gateway >/dev/null 2>&1
  wait_gateway || echo "   ⚠ gateway không lên sau khi bật limiter"
  read -r ON_RPS ON_P99 <<< "$(run_catalog "$OUT/ovh-on" 100 30)"

  echo "   limiter TẮT: ${OFF_RPS} RPS · p99 ${OFF_P99} ms | limiter BẬT (ngưỡng cao): ${ON_RPS} RPS · p99 ${ON_P99} ms"
  OVERHEAD="$(awk -v a="$OFF_P99" -v b="$ON_P99" 'BEGIN{printf "%+.1f", b-a}')"
  ok D6 "chi phí 1 round-trip Redis ở biên: Δp99 ${OVERHEAD} ms · ΔRPS $(awk -v a="$OFF_RPS" -v b="$ON_RPS" 'BEGIN{printf "%+.0f", b-a}')"

  # Trả cấu hình về như .env (mặc định: bật, ngưỡng mặc định).
  $COMPOSE up -d --no-deps api-gateway >/dev/null 2>&1
  wait_gateway || true
fi

echo
echo "== Kết quả: $PASS PASS / $FAIL FAIL · raw=$OUT"
[ "$FAIL" -eq 0 ] || exit 1
