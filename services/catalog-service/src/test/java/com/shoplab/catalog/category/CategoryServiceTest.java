package com.shoplab.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class CategoryServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  private final CategoryRepository repository = mock(CategoryRepository.class);

  private final CategoryService service =
      new CategoryService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  @DisplayName("tạo category: sinh slug từ tên, timestamps lấy từ Clock (test tất định)")
  void createShouldGenerateSlugAndTimestamps() {
    when(repository.existsBySlug("dien-thoai")).thenReturn(false);
    when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse created = service.create(new CreateCategoryRequest("  Điện Thoại  "));

    assertThat(created.name()).isEqualTo("Điện Thoại");
    // Tiếng Việt có dấu: mỗi ký tự không thuộc [a-z0-9] thành một '-' (chưa transliterate ở Phase
    // 1).
    assertThat(created.slug()).isEqualTo("i-n-tho-i");
    assertThat(created.createdAt()).isEqualTo(NOW);
    assertThat(created.updatedAt()).isEqualTo(NOW);
    assertThat(created.id()).isNotNull();
  }

  @Test
  @DisplayName("trùng slug → DuplicateCategoryException (409), KHÔNG ghi DB")
  void createShouldRejectDuplicateSlug() {
    when(repository.existsBySlug("ao-thun")).thenReturn(true);

    assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Ao thun")))
        .isInstanceOf(DuplicateCategoryException.class)
        .hasMessageContaining("ao-thun");

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("slugify: khoảng trắng liên tiếp gộp thành một '-', bỏ '-' ở hai đầu")
  void slugifyShouldCollapseSeparators() {
    assertThat(CategoryService.slugify("  Ao   Thun  ")).isEqualTo("ao-thun");
    assertThat(CategoryService.slugify("Giay/Dep-2026")).isEqualTo("giay-dep-2026");
  }

  @Test
  @DisplayName("tên toàn ký tự đặc biệt → slug fallback bằng UUID (vẫn unique)")
  void slugifyShouldFallbackToUuid() {
    assertThat(CategoryService.slugify("!!!")).hasSize(36);
  }

  @Test
  @DisplayName("findAll sắp theo tên (sort do DB làm, không sort trong bộ nhớ)")
  void findAllShouldSortByName() {
    Category category = Category.create("Áo", "ao", NOW);
    when(repository.findAll(any(Sort.class))).thenReturn(List.of(category));

    assertThat(service.findAll()).hasSize(1);
    verify(repository).findAll(eq(Sort.by(Sort.Direction.ASC, "name")));
  }
}
