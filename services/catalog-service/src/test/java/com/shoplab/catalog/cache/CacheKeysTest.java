package com.shoplab.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.catalog.testsupport.CacheTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Key schema là hợp đồng giữa "ghi cache" và "xoá cache" — sai một ký tự là invalidation không bao
 * giờ khớp key, và bug đó <b>không</b> làm test nào đỏ: nó chỉ làm dữ liệu cũ sống tới hết TTL.
 *
 * <p>Vì vậy test ở đây kiểm cả <b>hình dạng</b> key (đọc được bằng mắt) và tính <b>ổn định</b> của
 * fingerprint (cùng tham số ⇒ cùng key; khác 1 tham số ⇒ khác key).
 */
class CacheKeysTest {

  private final CacheKeys keys = new CacheKeys(CacheTestSupport.ENABLED);

  @Test
  @DisplayName("key detail/sku/categories đúng schema catalog:v1:*")
  void keysUseDocumentedSchema() {
    UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID skuId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    assertThat(keys.detail(productId))
        .isEqualTo("catalog:v1:product:11111111-1111-1111-1111-111111111111");
    assertThat(keys.sku(skuId)).isEqualTo("catalog:v1:sku:22222222-2222-2222-2222-222222222222");
    assertThat(keys.categories()).isEqualTo("catalog:v1:categories");
    assertThat(keys.listGeneration()).isEqualTo("catalog:v1:list:gen");
    assertThat(keys.list(7, "abc")).isEqualTo("catalog:v1:list:7:abc");
  }

  @Test
  @DisplayName("fingerprint: cùng tham số ⇒ cùng key, đổi 1 tham số ⇒ key khác")
  void fingerprintIsStableAndDistinct() {
    UUID categoryId = UUID.randomUUID();
    String a = CacheKeys.listFingerprint(0, 20, categoryId, "ao", "name", "ASC");
    String b = CacheKeys.listFingerprint(0, 20, categoryId, "ao", "name", "ASC");
    assertThat(a).isEqualTo(b).hasSize(24);

    assertThat(CacheKeys.listFingerprint(1, 20, categoryId, "ao", "name", "ASC")).isNotEqualTo(a);
    assertThat(CacheKeys.listFingerprint(0, 50, categoryId, "ao", "name", "ASC")).isNotEqualTo(a);
    assertThat(CacheKeys.listFingerprint(0, 20, null, "ao", "name", "ASC")).isNotEqualTo(a);
    assertThat(CacheKeys.listFingerprint(0, 20, categoryId, null, "name", "ASC")).isNotEqualTo(a);
    assertThat(CacheKeys.listFingerprint(0, 20, categoryId, "ao", "price", "ASC")).isNotEqualTo(a);
    assertThat(CacheKeys.listFingerprint(0, 20, categoryId, "ao", "name", "DESC")).isNotEqualTo(a);
  }

  @Test
  @DisplayName("null và '-' không trùng nhau (nối chuỗi phải phân biệt được)")
  void nullDiffersFromLiteralDash() {
    UUID categoryId = UUID.randomUUID();
    assertThat(CacheKeys.listFingerprint(0, 20, categoryId, null, "name", "ASC"))
        .isNotEqualTo(CacheKeys.listFingerprint(0, 20, categoryId, "-", "name", "ASC"));
  }

  @Test
  @DisplayName("lock key là hash cố định 24 ký tự, không phải key đích + hậu tố")
  void lockKeyIsHashed() {
    String detailKey = keys.detail(UUID.randomUUID());
    String lockKey = keys.lock(detailKey);

    assertThat(lockKey).startsWith("catalog:v1:lock:").hasSize("catalog:v1:lock:".length() + 24);
    assertThat(lockKey).doesNotContain(detailKey);
    // Hai lần cùng key ⇒ cùng lock key (nếu không thì single-flight vô hiệu).
    assertThat(keys.lock(detailKey)).isEqualTo(lockKey);
  }
}
