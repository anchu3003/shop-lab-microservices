package com.shoplab.catalog.category;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API category.
 *
 * <p>Đọc công khai (guest duyệt được), ghi chỉ {@code seller}/{@code admin} — phân quyền khai bằng
 * {@code @PreAuthorize} vì cùng path khác HTTP method.
 */
@Tag(name = "Category")
@RestController
@RequestMapping("/v1/categories")
public class CategoryController {

  private final CategoryService service;

  public CategoryController(CategoryService service) {
    this.service = service;
  }

  /** Tạo category (seller/admin) → 201 + {@code Location}. */
  @PostMapping
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<CategoryResponse> create(
      @Valid @RequestBody CreateCategoryRequest request) {
    CategoryResponse created = service.create(request);
    return ResponseEntity.created(URI.create("/v1/categories/" + created.id())).body(created);
  }

  /** Danh sách category (public). */
  @GetMapping
  public List<CategoryResponse> list() {
    return service.findAll();
  }
}
