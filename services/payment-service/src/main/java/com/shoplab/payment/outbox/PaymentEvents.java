package com.shoplab.payment.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payload event do `payment-service` phát (SL-3.5) — **chỉ field consumer cần**.
 *
 * <p><b>Vì sao không có {@code userId}</b>: `order-service` đã biết chủ đơn (đọc từ saga của chính
 * nó); thêm vào payload chỉ làm message phình thêm ở outbox + queue + DLQ. Nguyên tắc chung của
 * ADR-0007: envelope trả lời "ai phát/khi nào/loại gì", payload trả lời đúng câu consumer hỏi.
 *
 * <p><b>Vì sao có {@code paymentIntentId}</b>: consumer phải **đối chiếu** intent trong payload với
 * intent mà saga đang giữ. Không có nó thì một event của đơn khác (bug routing, replay sai) sẽ chốt
 * nhầm đơn — chốt nhầm đơn đã thanh toán là loại lỗi không sửa được bằng code.
 */
public final class PaymentEvents {

  /** Aggregate type trong outbox — "payment" (đối xứng `order` ở SL-3.3). */
  public static final String AGGREGATE = "payment";

  /** `payment-service` phát khi intent → `SUCCEEDED`; `order-service` (`q.order`) là consumer. */
  public static final String SUCCEEDED = "payment.succeeded.v1";

  public static Map<String, Object> succeeded(
      UUID paymentIntentId, UUID orderId, BigDecimal amount, String currency, Instant succeededAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("paymentIntentId", paymentIntentId.toString());
    payload.put("orderId", orderId.toString());
    payload.put("amount", amount);
    payload.put("currency", currency);
    payload.put("succeededAt", succeededAt.toString());
    return payload;
  }

  private PaymentEvents() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
