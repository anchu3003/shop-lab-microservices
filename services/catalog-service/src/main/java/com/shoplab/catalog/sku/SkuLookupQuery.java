package com.shoplab.catalog.sku;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Tham số đã validate của {@code GET /v1/skus?ids=...} (SL-2.13).
 *
 * <p>Tách khỏi controller để test được bằng unit test (không cần HTTP), cùng khuôn với {@code
 * ProductQuery}.
 *
 * <p><b>Vì sao dedupe nhưng giữ thứ tự</b>: giỏ hàng không bao giờ có 2 dòng cùng SKU, nhưng client
 * vẫn có thể gửi trùng (UI merge 2 nguồn). Dedupe để 1 SKU chỉ trả 1 lần; <b>giữ thứ tự client
 * gửi</b> ({@link LinkedHashSet}) để caller ghép kết quả vào đúng dòng của mình mà không phải tự
 * sort lại.
 */
public record SkuLookupQuery(List<UUID> ids) {

  /**
   * Trần cứng 100 — bằng trần số dòng của một đơn ({@code CreateOrderRequest.items} max 100).
   *
   * <p>Cùng lý do với {@code ProductQuery.MAX_SIZE}: đây là rate limit ở tầng dữ liệu, một client
   * không thể xin 1 triệu id trong 1 request để tự DoS service.
   */
  public static final int MAX_IDS = 100;

  /**
   * Parse {@code ids} thô ({@code "a,b,c"}) → danh sách id hợp lệ.
   *
   * @throws AppException (400) khi thiếu/rỗng, có phần tử rỗng, sai định dạng UUID, hoặc quá {@link
   *     #MAX_IDS}
   */
  public static SkuLookupQuery parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw invalid("ids là bắt buộc, dạng ?ids=<uuid>,<uuid>");
    }

    String[] parts = raw.split(",", -1);
    // Đếm theo SỐ PHẦN TỬ client gửi (không phải số id sau dedupe): gửi 500 id trùng nhau vẫn là
    // 500 phần tử phải parse — chặn ở đây mới đúng ý "trần kích thước request".
    if (parts.length > MAX_IDS) {
      throw invalid("ids tối đa " + MAX_IDS + " phần tử, nhận: " + parts.length);
    }

    LinkedHashSet<UUID> unique = new LinkedHashSet<>();
    List<String> invalidTokens = new ArrayList<>();
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) {
        invalidTokens.add("(rỗng)");
        continue;
      }
      try {
        unique.add(UUID.fromString(token));
      } catch (IllegalArgumentException ex) {
        invalidTokens.add(token);
      }
    }
    if (!invalidTokens.isEmpty()) {
      throw invalid("ids phải là UUID, không đọc được: " + String.join(", ", invalidTokens));
    }
    return new SkuLookupQuery(List.copyOf(unique));
  }

  private static AppException invalid(String message) {
    return new AppException(ErrorCode.VALIDATION_ERROR, message);
  }
}
