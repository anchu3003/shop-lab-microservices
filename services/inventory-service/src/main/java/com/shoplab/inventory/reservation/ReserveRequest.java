package com.shoplab.inventory.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Body của {@code POST /v1/stock/reserve}.
 *
 * <p>{@code orderId} bắt buộc vì nó là <b>khoá idempotency</b>: retry cùng orderId phải trả cùng
 * reservation chứ không giữ chỗ lần 2. Thiếu nó thì không phân biệt được "user bấm 2 lần" với "2
 * đơn khác nhau" — đúng loại bug chỉ lộ ra khi có retry.
 */
public record ReserveRequest(
    @NotNull UUID orderId, @NotEmpty @Size(max = 50) List<@Valid Item> items) {

  /** Một dòng hàng cần giữ chỗ. */
  public record Item(@NotNull UUID skuId, @NotNull @Min(1) Integer qty) {}
}
