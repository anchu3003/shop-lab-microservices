# Backlog Phase 3 — Async & event-driven (archive)

> **Archive** chi tiết của Phase 3 (đã đóng). `Backlog.md` chỉ giữ trạng thái + link tới đây.
> Snapshot code/doc tại thời điểm đóng phase: tag `phase-3` · [`docs/phases/phase-3.md`](docs/phases/phase-3.md) ·
> runbook [`docs/runbook-phase-3.md`](docs/runbook-phase-3.md) · số đo [`docs/reports/async-phase-3.md`](docs/reports/async-phase-3.md).
> Quyết định archive: [ADR-0005](docs/adr/0005-phase-snapshot-and-release.md).

| | |
|---|---|
| **Phase** | 3 — Async & event-driven |
| **Ticket** | `SL-3.0` … `SL-3.11` — **12/12 Done** (10 kế hoạch + 2 phát sinh: `SL-3.10` tắt CI tạm thời, `SL-3.11` tách service-to-service auth từ `SL-3.5`) |
| **Thời điểm đóng** | 2026-11-11 |
| **Stack** | Java 21 · Spring Boot 4.1.2 · PostgreSQL 16 · Redis 7 · **RabbitMQ 4 (dùng thật từ phase này)** · Docker Compose · GitHub Actions |
| **Service** | `api-gateway` (8080) · `identity` (8081) · `catalog` (8082) · `cart` (8083) · `inventory` (8084) · `promotion` (8086) · `order` (8087) · `payment` (8088) · **`notification` (8089)** · **`search` (8091)** |
| **Kết quả** | 4 luồng async: `order.confirmed` ⇒ email · `payment.succeeded` ⇒ order tự `CONFIRMED` · `product.updated/archived` ⇒ search index · lỗi ⇒ DLQ ⇒ replay tay. **DLQ = 0** dưới tải (100 checkout + 100 update), phát = xử lý **100/100**, 0 email trùng; consumer chết ⇒ checkout p95 **44,6 ms**; relay **63,7 msg/s** |

**Mục tiêu phase**: phần **không critical** chạy async và **event không bị mất** khi có sự cố — trọng tâm
*transactional outbox + at-least-once + consumer idempotent + DLQ*, không phải "dùng được Kafka".

**Điều kiện hoàn thành** (đã đạt): `order.confirmed` phát từ outbox ⇒ email async · `payment.succeeded` tự confirm
(**trả `TD-17`**) · index search cập nhật sau khi product đổi · message lỗi vào DLQ (không chặn queue) · có báo cáo số
liệu async · runbook + tag `phase-3`.

**Debt đã trả trong phase**: `TD-17` (webhook không cập nhật order — `SL-3.5`) · `TD-25` (sweeper không có token user —
`SL-3.5` + `SL-3.11`) · `TD-32` (outbox copy lần 3 — chấp nhận có ý thức, `SL-3.6`) · `TD-28` (CI tắt tạm — bật lại ở
`SL-3.9`) · `TD-35` (trùng `TD-29` — gộp ở `SL-3.9`).
**Debt mới của phase**: `TD-29` (outbox không có job dọn) · `TD-30` (`processed_events`/`notification_logs`) · `TD-31`
(không có refund) · `TD-33` (`min_price` chưa tính SKU override) · `TD-34` (OpenAPI thiếu spec 2 service mới) · `TD-36`
(IT `CheckoutSagaIT` flaky theo tải).

---
### SL-3.0 — ADR-0007: contract event (envelope · naming · outbox relay · DLQ)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-2.10` (Phase 2 đóng); chặn **mọi** ticket `SL-3.x` |
| **Ước lượng** | S |

**Bối cảnh**

Phase 2 gọi sync 5 dependency: nhanh, dễ debug, nhưng **sai một cái là cả checkout fail** và không có cách nào để phần
"không critical" (email, index search) chạy sau. Phase 3 thêm broker — nhưng nếu mỗi service tự đặt tên event, tự chọn
kiểu payload thì 3 tháng sau không ai đọc được message trong DLQ. Contract phải chốt **trước** dòng code đầu tiên
(`AGENTS.md` §3.3).

**Mục tiêu**

`docs/adr/0007-event-contract.md` chốt 5 quyết định: **envelope**, **đặt tên**, **topology**, **relay outbox**, **DLQ**.

**In scope**

- Envelope JSON: `eventId` (uuid) · `eventType` (string, có `.vN`) · `version` (int) · `occurredAt` (ISO-8601 UTC) ·
  `producer` (tên service) · `traceId` · `payload` (object). **Không** đặt field nghiệp vụ ở tầng envelope.
- Đặt tên: routing key `<aggregate>.<event>.v<major>` (vd `order.confirmed.v1`) trên exchange **topic**
  `shoplab.events`; queue theo **consumer** (`q.notification`, `q.order`, `q.search`) + `<queue>.dlq`; exchange chết `shoplab.dlx`.
- Ngữ nghĩa giao: **at-least-once** + consumer idempotent theo `eventId`; **không** hứa exactly-once/ordering toàn cục
  (ordering chỉ trong 1 queue + 1 consumer).
- Relay outbox: polling `@Scheduled` (mặc định 1 s, batch 100, `FOR UPDATE SKIP LOCKED`) + **publisher confirm** rồi mới
  đánh `SENT`. Nêu rõ vì sao **không** CDC/Debezium ở phase này và **điều kiện revisit**.
- DLQ: retry trong consumer 3 lần (backoff 200 ms/1 s/3 s) → `nack(requeue=false)` vào `<queue>.dlq`; **không auto-replay**;
  replay bằng tay (`tools/dlq.sh`). Ghi rõ ai được replay và cần kiểm gì trước khi replay.
- Versioning: **không** sửa payload `.v1`; thay đổi không tương thích ⇒ phát `.v2` song song, consumer hỗ trợ cả hai rồi
  mới bỏ `.v1`. Bảng 5 event khởi điểm: `order.confirmed.v1` · `order.cancelled.v1` · `payment.succeeded.v1` ·
  `product.updated.v1` · `product.archived.v1` (kèm producer/consumer/payload field).

**Out of scope**

- Không viết code broker/publisher/consumer (đó là `SL-3.1`/`SL-3.2`).
- Không bàn Kafka (đã chốt ở §11.2 — Phase 8), không bàn schema registry (không cần với 5 event).
- Không thiết kế event cho shipping/review (Phase 8).

**Acceptance Criteria**

- [x] `docs/adr/0007-event-contract.md` có đủ Context · Options · Decision · Consequences · **Revisit** (đúng skill `shoplab-adr`).
- [x] Có **bảng 5 event** với: routing key · producer · consumer · payload field · khi nào phát.
- [x] Có câu trả lời tường minh cho: *vì sao polling relay, không CDC* · *vì sao at-least-once* · *DLQ replay do ai*.
- [x] Mỗi quyết định có **điều kiện revisit verify được** (số liệu/ngưỡng), không phải "khi cần".
- [x] `architecture.md` §9.4 (API & contract) + §11.2 link tới ADR-0007.

**Ghi chú kỹ thuật**

- ADR này **cố ý ngắn** (không quá ~150 dòng): nó chốt *hợp đồng*, không dạy lại lý thuyết messaging.
- Ghi rõ **cái đã bỏ**: exactly-once (đắt, cần transactional broker), schema registry, outbox CDC — kèm điều kiện revisit.

**Kết quả verify** *(2026-11-08 — ticket docs-only, không có code)*

```console
$ ls docs/adr/
0001-tech-stack.md  0002-dev-environment.md  0003-spring-boot-version-and-module-layout.md
0004-service-commons-shared-library.md  0005-phase-snapshot-and-release.md
0006-checkout-model.md  0007-event-contract.md          ← mới

$ grep -n '^## ' docs/adr/0007-event-contract.md
11:## Context   43:## Options   89:## Decision   147:## Consequences   174:## Revisit
$ wc -l docs/adr/0007-event-contract.md
185 docs/adr/0007-event-contract.md                      (trong ngưỡng "~150 dòng" của ticket, +35 cho bảng event/topology)

# 5 quyết định đều có bảng Options (≥ 3 phương án, có phương án bị loại) + Revisit định lượng:
$ grep -c '^### [0-9]\. (D' docs/adr/0007-event-contract.md
5
$ grep -c '| **A\.' docs/adr/0007-event-contract.md     # mỗi D có phương án A (được chọn)
5
```

**Nội dung đã chốt** (đối chiếu AC):

| AC | Bằng chứng trong ADR |
|---|---|
| Bảng 5 event | § Decision → bảng `Routing key · Producer · Consumer · Payload · Phát khi` (5 dòng) |
| Vì sao polling relay | § Options D4 (4 phương án, loại CDC + publish-in-transaction + `LISTEN/NOTIFY` vì lý do cụ thể) |
| Vì sao at-least-once | § Options D3 (loại exactly-once vì broker không tham gia transaction DB; loại at-most-once vì mất message) |
| DLQ replay do ai | § Decision D5 (không auto-replay + 3 điều phải kiểm trước khi replay) + § Options D5 (loại auto-replay) |
| Revisit định lượng | § Revisit: relay p95 > 5 s **hoặc** > 30% CPU **hoặc** > 500 msg/s · schema registry khi > 20 event / ≥ 2 sự cố · DLQ > 50 msg/tuần |
| `architecture.md` link | §9.4 (bỏ "schema registry từ Phase 3") + §11.2 + header `DRAFT v0.5` + Changelog §8 |

**Sự cố trong lúc làm (2 lỗi do chính mình, đã sửa)**

- Chèn section bằng `insert_line` vào **giữa** bảng Options D3 ⇒ dòng `At-most-once` bị đẩy xuống cuối file. Phát hiện
  bằng `grep -n '^## '` thấy `## Decision` nằm trước nội dung bảng. Sửa bằng script di chuyển dòng về đúng chỗ.
- Lọt **1 từ tiếng Trung** (chữ Hán của "bên thứ ba") trong ô so sánh CloudEvents — `grep` theo ký tự Hán mới thấy. Đã đổi thành
  "bên thứ ba".

**Learning note**

- **ADR là chỗ *loại* phương án, không phải chỗ khoe phương án đã chọn**: 4/5 hạng mục có ≥ 1 phương án ghi rõ
  "❌ Không dùng" + lý do (publish-trong-transaction: broker không tham gia transaction DB ⇒ hoặc mất event hoặc bịa
  event). Nếu chỉ viết "chọn A vì A tốt" thì 3 tháng sau sẽ có người đề xuất lại đúng phương án đã loại.
- **Quyết định nào cũng phải kéo theo một *điều kiện bỏ* định lượng được**: `Revisit` ở đây toàn số (p95 > 5 s, > 30% CPU,
  > 500 msg/s, > 20 event, > 50 msg DLQ/tuần). Không có con số thì "khi cần" = không bao giờ.
- **Chốt contract trước khi code là rẻ nhất**: ADR này ~185 dòng nhưng nó khoá 6 ticket phía sau (`SL-3.1`…`SL-3.6`);
  đổi tên routing key hay bỏ outbox sau khi có code = migration message + sửa 3 service + sửa E2E.
- **Doc cũ có thể mâu thuẫn với chính nó**: §9.4 ghi "schema registry từ Phase 3" trong khi quyết định là **không** dùng
  ⇒ phải sửa §9.4 ngay trong ticket này, không để "lát nữa" (bài học `SL-2.10`: doc cũng mục như code).
- **Công cụ soạn doc cũng cần verify**: 2 lỗi (chèn sai vị trí, lọt chữ Trung) đều **không** bị lint/CI bắt — chỉ
  `grep -n '^## '` và đọc lại mới thấy. Với PR docs-only (không có CI) thì bước tự đọc lại là **bắt buộc**.

### SL-3.1 — RabbitMQ: topology + healthcheck + công cụ kiểm tra

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.0` (tên exchange/queue chốt ở ADR-0007); chặn `SL-3.2`+ |
| **Ước lượng** | S |

**Bối cảnh**

`rabbitmq` nằm trong compose **từ Phase 0** nhưng chưa một dòng code nào publish/consume — tức là chưa ai chứng minh nó
thật sự chạy được, chưa có healthcheck thật, và chưa có topology. Đây là "hạ tầng trang trí", đúng thứ `AGENTS.md` §3.4
muốn tránh nếu kéo sang phase sau.

**Mục tiêu**

Broker chạy được, có topology khai báo bằng code, và có **một lệnh** để xem exchange/queue/DLQ/consumer.

**In scope**

- Compose: user/password lấy từ `.env` (`RABBITMQ_DEFAULT_USER/PASS`), healthcheck `rabbitmq-diagnostics -q ping`,
  publish port 5672 + management **15672** (chỉ local), volume dữ liệu (queue durable sống qua restart).
- Topology khai báo bằng code trong `service-commons` (`Declarables`): exchange topic `shoplab.events`, exchange
  `shoplab.dlx`, 3 queue consumer + 3 `.dlq`, binding theo routing key (danh sách từ ADR-0007).
- `tools/rabbit-topology.sh`: in exchange/queue (ready, unacked, consumer count) qua management API, **không** cần UI.
- README: mục "Broker" — mở UI `localhost:15672`, lệnh xem queue, ý nghĩa `ready`/`unacked`.

**Out of scope**

- Không viết publisher/consumer nghiệp vụ (`SL-3.2`+). Không sharding/cluster broker (single node là đủ để học).
- Không đổi `.env.example` của service nào (chỉ thêm biến broker).

**Acceptance Criteria**

- [x] `docker compose ps` → `shoplab-rabbitmq` **healthy**; `docker restart` xong queue + binding **vẫn còn**.
- [x] `bash tools/rabbit-topology.sh` in ra: 1 exchange `shoplab.events` · 1 `shoplab.dlx` · 3 queue + 3 DLQ · binding đúng routing key.
- [x] Publish 1 message test (không có consumer) → `ready = 1`; consume bằng UI/script → `ready = 0`, `ack` thành công.
- [x] Message vào queue khi **không** có consumer vẫn còn sau `docker restart` broker (durable + persistent).
- [x] `mvn verify` không đổi (chưa có code nghiệp vụ); `e2e-smoke`/`e2e-checkout` không regress.

**Ghi chú kỹ thuật**

- **Khai topology bằng code** (không dùng `definitions.json`) để nó luôn khớp tên trong ADR-0007 và không phải import tay
  khi dựng máy mới.
- `unacked` là chỉ số hay bị bỏ qua: nó là **consumer đang giữ message** (đang retry) — DLQ chỉ tăng khi message bị
  `nack` hẳn.

**Kết quả verify** *(chạy thật 2026-11-08 — 12 container healthy)*

**1) Topology có thật trên broker** (`bash tools/rabbit-topology.sh`):

```console
== Exchanges (shoplab.*)
  shoplab.dlx          type=direct   durable=True
  shoplab.events       type=topic    durable=True
