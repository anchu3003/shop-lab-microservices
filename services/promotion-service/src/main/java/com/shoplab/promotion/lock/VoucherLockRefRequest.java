package com.shoplab.promotion.lock;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body của {@code POST /v1/vouchers/commit} và {@code /release} — chỉ cần id của lock. */
public record VoucherLockRefRequest(@NotNull UUID lockId) {}
