package com.shoplab.order.saga.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Voucher lock đã giữ ở `promotion-service` (`POST /v1/vouchers/lock`).
 *
 * <p>Saga dùng {@code discount} để dựng order và {@code lockId} để **release** khi phải bù. Không
 * nhận {@code finalTotal} làm số tiền phải thu: tiền của đơn là do `OrderPricing` tính từ snapshot
 * (một nguồn sự thật cho con số trên hoá đơn).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VoucherLock(UUID lockId, BigDecimal discount) {}
