#!/usr/bin/env bash
# ==============================================================================
# cache-drill.sh — kiểm 4 tính chất của cache catalog bằng lệnh thật (SL-4.2)
#
# Vì sao là script riêng (không nhồi vào `e2e-smoke.sh`):
#   - `e2e-smoke` trả lời "hệ còn chạy đúng không"; script này trả lời "cache có ĐÚNG TÍNH CHẤT
#     không" (hit ratio, chống stampede, invalidate tức thì, fail-open). Gộp lại thì khi đỏ không
#     biết hỏng nghiệp vụ hay hỏng cache.
#   - Drill fail-open phải `docker stop shoplab-redis` — thao tác phá hoại, không được nằm trong
#     đường chạy thường.
#
# 4 phép:
#   D1  Hit ratio của read path (detail/list/skus) — đọc từ `/debug/cache`, ngưỡng ≥ 80%
#   D2  Stampede: xoá key hot rồi bắn 200 request đồng thời ⇒ DB chỉ bị đọc ≤ 2 lần
#   D3  Invalidation: update product ⇒ detail + list trả dữ liệu mới trong ≤ 1 s
#   D4  Fail-open: `docker stop shoplab-redis` ⇒ endpoint catalog vẫn 200 (bật bằng --with-fail-open)
#
# Chạy (stack đã up, `SHOPLAB_DEBUG_ENDPOINTS=true`):
#   bash tools/cache-drill.sh
#   bash tools/cache-drill.sh --with-fail-open
#
# Chỉ dùng curl + python3 + docker (máy dev không có jq).
# ==============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
CATALOG="${CATALOG:-http://localhost:8082}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"
REDIS_CONTAINER="${REDIS_CONTAINER:-shoplab-redis}"
TS="$(date +%s)"
OUT="${OUT_DIR:-/tmp/cache-drill-${TS}}"
mkdir -p "$OUT"
WITH_FAIL_OPEN=0

while [ $# -gt 0 ]; do
  case "$1" in
    --with-fail-open) WITH_FAIL_OPEN=1; shift ;;
    -h|--help) sed -n '2,30p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "tham số lạ: $1"; exit 2 ;;
  esac
done

PASS=0
FAIL=0
TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

ok()  { PASS=$((PASS + 1)); echo "  PASS [$PASS] $1"; }
bad() { FAIL=$((FAIL + 1)); echo "  FAIL [x] $1 — $2"; }

