// ==============================================================================
// read-search.js — scenario tìm kiếm (PostgreSQL FTS) (SL-4.1)
//
// Vì sao đo riêng dù cũng là "đọc": search đi qua `search-service` + `tsvector`/GIN, khác hoàn
// toàn đường đọc của catalog (JPA + index B-tree). Gộp chung sẽ không biết chậm ở đâu.
//
// Term lấy từ `seed.json` (`searchTerms`) — chính là các cụm có trong tên product đã seed.
// Nếu hard-code term lạ thì `plainto_tsquery` trả 0 row: endpoint vẫn 200 nhưng ta đang đo
// "truy vấn không khớp gì" ⇒ số đẹp mà vô nghĩa.
//
// Chạy qua `tools/load.sh --scenario read-search`.
// ==============================================================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE, SEED, stages, SUMMARY_STATS, pick, randomInt, track429 } from './common.js';

export const options = {
  scenarios: {
    search: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: stages(),
      gracefulRampDown: '5s',
    },
  },
  summaryTrendStats: SUMMARY_STATS,
  thresholds: {
    'http_req_duration{name:search}': ['p(99)<150'],
  },
};

const TERMS = SEED.searchTerms && SEED.searchTerms.length ? SEED.searchTerms : ['ao thun'];
const SIZES = [10, 20, 50];

export default function () {
  const q = encodeURIComponent(pick(TERMS));
  const size = pick(SIZES);
  const res = http.get(`${BASE}/v1/search?q=${q}&page=0&size=${size}`, {
    tags: { name: 'search' },
  });
  // 200 là đủ: search trả page rỗng khi không khớp, không phải lỗi. Kiểm thêm `content` tồn tại
  // để không "xanh" nhầm khi service trả body sai shape (`SearchPage` dùng `content` như
  // `PageResponse` của catalog).
  check(res, {
    'status 200': (r) => r.status === 200,
    'có field content': (r) => r.status !== 200 || r.body.indexOf('"content"') >= 0,
  });
  track429(res);
}

export function handleSummary(data) {
  return { '/out/summary.json': JSON.stringify(data) };
}