== Queues (ready · unacked · consumers)
  q.notification / q.notification.dlq / q.order / q.order.dlq / q.search / q.search.dlq   (6 queue, ready=0)
== Bindings
  shoplab.dlx    --q.notification       --> q.notification.dlq
  shoplab.dlx    --q.order              --> q.order.dlq
  shoplab.dlx    --q.search             --> q.search.dlq
  shoplab.events --order.cancelled.v1   --> q.notification
  shoplab.events --order.confirmed.v1   --> q.notification
  shoplab.events --payment.succeeded.v1 --> q.order
  shoplab.events --product.archived.v1  --> q.search
  shoplab.events --product.updated.v1   --> q.search
```

**2) Publish → restart broker → message vẫn còn → drain ack:**

```console
$ bash tools/rabbit-topology.sh publish product.updated.v1
publish product.updated.v1 -> {"routed":true}          →  q.search ready=1
$ docker restart shoplab-rabbitmq                      # → healthy lại sau ~15 s
$ curl …/api/queues/%2F/q.search | …                   # messages=2 ready=2  ← KHÔNG mất
$ bash tools/rabbit-topology.sh drain q.search 2
  ack  routing_key=product.updated.v1  payload={"ping": true}
  ack  routing_key=product.updated.v1  payload={"ping": true}
$ bash tools/rabbit-topology.sh | grep 'q.search '     # ready=0  (ack xong)
```

**3) Bẫy im lặng `routed=false`** (gõ sai routing key) — tool in cảnh báo ngay:

```console
$ bash tools/rabbit-topology.sh publish product.updated.v9
publish product.updated.v9 -> {"routed":false}
  (routed=false nghĩa là KHÔNG queue nào bind routing key đó — sai tên key!)
```

**4) Test + không regress:**

```text
$ mvn -B -ntp verify          BUILD SUCCESS · 11 module · 662 test (544 unit + 118 IT) · 0 fail
                              service-commons 54 unit (+7: EventTopologyAutoConfigurationTest 5 · EventTopologyDeclarerTest 2)
$ e2e-smoke 17/17 · e2e-checkout 27/27 · e2e-checkout-fail 41/41
```

**Sự cố trong lúc verify (1 lỗi thiết kế + 2 lỗi phụ)**

1. **Topology KHÔNG xuất hiện dù code đã khai** — image có đủ `spring-rabbit`, nhưng script in rỗng. Nguyên nhân:
   `RabbitAdmin` khai **lazy**, chỉ khi có connection đầu tiên; SL-3.1 chưa có publisher/consumer ⇒ **không ai mở
   connection** ⇒ không khai. → Thêm `EventTopologyDeclarer` gọi `amqpAdmin.initialize()` lúc `SmartInitializingSingleton`.
2. **Message "mất" sau restart** (đọc `ready=0` ngay sau khi broker healthy): thực ra **không mất** — `/api/queues`
   tổng hợp số liệu ~5 s/lần nên trả số cũ. Kiểm bằng `messages` + `drain` mới đúng.
3. **Tool sai escaping**: `python3 -c '…'` trong single quote làm `\"` thành literal ⇒ `SyntaxError`; chuyển sang
   `.format()` + double quote, `publish` nhận body qua heredoc.

**Learning note**

- **"Khai bằng code" không có nghĩa là "đã khai"**: `Declarables` chỉ là *ý định*; việc khai thật do `RabbitAdmin`
  làm **lazy** khi mở connection. Service chỉ-khai-topology (chưa publish/consume) sẽ im lặng không có queue nào ⇒
  phải kiểm bằng **trạng thái của hệ ngoài** (management API), không kiểm bằng "code đã chạy".
- **Fail mềm là quyết định, không phải tiện tay**: broker chết lúc start **không** được làm service chết (async không
  nằm trên đường critical — ADR-0007). Nhưng fail mềm có giá: lỗi chỉ còn 1 dòng `WARN` ⇒ phải có tool để *nhìn thấy*
  hậu quả.
- **Monitoring có độ trễ riêng**: `ready/unacked` lag ~5 s sau restart ⇒ đọc "0 message" **không** chứng minh mất dữ
  liệu. Trước khi kết luận, kiểm bằng đường thứ hai (`messages` hoặc `drain` để ack thật).
- **Tool vận hành phải tự bảo vệ người dùng**: `routed=false` của RabbitMQ là lỗi **im lặng** (message rơi vào hư
  không, không exception) ⇒ tool in cảnh báo ngay cạnh kết quả và có `dlq`/`drain`/`purge` để không phải mở UI làm tay.
- **Test topology không cần broker**: `Declarables` là dữ liệu thuần ⇒ unit test khẳng định tên/durability/DLX-args/
  binding. Đây là test **hợp đồng** với ADR-0007 — routing key sai 1 ký tự đỏ ngay trong CI thay vì lặng lẽ mất message.


### SL-3.2 — `service-commons`: envelope + publisher + consumer base + dedupe

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.0` (contract) · `SL-3.1` (topology); chặn `SL-3.3`·`SL-3.4`·`SL-3.5`·`SL-3.6` |
| **Ước lượng** | M |

**Bối cảnh**

Nếu mỗi service tự gọi `rabbitTemplate.convertAndSend(...)` thì sẽ lặp lại đúng 3 lỗi: quên `traceId` (log không nối được),
quên dedupe (xử lý 2 lần khi message redeliver), và retry vô hạn (queue treo). Cùng bài học `SL-2.12`: hành vi **của toàn hệ**
thì phải nằm ở `service-commons`, không copy 6 lần.

**Mục tiêu**

Service mới chỉ cần khai dependency là có: publish đúng envelope, consume có retry → DLQ, dedupe theo `eventId`.

**In scope**

- `commons/events/EventEnvelope` (record) + `EventEnvelopeCodec` (JSON, `occurredAt` ISO-8601, **reject** `version` không hỗ trợ).
- `EventPublisher`: publish lên `shoplab.events` với `routingKey = eventType`; bật **publisher confirm**
  (`spring.rabbitmq.publisher-confirm-type=correlated`), message `persistent`, `traceId` lấy từ MDC nếu caller không truyền.
- `EventConsumer` (base class + dispatcher): parse envelope → `MDC.put(traceId)` → **dedupe** → gọi handler → `ack`;
  lỗi ⇒ retry **3 lần** (200 ms / 1 s / 3 s) ⇒ `nack(requeue=false)` ⇒ DLQ; mọi nhánh log đều có `eventId`.
- `ProcessedEventStore` (interface) + `JdbcProcessedEventStore` (`insert into processed_events(event_id, consumer, event_type)
  on conflict do nothing` ⇒ `0 row` = đã xử lý): kèm **SQL mẫu** trong javadoc, **không** ship Flyway migration
  (mỗi service tự thêm vào DB của mình).
- `EventAutoConfiguration`: `@ConditionalOnClass(RabbitTemplate)` + `@ConditionalOnProperty(shoplab.events.enabled)` ⇒
  service **không** dùng event (template, gateway) không bị ảnh hưởng.
- Unit test: codec (round-trip · version lạ) · dedupe (handler chỉ chạy 1 lần với cùng `eventId`) · retry → DLQ (mock `Channel`) ·
  auto-config back-off (`FilteredClassLoader`).

**Out of scope**

- Không có event nghiệp vụ nào trong commons (chỉ hạ tầng). Không schema registry. Không bàn ordering/partition key.
- Không làm outbox ở đây — outbox ghi vào DB của **từng service** (`SL-3.3`/`SL-3.5`/`SL-3.6`).

**Acceptance Criteria**

- [x] Publish 1 event từ test ⇒ message có đủ 7 field envelope; `traceId` khớp MDC của caller.
- [x] Consumer nhận **cùng `eventId` 2 lần** ⇒ handler chạy **1 lần**, lần 2 `ack` luôn (không vào DLQ).
- [x] Handler ném exception ⇒ retry 3 lần rồi **1 message** ở `<queue>.dlq`, `<queue>` rỗng, `unacked = 0`.
- [x] Service **không** khai `spring-boot-starter-amqp` vẫn boot bình thường (test `FilteredClassLoader` + `service-template` xanh).
- [x] `commons` unit test xanh **không cần broker thật** (mock `RabbitTemplate`/`Channel`); `mvn -B -ntp verify` toàn repo xanh.

**Ghi chú kỹ thuật**

- **Dedupe ở DB của consumer, không ở Redis**: Redis có thể mất dữ liệu (cart chấp nhận được, dedupe thì không), và dedupe
  phải nằm cùng transaction với việc xử lý. Bảng `processed_events` phình dần ⇒ ghi `TD` về job dọn.
- **Retry trong consumer** (blocking, giữ `unacked`) khác **retry ở transport** (RabbitMQ không tự retry): chọn retry trong
  consumer để còn log/inspect được — trade-off đã biết là consumer giữ thread ~4,2 s.
- `@ConditionalOnClass` phải ở **cấp class auto-config** (bài học `SL-2.12`: đặt trên `@Bean` method ⇒ `NoClassDefFoundError`).

**Kết quả verify** *(chạy thật 2026-11-08 — 12 container healthy)*

**1) `service-commons` 71 unit test xanh — 15 test mới cho hạ tầng event:**

```text
EventEnvelopeCodecTest          5   round-trip 7 field · version lạ ⇒ EventFormatException ·
                                    version payload ≠ hậu tố eventType ⇒ từ chối · JSON hỏng/thiếu field
EventPublisherTest              2   envelope đủ field + traceId từ MDC · broker nack ⇒ EventPublishException
AbstractEventConsumerTest       5   ack khi xong · trùng eventId ⇒ handler 1 lần · retry cạn ⇒ nack(false)
                                    · payload hỏng ⇒ nack ngay · event lạ ⇒ ack + bỏ qua
EventAutoConfigurationTest      3   có AMQP ⇒ codec/publisher/container MANUAL ack · JdbcTemplate ⇒ store
                                    · enabled=false ⇒ không bean · không AMQP ⇒ back-off
```

**2) Toàn repo:** `mvn -B -ntp verify` **BUILD SUCCESS · 679 test (561 unit + 118 IT) · 0 fail**.

**3) Trên stack thật** (rebuild `order-service` — service duy nhất có AMQP):

```console
$ docker ps | grep order        → shoplab-order-service Up (healthy)
$ docker exec shoplab-postgres psql -U shoplab -d order -c '\d processed_events'
                     Table "public.processed_events"
    Column    |           Type        | Nullable | Default
 event_id     | uuid                 | not null |
 consumer     | character varying(64)| not null |
 event_type   | character varying(128)| not null |
 processed_at | timestamp with time zone | not null | now()
Indexes: "processed_events_pkey" PRIMARY KEY, btree (event_id, consumer)

