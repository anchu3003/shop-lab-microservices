#!/usr/bin/env bash
# ==============================================================================
# seed.sh — dữ liệu cố định cho load test Phase 4 (SL-4.1)
#
# Dataset (theo "Chuẩn đo của phase" trong `Backlog.md`):
#   20 category · 1.000 product · 2.000 SKU · 200 buyer · tồn kho 1.000/SKU
#
# Vì sao seed qua API chứ không INSERT thẳng vào Postgres:
#   - Đi ĐÚNG đường thật: product → outbox → relay → search index; stock → ledger.
#     INSERT tay thì `read-search` sẽ đo một index rỗng — số đo vô nghĩa mà vẫn "chạy được".
#   - Idempotent dựa vào ràng buộc CỦA HỆ (slug category, code SKU, email user) thay vì
#     phải tự quản state ở ngoài.
#   Giá phải trả: ~5.000 request (≈ 2–3 phút) thay vì vài ms SQL. Baseline chạy 1 lần, đổi lại
#   được sự trung thực — đáng.
#
# Idempotent (AC #1: chạy 2 lần không tạo row mới, in cùng bộ ID):
#   - category : slug sinh từ name ⇒ tìm theo name trong `GET /v1/categories` (trả full list)
#   - product  : name KHÔNG unique trong DB ⇒ tìm bằng `q=<name>` rồi so khớp **exact**
#   - SKU      : `code` UNIQUE toàn hệ ⇒ lấy từ `GET /v1/products/{id}` (detail trả kèm skus)
#   - buyer    : email UNIQUE ⇒ register 409 là bình thường, login để lấy token
#   - stock    : `PUT /v1/stock/{skuId}` là SET TUYỆT ĐỐI ⇒ chạy lại = reset về 1.000
#                (409 nếu `onHand < reserved` — reservation TTL 15m, chờ hết hạn rồi chạy lại)
#
# Tên product cố ý có token tiếng Việt (`ao thun load 0001`) chứ không phải `load-prod-0001`:
# search dùng `plainto_tsquery('simple', unaccent(q))`, tên toàn chữ-số-la-tinh sẽ cho ra index
# mà truy vấn thật (`ao thun`, `giay`) không bao giờ khớp ⇒ `read-search` đo toàn 0 row.
#
# Chạy:
#   bash tools/load/seed.sh                 # đủ dataset (~2–3 phút), in seed.json + fingerprint
#   bash tools/load/seed.sh --scale small    # 20 product / 20 buyer để thử script
#   bash tools/load/seed.sh --buyers 400     # thêm buyer đệm (bậc VU 400 không phải chia sẻ giỏ)
#   bash tools/load/seed.sh --no-reindex     # bỏ bước dựng lại index search ở cuối
#
# Output: /tmp/load-seed/seed.json — `tools/load.sh` mount thư mục này vào container k6 (`/seed`).
# Chỉ dùng curl + python3 + docker (máy dev không có jq).
# ==============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shoplab.local}"
SELLER_EMAIL="${SELLER_EMAIL:-seller@shoplab.local}"
DEV_PASSWORD="${DEV_PASSWORD:-matkhau123}"
PG_CONTAINER="${PG_CONTAINER:-shoplab-postgres}"
PG_USER="${POSTGRES_USER:-shoplab}"

N_CATEGORIES="${N_CATEGORIES:-20}"
N_PRODUCTS="${N_PRODUCTS:-1000}"
SKUS_PER_PRODUCT="${SKUS_PER_PRODUCT:-2}"
N_BUYERS="${N_BUYERS:-200}"
STOCK="${STOCK:-1000}"

OUT_DIR="${OUT_DIR:-/tmp/load-seed}"
DO_REINDEX=1
COUNT_DB=1

while [ $# -gt 0 ]; do
  case "$1" in
    --scale)
      case "${2:-}" in
        small) N_CATEGORIES=4; N_PRODUCTS=20; SKUS_PER_PRODUCT=2; N_BUYERS=20 ;;
        *) echo "--scale chỉ nhận 'small' (dataset nhỏ để thử script)"; exit 2 ;;
      esac
      shift 2 ;;
    --buyers)   N_BUYERS="$2"; shift 2 ;;
    --products) N_PRODUCTS="$2"; shift 2 ;;
    --stock)    STOCK="$2"; shift 2 ;;
    --out)      OUT_DIR="$2"; shift 2 ;;
    --no-reindex) DO_REINDEX=0; shift ;;
    --no-counts)  COUNT_DB=0; shift ;;
    -h|--help)  sed -n '2,45p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "tham số lạ: $1 (xem --help)"; exit 2 ;;
  esac
done

