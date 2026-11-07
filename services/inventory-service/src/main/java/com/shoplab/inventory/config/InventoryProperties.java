package com.shoplab.inventory.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Tham số nghiệp vụ của inventory-service — đọc từ env để đổi hành vi không phải build lại
 * (12-factor).
 *
 * @param warehouseId kho đang dùng. Phase 2 chỉ có 1 kho nhưng <b>mọi</b> câu SQL đều kèm cột này ⇒
 *     mở multi-warehouse sau không phải sửa truy vấn, chỉ đổi cách chọn kho.
 * @param reservationTtl thời gian giữ chỗ (ADR-0006 D1: 15 phút, khớp luồng checkout §6.1)
 * @param maxItemsPerReservation trần số SKU trong 1 reservation — chặn 1 request 10.000 dòng làm
 *     nghẽn DB (rate limit ở tầng dữ liệu, rẻ hơn rate limit thật)
 * @param sweepBatchSize số reservation tối đa sweeper xử lý mỗi nhịp (SL-2.3) — batch để 1 lần quét
 *     không giữ transaction quá lâu và không lock cả bảng
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.inventory")
public record InventoryProperties(
    @DefaultValue("WH-1") @NotBlank String warehouseId,
    @DefaultValue("15m") Duration reservationTtl,
    @DefaultValue("50") @Min(1) @Max(500) int maxItemsPerReservation,
    @DefaultValue("100") @Min(1) @Max(1000) int sweepBatchSize) {

  public InventoryProperties {
    if (reservationTtl == null || reservationTtl.isNegative() || reservationTtl.isZero()) {
      throw new IllegalArgumentException("shoplab.inventory.reservation-ttl phải > 0");
    }
  }
}
