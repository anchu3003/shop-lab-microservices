package com.shoplab.inventory.stock;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API tồn kho phía admin/đọc.
 *
 * <p>{@code PUT} chỉ {@code admin}: Phase 2 inventory <b>không biết</b> SKU thuộc seller nào (dữ
 * liệu của catalog) ⇒ không thể cho seller tự sửa tồn kho của mình mà không kiểm chủ sở hữu. Đây là
 * chỗ sẽ mở rộng khi có luồng "seller nhập kho" thật (cần catalog trả seller_id hoặc event).
 *
 * <p>{@code GET} cần token (mọi role): số liệu vận hành, không phải dữ liệu công khai.
 */
@Tag(name = "Stock")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/stock")
public class StockController {

  private final StockService service;

  public StockController(StockService service) {
    this.service = service;
  }

  /** Đặt tồn kho thật (admin) → 200 kèm trạng thái sau khi đặt. */
  @PutMapping("/{skuId}")
  @PreAuthorize("hasRole('ADMIN')")
  public StockResponse setStock(
      @PathVariable UUID skuId, @Valid @RequestBody SetStockRequest request) {
    return StockResponse.from(service.setOnHand(skuId, request.onHand()));
  }

  /** Xem tồn kho của 1 SKU (mọi role đã đăng nhập). */
  @GetMapping("/{skuId}")
  public StockResponse getStock(@PathVariable UUID skuId) {
    return StockResponse.from(service.snapshot(skuId));
  }
}
