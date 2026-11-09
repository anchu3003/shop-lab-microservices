package com.shoplab.search.index;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Kết quả trả ra của `GET /v1/search` — chỉ field khách cần, không trả cả row index. */
public record SearchItem(
    UUID productId, String name, UUID categoryId, BigDecimal minPrice, Instant updatedAt) {}
