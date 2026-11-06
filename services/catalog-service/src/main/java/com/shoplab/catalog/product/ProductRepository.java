package com.shoplab.catalog.product;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository của {@link Product}. Danh sách + filter + phân trang đến ở SL-1.6. */
public interface ProductRepository extends JpaRepository<Product, UUID> {}