http_do() { # method url [curl args...]
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

login() {
  http_do POST "$GATEWAY/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  json_field accessToken
}

# Bộ đếm cache: in "hits misses dbLoad bypass error" của TỔNG (summary) — đọc delta giữa 2 lần gọi.
counters() {
  curl -s "$CATALOG/debug/cache" | python3 -c '
import sys, json
s = json.load(sys.stdin)["summary"]
print(s["hits"], s["misses"], s["dbLoads"], s["bypasses"], s["errors"])
' 2>/dev/null || echo "0 0 0 0 0"
}

delta() { # before after idx(1..5)
  awk -v a="$1" -v b="$2" -v i="$3" 'BEGIN{split(a,x," ");split(b,y," ");print y[i]-x[i]}'
}

# `date +%s%3N` KHÔNG đáng tin (trên máy này nó in epoch+nano, không cắt 3 số) ⇒ tự đổi sang ms.
now_ms() { echo $(( $(date +%s%N) / 1000000 )); }

redis_cli() {
  docker exec "$REDIS_CONTAINER" redis-cli -a "${REDIS_PASSWORD:-}" --no-auth-warning "$@" 2>/dev/null
}

echo "== cache drill (SL-4.2) · out=$OUT"
http_do GET "$CATALOG/debug/cache"
if [ "$STATUS" != "200" ]; then
  echo "LỖI: $CATALOG/debug/cache → $STATUS (cần SHOPLAB_DEBUG_ENDPOINTS=true)"
  exit 1
fi
SELLER_TOKEN="$(login "$SELLER_EMAIL")"
if [ -z "$SELLER_TOKEN" ]; then
  echo "LỖI: không login được seller — stack đã up chưa?"
  exit 1
fi


# ------------------------------------------------------------------------------
# Dữ liệu cho drill: 1 category + 1 product + 2 SKU (tên riêng để không đụng dữ liệu khác)
# ------------------------------------------------------------------------------
http_do POST "$GATEWAY/v1/categories" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"Cache drill $TS\"}"
CATEGORY_ID="$(json_field id)"
http_do POST "$GATEWAY/v1/products" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$CATEGORY_ID\",\"name\":\"Drill product $TS\",\"price\":150000.00}"
PRODUCT_ID="$(json_field id)"
SKU_IDS=()
for suffix in A B; do
  http_do POST "$GATEWAY/v1/products/$PRODUCT_ID/skus" -H "Authorization: Bearer $SELLER_TOKEN" \
    -H 'Content-Type: application/json' -d "{\"code\":\"DRILL-$TS-$suffix\"}"
  SKU_IDS+=("$(json_field id)")
done
if [ -z "$PRODUCT_ID" ] || [ -z "${SKU_IDS[0]:-}" ]; then
  echo "LỖI: không tạo được product/SKU để drill"
  exit 1
fi
echo "-- product=$PRODUCT_ID sku=${SKU_IDS[*]}"

# ------------------------------------------------------------------------------
# D1 — hit ratio của read path
# ------------------------------------------------------------------------------
echo "-- D1 hit ratio (200 detail + 100 list + 50 skus lookup)"
BEFORE="$(counters)"
for _ in $(seq 1 200); do curl -s -o /dev/null "$GATEWAY/v1/products/$PRODUCT_ID"; done
for _ in $(seq 1 100); do curl -s -o /dev/null "$GATEWAY/v1/products?page=0&size=20"; done
for _ in $(seq 1 50); do
  curl -s -o /dev/null "$GATEWAY/v1/skus?ids=${SKU_IDS[0]},${SKU_IDS[1]}"
done
AFTER="$(counters)"
HITS="$(delta "$BEFORE" "$AFTER" 1)"
MISSES="$(delta "$BEFORE" "$AFTER" 2)"
RATIO="$(awk -v h="$HITS" -v m="$MISSES" 'BEGIN{t=h+m; if (t==0) print 0; else printf "%.1f", 100*h/t}')"
echo "   hits=$HITS misses=$MISSES → hit ratio ${RATIO}%"
echo "hits=$HITS misses=$MISSES ratio=$RATIO" > "$OUT/d1-hit-ratio.txt"
if awk -v r="$RATIO" 'BEGIN{exit !(r>=80)}'; then
  ok "D1 hit ratio ${RATIO}% ≥ 80%"
else
  bad "D1 hit ratio ${RATIO}% < 80%" "xem $OUT/d1-hit-ratio.txt"
fi

# ------------------------------------------------------------------------------
# D2 — stampede: xoá key hot rồi bắn 200 request ĐỒNG THỜI
# ------------------------------------------------------------------------------
echo "-- D2 stampede: DEL key detail rồi bắn 200 request đồng thời"
curl -s -o /dev/null "$GATEWAY/v1/products/$PRODUCT_ID"        # bảo đảm key tồn tại
DELETED="$(redis_cli DEL "catalog:v1:product:$PRODUCT_ID" | tr -d '[:space:]')"
echo "   redis DEL → ${DELETED:-?} (1 = key tồn tại và đã xoá)"
BEFORE="$(counters)"

# 200 request song song (40 luồng) — dùng xargs -P để không cần GNU parallel.
seq 1 200 | xargs -P 40 -I{} curl -s -o /dev/null -w '%{http_code}\n' \
  "$GATEWAY/v1/products/$PRODUCT_ID" > "$OUT/d2-status.txt"
AFTER="$(counters)"
DB_LOADS="$(delta "$BEFORE" "$AFTER" 3)"
HITS_D2="$(delta "$BEFORE" "$AFTER" 1)"
BAD_STATUS="$(grep -cv '^200$' "$OUT/d2-status.txt" || true)"
echo "   DB loads=$DB_LOADS · hits=$HITS_D2 · response khác 200: $BAD_STATUS"
{
  echo "dbLoads=$DB_LOADS hits=$HITS_D2 badStatus=$BAD_STATUS"
} > "$OUT/d2-stampede.txt"
if [ "$BAD_STATUS" = "0" ] && [ "$DB_LOADS" -le 2 ]; then
  ok "D2 stampede: 200 request đồng thời → chỉ $DB_LOADS lần đọc DB (≤ 2)"
else
  bad "D2 stampede" "dbLoads=$DB_LOADS (cần ≤2), response lỗi=$BAD_STATUS"
fi

# ------------------------------------------------------------------------------
# D3 — invalidation: update product ⇒ detail + list tươi trong ≤ 1 s
# ------------------------------------------------------------------------------
echo "-- D3 invalidation: update product rồi poll detail/list (tối đa 1 s)"
NEW_NAME="Drill product MOI $TS"
curl -s -o /dev/null "$GATEWAY/v1/products/$PRODUCT_ID"                 # làm nóng cache
curl -s -o /dev/null "$GATEWAY/v1/products?page=0&size=20&q=drill%20product"
http_do PUT "$GATEWAY/v1/products/$PRODUCT_ID" -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"$NEW_NAME\",\"price\":150000.00}"
WRITE_STATUS="$STATUS"

DETAIL_OK_MS=-1
LIST_OK_MS=-1
START="$(now_ms)"
while [ $(( $(now_ms) - START )) -lt 3000 ]; do
  # KHÔNG dùng `curl | grep -q`: `grep -q` thoát ngay khi khớp ⇒ curl bị SIGPIPE ⇒ với
  # `set -o pipefail` pipeline trả non-zero ⇒ "tìm thấy" bị đọc thành "chưa thấy" (bẫy thật).
  if [ "$DETAIL_OK_MS" -lt 0 ]; then
    DETAIL_BODY="$(curl -s "$GATEWAY/v1/products/$PRODUCT_ID")"
    case "$DETAIL_BODY" in
      *"$NEW_NAME"*) DETAIL_OK_MS=$(( $(now_ms) - START )) ;;
    esac
  fi
  if [ "$LIST_OK_MS" -lt 0 ]; then
    LIST_BODY="$(curl -s "$GATEWAY/v1/products?page=0&size=20&q=drill%20product%20moi")"
    case "$LIST_BODY" in
      *"$NEW_NAME"*) LIST_OK_MS=$(( $(now_ms) - START )) ;;
    esac
  fi
  if [ "$DETAIL_OK_MS" -ge 0 ] && [ "$LIST_OK_MS" -ge 0 ]; then break; fi
  sleep 0.05
