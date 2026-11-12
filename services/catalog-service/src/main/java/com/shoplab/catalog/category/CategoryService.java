package com.shoplab.catalog.category;

import com.shoplab.catalog.cache.CatalogCache;
import java.text.Normalizer;
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
  private final CategoryCache cache;
  private final CatalogCache catalogCache;

  public CategoryService(
      CategoryRepository repository, Clock clock, CategoryCache cache, CatalogCache catalogCache) {
    this.repository = repository;
    this.clock = clock;
    this.cache = cache;
    this.catalogCache = catalogCache;
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
    // SL-4.2: danh sách category là MỘT key duy nhất ⇒ xoá thẳng, không cần generation.
    // Không xoá thì `e2e-smoke` (tạo category rồi list) sẽ đỏ trong 5 phút tới — chính là bug mà
    // bước verify bắt được.
    catalogCache.categoriesChanged();
    return CategoryResponse.from(saved);
  }

  /**
   * Toàn bộ category, sắp theo tên — danh sách nhỏ ở Phase 1 nên chưa phân trang.
   *
   * <p>Cache TTL dài ({@code categories-ttl}, mặc định 5 phút) vì category gần như không đổi; ghi
   * vẫn invalidate ngay nên TTL dài không làm dữ liệu cũ.
   */
  @Transactional(readOnly = true)
  public List<CategoryResponse> findAll() {
    return cache.all(this::loadAll);
  }

  private List<CategoryResponse> loadAll() {
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
  /**
   * Sinh slug từ tên: bỏ dấu tiếng Việt, chữ thường, mọi cụm ký tự không phải {@code a-z0-9} thành
   * một {@code -}.
   *
   * <p>Vì sao phải transliterate (SL-1.15): nếu chỉ lọc {@code [a-z0-9]} thì "Điện thoại" thành
   * {@code i-n-tho-i} — không ai đọc được, không ai đoán được URL. Bỏ dấu trước cho ra {@code
   * dien-thoai}.
   *
   * <p>{@code đ/Đ} không tách được bằng Unicode NFD (là chữ riêng, không phải dấu kết hợp) nên phải
   * map tay. Tên toàn ký tự đặc biệt ⇒ slug rỗng ⇒ dùng UUID để vẫn unique.
   */
  static String slugify(String name) {
    String withoutDiacritics =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            // \p{M} = mọi dấu kết hợp vừa tách ra khỏi nguyên âm.
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D');
    String slug =
        withoutDiacritics
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+)|(-+$)", "");
    return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
  }
}
