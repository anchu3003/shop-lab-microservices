package com.shoplab.payment.webhook;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Lỗi xác thực webhook — đều là <b>401</b>: webhook là endpoint public, không có JWT, nên chữ ký là
 * thứ duy nhất chứng minh request đến từ provider.
 *
 * <p>Tách riêng khỏi {@code PaymentExceptions} vì chúng thuộc tầng xác thực (verify trước khi
 * parse), không phải nghiệp vụ.
 */
public final class WebhookExceptions {

  private WebhookExceptions() {}

  /** Thiếu header hoặc chữ ký không khớp. */
  public static class InvalidSignature extends AppException {
    public InvalidSignature(String reason) {
      super(ErrorCode.UNAUTHORIZED, "Chữ ký webhook không hợp lệ: " + reason);
    }
  }

  /**
   * Timestamp lệch quá cửa sổ cho phép ⇒ request cũ bị phát lại (replay attack).
   *
   * <p>Chữ ký một mình không chặn được điều này: kẻ tấn công giữ nguyên body + chữ ký cũ và bắn lại
   * sau nhiều giờ. Kiểm timestamp là mảnh ghép bắt buộc đi kèm HMAC.
   */
  public static class StaleTimestamp extends AppException {
    public StaleTimestamp(long skewSeconds) {
      super(
          ErrorCode.UNAUTHORIZED,
          "Timestamp webhook lệch " + skewSeconds + "s — ngoài cửa sổ cho phép (replay?)");
    }
  }
}