SEED_FILE="$OUT_DIR/seed.json"
mkdir -p "$OUT_DIR"
PROD_TSV="$OUT_DIR/.products.tsv"   # productId \t name \t skuIds(space)
BUYER_TSV="$OUT_DIR/.buyers.tsv"    # email

TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

CREATED_CAT=0; HIT_CAT=0
CREATED_PROD=0; HIT_PROD=0
CREATED_SKU=0;  HIT_SKU=0
CREATED_BUYER=0; HIT_BUYER=0
STOCK_SET=0; STOCK_FAIL=0

# Vocab tiếng Việt không dấu — mỗi product lấy 1 cụm làm tiền tố tên. Pool này cũng chính là
# bộ term mà `read-search.js` dùng để truy vấn (đảm bảo có kết quả, không đo trên index rỗng).
TERMS=("ao thun" "quan jean" "giay the thao" "tai nghe" "dien thoai" \
       "ban phim" "chuot may tinh" "man hinh" "op lung" "sac du phong")
LETTERS=(A B C D)

req() { # method path [token] [json-body] → STATUS, BODY
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$GATEWAY$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  # Xoá body TRƯỚC khi curl: connection-refused thì curl không mở `-o` ⇒ không xoá trước là
  # ta đọc body của lần trước (bẫy đã gặp ở SL-1.7).
  : > "$TMP_BODY"
  STATUS="$(curl "${args[@]}" 2>/dev/null || true)"
  BODY="$(cat "$TMP_BODY")"
}

jget() { # field → giá trị (chuỗi) hoặc rỗng
  printf '%s' "$BODY" | python3 -c \
    "import sys,json
d=json.load(sys.stdin)
print(d.get('$1','') if isinstance(d,dict) else '')" 2>/dev/null || true
}

login() {
  req POST /v1/auth/login "" "{\"email\":\"$1\",\"password\":\"$DEV_PASSWORD\"}"
  jget accessToken
}

