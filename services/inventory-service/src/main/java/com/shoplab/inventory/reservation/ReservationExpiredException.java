package com.shoplab.inventory.reservation;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Reservation đã quá TTL → HTTP <b>410 Gone</b>.
 *
 * <p>Vì sao 410 chứ không 409: 409 = "xung đột trạng thái, thử lại có ích"; 410 = "tài nguyên này
 * đã chết, đừng thử lại" — đúng ngữ nghĩa với reservation hết hạn (client phải tạo reservation
 * mới). Đây cũng là mã mà ADR-0006 D1 đã ghi.
 */
public class ReservationExpiredException extends AppException {

  public ReservationExpiredException(String message) {
    super(ErrorCode.GONE, message);
  }
}
