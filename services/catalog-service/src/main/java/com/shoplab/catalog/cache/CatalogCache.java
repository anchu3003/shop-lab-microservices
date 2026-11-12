package com.shoplab.catalog.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Vô hiệu hoá cache khi dữ liệu catalog đổi (ADR-0009 D4).
 *
 * <p><b>Một chỗ duy nhất</b> để trả lời "ghi cái này thì phải xoá key nào". Rải logic này vào 3
 * service là cách chắc chắn nhất để quên một đường ghi ⇒ bán giá cũ / hiện sản phẩm đã xoá.
 *
 * <p>Hai kiểu vô hiệu, vì hai bài toán khác nhau:
 *
 * <ul>
 *   <li><b>Xoá trực tiếp</b> ({@code detail}, {@code sku}): biết chính xác key (theo id) ⇒ xoá đúng
 *       nó, hit ratio của các key khác không bị ảnh hưởng.
 *   <li><b>Bump generation</b> ({@code list}): tổ hợp {@code page × size × category × q × sort} là
 *       vô hạn ⇒ không thể liệt kê. Một {@code INCR} làm toàn bộ list cũ vô nghĩa, chúng chết dần
 *       theo TTL.
 * </ul>
 *
 * <p>Lỗi Redis ở đây là <b>không nghiêm trọng</b>: cache cũng đang không dùng được. Điều tệ nhất là
 * phục vụ dữ liệu cũ tới hết TTL (≤ 66 s với TTL 60 s + jitter 10%) — nên chỉ log DEBUG, không được
 * làm hỏng request ghi.
 */
@Component
public class CatalogCache {

  private static final Logger LOG = LoggerFactory.getLogger(CatalogCache.class);

  private final CacheStore store;
  private final CacheKeys keys;
  private final CacheMetrics metrics;

  public CatalogCache(CacheStore store, CacheKeys keys, CacheMetrics metrics) {
    this.store = store;
    this.keys = keys;
    this.metrics = metrics;
  }

  /**
   * Product đổi (update / archive): detail của nó + <b>mọi</b> dòng SKU của nó (tên/giá nằm trong
   * dòng SKU) + list (mọi trang).
   *
   * @param skuIds SKU của product — phải lấy <b>trước</b> khi archive, vì archive là bulk update
   *     {@code status} và sau đó vẫn đọc được nhưng lấy sớm cho rõ ý định
   */
  public void productChanged(UUID productId, Collection<UUID> skuIds) {
    List<String> targets = new ArrayList<>(skuIds.size() + 1);
    targets.add(keys.detail(productId));
    for (UUID skuId : skuIds) {
      targets.add(keys.sku(skuId));
    }
    invalidate(targets);
    bumpListGeneration();
  }

  /**
   * Chỉ detail đổi (thêm SKU): detail chứa danh sách SKU nên phải xoá; list <b>không</b> chứa SKU ⇒
   * không cần bump generation.
   */
  public void productDetailChanged(UUID productId) {
    invalidate(List.of(keys.detail(productId)));
  }

  /** Bump generation của list — 1 lệnh {@code INCR}, không phụ thuộc số key đang có. */
  public void bumpListGeneration() {
    try {
      store.increment(keys.listGeneration());
    } catch (CacheUnavailableException ex) {
      metrics.bypass("invalidate");
      LOG.debug("cache: không bump được generation của list ({})", ex.getMessage());
    }
  }

  /** Danh sách category đổi (chỉ có đường tạo mới — chưa có update/delete). */
  public void categoriesChanged() {
    invalidate(List.of(keys.categories()));
  }

  private void invalidate(Collection<String> targets) {
    try {
      store.deleteAll(targets);
    } catch (CacheUnavailableException ex) {
      metrics.bypass("invalidate");
      LOG.debug("cache: không xoá được {} key ({})", targets.size(), ex.getMessage());
    }
  }
}
