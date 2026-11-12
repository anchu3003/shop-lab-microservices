#!/usr/bin/env bash
# ==============================================================================
# load.sh — chạy 1 scenario k6 theo BẬC VU và tìm ĐIỂM VỠ (SL-4.1)
#
# Vì sao có script này thay vì "chạy k6 rồi chép số":
#   Mọi con số trong `docs/reports/scale-phase-4-baseline.md` phải tái lập được bằng MỘT lệnh,
#   và mọi bậc phải đo theo cùng một quy ước (cửa sổ, ngưỡng, cách tính RPS). Chép tay từ trí
#   nhớ là số không kiểm chứng được — đúng thứ Phase 4 muốn tránh.
#
# QUY ƯỚC ĐO (dùng chung cả phase — `Backlog.md` §"Chuẩn đo của phase"):
#   - Bậc VU: mặc định 50 → 100 → 200 → 400, mỗi bậc giữ ${DURATION_S}s (mặc định 60).
#   - ĐIỂM VỠ = bậc ĐẦU TIÊN có error > 1% HOẶC p99 > NFR (read 150 ms / checkout 500 ms).
#     Gặp điểm vỡ ⇒ dừng, KHÔNG chạy bậc cao hơn (chạy tiếp chỉ tạo số rác).
#   - RPS = số request của scenario ÷ (ramp + hold) — mẫu số là cửa sổ THẬT của bậc, không dùng
#     "rate" của k6 (k6 tính cả thời gian setup ⇒ thấp hơn thực tế với scenario checkout).
#   - Hệ số NFR = 50.000 ÷ RPS: máy dev 12 thread không tạo nổi 50k RPS ⇒ báo "đạt 1/F mục tiêu"
#     trung thực hơn, và vẫn so before/after được trên cùng bộ đo.
#   - CPU của chính k6 được sample 5 s/lần: k6 chạm trần CPU thì điểm vỡ là của CLIENT, phải nói rõ.
#
# Chạy (stack đã up + đã `bash tools/load/seed.sh`):
#   bash tools/load.sh --scenario read-catalog
#   bash tools/load.sh --scenario read-search
#   bash tools/load.sh --scenario checkout
#   bash tools/load.sh --scenario read-catalog --vus 50,100 --duration 30   # bản nhanh để thử
#
# Raw: /tmp/load-<scenario>-<ts>/ (mỗi bậc 1 thư mục: summary.json · k6.log · cpu.txt · exit-code)
# Chỉ dùng curl + python3 + docker.
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
PG_CONTAINER="${PG_CONTAINER:-shoplab-postgres}"
PG_USER="${POSTGRES_USER:-shoplab}"
REDIS_CONTAINER="${REDIS_CONTAINER:-shoplab-redis}"

SCENARIO=""
VUS_LIST="50,100,200,400"
DURATION_S="${DURATION_S:-60}"
RAMP_S="${RAMP_S:-5}"
WARMUP_S="${WARMUP_S:-20}"
WARMUP_VUS="${WARMUP_VUS:-10}"
NFR_MS=""
OUT=""
DO_WARMUP=1
K6_CPU_WARN="${K6_CPU_WARN:-800}"

while [ $# -gt 0 ]; do
  case "$1" in
    --scenario)  SCENARIO="${2:-}"; shift 2 ;;
    --vus)       VUS_LIST="${2:-}"; shift 2 ;;
    --duration)  DURATION_S="${2:-60}"; shift 2 ;;
    --ramp)      RAMP_S="${2:-5}"; shift 2 ;;
    --nfr)       NFR_MS="${2:-}"; shift 2 ;;
    --out)       OUT="${2:-}"; shift 2 ;;
    --seed-dir)  SEED_DIR="${2:-}"; shift 2 ;;
    --no-warmup) DO_WARMUP=0; shift ;;
    -h|--help)   sed -n '2,40p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "tham số lạ: $1 (xem --help)"; exit 2 ;;
  esac
done

