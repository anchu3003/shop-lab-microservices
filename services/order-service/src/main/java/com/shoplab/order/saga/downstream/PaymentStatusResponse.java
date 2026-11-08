package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Trạng thái payment intent đọc từ `payment-service` (`GET /v1/payment-intents/{id}`).
 *
 * <p>Saga chỉ cần {@code status} — và chỉ tin giá trị này để quyết định "đã trả tiền chưa". Client
 * nói "tôi trả rồi" là không đủ: ai cũng bấm được nút confirm.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentStatusResponse(String status) {

  /** Trạng thái coi là "tiền đã về". */
  public boolean succeeded() {
    return "SUCCEEDED".equals(status);
  }
}
