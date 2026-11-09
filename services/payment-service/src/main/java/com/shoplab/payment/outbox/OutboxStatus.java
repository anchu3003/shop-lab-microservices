package com.shoplab.payment.outbox;

/** Trạng thái của một dòng outbox ({@code PENDING} ⇒ còn phải publish, {@code SENT} ⇒ đã gửi). */
public enum OutboxStatus {
  PENDING,
  SENT
}
