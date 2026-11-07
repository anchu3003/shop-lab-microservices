package com.shoplab.payment.payment;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nhóm lỗi nghiệp vụ của thanh toán — tách theo HTTP status vì client xử lý mỗi loại khác nhau.
 *
 * <p>Đặt chung file vì chúng là một họ nhỏ, mỗi class vài dòng.
 */
public final class PaymentExceptions {

  private PaymentExceptions() {}

  /**
   * Đơn đã có intent còn sống với <b>số tiền khác</b> → 409.
   *
   * <p>Không ghi đè: hai intent cho cùng một đơn là hai đường thu tiền. Nếu số tiền giống nhau thì
   * trả lại intent cũ (idempotent theo nghiệp vụ), chỉ khác tiền mới là lỗi.
   */
  public static class IntentExists extends AppException {
    public IntentExists(UUID orderId, BigDecimal existing, BigDecimal requested) {
      super(
          ErrorCode.CONFLICT,
          "Đơn "
              + orderId
              + " đã có payment intent "
              + existing
              + ", không tạo intent "
              + requested);
    }
  }

  /** Webhook báo số tiền khác số tiền của intent → 409, không đổi trạng thái. */
  public static class AmountMismatch extends AppException {
    public AmountMismatch(BigDecimal expected, BigDecimal reported) {
      super(
          ErrorCode.CONFLICT,
          "Số tiền webhook (" + reported + ") khác số tiền payment intent (" + expected + ")");
    }
  }

  /** Webhook báo trạng thái không thuộc tập hỗ trợ → 400 (lỗi payload, retry không giúp gì). */
  public static class UnsupportedWebhookStatus extends AppException {
    public UnsupportedWebhookStatus(String status) {
      super(
          ErrorCode.VALIDATION_ERROR,
          "Trạng thái webhook không hỗ trợ: " + status + " (chỉ nhận succeeded | failed)");
    }
  }

  /** Hai payment khác nhau cùng provider_txn_id (dữ liệu provider sai) → 409, không ghi gì. */
  public static class TransactionAlreadyUsed extends AppException {
    public TransactionAlreadyUsed(String providerTxnId) {
      super(ErrorCode.CONFLICT, "provider_txn_id đã được dùng cho payment khác: " + providerTxnId);
    }
  }
}
