package com.shoplab.order.order;

import jakarta.validation.constraints.Size;

/** Body của {@code POST /v1/orders/{id}/cancel} — lý do không bắt buộc nhưng rất nên có (audit). */
public record ChangeStatusRequest(@Size(max = 200) String reason) {}