$ bash tools/e2e-smoke.sh 17/17 · e2e-checkout 27/27 · e2e-checkout-fail 41/41
```

Bảng do commons tạo hộ (`shoplab.events.dedup.auto-create-schema=true`) — bằng chứng store dedupe **có**
được tạo và **có** DDL đúng khoá `(event_id, consumer)`.

**Sự cố trong lúc verify (1 bug thật — loại bug im lặng nguy hiểm nhất)**

`processed_events` **không được tạo** dù service chạy healthy và log sạch. Nguyên nhân: `@ConditionalOnBean
(JdbcTemplate.class)` chỉ thấy bean **đã đăng ký tại thời điểm nó được đánh giá**; không khai thứ tự thì
auto-config của commons chạy **trước** `JdbcTemplateAutoConfiguration` ⇒ điều kiện luôn `false` ⇒ bean store
không bao giờ được tạo, **không một dòng lỗi**. Fix: `@AutoConfiguration(after = RabbitAutoConfiguration.class,
afterName = "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration")` + 2 test khoá lại
(có `JdbcTemplate` ⇒ có store; không có ⇒ không store, không lỗi).
Lưu ý phụ: Boot 4 đặt `JdbcTemplateAutoConfiguration` ở `org.springframework.boot.jdbc.autoconfigure` (module
`spring-boot-jdbc`) nên phải dùng `afterName` (string) — dùng `after = …class` sẽ buộc commons kéo thêm
dependency chỉ để tham chiếu một cái tên.

**Learning note**

- **`@ConditionalOnBean` là điều kiện *theo thời điểm*, không phải "bean này sẽ có"**: auto-config chạy theo
  thứ tự, và điều kiện chỉ thấy những gì đã đăng ký trước đó. Không khai `after`/`afterName` ⇒ điều kiện
  âm thầm `false`, bean không được tạo, **không có lỗi nào để lần theo**. Đây là họ bug giống `SL-3.1`
  (`RabbitAdmin` khai lazy): code đúng, hệ không làm gì, log sạch.
- **Cách phát hiện**: kiểm bằng **trạng thái của hệ ngoài** (bảng trong DB, exchange trên broker), không kiểm
  bằng "service healthy". Cả 2 lần trong phase 3 bug đều nằm ở khoảng giữa "app khởi động xong" và "hệ có
  thứ mình cần".
- **Marker dedupe phải xoá khi xử lý fail**: nếu chỉ `insert` trước khi handler chạy thì DLQ replay sẽ bị
  chính marker đó chặn ⇒ message nằm trong DLQ mà **không bao giờ** chạy lại được. Thiết kế đúng: mark →
  handle → (fail) unmark → `nack(requeue=false)`; test `shouldNackAfterRetriesExhausted` chốt điều này.
- **`AcknowledgeMode.MANUAL` là điều kiện của DLQ**: auto-ack nghĩa là message bị coi "xong" ngay khi
  listener trả về, nên không còn cơ hội `nack` khi handler fail ⇒ mất message trong im lặng. Test kiểm
  **container thật** (`createListenerContainer`) chứ không chỉ "bean tồn tại".
- **Thứ tự cũng là hợp đồng**: `markProcessed` phải nguyên tử (`insert … on conflict do nothing` trả
  `rowsAffected`) — kiểu "select rồi insert" vẫn có khe hở khi 2 lần redeliver tới cùng lúc.


### SL-3.3 — `order-service`: transactional outbox + relay ⇒ `order.confirmed.v1`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` (envelope/publisher); chặn `SL-3.4`/`SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

Đây là bài toán gốc của outbox: `order` chuyển `CONFIRMED` (commit DB) rồi mới publish — nếu process chết **giữa** hai bước
thì đơn đã confirmed mà **không ai biết** để gửi email. Ngược lại publish trước rồi ghi DB thì có thể phát event cho đơn
không tồn tại. Phase 2 giải quyết bằng cách **không** phát event; Phase 3 phải phát mà **không mất, không bịa**.

**Mục tiêu**

Mọi thay đổi state quan trọng của đơn ghi **outbox trong cùng transaction**, relay lo việc publish → `order.confirmed.v1` /
`order.cancelled.v1` tới được consumer **ít nhất một lần**.

**In scope**

- Migration `V4__create_outbox.sql`: `outbox(id, aggregate_type, aggregate_id, event_type, payload jsonb, status
  PENDING/SENT, attempts, last_error, created_at, sent_at)` + index `(status, created_at)` + CHECK status.
- Ghi outbox **trong cùng `@Transactional`** ở `CheckoutFinishService` (confirm) và luồng cancel.
- `OutboxRelay` (`@Scheduled`, env `SHOPLAB_OUTBOX_INTERVAL_MS`, `…BATCH_SIZE`): `FOR UPDATE SKIP LOCKED` → publish +
  **chờ confirm** → `SENT`; lỗi ⇒ `attempts++`, `last_error`, **giữ `PENDING`** (không mất event).
- Payload `order.confirmed.v1`: `{orderId, userId, total, currency, itemCount, confirmedAt}` (đủ để consumer hiểu, **không**
  copy toàn bộ đơn — payload lớn là nợ của tương lai).
- Test: unit (relay gửi + đánh SENT; lỗi ⇒ giữ PENDING, attempts tăng) · IT (confirm ⇒ đúng **1** row outbox; broker chết
  ⇒ order vẫn `CONFIRMED`, row `PENDING`; broker sống lại ⇒ `SENT`) · IT hai relay chạy song song ⇒ không gửi trùng.

**Out of scope**

- Không làm notification (`SL-3.4`), không làm event của payment (`SL-3.5`), không dọn row `SENT` (job retention ⇒ ghi `TD`).
- Không đổi payload/state machine của order (Phase 2 đã chốt).

**Acceptance Criteria**

- [x] `POST /v1/checkout/{id}/confirm` thành công ⇒ **1 row** `outbox` `PENDING` cùng transaction, sau ≤ 2 s thành `SENT`
      và có message `order.confirmed.v1` trên `q.notification` (kiểm bằng `tools/rabbit-topology.sh` / psql).
- [x] `docker stop shoplab-rabbitmq` rồi confirm ⇒ order **vẫn `CONFIRMED`** (không 503), row giữ `PENDING`,
      `attempts > 0`; `docker start` ⇒ message được gửi, row `SENT`.
- [x] Cancel ⇒ `order.cancelled.v1` phát ra tương tự; confirm#2 (idempotent) ⇒ **không** sinh row outbox thứ hai.
- [x] Chạy 2 instance relay (hoặc 2 vòng song song trong IT) ⇒ mỗi row chỉ publish **1 lần**, không message trùng.
- [x] `mvn -B -ntp verify` xanh; `e2e-checkout` 27/27 + `e2e-checkout-fail` 41/41 không regress.

**Ghi chú kỹ thuật**

- **Outbox là bảng, không phải queue**: publish phải là *hệ quả* của transaction đã commit, không phải một phần của nó.
- **Giữ `PENDING` khi lỗi** (không `FAILED`): event chưa gửi thì vẫn còn nghĩa vụ gửi; `attempts` + `last_error` đủ để biết
  cái nào đang kẹt.
- `FOR UPDATE SKIP LOCKED` (đúng khuôn `SL-2.3`): nhiều instance relay không giẫm nhau, không cần leader election.

**Kết quả verify** *(chạy thật 2026-11-08 — CI đang tắt theo `SL-3.10`, verify local thay CI)*

**1) `mvn -B -ntp verify` (toàn repo): BUILD SUCCESS · 687 test (566 unit + 121 IT) · 0 fail**
`order-service`: **70 unit** (+5 `OutboxRelayTest`) + **28 IT** (+3 outbox).

**2) Stack thật — confirm ⇒ outbox `PENDING` → `SENT` → message trên `q.notification`:**

```console
### 1) checkout + thanh toán + confirm
order=a5c51d63-…   confirm status=200
### 2) outbox NGAY sau confirm
PENDING|0                         ← ghi cùng transaction, relay chưa chạy
### 3) sau ~3 s (relay 1 s/nhịp)
SENT|1|t                          ← status=SENT, attempts=1, có sent_at
### 4) order CONFIRMED + payload
{"total": 100000.00, "userId": "fa72fd12-…", "orderId": "a5c51d63-…",
 "currency": "VND", "itemCount": 1, "confirmedAt": "2026-11-08T11:56:33…"}
### 5) message tới queue
  q.notification         ready= 14 unacked=  0 consumers=0
```

**3) Broker chết ⇒ order VẪN CONFIRMED, event không mất:**

```console
$ docker stop shoplab-rabbitmq
$ bash … confirm            → confirm status=200        ← KHÔNG 503 (publish không nằm trên đường commit)
PENDING|3|f                 ← vẫn PENDING, attempts tăng mỗi nhịp
$ (sau 12 s) orders.status=CONFIRMED · outbox.status=PENDING · attempts=11
$ docker start shoplab-rabbitmq   → healthy
SENT|21                     ← tự gửi lại thành công, KHÔNG mất event
```

**4) Cancel ⇒ `order.cancelled.v1`:**

```console
$ POST /v1/checkout/{id}/cancel → 200
$ psql: order.cancelled.v1|SENT
```

**5) Không regress:** `e2e-smoke` 17/17 · `e2e-checkout` 27/27 · `e2e-checkout-fail` 41/41.

**Sự cố trong lúc verify (2 bug thật — cả 2 đều bị outbox "che" an toàn)**

1. **Publisher confirm chưa bật** ⇒ `CorrelationData.getFuture()` **không bao giờ** complete ⇒ mỗi lần publish treo
   đúng 5 s (confirm timeout) rồi ném lỗi. Triệu chứng: outbox row `PENDING|1` mãi, log
   `không nhận được publisher confirm … sau 5000 ms`. Fix: `spring.rabbitmq.publisher-confirm-type: correlated`
   (+ `publisher-returns: true`) trong `order-service`. Đây là cấu hình **bắt buộc** của ADR-0007 D4 mà tôi đã
   quên — chính outbox giúp bug này **không mất event**, chỉ làm chậm 21 nhịp.
2. **`JdbcOutboxRepository` đòi bean `ObjectMapper`** ⇒ `order-service` IT đỏ **28/28** (`No qualifying bean of
   type ObjectMapper`). Fix: tự tạo mapper trong repository (như `EventEnvelopeCodec`) — dịch vụ này không phải
   tầng serialization web nên đừng phụ thuộc bean đó.

**Learning note**

- **`eventId` phải ổn định qua các lần thử** — chi tiết quyết định cả pattern: nếu relay sinh `eventId` mới mỗi lần
  publish lại thì `processed_events` của consumer **không chặn được** gì (mỗi lần là event "mới") ⇒ gửi email 2 lần.
  Dùng `outbox.id` làm `eventId` ⇒ mọi lần retry mang cùng id, dedupe có tác dụng đúng như ADR-0007 D3.
- **Claim ≠ publish trong cùng transaction**: claim là **transaction ngắn** (UPDATE … `SKIP LOCKED`) rồi mới publish
  ngoài transaction. Nếu publish trong transaction thì broker chậm (hoặc confirm 5 s như bug ở trên) sẽ giữ row lock
  của Postgres — biến sự cố broker thành sự cố DB. Thêm `claimed_at` (visibility timeout) để 2 relay không gửi trùng
  **chắc chắn**, không dựa vào việc 2 vòng quét lệch nhau.
- **Outbox biến "mất event" thành "trễ event"**: bug confirm ở trên làm mọi lần publish fail trong ~2 phút, nhưng
  không message nào mất và không đơn nào bị ảnh hưởng. Đây đúng là thứ outbox mua được — và cũng là lý do **không**
  được coi `attempts` cao là "chuyện nhỏ": nó là tín hiệu cấu hình sai.
- **`@Transactional` phải đi qua proxy**: tách `CheckoutOutbox` thành bean riêng vì gọi method `@Transactional` từ
  method khác **cùng class** thì không có transaction nào được mở — outbox sẽ ghi ngoài transaction và mất toàn bộ
  ý nghĩa (bug loại này không lộ ra ở happy path).
- **Thanh toán/HTTP nằm ngoài transaction**: transaction chỉ chứa 3 câu SQL local (order + saga + outbox). Giữ
  transaction qua lời gọi mạng là tự bắn vào chân — một dependency chậm sẽ khoá row hàng giây.


### SL-3.4 — `notification-service` (8089): consumer `order.confirmed` + retry + DLQ

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` (consumer base) · `SL-3.3` (event thật để consume); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

Đây là **vertical slice đầu tiên** của phase 3: một event đi từ outbox của `order` tới một service hoàn toàn mới. Nó là
chỗ để chứng minh "async thật sự chạy" — và cũng là chỗ để chứng minh **retry + DLQ** hoạt động, thứ mà nếu không test
thì chỉ là cấu hình.

**Mục tiêu**

`order.confirmed` ⇒ một email (mock) được "gửi" **async**, idempotent, lỗi thì retry rồi vào DLQ — **không** chặn queue.

**In scope**

- Service mới: `notification-service` (port **8089**), DB **`notification`** riêng, Flyway (`templates`, `notification_logs`,
  `processed_events`), Dockerfile + compose + CI matrix + gateway route `/v1/notifications`, OpenAPI tự sinh.
- Consumer `q.notification` bind `order.confirmed.v1` + `order.cancelled.v1` → render template → ghi `notification_logs`
  (`SENT`, `sent_at`) → log "đã gửi" (mock provider, không SMTP).
- Idempotent theo `eventId`; handler lỗi cố ý qua env `SHOPLAB_NOTIFICATION_FAIL_MODE` (chỉ local) để test retry/DLQ.
- `tools/dlq.sh`: liệt kê message trong `<queue>.dlq` và **move** 1 message về queue chính (replay có kiểm soát).
- `GET /v1/notifications/me` (buyer) — xem log của chính mình (verify được bằng mắt, không cần psql).

**Out of scope**

- Không SMTP/SMS thật, không template engine (placeholder `{{name}}` là đủ), không notification cho event khác.
- Không retry vô hạn, không dead-letter **process** riêng (replay bằng tay qua `tools/dlq.sh`).

**Acceptance Criteria**

- [x] Confirm đơn ⇒ trong ≤ **2 s** có row `notification_logs` `SENT` + `GET /v1/notifications/me` thấy nó; log có `eventId`.
- [x] Publish lại **cùng `eventId`** ⇒ vẫn **1** row (dedupe), không gửi lần 2.
- [x] Bật `SHOPLAB_NOTIFICATION_FAIL_MODE=true` ⇒ retry **3 lần** rồi message sang `q.notification.dlq`, queue chính
      `ready = 0`, `unacked = 0` (không treo).
- [x] `bash tools/dlq.sh move q.notification` (sau khi tắt fail-mode) ⇒ message được xử lý, `notification_logs` +1, DLQ rỗng.
- [x] Checkout vẫn xanh khi `notification-service` **chết** (async đúng nghĩa): confirm **200**, không 503.

**Ghi chú kỹ thuật**

- **Idempotent trước, retry sau**: nếu dedupe sai thì retry sẽ thành gửi email trùng — dedupe là điều kiện của retry.
- Env bơm lỗi **chỉ local** (`@ConditionalOnProperty(app.env=local)`) — bài học `SL-2.9`: hook debug không được tồn tại
  ở production.
- Port 8089 (8085 đã là Adminer).

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng dưới đây là `mvn verify` + e2e **chạy local**, dán nguyên output.

**1. Test tự động** — `mvn -B -ntp verify` (toàn repo, **12 module**):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 730, Failures: 0, Errors: 0   (600 unit + 130 IT)
  · riêng notification-service: 34 unit + 9 IT
  · NotificationServiceApplicationIT (5): Flyway tạo 3 bảng + seed 2 template · event_id UNIQUE chặn ghi trùng ·
    me không token → 401 · me chỉ trả log của chính mình (mới nhất trước) · ?limit bị chặn bởi trần cấu hình
  · NotificationConsumerIT (4): send + ack · publish lại cùng eventId → 1 row · fail-mode → nack(requeue=false) +
    xoá marker dedupe · fail-mode bị bỏ qua khi env != local
  · TemplateRendererTest (5) + NotificationServiceTest (7)
```

**2. Async end-to-end (stack thật)** — publish `order.confirmed.v1` (envelope thật) rồi poll DB:

```
publish: {"routed":true}
row xuất hiện sau 239 ms          <-- AC "≤ 2 s"
SENT|order.confirmed|Đơn hàng 4fe873b0-…-a52c6effed47 đã được xác nhận
```

`GET /v1/notifications/me` qua gateway (buyer token):

```
status=200
[{"id":"f8f27889-…","eventId":"50681af7-…","orderId":"b5a0d2ff-…","channel":"EMAIL",
  "template":"order.confirmed","recipient":"user-abb11f6d@shoplab.local",
  "subject":"Đơn hàng b5a0d2ff-… đã được xác nhận", …}]
```

**3. Dedupe** — publish **cùng `eventId`** lần 2: `rows = 1`, log consumer `đã xử lý trước đó`.

**4. Retry → DLQ** (`SHOPLAB_NOTIFICATION_FAIL_MODE=true`, restart service):

```
q.notification        ready=  0 unacked=  1 consumers=1     <-- đang retry
   … sau ~4 s (200ms + 1s + 3s) …
q.notification        ready=  0 unacked=  0 consumers=1     <-- KHÔNG treo
q.notification.dlq    ready=  1
docker logs … | grep -c 'lỗi xử lý|→ DLQ'  →  4             <-- 3 lần retry + 1 lần sang DLQ
```

**5. Replay có kiểm soát** — tắt fail-mode rồi `bash tools/dlq.sh move q.notification`:

```
  replay 1 message từ q.notification.dlq → shoplab.events (routing key GỐC = order.confirmed.v1) → {"routed":true}
rows cho eventId đó: 1        <-- message được xử lý lại, notification_logs +1
q.notification.dlq   ready=  0
```

**6. Decoupling** — `docker stop shoplab-notification-service` rồi confirm 1 đơn mới:

```
notification ĐANG TẮT → confirm: 200        <-- AC: không 503
bật lại → notification_logs có thêm: 1     <-- message chờ trong queue được xử lý khi consumer sống lại
```

**7. Regression** — e2e local (3 script):

```
== Kết quả: 17 PASS / 0 FAIL · 13 request · 1145 ms     (e2e-smoke)
== Kết quả: 27 PASS / 0 FAIL · 14 request · 3854 ms     (e2e-checkout)
== Kết quả: 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **Replay không phải "publish lại payload"** — bẫy này làm **mất message thật** ngay trong ticket này. Trong DLQ,
  `routing_key` của message là **tên queue** (`q.notification`) vì DLX đặt routing key = tên queue (ADR-0007 D2).
  Bản đầu của `tools/dlq.sh move` publish lại bằng `routing_key` hiện tại ⇒ `routed=false` ⇒ message bị **ack khỏi
  DLQ rồi rơi vào hư không**, không một lỗi nào. Routing key gốc nằm ở header `x-death[0].routing-keys`. Bài học
  kép: (1) đọc `x-death` khi replay; (2) **peek trước, consume sau** — validate xong mới `ack`, và script giờ exit
  **không consume** khi không parse được.
