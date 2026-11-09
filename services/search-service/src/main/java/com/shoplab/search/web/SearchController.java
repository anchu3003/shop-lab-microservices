package com.shoplab.search.web;

import com.shoplab.search.index.SearchItem;
import com.shoplab.search.index.SearchPage;
import com.shoplab.search.index.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * `GET /v1/search` — <b>public</b> (guest tìm hàng như xem catalog; không có gì riêng tư trong
 * index).
 *
 * <p>Controller mỏng có chủ ý: parse/validate nằm ở {@link com.shoplab.search.index.SearchQuery},
 * query ở repository. Nhờ vậy toàn bộ logic của endpoint test được mà không cần HTTP.
 */
@RestController
@RequestMapping("/v1/search")
@Tag(name = "Search", description = "Tìm sản phẩm (PostgreSQL FTS) — Phase 3")
public class SearchController {

  private final SearchService search;

  public SearchController(SearchService search) {
    this.search = search;
  }

  @GetMapping
  @Operation(
      summary = "Tìm sản phẩm",
      description =
          "Chỉ trả product ACTIVE. `q` không phân biệt hoa/thường và không phân biệt dấu"
              + " (`dien thoai` khớp `Điện thoại`). Ký tự đặc biệt như `%` không gây lỗi.")
  public SearchPage<SearchItem> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String categoryId,
      @RequestParam(required = false) String minPrice,
      @RequestParam(required = false) String maxPrice,
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String size) {
    return search.search(q, categoryId, minPrice, maxPrice, page, size);
  }
}
