package com.shoplab.search.index;

import com.shoplab.search.config.SearchProperties;
import org.springframework.stereotype.Service;

/**
 * Đường đọc của search (SL-3.6) — validate + query. Không có nghiệp vụ gì ngoài "lọc theo trạng
 * thái sống": index chỉ chứa bản sao, mọi luật nghiệp vụ (giá, tồn kho) thuộc service khác.
 */
@Service
public class SearchService {

  private final SearchRepository index;
  private final SearchProperties properties;

  public SearchService(SearchRepository index, SearchProperties properties) {
    this.index = index;
    this.properties = properties;
  }

  /**
   * @param q từ khoá thô từ query string (chưa validate)
   * @throws SearchQuery.InvalidQueryException khi tham số sai (→ 400 qua `GlobalExceptionHandler`)
   */
  public SearchPage<SearchItem> search(
      String q, String categoryId, String minPrice, String maxPrice, String page, String size) {
    SearchQuery query =
        SearchQuery.parse(
            q, categoryId, minPrice, maxPrice, page, size, properties.maxQueryLength());
    return index.search(query);
  }
}
