package com.shoplab.inventory.stock;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.util.UUID;

/**
 * Không đủ hàng để giữ chỗ → HTTP <b>409</b> (xung đột trạng thái dữ liệu, không phải lỗi cú pháp).
 *
 * <p>Mang theo {@code skuId} + {@code available} để client hiển thị được "chỉ còn 3" thay vì một
 * câu chung chung — và để test/assert biết <b>SKU nào</b> là nguyên nhân khi reserve nhiều SKU.
 */
public class InsufficientStockException extends AppException {

  private final UUID skuId;
  private final int available;
  private final int requested;

  public InsufficientStockException(UUID skuId, int available, int requested) {
    super(
        ErrorCode.CONFLICT,
        "Không đủ hàng cho SKU " + skuId + ": còn " + available + ", cần " + requested);
    this.skuId = skuId;
    this.available = available;
    this.requested = requested;
  }

  public UUID skuId() {
    return skuId;
  }

  public int available() {
    return available;
  }

  public int requested() {
    return requested;
  }
}
