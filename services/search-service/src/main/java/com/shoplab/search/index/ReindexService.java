package com.shoplab.search.index;

import com.shoplab.search.config.SearchProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reconcile: dựng lại index từ catalog (SL-3.6) — <b>đường lùi bắt buộc</b> của mọi index.
 *
 * <p>Vì sao phải có, dù đã có event: event chỉ đúng khi (a) không mất message, (b) không ai xoá tay
 * row index, (c) consumer không bỏ qua vì lý do gì. Cả ba đều có thể sai — nhất là khi test tay
 * hoặc khi đổi storage. Một hệ event-driven <b>không reconcile được</b> thì không được coi là
 * "xong".
 *
 * <p>Ba tính chất phải giữ:
 *
 * <ol>
 *   <li><b>Idempotent</b>: chạy 2 lần cho cùng kết quả (UPSERT theo `product_id`, `archived` chỉ
 *       đổi trạng thái). Không có INSERT trần ⇒ không thể nhân bản row.
 *   <li><b>Không archive oan</b>: chỉ archive row có `updated_at` cũ hơn mốc bắt đầu quét
 *       (watermark) — product tạo trong lúc quét chưa kịp vào kết quả, archive nó là tự bắn vào
 *       chân.
 *   <li><b>Không "thành công một nửa"</b>: catalog lỗi giữa đường thì exception lan ra, client nhận
 *       5xx và <b>không</b> có bước archive nào chạy.
 * </ol>
 */
@Service
public class ReindexService {

  private static final Logger LOG = LoggerFactory.getLogger(ReindexService.class);

  /**
   * Chặn vòng lặp vô hạn nếu catalog trả `totalPages` sai (1 triệu trang × 100 = 100 M product).
   */
  private static final int MAX_PAGES = 10_000;

  private final CatalogClient catalog;
  private final SearchRepository index;
  private final SearchProperties properties;
  private final Clock clock;

  public ReindexService(
      CatalogClient catalog, SearchRepository index, SearchProperties properties, Clock clock) {
    this.catalog = catalog;
    this.index = index;
    this.properties = properties;
    this.clock = clock;
  }

  public ReindexResult reindex() {
    Instant scanStartedAt = Instant.now(clock);
    Set<UUID> seen = new LinkedHashSet<>();
    int scanned = 0;
    int upserted = 0;

    int page = 0;
    int totalPages = 1;
    while (page < totalPages && page < MAX_PAGES) {
      CatalogClient.CatalogPage chunk = catalog.page(page, properties.catalogPageSize());
      totalPages = chunk.totalPages();
      if (chunk.content().isEmpty()) {
        break;
      }
      List<SearchProduct> products =
          chunk.content().stream().map(ReindexService::toIndexRow).toList();
      upserted += index.upsertBatch(products);
      scanned += products.size();
      products.forEach(product -> seen.add(product.productId()));
      page++;
    }

    int archived = index.archiveMissing(scanStartedAt, List.copyOf(seen), Instant.now(clock));
    LOG.info(
        "reindex: quét {} product · ghi {} row · archive {} row (catalog={}, batch={})",
        scanned,
        upserted,
        archived,
        properties.catalogUrl(),
        properties.catalogPageSize());
    return new ReindexResult(scanned, upserted, archived);
  }

  private static SearchProduct toIndexRow(CatalogClient.CatalogProduct product) {
    // Catalog list đã lọc product đã xoá mềm ⇒ mọi row đọc được đều ACTIVE. Lấy `status` từ payload
    // vẫn đúng hơn hard-code: contract là "trạng thái của product", không phải "trạng thái tôi
    // đoán".
    if (product.updatedAt() == null) {
      throw new CatalogClient.CatalogUnavailableException(
          "catalog trả product thiếu updatedAt: " + product.id());
    }
    return new SearchProduct(
        product.id(),
        product.name(),
        product.description(),
        product.categoryId(),
        product.price(),
        product.status() == null ? SearchProduct.ACTIVE : product.status(),
        Instant.parse(product.updatedAt()));
  }

  /**
   * @param scanned số product đọc được từ catalog
   * @param upserted số row index thực sự thay đổi
   * @param archived số row index bị đánh ARCHIVED vì catalog không còn
   */
  public record ReindexResult(int scanned, int upserted, int archived) {}
}