case "$SCENARIO" in
  read-catalog)
    SCRIPT=read-catalog.js; METRIC=http_req_duration; COUNT_METRIC=http_reqs
    ERR_KIND=http_req_failed; NFR_MS="${NFR_MS:-150}" ;;
  read-search)
    SCRIPT=read-search.js; METRIC=http_req_duration; COUNT_METRIC=http_reqs
    ERR_KIND=http_req_failed; NFR_MS="${NFR_MS:-150}" ;;
  checkout)
    # Metric chính là trend riêng, KHÔNG phải http_req_duration: http_req_duration trộn cả
    # ~800 request setup (login + dựng giỏ) vào p99 ⇒ p99 sai ngay từ bậc đầu.
    SCRIPT=checkout.js; METRIC=checkout_ms; COUNT_METRIC=checkout_requests
    ERR_KIND=checkout_fail; NFR_MS="${NFR_MS:-500}" ;;
  *)
    echo "cần --scenario read-catalog|read-search|checkout"; exit 2 ;;
esac

TS="$(date +%s)"
OUT="${OUT:-/tmp/load-${SCENARIO}-${TS}}"
RUN_ID="${SCENARIO}-${TS}"
WINDOW=$((RAMP_S + DURATION_S))
mkdir -p "$OUT"

echo "== load.sh (SL-4.1) · scenario=$SCENARIO"
echo "-- bậc VU: $VUS_LIST · mỗi bậc ${DURATION_S}s (+ramp ${RAMP_S}s) · NFR p99 ${NFR_MS} ms · error ≤ 1%"
echo "-- out: $OUT"

# --- preflight ---------------------------------------------------------------
[ -f "$SEED_DIR/seed.json" ] || {
  echo "LỖI: thiếu $SEED_DIR/seed.json — chạy 'bash tools/load/seed.sh' trước"
  exit 1
}

# Bậc VU > số buyer ⇒ 2 VU dùng chung 1 giỏ ⇒ checkout trả 422 vì VU kia vừa đổi giỏ.
# Đó là lỗi của HARNESS, không phải của server — cảnh báo to để không đọc sai thành điểm vỡ.
BUYERS_IN_SEED="$(python3 -c 'import json,sys;print(len(json.load(open(sys.argv[1])).get("buyers",[])))' "$SEED_DIR/seed.json" 2>/dev/null || echo 0)"
MAX_VU="$(printf '%s' "$VUS_LIST" | tr ',' '\n' | sort -n | tail -1)"
if [ "$SCENARIO" = "checkout" ] && [ "${MAX_VU:-0}" -gt "${BUYERS_IN_SEED:-0}" ]; then
  echo "-- ⚠ bậc ${MAX_VU} VU > ${BUYERS_IN_SEED} buyer trong seed → các VU sẽ CHIA SẺ giỏ"
  echo "   ⇒ 422 là lỗi harness, không phải điểm vỡ. Chạy: bash tools/load/seed.sh --buyers ${MAX_VU}"
fi
docker image inspect "$K6_IMAGE" >/dev/null 2>&1 || {
  echo "-- chưa có image $K6_IMAGE → pull"
  docker pull "$K6_IMAGE" >/dev/null || { echo "LỖI: pull $K6_IMAGE thất bại"; exit 1; }
}
docker network inspect "$NETWORK" >/dev/null 2>&1 || {
  echo "LỖI: không thấy network $NETWORK — stack đã up chưa?"
  echo "     docker compose -f infra/docker-compose.yml up -d"
  exit 1
}
HEALTH="$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/health" 2>/dev/null || true)"
[ "$HEALTH" = "200" ] || echo "-- cảnh báo: $GATEWAY/health → ${HEALTH:-không kết nối}"

# --- số liệu hạ tầng (trước/sau cả lần chạy) ---------------------------------
pg_snapshot() { # → "xact_commit blks_read blks_hit" (tổng mọi DB)
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -tA -F ' ' -c \
    "select coalesce(sum(xact_commit),0), coalesce(sum(blks_read),0), coalesce(sum(blks_hit),0)
       from pg_stat_database" 2>/dev/null || echo "0 0 0"
}
pg_activity() { # → "active lock_wait idle_in_tx"
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -tA -F ' ' -c \
    "select count(*) filter (where state='active'),
            count(*) filter (where wait_event_type='Lock'),
            count(*) filter (where state='idle in transaction')
       from pg_stat_activity where datname not in ('postgres')" 2>/dev/null || echo "0 0 0"
}
redis_cmds() {
  # Redis có `requirepass` (compose) ⇒ `redis-cli INFO` trần trả NOAUTH. `.env` đã được source ở
  # đầu script nên dùng luôn $REDIS_PASSWORD; thiếu biến thì trả 0 thay vì làm chết script.
  docker exec "$REDIS_CONTAINER" redis-cli -a "${REDIS_PASSWORD:-}" --no-auth-warning INFO stats 2>/dev/null \
    | awk -F: '/^total_commands_processed:/{gsub(/\r/,"",$2);print $2+0}' || echo 0
}

