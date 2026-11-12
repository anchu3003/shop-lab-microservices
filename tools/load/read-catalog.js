// ==============================================================================
// read-catalog.js — scenario read-heavy nhất của hệ (SL-4.1)
//
// Vì sao là scenario ĐẦU TIÊN phải đo: `architecture.md` §3 ghi traffic đọc catalog ≈ 20:1 so
// với ghi. Nếu chỉ đo checkout thì tối ưu sai chỗ ("nút cổ chai" thật nằm ở đọc).
//
// Trộn 3 endpoint theo tỉ lệ 60/20/20 (list / detail / batch sku) — bám hành vi thật của một
// phiên lướt hàng: xem danh sách → mở chi tiết → giỏ hỏi giá nhiều SKU cùng lúc.
// Tag theo tên endpoint để báo cáo chỉ ra được endpoint nào đắt, không chỉ nói "catalog chậm".
//
// Chạy qua `tools/load.sh --scenario read-catalog` (script này KHÔNG tự quyết bậc VU).
// ==============================================================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE, SEED, stages, SUMMARY_STATS, pick, randomInt } from './common.js';

export const options = {
  scenarios: {
    catalog: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: stages(),
      gracefulRampDown: '5s',
    },
  },
  summaryTrendStats: SUMMARY_STATS,
  // Threshold ở đây KHÔNG phải cửa chặn (load.sh tự tìm điểm vỡ). Hai tác dụng thật:
  //   1) NFR read 150 ms được ghi thành code, không nằm trong đầu ai;
  //   2) đặt threshold theo tag buộc k6 xuất sub-metric `http_req_duration{name:...}`
  //      ⇒ summary có p99 của TỪNG endpoint.
  thresholds: {
    'http_req_duration{name:products_list}': ['p(99)<150'],
    'http_req_duration{name:product_detail}': ['p(99)<150'],
    'http_req_duration{name:skus_lookup}': ['p(99)<150'],
  },
};

const PRODUCTS = SEED.products;
const SKUS = SEED.skus;
// Trang cuối có thể lệch nếu tổng số product chia không hết cho size — lấy page dư 1 vẫn hợp lệ
// (trả 200 + content rỗng) nên không cần chính xác tuyệt đối; đây chỉ là tham số "độ sâu" list.
const PAGES = Math.max(1, Math.ceil(PRODUCTS.length / 20)) + 1;

export default function () {
  const roll = Math.random();
  let res;

  if (roll < 0.6) {
    res = http.get(`${BASE}/v1/products?page=${randomInt(PAGES)}&size=20`, {
      tags: { name: 'products_list' },
    });
  } else if (roll < 0.8) {
    res = http.get(`${BASE}/v1/products/${pick(PRODUCTS).id}`, {
      tags: { name: 'product_detail' },
    });
  } else {
    const ids = [];
    for (let i = 0; i < 10; i++) ids.push(pick(SKUS));
    res = http.get(`${BASE}/v1/skus?ids=${ids.join(',')}`, {
      tags: { name: 'skus_lookup' },
    });
  }

  check(res, { 'status 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return { '/out/summary.json': JSON.stringify(data) };
}
