package com.shoplab.order.saga;

import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng audit của saga — "bước nào, mất bao lâu, kết quả gì".
 *
 * @param httpStatus status HTTP của service đích; {@code null} cho bước chạy tại chỗ (order-create)
 *     hoặc bước bị bỏ qua
 * @param durationMs thời gian bước đó — con số duy nhất cho biết p99 checkout đang nằm ở đâu
 * @param detail lý do/ngữ cảnh ngắn (vd `missingSku=…`, `total=0 → skip payment`)
 */
public record SagaStepRecord(
    UUID orderId,
    CheckoutStep step,
    SagaStepStatus status,
    Integer httpStatus,
    int durationMs,
    String detail,
    Instant createdAt) {

  /** Kết quả của một bước. */
  public enum SagaStepStatus {
    OK,
    FAILED,
    /** Bước không chạy vì điều kiện nghiệp vụ (vd đơn 0 đồng ⇒ không cần payment). */
    SKIPPED
  }
}
