package com.shoplab.catalog.sku;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API tra giá nhiều SKU cùng lúc (SL-2.13).
 *
 * <p><b>Public</b> như {@code GET /v1/products}: giá niêm yết là thông tin công khai (đã hiện ở
 * trang chi tiết sản phẩm), và khách chưa đăng nhập vẫn phải thấy giá trong giỏ. Không có
 * {@code @PreAuthorize} nào ở đây vì đây là đường <b>đọc</b> thuần.
 */
@Tag(name = "SkuLookup")
@RestController
@RequestMapping("/v1/skus")
public class SkuLookupController {

  private final SkuLookupService service;

  public SkuLookupController(SkuLookupService service) {
    this.service = service;
  }

  /**
   * Tra giá/tên cho tối đa {@value SkuLookupQuery#MAX_IDS} SKU → 200.
   *
   * <p>Tham số nhận dạng {@code String} rồi parse ở {@link SkuLookupQuery} (giống {@code
   * ProductQuery}): Spring bind thẳng sang {@code List<UUID>} sẽ ném lỗi HTML-ish và trả 400 không
   * theo format {@code ApiError} thống nhất.
   */
  @GetMapping
  public SkuLookupResponse lookup(@RequestParam(required = false) String ids) {
    return service.lookup(SkuLookupQuery.parse(ids));
  }
}