- **Quoting bash giết message**: bản `move` đầu tiên parse bằng `python3 -c "…nhiều dòng…"` trong `"$( … )"` — parse
  trả rỗng, mà message đã bị `get` mất rồi mới phát hiện. Chuyển sang **file python tạm + heredoc quoted** (không
  expansion) là hết hẳn lớp lỗi này. Với script vận hành, "parse lỗi" phải là **no-op**, không được là "mất dữ liệu".
- **`spring.rabbitmq.listener.simple.auto-startup` không áp cho factory tự khai**: service dùng
  `eventListenerContainerFactory` riêng (cần `AcknowledgeMode.MANUAL`) nên property của Boot **không** chạm tới ⇒ IT
  (không có broker) chết với `FatalListenerStartupException: Authentication failure`, **đỏ cả 17 test** dù logic
  đúng. Phải thêm cờ riêng của commons (`shoplab.events.consumer-auto-startup`). Tổng quát: **property của Boot chỉ
  có tác dụng với bean của Boot** — tự khai bean thì phải tự khai công tắc.
- **Dedupe là điều kiện của retry**, không phải tính năng phụ: retry gửi lại đúng message đó, nên marker
  `processed_events` **phải bị xoá khi handler fail** — xoá sai thứ tự thì retry thành "đã xử lý" (mất thông báo),
  không xoá thì retry vô nghĩa. `notification_logs.event_id UNIQUE` là chốt thứ hai ở tầng DB.
- **Async giải quyết "không chặn" nhưng đẻ ra "im lặng"**: khi service chết, confirm vẫn 200 (đúng), message **chờ
  trong queue** rồi được xử lý lúc consumer sống lại — nhưng nếu consumer chết **mãi** thì không ai biết thông báo
  đã không gửi. Đây là lý do `SL-3.8`/`SL-3.9` cần metric **queue depth + consumer down**, không chỉ log.
- 16 unit + 9 IT cho service mới, nhưng **0 dòng** retry/dedupe/DLQ trong code service — tất cả nằm ở
  `AbstractEventConsumer`. Đó là giá trị thật của `SL-3.2`: service thứ 2 trở đi chỉ viết ~60 dòng.

### SL-3.5 — `payment.succeeded` ⇒ `order-service` auto-confirm (trả `TD-17`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-09) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.2` · `SL-3.3` (khuôn outbox) · `SL-3.4` (khuôn consumer) · **`SL-3.11`** (token để gọi `commit` không cần user); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

`TD-17` (**mức `cao`**) — hệ quả của quyết định ở `SL-2.7`: webhook `payment.succeeded` **không** cập nhật order (Phase 2 chưa
có broker, chỉ saga giữ token user mới confirm được). Ca thật: khách trả tiền xong, webhook tới, nhưng **không** gọi
`/confirm` (đóng tab, mất mạng) ⇒ tiền đã thu mà đơn treo `PENDING_PAYMENT` đến khi sweeper `EXPIRED` ⇒ **hàng bị trả lại
dù tiền đã vào**. Phase 3 là lúc trả món nợ này đúng như điều kiện revisit đã ghi.

**Mục tiêu**

`payment-service` phát `payment.succeeded.v1` **từ outbox**; `order-service` consume và **tự confirm** đơn ⇒ hai đường
(webhook → event, và client gọi `/confirm`) đều dẫn tới **một** trạng thái, idempotent, không commit 2 lần.

**In scope**

- `payment-service`: migration outbox (`V?__create_outbox.sql`), ghi outbox **trong cùng transaction** khi webhook chuyển
  intent sang `SUCCEEDED`; relay riêng (cùng khuôn `SL-3.3`); payload `{paymentIntentId, orderId, amount, currency, succeededAt}`.
- `order-service`: consumer `q.order` bind `payment.succeeded.v1` → nếu saga `COMPLETED_START` + order `PENDING_PAYMENT` ⇒
  dùng lại `CheckoutFinishService` để commit stock + voucher ⇒ `CONFIRMED` (step audit `PAYMENT_EVENT_CONFIRM`).
- Ca không confirm được (order đã `EXPIRED`/`CANCELLED`): **ack + log WARN** có `paymentIntentId` (không ném lại vào DLQ
  vô ích) và ghi `TD` phần **refund** (chưa có `refunds`/`transactions`).
- Giữ `POST /v1/checkout/{id}/confirm` (client chủ động) — không đổi API surface.

**Out of scope**

- Không làm refund/hoàn tiền (Phase 6). Không bỏ endpoint `confirm`. Không đổi luồng saga start.

**Acceptance Criteria**

- [x] Mock-pay **không** gọi `/confirm` ⇒ ≤ **2 s** sau: order `CONFIRMED`, `saga = CONFIRMED`, `stock.on_hand` giảm đúng,
      voucher `COMMITTED`, `checkout_saga_steps` có `PAYMENT_EVENT_CONFIRM:OK`.
- [x] Gọi `/confirm` **sau** khi event đã confirm ⇒ **200** (idempotent), `stock`/voucher **không** bị trừ 2 lần.
- [x] Webhook lặp (cùng `provider_event_id`) ⇒ **không** sinh message `payment.succeeded.v2`, outbox vẫn 1 row.
- [x] `payment.succeeded` cho đơn đã `EXPIRED` ⇒ `ack`, log WARN, state **không** đổi, DLQ rỗng.
- [x] Broker chết lúc webhook ⇒ payment `SUCCEEDED` vẫn ghi DB + outbox `PENDING`; broker sống lại ⇒ event đi, order tự confirm.
- [x] `TD-17` → `✅ đã trả (SL-3.5)` (phần refund tách `TD` mới); `e2e-checkout`/`e2e-checkout-fail` không regress.

**Ghi chú kỹ thuật**

- **Hai đường confirm** là chấp nhận có chủ ý (client chủ động + hệ tự chốt) ⇒ **mọi** nhánh phải đi qua cùng một hàm
  compare-and-set trên `orders` (đã có từ `SL-2.6b`), nếu không sẽ commit 2 lần.
- Event này là **hệ quả của webhook**, không phải thay thế webhook: idempotency của webhook (`provider_event_id`) vẫn là
  chốt chặn thứ nhất; dedupe consumer (`eventId`) là chốt thứ hai.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng là `mvn verify` + e2e **local** + kịch bản chạy trên stack thật.

**1. Test tự động** — `mvn -B -ntp verify` (12 module):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 756, Failures: 0, Errors: 0   (624 unit + 132 IT)   <-- +10 so với baseline SL-3.11 (746)
  · order:   CheckoutFinishServiceEventTest (6 nhánh quyết định) · expire re-test (3: release bằng token ·
             release lỗi vẫn expire · đơn không còn PENDING_PAYMENT thì không release)
  · payment: PaymentWebhookServiceTest (+3: ghi outbox 1 lần · webhook lặp không ghi · FAILED không ghi)
