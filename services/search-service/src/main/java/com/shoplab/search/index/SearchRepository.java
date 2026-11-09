package com.shoplab.search.index;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập bảng `search_product` (SL-3.6).
 *
 * <p>JDBC chứ không JPA: toàn bộ phần khó ở đây là `tsvector` + GIN + `plainto_tsquery` và UPSERT
 * có điều kiện — JPA không diễn đạt được gọn, và index search không phải aggregate cần dirty
 * checking.
 */
public interface SearchRepository {

  /**
   * Ghi đè một dòng index.
   *
   * <p><b>Bảo vệ thứ tự</b>: nếu row hiện có `updated_at` <b>mới hơn</b> dữ liệu đang ghi thì bỏ
   * qua. At-least-once + retry đồng nghĩa hai event của cùng product có thể đến lệch thứ tự; không
   * có chốt này thì một event cũ đến muộn sẽ "hồi sinh" giá/tên cũ trong index.
   */
  void upsert(SearchProduct product);

  /** Như {@link #upsert} nhưng theo lô — dùng cho reindex (mỗi lô 100 product). */
  int upsertBatch(List<SearchProduct> products);

  /**
   * Đánh dấu ARCHIVED. <b>Không</b> xoá row: xoá rồi thì replay một event cũ sẽ làm product sống
   * lại (và không còn dấu vết nào để biết index từng có product đó).
   *
   * @return số row thay đổi (0 = product chưa từng được index, không có gì để ẩn)
   */
  int archive(UUID productId, Instant archivedAt);

  /** Query FTS + filter, chỉ trả {@code ACTIVE}. */
  SearchPage<SearchItem> search(SearchQuery query);

  long countByStatus(String status);

  /**
   * Archive các row {@code ACTIVE} không còn trong catalog (reconcile).
   *
   * @param scanStartedAt mốc bắt đầu quét — chỉ archive row có `updated_at` < mốc này. Không có
   *     watermark thì một product tạo **trong lúc** đang quét (chưa kịp nằm trong kết quả quét) sẽ
   *     bị archive oan
   * @param seenProductIds mọi product đã thấy trong lần quét (rỗng = catalog không còn product nào)
   * @return số row đã archive
   */
  int archiveMissing(Instant scanStartedAt, List<UUID> seenProductIds, Instant now);

  Optional<SearchProduct> findById(UUID productId);
}