# "active lock_wait idle_in_tx conn" — sample trong lúc chạy, lấy ĐỈNH.
pg_activity_total() {
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -tA -F ' ' -c \
    "select count(*) filter (where state='active'),
            count(*) filter (where wait_event_type='Lock'),
            count(*) filter (where state='idle in transaction'),
            count(*)
       from pg_stat_activity where datname not in ('postgres')" 2>/dev/null || echo "0 0 0 0"
}

col_max() { # file col → max (cột theo awk, 1-based)
  awk -v c="$2" 'BEGIN{m=-1} {if ($c+0>m) m=$c+0} END{if (m<0) print "n/a"; else print m}' "$1" 2>/dev/null || echo n/a
}

delta_of() { # "a b c" "d e f" idx → d-a (không âm: counter reset khi restart container)
  awk -v a="$1" -v b="$2" -v i="$3" \
    'BEGIN{split(a,x," ");split(b,y," ");d=y[i]-x[i];print (d<0)?0:d}'
}

ratio_of() { # "a b c"(commit read hit) before after → % buffer hit
  awk -v a="$1" -v b="$2" 'BEGIN{
    split(a,x," "); split(b,y," ");
    hit=y[3]-x[3]; rd=y[2]-x[2]; tot=hit+rd;
    if (tot<=0) print "n/a"; else printf "%.1f", 100*hit/tot}'
}

CPUS="$(nproc)"

PG_BEFORE="$(pg_snapshot)"
REDIS_BEFORE="$(redis_cmds)"
echo "-- pg trước: xact_commit/blks_read/blks_hit = $PG_BEFORE · activity(active/lock/idle-in-tx) = $(pg_activity)"
echo "-- redis trước: total_commands_processed = $REDIS_BEFORE"

{
  echo "scenario=$SCENARIO"
  echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "commit=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo "host=$(uname -srm) · cpus=$(nproc) · mem=$(awk '/MemTotal/{printf "%.1f GB", $2/1048576}' /proc/meminfo)"
  echo "docker=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo unknown)"
  echo "window_s=$WINDOW (ramp $RAMP_S + hold $DURATION_S) · nfr_p99_ms=$NFR_MS · k6_cpu_warn=$K6_CPU_WARN%"
  echo "pg_before=$PG_BEFORE · redis_before=$REDIS_BEFORE"
} > "$OUT/meta.txt"
sed 's/^/   /' "$OUT/meta.txt"

