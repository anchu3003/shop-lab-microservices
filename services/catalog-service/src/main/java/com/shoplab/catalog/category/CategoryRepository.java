package com.shoplab.catalog.category;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository của {@link Category} — Spring Data sinh implementation lúc runtime. */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

  /** Slug phải duy nhất (unique index ở DB); check trước để trả 409 thay vì 500 của DB. */
  boolean existsBySlug(String slug);
}
