package com.shoplab.identity.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy cập bảng {@code users}.
 *
 * <p>Chỉ khai method thật sự dùng: repository phình toàn method không dùng là dấu hiệu service đang
 * thiếu tầng service/domain.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Tìm theo email đã normalize (chữ thường). */
  Optional<User> findByEmail(String email);

  /** Dùng trước khi insert để trả 409 rõ ràng (thay vì để DB ném unique violation). */
  boolean existsByEmail(String email);
}
