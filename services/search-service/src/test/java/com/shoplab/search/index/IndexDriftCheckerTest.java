package com.shoplab.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoplab.search.config.SearchProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Job so index/catalog — chỉ kiểm phần "tính độ lệch + ngưỡng", không cần DB/broker.
 *
 * <p>Vì sao tách {@code checkOnce()} khỏi {@code @Scheduled}: test gọi thẳng logic thay vì chờ
 * scheduler, và ngưỡng WARN trở thành thứ kiểm được thay vì "tin vào log".
 */
class IndexDriftCheckerTest {

  private static SearchProperties properties(String threshold) {
    return new SearchProperties(
        List.of(Duration.ofMillis(200)),
        "http://localhost:8082",
        100,
        Duration.ofMinutes(10),
        Duration.ofSeconds(60),
        new BigDecimal(threshold),
        100);
  }

  @Test
  @DisplayName("lệch 0% khi index == catalog")
  void noDrift() {
    assertThat(IndexDriftChecker.driftPercent(20, 20)).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("lệch tính theo catalog (mẫu số là nguồn sự thật)")
  void driftIsRelativeToCatalog() {
    assertThat(IndexDriftChecker.driftPercent(95, 100)).isEqualByComparingTo("5.00");
    assertThat(IndexDriftChecker.driftPercent(105, 100)).isEqualByComparingTo("5.00");
  }

  @Test
  @DisplayName("catalog rỗng: index rỗng ⇒ 0%, có row ⇒ 100% (không chia cho 0)")
  void emptyCatalog() {
    assertThat(IndexDriftChecker.driftPercent(0, 0)).isEqualByComparingTo("0");
    assertThat(IndexDriftChecker.driftPercent(3, 0)).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName("checkOnce dùng đúng số của repository + catalog (không tự đếm kiểu khác)")
  void checkOnceCombinesBothSources() {
    SearchRepository index = mock(SearchRepository.class);
    CatalogClient catalog = mock(CatalogClient.class);
    when(index.countByStatus(SearchProduct.ACTIVE)).thenReturn(50L);
    when(catalog.totalActiveProducts()).thenReturn(100L);

    IndexDriftChecker checker = new IndexDriftChecker(catalog, index, properties("1"));

    assertThat(checker.checkOnce()).isEqualByComparingTo("50.00");
  }
}
