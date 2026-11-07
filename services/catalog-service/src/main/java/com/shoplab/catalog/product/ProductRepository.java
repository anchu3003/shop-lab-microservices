package com.shoplab.catalog.product;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository của {@link Product}: list + filter + phân trang (SL-1.6) + soft delete (SL-2.11). */
public interface ProductRepository extends JpaRepository<Product, UUID> {

  /**
   * Đọc product <b>còn sống</b> (SL-2.11).
   *
   * <p>Vì sao tách method thay vì lọc bằng {@code if} trong service: chỉ cần quên 1 chỗ là API trả
   * về product đã xoá (bug im lặng). Dùng method có {@code DeletedAtIsNull} trong tên thì chỗ nào
   * quên sẽ lộ ra ngay khi review.
   */
  Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

  /** Kiểm tra product còn sống — dùng trước khi ghi dữ liệu con (SKU). */
  boolean existsByIdAndDeletedAtIsNull(UUID id);

  /**
   * Truy vấn list: filter theo category và/hoặc tên (không phân biệt hoa/thường), sort + phân trang
   * do {@link Pageable} quyết định. Luôn loại product đã xoá mềm.
   *
   * <p>Vì sao viết {@code @Query} thay vì derived query: cần "bỏ qua filter nếu null" ({@code
   * :param is null or ...}) — derived query sẽ phải tách thành 4 method cho 4 tổ hợp filter.
   *
   * <p>Sort vẫn an toàn vì field/hướng đã được whitelist ở {@link ProductQuery} trước khi tới đây.
   */
  @Query(
      """
      select p from Product p
      where p.deletedAt is null
        and (:categoryId is null or p.categoryId = :categoryId)
        and (:q is null or lower(p.name) like :q)
      """)
  Page<Product> search(
      @Param("categoryId") UUID categoryId, @Param("q") String q, Pageable pageable);
}