```

**2. AC1 — mock-pay, KHÔNG gọi `/confirm`**

```
mock-pay → order CONFIRMED sau 1031 ms   (đơn thường)  ·  909 ms (đơn có voucher 10%)
saga = CONFIRMED
steps: STOCK_COMMIT:OK · VOUCHER_COMMIT:OK · PAYMENT_VERIFY:SKIPPED · PAYMENT_EVENT_CONFIRM:OK
stock: 5 → 3 available (mua 2)          voucher: used=1 locked=0 (COMMITTED)
outbox(payment): payment.succeeded.v1|SENT|attempts=1
```

**3. AC2 — `/confirm` sau khi event đã chốt**

```
confirm status = 200 · state = CONFIRMED   ·   stock vẫn 3 (KHÔNG trừ 2 lần)   ·   history 2 dòng (không thêm)
```

**4. AC3 — webhook lặp (cùng `provider_event_id`)**

```
lần 1: {"duplicate":false,"applied":false}      (intent đã SUCCEEDED ⇒ no-op, không ghi gì)
lần 2: {"duplicate":true,"applied":false}
outbox rows cho intent này = 1   ·   không có message `payment.succeeded.v2`
```

**5. AC4 — event cho đơn đã `EXPIRED`**

```
order status = EXPIRED (không đổi) · saga = EXPIRED · payment = SUCCEEDED · outbox = SENT
q.order.dlq ready = 0
log: "đơn 5ef18dca… ở saga EXPIRED — không chốt theo event … (cần xử lý tay)"
```

**6. AC5 — broker chết lúc webhook**

```
broker TẮT:  payment = SUCCEEDED · outbox = PENDING|attempts=3 · order = PENDING_PAYMENT
broker BẬT:  order CONFIRMED sau 7.8 s · outbox = SENT|attempts=11 · saga = CONFIRMED · stock 3 → 2
```

**7. `TD-25` — sweeper release ngay (không chờ TTL)**

```
checkout (payment-window 20 s): reserved = 2
order EXPIRED sau ~22 s  ⇒  HÀNG VỀ KHO sau ~0 s (reserved = 0)   <-- trước đây phải chờ TTL 15 phút
steps: STOCK_RELEASE:OK · EXPIRE:OK
```

**8. Regression e2e**

```
== 17 PASS / 0 FAIL · 13 request · 683 ms      (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 1059 ms     (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **"Hai đường confirm" không phải vấn đề idempotency, mà là vấn đề *thứ tự kiểm tra***. Chốt đơn theo event phải loại
  dần 5 khả năng trước khi chạm downstream: không có saga · saga đã `CONFIRMED` (bình thường, hai đường gặp nhau) · saga
  chưa `COMPLETED_START` · `paymentIntentId` lệch · đơn không còn `PENDING_PAYMENT`. Bỏ bất kỳ bước nào thì hệ **chốt
  nhầm đơn** — loại lỗi không sửa được bằng code (tiền đã thu, hàng đã giao). Vì thế test là **ma trận 6 nhánh**, không
  phải "một happy path + một lỗi".
- **Bằng chứng thanh toán đổi chỗ, không mất đi**: đường client gọi `/confirm` phải *hỏi* payment (`verifyPayment`);
  đường event **không hỏi lại** vì event được phát ra ngay sau CAS `SUCCEEDED` của chính payment. Nếu vẫn gọi lại thì
  vừa thừa (round-trip), vừa không làm được (không có token user) — và nếu cố tình dùng service token để gọi thì lại
  biến một luồng *đã có bằng chứng* thành phụ thuộc runtime vào payment. Ghi `PAYMENT_VERIFY:SKIPPED` vào audit để
  người đọc sau không tưởng là bước đó bị bỏ quên.
- **Payload event phải có "khoá đối chiếu"**: `paymentIntentId` trong payload không phải để trang trí — nó là thứ duy
  nhất phân biệt "event của đơn này" với "event bị route/replay sai". `orderId` một mình là chưa đủ.
- **Ack cho kết cục nghiệp vụ, retry cho lỗi hạ tầng**: đơn `EXPIRED` mà tiền đã thu là chuyện phải **hoàn tiền tay**
  (ack + log ERROR kèm `eventId`/`paymentIntentId`), còn inventory 503 là chuyện retry. Trộn hai loại này làm DLQ đầy
  message "không retry được" và che mất sự cố thật.
- **Sweeper release là chỗ dễ tạo bug bán quá tồn kho**: release *trước* khi CAS sang `EXPIRED` (đúng yêu cầu `TD-25`:
  hàng về ngay) mở một cửa sổ rất hẹp — nếu một confirm chen vào giữa thì hàng đã trả cho đơn vẫn được bán. Chặn bằng
  cách kiểm `orders.status = PENDING_PAYMENT` **trước** khi release, và log WARN nếu CAS sau đó trả `false` (nghĩa là
  release đã chạy nhưng đơn không expire được ⇒ phải kiểm tay). Cửa sổ không về 0, nhưng nó **nhìn thấy được**.
- **Bài học cũ lặp lại y nguyên**: `@ConfigurationProperties` inject một bean không tồn tại (`ObjectMapper` của Jackson 2)
  làm **context fail** — lần này ở IT của order-service. Cùng họ với `SL-3.3` (`JdbcOutboxRepository` tự tạo mapper).
  Với hạ tầng dùng chung trong `service-commons`, **không được giả định bean nào có mặt**: hoặc tự tạo, hoặc
  `@ConditionalOnBean` + fail mềm.
- **Copy khuôn outbox lần 2 là quyết định có ý thức, không phải lười**: 2 service cần đúng 500 dòng giống nhau. Nhưng
  lần thứ 3 (`SL-3.6` catalog) là lúc phải extract vào commons — đã ghi `TD-32` với điều kiện revisit rõ ràng, để "lần
  sau" không trôi thành "không bao giờ".

### SL-3.11 — Service-to-service auth: `client_credentials` + `aud`/`exp` (tách từ `SL-3.5`)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-08) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-1.4` (JWT/JWKS của identity) · `SL-3.2` (commons); chặn `SL-3.5` · trả một phần `TD-25` |
| **Ước lượng** | M (tách ra từ `SL-3.5` vì `SL-3.5` + phần auth = `L` — `AGENTS.md` §7) |

**Bối cảnh**

`SL-3.5` cần `order-service` gọi `inventory`/`promotion` `commit` **từ consumer** — nhưng consumer không có token
người dùng (token 15 phút, khách đã đóng tab). Cùng vấn đề với `checkout` sweeper (`TD-25`: không có token ⇒ không
gọi được `release`, phải nhờ TTL của service khác). Hệ hiện **chỉ** có 1 loại caller: user cuối. Ba lựa chọn đã
được so sánh trong phiên plan: internal token dùng chung (A) · endpoint internal riêng (B) · **service token thật
qua identity, `client_credentials` + `aud`/`exp` (C)** → chốt **C** vì repo là learning lab, ưu tiên hiểu đúng
(`AGENTS.md` §1): token có hạn, có audience, thu hồi/rotate được, và log biết **service nào** đang gọi.

**Mục tiêu**

Service (không có user) xin được **token ngắn hạn** từ `identity-service`, mang `role=SERVICE` + `aud=<service
đích>`, và mỗi service đích **từ chối** token phát cho service khác — mà **không** đổi hành vi của token user hiện
tại.

**In scope**

- `identity-service`: `POST /v1/auth/service-token` (`clientId` + `clientSecret` + `audience`) → JWT RS256
  `role=SERVICE`, `aud=[audience]`, `exp=5m`, `sub=svc:<clientId>`; registry client theo cấu hình
  (`shoplab.service-auth.clients.<id>.secret` + `.allowed-audiences`), secret so sánh **hằng thời gian**.
- `service-commons`: `ServiceTokenClient` (cache token tới `exp - skew`, không xin lại mỗi request) +
  `ServiceTokenRules.validator(issuer, serviceName)` (kiểm `aud` **chỉ khi** `role=SERVICE`).
- `inventory-service` + `promotion-service`: dùng validator đó ⇒ token của `order-service` gọi `inventory` bị
  `promotion` từ chối và ngược lại.
- `ADR-0008` (quyết định + trade-off + điều kiện revisit) + `architecture.md` §bảo mật.
- Env/compose cho client secret (12-factor, không hard-code).

**Out of scope**

- Chưa nối `order-service` dùng token này (việc của `SL-3.5`), chưa cho sweeper gọi `release` (chỉ đổi khi
  `SL-3.5` xong — `TD-25` đóng ở đó).
- Không dùng Spring Authorization Server, không mTLS (Phase 7), không refresh token cho service, không UI quản lý
  client.

**Acceptance Criteria**

- [x] `POST /v1/auth/service-token` đúng client + audience ⇒ **200**, token decode được: `role=SERVICE`,
      `aud=[audience]`, `exp-iat = 5m`, `iss` = issuer của hệ.
- [x] Sai secret / client lạ ⇒ **401**; `audience` không nằm trong `allowed-audiences` của client ⇒ **400**; thiếu
      field ⇒ **400**.
- [x] Gọi `inventory-service` bằng token `aud=inventory-service` ⇒ **404** (auth qua, chỉ thiếu resource — chứng minh
      token được chấp nhận); bằng token `aud=promotion-service` ⇒ **401 `invalid_token`** (chuẩn OAuth2: token không hợp
      lệ cho resource này là *invalid token*, không phải 403); token user thường vẫn hoạt động như cũ (không hồi quy).
- [x] `ServiceTokenClient` cache: 2 lần gọi liên tiếp chỉ **1** request tới identity; token gần hết hạn ⇒ xin mới.
- [x] `mvn -B -ntp verify` xanh toàn repo + e2e local 3 script không hồi quy.

**Ghi chú kỹ thuật**

- `aud` chỉ kiểm **khi** `role=SERVICE` — token user hiện chưa có `aud`; thêm `aud` cho token user là thay đổi hợp
  đồng token với 8 service ⇒ tách ticket riêng khi cần.
- Registry client để **trong config** (không thêm bảng DB): Phase 3 chỉ có 1–2 client; bảng + API quản lý client là
  hạ tầng của Phase 6/7 (`AGENTS.md` §3.4).
- `sub=svc:<clientId>` (không phải UUID) ⇒ service nào lỡ coi `sub` là userId sẽ **nổ ngay** chứ không âm thầm ghi
  dữ liệu sai.
- **Sai `aud` trả 401, không phải 403** (đo thật trên stack): Spring resource server map lỗi validator thành
  `invalid_token` ⇒ 401. Đúng chuẩn OAuth2 (token không hợp lệ cho resource này), và 403 để dành cho "đã xác thực nhưng
  thiếu quyền". Ghi lại vì AC ban đầu mình đoán 403 — đoán sai thì sửa AC, không uốn code cho khớp giấy.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng là `mvn verify` + e2e **chạy local** + curl trên stack thật.

**1. Test tự động** — `mvn -B -ntp verify` (12 module):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 746, Failures: 0, Errors: 0   (614 unit + 132 IT)   <-- +16 so với baseline SL-3.4 (730)
  · commons: ServiceTokenClientTest (5) + ServiceTokenRulesTest (5)
  · identity: ServiceAuthServiceTest (4) + IT +2 (claims của service token · reject sai secret/audience/thiếu field)
```

**2. Cấp token (stack thật)**

```
POST /v1/auth/service-token {order-service, <secret>, inventory-service}
tokenType= Bearer expiresIn= 300 audience= inventory-service
claims: role= SERVICE aud= inventory-service sub= svc:order-service iss= https://identity.shoplab.local
```

**3. Sai secret / audience ngoài allowlist**

```
sai secret            → status=401  {"code":"UNAUTHORIZED","message":"clientId hoặc clientSecret không đúng"}
aud=payment-service   → status=400  {"code":"VALIDATION_ERROR","message":"client order-service không được phép xin
                                    token cho audience 'payment-service' (cho phép: [inventory-service, promotion-service])"}
```

**4. Service đích kiểm `aud`**

```
inventory  + token aud=inventory-service   → 404   (qua auth, chỉ thiếu resource — token ĐƯỢC chấp nhận)
inventory  + token aud=promotion-service   → 401   {"code":"UNAUTHORIZED"}  (WWW-Authenticate: invalid_token)
promotion  + token aud=promotion-service   → 403   (qua auth, thiếu role ADMIN — token hợp lệ)
promotion  + token aud=inventory-service   → 401   (sai audience)
```

**5. Không hồi quy token người**

```
admin (token user) + inventory  → 404   ·   admin (token user) + promotion → 404      <-- vẫn qua auth như trước
```

**6. Regression e2e**

```
== 17 PASS / 0 FAIL · 13 request · 657 ms      (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 1343 ms     (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **"Biết ai đang gọi" mới là phần khó của service-to-service auth**, không phải "làm sao để gọi được". Bản A (shared
  secret) mất 30 phút là xong nhưng hệ quả là *mọi* service dùng chung một danh tính: log không phân biệt được ai gọi,
  và một chỗ rò rỉ = toàn bộ endpoint chỉ-cần-`authenticated()` bị mở. `aud` + `sub=svc:<name>` biến sự kiện "có
  request nội bộ" thành "**order-service** gọi **inventory** trong 5 phút tới" — đó mới là thứ debug được lúc 3h sáng.
- **`@ConfigurationProperties` record lồng record = sai đường dẫn bind, và triệu chứng rất dối**: mình bọc
  `record Clients(Map<String, Client> clients)` trong `ServiceAuthProperties` ⇒ path thật thành
  `clients.clients.<id>` ⇒ map **rỗng** ⇒ mọi request trả 401 "clientId hoặc clientSecret không đúng". Nhìn log thì
  tưởng sai secret, thực ra là bind sai. Map phải là **field trực tiếp** của record (hoặc dùng `@Name`). Bài học kèm:
  với map key có dấu `-`, đặt bằng property phẳng phải dùng `clients[order-service].secret`.
- **Kiểm `exp`/`aud` trong test phải dùng đồng hồ thật**: `JwtValidators.createDefaultWithIssuer` so `exp` với
  *now*, nên token dựng bằng mốc cố định (`Instant.parse("2026-11-08T14:00:00Z")`) **hết hạn ngay khi tạo** ⇒ 3 test đỏ
  mà thông báo chỉ nói "Jwt expired" — đọc dễ tưởng lỗi logic. Mốc thời gian trong test JWT phải là `Instant.now()`.
- **Chuẩn thắng kỳ vọng của mình**: mình viết AC là "sai audience ⇒ 403", đo thật thì Spring trả **401
  `invalid_token`** — và nó **đúng** (token không hợp lệ cho resource này; 403 dành cho "đã xác thực nhưng thiếu
  quyền"). Sửa AC theo thực tế, không uốn code cho khớp giấy. Đo được cả hai ca trên cùng endpoint `promotion`
  (`hasRole('ADMIN')`): token đúng `aud` ⇒ 403, token sai `aud` ⇒ 401.
- **Token không bao giờ được gửi kèm message**: nhét bearer vào payload event là biến broker thành kho token và biến
  mọi consumer thành "có quyền của user". Event chỉ mang `paymentIntentId`/`orderId`; consumer tự xin token của
  **chính nó**.
- **Fail-closed ở config rẻ hơn fail-closed ở code**: `secret: ${ORDER_SERVICE_CLIENT_SECRET}` **không có default** ⇒
  thiếu env là identity không khởi động. Đổi lại phải thêm biến vào `.env.example` + compose; nếu để default
  `dev-secret` thì mọi máy dev chạy được nhưng production có thể vô tình dùng secret ai cũng biết.

### SL-3.6 — `catalog` outbox + `search-service` (8091) Postgres FTS + reconcile

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-09) |
| **Phase** | 3 — Async & event-driven *(quyết định storage: Postgres FTS — xem ghi chú ở đầu phase)* |
| **Phụ thuộc** | `SL-3.2` (consumer/publisher) · `SL-3.3` (khuôn outbox); chặn `SL-3.7`/`SL-3.8` |
| **Ước lượng** | M |

**Bối cảnh**

`catalog` hiện là nguồn sự thật về product, nhưng tìm kiếm (`LIKE '%q%'`, `TD-7`) là full scan — không lên được khi dữ
liệu lớn. Bài học của phase 3 **không** phải "cài OpenSearch" mà là **index lệch thì phải phát hiện và sửa được**: event
cập nhật index + **reconcile job** khi lệch. Engine đổi ở Phase 8 mà contract event giữ nguyên.

**Mục tiêu**

Product đổi ⇒ index của `search-service` cập nhật ≤ 2 s; index lệch ⇒ **một lệnh** dựng lại được; tìm tiếng Việt có dấu /
không dấu đều ra.

**In scope**

- `catalog-service`: outbox + relay (cùng khuôn `SL-3.3`) phát `product.updated.v1` (created/updated gộp) và
  `product.archived.v1` khi soft delete (`SL-2.11`).
- `search-service` (port **8091** — `8090` thuộc `service-template` từ Phase 0), DB **`search`**: bảng `search_product(product_id PK, name, description, category_id,
  min_price, status, updated_at, search_vector tsvector)` + GIN index + extension `unaccent`; consumer `q.search` upsert/archive.
- `GET /v1/search?q=&categoryId=&minPrice=&maxPrice=&page=&size=` — chỉ trả product `ACTIVE`, body `ApiError` chuẩn, phân
  trang offset (ghi `TD` về keyset nếu cần, đồng bộ `TD-6`).
- **Reconcile**: `POST /v1/admin/search/reindex` (ADMIN) đọc catalog theo batch (`GET /v1/products?size=…&page=…`) và upsert;
  trả `{scanned, upserted, archived}`. Kèm `@Scheduled` so số row (log WARN khi lệch > 1%).
- Dockerfile + compose + CI matrix + gateway route `/v1/search` (public) + `/v1/admin/search/**` (ADMIN).

**Out of scope**

- **OpenSearch/Elasticsearch (Phase 8)** · relevance tuning, synonym, facet, highlight · search SKU (chỉ product).
- Không sửa `catalog` để phục vụ search (vẫn là nguồn sự thật, không biết gì về index).

**Acceptance Criteria**

- [ ] Tạo/sửa product ⇒ ≤ **2 s** `GET /v1/search?q=<tên>` thấy product với `minPrice` đúng.
- [ ] Soft delete product ⇒ ≤ 2 s search **không** còn; row index chuyển `ARCHIVED` (không xoá cứng).
- [ ] `q=dien thoai` khớp `"Điện thoại"` (unaccent) và `q=dien%` không làm lỗi (escape wildcard).
- [ ] Xoá tay 5 row index rồi `POST /v1/admin/search/reindex` ⇒ `upserted ≥ 5`, search trả lại đủ; gọi 2 lần liên tiếp
      ⇒ **idempotent** (không nhân bản row).
- [ ] Broker chết ⇒ catalog **vẫn** tạo/sửa product bình thường (chỉ index trễ); reconcile vẫn dựng lại được index.
- [ ] `product.archived` phát từ **outbox** (không phải gọi thẳng search) — kiểm bằng `outbox` row + message.

**Ghi chú kỹ thuật**

- **Đổi engine không đổi contract** là mục tiêu của phase này: payload event + reconcile là thứ giữ cho việc "lên
  OpenSearch" ở Phase 8 thành việc **thay storage**, không phải viết lại tích hợp.
- `unaccent` + `tsvector` cho tiếng Việt: dùng `unaccent(simple)` thay vì `vietnamese` config (Postgres không có sẵn) và
  ghi rõ giới hạn (không xử lý từ đồng nghĩa) — trung thực về chất lượng search.
- Reconcile là **đường lùi bắt buộc** của mọi index: index sai mà không reconcile được thì không được coi là "event-driven xong".

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng = `mvn verify` + AC chạy trên **stack thật** + e2e local.

**1. Test tự động** — `mvn -B -ntp verify` (13 module):

```
[INFO] BUILD SUCCESS
TOTAL: Tests run: 805, Failures: 0, Errors: 0   (662 unit + 143 IT)  <-- +49 so với SL-3.5 (756)
  · catalog: +2 (ProductServiceTest 4 assertion outbox · IT: outbox row product.updated/archived, V4)
  · search:  38 unit (SearchQueryTest 8 · SearchProductTest 4 · IndexDriftCheckerTest 4 · config/health 22)
             + 10 IT (schema+GIN+unaccent · AC1 · AC2 · AC3 · lệch thứ tự · filter/paging · 400 ·
                      reindex dựng lại + idempotent · archive orphan · 401/403)
```

**2. AC1 — tạo product ⇒ search thấy ≤ 2 s**

```
POST /v1/products (seller) → 201, product 8054d3b5-…
GET  /v1/search?q=dien thoai <ts>  → thấy sau 228 ms   (trễ = relay 1 s + consumer)
giá trả về: 12990000.00 (khớp giá catalog)
```

**3. AC2 — soft delete ⇒ search không còn, row index `ARCHIVED`**

```
DELETE /v1/products/<id> → 204
GET /v1/search?q=dien thoai <ts> → không còn (poll ~0.3 s)
search_product.status = ARCHIVED            (row KHÔNG bị xoá — replay event cũ không hồi sinh)
outbox(catalog): product.archived.v1|SENT
```

**4. AC3 — unaccent + wildcard**

```
q=dien        → khớp "Điện thoại"          (unaccent hai phía, tsquery — không phải LIKE)
q=dien%       → 200, không lỗi
q=%%%         → 200 (không row nào)
```

**5. AC6 — event product đi qua outbox (không gọi thẳng search)**

```
outbox(catalog): product.updated.v1|SENT · product.archived.v1|SENT
q.search  ready=0  consumers=1   ·   q.search.dlq ready=0
```

**6. AC4 — reconcile**

```
POST /v1/admin/search/reindex  (không token) → 401   ·   (buyer) → 403   ·   (admin) → 200
lần đầu  : {"scanned":61,"upserted":61,"archived":0}     catalog=61 · index ACTIVE=61
xoá tay 7 row index → còn 54
reindex  : {"scanned":61,"upserted":61,"archived":0}   → index về 61 row (đủ)
lần 2    : {"scanned":61,"upserted":61,"archived":0}   → số row KHÔNG đổi, không product_id trùng
```

**7. AC5 — broker chết**

```
docker stop shoplab-rabbitmq
POST /v1/products → 201 (catalog vẫn chạy bình thường)
outbox: product.updated.v1|PENDING|attempts=3     (event nằm chờ, KHÔNG mất)
GET /v1/search?q=… → chưa thấy (đúng: event chưa đi)
POST /v1/admin/search/reindex → {"scanned":62,"upserted":62}  → search thấy product
docker start shoplab-rabbitmq → outbox SENT trong ~6 s (relay gửi bù)
```

**8. Job phát hiện lệch** (hạ nhịp xuống 5 s bằng override tạm, rồi trả về mặc định)

```
index khớp catalog: index=65 catalog=65 lệch=0.00%
xoá tay 7 row index →
INDEX LỆCH: index=58 catalog=65 lệch=10.77% (ngưỡng 1%) ⇒ chạy `POST /v1/admin/search/reindex`
```

**9. Regression e2e**

```
== 17 PASS / 0 FAIL · 13 request · 531 ms      (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 1250 ms     (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request      (e2e-checkout-fail)
```

**Learning note** *(điền khi Done)*

- **"Index lệch" không phải bug, là trạng thái bình thường** — cái phải có là **phát hiện + dựng lại**. Ba tính chất
  làm nên điều đó: (1) event ghi trong **cùng transaction** với thay đổi product (outbox), (2) consumer idempotent
  (`UPSERT` theo khoá tự nhiên), (3) reconcile chạy lại bao nhiêu lần cũng ra cùng kết quả. Thiếu (3) thì "index" chỉ
  là một bản copy không ai dám tin.
- **`unaccent()` không IMMUTABLE ⇒ không dùng được generated column.** Đây là chỗ dễ mất 1 giờ: Postgres từ chối
  `GENERATED ALWAYS AS (to_tsvector('simple', unaccent(name)))` mà thông báo lỗi không nói rõ nguyên nhân. Cách đúng:
  cột `search_vector` thật, ghi trong **chính câu UPSERT** ⇒ chỉ có MỘT chỗ tính vector. Nếu để 2 chỗ (app + trigger)
  thì sớm muộn cũng lệch — và lệch kiểu đó API vẫn 200, chỉ là không bao giờ tìm thấy gì.
- **`plainto_tsquery` thay `LIKE` là thứ xoá luôn cả một lớp bug**: `%`, `_`, `\` không còn là cú pháp, nên "khách gõ
  ký tự lạ" không thể thành wildcard hay lỗi SQL. Đổi lại phải nói thật về chất lượng: `simple` + unaccent **không**
  stem tiếng Việt, **không** biết từ đồng nghĩa — "tai nghe" không khớp "headphone". Trung thực trong docs tốt hơn
  quảng cáo "search thông minh".
- **Event đến lệch thứ tự là chuyện của at-least-once** ⇒ mọi UPSERT của index phải có **chốt thời gian**
  (`WHERE search_product.updated_at <= EXCLUDED.updated_at`). Không có nó, một event cũ đến muộn sẽ đưa giá/tên cũ
  trở lại index — và vì reconcile cũng dùng UPSERT nên nó sẽ **giữ nguyên** cái sai đó.
- **Reconcile cần watermark, không chỉ cần so danh sách**: chỉ archive row có `updated_at < mốc bắt đầu quét`. Bỏ
  watermark thì product được tạo **trong lúc** đang quét (chưa kịp vào kết quả) sẽ bị archive oan — bug kinh điển của
  mọi job "full sync" mà chỉ xuất hiện khi có tải thật.
- **Nhánh rỗng phải viết tay**: Spring mở rộng `NOT IN (:ids)` với collection rỗng thành `NOT IN (NULL)` ⇒ **không row
  nào** bị archive, trong khi ý nghĩa đúng là "catalog rỗng thì index phải rỗng". Nếu không nghĩ tới, `reindex` sẽ báo
  thành công mà bỏ lại rác — đúng loại lỗi chỉ phát hiện khi có người hỏi "sao search còn hàng đã xoá".
- **`upserted` là "số row được ghi", không phải "số row đổi"**: gọi reindex 2 lần vẫn thấy `upserted=61` (vì `<=` cho
  phép ghi lại cùng mốc thời gian — cần thế để replay idempotent). Muốn con số "thay đổi thật" thì phải thêm `<>`
  so sánh từng cột; ta chọn **không** làm, và ghi rõ ở đây để lần sau không ai đọc số đó thành "index vừa lệch 61 row".
- **Bẫy hạ tầng lặp lại lần thứ 3** (đúng như dự đoán ở `TD-32`): `RestClient.Builder` là bean **không có** trong
  context của service mới (Boot 4 tách auto-config) ⇒ inject là context fail. Chọn JDK `HttpClient` + Jackson tường
  minh: ít phụ thuộc hơn, và lỗi lộ ra ngay ở compile thay vì lúc chạy.
- **Đừng tin sort của service khác**: `reindex` gọi `GET /v1/products?sort=updatedAt,desc` → catalog trả **400** vì
  whitelist sort chỉ có `price`/`createdAt` (AC4 đỏ 4 test cho tới khi sửa). Bài học: gọi API nội bộ cũng phải đọc
  whitelist của nó, và **test end-to-end mới bắt được** — unit test + IT (catalog giả) đều xanh.
- **`TD-32` đóng bằng lý do, không bằng im lặng**: lần copy thứ 3 (~500 dòng) đổi lấy việc `SL-3.6` giữ đúng scope và
  diff review được. Điều kiện revisit ghi rõ: producer thứ 4 hoặc khi lên OpenSearch (Phase 8) — lúc đó hình dạng
  outbox có thể phải đổi, extract sớm sẽ phải làm lại.

### SL-3.7 — E2E async (`tools/e2e-async.sh`) + bước trong `e2e.yml`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-09) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.3`·`SL-3.4`·`SL-3.5`·`SL-3.6` (cần đủ 4 luồng để assert); chặn `SL-3.9` |
| **Ước lượng** | S |

**Bối cảnh**

Async không nhìn thấy được bằng một `curl` duy nhất: API trả `200` **trước khi** việc thật xảy ra. Test phải **chờ có điều
kiện** (poll tới timeout) và assert **state cuối** — nếu không sẽ có test "xanh" vì... chưa kịp chạy.

**Mục tiêu**

Một lệnh chạy được **4 luồng async** trên stack thật: event → email · event → auto-confirm · event → index search ·
message lỗi → DLQ → replay.

**In scope**

- `tools/e2e-async.sh` (bash + `python3`, cùng khuôn `e2e-checkout.sh`): các bước `A1…A12`:
  confirm ⇒ `notification_logs`; mock-pay **không** confirm ⇒ order tự `CONFIRMED`; sửa product ⇒ search thấy; archive ⇒
  search mất; bật fail-mode ⇒ DLQ có message; `tools/dlq.sh move` ⇒ xử lý lại; queue `ready = 0` sau khi sạch.
- Mỗi bước `PASS/FAIL` + gợi ý lệnh kiểm tra; exit code `1` khi có FAIL; **cleanup trap** reset fail-mode (bài học `SL-2.9`).
- `.github/workflows/e2e.yml`: thêm bước chạy script (sau 2 script hiện có); README mục "E2E async".

**Out of scope**

- Không thay thế 3 script cũ (smoke/checkout/fail vẫn là chuẩn riêng). Không đo hiệu năng (đó là `SL-3.8`).

**Acceptance Criteria**

- [ ] `bash tools/e2e-async.sh` ⇒ **≥ 12 PASS / 0 FAIL**, chạy **2 lần liên tiếp** đều xanh (không phụ thuộc state cũ).
- [ ] Mọi assert async đều **poll có timeout** (≤ 5 s) chứ không `sleep` mù; FAIL in `ready`/`unacked`/`dlq` tại thời điểm fail.
- [ ] Script tự dọn state (fail-mode off, DLQ rỗng) kể cả khi chết giữa đường (`trap`).
- [ ] `e2e.yml` có bước mới; YAML parse được (`python3 yaml.safe_load`).
- [ ] 3 script cũ vẫn xanh (không regress).

**Ghi chú kỹ thuật**

- **Poll, không sleep**: async + máy chậm ⇒ `sleep 1` là flaky; poll state với timeout là cách duy nhất ổn định.
- DLQ test **phải** dùng fail-mode có guard (local/E2E only) — không có đường nào để bơm lỗi ở production.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng = chạy script trên **stack thật** + `mvn verify` + 3 script cũ.

**1. `bash tools/e2e-async.sh` — 2 lần liên tiếp (AC: không phụ thuộc state cũ)**

```
lần 1: == Kết quả: 48 PASS / 0 FAIL · 40 request · 27774 ms   ·  E2E async: OK   (EXIT=0)
lần 2: == Kết quả: 48 PASS / 0 FAIL · 40 request · 30893 ms   ·  E2E async: OK   (EXIT=0)
```

12 bước chính (mỗi bước = 1–3 assert, tổng 48):

| Bước | Assert (nguồn: DB/broker, không tin lời service) |
|---|---|
| 0 | dọn state: fail-mode OFF + `dlq.sh purge` + poll `/ready` 200 |
| 1–3 | login admin/seller · register + login buyer |
| 4–7 | seller tạo category/product/SKU · admin set `onHand=10` |
| 8–11 | **luồng 1**: dọn giỏ → thêm món → checkout 201 → mock-pay → `/confirm` → saga `CONFIRMED` → poll `notification_logs` có row |
| 12–15 | **luồng 2**: checkout → mock-pay, **KHÔNG** `/confirm` → poll `orders.status = CONFIRMED` → poll email (chuỗi 2 event) |
| 16–21 | **luồng 3**: PUT product (tên + giá) → poll `/v1/search` thấy + **giá mới 777000.00** → DELETE → poll search mất → poll `search_product.status = ARCHIVED` |
| 22 | **luồng 4**: tạo SKU mới cho đơn DLQ (SKU cũ đã ARCHIVED ở bước 19) + bật fail-mode (recreate container) → poll `/ready` |
| 23–25 | checkout đơn 3 → mock-pay → `/confirm` **200** (async không chặn đường chính) |
| 26–27 | poll `q.notification.dlq ≥ 1` (retry cạn ≈ 4,2 s) → poll `q.notification = 0` |
| 28–31 | tắt fail-mode → poll `/ready` → `dlq.sh move q.notification` → poll **có email** cho order 3 → poll DLQ rỗng |
| 32 | 6 queue (`q.notification/q.order/q.search` + 3 DLQ) `ready = 0` |

**2. Trap dọn state (AC: kể cả khi chết giữa đường)**

```
# chạy script, đợi tới bước 22 (fail-mode ON) rồi `kill -TERM`:
== TRƯỚC kill (đang trong luồng 4):
SHOPLAB_NOTIFICATION_FAIL_MODE=true
  q.notification.dlq       ready=  0
== SAU kill (trap chạy):
SHOPLAB_NOTIFICATION_FAIL_MODE=false      <-- container được recreate lại, tự tắt lỗi
  q.notification.dlq       ready=  0      <-- purge
```

(Chạy thật 2 lần kill ở 2 thời điểm khác nhau — cả 2 lần state đều về sạch.)

**3. `e2e.yml` parse được + có bước mới**

```
$ python3 -c "import yaml; ...safe_load('.github/workflows/e2e.yml')"
YAML OK · steps = 10
[…, 'Trạng thái container', 'E2E smoke', 'E2E checkout', 'E2E checkout nhánh fail',
 'E2E async (Phase 3)', 'Log container (khi fail)', 'Dọn stack']
job name: compose up (11 thành phần) + smoke + checkout + async
```

**4. Không regress 3 script cũ**

```
== 17 PASS / 0 FAIL · 13 request · 544 ms        (e2e-smoke)
== 27 PASS / 0 FAIL · 14 request · 961 ms        (e2e-checkout)
== 41 PASS / 0 FAIL / 0 SKIP · 37 request        (e2e-checkout-fail)
```

**5. `mvn -B -ntp verify`** — không đổi code Java; chạy lại để chắc nhánh không làm lệch gì:

```
[INFO] BUILD SUCCESS   (13 module · 805 test: 662 unit + 143 IT)
```

**Learning note** *(điền khi Done)*

- **Async test = poll state cuối, không phải assert sau khi gọi API.** Viết `curl` rồi assert ngay thì test xanh vì *chưa
  kịp chạy* — kiểu xanh nguy hiểm nhất. Mọi assert ở đây đều là `poll_until(predicate, timeout)` với predicate đọc
  **nguồn sự thật** (DB, broker, `GET` lại resource) chứ không đọc response của request vừa bắn.
- **Hai timeout, không phải một.** 5 s cho luồng event thường (relay 1 s + consumer). Riêng DLQ phải 15 s vì consumer
  retry `200ms,1s,3s` ≈ **4,2 s** rồi mới nack — hứa "≤ 5 s" cho mọi thứ là tự tạo flaky. Và `/api/queues` của
  RabbitMQ là **số liệu thống kê làm mới ~5 s**: `messages_ready` còn `1` sau khi message đã đi. Lần chạy đầu fail đúng
  ở đây — bài học: khi assert bằng metrics, timeout phải lớn hơn **chu kỳ làm mới của metrics**, không chỉ lớn hơn
  thời gian xử lý.
- **Bẫy thật #1 (lần chạy 1): xoá product ở luồng 3 làm SKU `ARCHIVED` ⇒ checkout luồng 4 chết `409`.** Lỗi hiện ra ở
  bước DLQ (xa chỗ gây lỗi) vì script không assert bước thêm-giỏ. Sửa 2 việc: tạo **SKU riêng** cho luồng 4, và assert
  **cả** `PUT /carts/items` lẫn `POST /v1/checkout`.
- **Bẫy thật #2: giỏ không tự sạch giữa các lần checkout.** `PUT /carts/items/{sku}` là **thêm**, không phải thay ⇒ giỏ
  còn SKU cũ (đã ARCHIVED) ⇒ saga chết ở bước `PRICE` với `409 "SKU không mua được"` — chẩn đoán phải vào
  `checkout_saga_steps` mới thấy. Sửa: `DELETE /v1/carts` đầu mỗi lần checkout ⇒ script chạy lại được vô điều kiện.
- **`$RANDOM` không phải nguồn khoá duy nhất**: bash có thể lặp giá trị ⇒ hai checkout khác body cùng `Idempotency-Key`
  ⇒ **409 request-hash mismatch**. Dùng prefix theo lần chạy + **số thứ tự tăng dần**.
- **Bật lỗi để test DLQ phải có đường quay lại chắc chắn**: fail-mode là property bind lúc boot ⇒ chỉ bật được bằng
  recreate container; nếu không có `trap` tắt lại thì **mọi lần chạy sau đều đỏ** và người ta sẽ "fix" bằng cách xoá
  test. Replay **sau khi** tắt lỗi (không replay lúc message còn độc) — script dạy đúng thao tác vận hành: sửa nguyên
  nhân rồi mới replay.
- **Đo kết quả replay bằng DB, không bằng DLQ rỗng**: "DLQ rỗng" cũng đúng khi message **mất**. Phải thấy
  `notification_logs` có row cho order đó ⇒ chứng minh message được xử lý lại thật.

### SL-3.8 — Báo cáo before/after async (số liệu đo thật)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | Done ✅ (2026-11-09) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | `SL-3.3`…`SL-3.7` (phải có luồng async chạy được mới đo) |
| **Ước lượng** | S |

**Bối cảnh**

"Chuyển sang async cho nhanh hơn" là câu nói không kiểm chứng được. Ở hệ này, **email vốn không nằm trên đường checkout**
⇒ async không làm checkout nhanh hơn; cái async mang lại là **decoupling** (notification chết không làm checkout fail) và
**độ trễ có kiểm soát**. Báo cáo phải nói đúng như vậy, bằng số đo — không bịa "before/after" của một thứ chưa từng chạy sync.

**Mục tiêu**

`docs/reports/async-phase-3.md` trả lời bằng số: async **được gì**, **mất gì**, **giới hạn ở đâu**.

**In scope**

- Đo `latency checkout` (p50/p95, n = 50 request tuần tự + 20 đồng thời) **trước/sau** khi bật notification (để chứng minh
  không đổi, hoặc đổi bao nhiêu).
- Đo **độ trễ event**: từ `confirm` → `notification_logs.sent_at` (p50/p95) và từ webhook → order `CONFIRMED`.
- Đo **throughput relay**: số message/s gửi được khi bơm 500 sự kiện (báo rõ bottleneck: relay batch hay broker).
- Đo **DLQ = 0** trong 5 phút chạy 100 checkout + 100 cập nhật product (không mất message, không message trùng).
- Đo ca **degradation**: `docker stop notification-service` ⇒ checkout p95 **không** đổi (bằng chứng của decoupling) và
  message tồn trong queue rồi được xử lý khi bật lại.

**Out of scope**

- Không load test ở mức flash sale (Phase 4 — k6). Không tối ưu throughput (Phase 4).
- Không so sánh broker (Kafka ở Phase 8).

**Acceptance Criteria**

- [ ] Báo cáo có **bảng số liệu** với: chỉ số · before · after · cách đo · điều kiện (máy, số request, commit).
- [ ] Có **kết luận trung thực**: cái gì async **không** cải thiện (latency checkout) và vì sao vẫn nên làm (decoupling).
- [ ] Có phần **giới hạn**: `unacked` khi retry giữ thread, relay polling trễ ≤ interval, `processed_events` phình.
- [ ] Số liệu trong báo cáo **khớp** với output đã dán trong ticket (không chép tay từ trí nhớ).

**Ghi chú kỹ thuật**

- Đo bằng `curl -w '%{time_total}'` (không cần k6 ở phase này) + `psql` cho độ trễ event; ghi rõ **n** và cách tính p95.
- Nếu số liệu xấu (relay trễ, DLQ > 0) thì **ghi nguyên** — mục đích là biết hệ thật sự ở đâu, không phải báo cáo đẹp.

**Kết quả verify** *(điền khi Done)*

CI **đang tắt** (`SL-3.10`/`TD-28`) ⇒ bằng chứng = chạy `tools/bench-async.sh` trên stack thật + `mvn verify`.

**1. Chạy đo đầy đủ** — `bash tools/bench-async.sh` (50 / 20 / 500 / 100 · cửa sổ DLQ 300 s):

```
PASS=20 FAIL=0    (M1 · M2 · M3 · M4 · M5, mỗi phép có 1–4 assert)
raw: /tmp/bench-async-1794245983/   summary.md = bảng đã in
commit f1aee5e · 2026-11-09T17:40:39Z · WSL2 i5-12600K 12 thread 19 GB · Docker 29.9.3
```

**2. Latency checkout (before = notification-service stopped / after = đang chạy)**

```
tuần tự   before n=50 p50=42.7 p95=48.9 max=51.9 ms   |  after p50=43.1 p95=59.6 max=63.5 ms
đồng thời before n=20 p50=178.4 p95=197.6            |  after p50=182.2 p95=203.6           (20 buyer riêng)
⇒ p50 +0,9% · p95 +21,9% nhưng cả hai < 60 ms  (khi consumer CHẾT p95 = 44,6 ms ⇒ chênh lệch là nhiễu tải)
```

**3. Độ trễ event (server-side)**

```
confirm → email        n=70 p50=551.9 p95=1010.6 max=1057.6 ms   (relay 1 s ⇒ trần ~1 s/chặng)
webhook → CONFIRMED    n=70 p50=28.6  p95=45.9   max=49.9   ms
```

**4. Throughput relay**

```
500 product.updated.v1 · relay gửi hết 7,85 s ⇒ 63,7 msg/s · đuôi 0,26 s · PENDING còn 0
producer bơm 65,7 request/s · trần lý thuyết = batch 100 × nhịp 1 s = 100 msg/s ⇒ nghẽn ở RELAY, không phải broker
```

**5. DLQ = 0 + không mất/trùng (100 checkout + 100 update, 300 s)**

```
DLQ max = 0 (q.notification.dlq · q.order.dlq · q.search.dlq)
order.confirmed.v1    phát 100 / xử lý 100
payment.succeeded.v1  phát 100 / xử lý 100
product.updated.v1    phát 100 / xử lý 100
notification_logs     100 row / 100 event_id duy nhất
```

**6. Degradation**

```
stop notification-service → 20 checkout: p50=35.8 p95=44.6 ms (KHÔNG tệ hơn lúc bình thường)
q.notification ready=20 · unacked=0
start lại → drain 9,3 s (gồm ~6 s Spring Boot khởi động) · email 20/20 đơn
```

**7. Script + repo**

```
bash -n tools/bench-async.sh        → OK
smoke 20/20 với n nhỏ (5/5/20/5 · 30 s) trước khi chạy full — đã sửa 4 lỗi script (xem learning note)
mvn -B -ntp verify                 → BUILD SUCCESS (805 test · 662 unit + 143 IT)   [nhánh chỉ thêm tools + docs]
```

**Learning note** *(điền khi Done)*

- **Câu "async cho nhanh" sai về bản chất, và số đo nói đúng thế**: p50 checkout 42,7 → 43,1 ms (+0,9%). Email/index
  chưa bao giờ nằm trên đường checkout ⇒ **không có gì để tăng tốc**. Thứ async mua được là **isolation** (consumer chết,
  checkout p95 vẫn 44,6 ms) và **độ trễ có trần** (p95 confirm→email 1011 ms ≈ relay 1 s). Báo cáo phải viết được cả hai
  vế này, nếu chỉ khoe "p95 < 60 ms" là báo cáo dở.
- **Đo "before" khi không thể revert code**: chọn trạng thái *không có ai tiêu thụ event* (`docker stop
  notification-service`) và **nói rõ định nghĩa** ngay đầu báo cáo. Đây cũng chính là phép thử decoupling — một mũi tên
  trúng hai đích, thay vì bịa ra số "trước Phase 3".
- **`p95` chênh +10,7 ms là nhiễu, và phải chứng minh bằng số khác**: khi consumer **chết** p95 = 44,6 ms (thấp hơn cả
  lúc chạy) ⇒ chênh lệch đến từ tải consumer trên **cùng** Postgres, không phải phụ thuộc đồng bộ. Nếu không có phép đo
  M5, con số +21,9% sẽ bị đọc thành "async làm chậm checkout" — sai.
- **Nghẽn phải chỉ đúng tên**: 63,7 msg/s so với trần 100 msg/s (batch 100 × 1 s) + đuôi chỉ 0,26 s ⇒ nghẽn là **relay
  polling**, không phải broker. Muốn nói "broker chậm" thì phải thấy `publisher confirm` treo, không phải suy đoán.
- **Đo độ trễ event phải ghép 2 database**: `notification_logs` và `order_status_history` nằm ở 2 DB riêng (database per
  service) ⇒ xuất `(order_id, epoch)` từ mỗi bên rồi trừ trong python. Đây là hệ quả trực tiếp của kiến trúc, phải chấp
  nhận chứ không "JOIN cho nhanh" (không làm được).
- **4 lỗi script tự gây ra, sửa bằng cách chạy smoke trước**: (1) `local a="$1" b=$(( ... + b ))` — `set -u` nổ vì bash
  expand cả command trước khi chạy `local`; (2) `psql -tA` mặc định separator `|` trong khi parser tách theo dấu cách ⇒
  M2 ra **n=0** (không lỗi, chỉ ra số vô nghĩa — nguy hiểm hơn lỗi); (3) chờ `queue ready=0` không đủ vì event còn trong
  outbox ⇒ M2 n=1/3, phải chờ **email xuất hiện**; (4) checkout đồng thời chỉ tạo đơn mà không thanh toán ⇒ 20 đơn treo
  `PENDING_PAYMENT` và mẫu M2 thiếu. Bài học: **script đo cũng phải có test** (chạy n với n nhỏ trước).
- **Số liệu tự chỉ ra nợ**: sau một buổi đo, `outbox` đã 715/380/359 row và **không có job dọn** ⇒ ghi `TD-35` ngay
  trong ticket này (cùng họ với `TD-30` cho `processed_events`/`notification_logs`). Đo không chỉ để báo cáo — nó là
  cách tìm ra giới hạn thật.

### SL-3.9 — `docs/runbook-phase-3.md` + DoD Phase 3 (archive · tag `phase-3` · release)

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-11) |
| **Phase** | 3 — Async & event-driven |
| **Phụ thuộc** | **mọi** ticket `SL-3.x` trước đó `Done` |
| **Ước lượng** | S |

**Bối cảnh**

Debug async khác debug sync ở một điểm cốt tử: **lỗi không nằm trong response HTTP**. Người trực phải biết nhìn
`ready`/`unacked`/DLQ/outbox `PENDING`, biết message nào đã đi và message nào còn kẹt. Không có runbook thì mỗi lần
"email không tới" là một cuộc điều tra từ đầu.

**Mục tiêu**

1. `docs/runbook-phase-3.md` đủ để người khác debug luồng async **mà không cần đọc code**.
2. Đóng Phase 3 theo DoD (`AGENTS.md` §5, ADR-0005): archive · snapshot doc · tag `phase-3` · release.

**In scope**

- Runbook: sơ đồ event (producer → exchange → queue → consumer → DB) · bảng tra nhanh (queue/DLQ/outbox/log theo service) ·
  lệnh kiểm tra từng tầng (`tools/rabbit-topology.sh`, `psql` outbox + `processed_events` + `notification_logs`, search index) ·
  bảng **≥ 6 lỗi** (email không tới · DLQ tăng · order không auto-confirm · index lệch · relay không chạy · message trùng) ·
  **§5 một ví dụ debug thật** dán nguyên văn.
- Archive `SL-3.x` → `Backlog-phase-3.md`; `docs/phases/phase-3.md`; cập nhật `docs/phases/README.md` + bảng archive +
  `architecture.md` §12; tag annotated `phase-3` + GitHub Release (sau merge).

**Out of scope**

- Không sửa code service; không mở `maint/phase-3`; không làm alert/dashboard (Phase 6).

**Acceptance Criteria**

- [x] Runbook có đủ mục như trên; **mỗi lệnh chạy được thật** trên stack compose.
- [x] §5 dán output debug thật của **một** ca async fail (DLQ hoặc relay kẹt), không viết tay.
- [x] `Backlog-phase-3.md` đủ ticket + header; `Backlog.md` không còn `### SL-3.x`; `docs/phases/phase-3.md` có số liệu của `SL-3.8`.
- [x] `docker compose down -v` → `up -d --build` ⇒ 14 container healthy (thêm notification + search), 4 script E2E xanh.
- [x] Tag `phase-3` + Release; `architecture.md` §12 đánh dấu Phase 3 ✅ Done.

**Ghi chú kỹ thuật**

- Bảng lỗi ưu tiên **triệu chứng người trực thấy** ("email không tới", "DLQ tăng") thay vì tên exception.
- Số liệu snapshot lấy từ báo cáo `SL-3.8` — **một** nguồn, không chép lại nhiều nơi.

**Kết quả verify** *(2026-11-11)*

**1) Snapshot dựng lại từ volume rỗng** (`down -v` → `up -d --build`) — đúng thứ người clone mới sẽ làm:

```console
$ docker compose -f infra/docker-compose.yml --env-file .env down -v
 Volume shoplab-pg-data Removed · Network shoplab-net Removed
$ docker compose -f infra/docker-compose.yml --env-file .env up -d --build
… EXIT=0

$ docker compose ps --format '{{.Name}} {{.Status}}' | grep shoplab | wc -l
14
 shoplab-adminer · api-gateway · cart · catalog · identity · inventory · notification · order
 payment · postgres · promotion · rabbitmq · redis · search      → 14 / 14 Up (healthy)
```

> Lần `up` **đầu tiên** của lượt này fail: `identity-service unhealthy` → `Flyway` không kết nối được vì
> `password authentication failed for user "shoplab"` — volume Postgres cũ giữ mật khẩu khác `.env`. Đúng lý do
> AC đòi `down -v`: **volume cũ che lỗi của người clone mới**. Đã ghi thành bẫy #2 trong `docs/phases/phase-3.md`.

**2) 4 script E2E trên stack đó** (thêm script async của phase này):

```text
$ bash tools/e2e-smoke.sh          17 PASS / 0 FAIL · 13 request · 593 ms
$ bash tools/e2e-checkout.sh       27 PASS / 0 FAIL · 14 request · 1176 ms
$ bash tools/e2e-checkout-fail.sh  41 PASS / 0 FAIL / 0 SKIP · 37 request · 13546 ms
$ bash tools/e2e-async.sh          48 PASS / 0 FAIL · 36 request · 37428 ms   ← 4 luồng async + DLQ replay
```

**3) Mọi lệnh trong runbook §3 chạy được thật** (dán output thật vào chính runbook, không viết tay):
`tools/rabbit-topology.sh` (topology + `consumers`/`ready`/`unacked`) · `tools/dlq.sh list/peek/move` ·
`psql` outbox (`SENT`/`attempts`/`last_error`) · `processed_events` · `notification_logs` ·
`search_product` + `POST /v1/admin/search/reindex` (`{"scanned":21,"upserted":21,"archived":0}`) ·
`order_status_history` · management API `messages_ready/unacknowledged/consumers`.

**4) Runbook §5 = ca debug thật "email không tới"** (`SHOPLAB_NOTIFICATION_FAIL_MODE=true`):

