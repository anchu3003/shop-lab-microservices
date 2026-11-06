package com.shoplab.catalog.sku;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API SKU — lồng trong product vì SKU không tồn tại độc lập.
 *
 * <p>Chỉ {@code seller}/{@code admin} được tạo (giống tạo product). Đọc SKU nằm trong {@code GET
 * /v1/products/{id}} (danh sách {@code skus}) để guest chỉ cần 1 request.
 */
@Tag(name = "SKU")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/products/{productId}/skus")
public class SkuController {

  private final SkuService service;

  public SkuController(SkuService service) {
    this.service = service;
  }

  /** Tạo SKU (seller/admin) → 201 + {@code Location}. */
  @PostMapping
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<SkuResponse> create(
      @PathVariable UUID productId, @Valid @RequestBody CreateSkuRequest request) {
    SkuResponse created = service.create(productId, request);
    return ResponseEntity.created(URI.create("/v1/products/" + productId + "/skus/" + created.id()))
        .body(created);
  }
}
