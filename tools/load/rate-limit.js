// ==============================================================================
// rate-limit.js — 500 VU KHÔNG token dội vào đọc public (SL-4.3)
//
// Mục đích KHÔNG phải đo throughput (đó là việc của `read-catalog`): đây là phép thử **lớp chặn ở
// biên**. Câu hỏi cần trả lời:
//   1) tải dồn vào một IP từ ngoài có bị chặn ở gateway không (≥ 90% là 429)?
//   2) phần bị chặn có **không** chạm DB không (`xact_commit` không tăng tương ứng)? → đo ở drill.
//
// Vì sao `constant-vus` chứ không ramp: cần "500 client cùng lúc" ổn định trong suốt cửa sổ đo;
// ramp chỉ làm loãng tỉ lệ 429 ở đoạn đầu.
//
// Chạy qua `tools/rate-limit-drill.sh` (D1). Chạy tay:
//   docker run --rm --network shoplab-net -v $PWD/tools/load:/scripts:ro -v /tmp/out:/out \
//     -e BASE_URL=http://api-gateway:8080 grafana/k6 run /scripts/rate-limit.js
// ==============================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, SEED, SUMMARY_STATS, track429 } from './common.js';

// Request "lọt" (không bị 429) — tách khỏi 5xx để drill nói được "hệ vẫn sống, chỉ là bị chặn".
const notThrottled = new Counter('rl_not_throttled');

export const options = {
  scenarios: {
    flood: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 500),
      duration: __ENV.DURATION || '60s',
    },
  },
  summaryTrendStats: SUMMARY_STATS,
  // KHÔNG đặt threshold cho tỉ lệ 429: ngưỡng của k6 (`rate`) cho Counter dễ bị hiểu sai (là
  // count/giây, không phải tỉ lệ). Drill tự tính `rl_429.count / http_reqs.count` từ summary.json —
  // một con số, một cách tính, không phải đoán.
};

const PAGES = Math.max(1, Math.ceil(SEED.products.length / 20)) + 1;

export default function () {
  const page = Math.floor(Math.random() * PAGES);
  const res = http.get(`${BASE}/v1/products?page=${page}&size=20`, {
    tags: { name: 'products_flood' },
  });

  track429(res);
  if (res.status !== 429) {
    notThrottled.add(1);
  }
  check(res, { 'có phản hồi HTTP': (r) => r.status > 0 });
}

export function handleSummary(data) {
  return { '/out/summary.json': JSON.stringify(data) };
}
