package com.shoplab.catalog.outbox;

import com.shoplab.catalog.product.Product;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Điểm duy nhất ghi outbox của catalog (SL-3.6) — gọi từ **trong** transaction của {@code
 * ProductService} để thay đổi product và event nằm chung một lần commit (ADR-0007 D4).
 *
 * <p><b>Vì sao class này KHÔNG có {@code @Transactional}</b>: nó phải chạy trong transaction của
 * caller. Tự mở transaction riêng (hoặc `REQUIRES_NEW`) sẽ phá đúng bất biến cần bảo vệ: product đã
 * commit mà event thì chưa (hoặc ngược lại). Đây là lý do method ở đây cố ý "trần" — không tự quyết
 * định ranh giới transaction.
 *
 * <p>Không ném exception khi publish: publish là việc của relay (ngoài transaction). Ở đây chỉ ghi
 * dòng dữ liệu; DB lỗi thì cả transaction rollback — đúng ý.
 */
@Service
public class CatalogOutbox {

  private static final Logger LOG = LoggerFactory.getLogger(CatalogOutbox.class);

  private final OutboxRepository outbox;

  public CatalogOutbox(OutboxRepository outbox) {
    this.outbox = outbox;
  }

  /** Tạo mới hoặc sửa product ⇒ `product.updated.v1`. */
  public void productUpdated(Product product) {
    outbox.append(
        ProductEvents.AGGREGATE,
        product.id(),
        ProductEvents.UPDATED,
        ProductEvents.updated(product));
    LOG.info(
        "outbox: ghi {} cho product={} (cùng transaction)", ProductEvents.UPDATED, product.id());
  }

  /**
   * Soft delete ⇒ `product.archived.v1`.
   *
   * <p>Gọi kèm {@code archivedAt} tường minh (không đọc lại từ entity) để mốc thời gian trong event
   * **đúng bằng** mốc đã ghi vào {@code product.deleted_at} — hai nguồn khác nhau cho cùng một sự
   * kiện là cách chắc chắn nhất để chúng lệch.
   */
  public void productArchived(UUID productId, Instant archivedAt) {
    outbox.append(
        ProductEvents.AGGREGATE,
        productId,
        ProductEvents.ARCHIVED,
        ProductEvents.archived(productId, archivedAt));
    LOG.info("outbox: ghi {} cho product={} (cùng transaction)", ProductEvents.ARCHIVED, productId);
  }
}