urlenc() { python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$1"; }

db_count() { # db sql → số (rỗng nếu không có docker/psql)
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$1" -tAc "$2" 2>/dev/null | tr -d '[:space:]'
}

counts_line() {
  local cat sku buyer
  cat="$(db_count catalog "select count(*) from category where name like 'load-cat-%'")"
  sku="$(db_count catalog "select count(*) from sku where code like 'LOAD-%'")"
  buyer="$(db_count identity "select count(*) from users where email like 'load-buyer-%'")"
  echo "catalog: ${cat:-?} category / ${sku:-?} sku · identity: ${buyer:-?} buyer"
}

echo "== seed load Phase 4 (SL-4.1) → $OUT_DIR"
echo "-- dataset: $N_CATEGORIES category · $N_PRODUCTS product · $((N_PRODUCTS * SKUS_PER_PRODUCT)) SKU · $N_BUYERS buyer · stock $STOCK/SKU"
[ "$COUNT_DB" = 1 ] && echo "-- DB trước khi seed: $(counts_line)"

ADMIN_TOKEN="$(login "$ADMIN_EMAIL")"
SELLER_TOKEN="$(login "$SELLER_EMAIL")"
if [ -z "$ADMIN_TOKEN" ] || [ -z "$SELLER_TOKEN" ]; then
  echo "LỖI: không login được admin/seller ở $GATEWAY"
  echo "     stack đã up chưa? (LocalDevUserSeeder chỉ seed admin/seller khi APP_ENV=local)"
  exit 1
fi

# ------------------------------------------------------------------------------
# 1) Category — idempotent theo name (slug sinh từ name nên trùng name = trùng row)
# ------------------------------------------------------------------------------
declare -A CAT_IDS=()
req GET /v1/categories
while IFS=$'\t' read -r cid cname; do
  [ -n "$cid" ] && CAT_IDS["$cname"]="$cid"
done < <(printf '%s' "$BODY" | python3 -c '
import sys, json
for c in json.load(sys.stdin):
    print(c["id"] + "\t" + c["name"])' 2>/dev/null || true)

for i in $(seq 1 "$N_CATEGORIES"); do
  cname="load-cat-$(printf '%02d' "$i")"
  if [ -n "${CAT_IDS[$cname]:-}" ]; then HIT_CAT=$((HIT_CAT + 1)); continue; fi
  req POST /v1/categories "$SELLER_TOKEN" "{\"name\":\"$cname\"}"
  cid="$(jget id)"
  if [ -z "$cid" ]; then
    echo "LỖI tạo category $cname (HTTP $STATUS): $(head -c 200 "$TMP_BODY")"
    exit 1
  fi
  CAT_IDS["$cname"]="$cid"
  CREATED_CAT=$((CREATED_CAT + 1))
done
echo "-- category: tạo mới $CREATED_CAT · đã có $HIT_CAT"

# ------------------------------------------------------------------------------
# 2) Product + SKU — product tìm bằng q rồi so khớp exact; SKU tìm theo code
# ------------------------------------------------------------------------------
: > "$PROD_TSV"
for i in $(seq 1 "$N_PRODUCTS"); do
  pname="${TERMS[$(((i - 1) % ${#TERMS[@]}))]} load $(printf '%04d' "$i")"
  cname="load-cat-$(printf '%02d' "$(((i - 1) % N_CATEGORIES + 1))")"
  price="$(printf '%d' $((100000 + ((i - 1) % 50) * 1000))).00"

  pid=""
  req GET "/v1/products?size=5&q=$(urlenc "$pname")"
  pid="$(printf '%s' "$BODY" | WANT="$pname" python3 -c '
import sys, json, os
want = os.environ["WANT"]
for p in json.load(sys.stdin).get("content", []):
    if p.get("name") == want:
        print(p["id"])
        break' 2>/dev/null || true)"

  sku_map=""
  if [ -n "$pid" ]; then
    HIT_PROD=$((HIT_PROD + 1))
    req GET "/v1/products/$pid"
    sku_map="$(printf '%s' "$BODY" | python3 -c '
import sys, json
for s in json.load(sys.stdin).get("skus", []):
    print(s["code"] + "\t" + s["id"])' 2>/dev/null || true)"
  else
    req POST /v1/products "$SELLER_TOKEN" \
      "{\"categoryId\":\"${CAT_IDS[$cname]}\",\"name\":\"$pname\",\"price\":$price}"
    pid="$(jget id)"
    if [ -z "$pid" ]; then
      echo "LỖI tạo product $pname (HTTP $STATUS): $(head -c 200 "$TMP_BODY")"
      exit 1
    fi
    CREATED_PROD=$((CREATED_PROD + 1))
  fi

  ids=""
  for s in $(seq 1 "$SKUS_PER_PRODUCT"); do
    code="LOAD-$(printf '%04d' "$i")-${LETTERS[$((s - 1))]}"
    sid="$(while IFS=$'\t' read -r c existing; do
             [ "$c" = "$code" ] && { echo "$existing"; break; }
           done <<< "$sku_map")"
    if [ -n "$sid" ]; then
      HIT_SKU=$((HIT_SKU + 1))
    else
      req POST "/v1/products/$pid/skus" "$SELLER_TOKEN" "{\"code\":\"$code\"}"
      sid="$(jget id)"
      if [ -z "$sid" ]; then
        echo "LỖI tạo SKU $code (HTTP $STATUS): $(head -c 200 "$TMP_BODY")"
        exit 1
      fi
      CREATED_SKU=$((CREATED_SKU + 1))
    fi
    ids="$ids $sid"
  done
  printf '%s\t%s\t%s\n' "$pid" "$pname" "${ids# }" >> "$PROD_TSV"

  [ $((i % 100)) -eq 0 ] && echo "   ... $i/$N_PRODUCTS product"
done
echo "-- product: tạo mới $CREATED_PROD · đã có $HIT_PROD"
echo "-- sku: tạo mới $CREATED_SKU · đã có $HIT_SKU"

# ------------------------------------------------------------------------------
# 3) Stock — PUT là SET TUYỆT ĐỐI ⇒ mỗi lần chạy seed lại là reset về $STOCK.
#    409 = onHand < reserved (đang có reservation chưa hết TTL 15m) → cảnh báo, không chặn.
# ------------------------------------------------------------------------------
LAST_STOCK_ERR=""
while IFS=$'\t' read -r pid pname skuids; do
  for sid in $skuids; do
    req PUT "/v1/stock/$sid" "$ADMIN_TOKEN" "{\"onHand\":$STOCK}"
    if [ "$STATUS" = "200" ]; then
      STOCK_SET=$((STOCK_SET + 1))
    else
      STOCK_FAIL=$((STOCK_FAIL + 1))
      LAST_STOCK_ERR="HTTP $STATUS $(head -c 160 "$TMP_BODY")"
    fi
  done
done < "$PROD_TSV"
# ------------------------------------------------------------------------------
# 4) Buyer — email UNIQUE ⇒ register 409 là bình thường. Login lại để chắc chắn dùng được
#    (token KHÔNG lưu vào seed.json: token ngắn hạn, `checkout.js` tự login trong `setup()`).
# ------------------------------------------------------------------------------
: > "$BUYER_TSV"
for b in $(seq 1 "$N_BUYERS"); do
  email="load-buyer-$(printf '%03d' "$b")@shoplab.local"
  req POST /v1/auth/register "" \
    "{\"email\":\"$email\",\"password\":\"$DEV_PASSWORD\",\"displayName\":\"Load Buyer $b\"}"
  if [ "$STATUS" = "201" ]; then
    CREATED_BUYER=$((CREATED_BUYER + 1))
  else
    HIT_BUYER=$((HIT_BUYER + 1))
  fi
  register_status="$STATUS"
  if [ -z "$(login "$email")" ]; then
    echo "LỖI: buyer $email không login được (register HTTP $register_status): $(head -c 200 "$TMP_BODY")"
    exit 1
  fi
  printf '%s\n' "$email" >> "$BUYER_TSV"
  [ $((b % 100)) -eq 0 ] && echo "   ... $b/$N_BUYERS buyer"
done
echo "-- buyer: tạo mới $CREATED_BUYER · đã có $HIT_BUYER"

# ------------------------------------------------------------------------------
# 5) Search index — seed qua API phát event product.* → consumer index dần; gọi reindex để
#    `read-search` không phải chờ (idempotent, gọi nhiều lần cùng kết quả).
# ------------------------------------------------------------------------------
if [ "$DO_REINDEX" = 1 ]; then
  echo "-- dựng lại index search (POST /v1/admin/search/reindex)"
  req POST /v1/admin/search/reindex "$ADMIN_TOKEN"
  echo "   HTTP $STATUS $(printf '%s' "$BODY" | head -c 220)"
fi

# ------------------------------------------------------------------------------
# 6) seed.json — thứ duy nhất `tools/load.sh` cần (mount vào container k6 ở `/seed`)
# ------------------------------------------------------------------------------
GATEWAY_INTERNAL="${GATEWAY_INTERNAL:-http://api-gateway:8080}"
python3 - "$PROD_TSV" "$BUYER_TSV" "$SEED_FILE" "$DEV_PASSWORD" "$STOCK" \
  "$GATEWAY_INTERNAL" "${TERMS[*]}" "$N_CATEGORIES" <<'PY'
import hashlib
import json
import sys
from datetime import datetime, timezone

(prod_tsv, buyer_tsv, out, password, stock,
 gateway_internal, terms, n_categories) = sys.argv[1:9]

products = []
skus = []
with open(prod_tsv, encoding="utf-8") as fh:
    for line in fh:
        line = line.rstrip("\n")
        if not line:
            continue
        pid, name, ids = line.split("\t")
        sku_ids = ids.split() if ids else []
        products.append({"id": pid, "name": name, "skuIds": sku_ids})
        skus.extend(sku_ids)

emails = [line.strip() for line in open(buyer_tsv, encoding="utf-8") if line.strip()]

# Mỗi buyer một CẶP SKU riêng: `checkout.js` giữ nguyên giỏ cho mọi iteration (checkout không
# xoá giỏ) ⇒ 2 VU dùng chung 1 buyer sẽ tranh nhau 1 giỏ (422 giả). Cặp riêng cũng rải stock
# đều trên nhiều SKU thay vì dồn 1 SKU.
pairs = []
if skus:
    for i in range(len(emails)):
        pair = [skus[(2 * i) % len(skus)], skus[(2 * i + 1) % len(skus)]]
        pairs.append(pair)

buyers = [
    {"email": email, "password": password, "skuIds": pairs[i] if i < len(pairs) else []}
    for i, email in enumerate(emails)
]

fingerprint = hashlib.sha256(
    ("\n".join(p["id"] for p in products) + "\n" + "\n".join(emails)).encode()
).hexdigest()[:16]

doc = {
    "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    "gatewayInternal": gateway_internal,
    "counts": {
        "categories": int(n_categories),
        "products": len(products),
        "skus": len(skus),
        "buyers": len(buyers),
        "stockPerSku": int(stock),
    },
    "fingerprint": fingerprint,
    "products": products,
    "skus": skus,
    "buyers": buyers,
    "searchTerms": terms.split(),
}

with open(out, "w", encoding="utf-8") as fh:
    json.dump(doc, fh, ensure_ascii=False, indent=0)
print(fingerprint)
PY
FINGERPRINT="$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["fingerprint"])' "$SEED_FILE")"

echo
echo "== xong"
[ "$COUNT_DB" = 1 ] && echo "-- DB sau khi seed: $(counts_line)"
echo "-- fingerprint : $FINGERPRINT   (chạy lại phải RA CÙNG số này ⇒ không tạo row mới)"
echo "-- seed.json    : $SEED_FILE ($(du -h "$SEED_FILE" | cut -f1))"
echo "-- product đầu/cuối: $(head -1 "$PROD_TSV" | cut -f1) … $(tail -1 "$PROD_TSV" | cut -f1)"
echo "-- buyer đầu/cuối  : $(head -1 "$BUYER_TSV") … $(tail -1 "$BUYER_TSV")"
echo
echo "Tiếp: bash tools/load.sh --scenario read-catalog   (hoặc read-search / checkout)"
