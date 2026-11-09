package com.shoplab.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validate tham số `GET /v1/search` — ma trận nhỏ nhưng là toàn bộ "hợp đồng" của endpoint với
 * client. Sai ở đây là 400 không rõ nghĩa hoặc (tệ hơn) một query chạy full scan.
 */
class SearchQueryTest {

  private static final int MAX_Q = 100;

  private static SearchQuery parse(String q, String size) {
    return SearchQuery.parse(q, null, null, null, null, size, MAX_Q);
  }

  @Test
  @DisplayName("mặc định: page=0, size=20, không filter")
  void defaults() {
    SearchQuery query = SearchQuery.parse(null, null, null, null, null, null, MAX_Q);

    assertThat(query.q()).isNull();
    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.offset()).isZero();
  }

  @Test
  @DisplayName("q rỗng/toàn khoảng trắng = không lọc; q có nội dung thì trim")
  void termNormalization() {
    assertThat(parse("   ", null).q()).isNull();
    assertThat(parse("  dien thoai  ", null).q()).isEqualTo("dien thoai");
  }

  @Test
  @DisplayName("q quá dài → 400 (mỗi query FTS đều tốn CPU, không nhận chuỗi vô hạn)")
  void tooLongTermRejected() {
    assertThatThrownBy(() -> parse("x".repeat(MAX_Q + 1), null))
        .isInstanceOf(SearchQuery.InvalidQueryException.class)
        .hasMessageContaining("q tối đa");
  }

  @Test
  @DisplayName("size ngoài 1..100 hoặc page âm → 400, KHÔNG âm thầm clamp")
  void pagingRejected() {
    assertThatThrownBy(() -> parse(null, "0"))
        .isInstanceOf(SearchQuery.InvalidQueryException.class);
    assertThatThrownBy(() -> parse(null, "101"))
        .isInstanceOf(SearchQuery.InvalidQueryException.class);
    assertThatThrownBy(() -> SearchQuery.parse(null, null, null, null, "-1", null, MAX_Q))
        .isInstanceOf(SearchQuery.InvalidQueryException.class);
    assertThatThrownBy(() -> parse(null, "abc"))
        .isInstanceOf(SearchQuery.InvalidQueryException.class);
  }

  @Test
  @DisplayName("offset = page * size (phân trang offset — TD-6 vẫn mở, xem ticket)")
  void offsetComputed() {
    SearchQuery query = SearchQuery.parse(null, null, null, null, "3", "25", MAX_Q);

    assertThat(query.offset()).isEqualTo(75);
  }

  @Test
  @DisplayName("khoảng giá: âm hoặc min > max → 400")
  void priceRangeValidated() {
    assertThatThrownBy(() -> SearchQuery.parse(null, null, "-1", null, null, null, MAX_Q))
        .isInstanceOf(SearchQuery.InvalidQueryException.class)
        .hasMessageContaining("không được âm");
    assertThatThrownBy(() -> SearchQuery.parse(null, null, "200", "100", null, null, MAX_Q))
        .isInstanceOf(SearchQuery.InvalidQueryException.class)
        .hasMessageContaining("minPrice");
    assertThatThrownBy(() -> SearchQuery.parse(null, null, "abc", null, null, null, MAX_Q))
        .isInstanceOf(SearchQuery.InvalidQueryException.class);
  }

  @Test
  @DisplayName("categoryId không phải UUID → 400 (không để Postgres ném lỗi cast thành 500)")
  void categoryIdValidated() {
    UUID ok = UUID.randomUUID();
    assertThat(SearchQuery.parse(null, ok.toString(), null, null, null, null, MAX_Q).categoryId())
        .isEqualTo(ok);

    assertThatThrownBy(
            () -> SearchQuery.parse(null, "khong-phai-uuid", null, null, null, null, MAX_Q))
        .isInstanceOf(SearchQuery.InvalidQueryException.class)
        .hasMessageContaining("categoryId");
  }

  @Test
  @DisplayName("khoảng giá hợp lệ được parse đúng kiểu BigDecimal")
  void priceRangeParsed() {
    SearchQuery query = SearchQuery.parse(null, null, "100000.00", "500000.50", null, null, MAX_Q);

    assertThat(query.minPrice()).isEqualByComparingTo(new BigDecimal("100000.00"));
    assertThat(query.maxPrice()).isEqualByComparingTo(new BigDecimal("500000.50"));
  }
}
