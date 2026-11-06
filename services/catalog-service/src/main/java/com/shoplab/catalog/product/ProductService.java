package com.shoplab.catalog.product;

import com.shoplab.catalog.category.CategoryRepository;
import com.shoplab.catalog.sku.SkuRepository;
import com.shoplab.catalog.web.PageResponse;
import com.shoplab.commons.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ product: tạo / đọc / sửa / xoá / list có filter (CRUD + truy vấn Phase 1). */
@Service
public class ProductService {

  private final ProductRepository products;
  private final CategoryRepository categories;
  private final SkuRepository skus;
  private final Clock clock;

  public ProductService(
      ProductRepository products, CategoryRepository categories, SkuRepository skus, Clock clock) {
    this.products = products;
    this.categories = categories;
    this.skus = skus;
    this.clock = clock;
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
    return ProductResponse.from(saved);
  }

  /**
   * @throws NotFoundException khi id không tồn tại
   */
  @Transactional(readOnly = true)
  public ProductResponse findById(UUID id) {
    return ProductResponse.from(require(id));
  }

  /**
   * Chi tiết product kèm SKU — dùng cho {@code GET /v1/products/{id}}.
   *
   * @throws NotFoundException khi id không tồn tại
   */
  @Transactional(readOnly = true)
  public ProductDetailResponse findDetail(UUID id) {
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
    return ProductResponse.from(products.saveAndFlush(product));
  }

  /**
   * @throws NotFoundException khi id không tồn tại
   */
  @Transactional
  public void delete(UUID id) {
    products.delete(require(id));
  }

  private Product require(UUID id) {
    return products
        .findById(id)
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
