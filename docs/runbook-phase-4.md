# Runbook — Phase 4 (scale): rate limit ở gateway (`SL-4.3`)

> Phase 4 chưa đóng, file này **chỉ** chứa playbook của rate limit. `SL-4.12` sẽ mở rộng thành runbook đầy đủ của phase
> (cache, replica lag, pool cạn, partition). Quyết định thiết kế: **ADR-0010**.

## 1. Bảng tra nhanh

| Triệu chứng | Nhiều khả năng là | Kiểm bằng | Xử lý |
|---|---|---|---|
| Client báo **429 hàng loạt** | ngưỡng quá chặt cho luồng đó, hoặc một client chiếm hạn mức | `curl -H "Authorization: Bearer $TOKEN" :8080/debug/rate-limit` → `counters.rejected`, `routes[].perUser/perIp` | nâng ngưỡng bằng env (`RATE_LIMIT_*`) rồi `up -d --no-deps api-gateway`; KHÔNG tắt hẳn trừ khi là ca sự cố |
| **429 cả với request nội bộ/seed/e2e** | công cụ dồn request qua biên từ 1 IP | `docker logs shoplab-api-gateway \| grep 'rate-limit: chặn'` | cho công cụ đó chờ `Retry-After` (seed.sh đã làm), hoặc chạy với `RATE_LIMIT_ENABLED=false` rồi bật lại |
| p99 tăng **đều mọi endpoint** sau khi bật limiter | +1 round-trip Redis ở cửa vào (D6: +16 ms @100 VU) | so `--with-overhead` | chấp nhận (đã ghi `TD-40`), hoặc nâng `RATE_LIMIT_ENABLED` policy ở ticket Phase 5 (local pre-check) |
| **Log WARN `rate-limit: redis lỗi, fail-open`** | Redis chết/chậm > 100 ms | `docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" ping`, `docker stats shoplab-redis` | fail-open đang bảo vệ hệ: **không** cần làm gì gấp; sửa Redis. Tuyệt đối **không** đặt `RATE_LIMIT_FAIL_OPEN=false` để "cho chắc" |
| Redis **không** lên ⇒ ai cũng hỏi "sao gateway vẫn chạy?" | **đúng thiết kế** | `docker inspect shoplab-api-gateway \| grep -A5 Health` | gateway **không** `depends_on` Redis và Redis **không** trong `/ready` — mất lớp bảo vệ ≠ mất cửa vào |
| Bậc load test có 429 ⇒ "điểm vỡ" sai | limiter đang bật khi đo | `tools/load.sh` in `⚠ ... × 429` | đo lại với `RATE_LIMIT_ENABLED=false` (k6 đi từ 1 IP nên xô IP chặn chính phép đo) |
| CPU gateway tăng sau khi bật | Redis hop + parse Lua | `docker stats shoplab-api-gateway` trước/sau | giảm tần suất gọi bằng cách cho request bypass (`/health`, `/version`, `/debug/**` đã có) |

## 2. Lệnh chẩn đoán (copy-paste được)

```bash
# 1) limiter đang bật? ngưỡng hiện tại? bao nhiêu request bị chặn từ lúc khởi động?
TOKEN=$(curl -s -X POST localhost:8080/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@shoplab.local","password":"matkhau123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/debug/rate-limit | python3 -m json.tool

# 2) xô nào đang cạn? (key: rl:<route>:u:<sub> | rl:<route>:ip:<ip>)
docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern 'rl:*' | head
docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning HGETALL 'rl:default:ip:172.18.0.1'

# 3) Redis còn khoẻ?
docker exec shoplab-redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning INFO stats | grep -E 'total_commands|rejected'

# 4) chứng minh lại 5 tính chất sau khi đổi cấu hình
bash tools/rate-limit-drill.sh
```

## 3. Ba việc **không** được làm

1. **Không** đặt `RATE_LIMIT_FAIL_OPEN=false` để "chắc chắn có rate limit". Redis chập chờn ⇒ toàn hệ nhận 503 từ cửa duy
   nhất. Muốn chặn cứng thì phải có Redis HA, không phải đổi cờ.
2. **Không** nới ngưỡng toàn cục (`RATE_LIMIT_DEFAULT_IP_RPS=1000000`) để "chạy cho xong" một phép đo — khi đó bậc đo không
   còn là của hệ, và không ai nhớ để trả lại. Dùng `RATE_LIMIT_ENABLED=false` **tạm thời**, đo xong bật lại ngay.
3. **Không** thêm Redis vào `/health` hay `/ready`. Đã có ghi chú trong `application.yml` và ADR-0010 D5 — nếu ai thêm vào,
   hãy đọc D5 trước.
