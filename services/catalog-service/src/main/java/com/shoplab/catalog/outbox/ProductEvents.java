package com.shoplab.catalog.outbox;

import com.shoplab.catalog.product.Product;
import com.shoplab.commons.events.EventTopology;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payload của event do `catalog-service` phát (SL-3.6) — **chỉ field consumer cần**.
 *
 * <p>Consumer hiện tại là `search-service` (index sản phẩm), nhưng payload **không** được thiết kế
 * riêng cho nó: đổi engine search ở Phase 8 (OpenSearch) thì contract này giữ nguyên — đó là toàn
 * bộ mục tiêu của phase. Vì vậy payload chứa đúng dữ liệu của **product** (nguồn sự thật), không
 * chứa "cách index".
 *
 * <p>Cố ý **không** có SKU/giá theo SKU: `sku.price_override` là dữ liệu con, thêm vào đây là biến
 * event product thành bản sao của nửa catalog và mỗi lần sửa SKU lại phải phát lại event product.
 */
public final class ProductEvents {

  /** Aggregate dùng chung cho cả hai event — truy vết outbox theo product. */
  public static final String AGGREGATE = "product";

  /**
   * Tạo mới **và** sửa đều phát event này: consumer chỉ cần trạng thái cuối (upsert idempotent).
   */
  public static final String UPDATED = EventTopology.ROUTING_PRODUCT_UPDATED;

  /** Soft delete ⇒ index phải chuyển `ARCHIVED` (không xoá cứng — cùng triết lý với catalog). */
  public static final String ARCHIVED = EventTopology.ROUTING_PRODUCT_ARCHIVED;

  public static Map<String, Object> updated(Product product) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("productId", product.id().toString());
    payload.put("name", product.name());
    payload.put("description", product.description());
    payload.put("categoryId", product.categoryId().toString());
    payload.put("price", product.price());
    payload.put("status", product.status().name());
    payload.put("updatedAt", product.updatedAt().toString());
    return payload;
  }

  /**
   * Chỉ cần id + mốc thời gian: consumer đánh dấu row index là `ARCHIVED` và **không** xoá row (nếu
   * xoá thì lần replay một event cũ sẽ "hồi sinh" sản phẩm đã bị xoá).
   */
  public static Map<String, Object> archived(UUID productId, Instant archivedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("productId", productId.toString());
    payload.put("archivedAt", archivedAt.toString());
    return payload;
  }

  private ProductEvents() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
