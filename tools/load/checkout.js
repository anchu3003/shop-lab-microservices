// ==============================================================================
// checkout.js — scenario ghi nặng nhất: `POST /v1/checkout` (SL-4.1)
//
// Quyết định thiết kế (đo được, không phải cảm tính):
//   1) Mỗi iteration CHỈ 1 request: `POST /v1/checkout`. Giỏ được chuẩn bị 1 lần trong `setup()`.
//      Lý do: `checkout` KHÔNG xoá giỏ (probe: 3 lần checkout liên tiếp trên cùng 1 giỏ đều 201),
//      nên `clear + set item + checkout` mỗi vòng là 3 request — p99 khi đó là p99 của "3 request",
//      không phải của endpoint có NFR 500 ms. k6 cũng đỡ tốn CPU ⇒ ít nguy cơ k6 là điểm vỡ giả.
//   2) Mỗi buyer dùng **cặp SKU riêng** (xem `seed.sh`): 2 VU chung 1 buyer sẽ tranh nhau 1 giỏ
//      ⇒ 422 giả, dễ bị đọc sai thành "server vỡ".
//   3) `Idempotency-Key` = `<RUN_ID>-<vu>-<iter>` (bài học `SL-3.8`): thiếu nó thì retry/đo lại
//      sẽ replay đơn cũ và p99 đo được là p99 của cache replay.
//
// Metric chính: `checkout_ms` (chỉ POST checkout) + `checkout_requests`/`checkout_fail`.
// KHÔNG dùng `http_req_duration` cho kết luận: nó trộn cả ~800 request setup (login/giỏ) vào p99.
//
// Chạy qua `tools/load.sh --scenario checkout`.
// ==============================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE, SEED, stages, SUMMARY_STATS, track429 } from './common.js';

const checkoutMs = new Trend('checkout_ms', true);
const checkoutReq = new Counter('checkout_requests');
const checkoutFail = new Counter('checkout_fail');

const RUN_ID = __ENV.RUN_ID || 'run';
const SETUP_BATCH = Number(__ENV.SETUP_BATCH || 25);

export const options = {
  scenarios: {
    checkout: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: stages(),
      gracefulRampDown: '5s',
    },
  },
  summaryTrendStats: SUMMARY_STATS,
  thresholds: {
    // NFR checkout 500 ms — cùng lý do với read-*: ghi NFR thành code, không phải cửa chặn.
    checkout_ms: ['p(99)<500'],
  },
};

// setup() chạy 1 lần, KHÔNG tính vào `checkout_ms`. Đây là chỗ đắt nhất về số request
// (mỗi buyer: 1 login + 1 clear + 2 set item) nên batch theo lô để không mất 30 s khởi động.
export function setup() {
  const tokens = [];
  const buyers = SEED.buyers;
  for (let start = 0; start < buyers.length; start += SETUP_BATCH) {
    const slice = buyers.slice(start, start + SETUP_BATCH);
    const logins = http.batch(
      slice.map((b) => ({
        method: 'POST',
        url: `${BASE}/v1/auth/login`,
        body: JSON.stringify({ email: b.email, password: b.password }),
        params: {
          headers: { 'Content-Type': 'application/json' },
          tags: { name: 'setup_login' },
        },
      }))
    );
    logins.forEach((res, i) => {
      if (res.status !== 200) return;
      const token = res.json('accessToken');
      if (!token) return;
      const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
      http.del(`${BASE}/v1/carts`, null, { headers, tags: { name: 'setup_cart_clear' } });
      slice[i].skuIds.forEach((skuId) => {
        http.put(`${BASE}/v1/carts/items/${skuId}`, JSON.stringify({ qty: 1 }), {
          headers,
          tags: { name: 'setup_cart_item' },
        });
      });
      tokens.push(token);
    });
  }
  if (tokens.length === 0) {
    throw new Error(
      `setup: không login được buyer nào từ ${buyers.length} buyer trong seed.json — seed.sh đã chạy chưa?`
    );
  }
  return { tokens };
}

export default function (data) {
  // VU ↔ buyer: chia đều, KHÔNG ngẫu nhiên (tránh 2 VU tình cờ trùng buyer và tranh giỏ).
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const res = http.post(`${BASE}/v1/checkout`, '{}', {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Idempotency-Key': `${RUN_ID}-${__VU}-${__ITER}`,
    },
    tags: { name: 'checkout' },
  });

  checkoutMs.add(res.timings.duration);
  checkoutReq.add(1);
  track429(res);
  const ok = check(res, { 'checkout 201': (r) => r.status === 201 });
  if (!ok) {
    checkoutFail.add(1);
    // Log lỗi đầu tiên của mỗi VU (k6 log vào $dir/k6.log) — thiếu bước này thì "error 3%"
    // không nói được là 409 hết hàng, 422 giỏ rỗng hay 503 service chết.
    if (__ITER === 0) {
      console.warn(`checkout lỗi đầu tiên: vu=${__VU} status=${res.status} body=${res.body}`);
    }
  }
}

export function handleSummary(data) {
  return { '/out/summary.json': JSON.stringify(data) };
}