# --- chạy k6 cho 1 bậc -------------------------------------------------------
run_k6() { # vus duration_s ramp_s dir [label]
  local vu="$1" dur="$2" ramp="$3" dir="$4" label="${5:-vu-$1}"
  mkdir -p "$dir"
  # Image k6 chạy bằng user `k6` (không phải root) ⇒ thư mục mount của host phải cho ghi, nếu
  # không `handleSummary` chết với "permission denied" và ta mất summary.json (đã gặp thật).
  chmod 777 "$dir" 2>/dev/null
  local name="shoplab-k6-${SCENARIO}-${label}"
  docker rm -f "$name" >/dev/null 2>&1
  docker run -d --name "$name" --network "$NETWORK" \
    -v "$REPO_ROOT/tools/load:/scripts:ro" \
    -v "$SEED_DIR:/seed:ro" \
    -v "$dir:/out" \
    -e BASE_URL="$GATEWAY_INTERNAL" \
    -e SEED_FILE=/seed/seed.json \
    -e VUS="$vu" -e DURATION="${dur}s" -e RAMP="${ramp}s" \
    -e RUN_ID="${RUN_ID}-${label}" \
    "$K6_IMAGE" run /scripts/"$SCRIPT" > "$dir/docker-run.log" 2>&1

  # Sample CPU của chính k6 trong lúc chạy: k6 chạm trần CPU = điểm vỡ của CLIENT, không phải
  # của server. `--no-stream` mất ~1 s/lần nên chỉ sample mỗi 5 s.
  # Sample luôn `pg_stat_activity`: đỉnh (không phải giá trị cuối) mới là thứ chứng minh nghẽn.
  : > "$dir/cpu.txt"
  : > "$dir/pg.txt"
  local deadline=$(( $(date +%s) + dur + ramp + 180 ))
  while :; do
    local running
    running="$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null || echo false)"
    [ "$running" = "true" ] || break
    docker stats --no-stream --format '{{.CPUPerc}}' "$name" 2>/dev/null | tr -d '%' >> "$dir/cpu.txt"
    pg_activity_total >> "$dir/pg.txt" 2>/dev/null
    if [ "$(date +%s)" -gt "$deadline" ]; then
      echo "timeout-after-${deadline}s" > "$dir/TIMEOUT"
      break
    fi
    sleep 5
  done
  docker wait "$name" > "$dir/exit-code.txt" 2>/dev/null || true
  docker logs "$name" > "$dir/k6.log" 2>&1 || true
  docker rm -f "$name" >/dev/null 2>&1
}

cpu_max() { # file → max % ("n/a" nếu chưa sample được)
  awk 'BEGIN{m=-1} {if ($1+0>m) m=$1+0} END{if (m<0) print "n/a"; else print m}' "$1" 2>/dev/null || echo n/a
}

# Đọc summary.json (do `handleSummary` ghi ra) → "rps p50 p95 p99 err"
# RPS tính theo CỬA SỔ THẬT ($WINDOW), không dùng `rate` của k6 (k6 tính cả setup).
parse_summary() {
  python3 - "$1" "$METRIC" "$COUNT_METRIC" "$ERR_KIND" "$WINDOW" <<'PY'
import json, sys

path, metric, count_metric, err_kind, window = sys.argv[1:6]
try:
    metrics = json.load(open(path))["metrics"]
except Exception:
    print("0.0\t0.0\t0.0\t0.0\t100.00")
    raise SystemExit

def vals(key):
    """Lấy phần số của 1 metric.

    k6 v1: {type, med, p(99), ...} · k6 v2 (image `latest` hiện tại): số nằm trong `values`.
    Đọc được cả hai để script không gãy khi image đổi — và `n/a` thay vì số 0 giả khi thiếu metric.
    """
    m = metrics.get(key, {})
    return m.get("values", m) if isinstance(m, dict) else {}

m = vals(metric)
p50, p95, p99 = m.get("med", 0.0), m.get("p(95)", 0.0), m.get("p(99)", 0.0)
count = vals(count_metric).get("count", 0) or 0
rps = count / float(window)
if err_kind == "http_req_failed":
    err = vals("http_req_failed").get("rate", 0.0) * 100
else:
    reqs = vals("checkout_requests").get("count", 0) or 1
    err = (vals(err_kind).get("count", 0) / reqs) * 100
print(f"{rps:.1f}\t{p50:.1f}\t{p95:.1f}\t{p99:.1f}\t{err:.2f}")
PY
}

# p99 từng endpoint (chỉ read-catalog tạo sub-metric theo tag) — để báo cáo nói được
# "chậm ở detail hay ở batch sku", không chỉ nói "catalog chậm".
endpoint_p99() {
  python3 - "$1" <<'PY'
import json, sys
metrics = json.load(open(sys.argv[1]))["metrics"]
parts = []
for key, metric in metrics.items():
    if not key.startswith("http_req_duration{name:"):
        continue
    name = key.split("name:")[1].rstrip("}")
    if name.startswith("setup_"):
        continue
    values = metric.get("values", metric)
    if "p(99)" not in values:
        continue
    parts.append(f"{name}={values['p(99)']:.0f}")
print(" ".join(parts) if parts else "(không có)")
PY
}

