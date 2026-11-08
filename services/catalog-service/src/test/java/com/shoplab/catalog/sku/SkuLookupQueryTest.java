package com.shoplab.catalog.sku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho parse {@code GET /v1/skus?ids=...} (SL-2.13).
 *
 * <p>Test riêng phần validate vì đây là <b>cửa vào public</b>: mọi input rác phải bị chặn ở đây
 * thành 400 có message đọc được, chứ không lọt xuống tầng repository.
 */
class SkuLookupQueryTest {

  @Test
  @DisplayName("ids hợp lệ: giữ NGUYÊN thứ tự client gửi")
  void parseKeepsOrder() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    SkuLookupQuery query = SkuLookupQuery.parse(" " + second + " , " + first + " ");

    assertThat(query.ids()).containsExactly(second, first);
  }

  @Test
  @DisplayName("id trùng lặp bị dedupe (1 SKU trả 1 lần) nhưng vẫn giữ thứ tự xuất hiện đầu")
  void parseDeduplicates() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    SkuLookupQuery query = SkuLookupQuery.parse(first + "," + second + "," + first + "," + second);

    assertThat(query.ids()).containsExactly(first, second);
  }

  @Test
  @DisplayName("đúng 100 id → OK (biên trên)")
  void parseAcceptsMaxIds() {
    String raw =
        IntStream.range(0, SkuLookupQuery.MAX_IDS)
            .mapToObj(i -> UUID.randomUUID().toString())
            .collect(Collectors.joining(","));

    assertThat(SkuLookupQuery.parse(raw).ids()).hasSize(SkuLookupQuery.MAX_IDS);
  }

  @Test
  @DisplayName("101 id → 400, không âm thầm cắt bớt")
  void parseRejectsTooMany() {
    String raw =
        IntStream.range(0, SkuLookupQuery.MAX_IDS + 1)
            .mapToObj(i -> UUID.randomUUID().toString())
            .collect(Collectors.joining(","));

    assertThatThrownBy(() -> SkuLookupQuery.parse(raw))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("tối đa " + SkuLookupQuery.MAX_IDS)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  @DisplayName("id sai định dạng UUID → 400 (không để 500 từ tầng dưới)")
  void parseRejectsMalformedId() {
    assertThatThrownBy(() -> SkuLookupQuery.parse("khong-phai-uuid"))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("UUID")
        .hasMessageContaining("khong-phai-uuid");
  }

  @Test
  @DisplayName("phần tử rỗng (vd 'a,,b') → 400 thay vì bỏ qua im lặng")
  void parseRejectsEmptyElement() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> SkuLookupQuery.parse(id + ",," + UUID.randomUUID()))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("(rỗng)");
  }

  @Test
  @DisplayName("thiếu ids hoặc ids rỗng → 400")
  void parseRejectsMissingIds() {
    assertThatThrownBy(() -> SkuLookupQuery.parse(null))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("bắt buộc");
    assertThatThrownBy(() -> SkuLookupQuery.parse("   "))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("bắt buộc");
  }
}
