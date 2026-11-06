package com.shoplab.catalog.product;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository của {@link Product}: list + filter + phân trang (SL-1.6). */
public interface ProductRepository extends JpaRepository<Product, UUID> {

  /**
   * Truy vấn list: filter theo category và/hoặc tên (không phân biệt hoa/thường), sort + phân trang
   * do {@link Pageable} quyết định.
   *
   * <p>Vì sao viết {@code @Query} thay vì derived query: cần "bỏ qua filter nếu null" ({@code
   * :param is null or ...}) — derived query sẽ phải tách thành 4 method cho 4 tổ hợp filter.
   *
   * <p>Sort vẫn an toàn vì field/hướng đã được whitelist ở {@link ProductQuery} trước khi tới đây.
   */
  @Query(
      """
      select p from Product p
      where (:categoryId is null or p.categoryId = :categoryId)
        and (:q is null or lower(p.name) like :q)
      """)
  Page<Product> search(
      @Param("categoryId") UUID categoryId, @Param("q") String q, Pageable pageable);
}
