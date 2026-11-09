package com.shoplab.search.index;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation của index search (SL-3.6).
 *
 * <p><b>`search_vector` được ghi từ app, không phải generated column</b>: `unaccent()` không
 * IMMUTABLE nên Postgres từ chối `GENERATED ALWAYS AS ... STORED` (xem `V1__create_search.sql`). Hệ
 * quả phải nhớ: mọi chỗ ghi row phải tính lại vector — nên cả event và reindex dùng **chung một**
 * câu UPSERT ở class này, không có đường ghi thứ hai.
 */
@Repository
public class JdbcSearchRepository implements SearchRepository {

  /**
   * `WHERE search_product.updated_at <= EXCLUDED.updated_at` là chốt chống ghi đè bằng dữ liệu cũ
   * (event đến lệch thứ tự). `<=` (không phải `<`) để event trùng mốc vẫn ghi được — replay
   * idempotent phải cho ra cùng kết quả.
   */
  private static final String UPSERT =
      """
      INSERT INTO search_product
          (product_id, name, description, category_id, min_price, status, updated_at, search_vector)
      VALUES (?, ?, ?, ?, ?, ?, ?,
              to_tsvector('simple', unaccent(coalesce(?, '') || ' ' || coalesce(?, ''))))
      ON CONFLICT (product_id) DO UPDATE SET
          name          = EXCLUDED.name,
          description   = EXCLUDED.description,
          category_id   = EXCLUDED.category_id,
          min_price     = EXCLUDED.min_price,
          status        = EXCLUDED.status,
          updated_at    = EXCLUDED.updated_at,
          search_vector = EXCLUDED.search_vector
      WHERE search_product.updated_at <= EXCLUDED.updated_at
      """;

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  public JdbcSearchRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
    this.jdbc = jdbc;
    this.named = named;
  }

  @Override
  public void upsert(SearchProduct product) {
    jdbc.update(
        UPSERT,
        product.productId(),
        product.name(),
        product.description(),
        product.categoryId(),
        product.minPrice(),
        product.status(),
        Timestamp.from(product.updatedAt()),
        product.name(),
        product.description());
  }

  @Override
  public int upsertBatch(List<SearchProduct> products) {
    if (products.isEmpty()) {
      return 0;
    }
    int[] updated =
        jdbc.batchUpdate(
            UPSERT,
            new BatchPreparedStatementSetter() {
              @Override
              public void setValues(PreparedStatement ps, int i) throws SQLException {
                SearchProduct product = products.get(i);
                ps.setObject(1, product.productId());
                ps.setString(2, product.name());
                ps.setString(3, product.description());
                ps.setObject(4, product.categoryId());
                ps.setBigDecimal(5, product.minPrice());
                ps.setString(6, product.status());
                ps.setTimestamp(7, Timestamp.from(product.updatedAt()));
                ps.setString(8, product.name());
                ps.setString(9, product.description());
              }

              @Override
              public int getBatchSize() {
                return products.size();
              }
            });
    // batchUpdate trả 0 cho row bị chốt `updated_at` chặn ⇒ cộng lại để con số là "số row index
    // thật
    // sự đổi" (điều người vận hành quan tâm), không phải "số product đã đọc".
    int changed = 0;
    for (int count : updated) {
      changed += Math.max(count, 0);
    }
    return changed;
  }

  @Override
  public int archive(UUID productId, Instant archivedAt) {
    return jdbc.update(
        "UPDATE search_product SET status = ?, updated_at = ?"
            + " WHERE product_id = ? AND updated_at <= ?",
        SearchProduct.ARCHIVED,
        Timestamp.from(archivedAt),
        productId,
        Timestamp.from(archivedAt));
  }

  @Override
  public SearchPage<SearchItem> search(SearchQuery query) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder where = new StringBuilder(" WHERE status = 'ACTIVE'");
    if (query.categoryId() != null) {
      where.append(" AND category_id = :categoryId");
      params.addValue("categoryId", query.categoryId());
    }
    if (query.minPrice() != null) {
      where.append(" AND min_price >= :minPrice");
      params.addValue("minPrice", query.minPrice());
    }
    if (query.maxPrice() != null) {
      where.append(" AND min_price <= :maxPrice");
      params.addValue("maxPrice", query.maxPrice());
    }
    if (query.q() != null) {
      // `plainto_tsquery` nhận text thường (không phải cú pháp tsquery) ⇒ ký tự đặc biệt như
      // `%`/`_`
      // là vô hại, không cần escape như `LIKE`. `unaccent` ở cả hai phía để "dien thoai" khớp
      // "Điện thoại".
      where.append(" AND search_vector @@ plainto_tsquery('simple', unaccent(:q))");
      params.addValue("q", query.q());
    }

    long total = countOf("SELECT count(*) FROM search_product" + where, params);

    String order =
        query.q() == null
            ? " ORDER BY updated_at DESC"
            : " ORDER BY ts_rank(search_vector, plainto_tsquery('simple', unaccent(:q))) DESC,"
                + " updated_at DESC";
    params.addValue("limit", query.size());
    params.addValue("offset", query.offset());

    List<SearchItem> items =
        named.query(
            "SELECT product_id, name, category_id, min_price, updated_at FROM search_product"
                + where
                + order
                + " LIMIT :limit OFFSET :offset",
            params,
            ITEM_MAPPER);

    int totalPages = (int) ((total + query.size() - 1) / query.size());
    return new SearchPage<>(items, query.page(), query.size(), total, totalPages);
  }

  @Override
  public long countByStatus(String status) {
    Long count =
        jdbc.queryForObject(
            "SELECT count(*) FROM search_product WHERE status = ?", Long.class, status);
    return count == null ? 0 : count;
  }

  @Override
  public int archiveMissing(Instant scanStartedAt, List<UUID> seenProductIds, Instant now) {
    // Nhánh rỗng phải xử lý riêng: Spring mở rộng `NOT IN (:ids)` với collection rỗng thành
    // `NOT IN (NULL)` ⇒ KHÔNG row nào bị archive, trong khi ý nghĩa đúng là "catalog không còn
    // product
    // nào ⇒ index phải rỗng". Đúng kiểu lỗi "reconcile báo xong mà không làm gì".
    if (seenProductIds.isEmpty()) {
      return jdbc.update(
          "UPDATE search_product SET status = ?, updated_at = ?"
              + " WHERE status = 'ACTIVE' AND updated_at < ?",
          SearchProduct.ARCHIVED,
          Timestamp.from(now),
          Timestamp.from(scanStartedAt));
    }
    Map<String, Object> params = new HashMap<>();
    params.put("status", SearchProduct.ARCHIVED);
    params.put("now", Timestamp.from(now));
    params.put("scanStartedAt", Timestamp.from(scanStartedAt));
    params.put("seen", seenProductIds);
    return named.update(
        "UPDATE search_product SET status = :status, updated_at = :now"
            + " WHERE status = 'ACTIVE' AND updated_at < :scanStartedAt"
            + " AND product_id NOT IN (:seen)",
        params);
  }

  @Override
  public Optional<SearchProduct> findById(UUID productId) {
    List<SearchProduct> rows =
        jdbc.query(
            "SELECT product_id, name, description, category_id, min_price, status, updated_at"
                + " FROM search_product WHERE product_id = ?",
            PRODUCT_MAPPER,
            productId);
    return rows.stream().findFirst();
  }

  /**
   * Đọc `search_vector` dạng text — dùng cho test/verify để chắc vector được tính từ name +
   * description (một cột "có nhưng luôn NULL" là loại bug không nhìn thấy qua API).
   */
  public String searchVectorOf(UUID productId) {
    List<String> rows =
        jdbc.queryForList(
            "SELECT search_vector::text FROM search_product WHERE product_id = ?",
            String.class,
            productId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  private long countOf(String sql, MapSqlParameterSource params) {
    Long count = named.queryForObject(sql, params, Long.class);
    return count == null ? 0 : count;
  }

  private static final RowMapper<SearchItem> ITEM_MAPPER =
      (rs, rowNum) ->
          new SearchItem(
              rs.getObject("product_id", UUID.class),
              rs.getString("name"),
              rs.getObject("category_id", UUID.class),
              rs.getBigDecimal("min_price"),
              rs.getTimestamp("updated_at").toInstant());

  private static final RowMapper<SearchProduct> PRODUCT_MAPPER =
      (rs, rowNum) ->
          new SearchProduct(
              rs.getObject("product_id", UUID.class),
              rs.getString("name"),
              rs.getString("description"),
              rs.getObject("category_id", UUID.class),
              rs.getBigDecimal("min_price"),
              rs.getString("status"),
              rs.getTimestamp("updated_at").toInstant());
}