```text
checkout=201 · confirm=200            ← đường chính KHÔNG bị ảnh hưởng
log: lỗi xử lý (lần 1, 200 ms) → (lần 2, 1000 ms) → (lần 3, 3000 ms) → "thất bại sau 4 lần thử → DLQ"
q.notification ready=0 · q.notification.dlq ready=1
notification_logs = 0 · processed_events = 0 · outbox = SENT (attempts=1) · orders.status = CONFIRMED
dlq.sh peek → x-death routing key GỐC = order.confirmed.v1
fail-mode=false (recreate) + dlq.sh move → notification_logs = order.confirmed|SENT · processed_events = 1 row · DLQ = 0
```

**5) Test + contract + lint** (`mvn -B -ntp verify` — chạy **sau khi `docker compose down`**):

```text
BUILD SUCCESS · 3 min 04 s · 805 test (662 unit + 143 IT) · 0 fail
  commons 81 · template 35 · identity 54+24 · catalog 79+26 · inventory 44+15 · promotion 48+13
  order 78+28 · payment 66+12 · cart 34+6 · notification 34+9 · search 38+10 · gateway 71
$ bash tools/export-openapi.sh → VALIDATE PASS · 42 path · 51 operation · 35 cần token · 48 schema
  (thêm `/v1/auth/service-token` của `SL-3.11` vào snapshot contract)
```

