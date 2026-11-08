package com.shoplab.order.outbox;

/** Trạng thái của một dòng outbox. Chỉ 2 trạng thái: chưa gửi được thì vẫn còn nghĩa vụ gửi. */
public enum OutboxStatus {
  PENDING,
  SENT
}
