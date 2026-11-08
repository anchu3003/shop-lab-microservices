package com.shoplab.catalog.sku;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả {@code GET /v1/skus?ids=...} (SL-2.13) — dữ liệu để bên gọi dựng dòng hàng / hiển thị giá
 * mà <b>không</b> phải gọi thêm request nào.
 *
 * <p><b>Vì sao có {@code missingIds} thay vì 404 cả request</b>: một SKU đã archive không nên làm
 * hỏng cả giỏ. Caller cần biết <i>cái nào</i> không mua được để báo đúng dòng cho user (409/422 kèm
 * chi tiết), còn các dòng còn lại vẫn dùng được.
 *
 * <p><b>Không trả {@code status}</b>: endpoint chỉ trả SKU <b>mua được</b> — trả thêm trạng thái là
 * mời client tự quyết một việc server đã quyết.
 *
 * <p><b>Không trả {@code currency}</b>: catalog Phase 2 chưa có khái niệm tiền tệ (giá là số
 * thuần); bên gọi áp currency của mình (`order-service` mặc định {@code VND}). Thêm khi làm
 * multi-currency.
 *
 * @param items SKU mua được, <b>đúng thứ tự id client gửi</b>
 * @param missingIds id không tồn tại hoặc không mua được (SKU/product đã archive) — cùng thứ tự
 */
public record SkuLookupResponse(List<SkuLine> items, List<UUID> missingIds) {

  /**
   * Một dòng giá.
   *
   * <p>Tên {@code SkuLine} chứ không phải {@code Item}/{@code Line}: spec OpenAPI merge mọi service
   * vào một file, tên ngắn chung chung sẽ đụng schema của service khác (bài học `SL-2.5`/`SL-2.1`).
   *
   * @param unitPrice giá bán thật = {@code sku.priceOverride} nếu có, ngược lại là giá product
   * @param title tên product — snapshot cho {@code order_items}
   */
  public record SkuLine(
      UUID skuId, UUID productId, String skuCode, String title, BigDecimal unitPrice) {}
}
