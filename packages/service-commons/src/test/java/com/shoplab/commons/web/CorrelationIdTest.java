package com.shoplab.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrelationIdTest {

  @Test
  @DisplayName("id hợp lệ của client được giữ nguyên")
  void shouldReuseValidClientId() {
    assertThat(CorrelationId.resolve("abc-123_XYZ.9:0")).isEqualTo("abc-123_XYZ.9:0");
  }

  @Test
  @DisplayName("cắt khoảng trắng thừa ở hai đầu")
  void shouldTrimWhitespace() {
    assertThat(CorrelationId.resolve("  abc-123  ")).isEqualTo("abc-123");
  }

  @Test
  @DisplayName("thiếu header → sinh id mới, không bao giờ null")
  void shouldGenerateNewIdWhenMissing() {
    assertThat(CorrelationId.resolve(null)).isNotBlank();
  }

  @Test
  @DisplayName("id rỗng → sinh id mới")
  void shouldGenerateNewIdWhenBlank() {
    assertThat(CorrelationId.resolve("   ")).isNotBlank();
  }

  @Test
  @DisplayName("id quá dài → bỏ, sinh id mới (chặn client nhét rác vào log)")
  void shouldRejectTooLongId() {
    String tooLong = "a".repeat(CorrelationId.MAX_LENGTH + 1);

    assertThat(CorrelationId.resolve(tooLong)).isNotEqualTo(tooLong).isNotBlank();
  }

  @Test
  @DisplayName("id chứa ký tự nguy hiểm (newline, ngoặc) → bỏ, sinh id mới")
  void shouldRejectUnsafeCharacters() {
    assertThat(CorrelationId.resolve("abc\n{\"injected\":true}"))
        .isNotEqualTo("abc\n{\"injected\":true}");
    assertThat(CorrelationId.resolve("abc def")).doesNotContain(" ");
  }

  @Test
  @DisplayName("ngoài request thì current() là null, không ném lỗi")
  void shouldReturnNullOutsideRequest() {
    assertThat(CorrelationId.current()).isNull();
  }

  @Test
  @DisplayName("newId() sinh giá trị khác nhau mỗi lần")
  void shouldGenerateUniqueIds() {
    assertThat(CorrelationId.newId()).isNotEqualTo(CorrelationId.newId());
  }
}
