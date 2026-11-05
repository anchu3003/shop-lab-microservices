package com.shoplab.identity.auth;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;

/**
 * Refresh token không hợp lệ: không tồn tại, đã hết hạn, đã logout, hoặc <b>đã được rotate và bị
 * dùng lại</b> (dấu hiệu token bị đánh cắp).
 *
 * <p>Mọi trường hợp trả <b>cùng một</b> lỗi 401 với cùng message: phân biệt chi tiết sẽ giúp kẻ tấn
 * công dò được token nào còn sống (oracle). Chi tiết thật ghi ở log phía server.
 */
public class InvalidRefreshTokenException extends AppException {

  public InvalidRefreshTokenException() {
    super(ErrorCode.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã bị thu hồi");
  }
}