# --- warm-up: JVM + connection pool nóng trước khi đo bậc 50 -----------------
# Không có bước này, bậc đầu tiên đo cả JIT/khởi tạo pool ⇒ dễ kết luận "vỡ ở 50 VU" sai.
if [ "$DO_WARMUP" = 1 ]; then
  echo "-- warm-up ${WARMUP_VUS} VU / ${WARMUP_S}s (không tính vào bảng)"
  run_k6 "$WARMUP_VUS" "$WARMUP_S" 2 "$OUT/warmup" "warmup"
  echo "   warm-up xong · k6 CPU max $(cpu_max "$OUT/warmup/cpu.txt")%"
fi

# --- vòng bậc VU -------------------------------------------------------------
ROWS=()
RESULT_TSV="$OUT/result.tsv"
printf 'vu\trps\tp50_ms\tp95_ms\tp99_ms\terr_pct\tk6_cpu_max\n' > "$RESULT_TSV"
KNEE_VU=""; KNEE_REASON=""; CPU_HOT_NOTE=""
LAST_OK_VU=""; LAST_OK_RPS=""; LAST_OK_P99=""; LAST_OK_ERR=""; LAST_OK_CPU=""

IFS=',' read -r -a VUS_ARR <<< "$VUS_LIST"
for vu in "${VUS_ARR[@]}"; do
  dir="$OUT/vu-$vu"
  echo "-- bậc ${vu} VU (${DURATION_S}s) → $dir"
  run_k6 "$vu" "$DURATION_S" "$RAMP_S" "$dir"
  [ -f "$dir/TIMEOUT" ] && echo "   LỖI: k6 không kết thúc trong thời gian cho phép (xem $dir/TIMEOUT)"
  if [ ! -f "$dir/summary.json" ]; then
    echo "   LỖI: không có summary.json — log cuối:"
    tail -8 "$dir/k6.log" 2>/dev/null | sed 's/^/     /'
    exit 1
  fi

  read -r rps p50 p95 p99 err <<< "$(parse_summary "$dir/summary.json")"
  cpu="$(cpu_max "$dir/cpu.txt")"
  echo "   RPS ${rps} · p50 ${p50} · p95 ${p95} · p99 ${p99} · error ${err}% · k6 CPU ${cpu}%"
  [ "$SCENARIO" != "checkout" ] && echo "   p99 theo endpoint: $(endpoint_p99 "$dir/summary.json")"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$vu" "$rps" "$p50" "$p95" "$p99" "$err" "$cpu" >> "$RESULT_TSV"
  ROWS+=("| $vu | $rps | $p50 | $p95 | $p99 | $err | $cpu |")

  over_p99="$(awk -v a="$p99" -v b="$NFR_MS" 'BEGIN{print (a>b)?1:0}')"
  over_err="$(awk -v a="$err" 'BEGIN{print (a>1)?1:0}')"
  cpu_hot="$(awk -v a="$cpu" -v b="$K6_CPU_WARN" 'BEGIN{print (a>b)?1:0}' 2>/dev/null || echo 0)"

  if [ "$over_p99" = 1 ] || [ "$over_err" = 1 ]; then
    KNEE_VU="$vu"
    [ "$over_p99" = 1 ] && KNEE_REASON="p99 ${p99} ms > NFR ${NFR_MS} ms"
    [ "$over_err" = 1 ] && KNEE_REASON="${KNEE_REASON:+$KNEE_REASON + }error ${err}% > 1%"
    echo "   ⇒ ĐIỂM VỠ ở ${vu} VU (${KNEE_REASON}) — dừng ramp"
    break
  fi

  LAST_OK_VU="$vu"; LAST_OK_RPS="$rps"; LAST_OK_P99="$p99"; LAST_OK_ERR="$err"; LAST_OK_CPU="$cpu"
  if [ "$cpu_hot" = 1 ]; then
    echo "   ⚠ k6 CPU ${cpu}% > ${K6_CPU_WARN}% ⇒ bậc này có thể đã chạm trần CLIENT"
    CPU_HOT_NOTE="${CPU_HOT_NOTE}${CPU_HOT_NOTE:+ · }bậc ${vu} VU: k6 CPU ${cpu}%"
  fi
done

