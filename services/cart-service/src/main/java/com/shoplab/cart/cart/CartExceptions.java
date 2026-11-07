package com.shoplab.cart.cart;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/** Nhóm lỗi của giỏ hàng — mỗi class ứng với một cách client xử lý khác nhau. */
public final class CartExceptions {

  private CartExceptions() {}

  /**
   * Không xác định được chủ giỏ: không JWT và không có guest token hợp lệ → <b>401</b>.
   *
   * <p>Không phải 400: client cần lấy token mới ({@code POST /v1/carts/guest}) hoặc đăng nhập rồi
   * thử lại — đây là vấn đề danh tính, không phải payload sai.
   */
  public static class Unauthenticated extends AppException {
    public Unauthenticated(String reason) {
      super(ErrorCode.UNAUTHORIZED, "Không xác định được giỏ hàng: " + reason);
    }
  }

  /**
   * Giỏ đã đủ số dòng tối đa → <b>422</b>: client phải xoá bớt trước khi thêm (retry không giúp
   * gì).
   */
  public static class TooManyLines extends AppException {
    public TooManyLines(int maxLines) {
      super(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "Giỏ đã đủ " + maxLines + " dòng — xoá bớt trước khi thêm dòng mới");
    }
  }

  /** Số lượng ngoài {@code 1..maxQty} → <b>422</b>. */
  public static class QtyOutOfRange extends AppException {
    public QtyOutOfRange(int qty, int maxQty) {
      super(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "qty phải trong khoảng 1.." + maxQty + " (nhận được " + qty + ")");
    }
  }
}
