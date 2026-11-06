package com.shoplab.catalog.product;

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
import org.springframework.web.bind.annotation.RestController;

/**
 * API product.
 *
 * <p>{@code GET} công khai (guest xem sản phẩm — lý do catalog là service read-heavy đầu tiên);
 * {@code POST}/{@code PUT}/{@code DELETE} chỉ {@code seller}/{@code admin}.
 */
@RestController
@RequestMapping("/v1/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  /** Tạo product (seller/admin) → 201 + {@code Location}. */
  @PostMapping
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    ProductResponse created = service.create(request);
    return ResponseEntity.created(URI.create("/v1/products/" + created.id())).body(created);
  }

  /** Chi tiết product (public). */
  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable UUID id) {
    return service.findById(id);
  }

  /** Sửa product (seller/admin) — thay toàn bộ field sửa được. */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ProductResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
    return service.update(id, request);
  }

  /** Xoá product (seller/admin) → 204. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
