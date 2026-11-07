package com.shoplab.order.order;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tính tiền của một order từ snapshot đầu vào — <b>hàm thuần</b> (test được mọi biên).
 *
 * <p>Quy ước (giống promotion-service để 2 service không lệch nhau):
 *
 * <ul>
 *   <li>{@code lineTotal = unitPrice × qty}, làm tròn {@link RoundingMode#HALF_UP} 2 chữ số.
 *   <li>{@code subtotal = Σ lineTotal}; {@code total = subtotal − discount}.
 *   <li>Giảm giá không vượt subtotal (tổng không âm) — chốt ở đây và cả CHECK ở DB.
 * </ul>
 *
 * <p>Vì sao tính ở đây chứ không tin client: {@code subtotal/total} là số tiền <b>phải thu</b>.
 * Client gửi tổng đã tính sẵn là mời gọi gian lận (sửa 1 field). Ở đây chỉ tin {@code unitPrice}
 * (đến từ cart/catalog nội bộ) và qty.
 */
public final class OrderPricing {

  private static final int MONEY_SCALE = 2;

  /**
   * Kết quả tính tiền của toàn đơn.
   *
   * @param items dòng hàng đã có {@code lineTotal}
   * @param subtotal tổng trước giảm giá
   * @param discount giảm giá đã áp (<= subtotal)
   * @param total số tiền phải thu
   */
  public record Priced(
      List<PricedItem> items, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

  /** Một dòng hàng đã tính {@code lineTotal}. */
  public record PricedItem(
      java.util.UUID skuId,
      String skuCode,
      String title,
      BigDecimal unitPrice,
      String currency,
      int qty,
      BigDecimal lineTotal) {}

  /**
   * @throws AppException (400) khi discount âm / lớn hơn subtotal, hoặc danh sách item rỗng
   */
  public static Priced price(List<PricedItem> rawItems, BigDecimal discount, String currency) {
    if (rawItems == null || rawItems.isEmpty()) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "Order phải có ít nhất 1 item");
    }
    BigDecimal effectiveDiscount = discount == null ? BigDecimal.ZERO : discount;
    if (effectiveDiscount.signum() < 0) {
      throw new AppException(ErrorCode.VALIDATION_ERROR, "discount phải >= 0");
    }

    List<PricedItem> items =
        rawItems.stream()
            .map(
                item ->
                    new PricedItem(
                        item.skuId(),
                        item.skuCode(),
                        item.title(),
                        item.unitPrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        currency,
                        item.qty(),
                        item.unitPrice()
                            .multiply(BigDecimal.valueOf(item.qty()))
                            .setScale(MONEY_SCALE, RoundingMode.HALF_UP)))
            .toList();

    BigDecimal subtotal =
        items.stream().map(PricedItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

    if (effectiveDiscount.compareTo(subtotal) > 0) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "discount (" + effectiveDiscount + ") không được lớn hơn subtotal (" + subtotal + ")");
    }
    BigDecimal total =
        subtotal.subtract(effectiveDiscount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    return new Priced(items, subtotal, effectiveDiscount, total);
  }

  private OrderPricing() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