**6) DoD phase (`AGENTS.md` §5, ADR-0005)**

```text
[x] 12/12 ticket Done → tách chi tiết sang Backlog-phase-3.md (12 mục `### SL-3.x`); Backlog.md còn 0 mục
[x] docs/runbook-phase-3.md · docs/phases/phase-3.md · docs/phases/README.md (dòng phase 3 ✅)
[x] architecture.md §12 Phase 3 → ✅ Done (+ changelog)
[x] bảng "Archive các phase đã xong" trong Backlog.md + header §1 (12/12) + Learning log + Changelog
[x] CI bật lại: gh variable delete CI_ENABLED → `gh variable list` rỗng (TD-28 → ✅ đã trả)
[x] tag annotated `phase-3` trên merge commit + GitHub Release (notes tái dùng Backlog-phase-3.md)

**Sự cố trong lúc làm**

- **`mvn verify` đỏ ở lượt đầu** (`CheckoutSagaIT.sweeperExpiresOverdueOrder`: `second` mong `0`, nhận `1`) khi **14
  container compose đang chạy**. Chạy riêng class: **15/15 xanh**; chạy lại full sau `docker compose down`: **xanh**.
  Kết luận: flaky **theo tải** (payment-window trong IT chỉ 1 s) ⇒ ghi `TD-36` thay vì "sửa test trong ticket docs"
  (đổi code test là ticket riêng, `AGENTS.md` §3.1).
