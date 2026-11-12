package com.shoplab.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.catalog.cache.CacheKeys;
import com.shoplab.catalog.cache.CacheMetrics;
import com.shoplab.catalog.cache.CacheProperties;
import com.shoplab.catalog.cache.CatalogCache;
import com.shoplab.catalog.testsupport.CacheTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class CategoryServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
  private static final CacheProperties PROPS = CacheTestSupport.DISABLED;

  private final CategoryRepository repository = mock(CategoryRepository.class);
  private final CatalogCache catalogCache = mock(CatalogCache.class);
  private final CategoryCache cache =
      new CategoryCache(
          CacheTestSupport.aside(
              new CacheTestSupport.InMemoryCacheStore(), PROPS, new CacheMetrics()),
          new CacheKeys(PROPS),
          PROPS,
          CacheTestSupport.MAPPER);

  private final CategoryService service =
      new CategoryService(repository, Clock.fixed(NOW, ZoneOffset.UTC), cache, catalogCache);

  @Test
  @DisplayName("tạo category: sinh slug từ tên, timestamps lấy từ Clock (test tất định)")
  void createShouldGenerateSlugAndTimestamps() {
    when(repository.existsBySlug("dien-thoai")).thenReturn(false);
    when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse created = service.create(new CreateCategoryRequest("  Điện Thoại  "));

    assertThat(created.name()).isEqualTo("Điện Thoại");
    // SL-1.15: bỏ dấu tiếng Việt trước khi slugify ⇒ slug đọc được, không phải "i-n-tho-i".
    assertThat(created.slug()).isEqualTo("dien-thoai");
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
  @DisplayName("slugify: bỏ dấu tiếng Việt, kể cả đ/Đ (không tách được bằng NFD)")
  void slugifyShouldStripVietnameseDiacritics() {
    assertThat(CategoryService.slugify("Điện thoại")).isEqualTo("dien-thoai");
    assertThat(CategoryService.slugify("Đồ gia dụng")).isEqualTo("do-gia-dung");
    assertThat(CategoryService.slugify("Áo thun")).isEqualTo("ao-thun");
    assertThat(CategoryService.slugify("Giày thể thao")).isEqualTo("giay-the-thao");
    // đ ở giữa từ, không có dấu thanh: "đồ" → "do", "đỏ" → "do"
    assertThat(CategoryService.slugify("Đỏ đen")).isEqualTo("do-den");
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