done
echo "   write=$WRITE_STATUS · detail tươi sau ${DETAIL_OK_MS} ms · list tươi sau ${LIST_OK_MS} ms"
echo "write=$WRITE_STATUS detail_ms=$DETAIL_OK_MS list_ms=$LIST_OK_MS" > "$OUT/d3-invalidation.txt"
if [ "$WRITE_STATUS" = "200" ] && [ "$DETAIL_OK_MS" -ge 0 ] && [ "$DETAIL_OK_MS" -le 1000 ] \
   && [ "$LIST_OK_MS" -ge 0 ] && [ "$LIST_OK_MS" -le 1000 ]; then
  ok "D3 invalidation: detail ${DETAIL_OK_MS} ms · list ${LIST_OK_MS} ms (≤ 1000)"
else
  bad "D3 invalidation" "detail=${DETAIL_OK_MS}ms list=${LIST_OK_MS}ms (cần ≥0 và ≤1000)"

fi
# ------------------------------------------------------------------------------
# D4 — fail-open: Redis chết ⇒ catalog vẫn 200 (opt-in vì phá hoại: cart-service cũng mất Redis)
# ------------------------------------------------------------------------------
if [ "$WITH_FAIL_OPEN" = "1" ]; then
  echo "-- D4 fail-open: docker stop $REDIS_CONTAINER"
  # Trap để CHẮC CHẮN redis được bật lại, kể cả khi script chết giữa đường.
  trap 'docker start "$REDIS_CONTAINER" >/dev/null 2>&1 || true' EXIT
  docker stop "$REDIS_CONTAINER" > /dev/null 2>&1 || true
  sleep 1

  D4_DETAIL="$(curl -s -o "$OUT/d4-detail.json" -w '%{http_code}' "$GATEWAY/v1/products/$PRODUCT_ID")"
  D4_LIST="$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/v1/products?page=0&size=20")"
  D4_SKUS="$(curl -s -o /dev/null -w '%{http_code}' \
    "$GATEWAY/v1/skus?ids=${SKU_IDS[0]},${SKU_IDS[1]}")"
  D4_CATS="$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/v1/categories")"
  echo "   detail=$D4_DETAIL list=$D4_LIST skus=$D4_SKUS categories=$D4_CATS (Redis đang tắt)"
  echo "detail=$D4_DETAIL list=$D4_LIST skus=$D4_SKUS categories=$D4_CATS" > "$OUT/d4-fail-open.txt"
  if [ "$D4_DETAIL" = "200" ] && [ "$D4_LIST" = "200" ] && [ "$D4_SKUS" = "200" ] \
     && [ "$D4_CATS" = "200" ]; then
    ok "D4 fail-open: 4 endpoint catalog vẫn 200 khi Redis chết"
  else
    bad "D4 fail-open" "detail=$D4_DETAIL list=$D4_LIST skus=$D4_SKUS categories=$D4_CATS"
  fi

  docker start "$REDIS_CONTAINER" > /dev/null 2>&1 || true
  sleep 2
  trap - EXIT
  # Cache phải tự sống lại, không cần restart service.
  curl -s -o /dev/null "$GATEWAY/v1/products/$PRODUCT_ID"
  BEFORE="$(counters)"
  curl -s -o /dev/null "$GATEWAY/v1/products/$PRODUCT_ID"
  AFTER="$(counters)"
  RECOVER_HIT="$(delta "$BEFORE" "$AFTER" 1)"
  if [ "$RECOVER_HIT" -ge 1 ]; then
    ok "D4b Redis bật lại: cache tự hoạt động lại (hit=$RECOVER_HIT)"
  else
    bad "D4b Redis bật lại" "không thấy hit sau khi bật lại (hit=$RECOVER_HIT)"
  fi
fi

echo
echo "== Kết quả: $PASS PASS / $FAIL FAIL · raw=$OUT"
exit $(( FAIL > 0 ? 1 : 0 ))

