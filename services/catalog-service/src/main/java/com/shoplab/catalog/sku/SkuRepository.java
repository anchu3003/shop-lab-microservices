package com.shoplab.catalog.sku;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository của {@link Sku}. */
public interface SkuRepository extends JpaRepository<Sku, UUID> {

  /** SKU của một product, sắp theo mã — thứ tự ổn định để response không "nhảy" giữa 2 lần gọi. */
  List<Sku> findByProductIdOrderByCodeAsc(UUID productId);

  /**
   * Tra <b>nhiều</b> SKU theo id + trạng thái (SL-2.13) — 1 câu query cho N id.
   *
   * <p>Điều kiện {@code status} nằm ở DB chứ không lọc ở Java: (a) không kéo row đã archive về app,
   * (b) người đọc thấy ngay "chỉ SKU đang bán" là ràng buộc của truy vấn, không phải một {@code if}
   * ai đó có thể quên ở tầng service.
   */
  List<Sku> findByIdInAndStatus(List<UUID> ids, SkuStatus status);

  /** Mã SKU phải duy nhất toàn hệ (unique index ở DB); check trước để trả 409 thay vì 500. */
  boolean existsByCode(String code);

  /**
   * Archive toàn bộ SKU của một product bằng <b>1 câu UPDATE</b> (SL-2.11).
   *
   * <p>Vì sao bulk update thay vì load từng entity rồi set: product có thể có hàng chục SKU, và hàm
   * này chạy trong transaction của {@code DELETE /v1/products/{id}}. N+1 ở đây là không cần thiết —
   * ta không cần đọc dữ liệu, chỉ cần đổi trạng thái.
   *
   * <p>{@code flushAutomatically} để thay đổi đang chờ của product được ghi trước; {@code
   * clearAutomatically} để entity SKU đã load trong cùng persistence context không trả về trạng
   * thái cũ.
   *
   * @return số SKU thực sự đổi trạng thái (0 nếu product không có SKU hoặc đã archive trước đó)
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update Sku s set s.status = :status where s.productId = :productId and s.status <> :status")
  int updateStatusByProductId(
      @Param("productId") UUID productId, @Param("status") SkuStatus status);
}
