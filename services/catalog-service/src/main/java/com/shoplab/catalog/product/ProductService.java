package com.shoplab.catalog.product;

import com.shoplab.catalog.cache.CatalogCache;
import com.shoplab.catalog.category.CategoryRepository;
import com.shoplab.catalog.outbox.CatalogOutbox;
import com.shoplab.catalog.sku.Sku;
import com.shoplab.catalog.sku.SkuRepository;
import com.shoplab.catalog.sku.SkuStatus;
import com.shoplab.catalog.web.PageResponse;
import com.shoplab.commons.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ product: tạo / đọc / sửa / xoá / list có filter (CRUD + truy vấn Phase 1).
 *
 * <p><b>SL-4.2</b>: 2 đường ĐỌC ({@code findDetail}, {@code search}) đi qua cache-aside (ADR-0009);
 * 3 đường GHI vô hiệu hoá cache tương ứng. Đường ghi <b>không</b> đọc cache — dữ liệu trả về sau
 * ghi luôn lấy từ entity vừa ghi, nên không có chuyện response "cũ" sau update.
 */
@Service
public class ProductService {

  private final ProductRepository products;
  private final CategoryRepository categories;
  private final SkuRepository skus;
  private final CatalogOutbox outbox;
  private final Clock clock;
  private final ProductCache cache;
  private final CatalogCache catalogCache;

  public ProductService(
      ProductRepository products,
      CategoryRepository categories,
      SkuRepository skus,
      CatalogOutbox outbox,
      Clock clock,
      ProductCache cache,
      CatalogCache catalogCache) {
    this.products = products;
    this.categories = categories;
    this.skus = skus;
    this.outbox = outbox;
    this.clock = clock;
    this.cache = cache;
    this.catalogCache = catalogCache;
  }

  /**
   * Tạo product.
   *
   * @throws NotFoundException khi {@code categoryId} không tồn tại (404 — tham chiếu tới tài nguyên
   *     không có thật, không phải lỗi cú pháp)
   */
  @Transactional
  public ProductResponse create(CreateProductRequest request) {
    if (!categories.existsById(request.categoryId())) {
      throw new NotFoundException("Category không tồn tại: " + request.categoryId());
    }
    Product saved =
        products.save(
            Product.create(
                request.categoryId(),
                request.name().trim(),
                normalize(request.description()),
                request.price(),
                Instant.now(clock)));
    // SL-3.6: event `product.updated.v1` ghi TRONG cùng transaction với INSERT product.
    outbox.productUpdated(saved);
    // SL-4.2: product mới phải xuất hiện trong list ngay ⇒ bump generation (xoá từng key list là
    // bất khả thi — tổ hợp page/size/category/q/sort là vô hạn).
    catalogCache.bumpListGeneration();
    return ProductResponse.from(saved);
  }

  /**
   * Chi tiết product kèm SKU — dùng cho {@code GET /v1/products/{id}}.
   *
   * <p>Cache theo id (key {@code catalog:v1:product:<id>}): mọi client xem cùng một sản phẩm dùng
   * chung key ⇒ hit ratio cao và invalidation chính xác (update product biết phải xoá key nào).
   *
   * @throws NotFoundException khi id không tồn tại <b>hoặc</b> đã bị xoá mềm (`SL-2.11`)
   */
  @Transactional(readOnly = true)
  public ProductDetailResponse findDetail(UUID id) {
    // 404 KHÔNG được cache: lỗi không phải dữ liệu, và cache lỗi sẽ biến một lần đọc sai thành 404
    // tồn tại 60 s sau khi product được tạo/xoá lại.
    return cache.detail(id, () -> loadDetail(id));
  }

  private ProductDetailResponse loadDetail(UUID id) {
    Product product = require(id);
    return ProductDetailResponse.from(product, skus.findByProductIdOrderByCodeAsc(id));
  }

  /**
   * List có filter + phân trang + sort (public cho guest).
   *
   * <p>Sort lấy từ {@link ProductQuery} — đã validate whitelist, nên {@code Sort.by} ở đây không
   * thể nhận field lạ từ client.
   */
  @Transactional(readOnly = true)
  public PageResponse<ProductResponse> search(ProductQuery query) {
    return cache.list(query, () -> loadList(query));
  }

