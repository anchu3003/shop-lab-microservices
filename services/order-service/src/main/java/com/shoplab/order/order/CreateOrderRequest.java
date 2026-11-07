package com.shoplab.order.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body của {@code POST /v1/orders}.
 *
 * <p>Mang **snapshot** giá/tên SKU (lấy từ cart hoặc catalog bởi caller) — order-service không gọi
 * catalog khi tạo đơn (xem ghi chú ở ticket: `GET /v1/skus?ids=` chưa tồn tại; thêm nó là ticket
 * riêng của catalog).
 *
 * <p>{@code subtotal}/{@code total} **không** nhận từ client: tiền phải thu do server tính từ
 * {@code unitPrice × qty} (client gửi tổng là mời gọi sửa 1 field để trả ít tiền hơn).
 */
public record CreateOrderRequest(
    @NotEmpty @Size(max = 100) List<@Valid Line> items,
    @DecimalMin(value = "0.00", message = "phải >= 0") BigDecimal discount,
    @Pattern(regexp = "[A-Z]{3}", message = "currency phải là 3 chữ in hoa, vd VND")
        String currency) {

  /** Một dòng hàng — snapshot tại thời điểm đặt. */
  public record Line(
      @NotNull UUID skuId,
      @NotBlank @Size(max = 64) String skuCode,
      @NotBlank @Size(max = 200) String title,
      @NotNull @DecimalMin(value = "0.00", message = "phải >= 0") BigDecimal unitPrice,
      @NotNull @Min(1) Integer qty) {}

  /** Currency mặc định cho lab (Phase 2 dùng VND). */
  public String currencyOrDefault() {
    return currency == null ? "VND" : currency;
  }
}
