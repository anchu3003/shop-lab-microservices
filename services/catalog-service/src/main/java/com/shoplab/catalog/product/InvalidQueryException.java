package com.shoplab.catalog.product;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Tham số truy vấn list không hợp lệ (page/size/sort/categoryId/q) → HTTP 400.
 *
 * <p>Vì sao 400 chứ không âm thầm "clamp" (kẹp về giá trị hợp lệ): client gửi {@code size=1000} mà
 * server lặng lẽ trả 20 là bug im lặng — client tưởng đã lấy đủ dữ liệu. Trả 400 kèm message nêu rõ
 * field thì sửa một lần là xong.
 */
public class InvalidQueryException extends AppException {

  public InvalidQueryException(String message) {
    super(ErrorCode.VALIDATION_ERROR, message);
  }
}
