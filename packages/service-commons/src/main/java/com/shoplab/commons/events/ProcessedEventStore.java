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
}
