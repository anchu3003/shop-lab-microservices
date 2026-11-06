package com.shoplab.catalog.product;

import com.shoplab.catalog.web.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API product.
 *
 * <p>{@code GET} công khai (guest xem sản phẩm — lý do catalog là service read-heavy đầu tiên);
 * {@code POST}/{@code PUT}/{@code DELETE} chỉ {@code seller}/{@code admin}.
 */
@Tag(name = "Product")
@RestController
@RequestMapping("/v1/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  /**
   * List sản phẩm có filter + phân trang + sort (public).
   *
   * <p>Tham số để dạng {@code String} rồi parse ở {@link ProductQuery}: Spring tự trả 400 với thông
   * báo HTML-ish khi bind sai kiểu, còn ở đây ta trả {@code ApiError} thống nhất.
   */
  @GetMapping
  public PageResponse<ProductResponse> list(
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String size,
      @RequestParam(required = false) String categoryId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String sort) {
    return service.search(ProductQuery.parse(page, size, categoryId, q, sort));
  }

  /** Tạo product (seller/admin) → 201 + {@code Location}. */
  @PostMapping
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    ProductResponse created = service.create(request);
    return ResponseEntity.created(URI.create("/v1/products/" + created.id())).body(created);
  }

  /** Chi tiết product kèm SKU (public). */
  @GetMapping("/{id}")
  public ProductDetailResponse get(@PathVariable UUID id) {
    return service.findDetail(id);
  }

  /** Sửa product (seller/admin) — thay toàn bộ field sửa được. */
  @PutMapping("/{id}")
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ProductResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
    return service.update(id, request);
  }

  /** Xoá product (seller/admin) → 204. */
  @DeleteMapping("/{id}")
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
