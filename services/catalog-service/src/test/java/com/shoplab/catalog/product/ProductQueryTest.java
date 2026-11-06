package com.shoplab.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

/**
 * Unit test cho phần parse/validate query string của {@code GET /v1/products}.
 *
 * <p>Đây là chỗ chặn input xấu trước khi tới SQL — test ở đây rẻ hơn nhiều so với test qua HTTP, và
 * là nơi dễ sinh bug nhất (parse số, whitelist sort, default).
 */
class ProductQueryTest {

  @Test
  @DisplayName("không truyền gì → page 0, size 20, sort createdAt desc, không filter")
  void shouldApplyDefaults() {
    ProductQuery query = ProductQuery.parse(null, null, null, null, null);

    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(ProductQuery.DEFAULT_SIZE);
    assertThat(query.categoryId()).isNull();
    assertThat(query.q()).isNull();
    assertThat(query.sortField()).isEqualTo("createdAt");
    assertThat(query.sortDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("q được lower-case + trim; categoryId parse UUID")
  void shouldNormalizeTermAndCategory() {
    UUID category = UUID.randomUUID();

    ProductQuery query = ProductQuery.parse("1", "5", " " + category + " ", "  IPHONE  ", null);

    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(5);
    assertThat(query.categoryId()).isEqualTo(category);
    assertThat(query.q()).isEqualTo("iphone");
  }

  @ParameterizedTest
  @CsvSource({
    "price,asc,price,ASC",
    "price,desc,price,DESC",
    "createdAt,asc,createdAt,ASC",
    "CREATEDAT,DESC,createdAt,DESC"
  })
  @DisplayName("sort hợp lệ: whitelist price/createdAt, hoa thường đều nhận")
  void shouldAcceptWhitelistedSort(
      String raw, String direction, String expectedField, Sort.Direction expectedDirection) {
    ProductQuery query = ProductQuery.parse(null, null, null, null, raw + "," + direction);

    assertThat(query.sortField()).isEqualTo(expectedField);
    assertThat(query.sortDirection()).isEqualTo(expectedDirection);
  }

  @Test
  @DisplayName("sort chỉ có field → mặc định ASC")
  void shouldDefaultDirectionToAsc() {
    ProductQuery query = ProductQuery.parse(null, null, null, null, "price");

    assertThat(query.sortField()).isEqualTo("price");
    assertThat(query.sortDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @ParameterizedTest
  @ValueSource(strings = {"drop table", "name", "price,sideways", "price,asc,desc", "id"})
  @DisplayName("sort không thuộc whitelist / hướng sai / quá 2 phần → 400 (không lọt vào SQL)")
  void shouldRejectInvalidSort(String raw) {
    assertThatThrownBy(() -> ProductQuery.parse(null, null, null, null, raw))
        .isInstanceOf(InvalidQueryException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "101", "1000", "-5"})
  @DisplayName("size ngoài 1..100 → 400 (KHÔNG âm thầm clamp)")
  void shouldRejectOutOfRangeSize(String raw) {
    assertThatThrownBy(() -> ProductQuery.parse(null, raw, null, null, null))
        .isInstanceOf(InvalidQueryException.class)
        .hasMessageContaining("size");
  }

  @Test
  @DisplayName("size = 100 (đúng biên) → hợp lệ")
  void shouldAcceptMaxSize() {
    assertThat(ProductQuery.parse(null, "100", null, null, null).size()).isEqualTo(100);
  }

  @Test
  @DisplayName("page âm hoặc không phải số → 400")
  void shouldRejectInvalidPage() {
    assertThatThrownBy(() -> ProductQuery.parse("-1", null, null, null, null))
        .isInstanceOf(InvalidQueryException.class)
        .hasMessageContaining("page");
    assertThatThrownBy(() -> ProductQuery.parse("abc", null, null, null, null))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  @DisplayName("categoryId không phải UUID → 400")
  void shouldRejectInvalidCategoryId() {
    assertThatThrownBy(() -> ProductQuery.parse(null, null, "khong-phai-uuid", null, null))
        .isInstanceOf(InvalidQueryException.class)
        .hasMessageContaining("categoryId");
  }

  @Test
  @DisplayName("q dài quá 100 ký tự → 400 (chặn query rác)")
  void shouldRejectTooLongTerm() {
    String term = "a".repeat(101).toUpperCase(Locale.ROOT);

    assertThatThrownBy(() -> ProductQuery.parse(null, null, null, term, null))
        .isInstanceOf(InvalidQueryException.class)
        .hasMessageContaining("q tối đa");
  }
}
