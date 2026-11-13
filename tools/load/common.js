// ==============================================================================
// common.js — phần dùng chung cho 3 scenario load test Phase 4 (SL-4.1)
//
// Vì sao tách file: 3 scenario cùng cần base URL, seed pool, stage ramp và p99 stats.
// Copy-paste 4 chỗ thì lần sau sửa NFR/thống kê sẽ sửa thiếu 1 chỗ — đúng loại lỗi "số đo
// không so sánh được" mà Phase 4 muốn tránh.
// ==============================================================================
import { sleep } from 'k6';
import { Counter } from 'k6/metrics';

// k6 chạy trong network `shoplab-net` (Docker) chứ không phải trên host ⇒ gọi tên container,
// KHÔNG dùng localhost. Đổi khi chạy k6 trực tiếp trên host: `-e BASE_URL=http://localhost:8080`.
export const BASE = __ENV.BASE_URL || 'http://api-gateway:8080';

// SL-4.3: đếm 429 của MỌI scenario.
//
// Vì sao phải đếm: từ khi gateway có rate limit, một bậc VU có thể "vỡ" vì bị **chặn ở biên**
// (k6 đi từ 1 IP nên bucket IP coi cả bài đo là một client) — hoàn toàn khác "hệ không chịu nổi tải".
// Không đếm riêng thì hai hiện tượng này trông giống nhau trong summary (chỉ thấy error cao), và
// báo cáo sẽ ghi sai bản chất. `tools/load.sh` đọc `rl_429` để cảnh báo bậc không hợp lệ.
export const RL_429 = new Counter('rl_429');

export function track429(res) {
  if (res && res.status === 429) {
    RL_429.add(1);
  }
}

// seed.json do `tools/load/seed.sh` sinh ra, được mount vào container ở `/seed`.
// `open()` chỉ gọi được ở init context (ngoài default function) — đây đúng chỗ.
export const SEED = JSON.parse(open(__ENV.SEED_FILE || '/seed/seed.json'));

export function vus() {
  return Number(__ENV.VUS || 50);
}

export function duration() {
  return __ENV.DURATION || '60s';
}

// Ramp NGẮN (5 s) rồi giữ phẳng: bậc phải là "bao nhiêu client đồng thời", không phải "ramp mượt".
// Cửa sổ đo = ramp + hold (load.sh tính RPS theo đúng cửa sổ này) ⇒ ramp 5/65 ≈ 8% độ pha loãng,
// đủ nhỏ để không đổi kết luận điểm vỡ, và ghi rõ trong báo cáo thay vì giấu.
export function stages() {
  return [
    { duration: __ENV.RAMP || '5s', target: vus() },
    { duration: duration(), target: vus() },
  ];
}

// p99 phải có trong summary: mặc định k6 chỉ in p(90)/p(95) ⇒ load.sh cần p(99) để so NFR.
export const SUMMARY_STATS = ['avg', 'med', 'p(95)', 'p(99)', 'max'];

export function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export function randomInt(maxExclusive) {
  return Math.floor(Math.random() * maxExclusive);
}

// handleSummary chuẩn của cả 3 script: ghi raw JSON cho `tools/load.sh` parse.
// Không in text summary ra stdout — bảng markdown do load.sh in (một nguồn số duy nhất).
export function handleSummary(data) {
  return { '/out/summary.json': JSON.stringify(data) };
}

export function think() {
  sleep(Number(__ENV.THINK_S || 0));
}
