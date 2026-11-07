package com.shoplab.order.idempotency;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Nhóm lỗi idempotency — tách theo HTTP status vì client xử lý mỗi loại một cách khác nhau
 * (ADR-0006 D2).
 *
 * <p>Đặt chung file vì chúng là một họ nhỏ, mỗi class chỉ vài dòng; tách file riêng sẽ làm loãng
 * tầm nhìn.
 */
public final class IdempotencyExceptions {

  private IdempotencyExceptions() {}

  /** Thiếu header {@code Idempotency-Key} ở endpoint bắt buộc → <b>400</b>. */
  public static class MissingIdempotencyKey extends AppException {
    public MissingIdempotencyKey() {
      super(ErrorCode.VALIDATION_ERROR, "Thiếu header Idempotency-Key (bắt buộc, tối đa 64 ký tự)");
    }
  }

  /** Cùng key nhưng payload khác → <b>422</b>: client phải sửa request, retry không giúp gì. */
  public static class KeyReused extends AppException {
    public KeyReused(String key) {
      super(ErrorCode.UNPROCESSABLE_ENTITY, "Idempotency-Key đã dùng cho payload khác: " + key);
    }
  }

  /**
   * Request trước cùng key còn đang chạy (hoặc đã crash, chờ job dọn) → <b>409</b> + Retry-After.
   */
  public static class InProgress extends AppException {
    public InProgress(String key) {
      super(
          ErrorCode.CONFLICT,
          "Request trước với Idempotency-Key này chưa xong, thử lại sau: " + key);
    }
  }
}
