package com.shoplab.search.index;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Áp event product vào index (SL-3.6) — nơi duy nhất dịch "sự kiện" thành "hàng dữ liệu trong
 * index".
 *
 * <p><b>Idempotent</b>: cùng một event xử lý 2 lần cho ra cùng một trạng thái (UPSERT theo
 * `product_id`) ⇒ at-least-once của broker không làm hỏng index. Đây là điều kiện để consumer chỉ
 * cần dedupe `eventId` ở tầng commons chứ không phải tự viết logic chống trùng.
 */
@Service
public class SearchIndexService {

  private static final Logger LOG = LoggerFactory.getLogger(SearchIndexService.class);

  private final SearchRepository index;

  public SearchIndexService(SearchRepository index) {
    this.index = index;
  }

  /** `product.updated.v1` (tạo mới + sửa đều dùng event này). */
  public void applyUpdated(Map<String, Object> payload) {
    SearchProduct product = SearchProduct.fromEvent(payload);
    index.upsert(product);
    LOG.info("index: upsert product={} status={}", product.productId(), product.status());
  }

  /**
   * `product.archived.v1` (soft delete của catalog).
   *
   * <p>{@code occurredAt} của envelope là phương án dự phòng khi payload thiếu `archivedAt` — index
   * phải ẩn product ngay cả khi producer cũ chỉ gửi id. Xoá cứng thì <b>không</b>: replay một event
   * `updated` cũ sẽ làm product sống lại (xem javadoc {@link SearchRepository#archive}).
   */
  public void applyArchived(Map<String, Object> payload, Instant occurredAt) {
    Object rawId = payload.get("productId");
    if (rawId == null) {
      throw new IllegalArgumentException("payload product.archived.v1 thiếu field: productId");
    }
    UUID productId = UUID.fromString(rawId.toString());
    Object rawAt = payload.get("archivedAt");
    Instant archivedAt = rawAt == null ? occurredAt : Instant.parse(rawAt.toString());
    int changed = index.archive(productId, archivedAt);
    LOG.info("index: archive product={} (row đổi: {})", productId, changed);
  }
}
