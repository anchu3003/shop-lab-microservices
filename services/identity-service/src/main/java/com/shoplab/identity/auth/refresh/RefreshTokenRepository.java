package com.shoplab.identity.auth.refresh;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Truy cập bảng {@code refresh_tokens}. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /** Đường tra cứu chính: client gửi token thật → service hash rồi tìm theo hash. */
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
