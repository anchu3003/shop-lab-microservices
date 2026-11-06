package com.shoplab.catalog.category;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ category: tạo (chặn trùng slug) và liệt kê. */
@Service
public class CategoryService {

  private final CategoryRepository repository;
  private final Clock clock;

  public CategoryService(CategoryRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Tạo category mới.
   *
   * @throws DuplicateCategoryException khi slug đã tồn tại (409)
   */
  @Transactional
  public CategoryResponse create(CreateCategoryRequest request) {
    String name = request.name().trim();
    String slug = slugify(name);

    // Check trước để trả 409 thay vì để unique index ném lỗi DB → 500 khó hiểu cho client.
    if (repository.existsBySlug(slug)) {
      throw new DuplicateCategoryException(slug);
    }

    Category saved = repository.save(Category.create(name, slug, Instant.now(clock)));
    return CategoryResponse.from(saved);
  }

  /** Toàn bộ category, sắp theo tên — danh sách nhỏ ở Phase 1 nên chưa phân trang. */
  @Transactional(readOnly = true)
  public List<CategoryResponse> findAll() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(CategoryResponse::from)
        .toList();
  }

  /**
   * Sinh slug từ tên: chữ thường, mọi cụm ký tự không phải {@code a-z0-9} thành một {@code -}.
   *
   * <p>Tiếng Việt có dấu sẽ mất dấu ("Áo thun" → "o-thun") — chấp nhận ở Phase 1: slug chỉ để đọc,
   * muốn giữ dấu thì phải thêm bảng transliterate (chưa cần). Tên toàn ký tự đặc biệt ⇒ slug rỗng ⇒
   * dùng UUID để vẫn unique.
   */
  static String slugify(String name) {
    String slug =
        name.toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+)|(-+$)", "");
    return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
  }
}