  private PageResponse<ProductResponse> loadList(ProductQuery query) {
    Pageable pageable =
        PageRequest.of(
            query.page(), query.size(), Sort.by(query.sortDirection(), query.sortField()));
    // `q` đã được lower-case ở ProductQuery ⇒ so với `lower(p.name)` trong câu query.
    String like = query.q() == null ? null : "%" + query.q() + "%";

    Page<Product> found = products.search(query.categoryId(), like, pageable);
    return new PageResponse<>(
        found.getContent().stream().map(ProductResponse::from).toList(),
        found.getNumber(),
        found.getSize(),
        found.getTotalElements(),
        found.getTotalPages());
  }

  /**
   * @throws NotFoundException khi id không tồn tại
   */
  @Transactional
  public ProductResponse update(UUID id, UpdateProductRequest request) {
    Product product = require(id);
    product.update(
        request.name().trim(),
        normalize(request.description()),
        request.price(),
        Instant.now(clock));
    // Không cần save(): entity đang managed trong transaction, Hibernate flush khi commit.
    // NHƯNG phải flush TRƯỚC khi đọc `version`: optimistic locking tăng version ở thời điểm flush,
    // không phải lúc gán field ⇒ không flush thì response trả version CŨ (0) và client sẽ update
    // bằng version sai ở lần sau (409 không đáng có). Bug này bắt được ở bước verify SL-1.5.
    Product updated = products.saveAndFlush(product);
    // saveAndFlush TRƯỚC outbox: event phải mang `updatedAt`/`version` đã thật sự ghi xuống DB.
    outbox.productUpdated(updated);
    // SL-4.2: tên/giá nằm trong cả detail LẪN dòng SKU (giá SKU = priceOverride ?? product.price) ⇒
    // phải xoá cả hai. Lấy SKU ngay tại đây thay vì "xoá theo pattern": pattern là SCAN, không
    // được.
    catalogCache.productChanged(id, skuIdsOf(id));
    return ProductResponse.from(updated);
  }

  /**
   * Xoá mềm product (SL-2.11, ADR-0006 D5): đánh dấu {@code ARCHIVED} + ghi {@code deletedAt}, và
   * archive luôn SKU của nó. <b>Không</b> xoá row — Phase 2 còn tham chiếu {@code sku_id} từ
   * inventory / order.
   *
   * <p>Vì sao không {@code products.delete(...)} nữa: `sku.product_id` có {@code ON DELETE CASCADE}
   * ⇒ xoá product là mất luôn SKU, kéo theo mất khả năng reconcile tồn kho (`TD-12`).
   *
   * @throws NotFoundException khi id không tồn tại hoặc đã bị xoá mềm (DELETE lần 2 ⇒ 404)
   */
  @Transactional
  public void delete(UUID id) {
    Product product = require(id);
    Instant now = Instant.now(clock);
    // SL-4.2: lấy id SKU TRƯỚC khi archive — cần biết xoá những key SKU nào. (Bulk update bên dưới
    // không trả về danh sách id đã đổi.)
    List<UUID> skuIds = skuIdsOf(id);
    product.archive(now);
    // Bulk update: 1 câu SQL cho mọi SKU của product, không load entity (xem SkuRepository).
    skus.updateStatusByProductId(id, SkuStatus.ARCHIVED);
    // SL-3.6: `product.archived.v1` cùng transaction ⇒ index search không thể "sót" product đã
    // xoá nếu catalog đã commit (chỉ có thể trễ, và trễ thì reconcile dựng lại được).
    outbox.productArchived(id, now);
    // SL-4.2: nếu không xoá, cache sẽ phục vụ product đã xoá thêm tới hết TTL — sai nghiệp vụ,
    // không
    // chỉ sai số.
    catalogCache.productChanged(id, skuIds);
  }

  /** Id các SKU của product — dùng cho invalidation cache theo từng SKU. */
  private List<UUID> skuIdsOf(UUID productId) {
    return skus.findByProductIdOrderByCodeAsc(productId).stream().map(Sku::id).toList();
  }

  /**
   * Chỉ trả product <b>còn sống</b> — product đã xoá mềm coi như không tồn tại với mọi đường API.
   */
  private Product require(UUID id) {
    return products
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("Product không tồn tại: " + id));
  }

  /** Chuỗi toàn khoảng trắng coi như không có mô tả (tránh lưu " " vào DB). */
  private static String normalize(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }
}