- **`TD-35` là nợ trùng `TD-29`** — phát hiện khi rà §7 để archive; đã gộp (giữ dòng `TD-35` làm dấu vết), đồng thời
  sửa dòng `TD-29` vốn ghi sai "**Đã trả ở `SL-3.7`** (chưa)".
- **`export-openapi.sh` làm thay đổi `docs/api/openapi.yaml`** (+78 dòng: `/v1/auth/service-token`): snapshot contract
  trước đó thiếu endpoint của `SL-3.11` ⇒ commit luôn trong PR này (doc artifact, không phải code service).
- **`.env` lệch mật khẩu so với volume Postgres cũ** ⇒ 14/14 container không lên được. Bài học: `down -v` không phải
  thủ tục cho vui; nó là bước duy nhất phát hiện lớp lỗi này.

**Learning note**

- **Đóng phase là việc *đọc lại toàn bộ* tài liệu của phase, và nó luôn tìm ra lỗi**: lượt này lộ 1 nợ **trùng**
  (`TD-35` ≈ `TD-29`), 1 dòng trạng thái **sai trong chính bảng nợ** (`TD-29` ghi "đã trả… (chưa)"), 1 heading sai port
  (`search-service` 8090 vs **8091**), 1 snapshot contract **thiếu** endpoint mới (`/v1/auth/service-token`) và 1 test IT
  **flaky theo tải**. Không có bước nào trong số này hiện ra nếu chỉ chạy `git tag`.
- **Runbook chỉ đáng tin khi chính mình dùng nó để debug**: §5 là output của một ca **thật** (bật fail-mode, checkout,
  retry → DLQ, peek, sửa, replay). Ca đó dạy 4 điều không tài liệu thiết kế nào nói: `SENT` ≠ "đã xử lý";
  `processed_events` **rỗng đúng thiết kế** khi handler fail; log ghi "sau **4** lần thử" khi chỉ cấu hình 3 retry;
  và `move` **ack khỏi DLQ** nên sai routing key là mất message.
- **Verify "snapshot chạy được" phải là `down -v` + `up --build`** — lần này nó bắt ngay một lỗi mà `up -d` trên volume
  cũ che mất (mật khẩu Postgres lệch `.env`). Cùng bài học với `SL-2.10`, nhưng là *biến thể khác*: không phải
  Flyway/seed, mà là **volume giữ credential cũ**.
- **Flaky test phải được xử lý bằng dữ liệu, không bằng cách chạy lại tới khi xanh**: chứng minh được (chạy riêng →
  xanh; máy tải → đỏ; tắt stack → xanh) rồi ghi `TD-36` kèm **2 hướng sửa cụ thể**. Sửa test là **code** ⇒ không gộp
  vào ticket doc, dù nó "chỉ 1 dòng".
- **Tắt CI là quyết định, bật lại cũng là quyết định — và phải có người bấm**: `TD-28` tồn tại đúng 3 ngày; nếu để nó
  "tự hiểu là sẽ bật lại sau" thì phase sau sẽ merge tiếp với 0 check. Điều kiện revisit ghi được (**"khi đóng Phase 3"**)
  là thứ khiến việc bật lại xảy ra, chứ không phải kỷ luật cá nhân.

```

### SL-3.10 — Process: tắt CI tạm thời + `merge-pr.sh --skip-ci-check`

| Trường | Giá trị |
|---|---|
| **Trạng thái** | ✅ Done (2026-11-08) |
| **Phase** | 3 — Async & event-driven *(phát sinh — quyết định của human 2026-11-08)* |
| **Phụ thuộc** | — (áp dụng từ `SL-3.2` xong trở đi); **chặn** không ticket nào, nhưng đổi cách mọi ticket sau được merge |
| **Ước lượng** | S |

**Bối cảnh**

Đo thật: mỗi PR tốn **~5–6 phút** CI (`test` ~5 min là phần lớn) + thêm ~5 phút run trên `main` ⇒ ~10 phút/ticket chỉ để
chờ. Trong khi đó `mvn verify` local chạy **đúng nội dung job `test`** (unit + IT Testcontainers + JaCoCo gate). Phase 3
còn 7 ticket ⇒ ~70 phút chờ. Quyết định: **tắt CI tạm thời** để đẩy tốc độ, chấp nhận mất hàng rào tự động — nhưng phải
tắt **có ghi chép và có đường bật lại**, không để rule "CI xanh mới merge" mục âm thầm.

**Mục tiêu**

1. CI tắt bằng **kill-switch có sẵn** (`vars.CI_ENABLED=false`) — không sửa file workflow, bật lại bằng 1 lệnh.
2. `tools/merge-pr.sh` có cờ `--skip-ci-check` để merge được khi không có run CI, **kèm cảnh báo rõ** trong output và
   trong comment evidence hậu merge (người đọc PR sau vẫn biết PR này **không** qua CI).
3. Rule + nợ kỹ thuật được ghi lại: `AGENTS.md` §9, `.clinerules/10-git-workflow.md`, `Backlog.md` §7 (`TD`).

**In scope**

- `tools/merge-pr.sh`: thêm `--skip-ci-check` (bỏ qua bước đòi CI xanh; vẫn **bắt buộc** PR mở/không draft + có comment
  tóm tắt; in `CẢNH BÁO` + ghi vào evidence `CI: bỏ qua (--skip-ci-check)`).
- `gh variable set CI_ENABLED --body false` (và hướng dẫn bật lại).
- `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + `README.md` (mục CI): ghi trạng thái đang tắt + điều kiện bật lại.
- `Backlog.md` §7: `TD-28` — "CI đang tắt" với **điều kiện revisit verify được**.

**Out of scope**

- Không sửa `.github/workflows/ci.yml` (kill-switch đã có từ `SL-1.0`; sửa workflow lúc này là thêm rủi ro vô ích).
- Không bỏ hẳn rule "CI xanh mới merge" — đây là **ngoại lệ tạm thời**, hết Phase 3 phải bật lại.
- Không đổi `e2e.yml` (vốn chỉ chạy `schedule`/`dispatch`).

**Acceptance Criteria**

- [x] `gh variable list` cho thấy `CI_ENABLED=false`.
- [x] Push 1 nhánh bất kỳ ⇒ **0 run** CI mới (kiểm bằng `gh run list --branch <branch>`).
- [x] `bash tools/merge-pr.sh <pr> --summary-file f --skip-ci-check` merge được PR **không có run**, in cảnh báo, và
      comment evidence có dòng `CI: BỎ QUA`.
- [x] Không có `--skip-ci-check` ⇒ script **vẫn chặn** PR code không có run CI (hành vi cũ giữ nguyên).
- [x] `AGENTS.md` §9 + `.clinerules/10-git-workflow.md` + README + `TD-28` đã ghi, kèm câu lệnh bật lại.
- [x] Mỗi ticket còn lại của Phase 3 vẫn **dán output `mvn verify` + e2e local** vào ticket (thay cho CI).
      *(ràng buộc quy trình — áp dụng từ `SL-3.3`; đã ghi vào header "Ticket tiếp theo" + `AGENTS.md` §9)*

**Ghi chú kỹ thuật**

- Kill-switch nằm ở `if:` của cả 3 job (`vars.CI_ENABLED != 'false'`) ⇒ job bị **skip**, không đốt phút. Đổi lại: PR không
  có check nào ⇒ GitHub UI không có dấu hiệu gì; vì vậy **comment evidence** là chỗ duy nhất còn dấu vết.
- `--skip-ci-check` **không** được là mặc định: mặc định vẫn chặn, phải gõ cờ mới bỏ qua — tránh việc "quên bật lại CI"
  mà vẫn merge ầm ầm.
- Rủi ro đã chấp nhận: `build-image` matrix (9 image) là chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module ⇒
  trong giai đoạn này, service nào đổi thì **tự build image local** (đã làm ở `SL-3.1`/`SL-3.2`).

**Kết quả verify** *(2026-11-08 — đo thật trên repo này)*

**1) Kill-switch đã tắt đúng — job `skipping`, 0 phút:**

```console
$ gh variable list
CI_ENABLED  false  less than a minute ago

$ git push -u origin chore/sl-3.10-ci-off-burst        # push 1 nhánh mới
$ gh run list --branch chore/sl-3.10-ci-off-burst
(empty)                                                ← không có run mới

$ gh pr checks 55
build-image (${{ matrix.service }})   skipping   0
lint (checkstyle + spotless)          skipping   0
test (unit + jacoco gate)             skipping   0
```

**2) `merge-pr.sh` giữ đúng hàng rào khi KHÔNG truyền cờ:**

```console
$ bash tools/merge-pr.sh 55 --summary-file /tmp/pr-310.md
merge-pr: run CI mới nhất của 'chore/sl-3.10-ci-off-burst' là 'skipped' (cần 'success'). Không merge.
```

**3) Có cờ ⇒ cảnh báo rõ + đi tiếp (dry-run):**

```console
$ bash tools/merge-pr.sh 55 --summary-file /tmp/pr-310.md --skip-ci-check --dry-run
merge-pr: ⚠️  CẢNH BÁO — BỎ QUA kiểm tra CI (--skip-ci-check).
merge-pr:    Chỉ dùng khi CI đang tắt (gh variable get CI_ENABLED = false).
merge-pr:    Trách nhiệm verify chuyển về ticket: phải có output 'mvn verify' + e2e local.
merge-pr: PR #55 (chore/sl-3.10-ci-off-burst) — ⚠️ CI BỎ QUA (--skip-ci-check) · HEAD 92ff2b9c…
merge-pr: --dry-run → không comment, không merge.
```

**4) Rule + nợ đã ghi:** `AGENTS.md` §9 (ngoại lệ + lệnh bật lại) · `.clinerules/10-git-workflow.md` (điều kiện merge
thay thế) · `README.md` mục CI · `Backlog.md` §7 **`TD-28`** (mức `cao`, điều kiện revisit = **bắt buộc** bật lại khi
đóng Phase 3).

**Learning note**

- **Tắt CI không phải "không ảnh hưởng"**: mất 3 hàng rào (test độc lập + JaCoCo gate, lint môi trường sạch,
  `build-image` matrix — chốt duy nhất bắt lỗi Dockerfile/COPY pom khi thêm module). Đổi lại được ~5–6 phút/PR.
  Cách đúng: tắt bằng **kill-switch có sẵn** (`vars.CI_ENABLED`) + **ghi nợ có điều kiện bật lại**, không sửa workflow.
- **Một cờ "bỏ qua hàng rào" phải mặc định TẮT và phải nói to**: `--skip-ci-check` chỉ chạy khi gõ tường minh; script in
  cảnh báo + ghi `CI: BỎ QUA` vào comment evidence hậu merge ⇒ người đọc PR 3 tháng sau vẫn biết PR đó **không** qua CI.
  Nếu ghi ở nơi dễ mất (chat, trí nhớ) thì "tạm thời" sẽ thành vĩnh viễn mà không ai biết.
- **Kill-switch để lại một khoảng trống quan sát**: PR không có check nào ⇒ GitHub UI **không** hiện dấu hiệu gì. Bù lại bằng
  kỷ luật *dán output local vào ticket*; đây là chỗ dễ trượt nhất khi vội — nên ghi thẳng vào AC của mọi ticket còn lại.
- **Verify tắt-xong-phải-thử-cả-hai-nhánh**: kiểm "có cờ thì chạy" thôi là chưa đủ; phải kiểm **"không cờ vẫn chặn"** —
  nếu không, một lần refactor sau có thể làm bỏ qua kiểm CI trở thành mặc định.
