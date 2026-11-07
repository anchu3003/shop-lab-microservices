package com.shoplab.cart.cart;

import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng trong giỏ.
 *
 * <p>Cố ý <b>không</b> có giá/tên: quyết định của `SL-2.1` là cart không snapshot giá (không gọi
 * catalog). Giá hiển thị lấy từ product page; tiền thật do `order-service` tính lại từ payload lúc
 * checkout — nhờ vậy giá trong giỏ không bao giờ là nguồn sự thật về tiền. Hệ quả ghi ở `TD-20`.
 *
 * @param addedAt giữ nguyên từ lần thêm đầu tiên (đổi số lượng không làm "mới" dòng hàng, để sau
 *     này sort/abandoned-cart theo thời điểm thêm thật)
 */
public record CartItem(UUID skuId, int qty, Instant addedAt) {}
