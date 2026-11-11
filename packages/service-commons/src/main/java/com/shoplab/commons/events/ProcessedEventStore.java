package com.shoplab.commons.events;

/**
 * Ghi nhận "event này đã xử lý" theo {@code (eventId, consumer)} — chốt chặn của ngữ nghĩa
 * at-least-once (SL-3.2, ADR-0007 D3).
 *
 * <p><b>Vì sao ở DB của consumer, không ở Redis</b> (quyết định đã chốt trong ticket): dedupe phải
 * bền như chính dữ liệu được bảo vệ. Redis mất dữ liệu là chấp nhận được với cart (học ở `SL-2.1`)
 * nhưng **không** chấp nhận được ở đây — mất dấu dedupe = gửi email 2 lần / cộng tiền 2 lần.
 *
 * <p>Bảng phình dần (1 row/message/service) ⇒ cần job dọn; đây là nợ đã biết, ghi `TD` khi làm
 * service consumer đầu tiên (`SL-3.4`).
 */
public interface ProcessedEventStore {

  /**
   * Đánh dấu event đã xử lý (idempotent).
   *
   * @return {@code true} nếu **lần đầu** thấy event này (caller được phép xử lý); {@code false} nếu
   *     đã có ⇒ caller **bỏ qua** và chỉ cần ack
   */
  boolean markProcessed(String consumer, EventEnvelope envelope);

  /**
   * Bỏ dấu "đã xử lý" khi lần xử lý **thất bại** (trước khi đẩy message vào DLQ).
   *
   * <p>Không có bước này thì DLQ replay vô nghĩa: message đã bị đánh dấu xử lý ở lần fail đầu,
   * replay sẽ bị dedupe chặn và **không bao giờ được chạy lại**. Bài học: marker chỉ được giữ khi
   * việc xử lý đã **thành công**.
   */
  void unmarkProcessed(String consumer, EventEnvelope envelope);

  /**
   * Xoá marker cũ hơn {@code cutoff} — job retention của service gọi định kỳ (`TD-30`, `SL-4.0`).
   *
   * <p><b>Vì sao phải có</b>: bảng tăng 1 row/event/consumer **vĩnh viễn** nếu không dọn. Nhưng
   * <b>không được xoá sớm</b>: marker là thứ chặn xử lý trùng, nên retention phải <b>≥ retention
   * của bảng {@code outbox}</b> — nếu marker hết hạn trước khi event bị xoá khỏi outbox thì một lần
   * replay/redeliver sau đó sẽ được xử lý lần hai.
   *
   * <p>Xoá **theo batch** để transaction ngắn (không khoá bảng, không tạo WAL khổng lồ).
   *
   * @param cutoff xoá row có {@code processed_at < cutoff}
   * @param batch số row tối đa mỗi lần gọi
   * @return số row đã xoá (0 ⇒ bảng đã sạch tới ngưỡng)
   */
  int deleteProcessedBefore(java.time.Instant cutoff, int batch);
}
