package com.shoplab.search.web;

import com.shoplab.search.index.ReindexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * `POST /v1/admin/search/reindex` — dựng lại index từ catalog (SL-3.6).
 *
 * <p><b>Chỉ ADMIN</b>: đây là thao tác toàn hệ (đọc toàn bộ catalog, ghi toàn bộ index), chạy đồng
 * thời 2 lần sẽ nhân đôi tải lên cả hai service. Idempotent nên không hỏng dữ liệu, nhưng không
 * phải việc của user thường.
 *
 * <p>Không đặt dưới `/v1/products` hay `/v1/search`: nó là vận hành, không phải nghiệp vụ — và
 * gateway phải route riêng prefix `/v1/admin/search` (dài hơn `/v1/admin` của identity).
 */
@RestController
@RequestMapping("/v1/admin/search")
@Tag(name = "Search Admin", description = "Vận hành index — reconcile từ catalog")
public class ReindexController {

  private final ReindexService reindex;

  public ReindexController(ReindexService reindex) {
    this.reindex = reindex;
  }

  @PostMapping("/reindex")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Dựng lại index từ catalog",
      description =
          "Đọc toàn bộ product ACTIVE của catalog theo lô, UPSERT vào index, rồi archive các row"
              + " index không còn trong catalog. Idempotent: gọi nhiều lần cho cùng kết quả.")
  public ReindexService.ReindexResult reindex() {
    return reindex.reindex();
  }
}