# --- tổng hợp ----------------------------------------------------------------
PG_AFTER="$(pg_snapshot)"
REDIS_AFTER="$(redis_cmds)"

# RPS "ngay trước điểm vỡ" = bậc cuối cùng còn ĐẠT ngưỡng.
BASE_RPS="${LAST_OK_RPS:-$(awk 'NR==2{print $2}' "$RESULT_TSV")}"
FACTOR="$(awk -v r="$BASE_RPS" 'BEGIN{if (r+0>0) printf "%.0f", 50000/r; else print "n/a"}')"
if [ -n "$KNEE_VU" ]; then
  KNEE_LINE="${KNEE_VU} VU — ${KNEE_REASON}"
else
  KNEE_LINE="không — ramp hết ${VUS_LIST} VU vẫn đạt ngưỡng"
fi
if [ -z "$LAST_OK_VU" ]; then
  # Trường hợp chạy 1 bậc duy nhất và bậc đó đã vỡ: KHÔNG có "bậc ngay trước điểm vỡ" để so.
  # Nói thẳng ra thay vì in "bậc cuối đạt" sai (đã gặp khi probe 1 bậc để tìm nghẽn).
  BASE_LINE="(không có bậc nào đạt ngưỡng — bậc đầu tiên đã vỡ)"
  FACTOR="n/a"
else
  BASE_LINE="${LAST_OK_VU} VU · ${LAST_OK_RPS} req/s · p99 ${LAST_OK_P99} ms · error ${LAST_OK_ERR}%"
fi

TABLE="| VU | RPS | p50 (ms) | p95 (ms) | p99 (ms) | error % | k6 CPU max % |
|---|---|---|---|---|---|---|
$(printf '%s\n' "${ROWS[@]}")"

{
  echo "## $SCENARIO"
  echo
  echo "$TABLE"
  echo
  echo "- **Điểm vỡ**: ${KNEE_LINE}"
  echo "- **Bậc cuối ĐẠT ngưỡng**: ${BASE_LINE}"
  echo "- **Hệ số quy đổi NFR**: 50.000 ÷ ${BASE_RPS} = **1/${FACTOR}** mục tiêu (máy dev ${CPUS} thread)"
  echo "- **k6 CPU max**: ${LAST_OK_CPU:-n/a}% (ngưỡng cảnh báo ${K6_CPU_WARN}%)${CPU_HOT_NOTE:+ — ⚠ $CPU_HOT_NOTE}"
  echo "- **PG**: xact_commit $(delta_of "$PG_BEFORE" "$PG_AFTER" 1) · blks_read $(delta_of "$PG_BEFORE" "$PG_AFTER" 2) · blks_hit $(delta_of "$PG_BEFORE" "$PG_AFTER" 3) · buffer hit ratio $(ratio_of "$PG_BEFORE" "$PG_AFTER")%"
  echo "- **PG activity đỉnh (trong bậc cuối)**: active $(col_max "$dir/pg.txt" 1) · lock-wait $(col_max "$dir/pg.txt" 2) · idle-in-tx $(col_max "$dir/pg.txt" 3) · conn $(col_max "$dir/pg.txt" 4)"
  echo "- **Redis**: total_commands_processed +$(awk -v a="$REDIS_BEFORE" -v b="$REDIS_AFTER" 'BEGIN{print (b-a<0)?0:b-a}')"
  echo "- **Raw**: \`$OUT\` (mỗi bậc: \`summary.json\` · \`k6.log\` · \`cpu.txt\` · \`pg.txt\` · \`exit-code.txt\`)"
} | tee "$OUT/summary.md"

# --- chốt ra stdout cho người đọc -------------------------------------------
echo
if [ -n "$KNEE_VU" ]; then
  echo ">> ĐIỂM VỠ: ${KNEE_VU} VU — ${KNEE_REASON}"
  echo ">> Bậc cuối ĐẠT: ${BASE_LINE} ⇒ 1/${FACTOR} mục tiêu NFR"
else
  echo ">> Không vỡ trong ${VUS_LIST} — bậc cao nhất vẫn đạt ngưỡng (p99 ${LAST_OK_P99:-n/a} ms)"
fi
echo ">> bảng + raw: $OUT/summary.md · $OUT/result.tsv"
