package com.shoplab.gateway.auth;

/**
 * Không lấy được JWKS (identity chết, timeout, JSON hỏng) — khác hẳn "token sai".
 *
 * <p>Phân biệt này quan trọng vì nó quyết định status trả cho client: token sai là lỗi của client
 * (<b>401</b>, đừng retry), còn không verify được vì thiếu khoá là lỗi hạ tầng (<b>503</b>, retry
 * được). Gộp hai thứ này thành 401 sẽ khiến client tưởng mình hết quyền và đăng nhập lại vô ích.
 */
public class JwksUnavailableException extends RuntimeException {

  public JwksUnavailableException(String message) {
    super(message);
  }

  public JwksUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
