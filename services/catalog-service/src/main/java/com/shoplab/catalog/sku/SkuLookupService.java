package com.shoplab.catalog.sku;

import com.shoplab.catalog.cache.SkuLineCache;
import com.shoplab.catalog.product.Product;
import com.shoplab.catalog.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra <b>nhiều</b> SKU trong một lượt (SL-2.13) — đường mà saga checkout và giỏ hàng dùng để lấy
 * giá thật.
 *
 * <p><b>Vì sao là batch</b>: giỏ 10 dòng khác product = 10 lần {@code GET /v1/products/{id}} nếu
 * không có endpoint này, và mỗi round-trip cộng thẳng vào p99 checkout (NFR &lt; 500 ms). Batch là
 * 2 câu query cố định, không phụ thuộc số dòng.
 *
 * <p><b>SL-4.2</b>: kết quả cache <b>theo từng SKU</b> (ADR-0009 D3) — 1 {@code MGET} thay cho
 * query, phần thiếu mới xuống DB.
 *
 * <p><b>Bất biến</b>: chỉ trả SKU <b>mua được</b> — SKU {@code ARCHIVED} và SKU của product đã xoá
 * mềm bị loại (ADR-0006 D5). SKU còn lại trả giá {@code priceOverride ?? product.price}, đúng bất
 * biến của {@link Sku} ("null = dùng giá product", cố ý không copy giá ⇒ không có nguồn sự thật thứ
 * hai).
 */
@Service
public class SkuLookupService {

  private final SkuRepository skus;
  private final ProductRepository products;
  private final SkuLineCache cache;

  public SkuLookupService(SkuRepository skus, ProductRepository products, SkuLineCache cache) {
    this.skus = skus;
    this.products = products;
    this.cache = cache;
  }

  /** Tra giá/tên cho danh sách id (đã validate ở {@link SkuLookupQuery}). */
  @Transactional(readOnly = true)
  public SkuLookupResponse lookup(SkuLookupQuery query) {
    SkuLineCache.Batch batch = cache.load(query.ids(), this::loadFromDb);

    // Thứ tự trả PHẢI theo thứ tự client hỏi: cart/saga ghép giá theo vị trí trong giỏ. Cache trả
    // Map nên nếu lặp theo Map.keySet() thì thứ tự đổi mỗi lần — bug chỉ lộ khi giỏ > 1 dòng.
    List<SkuLookupResponse.SkuLine> items = new ArrayList<>(batch.lines().size());
    List<UUID> missing = new ArrayList<>();
    for (UUID id : query.ids()) {
      SkuLookupResponse.SkuLine line = batch.lines().get(id);
      if (line == null) {
        missing.add(id);
      } else {
        items.add(line);
      }
    }
    return new SkuLookupResponse(List.copyOf(items), List.copyOf(missing));
  }

  /**
   * Nguồn sự thật cho tập id cần tra: 2 query cố định (SKU + product sống) rồi map sang dòng bán
   * được.
   */
  private SkuLineCache.Batch loadFromDb(List<UUID> ids) {
    List<Sku> found = skus.findByIdInAndStatus(ids, SkuStatus.ACTIVE);
    Map<UUID, Product> liveProducts = loadLiveProducts(found);

    Map<UUID, SkuLookupResponse.SkuLine> lines = new HashMap<>(found.size());
    // Mặc định "không mua được"; id nào map được sang dòng thì bỏ ra. Nhờ vậy id không tồn tại cũng
    // được cache sentinel (client hỏi id sai không đập DB mỗi lần).
    Set<UUID> notBuyable = new HashSet<>(ids);
    for (Sku sku : found) {
      // SKU ACTIVE nhưng product đã xoá mềm: SL-2.11 archive SKU cùng lúc, đây là chốt chặn thứ
      // hai — nếu một migration/script sau này bỏ bước đó, API vẫn không bán SKU của product đã
      // xoá.
      Product product = liveProducts.get(sku.productId());
      if (product != null) {
        lines.put(sku.id(), toLine(sku, product));
        notBuyable.remove(sku.id());
      }
    }
    return new SkuLineCache.Batch(lines, notBuyable);
  }

  /** 1 query cho N product; không gọi khi không có SKU nào (tránh query rỗng). */
  private Map<UUID, Product> loadLiveProducts(List<Sku> found) {
    if (found.isEmpty()) {
      return Map.of();
    }
    List<UUID> productIds = found.stream().map(Sku::productId).distinct().toList();
    Map<UUID, Product> byId = new HashMap<>();
    for (Product product : products.findByIdInAndDeletedAtIsNull(productIds)) {
      byId.put(product.id(), product);
    }
    return byId;
  }

  private static SkuLookupResponse.SkuLine toLine(Sku sku, Product product) {
    BigDecimal unitPrice = sku.priceOverride() == null ? product.price() : sku.priceOverride();
    return new SkuLookupResponse.SkuLine(
        sku.id(), product.id(), sku.code(), product.name(), unitPrice);
  }
}
