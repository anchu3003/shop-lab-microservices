package com.shoplab.inventory.reservation;

import java.util.UUID;

/** Một dòng hàng trong reservation (SKU + số lượng cần giữ). */
public record ReservationItemRecord(UUID skuId, int qty) {}
